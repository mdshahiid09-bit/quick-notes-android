package com.quicknotes.shahid.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["folder"]),
        Index(value = ["isTrash"]),
        Index(value = ["isArchived"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val filePath: String,
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val color: String = "default",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val preview: String = "",
    val isArchived: Boolean = false,
    val isTrash: Boolean = false,
    val trashedAt: Long? = null
)
