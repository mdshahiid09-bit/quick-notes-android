package com.quicknotes.shahid.app.di

import android.content.Context
import android.content.Intent
import com.quicknotes.shahid.app.data.local.db.QuickNotesDatabase
import com.quicknotes.shahid.app.data.local.preferences.UserPreferencesRepository
import com.quicknotes.shahid.app.data.local.storage.DocumentDownloadManager
import com.quicknotes.shahid.app.data.local.storage.MarkdownStorageManager
import com.quicknotes.shahid.app.data.local.storage.ZipExportImportManager
import com.quicknotes.shahid.app.data.local.vault.VaultRepository
import com.quicknotes.shahid.app.data.local.vault.VaultSecurityManager
import com.quicknotes.shahid.app.data.repository.NotesRepository
import com.quicknotes.shahid.app.data.repository.NotesRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainerImpl(private val context: Context) : AppContainer {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: QuickNotesDatabase by lazy {
        QuickNotesDatabase.getInstance(context)
    }

    override val storageManager: MarkdownStorageManager by lazy {
        MarkdownStorageManager(context, database)
    }

    override val zipManager: ZipExportImportManager by lazy {
        ZipExportImportManager(context, storageManager)
    }

    override val downloadManager: DocumentDownloadManager by lazy {
        DocumentDownloadManager(context)
    }

    override val securityManager: VaultSecurityManager by lazy {
        VaultSecurityManager()
    }

    override val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(context)
    }

    override val vaultRepository: VaultRepository by lazy {
        VaultRepository(
            context = context,
            securityManager = securityManager,
            preferencesRepository = preferencesRepository,
            applicationScope = applicationScope
        )
    }

    override val notesRepository: NotesRepository by lazy {
        NotesRepositoryImpl(database, storageManager)
    }
}