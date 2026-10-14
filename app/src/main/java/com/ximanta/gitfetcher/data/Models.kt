package com.ximanta.gitfetcher.data

/**
 * Minimal metadata about the repository the user asked for.
 *
 * @param owner      GitHub username or org (e.g. "realximanta")
 * @param name       Repository name, without ".git" (e.g. "ximanta-gitfetcher")
 * @param branch     Default branch we resolved from the API (e.g. "main")
 * @param htmlUrl    Canonical web URL for display purposes
 * @param description Optional short description shown in the Files screen header
 */
data class RepoInfo(
    val owner: String,
    val name: String,
    val branch: String,
    val htmlUrl: String,
    val description: String?
) {
    /** "owner/name" — handy for labels and folder names. */
    val fullName: String get() = "$owner/$name"
}

/**
 * A single blob (file) inside the repository tree.
 *
 * Path is the full path relative to the repo root as returned by the GitHub
 * tree API, e.g. "src/main/java/com/ximanta/gitfetcher/MainActivity.kt".
 *
 * @param path    Full repo-relative path.
 * @param size    File size in bytes (from the tree API; may be 0 for submodules).
 * @param sha     Git blob SHA — useful for de-duplication if we ever cache.
 * @param isBinary Heuristic flag: true if the extension is known to be non-text.
 */
data class RepoFile(
    val path: String,
    val size: Long,
    val sha: String,
    val isBinary: Boolean = false
) {
    /** Just the trailing file name, e.g. "MainActivity.kt". */
    val fileName: String
        get() = path.substringAfterLast('/')

    /** The directory portion, or "" if the file sits at the repo root. */
    val dirPath: String
        get() = if (path.contains('/')) path.substringBeforeLast('/') else ""

    /**
     * Flattened filename used when saving to storage.
     *
     * "src/main/java/Foo.kt"  ->  "src__main__java__Foo.kt"
     *
     * This keeps the original path human-readable while ensuring every file
     * lands in a single flat folder with no collisions.
     */
    val flatFileName: String
        get() = path.replace('/', '_').replace('\\', '_')

    /** Human-readable size, e.g. "12.4 KB". */
    val prettySize: String get() = formatSize(size)

    companion object {
        /** Extensions we treat as non-text so we can skip preview attempts. */
        private val BINARY_EXTENSIONS = setOf(
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico", "svg",
            "pdf", "zip", "tar", "gz", "7z", "rar", "jar", "apk", "aab",
            "mp3", "mp4", "wav", "ogg", "flac", "mkv", "mov", "avi",
            "ttf", "otf", "woff", "woff2", "eot",
            "so", "dll", "dylib", "exe", "bin", "class", "dex",
            "psd", "ai", "sketch", "xcf"
        )

        fun isBinaryPath(path: String): Boolean {
            val ext = path.substringAfterLast('.', "").lowercase()
            return ext in BINARY_EXTENSIONS
        }

        fun formatSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var i = 0
            while (value >= 1024 && i < units.lastIndex) {
                value /= 1024
                i++
            }
            return if (i == 0) {
                "${bytes} B"
            } else {
                String.format("%.1f %s", value, units[i])
            }
        }
    }
}

/**
 * Wrapper for the whole fetch result so we can pass it around as one unit.
 */
data class RepoSnapshot(
    val repo: RepoInfo,
    val files: List<RepoFile>
) {
    val totalBytes: Long get() = files.sumOf { it.size }
    val prettyTotal: String get() = RepoFile.formatSize(totalBytes)
}

/**
 * Sealed result type used by the API layer so we never throw across coroutine
 * boundaries — everything is returned as a value.
 */
sealed interface FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : FetchResult<Nothing>
}
