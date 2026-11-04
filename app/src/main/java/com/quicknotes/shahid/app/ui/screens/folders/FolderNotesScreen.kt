package com.quicknotes.shahid.app.ui.screens.folders

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.db.entity.NoteEntity
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.ui.components.EmptyState
import com.quicknotes.shahid.app.ui.components.NoteCard
import com.quicknotes.shahid.app.ui.components.NoteListItem
import com.quicknotes.shahid.app.ui.components.ShimmerNotesGrid
import com.quicknotes.shahid.app.ui.components.ShimmerNotesList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderNotesScreen(
    viewModel: FolderNotesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            if (uiState.isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.notes_search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setSearchActive(false) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    },
                    actions = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search)
                                )
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.folderName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSearchActive(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (uiState.viewMode == NotesViewMode.GRID) {
                                    Icons.Default.ViewList
                                } else {
                                    Icons.Default.GridView
                                },
                                contentDescription = stringResource(R.string.cd_view_mode)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_note_to_folder)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = uiState.isLoading, label = "folderNotesCrossfade") { loading ->
                if (loading) {
                    if (uiState.viewMode == NotesViewMode.GRID) {
                        ShimmerNotesGrid()
                    } else {
                        ShimmerNotesList()
                    }
                } else if (uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty()) {
                    if (uiState.searchQuery.isNotBlank()) {
                        EmptyState(
                            iconRes = R.drawable.ic_empty_search,
                            title = stringResource(R.string.notes_search_empty_title),
                            subtitle = stringResource(R.string.notes_search_empty_subtitle),
                            actionLabel = stringResource(R.string.action_clear),
                            onActionClick = { viewModel.setSearchQuery("") }
                        )
                    } else {
                        EmptyState(
                            iconRes = R.drawable.ic_empty_notes,
                            title = stringResource(R.string.folder_notes_empty_title),
                            subtitle = stringResource(R.string.folder_notes_empty_subtitle, uiState.folderName),
                            actionLabel = stringResource(R.string.folder_notes_create_note, uiState.folderName),
                            onActionClick = { onNavigateToEditor(null) }
                        )
                    }
                } else {
                    if (uiState.viewMode == NotesViewMode.GRID) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.pinnedNotes.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = stringResource(R.string.notes_pinned_header),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(
                                    items = uiState.pinnedNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteCard(
                                        note = note,
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onClick = { onNavigateToEditor(note.id) },
                                        onLongClick = { noteToDelete = note }
                                    )
                                }
                            }

                            if (uiState.otherNotes.isNotEmpty()) {
                                if (uiState.pinnedNotes.isNotEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(
                                            text = stringResource(R.string.notes_all_header),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                                items(
                                    items = uiState.otherNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteCard(
                                        note = note,
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onClick = { onNavigateToEditor(note.id) },
                                        onLongClick = { noteToDelete = note }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (uiState.pinnedNotes.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.notes_pinned_header),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(
                                    items = uiState.pinnedNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteListItem(
                                        note = note,
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onClick = { onNavigateToEditor(note.id) },
                                        onLongClick = { noteToDelete = note },
                                        onSwipeToTrash = { noteToDelete = note },
                                        onSwipeToArchive = {}
                                    )
                                }
                            }

                            if (uiState.otherNotes.isNotEmpty()) {
                                if (uiState.pinnedNotes.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = stringResource(R.string.notes_all_header),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                                items(
                                    items = uiState.otherNotes,
                                    key = { it.id }
                                ) { note ->
                                    NoteListItem(
                                        note = note,
                                        isSelected = false,
                                        isSelectionMode = false,
                                        onClick = { onNavigateToEditor(note.id) },
                                        onLongClick = { noteToDelete = note },
                                        onSwipeToTrash = { noteToDelete = note },
                                        onSwipeToArchive = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for note deletion in folder
    if (noteToDelete != null) {
        val targetNote = noteToDelete
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(text = stringResource(R.string.notes_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.notes_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (targetNote != null) {
                            viewModel.trashNote(targetNote.id)
                        }
                        noteToDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
