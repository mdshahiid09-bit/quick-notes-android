package com.quicknotes.shahid.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.data.local.storage.ZipExportImportManager
import com.quicknotes.shahid.app.ui.components.EmptyState
import com.quicknotes.shahid.app.ui.components.NoteCard
import com.quicknotes.shahid.app.ui.components.NoteListItem
import com.quicknotes.shahid.app.ui.components.ShimmerNotesGrid
import com.quicknotes.shahid.app.ui.components.ShimmerNotesList
import com.quicknotes.shahid.app.ui.navigation.Screen
import com.quicknotes.shahid.app.ui.theme.NoteAmberLight
import com.quicknotes.shahid.app.ui.theme.NoteEmeraldLight
import com.quicknotes.shahid.app.ui.theme.NoteIndigoLight
import com.quicknotes.shahid.app.ui.theme.NoteRoseLight
import com.quicknotes.shahid.app.ui.theme.NoteTealLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    zipManager: ZipExportImportManager,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showColorDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val count = zipManager.importNotesFromZip(uri)
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.import_success, count)
                    )
                    viewModel.loadNotes()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.import_failed)
                    )
                }
            }
        }
    }

    // Handle delete undo snackbar
    LaunchedEffect(uiState.recentlyDeletedNote) {
        val note = uiState.recentlyDeletedNote
        if (note != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.notes_moved_to_trash),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoTrash()
            }
        }
    }

    // Handle archive undo snackbar
    LaunchedEffect(uiState.recentlyArchivedNote) {
        val note = uiState.recentlyArchivedNote
        if (note != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.notes_archived),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoArchive()
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawer(
                noteCount = uiState.pinnedNotes.size + uiState.otherNotes.size,
                onNavigateAllNotes = {
                    coroutineScope.launch { drawerState.close() }
                },
                onNavigateFolders = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToFolders()
                    }
                },
                onNavigateTags = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToTags()
                    }
                },
                onNavigateArchive = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToArchive()
                    }
                },
                onNavigateTrash = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToTrash()
                    }
                },
                onExportNotes = {
                    coroutineScope.launch {
                        drawerState.close()
                        try {
                            val (_, uri) = zipManager.exportNotesToZip()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, context.getString(R.string.drawer_export))
                            )
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(context.getString(R.string.export_failed))
                        }
                    }
                },
                onImportNotes = {
                    coroutineScope.launch {
                        drawerState.close()
                        importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                    }
                },
                onToggleTheme = {
                    coroutineScope.launch {
                        drawerState.close()
                        onNavigateToSettings()
                    }
                },
                onShowAbout = {
                    coroutineScope.launch {
                        drawerState.close()
                        showAboutDialog = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (uiState.selectedNoteIds.isNotEmpty()) {
                    // Selection Mode Top Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.notes_selected_count, uiState.selectedNoteIds.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_dismiss)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.pinSelectedNotes(true) }) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = stringResource(R.string.action_pin)
                                )
                            }
                            IconButton(onClick = { showColorDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.ColorLens,
                                    contentDescription = stringResource(R.string.toolbar_highlight)
                                )
                            }
                            IconButton(onClick = { showFolderDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileMove,
                                    contentDescription = stringResource(R.string.drawer_folders)
                                )
                            }
                            IconButton(onClick = { viewModel.deleteSelectedNotes() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                } else if (uiState.isSearchActive) {
                    // Expanded Search Bar
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
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.action_close)
                                )
                            }
                        },
                        actions = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.action_clear)
                                    )
                                }
                            }
                        }
                    )
                } else {
                    // Normal Top Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.cd_menu)
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
                        contentDescription = stringResource(R.string.cd_add_note)
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
                Crossfade(
                    targetState = uiState.isLoading,
                    label = "homeContentCrossfade"
                ) { loading ->
                    if (loading) {
                        if (uiState.viewMode == NotesViewMode.GRID) {
                            ShimmerNotesGrid()
                        } else {
                            ShimmerNotesList()
                        }
                    } else {
                        val isEmpty = uiState.pinnedNotes.isEmpty() && uiState.otherNotes.isEmpty()

                        if (isEmpty) {
                            if (uiState.searchQuery.isNotEmpty()) {
                                EmptyState(
                                    iconRes = R.drawable.ic_empty_search,
                                    title = stringResource(R.string.notes_search_empty_title),
                                    subtitle = stringResource(R.string.notes_search_empty_subtitle)
                                )
                            } else {
                                EmptyState(
                                    iconRes = R.drawable.ic_empty_notes,
                                    title = stringResource(R.string.notes_empty_title),
                                    subtitle = stringResource(R.string.notes_empty_subtitle),
                                    actionLabel = stringResource(R.string.cd_add_note),
                                    onActionClick = { onNavigateToEditor(null) }
                                )
                            }
                        } else {
                            if (uiState.viewMode == NotesViewMode.GRID) {
                                // Grid View
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
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(
                                            items = uiState.pinnedNotes,
                                            key = { it.id }
                                        ) { note ->
                                            NoteCard(
                                                note = note,
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                isSelectionMode = uiState.selectedNoteIds.isNotEmpty(),
                                                onClick = {
                                                    if (uiState.selectedNoteIds.isNotEmpty()) {
                                                        viewModel.toggleNoteSelection(note.id)
                                                    } else {
                                                        onNavigateToEditor(note.id)
                                                    }
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                                modifier = Modifier.animateItem()
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
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                isSelectionMode = uiState.selectedNoteIds.isNotEmpty(),
                                                onClick = {
                                                    if (uiState.selectedNoteIds.isNotEmpty()) {
                                                        viewModel.toggleNoteSelection(note.id)
                                                    } else {
                                                        onNavigateToEditor(note.id)
                                                    }
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            } else {
                                // List View
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
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        items(
                                            items = uiState.pinnedNotes,
                                            key = { it.id }
                                        ) { note ->
                                            NoteListItem(
                                                note = note,
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                isSelectionMode = uiState.selectedNoteIds.isNotEmpty(),
                                                onClick = {
                                                    if (uiState.selectedNoteIds.isNotEmpty()) {
                                                        viewModel.toggleNoteSelection(note.id)
                                                    } else {
                                                        onNavigateToEditor(note.id)
                                                    }
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                                onSwipeToTrash = { viewModel.trashNote(note) },
                                                onSwipeToArchive = { viewModel.archiveNote(note) },
                                                modifier = Modifier.animateItem()
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
                                                isSelected = uiState.selectedNoteIds.contains(note.id),
                                                isSelectionMode = uiState.selectedNoteIds.isNotEmpty(),
                                                onClick = {
                                                    if (uiState.selectedNoteIds.isNotEmpty()) {
                                                        viewModel.toggleNoteSelection(note.id)
                                                    } else {
                                                        onNavigateToEditor(note.id)
                                                    }
                                                },
                                                onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                                onSwipeToTrash = { viewModel.trashNote(note) },
                                                onSwipeToArchive = { viewModel.archiveNote(note) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Color Selector Dialog for selection mode
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text(text = stringResource(R.string.toolbar_highlight)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val colors = listOf(
                        "default" to MaterialTheme.colorScheme.surfaceVariant,
                        "teal" to NoteTealLight,
                        "amber" to NoteAmberLight,
                        "rose" to NoteRoseLight,
                        "indigo" to NoteIndigoLight,
                        "emerald" to NoteEmeraldLight
                    )
                    colors.forEach { (colorKey, colorVal) ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorVal)
                                .clickable {
                                    viewModel.setColorForSelectedNotes(colorKey)
                                    showColorDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Move to folder dialog for selection mode
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text(text = stringResource(R.string.folders_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.drawer_all_notes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.moveSelectedNotesToFolder("")
                                showFolderDialog = false
                            }
                            .padding(vertical = 12.dp)
                    )
                    uiState.folders.forEach { folder ->
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.moveSelectedNotesToFolder(folder.name)
                                    showFolderDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = stringResource(R.string.drawer_about)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version " + stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }
}
