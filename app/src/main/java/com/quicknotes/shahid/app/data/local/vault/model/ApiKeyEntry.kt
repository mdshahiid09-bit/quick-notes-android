package com.quicknotes.shahid.app.data.local.vault.model

data class ApiKeyEntry(
    val id: String,
    val providerName: String,
    val encryptedApiKey: String,
    val iv: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DecryptedApiKey(
    val id: String,
    val providerName: String,
    val plainKey: String,
    val createdAt: Long,
    val updatedAt: Long
)
