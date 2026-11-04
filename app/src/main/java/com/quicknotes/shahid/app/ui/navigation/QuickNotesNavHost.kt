package com.quicknotes.shahid.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.quicknotes.shahid.app.R
import com.quicknotes.shahid.app.di.AppContainer
import com.quicknotes.shahid.app.ui.screens.archive.ArchiveScreen
import com.quicknotes.shahid.app.ui.screens.archive.ArchiveViewModel
import com.quicknotes.shahid.app.ui.screens.editor.EditorViewModel
import com.quicknotes.shahid.app.ui.screens.editor.MarkdownEditorScreen
import com.quicknotes.shahid.app.ui.screens.folders.FolderNotesScreen
import com.quicknotes.shahid.app.ui.screens.folders.FolderNotesViewModel
import com.quicknotes.shahid.app.ui.screens.folders.FoldersScreen
import com.quicknotes.shahid.app.ui.screens.folders.FoldersViewModel
import com.quicknotes.shahid.app.ui.screens.home.HomeScreen
import com.quicknotes.shahid.app.ui.screens.home.HomeViewModel
import com.quicknotes.shahid.app.ui.screens.onboarding.OnboardingScreen
import com.quicknotes.shahid.app.ui.screens.permission.StoragePermissionScreen
import com.quicknotes.shahid.app.ui.screens.settings.SettingsScreen
import com.quicknotes.shahid.app.ui.screens.settings.SettingsViewModel
import com.quicknotes.shahid.app.ui.screens.splash.SplashScreen
import com.quicknotes.shahid.app.ui.screens.tags.TagNotesScreen
import com.quicknotes.shahid.app.ui.screens.tags.TagNotesViewModel
import com.quicknotes.shahid.app.ui.screens.tags.TagsScreen
import com.quicknotes.shahid.app.ui.screens.tags.TagsViewModel
import com.quicknotes.shahid.app.ui.screens.trash.TrashScreen
import com.quicknotes.shahid.app.ui.screens.trash.TrashViewModel
import com.quicknotes.shahid.app.ui.screens.vault.VaultRecoveryScreen
import com.quicknotes.shahid.app.ui.screens.vault.VaultScreen
import com.quicknotes.shahid.app.ui.screens.vault.VaultSetupScreen
import com.quicknotes.shahid.app.ui.screens.vault.VaultViewModel

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelRes: Int
)

@Composable
fun QuickNotesNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    isDisguiseLaunch: Boolean,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, Icons.Default.Description, R.string.nav_notes),
        BottomNavItem(Screen.Folders, Icons.Default.Folder, R.string.nav_folders),
        BottomNavItem(Screen.Vault, Icons.Default.Lock, R.string.nav_vault),
        BottomNavItem(Screen.Settings, Icons.Default.Settings, R.string.nav_settings)
    )

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Folders.route,
        Screen.Vault.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.screen.route) {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(item.labelRes))
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Splash
            composable(Screen.Splash.route) {
                SplashScreen(
                    preferencesRepository = appContainer.preferencesRepository,
                    isDisguiseLaunch = isDisguiseLaunch,
                    vaultRepository = appContainer.vaultRepository,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    preferencesRepository = appContainer.preferencesRepository,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // Storage Permission
            composable(Screen.StoragePermission.route) {
                StoragePermissionScreen(
                    storageManager = appContainer.storageManager,
                    vaultRepository = appContainer.vaultRepository,
                    onNavigateHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.StoragePermission.route) { inclusive = true }
                        }
                    }
                )
            }

            // Home (Notes)
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.provideFactory(
                        appContainer.notesRepository,
                        appContainer.preferencesRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    zipManager = appContainer.zipManager,
                    onNavigateToEditor = { noteId ->
                        navController.navigate(Screen.Editor.createRoute(noteId))
                    },
                    onNavigateToFolders = {
                        navController.navigate(Screen.Folders.route)
                    },
                    onNavigateToTags = {
                        navController.navigate(Screen.Tags.route)
                    },
                    onNavigateToArchive = {
                        navController.navigate(Screen.Archive.route)
                    },
                    onNavigateToTrash = {
                        navController.navigate(Screen.Trash.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // Folders
            composable(Screen.Folders.route) {
                val foldersViewModel: FoldersViewModel = viewModel(
                    factory = FoldersViewModel.provideFactory(appContainer.notesRepository)
                )
                FoldersScreen(
                    viewModel = foldersViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onFolderClick = { folderName ->
                        navController.navigate(Screen.FolderNotes.createRoute(folderName))
                    }
                )
            }

            // Folder Notes
            composable(
                route = Screen.FolderNotes.route,
                arguments = listOf(
                    navArgument("folderName") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val rawFolder = backStackEntry.arguments?.getString("folderName") ?: ""
                val folderName = android.net.Uri.decode(rawFolder)
                val folderNotesViewModel: FolderNotesViewModel = viewModel(
                    key = "folder_$folderName",
                    factory = FolderNotesViewModel.provideFactory(
                        folderName = folderName,
                        notesRepository = appContainer.notesRepository,
                        preferencesRepository = appContainer.preferencesRepository
                    )
                )
                FolderNotesScreen(
                    viewModel = folderNotesViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { noteId, initialTitle ->
                        navController.navigate(
                            Screen.Editor.createRoute(
                                noteId = noteId,
                                folder = folderName,
                                title = initialTitle
                            )
                        )
                    },
                    downloadManager = appContainer.downloadManager,
                    storageManager = appContainer.storageManager
                )
            }

            // Tags
            composable(Screen.Tags.route) {
                val tagsViewModel: TagsViewModel = viewModel(
                    factory = TagsViewModel.provideFactory(appContainer.notesRepository)
                )
                TagsScreen(
                    viewModel = tagsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onTagClick = { tag ->
                        navController.navigate(Screen.TagNotes.createRoute(tag))
                    }
                )
            }

            // Tag Notes
            composable(
                route = Screen.TagNotes.route,
                arguments = listOf(
                    navArgument("tag") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val rawTag = backStackEntry.arguments?.getString("tag") ?: ""
                val tag = android.net.Uri.decode(rawTag)
                val tagNotesViewModel: TagNotesViewModel = viewModel(
                    key = "tag_$tag",
                    factory = TagNotesViewModel.provideFactory(
                        tag = tag,
                        notesRepository = appContainer.notesRepository,
                        preferencesRepository = appContainer.preferencesRepository
                    )
                )
                TagNotesScreen(
                    viewModel = tagNotesViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEditor = { noteId ->
                        navController.navigate(Screen.Editor.createRoute(noteId))
                    }
                )
            }

            // Trash
            composable(Screen.Trash.route) {
                val trashViewModel: TrashViewModel = viewModel(
                    factory = TrashViewModel.provideFactory(appContainer.notesRepository)
                )
                TrashScreen(
                    viewModel = trashViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Archive
            composable(Screen.Archive.route) {
                val archiveViewModel: ArchiveViewModel = viewModel(
                    factory = ArchiveViewModel.provideFactory(appContainer.notesRepository)
                )
                ArchiveScreen(
                    viewModel = archiveViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.Editor.createRoute(noteId))
                    }
                )
            }

            // Vault
            composable(Screen.Vault.route) {
                val vaultViewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.provideFactory(
                        appContainer.vaultRepository,
                        appContainer.securityManager
                    )
                )
                VaultScreen(
                    viewModel = vaultViewModel,
                    onNavigateToSetup = { navController.navigate(Screen.VaultSetup.route) },
                    onNavigateToRecovery = { navController.navigate(Screen.VaultRecovery.route) }
                )
            }

            // Vault Setup
            composable(Screen.VaultSetup.route) {
                val vaultViewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.provideFactory(
                        appContainer.vaultRepository,
                        appContainer.securityManager
                    )
                )
                VaultSetupScreen(
                    viewModel = vaultViewModel,
                    onSetupComplete = {
                        navController.navigate(Screen.StoragePermission.route) {
                            popUpTo(Screen.VaultSetup.route) { inclusive = true }
                        }
                    }
                )
            }

            // Vault Recovery
            composable(Screen.VaultRecovery.route) {
                val vaultViewModel: VaultViewModel = viewModel(
                    factory = VaultViewModel.provideFactory(
                        appContainer.vaultRepository,
                        appContainer.securityManager
                    )
                )
                VaultRecoveryScreen(
                    viewModel = vaultViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Markdown Editor
            composable(
                route = Screen.Editor.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("folder") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val rawFolder = backStackEntry.arguments?.getString("folder")
                val folder = if (rawFolder != null) android.net.Uri.decode(rawFolder) else null
                val rawTitle = backStackEntry.arguments?.getString("title")
                val title = if (rawTitle != null) android.net.Uri.decode(rawTitle) else null

                val viewModelKey = if (noteId != null) "note_$noteId" else "folder_${folder ?: "default"}_title_${title ?: "new"}"
                val editorViewModel: EditorViewModel = viewModel(
                    key = viewModelKey,
                    factory = EditorViewModel.provideFactory(
                        noteId = noteId,
                        folder = folder,
                        initialTitle = title,
                        notesRepository = appContainer.notesRepository
                    )
                )
                MarkdownEditorScreen(
                    viewModel = editorViewModel,
                    downloadManager = appContainer.downloadManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Settings
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(
                        appContainer.preferencesRepository,
                        appContainer.vaultRepository
                    )
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    vaultRepository = appContainer.vaultRepository,
                    zipManager = appContainer.zipManager,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}