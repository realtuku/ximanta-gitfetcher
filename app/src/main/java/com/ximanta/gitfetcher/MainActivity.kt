package com.ximanta.gitfetcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.ximanta.gitfetcher.data.RepoFile
import com.ximanta.gitfetcher.data.RepoInfo
import com.ximanta.gitfetcher.ui.FilesScreen
import com.ximanta.gitfetcher.ui.TermsScreen
import com.ximanta.gitfetcher.ui.UrlScreen
import com.ximanta.gitfetcher.ui.theme.XimantaGitFetcherTheme
import com.ximanta.gitfetcher.util.Prefs

/**
 * Single-activity entry point.
 *
 * Navigation is a simple 3-state sealed class — no Navigation-Compose dependency
 * needed for such a small app.
 *
 *   TermsScreen  ──agree──▶  UrlScreen  ──fetch──▶  FilesScreen
 *
 * v2.0.0:
 *   • Terms acceptance is now persisted via Prefs (SharedPreferences).
 *     On every launch we check disk first. Once agreed, the Terms screen
 *     is skipped forever on this installation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XimantaGitFetcherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    XimantaApp()
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Navigation state
// -----------------------------------------------------------------------------

private sealed interface Screen {
    data object Terms : Screen
    data object Url : Screen
    data class Files(val repo: RepoInfo, val files: List<RepoFile>) : Screen
}

@Composable
private fun XimantaApp() {

    val context = LocalContext.current

    // ---------------------------------------------------------------------
    // Terms acceptance — read once at startup from SharedPreferences.
    //
    // remember { } (not rememberSaveable) is correct here because the
    // initial value must be read from disk exactly once per process.
    // Rotation doesn't re-run the lambda, process death does, which is
    // fine — the value on disk is authoritative.
    // ---------------------------------------------------------------------
    var termsAgreed by remember {
        mutableStateOf(Prefs.hasAcceptedTerms(context))
    }

    // Current screen. Not saveable because RepoFile/RepoInfo aren't Parcelable —
    // on rotation during Files screen we fall back to Url screen, which is fine.
    var screen: Screen by remember {
        mutableStateOf<Screen>(
            if (termsAgreed) Screen.Url else Screen.Terms
        )
    }

    when (val current = screen) {

        Screen.Terms -> {
            TermsScreen(
                onAgree = {
                    // Persist to disk FIRST so a crash right after tap
                    // doesn't send the user back to the Terms screen.
                    Prefs.setTermsAccepted(context)
                    termsAgreed = true
                    screen = Screen.Url
                }
            )
        }

        Screen.Url -> {
            UrlScreen(
                onBack = { screen = Screen.Terms },
                onRepoLoaded = { repo, files ->
                    screen = Screen.Files(repo, files)
                }
            )
        }

        is Screen.Files -> {
            FilesScreen(
                repo = current.repo,
                files = current.files,
                onBack = { screen = Screen.Url }
            )
        }
    }
}
