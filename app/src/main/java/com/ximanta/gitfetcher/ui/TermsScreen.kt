package com.ximanta.gitfetcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Terms & Conditions gate.
 *
 * The user MUST tick the checkbox before the AGREE button enables.
 * Once tapped, `onAgree()` fires and MainActivity swaps to UrlScreen.
 */
@Composable
fun TermsScreen(
    onAgree: () -> Unit,
    modifier: Modifier = Modifier
) {
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(20.dp))

        // ---------- Hero: logo + wordmark -----------------------------------
        XimantaLogo(size = 96)
        Spacer(Modifier.height(20.dp))
        BrandHeader()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Fetch any GitHub repository — every file, one flat list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // ---------- Terms card -----------------------------------------------
        SectionCard(
            title = "Terms & Conditions",
            icon = Icons.Filled.Shield
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TermsParagraph(
                    "1. Public repositories only.",
                    "Ximanta GitFetcher uses GitHub's public API. Private or " +
                            "authenticated repositories cannot be accessed."
                )
                TermsParagraph(
                    "2. Rate limits apply.",
                    "The GitHub API allows 60 unauthenticated requests per hour " +
                            "per network. Heavy usage may temporarily block fetching."
                )
                TermsParagraph(
                    "3. You are responsible for downloaded content.",
                    "Do not redistribute copyrighted code. Respect each " +
                            "repository's license."
                )
                TermsParagraph(
                    "4. No warranty.",
                    "This app is provided as-is. Ximanta is not liable for any " +
                            "damages arising from its use."
                )
                TermsParagraph(
                    "5. Storage access.",
                    "To save files, the app requires permission to write to your " +
                            "Downloads folder."
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- Checkbox row ---------------------------------------------
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = { accepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "I have read and agree to the Terms & Conditions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---------- AGREE button ---------------------------------------------
        Button(
            onClick = onAgree,
            enabled = accepted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (accepted) "AGREE & CONTINUE" else "TICK TO AGREE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---------- Footer ---------------------------------------------------
        BrandFooter()
        Spacer(Modifier.height(12.dp))

        // Extra breathing room so the scroll fling feels nice.
        Box(modifier = Modifier.height(8.dp))
    }
}

/**
 * A numbered terms paragraph — bold title, regular body below.
 */
@Composable
private fun TermsParagraph(
    title: String,
    body: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
    }
}
