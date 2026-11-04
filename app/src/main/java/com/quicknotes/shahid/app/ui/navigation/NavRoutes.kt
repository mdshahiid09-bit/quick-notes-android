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
        fun createRoute(folderName: String) = "folder_notes/${android.net.Uri.encode(folderName)}"
    }
    data object Tags : Screen("tags")
    data object TagNotes : Screen("tag_notes/{tag}") {
        fun createRoute(tag: String) = "tag_notes/${android.net.Uri.encode(tag)}"
    }
    data object Trash : Screen("trash")
    data object Archive : Screen("archive")
    data object Vault : Screen("vault")
    data object Editor : Screen("editor?noteId={noteId}&folder={folder}&title={title}") {
        fun createRoute(noteId: String? = null, folder: String? = null, title: String? = null): String {
            val params = mutableListOf<String>()
            if (!noteId.isNullOrBlank()) {
                params.add("noteId=$noteId")
            }
            if (!folder.isNullOrBlank()) {
                params.add("folder=${android.net.Uri.encode(folder)}")
            }
            if (!title.isNullOrBlank()) {
                params.add("title=${android.net.Uri.encode(title)}")
            }
            return if (params.isEmpty()) "editor" else "editor?" + params.joinToString("&")
        }
    }
    data object Settings : Screen("settings")
}