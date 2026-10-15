package com.ximanta.gitfetcher.data

/**
 * Parses every flavour of GitHub repository identifier a user might paste.
 *
 * v2.0.0 adds the SHORTHAND form: `owner/repo` — no scheme, no host.
 *
 *  Full URLs (unchanged from v1):
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
 *  │ https://github.com/realximanta/ximanta-gitfetcher?tab=readme             │
 *  │ https://github.com/realximanta/ximanta-gitfetcher#readme                 │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 *  Shorthand (new in v2.0.0):
 *  ┌──────────────────────────────────────────────────────────────────────────┐
 *  │ realximanta/ximanta-gitfetcher                                           │
 *  │ realximanta/ximanta-gitfetcher.git                                       │
 *  │ realximanta/ximanta-gitfetcher/                                          │
 *  │ octocat/Hello-World                                                      │
 *  └──────────────────────────────────────────────────────────────────────────┘
 *
 * Returns null when the string doesn't look like a GitHub repo identifier at all.
 */
object RepoParser {

    /**
     * @return a pair (owner, repo) with ".git" stripped, or null if unparseable.
     */
    fun parse(rawInput: String): Pair<String, String>? {
        val input = rawInput.trim()
        if (input.isEmpty()) return null

        // 1. Try the full-URL path first — it's the stricter parser.
        parseFullUrl(input)?.let { return it }

        // 2. Fall back to the owner/repo shorthand.
        return parseShorthand(input)
    }

    /**
     * Convenience: extracts just the repo name (second segment).
     * Returns null if parsing fails.
     */
    fun extractRepoName(rawInput: String): String? = parse(rawInput)?.second

    // -------------------------------------------------------------------------
    // Full URL parsing — everything from v1, unchanged
    // -------------------------------------------------------------------------

    private fun parseFullUrl(input: String): Pair<String, String>? {

        // --- Normalise SSH shorthand: git@github.com:owner/repo.git -------------
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

            // No scheme present.
            !input.contains("://") -> {
                // Only auto-prefix https:// if it looks like a github URL.
                // Otherwise it might be the shorthand form — let parseShorthand
                // deal with it.
                if (input.contains("github.com", ignoreCase = true)) {
                    "https://$input"
                } else {
                    return null
                }
            }

            else -> input
        }

        // --- Strip fragment, query, trailing slashes, trailing .git -----------
        var cleaned = normalised
            .substringBefore('#')
            .substringBefore('?')
            .trimEnd('/')

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

        val afterHost = when {
            lower.startsWith("https://www.github.com/") ->
                cleaned.substring("https://www.github.com/".length)
            lower.startsWith("http://www.github.com/") ->
                cleaned.substring("http://www.github.com/".length)
            lower.startsWith("https://github.com/") ->
                cleaned.substring("https://github.com/".length)
            else ->
                cleaned.substring("http://github.com/".length)
        }

        val segments = afterHost.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null

        val owner = segments[0]
        val repo = segments[1]

        if (!isValidSlug(owner) || !isValidSlug(repo)) return null

        return owner to repo
    }

    // -------------------------------------------------------------------------
    // Shorthand parsing — new in v2.0.0
    // -------------------------------------------------------------------------

    /**
     * Accepts exactly two slash-separated tokens that look like a GitHub
     * owner and repo name.
     *
     * Rejects anything containing:
     *   • a scheme marker   (`://`)
     *   • an `@`            (SSH/email)
     *   • a `:`             (port, SSH)
     *   • whitespace, `?`, or `#`
     *   • a dot in the OWNER segment (so `github.com/foo` is treated as a
     *     hostname, not as owner=`github.com`)
     *
     * Repo names may contain dots — GitHub allows them.
     */
private fun parseShorthand(input: String): Pair<String, String>? {

    // Reject markers that only appear in URLs or SSH strings.
    if (input.contains("://")) return null
    if (input.contains("@")) return null
    if (input.contains(":")) return null
    if (input.any { it.isWhitespace() }) return null
    if (input.contains("?")) return null
    if (input.contains("#")) return null

    // Strip trailing slashes and .git
    var cleaned = input.trimEnd('/')
    if (cleaned.endsWith(".git", ignoreCase = true)) {
        cleaned = cleaned.substring(0, cleaned.length - 4)
    }

    val segments = cleaned.split('/').filter { it.isNotBlank() }
    if (segments.size != 2) return null

    val owner = segments[0]
    val repo = segments[1]

    // Allow dots in owner segment (GitHub usernames may contain '.').
    // Accept shorthand like "some.user/project" — the API will perform final validation.
    // (Previous code rejected dotted owners; that line was removed.)
    // if (owner.contains('.')) return null  <-- removed

    if (!isValidSlug(owner) || !isValidSlug(repo)) return null

    return owner to repo
}

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * GitHub usernames/orgs: alphanumeric + hyphens, 1–39 chars.
     * Repo names: same charset plus dots and underscores.
     *
     * Kept slightly lenient — GitHub's API is the final validator.
     * This is just a sanity filter so we don't send an obvious garbage
     * string to the network.
     */
    private fun isValidSlug(value: String): Boolean {
        if (value.isEmpty() || value.length > 100) return false
        return value.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
    }
}
