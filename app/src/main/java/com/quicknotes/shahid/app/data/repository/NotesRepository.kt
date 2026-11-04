package com.quicknotes.shahid.app.data.repository

import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun getActiveNotes(): Flow<List<NoteEntity>>
    fun getArchivedNotes(): Flow<List<NoteEntity>>
    fun getTrashNotes(): Flow<List<NoteEntity>>
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>
    fun searchNotes(query: String): Flow<List<NoteEntity>>
    fun getAllFolders(): Flow<List<FolderEntity>>
    fun getAllTagsWithCounts(): Flow<Map<String, Int>>

    suspend fun getNoteById(id: String): NoteEntity?
    suspend fun readNoteContent(filePath: String): String
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
    ): NoteEntity

    suspend fun moveToTrash(id: String)
    suspend fun restoreFromTrash(id: String)
    suspend fun deletePermanently(id: String)
    suspend fun emptyTrash()
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun setPinned(id: String, pinned: Boolean)
    suspend fun setColor(id: String, color: String)
    suspend fun moveToFolder(id: String, folder: String)

    suspend fun createFolder(name: String): FolderEntity
    suspend fun renameFolder(id: String, oldName: String, newName: String)
    suspend fun deleteFolder(id: String, name: String)

    suspend fun scanStorage()
}
