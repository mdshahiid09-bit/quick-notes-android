package com.quicknotes.shahid.app.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FolderNotesUiState(
    val folderName: String = "",
    val pinnedNotes: List<NoteEntity> = emptyList(),
    val otherNotes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val viewMode: NotesViewMode = NotesViewMode.GRID,
    val isLoading: Boolean = true
)

class FolderNotesViewModel(
    private val folderName: String,
    private val notesRepository: NotesRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(FolderNotesUiState(folderName = folderName))
    val uiState: StateFlow<FolderNotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                notesRepository.getNotesByFolder(folderName),
                preferencesRepository.notesViewMode,
                _searchQuery,
                _isSearchActive
            ) { notes, viewMode, query, isSearchActive ->
                val filtered = if (query.isBlank()) {
                    notes
                } else {
                    notes.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.preview.contains(query, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                    }
                }
                val pinned = filtered.filter { it.pinned }
                val others = filtered.filter { !it.pinned }
                FolderNotesUiState(
                    folderName = folderName,
                    pinnedNotes = pinned,
                    otherNotes = others,
                    searchQuery = query,
                    isSearchActive = isSearchActive,
                    viewMode = viewMode,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun toggleViewMode() {
        val next = if (_uiState.value.viewMode == NotesViewMode.GRID) NotesViewMode.LIST else NotesViewMode.GRID
        viewModelScope.launch {
            preferencesRepository.setNotesViewMode(next)
        }
    }

    fun togglePinned(note: NoteEntity) {
        viewModelScope.launch {
            notesRepository.setPinned(note.id, !note.pinned)
        }
    }

    fun trashNote(noteId: String) {
        viewModelScope.launch {
            notesRepository.moveToTrash(noteId)
        }
    }

    companion object {
        fun provideFactory(
            folderName: String,
            notesRepository: NotesRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FolderNotesViewModel(folderName, notesRepository, preferencesRepository) as T
            }
        }
    }
}
