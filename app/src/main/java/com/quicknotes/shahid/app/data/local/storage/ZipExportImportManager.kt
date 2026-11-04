package com.quicknotes.shahid.app.data.local.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipExportImportManager(
    private val context: Context,
    private val storageManager: MarkdownStorageManager
) {

    suspend fun exportNotesToZip(): Pair<File, Uri> = withContext(Dispatchers.IO) {
        val rootDir = storageManager.getNotesRootDirectory()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val zipFile = File(exportDir, "QuickNotes_Backup_$timeStamp.zip")
        val zos = ZipOutputStream(FileOutputStream(zipFile))

        fun zipDirectory(folder: File, parentPath: String) {
            val files = folder.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    val folderEntryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                    zos.putNextEntry(ZipEntry("$folderEntryName/"))
                    zos.closeEntry()
                    zipDirectory(file, folderEntryName)
                } else if (file.isFile && file.extension.equals("md", ignoreCase = true)) {
                    val entryName = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        zipDirectory(rootDir, "")
        zos.finish()
        zos.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zipFile
        )

        Pair(zipFile, uri)
    }

    suspend fun importNotesFromZip(zipUri: Uri): Int = withContext(Dispatchers.IO) {
        val targetRoot = storageManager.getNotesRootDirectory()
        var importedCount = 0

        context.contentResolver.openInputStream(zipUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val destinationFile = File(targetRoot, name)
                    val canonicalDest = destinationFile.canonicalPath
                    val canonicalRoot = targetRoot.canonicalPath

                    if (canonicalDest.startsWith(canonicalRoot)) {
                        if (entry.isDirectory) {
                            destinationFile.mkdirs()
                        } else if (name.endsWith(".md", ignoreCase = true)) {
                            destinationFile.parentFile?.mkdirs()
                            FileOutputStream(destinationFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            importedCount++
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (importedCount > 0) {
            storageManager.scanStorage()
        }

        importedCount
    }
}
