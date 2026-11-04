package com.quicknotes.shahid.app.ui.screens.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.vault.model.DecryptedApiKey
import com.quicknotes.shahid.app.data.local.vault.model.VaultState
import com.quicknotes.shahid.app.ui.components.CheckmarkMorphIcon
import com.quicknotes.shahid.app.ui.components.EmptyState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToRecovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(uiState.showWrongPasscodeShake) {
        if (uiState.showWrongPasscodeShake) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -25f at 50
                    25f at 100
                    -20f at 150
                    20f at 200
                    -10f at 250
                    10f at 300
                    0f at 400
                }
            )
        }
    }

    var showAddKeyDialog by remember { mutableStateOf(false) }
    var keyToEdit by remember { mutableStateOf<DecryptedApiKey?>(null) }
    var keyToDelete by remember { mutableStateOf<DecryptedApiKey?>(null) }

    var providerInput by remember { mutableStateOf("") }
    var keyInput by remember { mutableStateOf("") }

    val visibleKeyIds = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.vault_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (uiState.vaultState is VaultState.Unlocked) {
                        IconButton(onClick = { viewModel.lockVault() }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.action_lock_now)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.vaultState is VaultState.Unlocked) {
                FloatingActionButton(
                    onClick = {
                        providerInput = ""
                        keyInput = ""
                        showAddKeyDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_key)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState.vaultState) {
                is VaultState.NotConfigured -> {
                    LaunchedEffect(Unit) {
                        onNavigateToSetup()
                    }
                }
                is VaultState.Locked -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterVertically,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.cd_vault_lock),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(68.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = stringResource(R.string.vault_locked_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.vault_locked_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        val isLockedOut = state.lockoutRemainingSeconds > 0

                        if (isLockedOut) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.vault_locked_out, state.lockoutRemainingSeconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.passcode,
                                onValueChange = { viewModel.onPasscodeChange(it) },
                                label = { Text(stringResource(R.string.vault_passcode_hint)) },
                                singleLine = true,
                                visualTransformation = if (uiState.isPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { viewModel.unlock() }),
                                trailingIcon = {
                                    IconButton(onClick = { viewModel.togglePasscodeVisibility() }) {
                                        Icon(
                                            imageVector = if (uiState.isPasscodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = stringResource(R.string.cd_toggle_visibility)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(x = shakeOffset.value.roundToInt(), y = 0) }
                            )

                            if (state.errorMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.vault_wrong_passcode),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.unlock() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = stringResource(R.string.action_unlock))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = onNavigateToRecovery) {
                            Text(
                                text = stringResource(R.string.vault_forgot_passcode),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                is VaultState.Unlocked -> {
                    if (state.keys.isEmpty()) {
                        EmptyState(
                            iconRes = R.drawable.ic_empty_vault,
                            title = stringResource(R.string.vault_empty_title),
                            subtitle = stringResource(R.string.vault_empty_subtitle),
                            actionLabel = stringResource(R.string.cd_add_key),
                            onActionClick = {
                                providerInput = ""
                                keyInput = ""
                                showAddKeyDialog = true
                            }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = state.keys,
                                key = { it.id }
                            ) { keyEntry ->
                                val isVisible = visibleKeyIds[keyEntry.id] ?: false
                                val isCopied = uiState.copiedKeyId == keyEntry.id

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = keyEntry.providerName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = {
                                                    keyToEdit = keyEntry
                                                    providerInput = keyEntry.providerName
                                                    keyInput = keyEntry.plainKey
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = stringResource(R.string.action_edit),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(onClick = { keyToDelete = keyEntry }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = stringResource(R.string.action_delete),
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = if (isVisible) keyEntry.plainKey else "••••••••••••••••••••••••",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = {
                                                        visibleKeyIds[keyEntry.id] = !isVisible
                                                    }) {
                                                        Icon(
                                                            imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = stringResource(R.string.cd_toggle_visibility),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    IconButton(onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.copyKey(keyEntry)
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                context.getString(R.string.vault_copied_snackbar)
                                                            )
                                                        }
                                                    }) {
                                                        CheckmarkMorphIcon(isCopied = isCopied)
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
            }
        }
    }

    if (showAddKeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddKeyDialog = false },
            title = { Text(text = stringResource(R.string.vault_add_key_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = providerInput,
                        onValueChange = { providerInput = it },
                        label = { Text(stringResource(R.string.vault_provider_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val suggestions = listOf("Gemini", "Groq", "OpenCode", "OpenRouter", "Custom")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { providerInput = suggestion }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(stringResource(R.string.vault_key_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (providerInput.isNotBlank() && keyInput.isNotBlank()) {
                        viewModel.addApiKey(providerInput, keyInput) {
                            showAddKeyDialog = false
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeyDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    keyToEdit?.let { editEntry ->
        AlertDialog(
            onDismissRequest = { keyToEdit = null },
            title = { Text(text = stringResource(R.string.vault_edit_key_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = providerInput,
                        onValueChange = { providerInput = it },
                        label = { Text(stringResource(R.string.vault_provider_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(stringResource(R.string.vault_key_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (providerInput.isNotBlank() && keyInput.isNotBlank()) {
                        viewModel.updateApiKey(editEntry.id, providerInput, keyInput) {
                            keyToEdit = null
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToEdit = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    keyToDelete?.let { delEntry ->
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = { Text(text = stringResource(R.string.vault_delete_key_confirm_title)) },
            text = { Text(text = stringResource(R.string.vault_delete_key_confirm_message, delEntry.providerName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteApiKey(delEntry.id)
                    keyToDelete = null
                }) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
