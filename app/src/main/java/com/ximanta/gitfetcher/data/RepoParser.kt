package com.ximanta.gitfetcher.data

/**
 * Parses every flavour of GitHub repository URL a user might paste.
 *
 * Supported inputs (case-insensitive for the host, case-preserving for owner/repo):
 *
 *  ┌──────────────────────────────────────────────────────────────────────────┐
 *  │ https://github.com/realximanta/ximanta-gitfetcher                        │
 *  │ https://github.com/realximanta/ximanta-gitfetcher.git                    │
 *  │ https://github.com/realximanta/ximanta-gitfetcher/                       │
 *  │ http://github.com/realximanta/ximanta-gitfetcher                         │
 *  │ github.com/realximanta/ximanta-gitfetcher                                │
 *  │ www.github.com/realximanta/ximanta-gitfetcher                            │
 *  │ git@github.com:realximanta/ximanta-gitfetcher.git                        │
 *  │ ssh://git@github.com/realximanta/ximanta-gitfetcher.git                  │
 *  │ https://github.com/realximanta/ximanta-gitfetcher/tree/main              │
 *  │ https://github.com/realximanta/ximanta-gitfetcher/tree/main/src/main     │
 *  │ https://github.com/realximanta/ximanta-gitfetcher?tab=readme             │
 *  │ https://github.com/realximanta/ximanta-gitfetcher#readme                 │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 * Returns null when the string doesn't look like a GitHub repo URL at all.
 */
object RepoParser {

    /**
     * @return a pair (owner, repo) with ".git" stripped, or null if unparseable.
     */
    fun parse(rawInput: String): Pair<String, String>? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        // --- Normalise SSH shorthand: git@github.com:owner/repo.git -------------
        // Convert to https://github.com/owner/repo.git so the rest of the
        // parser only needs to deal with one shape.
        val normalised = when {
            input.startsWith("git@", ignoreCase = true) -> {
                val afterAt = input.substringAfter("@")
                val host = afterAt.substringBefore(":")
                val path = afterAt.substringAfter(":", "")
                "https://$host/$path"
            }

            input.startsWith("ssh://", ignoreCase = true) -> {
                input.removePrefix("ssh://")
                    .removePrefix("git@")
                    .replaceFirst(":", "/")
            }

            input.startsWith("git://", ignoreCase = true) -> {
                "https://" + input.removePrefix("git://")
            }

            // No scheme: "github.com/owner/repo" → add https://
            !input.contains("://") -> "https://$input"

            else -> input
        }

        // --- Strip fragment and query string ------------------------------------
        var cleaned = normalised
            .substringBefore('#')
            .substringBefore('?')

        // --- Strip trailing slashes ---------------------------------------------
        cleaned = cleaned.trimEnd('/')

        // --- Strip a trailing ".git" --------------------------------------------
        if (cleaned.endsWith(".git", ignoreCase = true)) {
            cleaned = cleaned.substring(0, cleaned.length - 4)
        }

        // --- Must start with http(s)://github.com/ (or www.github.com) ---------
        val lower = cleaned.lowercase()
        val isGithub = lower.startsWith("https://github.com/") ||
                lower.startsWith("http://github.com/") ||
                lower.startsWith("https://www.github.com/") ||
                lower.startsWith("http://www.github.com/")

        if (!isGithub) return null

        // --- Split the path portion --------------------------------------------
        val afterHost = when {
            lower.startsWith("https://www.github.com/") -> cleaned.substring("https://www.github.com/".length)
            lower.startsWith("http://www.github.com/") -> cleaned.substring("http://www.github.com/".length)
            lower.startsWith("https://github.com/") -> cleaned.substring("https://github.com/".length)
            else -> cleaned.substring("http://github.com/".length)
        }

        val segments = afterHost.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null

        val owner = segments[0]
        val repo = segments[1]

        // Reject obviously-invalid owner/repo tokens
        if (!isValidSlug(owner) || !isValidSlug(repo)) return null

        return owner to repo
    }

    /**
     * Convenience: extracts just the repo name (second segment).
     * Returns null if parsing fails.
     */
    fun extractRepoName(rawInput: String): String? = parse(rawInput)?.second

    /**
     * GitHub usernames/orgs: alphanumeric + hyphens, 1–39 chars, no leading
     * or trailing hyphen, no consecutive hyphens.
     * Repo names: same charset plus dots and underscores.
     *
     * We keep this slightly lenient — GitHub itself is the final validator;
     * this is just a sanity filter so we don't send an obvious garbage string
     * to the API.
     */
    private fun isValidSlug(value: String): Boolean {
        if (value.isEmpty() || value.length > 100) return false
        return value.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    }
}
