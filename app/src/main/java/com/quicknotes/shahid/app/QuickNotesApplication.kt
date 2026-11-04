package com.quicknotes.shahid.app

import android.app.Application
import com.quicknotes.shahid.app.di.AppContainer
import com.quicknotes.shahid.app.di.AppContainerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickNotesApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)

        applicationScope.launch {
            try {
                container.storageManager.scanStorage()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
