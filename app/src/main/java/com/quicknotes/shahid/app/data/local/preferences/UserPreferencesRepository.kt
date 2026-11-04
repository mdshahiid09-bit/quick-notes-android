package com.quicknotes.shahid.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quick_notes_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class NotesViewMode {
    GRID, LIST
}

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val KEY_VAULT_SETUP_COMPLETED = booleanPreferencesKey("is_vault_setup_completed")
        val KEY_NOTES_VIEW_MODE = stringPreferencesKey("notes_view_mode")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DISGUISE_ENABLED = booleanPreferencesKey("is_disguise_enabled")

        val KEY_VAULT_PASSCODE_HASH = stringPreferencesKey("vault_passcode_hash")
        val KEY_VAULT_PASSCODE_SALT = stringPreferencesKey("vault_passcode_salt")
        val KEY_VAULT_RECOVERY_HASH = stringPreferencesKey("vault_recovery_hash")
        val KEY_VAULT_RECOVERY_SALT = stringPreferencesKey("vault_recovery_salt")
        val KEY_VAULT_FAILED_ATTEMPTS = intPreferencesKey("vault_failed_attempts")
        val KEY_VAULT_LOCKOUT_UNTIL = longPreferencesKey("vault_lockout_until")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    val isVaultSetupCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_VAULT_SETUP_COMPLETED] ?: false
    }

    suspend fun setVaultSetupCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_VAULT_SETUP_COMPLETED] = completed
        }
    }

    val notesViewMode: Flow<NotesViewMode> = dataStore.data.map { prefs ->
        val mode = prefs[KEY_NOTES_VIEW_MODE] ?: NotesViewMode.GRID.name
        try {
            NotesViewMode.valueOf(mode)
        } catch (e: Exception) {
            NotesViewMode.GRID
        }
    }

    suspend fun setNotesViewMode(mode: NotesViewMode) {
        dataStore.edit { prefs ->
            prefs[KEY_NOTES_VIEW_MODE] = mode.name
        }
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val theme = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(theme)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    val isDisguiseEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DISGUISE_ENABLED] ?: false
    }

    suspend fun setDisguiseEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_DISGUISE_ENABLED] = enabled
        }
    }

    suspend fun getVaultSecurityInfo(): VaultSecurityInfo {
        var info = VaultSecurityInfo()
        dataStore.edit { prefs ->
            info = VaultSecurityInfo(
                passcodeHash = prefs[KEY_VAULT_PASSCODE_HASH],
                passcodeSalt = prefs[KEY_VAULT_PASSCODE_SALT],
                recoveryHash = prefs[KEY_VAULT_RECOVERY_HASH],
                recoverySalt = prefs[KEY_VAULT_RECOVERY_SALT],
                failedAttempts = prefs[KEY_VAULT_FAILED_ATTEMPTS] ?: 0,
                lockoutUntil = prefs[KEY_VAULT_LOCKOUT_UNTIL] ?: 0L
            )
        }
        return info
    }

    suspend fun updateVaultCredentials(
        passcodeHash: String,
        passcodeSalt: String,
        recoveryHash: String,
        recoverySalt: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_VAULT_PASSCODE_HASH] = passcodeHash
            prefs[KEY_VAULT_PASSCODE_SALT] = passcodeSalt
            prefs[KEY_VAULT_RECOVERY_HASH] = recoveryHash
            prefs[KEY_VAULT_RECOVERY_SALT] = recoverySalt
            prefs[KEY_VAULT_SETUP_COMPLETED] = true
            prefs[KEY_VAULT_FAILED_ATTEMPTS] = 0
            prefs[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun updateVaultPasscodeOnly(
        passcodeHash: String,
        passcodeSalt: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_VAULT_PASSCODE_HASH] = passcodeHash
            prefs[KEY_VAULT_PASSCODE_SALT] = passcodeSalt
            prefs[KEY_VAULT_FAILED_ATTEMPTS] = 0
            prefs[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }

    suspend fun recordVaultFailure(failedAttempts: Int, lockoutUntil: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_VAULT_FAILED_ATTEMPTS] = failedAttempts
            prefs[KEY_VAULT_LOCKOUT_UNTIL] = lockoutUntil
        }
    }

    suspend fun resetVaultFailures() {
        dataStore.edit { prefs ->
            prefs[KEY_VAULT_FAILED_ATTEMPTS] = 0
            prefs[KEY_VAULT_LOCKOUT_UNTIL] = 0L
        }
    }
}

data class VaultSecurityInfo(
    val passcodeHash: String? = null,
    val passcodeSalt: String? = null,
    val recoveryHash: String? = null,
    val recoverySalt: String? = null,
    val failedAttempts: Int = 0,
    val lockoutUntil: Long = 0L
)
