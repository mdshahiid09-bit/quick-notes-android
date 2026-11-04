package com.quicknotes.shahid.app.ui.screens.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderWithCount(
    val folder: FolderEntity,
    val noteCount: Int
)

data class FoldersUiState(
    val foldersWithCount: List<FolderWithCount> = emptyList(),
    val isLoading: Boolean = true
)

class FoldersViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<FoldersUiState> = combine(
        notesRepository.getAllFolders(),
        notesRepository.getActiveNotes(),
        _isLoading
    ) { folders, notes, loading ->
        val list = folders.map { folder ->
            val count = notes.count { it.folder == folder.name }
            FolderWithCount(folder, count)
        }
        FoldersUiState(foldersWithCount = list, isLoading = loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FoldersUiState()
    )

    init {
        loadFolders()
    }

    fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            notesRepository.scanStorage()
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 400L) {
                delay(400L - elapsed)
            }
            _isLoading.value = false
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            notesRepository.createFolder(name.trim())
        }
    }

    fun renameFolder(id: String, oldName: String, newName: String) {
        if (newName.isBlank() || oldName == newName) return
        viewModelScope.launch {
            notesRepository.renameFolder(id, oldName, newName.trim())
        }
    }

    fun deleteFolder(id: String, name: String) {
        viewModelScope.launch {
            notesRepository.deleteFolder(id, name)
        }
    }

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FoldersViewModel(notesRepository) as T
            }
        }
    }
}
