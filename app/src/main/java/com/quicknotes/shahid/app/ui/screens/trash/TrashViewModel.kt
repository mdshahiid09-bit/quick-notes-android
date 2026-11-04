package com.quicknotes.shahid.app.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.local.storage.MarkdownStorageManager
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrashUiState(
    val trashNotes: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = false
)

class TrashViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = notesRepository.getTrashNotes()
        .map { notes -> TrashUiState(trashNotes = notes, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrashUiState(isLoading = true)
        )

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.restoreFromTrash(noteId)
        }
    }

    fun deleteForever(noteId: String) {
        viewModelScope.launch {
            notesRepository.deletePermanently(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            notesRepository.emptyTrash()
        }
    }

    fun calculateDaysLeft(trashedAt: Long?): Int {
        if (trashedAt == null) return 30
        val elapsed = System.currentTimeMillis() - trashedAt
        val daysLeft = (MarkdownStorageManager.THIRTY_DAYS_MS - elapsed) / (24 * 60 * 60 * 1000L)
        return daysLeft.coerceAtLeast(0).toInt()
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TrashViewModel(notesRepository) as T
            }
        }
    }
}
