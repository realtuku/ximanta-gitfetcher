package com.ximanta.gitfetcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the three GitHub endpoints we need.
 *
 *   1. GET /repos/{owner}/{repo}                  → default_branch + description
 *   2. GET /repos/{owner}/{repo}/git/trees/{ref}  → every blob in one shot
 *   3. GET raw.githubusercontent.com/.../{path}   → file bytes
 *
 * No Retrofit, no Gson — the payloads are small and `org.json` is already in
 * the Android framework.
 */
object GitHubApi {

    private const val API_BASE = "https://api.github.com"
    private const val RAW_BASE = "https://raw.githubusercontent.com"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun baseHeaders(): Map<String, String> = mapOf(
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "User-Agent" to "Ximanta-GitFetcher/1.0"
    )

    // -------------------------------------------------------------------------
    // 1. Repo metadata
    // -------------------------------------------------------------------------

    suspend fun fetchRepoInfo(owner: String, repo: String): FetchResult<RepoInfo> =
        withContext(Dispatchers.IO) {
            val url = "$API_BASE/repos/$owner/$repo"
            val request = Request.Builder()
                .url(url)
                .apply { baseHeaders().forEach { (k, v) -> addHeader(k, v) } }
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()

                    when (response.code) {
                        200 -> {
                            val json = JSONObject(body)
                            val branch = json.optString("default_branch", "main")
                                .ifBlank { "main" }
                            val htmlUrl = json.optString(
                                "html_url",
                                "https://github.com/$owner/$repo"
                            )
                            val description = json.optString("description")
                                .takeIf { it.isNotBlank() && it != "null" }

                            FetchResult.Success(
                                RepoInfo(
                                    owner = owner,
                                    name = repo,
                                    branch = branch,
                                    htmlUrl = htmlUrl,
                                    description = description
                                )
                            )
                        }

                        404 -> FetchResult.Failure(
                            "Repository \"$owner/$repo\" was not found.\n" +
                                    "Check the URL, or the repo may be private."
                        )

                        403 -> FetchResult.Failure(
                            "GitHub rate limit reached (HTTP 403).\n" +
                                    "Wait an hour, or try again later."
                        )

                        else -> FetchResult.Failure(
                            "GitHub returned HTTP ${response.code}.\n${body.take(200)}"
                        )
                    }
                }
            } catch (io: IOException) {
                FetchResult.Failure(
                    "Network error: ${io.message ?: "unable to reach github.com"}",
                    io
                )
            } catch (t: Throwable) {
                FetchResult.Failure("Unexpected error: ${t.message}", t)
            }
        }

    // -------------------------------------------------------------------------
    // 2. Recursive tree — every file in the repo in a single call
    // -------------------------------------------------------------------------

    suspend fun fetchRepoTree(
        owner: String,
        repo: String,
        branch: String
    ): FetchResult<List<RepoFile>> = withContext(Dispatchers.IO) {

        // Try the requested branch first, then fall back to "main"/"master" in
        // case the API told us a branch that the tree endpoint won't accept.
        val candidates = linkedSetOf(branch, "main", "master", "HEAD")

        var lastError: FetchResult.Failure? = null

        for (ref in candidates) {
            val url = "$API_BASE/repos/$owner/$repo/git/trees/$ref?recursive=1"
            val request = Request.Builder()
                .url(url)
                .apply { baseHeaders().forEach { (k, v) -> addHeader(k, v) } }
                .get()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()

                    when (response.code) {
                        200 -> {
                            val files = parseTree(body)
                            return@withContext FetchResult.Success(files)
                        }

                        403 -> return@withContext FetchResult.Failure(
                            "GitHub rate limit reached (HTTP 403).\n" +
                                    "Unauthenticated limit is 60 req/hour."
                        )

                        404 -> {
                            lastError = FetchResult.Failure(
                                "Branch \"$ref\" not found in $owner/$repo."
                            )
                        }

                        else -> {
                            lastError = FetchResult.Failure(
                                "Tree request failed: HTTP ${response.code}.\n" +
                                        body.take(200)
                            )
                        }
                    }
                }
            } catch (io: IOException) {
                lastError = FetchResult.Failure(
                    "Network error while reading tree: ${io.message}", io
                )
            }
        }

        lastError ?: FetchResult.Failure("Could not read repository tree.")
    }

    /**
     * Parses the tree JSON and keeps only blobs (files). Directories and
     * submodule entries are dropped — the recursive flag already flattens
     * the whole hierarchy for us.
     */
    private fun parseTree(body: String): List<RepoFile> {
        val root = JSONObject(body)
        val array = root.optJSONArray("tree") ?: return emptyList()

        val out = ArrayList<RepoFile>(array.length())

        for (i in 0 until array.length()) {
            val node = array.optJSONObject(i) ?: continue
            val type = node.optString("type")
            if (type != "blob") continue // skip "tree" (dirs) and "commit" (submodules)

            val path = node.optString("path")
            if (path.isBlank()) continue

            val size = node.optLong("size", 0L)
            val sha = node.optString("sha", "")

            out.add(
                RepoFile(
                    path = path,
                    size = size,
                    sha = sha,
                    isBinary = RepoFile.isBinaryPath(path)
                )
            )
        }

        // Stable sort: directories first, then alphabetically — feels natural
        // when scrolling a long list.
        return out.sortedWith(
            compareBy({ it.dirPath.count { c -> c == '/' } }, { it.path.lowercase() })
        )
    }

    // -------------------------------------------------------------------------
    // 3. Raw file bytes
    // -------------------------------------------------------------------------

    /**
     * Downloads the raw bytes of a single file.
     *
     * Uses raw.githubusercontent.com because it is:
     *   • Not rate-limited the same way as the REST API
     *   • Returns the plain file, no base64 wrapping
     *   • Works for public repos without auth
     */
    suspend fun downloadFile(
        owner: String,
        repo: String,
        branch: String,
        path: String
    ): FetchResult<ByteArray> = withContext(Dispatchers.IO) {

        // Path segments need encoding — spaces, #, ? etc.
        val encodedPath = path.split('/').joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8")
                .replace("+", "%20")
        }

        val url = "$RAW_BASE/$owner/$repo/$branch/$encodedPath"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Ximanta-GitFetcher/1.0")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext FetchResult.Failure(
                        "HTTP ${response.code} while downloading $path"
                    )
                }
                val bytes = response.body?.bytes()
                    ?: return@withContext FetchResult.Failure(
                        "Empty body for $path"
                    )
                FetchResult.Success(bytes)
            }
        } catch (io: IOException) {
            FetchResult.Failure("Download failed for $path: ${io.message}", io)
        } catch (t: Throwable) {
            FetchResult.Failure("Unexpected error for $path: ${t.message}", t)
        }
    }
}
