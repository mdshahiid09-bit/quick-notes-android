package com.quicknotes.shahid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.quicknotes.shahid.app.data.local.preferences.ThemeMode
import com.quicknotes.shahid.app.ui.navigation.QuickNotesNavHost
import com.quicknotes.shahid.app.ui.theme.QuickNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as QuickNotesApplication
        val container = app.container

        val isDisguiseLaunch = intent?.component?.className?.contains("Disguise") == true

        setContent {
            val themeMode by container.preferencesRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            QuickNotesTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    QuickNotesNavHost(
                        navController = navController,
                        appContainer = container,
                        isDisguiseLaunch = isDisguiseLaunch
                    )
                }
            }
        }
    }
}
