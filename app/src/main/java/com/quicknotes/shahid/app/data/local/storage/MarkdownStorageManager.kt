package com.quicknotes.shahid.app.data.local.storage

import android.content.Context
import android.os.Environment
import com.quicknotes.shahid.app.data.local.db.QuickNotesDatabase
import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

class MarkdownStorageManager(
    private val context: Context,
    private val database: QuickNotesDatabase
) {

    companion object {
        const val ROOT_FOLDER_NAME = "QuickNotes"
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000L
    }

    fun getNotesRootDirectory(): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docsDir, ROOT_FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getFolderDirectory(folderName: String): File {
        val root = getNotesRootDirectory()
        if (folderName.isBlank()) return root
        val sanitized = sanitizeFileName(folderName)
        val dir = File(root, sanitized)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun sanitizeFileName(name: String): String {
        val cleanName = name.trim().removeSuffix(".md").removeSuffix(".MD")
        val sanitized = cleanName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (sanitized.isEmpty()) "Untitled" else sanitized.take(80)
    }

    suspend fun scanStorage(): Unit = withContext(Dispatchers.IO) {
        val root = getNotesRootDirectory()
        if (!root.exists() || !root.isDirectory) return@withContext

        val noteDao = database.noteDao()
        val folderDao = database.folderDao()
        val now = System.currentTimeMillis()

        // 1. Auto-purge old trash (older than 30 days)
        val purgeThreshold = now - THIRTY_DAYS_MS
        val existingTrash = noteDao.getTrashNotes()
        // Purge files from disk
        val oldNotes = noteDao.getAllNotesSync().filter { it.isTrash && it.trashedAt != null && it.trashedAt < purgeThreshold }
        for (oldNote in oldNotes) {
            val file = File(oldNote.filePath)
            if (file.exists()) {
                file.delete()
            }
            noteDao.deleteById(oldNote.id)
        }

        // 2. Discover folders and files
        val discoveredFiles = mutableListOf<File>()
        val discoveredFolders = mutableSetOf<String>()

        fun traverse(dir: File, currentFolder: String) {
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    discoveredFolders.add(child.name)
                    traverse(child, child.name)
                } else if (child.isFile && child.extension.equals("md", ignoreCase = true)) {
                    discoveredFiles.add(child)
                }
            }
        }

        traverse(root, "")

        // Sync folders to Room
        for (folderName in discoveredFolders) {
            val existing = folderDao.getFolderByName(folderName)
            if (existing == null) {
                folderDao.insertFolder(
                    FolderEntity(
                        id = UUID.randomUUID().toString(),
                        name = folderName,
                        createdAt = now
                    )
                )
            }
        }

        // Sync notes to Room
        val existingNotes = noteDao.getAllNotesSync().associateBy { it.filePath }
        val activePathsOnDisk = mutableSetOf<String>()

        for (file in discoveredFiles) {
            val path = file.absolutePath
            activePathsOnDisk.add(path)
            val fileLastModified = file.lastModified()
            val existing = existingNotes[path]

            if (existing == null || existing.updatedAt != fileLastModified) {
                try {
                    val rawText = file.readText()
                    val (frontMatter, body) = FrontMatterParser.parse(rawText)
                    val noteId = existing?.id ?: UUID.randomUUID().toString()
                    val noteTitle = FrontMatterParser.extractTitle(body, file.nameWithoutExtension)
                    val preview = FrontMatterParser.stripMarkdownForPreview(body)

                    // Determine folder from relative path
                    val parentFile = file.parentFile
                    val folderName = if (parentFile != null && parentFile.absolutePath != root.absolutePath) {
                        parentFile.name
                    } else {
                        frontMatter.folder
                    }

                    val createdAtEpoch = try {
                        Instant.parse(frontMatter.createdIso).toEpochMilli()
                    } catch (e: Exception) {
                        fileLastModified
                    }

                    val updatedNote = NoteEntity(
                        id = noteId,
                        title = noteTitle,
                        filePath = path,
                        folder = folderName,
                        tags = frontMatter.tags,
                        pinned = frontMatter.pinned,
                        color = frontMatter.color,
                        createdAt = createdAtEpoch,
                        updatedAt = fileLastModified,
                        preview = preview,
                        isArchived = frontMatter.isArchived,
                        isTrash = frontMatter.isTrash,
                        trashedAt = frontMatter.trashedAtEpoch
                    )
                    noteDao.insertOrUpdate(updatedNote)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Remove notes that were deleted on disk outside app
        for ((existingPath, note) in existingNotes) {
            if (!activePathsOnDisk.contains(existingPath)) {
                noteDao.deleteByPath(existingPath)
            }
        }
    }

    suspend fun saveNote(
        existingId: String?,
        title: String,
        body: String,
        folder: String = "",
        tags: List<String> = emptyList(),
        pinned: Boolean = false,
        color: String = "default",
        isArchived: Boolean = false,
        isTrash: Boolean = false,
        trashedAt: Long? = null
    ): NoteEntity = withContext(Dispatchers.IO) {
        val noteDao = database.noteDao()
        val now = System.currentTimeMillis()
        val nowIso = DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(now))

        val existingNote = if (existingId != null) noteDao.getNoteById(existingId) else null
        val targetDir = getFolderDirectory(folder)
        val sanitizedTitle = sanitizeFileName(if (title.isBlank()) "Untitled" else title)

        // Resolve file path
        var file = if (existingNote != null) {
            val oldFile = File(existingNote.filePath)
            val parentMatches = oldFile.parentFile?.absolutePath == targetDir.absolutePath
            val titleMatches = oldFile.nameWithoutExtension == sanitizedTitle
            if (parentMatches && titleMatches) {
                oldFile
            } else {
                // Moving or renaming file
                val newFile = generateUniqueFile(targetDir, sanitizedTitle)
                if (oldFile.exists()) {
                    oldFile.delete()
                }
                newFile
            }
        } else {
            generateUniqueFile(targetDir, sanitizedTitle)
        }

        val createdIso = if (existingNote != null) {
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(existingNote.createdAt))
        } else {
            nowIso
        }

        val frontMatter = NoteFrontMatter(
            tags = tags,
            pinned = pinned,
            color = color,
            createdIso = createdIso,
            updatedIso = nowIso,
            isArchived = isArchived,
            isTrash = isTrash,
            trashedAtEpoch = trashedAt,
            folder = folder
        )

        val fullContent = FrontMatterParser.serialize(frontMatter, body)
        file.writeText(fullContent)
        val fileModifiedTime = file.lastModified()

        val cleanTitle = if (title.isBlank()) FrontMatterParser.extractTitle(body, sanitizedTitle) else title
        val preview = FrontMatterParser.stripMarkdownForPreview(body)

        val entity = NoteEntity(
            id = existingNote?.id ?: UUID.randomUUID().toString(),
            title = cleanTitle,
            filePath = file.absolutePath,
            folder = folder,
            tags = tags,
            pinned = pinned,
            color = color,
            createdAt = existingNote?.createdAt ?: now,
            updatedAt = fileModifiedTime,
            preview = preview,
            isArchived = isArchived,
            isTrash = isTrash,
            trashedAt = trashedAt
        )

        noteDao.insertOrUpdate(entity)
        entity
    }

    private fun generateUniqueFile(directory: File, baseName: String): File {
        var file = File(directory, "$baseName.md")
        var counter = 1
        while (file.exists()) {
            file = File(directory, "${baseName}_$counter.md")
            counter++
        }
        return file
    }

    suspend fun readNoteBody(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext ""
        val raw = file.readText()
        val (_, body) = FrontMatterParser.parse(raw)
        body
    }

    suspend fun deletePermanently(noteId: String): Unit = withContext(Dispatchers.IO) {
        val noteDao = database.noteDao()
        val note = noteDao.getNoteById(noteId) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            file.delete()
        }
        noteDao.deleteById(noteId)
    }

    suspend fun emptyTrash(): Unit = withContext(Dispatchers.IO) {
        val noteDao = database.noteDao()
        val trashNotes = noteDao.getAllNotesSync().filter { it.isTrash }
        for (note in trashNotes) {
            val file = File(note.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        noteDao.emptyTrash()
    }

    suspend fun createFolder(name: String): FolderEntity = withContext(Dispatchers.IO) {
        val folderDao = database.folderDao()
        val dir = getFolderDirectory(name)
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis()
        )
        folderDao.insertFolder(folder)
        folder
    }

    suspend fun renameFolder(id: String, oldName: String, newName: String): Unit = withContext(Dispatchers.IO) {
        val folderDao = database.folderDao()
        val noteDao = database.noteDao()
        val oldDir = getFolderDirectory(oldName)
        val newDir = getFolderDirectory(newName)

        if (oldDir.exists()) {
            oldDir.renameTo(newDir)
        }

        folderDao.renameFolder(id, newName)
        noteDao.renameFolderInNotes(oldName, newName)

        // Rescan to update file paths
        scanStorage()
    }

    suspend fun deleteFolder(id: String, name: String): Unit = withContext(Dispatchers.IO) {
        val folderDao = database.folderDao()
        val noteDao = database.noteDao()

        // Move notes to trash
        noteDao.trashNotesInFolder(name, System.currentTimeMillis())
        folderDao.deleteFolder(id)

        // Re-serialize affected notes with trash front matter
        val notesInFolder = noteDao.getAllNotesSync().filter { it.folder == name }
        for (note in notesInFolder) {
            val file = File(note.filePath)
            if (file.exists()) {
                val raw = file.readText()
                val (fm, body) = FrontMatterParser.parse(raw)
                val updatedFm = fm.copy(isTrash = true, trashedAtEpoch = System.currentTimeMillis(), pinned = false)
                file.writeText(FrontMatterParser.serialize(updatedFm, body))
            }
        }
    }
}