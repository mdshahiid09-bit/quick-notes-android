package com.quicknotes.shahid.app.ui.screens.editor

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.storage.DocumentDownloadManager
import com.quicknotes.shahid.app.ui.components.ParticleExplosion
import com.quicknotes.shahid.app.ui.theme.NoteAmberLight
import com.quicknotes.shahid.app.ui.theme.NoteEmeraldLight
import com.quicknotes.shahid.app.ui.theme.NoteIndigoLight
import com.quicknotes.shahid.app.ui.theme.NoteRoseLight
import com.quicknotes.shahid.app.ui.theme.NoteTealLight
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MarkdownEditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    downloadManager: DocumentDownloadManager? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var linkTitleInput by remember { mutableStateOf("") }
    var linkUrlInput by remember { mutableStateOf("") }

    fun handleBack() {
        coroutineScope.launch {
            viewModel.saveNow()
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    val bodyText = uiState.body.text
    val wordCount = if (bodyText.isBlank()) 0 else bodyText.trim().split(Regex("\\s+")).size
    val charCount = bodyText.length
    val readingTimeMin = (wordCount / 200).coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            BasicTextField(
                                value = uiState.title,
                                onValueChange = { viewModel.onTitleChange(it) },
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (uiState.title.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.editor_title_hint),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uiState.isSaving) {
                                        stringResource(R.string.editor_status_saving)
                                    } else {
                                        stringResource(R.string.editor_status_saved)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isSaving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                if (uiState.folder.isNotEmpty()) {
                                    Text(
                                        text = " · ${uiState.folder}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { handleBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    },
                    actions = {
                        // Toggle Edit / Preview
                        IconButton(onClick = { viewModel.togglePreviewMode() }) {
                            Icon(
                                imageVector = if (uiState.isPreviewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = stringResource(
                                    if (uiState.isPreviewMode) R.string.editor_mode_edit else R.string.editor_mode_preview
                                )
                            )
                        }

                        // Pin
                        IconButton(onClick = { viewModel.togglePinned() }) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = stringResource(R.string.cd_pin_note),
                                tint = if (uiState.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }

                        // Download document directly to Downloads
                        IconButton(onClick = {
                            coroutineScope.launch {
                                viewModel.saveNow()
                                val path = uiState.filePath
                                if (path.isNotEmpty() && downloadManager != null) {
                                    val result = downloadManager.downloadNoteToDownloads(path, uiState.title.ifBlank { null })
                                    if (result.isSuccess) {
                                        val fileName = result.getOrNull()?.name ?: ""
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.download_file_success, fileName)
                                        )
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.download_failed)
                                        )
                                    }
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.cd_download_file)
                            )
                        }

                        // More Menu
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_download)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = stringResource(R.string.action_download)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        viewModel.saveNow()
                                        val path = uiState.filePath
                                        if (path.isNotEmpty() && downloadManager != null) {
                                            val result = downloadManager.downloadNoteToDownloads(path, uiState.title.ifBlank { null })
                                            if (result.isSuccess) {
                                                val fileName = result.getOrNull()?.name ?: ""
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.download_file_success, fileName)
                                                )
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.download_failed)
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_share)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.action_share)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    coroutineScope.launch {
                                        viewModel.saveNow()
                                        val path = uiState.filePath
                                        if (path.isNotEmpty()) {
                                            val file = File(path)
                                            if (file.exists()) {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/markdown"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(intent, context.getString(R.string.action_share))
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.folders_title)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = stringResource(R.string.folders_title)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showFolderDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.tags_title)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.tags_title)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showTagDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.toolbar_highlight)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ColorLens,
                                        contentDescription = stringResource(R.string.toolbar_highlight)
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showColorDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.action_delete)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                ) {
                    // Find and Replace panel
                    AnimatedVisibility(visible = uiState.isFindReplaceActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.findQuery,
                                        onValueChange = { viewModel.onFindQueryChange(it) },
                                        placeholder = { Text(stringResource(R.string.editor_find_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (uiState.findMatches.isEmpty()) {
                                            stringResource(R.string.editor_find_no_matches)
                                        } else {
                                            stringResource(
                                                R.string.editor_find_matches,
                                                uiState.currentMatchIndex + 1,
                                                uiState.findMatches.size
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    IconButton(onClick = { viewModel.prevMatch() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(onClick = { viewModel.nextMatch() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.replaceQuery,
                                        onValueChange = { viewModel.onReplaceQueryChange(it) },
                                        placeholder = { Text(stringResource(R.string.editor_replace_placeholder)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(onClick = { viewModel.replaceCurrentMatch() }) {
                                        Text(stringResource(R.string.editor_replace_action))
                                    }
                                    TextButton(onClick = { viewModel.replaceAllMatches() }) {
                                        Text(stringResource(R.string.editor_replace_all_action))
                                    }
                                }
                            }
                        }
                    }

                    // Markdown Toolbar (visible in Edit mode)
                    if (!uiState.isPreviewMode) {
                        MarkdownToolbar(
                            canUndo = uiState.canUndo,
                            canRedo = uiState.canRedo,
                            onUndo = { viewModel.undo() },
                            onRedo = { viewModel.redo() },
                            onBold = { viewModel.applySurrounding("**", "**") },
                            onItalic = { viewModel.applySurrounding("*", "*") },
                            onUnderline = { viewModel.applySurrounding("<u>", "</u>") },
                            onStrikethrough = { viewModel.applySurrounding("~~", "~~") },
                            onH1 = { viewModel.applyLinePrefix("# ") },
                            onH2 = { viewModel.applyLinePrefix("## ") },
                            onH3 = { viewModel.applyLinePrefix("### ") },
                            onBulletList = { viewModel.applyLinePrefix("- ") },
                            onNumberedList = { viewModel.applyLinePrefix("1. ") },
                            onChecklist = { viewModel.applyLinePrefix("- [ ] ") },
                            onCodeBlock = { viewModel.insertCodeBlock() },
                            onLink = { showLinkDialog = true },
                            onHighlight = { viewModel.applySurrounding("==", "==") },
                            onFindReplace = { viewModel.toggleFindReplace() }
                        )
                    }

                    // Word count & info bottom strip
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stringResource(R.string.editor_stats_words, wordCount)} · ${stringResource(R.string.editor_stats_chars, charCount)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.editor_stats_reading_time, readingTimeMin),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.isPreviewMode) {
                    // Rich Preview Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        RenderMarkdownPreview(
                            content = uiState.body.text,
                            onToggleTask = { lineIndex, currentLine ->
                                val updatedLine = if (currentLine.contains("- [ ]")) {
                                    currentLine.replace("- [ ]", "- [x]")
                                } else {
                                    currentLine.replace(Regex("- \\[[xX]\\]"), "- [ ]")
                                }
                                val lines = uiState.body.text.lines().toMutableList()
                                if (lineIndex in lines.indices) {
                                    lines[lineIndex] = updatedLine
                                    val newText = lines.joinToString("\n")
                                    viewModel.onBodyChange(TextFieldValue(newText))
                                }
                            }
                        )
                    }
                } else {
                    // Edit Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        // Tag chips row
                        if (uiState.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                uiState.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Live Markdown Editor
                        BasicTextField(
                            value = uiState.body,
                            onValueChange = { viewModel.onBodyChange(it) },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Default,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            decorationBox = { innerTextField ->
                                if (uiState.body.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.editor_body_hint),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        }

        // Particle explosion on checklist completion
        ParticleExplosion(
            trigger = uiState.triggerConfetti,
            onAnimationEnd = { viewModel.onConfettiHandled() }
        )
    }

    // Link Dialog
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text(text = stringResource(R.string.dialog_link_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = linkTitleInput,
                        onValueChange = { linkTitleInput = it },
                        label = { Text(stringResource(R.string.dialog_link_text)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = linkUrlInput,
                        onValueChange = { linkUrlInput = it },
                        label = { Text(stringResource(R.string.dialog_link_url)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linkTitleInput.isNotEmpty() || linkUrlInput.isNotEmpty()) {
                        viewModel.insertLink(linkTitleInput.ifEmpty { "Link" }, linkUrlInput)
                        linkTitleInput = ""
                        linkUrlInput = ""
                    }
                    showLinkDialog = false
                }) {
                    Text(text = stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Color Dialog
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
                                    viewModel.setColor(colorKey)
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

    // Folder Dialog
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
                                viewModel.setFolder("")
                                showFolderDialog = false
                            }
                            .padding(vertical = 10.dp)
                    )
                    uiState.availableFolders.forEach { folder ->
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFolder(folder.name)
                                    showFolderDialog = false
                                }
                                .padding(vertical = 10.dp)
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

    // Tag Editor Dialog
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text(text = stringResource(R.string.tags_title)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            label = { Text(stringResource(R.string.tags_add_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            val clean = newTagInput.trim().removePrefix("#")
                            if (clean.isNotEmpty() && !uiState.tags.contains(clean)) {
                                viewModel.setTags(uiState.tags + clean)
                                newTagInput = ""
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable {
                                                viewModel.setTags(uiState.tags - tag)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text(text = stringResource(R.string.action_close))
                }
            }
        )
    }

    // Delete Note Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = stringResource(R.string.notes_delete_confirm_title)) },
            text = { Text(text = stringResource(R.string.notes_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteNote()
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun RenderMarkdownPreview(
    content: String,
    onToggleTask: (Int, String) -> Unit
) {
    val lines = content.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                // End code block
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Text(
                        text = codeBlockLines.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                codeBlockLines.clear()
                inCodeBlock = false
            } else {
                inCodeBlock = true
            }
            return@forEachIndexed
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            return@forEachIndexed
        }

        // Checklist items
        val isTaskUnchecked = trimmed.startsWith("- [ ]")
        val isTaskChecked = trimmed.startsWith("- [x]", ignoreCase = true)

        if (isTaskUnchecked || isTaskChecked) {
            val taskText = trimmed.replace(Regex("^-\\s*\\[[\\sxX]\\]\\s*"), "")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTask(index, line) }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isTaskChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isTaskChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = taskText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isTaskChecked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isTaskChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            return@forEachIndexed
        }

        // Headings
        when {
            trimmed.startsWith("# ") -> {
                Text(
                    text = trimmed.removePrefix("# "),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                )
            }
            trimmed.startsWith("## ") -> {
                Text(
                    text = trimmed.removePrefix("## "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
            }
            trimmed.startsWith("### ") -> {
                Text(
                    text = trimmed.removePrefix("### "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(text = "• ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = trimmed.substring(2), style = MaterialTheme.typography.bodyLarge)
                }
            }
            trimmed.startsWith("> ") -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trimmed.removePrefix("> "),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trimmed.isEmpty() -> {
                Spacer(modifier = Modifier.height(10.dp))
            }
            else -> {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}