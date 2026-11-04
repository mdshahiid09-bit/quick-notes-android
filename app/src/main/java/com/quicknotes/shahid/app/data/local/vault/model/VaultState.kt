package com.quicknotes.shahid.app.data.local.vault.model

sealed interface VaultState {
    data object NotConfigured : VaultState
    data class Locked(
        val failedAttempts: Int = 0,
        val lockoutRemainingSeconds: Int = 0,
        val errorMessage: String? = null
    ) : VaultState
    data class Unlocked(
        val keys: List<DecryptedApiKey> = emptyList()
    ) : VaultState
}
