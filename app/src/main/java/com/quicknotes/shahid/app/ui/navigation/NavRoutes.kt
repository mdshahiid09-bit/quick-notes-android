package com.quicknotes.shahid.app.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object StoragePermission : Screen("storage_permission")
    data object VaultSetup : Screen("vault_setup")
    data object VaultRecovery : Screen("vault_recovery")
    data object Home : Screen("home")
    data object Folders : Screen("folders")
    data object FolderNotes : Screen("folder_notes/{folderName}") {
        fun createRoute(folderName: String) = "folder_notes/$folderName"
    }
    data object Tags : Screen("tags")
    data object TagNotes : Screen("tag_notes/{tag}") {
        fun createRoute(tag: String) = "tag_notes/$tag"
    }
    data object Trash : Screen("trash")
    data object Archive : Screen("archive")
    data object Vault : Screen("vault")
    data object Editor : Screen("editor?noteId={noteId}") {
        fun createRoute(noteId: String? = null) = if (noteId != null) "editor?noteId=$noteId" else "editor"
    }
    data object Settings : Screen("settings")
}
