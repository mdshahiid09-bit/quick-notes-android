package com.quicknotes.shahid.app.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.preferences.NotesViewMode
import com.quicknotes.shahid.app.data.local.preferences.ThemeMode
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.local.vault.VaultRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notesViewMode: NotesViewMode = NotesViewMode.GRID,
    val isDisguiseEnabled: Boolean = false
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val vaultRepository: VaultRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.themeMode,
        preferencesRepository.notesViewMode,
        preferencesRepository.isDisguiseEnabled
    ) { theme, viewMode, disguise ->
        SettingsUiState(
            themeMode = theme,
            notesViewMode = viewMode,
            isDisguiseEnabled = disguise
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setNotesViewMode(mode: NotesViewMode) {
        viewModelScope.launch {
            preferencesRepository.setNotesViewMode(mode)
        }
    }

    fun setDisguiseMode(context: Context, enableDisguise: Boolean) {
        viewModelScope.launch {
            val packageManager = context.packageManager
            val mainAlias = ComponentName(context.packageName, "${context.packageName}.MainActivityAlias")
            val disguiseAlias = ComponentName(context.packageName, "${context.packageName}.DisguiseActivityAlias")

            if (enableDisguise) {
                packageManager.setComponentEnabledSetting(
                    disguiseAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    mainAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                packageManager.setComponentEnabledSetting(
                    mainAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    disguiseAlias,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            preferencesRepository.setDisguiseEnabled(enableDisguise)
        }
    }

    fun lockVaultNow() {
        vaultRepository.lockVault()
    }

    companion object {
        fun provideFactory(
            preferencesRepository: UserPreferencesRepository,
            vaultRepository: VaultRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(preferencesRepository, vaultRepository) as T
            }
        }
    }
}
