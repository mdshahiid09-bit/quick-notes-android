package com.quicknotes.shahid.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isArchived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun getActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 1 ORDER BY trashedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE filePath = :path LIMIT 1")
    suspend fun getNoteByPath(path: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isArchived = 0 AND folder = :folder ORDER BY pinned DESC, updatedAt DESC")
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isTrash = 0 AND isArchived = 0 AND (title LIKE '%' || :query || '%' OR preview LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY pinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM notes WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("DELETE FROM notes WHERE isTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE isTrash = 1 AND trashedAt IS NOT NULL AND trashedAt < :threshold")
    suspend fun purgeOldTrash(threshold: Long): Int

    @Query("UPDATE notes SET isTrash = 1, trashedAt = :now, pinned = 0 WHERE id = :id")
    suspend fun moveToTrash(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isTrash = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("UPDATE notes SET isArchived = :archived, pinned = 0 WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE notes SET color = :color WHERE id = :id")
    suspend fun setColor(id: String, color: String)

    @Query("UPDATE notes SET folder = :newFolder WHERE id = :id")
    suspend fun moveToFolder(id: String, newFolder: String)

    @Query("UPDATE notes SET isTrash = 1, trashedAt = :now, pinned = 0 WHERE folder = :folder")
    suspend fun trashNotesInFolder(folder: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET folder = :newFolder WHERE folder = :oldFolder")
    suspend fun renameFolderInNotes(oldFolder: String, newFolder: String)
}
