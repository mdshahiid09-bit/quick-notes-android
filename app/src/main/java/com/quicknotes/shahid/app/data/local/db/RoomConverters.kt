package com.quicknotes.shahid.app.data.local.db

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromTagList(tags: List<String>?): String {
        return tags?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toTagList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
