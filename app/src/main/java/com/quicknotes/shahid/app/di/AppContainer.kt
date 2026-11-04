package com.quicknotes.shahid.app.di

import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.local.storage.DocumentDownloadManager
import com.quicknotes.shahid.app.data.local.storage.MarkdownStorageManager
import com.quicknotes.shahid.app.data.local.storage.ZipExportImportManager
import com.quicknotes.shahid.app.data.local.vault.VaultRepository
import com.quicknotes.shahid.app.data.local.vault.VaultSecurityManager
import com.quicknotes.shahid.app.data.repository.NotesRepository

interface AppContainer {
    val notesRepository: NotesRepository
    val vaultRepository: VaultRepository
    val preferencesRepository: UserPreferencesRepository
    val storageManager: MarkdownStorageManager
    val zipManager: ZipExportImportManager
    val securityManager: VaultSecurityManager
    val downloadManager: DocumentDownloadManager
}