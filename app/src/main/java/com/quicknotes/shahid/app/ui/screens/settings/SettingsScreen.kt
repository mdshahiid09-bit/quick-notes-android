package com.quicknotes.shahid.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.data.local.preferences.ThemeMode
import com.quicknotes.shahid.app.data.local.storage.ZipExportImportManager
import com.quicknotes.shahid.app.data.local.vault.VaultRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    vaultRepository: VaultRepository,
    zipManager: ZipExportImportManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDisguiseDialog by remember { mutableStateOf(false) }
    var showChangePasscodeDialog by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    var oldPasscodeInput by remember { mutableStateOf("") }
    var newPasscodeInput by remember { mutableStateOf("") }
    var confirmPasscodeInput by remember { mutableStateOf("") }
    var passcodeError by remember { mutableStateOf<String?>(null) }

    var recoveryPasscodeInput by remember { mutableStateOf("") }
    var revealedRecoveryKey by remember { mutableStateOf<String?>(null) }
    var recoveryError by remember { mutableStateOf<String?>(null) }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val count = zipManager.importNotesFromZip(uri)
                    snackbarHostState.showSnackbar(context.getString(R.string.import_success, count))
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(context.getString(R.string.import_failed))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_system),
                        isSelected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_light),
                        isSelected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_theme_dark),
                        isSelected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = stringResource(R.string.settings_view_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_view_grid),
                        isSelected = uiState.notesViewMode == NotesViewMode.GRID,
                        onClick = { viewModel.setNotesViewMode(NotesViewMode.GRID) }
                    )
                    ThemeOptionRow(
                        label = stringResource(R.string.settings_view_list),
                        isSelected = uiState.notesViewMode == NotesViewMode.LIST,
                        onClick = { viewModel.setNotesViewMode(NotesViewMode.LIST) }
                    )
                }
            }

            SettingsSectionHeader(title = stringResource(R.string.settings_section_storage))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_storage_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Upload,
                        title = stringResource(R.string.settings_export_title),
                        subtitle = stringResource(R.string.settings_export_desc),
                        onClick = {
                            coroutineScope.launch {
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
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.settings_import_title),
                        subtitle = stringResource(R.string.settings_import_desc),
                        onClick = {
                            importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        }
                    )
                }
            }

            SettingsSectionHeader(title = stringResource(R.string.settings_section_vault))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsActionRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.settings_change_passcode),
                        subtitle = "Update the 8+ character passcode encrypting your vault",
                        onClick = {
                            oldPasscodeInput = ""
                            newPasscodeInput = ""
                            confirmPasscodeInput = ""
                            passcodeError = null
                            showChangePasscodeDialog = true
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Key,
                        title = stringResource(R.string.settings_view_recovery),
                        subtitle = "View your 32-character recovery key",
                        onClick = {
                            recoveryPasscodeInput = ""
                            revealedRecoveryKey = null
                            recoveryError = null
                            showRecoveryDialog = true
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SettingsActionRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.settings_lock_now),
                        subtitle = "Immediately lock the vault",
                        onClick = {
                            viewModel.lockVaultNow()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Vault locked")
                            }
                        }
                    )
                }
            }

            SettingsSectionHeader(title = stringResource(R.string.settings_section_disguise))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_disguise_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_disguise_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.isDisguiseEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showDisguiseDialog = true
                            } else {
                                viewModel.setDisguiseMode(context, false)
                            }
                        }
                    )
                }
            }

            SettingsSectionHeader(title = stringResource(R.string.settings_section_about))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version " + stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsActionRow(
                        icon = Icons.Default.OpenInBrowser,
                        title = stringResource(R.string.settings_github_link),
                        subtitle = "https://github.com/mdshahiid09-bit/quick-notes-android",
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/mdshahiid09-bit/quick-notes-android")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showDisguiseDialog) {
        AlertDialog(
            onDismissRequest = { showDisguiseDialog = false },
            title = { Text(text = stringResource(R.string.settings_disguise_dialog_title)) },
            text = { Text(text = stringResource(R.string.settings_disguise_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDisguiseMode(context, true)
                    showDisguiseDialog = false
                }) {
                    Text(text = stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisguiseDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showChangePasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasscodeDialog = false },
            title = { Text(text = stringResource(R.string.settings_change_passcode)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPasscodeInput,
                        onValueChange = {
                            oldPasscodeInput = it
                            passcodeError = null
                        },
                        label = { Text("Current Passcode") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPasscodeInput,
                        onValueChange = {
                            newPasscodeInput = it
                            passcodeError = null
                        },
                        label = { Text("New Passcode (min 8 chars)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPasscodeInput,
                        onValueChange = {
                            confirmPasscodeInput = it
                            passcodeError = null
                        },
                        label = { Text("Confirm New Passcode") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passcodeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = passcodeError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPasscodeInput.length < 8) {
                        passcodeError = "New passcode must be at least 8 characters"
                        return@TextButton
                    }
                    if (newPasscodeInput != confirmPasscodeInput) {
                        passcodeError = "New passcodes do not match"
                        return@TextButton
                    }
                    coroutineScope.launch {
                        val valid = vaultRepository.unlockWithPasscode(oldPasscodeInput)
                        if (valid) {
                            vaultRepository.setupVault(newPasscodeInput)
                            showChangePasscodeDialog = false
                            snackbarHostState.showSnackbar("Passcode updated successfully")
                        } else {
                            passcodeError = "Current passcode is incorrect"
                        }
                    }
                }) {
                    Text(text = stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasscodeDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showRecoveryDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDialog = false },
            title = { Text(text = stringResource(R.string.settings_view_recovery)) },
            text = {
                Column {
                    if (revealedRecoveryKey == null) {
                        Text(
                            text = "Please enter your vault passcode to view your recovery key.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = recoveryPasscodeInput,
                            onValueChange = {
                                recoveryPasscodeInput = it
                                recoveryError = null
                            },
                            label = { Text(stringResource(R.string.vault_passcode_hint)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (recoveryError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = recoveryError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "Your Recovery Key (Keep this safe!):",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = revealedRecoveryKey ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (revealedRecoveryKey == null) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val valid = vaultRepository.unlockWithPasscode(recoveryPasscodeInput)
                            if (valid) {
                                val key = vaultRepository.setupVault(recoveryPasscodeInput)
                                revealedRecoveryKey = key
                            } else {
                                recoveryError = "Incorrect passcode"
                            }
                        }
                    }) {
                        Text(text = stringResource(R.string.action_confirm))
                    }
                } else {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text(text = stringResource(R.string.action_close))
                    }
                }
            },
            dismissButton = {
                if (revealedRecoveryKey == null) {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
