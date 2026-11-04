package com.quicknotes.shahid.app.ui.screens.tags

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

data class TagNotesUiState(
    val tag: String = "",
    val pinnedNotes: List<NoteEntity> = emptyList(),
    val otherNotes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val viewMode: NotesViewMode = NotesViewMode.GRID,
    val isLoading: Boolean = true
)

class TagNotesViewModel(
    private val tag: String,
    private val notesRepository: NotesRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(TagNotesUiState(tag = tag))
    val uiState: StateFlow<TagNotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                notesRepository.getActiveNotes(),
                preferencesRepository.notesViewMode,
                _searchQuery,
                _isSearchActive
            ) { notes, viewMode, query, isSearchActive ->
                val matchingTagNotes = notes.filter { it.tags.contains(tag) }
                val filtered = if (query.isBlank()) {
                    matchingTagNotes
                } else {
                    matchingTagNotes.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.preview.contains(query, ignoreCase = true)
                    }
                }
                val pinned = filtered.filter { it.pinned }
                val others = filtered.filter { !it.pinned }
                TagNotesUiState(
                    tag = tag,
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
            tag: String,
            notesRepository: NotesRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagNotesViewModel(tag, notesRepository, preferencesRepository) as T
            }
        }
    }
}
