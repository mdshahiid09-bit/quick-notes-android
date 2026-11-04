package com.quicknotes.shahid.app.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quicknotes.shahid.app.data.local.vault.PasscodeStrength
import com.quicknotes.shahid.app.data.local.vault.VaultRepository
import com.quicknotes.shahid.app.data.local.vault.VaultSecurityManager
import com.quicknotes.shahid.app.data.local.vault.model.DecryptedApiKey
import com.quicknotes.shahid.app.data.local.vault.model.VaultState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultUiState(
    val vaultState: VaultState = VaultState.Locked(),
    val passcode: String = "",
    val confirmPasscode: String = "",
    val isPasscodeVisible: Boolean = false,
    val passcodeStrength: PasscodeStrength = PasscodeStrength.WEAK,
    val generatedRecoveryKey: String = "",
    val isRecoverySavedChecked: Boolean = false,
    val recoveryKeyInput: String = "",
    val copiedKeyId: String? = null,
    val showWrongPasscodeShake: Boolean = false,
    val errorMessage: String? = null
)

class VaultViewModel(
    private val vaultRepository: VaultRepository,
    private val securityManager: VaultSecurityManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    private var lockoutCountdownJob: Job? = null
    private var resetCopiedIconJob: Job? = null

    init {
        viewModelScope.launch {
            vaultRepository.vaultState.collect { state ->
                _uiState.value = _uiState.value.copy(vaultState = state)
                if (state is VaultState.Locked && state.lockoutRemainingSeconds > 0) {
                    startLockoutCountdown(state.lockoutRemainingSeconds)
                }
            }
        }
    }

    private fun startLockoutCountdown(initialSeconds: Int) {
        lockoutCountdownJob?.cancel()
        lockoutCountdownJob = viewModelScope.launch {
            var seconds = initialSeconds
            while (seconds > 0) {
                delay(1000L)
                seconds--
                val currentState = _uiState.value.vaultState
                if (currentState is VaultState.Locked) {
                    _uiState.value = _uiState.value.copy(
                        vaultState = currentState.copy(lockoutRemainingSeconds = seconds)
                    )
                }
            }
        }
    }

    fun onPasscodeChange(input: String) {
        val strength = securityManager.evaluatePasscodeStrength(input)
        _uiState.value = _uiState.value.copy(
            passcode = input,
            passcodeStrength = strength,
            errorMessage = null
        )
    }

    fun onConfirmPasscodeChange(input: String) {
        _uiState.value = _uiState.value.copy(
            confirmPasscode = input,
            errorMessage = null
        )
    }

    fun togglePasscodeVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasscodeVisible = !_uiState.value.isPasscodeVisible
        )
    }

    fun setRecoverySavedChecked(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isRecoverySavedChecked = checked)
    }

    fun onRecoveryKeyInputChange(input: String) {
        _uiState.value = _uiState.value.copy(recoveryKeyInput = input, errorMessage = null)
    }

    fun unlock() {
        val entered = _uiState.value.passcode
        viewModelScope.launch {
            val success = vaultRepository.unlockWithPasscode(entered)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    showWrongPasscodeShake = true,
                    passcode = ""
                )
                delay(600L)
                _uiState.value = _uiState.value.copy(showWrongPasscodeShake = false)
            } else {
                _uiState.value = _uiState.value.copy(passcode = "")
            }
        }
    }

    fun startVaultSetup(): Boolean {
        val p1 = _uiState.value.passcode
        val p2 = _uiState.value.confirmPasscode

        if (p1.length < 8) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passcode must be at least 8 characters")
            return false
        }
        if (p1 != p2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passcodes do not match")
            return false
        }

        viewModelScope.launch {
            val recoveryKey = vaultRepository.setupVault(p1)
            _uiState.value = _uiState.value.copy(
                generatedRecoveryKey = recoveryKey,
                passcode = "",
                confirmPasscode = "",
                errorMessage = null
            )
        }
        return true
    }

    fun verifyAndResetWithRecovery(newPasscode: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val verified = vaultRepository.verifyRecoveryKey(_uiState.value.recoveryKeyInput)
            if (verified) {
                vaultRepository.resetPasscodeWithRecovery(newPasscode)
                _uiState.value = _uiState.value.copy(recoveryKeyInput = "", errorMessage = null)
                onComplete()
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Invalid recovery key")
            }
        }
    }

    fun lockVault() {
        vaultRepository.lockVault()
    }

    fun addApiKey(provider: String, key: String, onDone: () -> Unit) {
        if (provider.isBlank() || key.isBlank()) return
        viewModelScope.launch {
            vaultRepository.addApiKey(provider, key)
            onDone()
        }
    }

    fun updateApiKey(id: String, provider: String, key: String, onDone: () -> Unit) {
        if (provider.isBlank() || key.isBlank()) return
        viewModelScope.launch {
            vaultRepository.updateApiKey(id, provider, key)
            onDone()
        }
    }

    fun deleteApiKey(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteApiKey(id)
        }
    }

    fun copyKey(keyEntry: DecryptedApiKey) {
        vaultRepository.copyKeyToClipboard(keyEntry.plainKey)
        _uiState.value = _uiState.value.copy(copiedKeyId = keyEntry.id)

        resetCopiedIconJob?.cancel()
        resetCopiedIconJob = viewModelScope.launch {
            delay(2000L)
            if (_uiState.value.copiedKeyId == keyEntry.id) {
                _uiState.value = _uiState.value.copy(copiedKeyId = null)
            }
        }
    }

    companion object {
        fun provideFactory(
            vaultRepository: VaultRepository,
            securityManager: VaultSecurityManager
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VaultViewModel(vaultRepository, securityManager) as T
            }
        }
    }
}
