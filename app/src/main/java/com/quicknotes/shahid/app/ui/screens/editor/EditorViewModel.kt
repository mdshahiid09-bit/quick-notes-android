package com.quicknotes.shahid.app.ui.screens.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.db.entity.FolderEntity
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.repository.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val body: TextFieldValue = TextFieldValue(""),
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val color: String = "default",
    val filePath: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = true,
    val isPreviewMode: Boolean = false,
    val isFindReplaceActive: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val findMatches: List<IntRange> = emptyList(),
    val currentMatchIndex: Int = 0,
    val triggerConfetti: Boolean = false,
    val availableFolders: List<FolderEntity> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

class EditorViewModel(
    private val noteIdArg: String?,
    private val initialFolder: String? = null,
    private val initialTitle: String? = null,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditorUiState(
            noteId = noteIdArg,
            title = initialTitle ?: "",
            folder = initialFolder ?: ""
        )
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var autosaveJob: Job? = null

    init {
        loadNote()
        loadFolders()
    }

    private fun loadFolders() {
        viewModelScope.launch {
            notesRepository.getAllFolders().collect { folders ->
                _uiState.value = _uiState.value.copy(availableFolders = folders)
            }
        }
    }

    private fun loadNote() {
        if (noteIdArg == null) return
        viewModelScope.launch {
            val note = notesRepository.getNoteById(noteIdArg)
            if (note != null) {
                val content = notesRepository.readNoteContent(note.filePath)
                val tfv = TextFieldValue(content, TextRange(content.length))
                _uiState.value = _uiState.value.copy(
                    noteId = note.id,
                    title = note.title,
                    body = tfv,
                    folder = note.folder,
                    tags = note.tags,
                    pinned = note.pinned,
                    color = note.color,
                    filePath = note.filePath,
                    isSaved = true
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.value = _uiState.value.copy(
            title = newTitle,
            isSaved = false
        )
        scheduleAutosave()
    }

    fun onBodyChange(newBody: TextFieldValue) {
        val oldBody = _uiState.value.body

        // Check if a checklist task was newly marked completed: [ ] -> [x] or [X]
        if (oldBody.text.contains("- [ ]") && (newBody.text.contains("- [x]") || newBody.text.contains("- [X]"))) {
            _uiState.value = _uiState.value.copy(triggerConfetti = true)
        }

        // Push to undo stack if text changed meaningfully
        if (oldBody.text != newBody.text) {
            undoStack.add(oldBody)
            redoStack.clear()
        }

        _uiState.value = _uiState.value.copy(
            body = newBody,
            isSaved = false,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )

        if (_uiState.value.isFindReplaceActive && _uiState.value.findQuery.isNotEmpty()) {
            calculateFindMatches(_uiState.value.findQuery, newBody.text)
        }

        scheduleAutosave()
    }

    fun onConfettiHandled() {
        _uiState.value = _uiState.value.copy(triggerConfetti = false)
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _uiState.value.body
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(current)
            _uiState.value = _uiState.value.copy(
                body = previous,
                isSaved = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = true
            )
            scheduleAutosave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _uiState.value.body
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(current)
            _uiState.value = _uiState.value.copy(
                body = next,
                isSaved = false,
                canUndo = true,
                canRedo = redoStack.isNotEmpty()
            )
            scheduleAutosave()
        }
    }

    fun togglePinned() {
        val newPinned = !_uiState.value.pinned
        _uiState.value = _uiState.value.copy(pinned = newPinned, isSaved = false)
        scheduleAutosave()
    }

    fun setFolder(folder: String) {
        _uiState.value = _uiState.value.copy(folder = folder, isSaved = false)
        scheduleAutosave()
    }

    fun setColor(color: String) {
        _uiState.value = _uiState.value.copy(color = color, isSaved = false)
        scheduleAutosave()
    }

    fun setTags(tags: List<String>) {
        _uiState.value = _uiState.value.copy(tags = tags, isSaved = false)
        scheduleAutosave()
    }

    fun togglePreviewMode() {
        _uiState.value = _uiState.value.copy(isPreviewMode = !_uiState.value.isPreviewMode)
    }

    fun toggleFindReplace() {
        val active = !_uiState.value.isFindReplaceActive
        _uiState.value = _uiState.value.copy(
            isFindReplaceActive = active,
            findQuery = "",
            replaceQuery = "",
            findMatches = emptyList(),
            currentMatchIndex = 0
        )
    }

    fun onFindQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(findQuery = query)
        calculateFindMatches(query, _uiState.value.body.text)
    }

    fun onReplaceQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(replaceQuery = query)
    }

    private fun calculateFindMatches(query: String, text: String) {
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(findMatches = emptyList(), currentMatchIndex = 0)
            return
        }

        val matches = mutableListOf<IntRange>()
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index >= 0) {
            matches.add(index until (index + query.length))
            index = text.indexOf(query, index + query.length.coerceAtLeast(1), ignoreCase = true)
        }

        val currentIndex = if (matches.isNotEmpty()) 0 else 0
        _uiState.value = _uiState.value.copy(
            findMatches = matches,
            currentMatchIndex = currentIndex
        )

        if (matches.isNotEmpty()) {
            val range = matches[0]
            _uiState.value = _uiState.value.copy(
                body = _uiState.value.body.copy(selection = TextRange(range.first, range.last + 1))
            )
        }
    }

    fun nextMatch() {
        val matches = _uiState.value.findMatches
        if (matches.isEmpty()) return
        val nextIndex = (_uiState.value.currentMatchIndex + 1) % matches.size
        val range = matches[nextIndex]
        _uiState.value = _uiState.value.copy(
            currentMatchIndex = nextIndex,
            body = _uiState.value.body.copy(selection = TextRange(range.first, range.last + 1))
        )
    }

    fun prevMatch() {
        val matches = _uiState.value.findMatches
        if (matches.isEmpty()) return
        val prevIndex = if (_uiState.value.currentMatchIndex - 1 < 0) matches.size - 1 else _uiState.value.currentMatchIndex - 1
        val range = matches[prevIndex]
        _uiState.value = _uiState.value.copy(
            currentMatchIndex = prevIndex,
            body = _uiState.value.body.copy(selection = TextRange(range.first, range.last + 1))
        )
    }

    fun replaceCurrentMatch() {
        val state = _uiState.value
        if (state.findMatches.isEmpty()) return
        val range = state.findMatches[state.currentMatchIndex]
        val text = state.body.text
        val newText = text.replaceRange(range.first, range.last + 1, state.replaceQuery)

        onBodyChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(range.first + state.replaceQuery.length)
            )
        )
    }

    fun replaceAllMatches() {
        val state = _uiState.value
        if (state.findQuery.isEmpty()) return
        val newText = state.body.text.replace(state.findQuery, state.replaceQuery, ignoreCase = true)
        onBodyChange(TextFieldValue(newText))
    }

    // Markdown Toolbar Operations
    fun applySurrounding(prefix: String, suffix: String) {
        val tfv = _uiState.value.body
        val selection = tfv.selection
        val text = tfv.text

        val newText: String
        val newSelection: TextRange

        if (selection.collapsed) {
            newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
            newSelection = TextRange(selection.start + prefix.length)
        } else {
            val selectedText = text.substring(selection.start, selection.end)
            newText = text.substring(0, selection.start) + prefix + selectedText + suffix + text.substring(selection.end)
            newSelection = TextRange(selection.start + prefix.length, selection.end + prefix.length)
        }

        onBodyChange(TextFieldValue(newText, newSelection))
    }

    fun applyLinePrefix(prefix: String) {
        val tfv = _uiState.value.body
        val text = tfv.text
        val selection = tfv.selection

        // Find line start
        val lineStart = text.lastIndexOf('\n', selection.start - 1).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursor = selection.start + prefix.length

        onBodyChange(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun insertLink(linkText: String, url: String) {
        val tfv = _uiState.value.body
        val selection = tfv.selection
        val markdownLink = "[$linkText]($url)"
        val newText = tfv.text.replaceRange(selection.start, selection.end, markdownLink)
        onBodyChange(TextFieldValue(newText, TextRange(selection.start + markdownLink.length)))
    }

    fun insertCodeBlock() {
        val tfv = _uiState.value.body
        val selection = tfv.selection
        val codeSnippet = "```\n\n```"
        val newText = tfv.text.replaceRange(selection.start, selection.end, codeSnippet)
        onBodyChange(TextFieldValue(newText, TextRange(selection.start + 4)))
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            delay(1000L) // 1 second of inactivity
            saveNow()
        }
    }

    suspend fun saveNow() {
        val state = _uiState.value
        // Avoid creating blank files if both title and body are empty
        if (state.title.isBlank() && state.body.text.isBlank()) {
            _uiState.value = state.copy(isSaving = false, isSaved = true)
            return
        }

        try {
            val savedEntity = notesRepository.saveNote(
                existingId = state.noteId,
                title = state.title,
                body = state.body.text,
                folder = state.folder,
                tags = state.tags,
                pinned = state.pinned,
                color = state.color
            )
            _uiState.value = state.copy(
                noteId = savedEntity.id,
                filePath = savedEntity.filePath,
                isSaving = false,
                isSaved = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = state.copy(isSaving = false)
        }
    }

    fun deleteNote() {
        val id = _uiState.value.noteId ?: return
        viewModelScope.launch {
            notesRepository.moveToTrash(id)
        }
    }

    companion object {
        fun provideFactory(
            noteId: String?,
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = provideFactory(noteId, null, null, notesRepository)

        fun provideFactory(
            noteId: String?,
            folder: String? = null,
            initialTitle: String? = null,
            notesRepository: NotesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return EditorViewModel(noteId, folder, initialTitle, notesRepository) as T
            }
        }
    }
}