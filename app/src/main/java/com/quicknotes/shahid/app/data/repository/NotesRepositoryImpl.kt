package com.quicknotes.shahid.app.data.repository

import com.quicknotes.shahid.app.data.local.db.QuickNotesDatabase
import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.local.storage.FrontMatterParser
import com.quicknotes.shahid.app.data.local.storage.MarkdownStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class NotesRepositoryImpl(
    private val database: QuickNotesDatabase,
    private val storageManager: MarkdownStorageManager
) : NotesRepository {

    private val noteDao = database.noteDao()
    private val folderDao = database.folderDao()

    override fun getActiveNotes(): Flow<List<NoteEntity>> = noteDao.getActiveNotes()

    override fun getArchivedNotes(): Flow<List<NoteEntity>> = noteDao.getArchivedNotes()

    override fun getTrashNotes(): Flow<List<NoteEntity>> = noteDao.getTrashNotes()

    override fun getNotesByFolder(folder: String): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folder)

    override fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    override fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    override fun getAllTagsWithCounts(): Flow<Map<String, Int>> {
        return noteDao.getActiveNotes().map { notes ->
            val tagMap = mutableMapOf<String, Int>()
            for (note in notes) {
                for (tag in note.tags) {
                    val clean = tag.trim()
                    if (clean.isNotEmpty()) {
                        tagMap[clean] = (tagMap[clean] ?: 0) + 1
                    }
                }
            }
            tagMap
        }
    }

    override suspend fun getNoteById(id: String): NoteEntity? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    override suspend fun readNoteContent(filePath: String): String = withContext(Dispatchers.IO) {
        storageManager.readNoteBody(filePath)
    }

    override suspend fun saveNote(
        existingId: String?,
        title: String,
        body: String,
        folder: String,
        tags: List<String>,
        pinned: Boolean,
        color: String,
        isArchived: Boolean,
        isTrash: Boolean,
        trashedAt: Long?
    ): NoteEntity = withContext(Dispatchers.IO) {
        storageManager.saveNote(
            existingId = existingId,
            title = title,
            body = body,
            folder = folder,
            tags = tags,
            pinned = pinned,
            color = color,
            isArchived = isArchived,
            isTrash = isTrash,
            trashedAt = trashedAt
        )
    }

    override suspend fun moveToTrash(id: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        noteDao.moveToTrash(id, now)
        val note = noteDao.getNoteById(id) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            val (fm, body) = FrontMatterParser.parse(file.readText())
            val updated = fm.copy(isTrash = true, trashedAtEpoch = now, pinned = false)
            file.writeText(FrontMatterParser.serialize(updated, body))
        }
    }

    override suspend fun restoreFromTrash(id: String) = withContext(Dispatchers.IO) {
        noteDao.restoreFromTrash(id)
        val note = noteDao.getNoteById(id) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            val (fm, body) = FrontMatterParser.parse(file.readText())
            val updated = fm.copy(isTrash = false, trashedAtEpoch = null)
            file.writeText(FrontMatterParser.serialize(updated, body))
        }
    }

    override suspend fun deletePermanently(id: String) = withContext(Dispatchers.IO) {
        storageManager.deletePermanently(id)
    }

    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        storageManager.emptyTrash()
    }

    override suspend fun setArchived(id: String, archived: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setArchived(id, archived)
        val note = noteDao.getNoteById(id) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            val (fm, body) = FrontMatterParser.parse(file.readText())
            val updated = fm.copy(isArchived = archived, pinned = if (archived) false else fm.pinned)
            file.writeText(FrontMatterParser.serialize(updated, body))
        }
    }

    override suspend fun setPinned(id: String, pinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.setPinned(id, pinned)
        val note = noteDao.getNoteById(id) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            val (fm, body) = FrontMatterParser.parse(file.readText())
            val updated = fm.copy(pinned = pinned)
            file.writeText(FrontMatterParser.serialize(updated, body))
        }
    }

    override suspend fun setColor(id: String, color: String) = withContext(Dispatchers.IO) {
        noteDao.setColor(id, color)
        val note = noteDao.getNoteById(id) ?: return@withContext
        val file = File(note.filePath)
        if (file.exists()) {
            val (fm, body) = FrontMatterParser.parse(file.readText())
            val updated = fm.copy(color = color)
            file.writeText(FrontMatterParser.serialize(updated, body))
        }
    }

    override suspend fun moveToFolder(id: String, folder: String) = withContext(Dispatchers.IO) {
        val note = noteDao.getNoteById(id) ?: return@withContext
        val body = readNoteContent(note.filePath)
        saveNote(
            existingId = id,
            title = note.title,
            body = body,
            folder = folder,
            tags = note.tags,
            pinned = note.pinned,
            color = note.color,
            isArchived = note.isArchived,
            isTrash = note.isTrash,
            trashedAt = note.trashedAt
        )
    }

    override suspend fun createFolder(name: String): FolderEntity = withContext(Dispatchers.IO) {
        storageManager.createFolder(name)
    }

    override suspend fun renameFolder(id: String, oldName: String, newName: String) = withContext(Dispatchers.IO) {
        storageManager.renameFolder(id, oldName, newName)
    }

    override suspend fun deleteFolder(id: String, name: String) = withContext(Dispatchers.IO) {
        storageManager.deleteFolder(id, name)
    }

    override suspend fun scanStorage() = withContext(Dispatchers.IO) {
        storageManager.scanStorage()
    }
}
