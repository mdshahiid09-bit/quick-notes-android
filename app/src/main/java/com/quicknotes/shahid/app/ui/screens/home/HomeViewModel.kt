package com.quicknotes.shahid.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val pinnedNotes: List<NoteEntity> = emptyList(),
    val otherNotes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedNoteIds: Set<String> = emptySet(),
    val viewMode: NotesViewMode = NotesViewMode.GRID,
    val recentlyDeletedNote: NoteEntity? = null,
    val recentlyArchivedNote: NoteEntity? = null
)

private data class FilterState(
    val query: String,
    val isSearch: Boolean,
    val selected: Set<String>,
    val loading: Boolean
)

private data class NotificationState(
    val deleted: NoteEntity?,
    val archived: NoteEntity?
)

class HomeViewModel(
    private val notesRepository: NotesRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _recentlyDeletedNote = MutableStateFlow<NoteEntity?>(null)
    private val _recentlyArchivedNote = MutableStateFlow<NoteEntity?>(null)

    private val _filterState = combine(
        _searchQuery,
        _isSearchActive,
        _selectedNoteIds,
        _isLoading
    ) { query, isSearch, selected, loading ->
        FilterState(query, isSearch, selected, loading)
    }

    private val _notificationState = combine(
        _recentlyDeletedNote,
        _recentlyArchivedNote
    ) { deleted, archived ->
        NotificationState(deleted, archived)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        notesRepository.getActiveNotes(),
        notesRepository.getAllFolders(),
        preferencesRepository.notesViewMode,
        _filterState,
        _notificationState
    ) { activeNotes, folders, viewMode, filter, notif ->
        val filteredNotes = if (filter.query.isBlank()) {
            activeNotes
        } else {
            activeNotes.filter { note ->
                note.title.contains(filter.query, ignoreCase = true) ||
                        note.preview.contains(filter.query, ignoreCase = true) ||
                        note.tags.any { it.contains(filter.query, ignoreCase = true) }
            }
        }

        val pinned = filteredNotes.filter { it.pinned }
        val others = filteredNotes.filter { !it.pinned }

        HomeUiState(
            pinnedNotes = pinned,
            otherNotes = others,
            folders = folders,
            isLoading = filter.loading,
            searchQuery = filter.query,
            isSearchActive = filter.isSearch,
            selectedNoteIds = filter.selected,
            viewMode = viewMode,
            recentlyDeletedNote = notif.deleted,
            recentlyArchivedNote = notif.archived
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            // Enforce minimum 400ms shimmer visible time
            val startTime = System.currentTimeMillis()
            notesRepository.scanStorage()
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 400L) {
                delay(400L - elapsed)
            }
            _isLoading.value = false
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
        viewModelScope.launch {
            val current = uiState.value.viewMode
            val next = if (current == NotesViewMode.GRID) NotesViewMode.LIST else NotesViewMode.GRID
            preferencesRepository.setNotesViewMode(next)
        }
    }

    fun toggleNoteSelection(noteId: String) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        val ids = _selectedNoteIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                notesRepository.moveToTrash(id)
            }
            clearSelection()
        }
    }

    fun pinSelectedNotes(pin: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                notesRepository.setPinned(id, pin)
            }
            clearSelection()
        }
    }

    fun setColorForSelectedNotes(color: String) {
        val ids = _selectedNoteIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                notesRepository.setColor(id, color)
            }
            clearSelection()
        }
    }

    fun moveSelectedNotesToFolder(folderName: String) {
        val ids = _selectedNoteIds.value.toList()
        viewModelScope.launch {
            ids.forEach { id ->
                notesRepository.moveToFolder(id, folderName)
            }
            clearSelection()
        }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch {
            _recentlyDeletedNote.value = note
            notesRepository.moveToTrash(note.id)
        }
    }

    fun undoTrash() {
        val note = _recentlyDeletedNote.value ?: return
        viewModelScope.launch {
            notesRepository.restoreFromTrash(note.id)
            _recentlyDeletedNote.value = null
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            _recentlyArchivedNote.value = note
            notesRepository.setArchived(note.id, true)
        }
    }

    fun undoArchive() {
        val note = _recentlyArchivedNote.value ?: return
        viewModelScope.launch {
            notesRepository.setArchived(note.id, false)
            _recentlyArchivedNote.value = null
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(notesRepository, preferencesRepository) as T
            }
        }
    }
}
