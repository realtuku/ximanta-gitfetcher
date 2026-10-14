package com.ximanta.gitfetcher.util

import android.content.Context

/**
 * Tiny persistent storage for Ximanta GitFetcher.
 *
 * Right now this holds exactly one flag — whether the user has accepted the
 * Terms & Conditions. Everything else in the app is either stateless or
 * lives on disk as a downloaded file.
 *
 * Backed by SharedPreferences, which Android keeps in:
 *     /data/data/com.ximanta.gitfetcher/shared_prefs/ximanta_prefs.xml
 *
 * Data survives:
 *   ✅ App close / force-stop
 *   ✅ Device sleep / restart
 *   ✅ App updates (same signing key)
 *   ✅ Auto Backup restore on new device (API 23+)
 *
 * Data does NOT survive:
 *   ❌ Uninstall → reinstall
 *   ❌ Clearing app data from Settings
 *
 * That's the right behaviour — reinstalling the app is a fresh consent.
 */
object Prefs {

    private const val FILE_NAME = "ximanta_prefs"
    private const val KEY_TERMS_ACCEPTED = "terms_accepted"

    /**
     * @return true if the user has previously accepted the Terms & Conditions
     *         on this device (under this installation).
     */
    fun hasAcceptedTerms(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TERMS_ACCEPTED, false)

    /**
     * Records that the user accepted the Terms & Conditions.
     *
     * Uses `.apply()` (async commit) rather than `.commit()` (blocking) so
     * we never stall the UI thread. The write is fast enough that even if
     * the process dies immediately after, Android flushes it.
     */
    fun setTermsAccepted(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_TERMS_ACCEPTED, true)
            .apply()
    }

    /**
     * Development helper — wipes all persisted state.
     * Not exposed in the UI yet; kept for future "Reset app" buttons.
     */
    @Suppress("unused")
    fun clearAll(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(
            FILE_NAME,
            Context.MODE_PRIVATE
        )
}
