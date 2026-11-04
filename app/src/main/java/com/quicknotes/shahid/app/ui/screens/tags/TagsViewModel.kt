package com.quicknotes.shahid.app.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TagItem(
    val name: String,
    val noteCount: Int
)

data class TagsUiState(
    val tags: List<TagItem> = emptyList(),
    val isLoading: Boolean = false
)

class TagsViewModel(
    notesRepository: NotesRepository
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = notesRepository.getAllTagsWithCounts()
        .map { map ->
            val list = map.map { TagItem(it.key, it.value) }.sortedByDescending { it.noteCount }
            TagsUiState(tags = list, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TagsUiState(isLoading = true)
        )

    companion object {
        fun provideFactory(
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagsViewModel(notesRepository) as T
            }
        }
    }
}
