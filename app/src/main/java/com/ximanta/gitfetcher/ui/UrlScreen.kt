package com.ximanta.gitfetcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Github
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ximanta.gitfetcher.data.FetchResult
import com.ximanta.gitfetcher.data.GitHubApi
import com.ximanta.gitfetcher.data.RepoFile
import com.ximanta.gitfetcher.data.RepoInfo
import com.ximanta.gitfetcher.data.RepoParser
import kotlinx.coroutines.launch

/**
 * URL entry screen.
 *
 *  • User pastes a GitHub repo URL (any of the accepted shapes).
 *  • The repo name is parsed live and appears in the button label.
 *  • Tapping the button fetches repo info + the flattened tree.
 *  • On success, `onRepoLoaded` is invoked with the parsed data.
 */
@Composable
fun UrlScreen(
    onBack: () -> Unit,
    onRepoLoaded: (RepoInfo, List<RepoFile>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var urlText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Live-parse the URL so the button label updates as the user types.
    val parsed: Pair<String, String>? = remember(urlText) {
        RepoParser.parse(urlText)
    }
    val repoName: String? = parsed?.second

    // Convenience: is the input valid enough to enable the button?
    val canFetch = parsed != null && !isLoading

    // Auto-focus the input on first composition.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(12.dp))

        // ---------- Compact brand header -------------------------------------
        XimantaLogo(size = 72)
        Spacer(Modifier.height(16.dp))
        BrandHeader()
        Spacer(Modifier.height(24.dp))

        // ---------- URL input card -------------------------------------------
        SectionCard(
            title = "Repository URL",
            icon = Icons.Filled.Link
        ) {
            Column {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            "https://github.com/user/repo",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Github,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (urlText.isNotBlank()) {
                                IconButton(onClick = { urlText = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                            IconButton(onClick = {
                                val clip = clipboard.getText()?.text.orEmpty()
                                if (clip.isNotBlank()) {
                                    urlText = clip.trim()
                                    errorMessage = null
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (canFetch) {
                                keyboard?.hide()
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = fetchRepo(parsed!!.first, parsed.second)
                                    isLoading = false
                                    when (result) {
                                        is FetchResult.Success -> onRepoLoaded(
                                            result.data.first,
                                            result.data.second
                                        )
                                        is FetchResult.Failure -> errorMessage = result.message
                                    }
                                }
                            }
                        }
                    ),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Hint row — shows what the parser detected, live.
                when {
                    urlText.isBlank() -> Text(
                        text = "Paste a link to any public GitHub repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    parsed != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            text = "${parsed.first}/${parsed.second}",
                            tone = ChipTone.Success,
                            leadingIcon = Icons.Filled.CheckCircle2
                        )
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(
                            text = "Not a valid GitHub URL",
                            tone = ChipTone.Warning
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---------- Fetch button ---------------------------------------------
        Button(
            onClick = {
                if (!canFetch || parsed == null) return@Button
                keyboard?.hide()
                isLoading = true
                errorMessage = null
                scope.launch {
                    val result = fetchRepo(parsed.first, parsed.second)
                    isLoading = false
                    when (result) {
                        is FetchResult.Success -> onRepoLoaded(
                            result.data.first,
                            result.data.second
                        )
                        is FetchResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = canFetch,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Fetching…",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (repoName != null) "Fetch $repoName" else "Fetch",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // ---------- Inline error card ----------------------------------------
        AnimatedVisibility(visible = errorMessage != null) {
            Column {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---------- Example hints --------------------------------------------
        ExampleHints(
            onPick = { example ->
                urlText = example
                errorMessage = null
            }
        )

        Spacer(Modifier.height(24.dp))

        BrandFooter()
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.height(8.dp))
    }
}

// -----------------------------------------------------------------------------
// Fetch helper — runs the two-step API call (info + tree)
// -----------------------------------------------------------------------------

private suspend fun fetchRepo(
    owner: String,
    repo: String
): FetchResult<Pair<RepoInfo, List<RepoFile>>> {

    return when (val infoResult = GitHubApi.fetchRepoInfo(owner, repo)) {
        is FetchResult.Failure -> infoResult

        is FetchResult.Success -> {
            val info = infoResult.data
            when (val treeResult = GitHubApi.fetchRepoTree(owner, repo, info.branch)) {
                is FetchResult.Failure -> treeResult
                is FetchResult.Success -> FetchResult.Success(info to treeResult.data)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Example hint row
// -----------------------------------------------------------------------------

@Composable
private fun ExampleHints(
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val examples = listOf(
        "https://github.com/realximanta/ximanta-gitfetcher",
        "https://github.com/octocat/Hello-World"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LabeledDivider("TRY AN EXAMPLE")
        Spacer(Modifier.height(10.dp))
        examples.forEach { ex ->
            Surface(
                onClick = { onPick(ex) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = ex,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
