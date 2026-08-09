package ai.blueview.weather.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ai.blueview.weather.BuildConfig
import ai.blueview.weather.data.update.UpdateState
import ai.blueview.weather.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val update by viewModel.update.collectAsStateWithLifecycle()

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        },
        containerColor = NavyDeep
    ) { padding ->
        Column(
            modifier            = Modifier
                .padding(padding)
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🌤", fontSize = 56.sp)
            Text("Blue View Weather",
                style     = MaterialTheme.typography.headlineLarge,
                color     = TextPrimary,
                textAlign = TextAlign.Center)
            Text("Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary)
            Text("Floating weather panel with live radar\n7-day forecast · Hourly drill-down",
                style     = MaterialTheme.typography.bodyMedium,
                color     = BlueAccent,
                textAlign = TextAlign.Center)

            Spacer(Modifier.height(4.dp))
            UpdateSection(update, viewModel, ::openUrl)
            Spacer(Modifier.height(4.dp))

            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            AboutRow("Author",  "Frank Perez")
            AboutLinkRow("Email",   "frank@blueview.ai")  { openUrl("mailto:frank@blueview.ai") }
            AboutLinkRow("OS",      "bvos.blueview.ai")   { openUrl("https://bvos.blueview.ai") }
            AboutLinkRow("Paper",   "mypapertrail.co")    { openUrl("https://mypapertrail.co") }
            AboutLinkRow("Read2Me", "read2me.co")         { openUrl("https://read2me.co") }
            AboutLinkRow("Web",     "blueview.ai")        { openUrl("https://blueview.ai") }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            Text("Powered by Open-Meteo · RainViewer",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextMuted,
                textAlign = TextAlign.Center)
            Text("© 2026 BlueView / Frank Perez",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextMuted,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun UpdateSection(
    update: UpdateState,
    viewModel: AboutViewModel,
    openUrl: (String) -> Unit
) {
    when (update) {
        is UpdateState.Idle -> {
            OutlinedButton(
                onClick  = viewModel::checkForUpdate,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Check for Updates") }
        }
        is UpdateState.Checking -> {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = BlueAccent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Checking…", color = TextSecondary)
            }
        }
        is UpdateState.UpToDate -> {
            Text("✓  You're on the latest version",
                color = SuccessGreen,
                style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = viewModel::reset) {
                Text("Check again", color = TextMuted)
            }
        }
        is UpdateState.Available -> {
            Card(
                colors   = CardDefaults.cardColors(containerColor = NavyCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Update available: ${update.version}",
                        color = BlueAccent,
                        style = MaterialTheme.typography.titleSmall)
                    Text("Tap below to download — your browser will open and Android will offer to install it when the download finishes.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick  = { openUrl(update.url) },
                        colors   = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Download ${update.version}", color = NavyDeep) }
                }
            }
        }
        is UpdateState.Error -> {
            Text("⚠  ${update.message}",
                color = ErrorRed,
                style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = viewModel::reset) {
                Text("Try again", color = TextMuted)
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = TextMuted, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AboutLinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = TextMuted, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = BlueAccent)
        Icon(Icons.Default.OpenInBrowser, null, tint = TextMuted,
            modifier = Modifier.size(14.dp))
    }
}
