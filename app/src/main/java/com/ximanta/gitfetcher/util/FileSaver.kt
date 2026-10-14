package com.ximanta.gitfetcher.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Saves downloaded bytes to the user's Downloads folder.
 *
 * Two storage paths are used depending on OS version:
 *
 *  ┌────────────────────┬──────────────────────────────────────────────────┐
 *  │ Android 10+ (29+)  │ MediaStore.Downloads — no runtime permission    │
 *  │ Android 8 – 9      │ Legacy File API — requires WRITE_EXTERNAL_STORAGE│
 *  └────────────────────┴──────────────────────────────────────────────────┘
 *
 * Every file is written into:
 *
 *     Downloads/Ximanta/<repo-name>/<flattened-filename>
 *
 * The "flattened" name replaces "/" with "_" so nested paths never collide.
 */
object FileSaver {

    private const val ROOT_FOLDER = "Ximanta"

    /**
     * Result of a save operation.
     */
    sealed interface SaveResult {
        data class Success(val displayPath: String, val uri: Uri?) : SaveResult
        data class Failure(val message: String, val cause: Throwable? = null) : SaveResult
    }

    /**
     * Saves [bytes] as a file named [flatFileName] inside a per-repo folder.
     *
     * @param context       Any context; we only use it for ContentResolver.
     * @param repoName      Used as the subfolder name (e.g. "ximanta-gitfetcher").
     * @param flatFileName  Already-flattened filename, e.g. "app_build.gradle.kts".
     * @param mimeType      Best-effort MIME type; may be "application/octet-stream".
     */
    suspend fun saveFile(
        context: Context,
        repoName: String,
        flatFileName: String,
        bytes: ByteArray,
        mimeType: String = "application/octet-stream"
    ): SaveResult = withContext(Dispatchers.IO) {

        val safeRepo = sanitize(repoName.ifBlank { "repo" })
        val safeName = sanitizeFileName(flatFileName.ifBlank { "file.txt" })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, safeRepo, safeName, bytes, mimeType)
        } else {
            saveViaLegacyStorage(safeRepo, safeName, bytes)
        }
    }

    // -------------------------------------------------------------------------
    // Android 10+ — MediaStore.Downloads (no permission needed)
    // -------------------------------------------------------------------------

    private fun saveViaMediaStore(
        context: Context,
        repoFolder: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): SaveResult {
        var output: OutputStream? = null
        var uri: Uri? = null

        try {
            val resolver = context.contentResolver

            // Relative path must end with "/" and must NOT include "Download/" twice.
            val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_FOLDER/$repoFolder"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return SaveResult.Failure("MediaStore rejected the insert request.")

            output = resolver.openOutputStream(uri)
                ?: return SaveResult.Failure("Could not open output stream for MediaStore.")

            output.use { it.write(bytes) }
            output = null

            // Clear the pending flag so the file becomes visible to other apps.
            val done = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(uri, done, null, null)

            return SaveResult.Success(
                displayPath = "Downloads/$ROOT_FOLDER/$repoFolder/$fileName",
                uri = uri
            )

        } catch (io: IOException) {
            uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            return SaveResult.Failure("Write failed: ${io.message}", io)
        } catch (t: Throwable) {
            uri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            return SaveResult.Failure("Unexpected save error: ${t.message}", t)
        } finally {
            runCatching { output?.close() }
        }
    }

    // -------------------------------------------------------------------------
    // Android 8 – 9 — Legacy external storage
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    private fun saveViaLegacyStorage(
        repoFolder: String,
        fileName: String,
        bytes: ByteArray
    ): SaveResult {
        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val targetDir = File(downloads, "$ROOT_FOLDER/$repoFolder")

            if (!targetDir.exists() && !targetDir.mkdirs()) {
                return SaveResult.Failure(
                    "Could not create folder: ${targetDir.absolutePath}"
                )
            }

            val targetFile = File(targetDir, fileName)

            FileOutputStream(targetFile).use { it.write(bytes) }

            SaveResult.Success(
                displayPath = targetFile.absolutePath,
                uri = null
            )
        } catch (io: IOException) {
            SaveResult.Failure("Legacy write failed: ${io.message}", io)
        } catch (t: Throwable) {
            SaveResult.Failure("Unexpected legacy error: ${t.message}", t)
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Picks a plausible MIME type from the file extension.
     * Fallback is octet-stream, which every file manager accepts.
     */
    fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "md", "markdown" -> "text/plain"
            "kt", "kts" -> "text/x-kotlin"
            "java" -> "text/x-java"
            "xml" -> "text/xml"
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "mjs" -> "application/javascript"
            "ts" -> "application/typescript"
            "json" -> "application/json"
            "yml", "yaml" -> "text/yaml"
            "gradle", "properties" -> "text/plain"
            "py" -> "text/x-python"
            "rb" -> "text/x-ruby"
            "go" -> "text/x-go"
            "rs" -> "text/x-rust"
            "c" -> "text/x-c"
            "h", "hpp" -> "text/x-c-header"
            "cpp", "cc", "cxx" -> "text/x-c++"
            "sh", "bash", "zsh" -> "text/x-shellscript"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "so" -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }

    /** Removes path separators from a folder name. */
    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)

    /**
     * Sanitizes a flat filename. Preserves dots so the extension survives
     * but strips characters that are illegal on ext4 / FAT32.
     */
    private fun sanitizeFileName(name: String): String {
        val cleaned = name
            .replace(Regex("[\\\\:*?\"<>|]"), "_")
            .replace(Regex("/+"), "_")
            .trim()
            .take(180)
        return cleaned.ifBlank { "file_${System.currentTimeMillis()}.txt" }
    }
}
