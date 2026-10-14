package com.ximanta.gitfetcher.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ximanta.gitfetcher.data.FetchResult
import com.ximanta.gitfetcher.data.GitHubApi
import com.ximanta.gitfetcher.data.RepoFile
import com.ximanta.gitfetcher.data.RepoInfo
import com.ximanta.gitfetcher.util.FileSaver
import kotlinx.coroutines.launch

/**
 * Files list screen.
 *
 * Shows every file in the repo as a flat list, top to bottom. Each row has a
 * download button on the right. A "Download All Files" button sits at the
 * bottom of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    repo: RepoInfo,
    files: List<RepoFile>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Search filter — empty means "show everything".
    var query by remember { mutableStateOf("") }

    // Set of flat file names that have been saved successfully.
    var downloaded by remember { mutableStateOf(setOf<String>()) }

    // Per-file "in progress" set.
    var inProgress by remember { mutableStateOf(setOf<String>()) }

    // Bulk download state.
    var bulkActive by remember { mutableStateOf(false) }
    var bulkDone by remember { mutableIntStateOf(0) }

    // Storage permission gate. On Android 10+ this launcher is a no-op.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it } || grants.isEmpty()
        if (!granted) {
            scope.launch {
                snackbar.showSnackbar("Storage permission denied — cannot save files.")
            }
        }
    }

    // Ask for the legacy storage permission once on entry (only matters ≤ Android 9).
    remember(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
        true
    }

    // Filtered list used by the UI.
    val visibleFiles = remember(query, files) {
        if (query.isBlank()) files
        else files.filter {
            it.path.contains(query, ignoreCase = true) ||
                    it.fileName.contains(query, ignoreCase = true)
        }
    }

    // Total size of what's visible (or everything, if no filter).
    val totalBytes = visibleFiles.sumOf { it.size }
    val prettyTotal = RepoFile.formatSize(totalBytes)

    // ---------------------------------------------------------------------
    // Download actions
    // ---------------------------------------------------------------------

    fun downloadOne(file: RepoFile) {
        if (file.path in inProgress || file.path in downloaded) return
        inProgress = inProgress + file.path
        scope.launch {
            val result = GitHubApi.downloadFile(
                owner = repo.owner,
                repo = repo.name,
                branch = repo.branch,
                path = file.path
            )
            when (result) {
                is FetchResult.Success -> {
                    val save = FileSaver.saveFile(
                        context = context,
                        repoName = repo.name,
                        flatFileName = file.flatFileName,
                        bytes = result.data,
                        mimeType = FileSaver.guessMimeType(file.fileName)
                    )
                    when (save) {
                        is FileSaver.SaveResult.Success -> {
                            downloaded = downloaded + file.path
                            snackbar.showSnackbar("Saved ${file.fileName}")
                        }
                        is FileSaver.SaveResult.Failure -> {
                            snackbar.showSnackbar("Save failed: ${save.message}")
                        }
                    }
                }
                is FetchResult.Failure -> {
                    snackbar.showSnackbar("Download failed: ${result.message}")
                }
            }
            inProgress = inProgress - file.path
        }
    }

    fun downloadAll() {
        if (bulkActive) return
        bulkActive = true
        bulkDone = 0
        scope.launch {
            var ok = 0
            var fail = 0
            for (file in visibleFiles) {
                if (file.path in downloaded) {
                    ok++
                    bulkDone = ok
                    continue
                }
                inProgress = inProgress + file.path
                val result = GitHubApi.downloadFile(
                    owner = repo.owner,
                    repo = repo.name,
                    branch = repo.branch,
                    path = file.path
                )
                when (result) {
                    is FetchResult.Success -> {
                        val save = FileSaver.saveFile(
                            context = context,
                            repoName = repo.name,
                            flatFileName = file.flatFileName,
                            bytes = result.data,
                            mimeType = FileSaver.guessMimeType(file.fileName)
                        )
                        if (save is FileSaver.SaveResult.Success) {
                            downloaded = downloaded + file.path
                            ok++
                        } else {
                            fail++
                        }
                    }
                    is FetchResult.Failure -> fail++
                }
                inProgress = inProgress - file.path
                bulkDone = ok
            }
            bulkActive = false
            snackbar.showSnackbar(
                if (fail == 0) "Downloaded $ok file(s) to Downloads/Ximanta/${repo.name}/"
                else "Downloaded $ok file(s), $fail failed."
            )
        }
    }

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = repo.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${repo.owner} · ${repo.branch}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
        ) {

            // ---------- Summary bar ------------------------------------------
            RepoSummaryBar(
                fileCount = visibleFiles.size,
                totalFiles = files.size,
                prettySize = prettyTotal,
                downloadedCount = downloaded.size
            )

            // ---------- Search ------------------------------------------------
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Filter files…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // ---------- File list --------------------------------------------
            if (visibleFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Filled.Folder,
                        title = if (files.isEmpty()) "No files found"
                        else "No matches",
                        subtitle = if (files.isEmpty())
                            "This repository appears to be empty."
                        else "Try a different search term."
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = visibleFiles,
                        key = { it.path }
                    ) { file ->
                        FileRow(
                            file = file,
                            isDownloaded = file.path in downloaded,
                            isDownloading = file.path in inProgress,
                            onDownload = { downloadOne(file) }
                        )
                    }

                    // ---------- Download All button — bottom of list --------
                    item(key = "__download_all__") {
                        Spacer(Modifier.height(12.dp))
                        DownloadAllButton(
                            totalCount = visibleFiles.size,
                            completedCount = downloaded.size,
                            prettyTotal = prettyTotal,
                            bulkActive = bulkActive,
                            bulkDone = bulkDone,
                            onClick = { downloadAll() }
                        )
                        Spacer(Modifier.height(24.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            BrandFooter()
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// =============================================================================
// Summary bar
// =============================================================================

@Composable
private fun RepoSummaryBar(
    fileCount: Int,
    totalFiles: Int,
    prettySize: String,
    downloadedCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusChip(
                text = if (fileCount == totalFiles) "$fileCount files"
                else "$fileCount / $totalFiles files",
                tone = ChipTone.Info
            )
            StatusChip(
                text = prettySize,
                tone = ChipTone.Neutral
            )
            if (downloadedCount > 0) {
                StatusChip(
                    text = "$downloadedCount saved",
                    tone = ChipTone.Success,
                    leadingIcon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}

// =============================================================================
// Single file row
// =============================================================================

@Composable
private fun FileRow(
    file: RepoFile,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---------- File-type icon ------------------------------------
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBackground(file)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(file),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // ---------- Path + size --------------------------------------
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.dirPath.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    MonoLine(
                        text = file.dirPath,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = file.prettySize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // ---------- Download / state button --------------------------
            when {
                isDownloading -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                isDownloaded -> {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Downloaded",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                else -> {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download ${file.fileName}",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Download All button
// =============================================================================

@Composable
private fun DownloadAllButton(
    totalCount: Int,
    completedCount: Int,
    prettyTotal: String,
    bulkActive: Boolean,
    bulkDone: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDone = completedCount >= totalCount && totalCount > 0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Download All Files",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (bulkActive)
                    "Downloading… $bulkDone / $totalCount"
                else if (allDone)
                    "All $totalCount file(s) saved"
                else
                    "$totalCount file(s) · $prettyTotal total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            if (bulkActive) {
                Spacer(Modifier.height(10.dp))
                val progress = if (totalCount == 0) 0f
                else bulkDone.toFloat() / totalCount.toFloat()
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onClick,
                enabled = !bulkActive && totalCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (bulkActive) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Downloading…")
                } else {
                    Icon(
                        imageVector = if (allDone) Icons.Filled.Refresh
                        else Icons.Filled.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (allDone) "Re-download All"
                        else "Download All Files",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Saved to Downloads/Ximanta/",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// =============================================================================
// Icon helpers
// =============================================================================

private fun iconFor(file: RepoFile) = when {
    file.isBinary && isImageExt(file.fileName) -> Icons.Filled.Image
    isCodeExt(file.fileName) -> Icons.Filled.Code
    isDocExt(file.fileName) -> Icons.Filled.Description
    else -> Icons.Filled.InsertDriveFile
}

private fun iconBackground(file: RepoFile): Color = when {
    isCodeExt(file.fileName) -> Color(0xFF6C4CF1).copy(alpha = 0.12f)
    isImageExt(file.fileName) -> Color(0xFF2DD4BF).copy(alpha = 0.14f)
    isDocExt(file.fileName) -> Color(0xFF2563EB).copy(alpha = 0.12f)
    else -> Color(0xFF7A7885).copy(alpha = 0.12f)
}

private fun isCodeExt(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf(
        "kt", "kts", "java", "py", "rb", "go", "rs", "c", "cpp", "cc", "h", "hpp",
        "js", "mjs", "ts", "tsx", "jsx", "swift", "dart", "php", "sh", "bash",
        "zsh", "sql", "html", "css", "scss", "xml", "json", "yml", "yaml",
        "gradle", "properties", "toml", "ini"
    )
}

private fun isImageExt(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "ico")
}

private fun isDocExt(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("md", "markdown", "txt", "rst", "pdf", "doc", "docx", "rtf")
}
