package com.quicknotes.shahid.app.ui.screens.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val archivedNotes: List<NoteEntity> = emptyList(),
    val isLoading: Boolean = false
)

class ArchiveViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    val uiState: StateFlow<ArchiveUiState> = notesRepository.getArchivedNotes()
        .map { notes -> ArchiveUiState(archivedNotes = notes, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ArchiveUiState(isLoading = true)
        )

    fun unarchiveNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.setArchived(noteId, false)
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            notesRepository.moveToTrash(noteId)
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ArchiveViewModel(notesRepository) as T
            }
        }
    }
}
