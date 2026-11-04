package com.quicknotes.shahid.app.data.local.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocumentDownloadManager(
    private val context: Context
) {

    private fun getPublicDownloadsDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }

    suspend fun downloadNoteToDownloads(
        sourceFilePath: String,
        preferredFileName: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourceFilePath)
            if (!sourceFile.exists()) {
                return@withContext Result.failure(Exception("Source file not found"))
            }

            val downloadsDir = getPublicDownloadsDirectory()
            val rawName = preferredFileName?.trim()?.ifEmpty { null } ?: sourceFile.name
            val baseName = if (rawName.endsWith(".md", ignoreCase = true)) {
                rawName.substring(0, rawName.length - 3)
            } else {
                rawName
            }
            val sanitizedBase = baseName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "document" }

            var destFile = File(downloadsDir, "$sanitizedBase.md")
            var counter = 1
            while (destFile.exists()) {
                destFile = File(downloadsDir, "${sanitizedBase}_$counter.md")
                counter++
            }

            sourceFile.copyTo(destFile, overwrite = false)

            // Trigger MediaScanner so the file is instantly visible in system file manager / Downloads app
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("text/markdown", "text/plain"),
                null
            )

            Result.success(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun downloadFolderToDownloads(
        folderDir: File,
        folderName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!folderDir.exists() || !folderDir.isDirectory) {
                return@withContext Result.failure(Exception("Folder does not exist"))
            }

            val downloadsDir = getPublicDownloadsDirectory()
            val sanitizedFolder = folderName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Folder" }

            var zipFile = File(downloadsDir, "${sanitizedFolder}_documents.zip")
            var counter = 1
            while (zipFile.exists()) {
                zipFile = File(downloadsDir, "${sanitizedFolder}_documents_$counter.zip")
                counter++
            }

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val files = folderDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && file.extension.equals("md", ignoreCase = true)) {
                        val entry = ZipEntry(file.name)
                        entry.time = file.lastModified()
                        zos.putNextEntry(entry)
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }

            MediaScannerConnection.scanFile(
                context,
                arrayOf(zipFile.absolutePath),
                arrayOf("application/zip"),
                null
            )

            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}