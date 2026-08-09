package ai.blueview.weather.ui.screens.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.blueview.weather.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://$url")))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("About", color = TextPrimary) },
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
            Text("Version 1.0.0",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary)
            Text("Floating weather panel with live radar\n7-day forecast · Hourly drill-down",
                style     = MaterialTheme.typography.bodyMedium,
                color     = BlueAccent,
                textAlign = TextAlign.Center)

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = TextMuted.copy(alpha = 0.3f))
            Spacer(Modifier.height(4.dp))

            AboutRow("Author",   "Frank Perez")
            AboutLinkRow("Email",    "frank@blueview.ai")   { openUrl("mailto:frank@blueview.ai") }
            AboutLinkRow("OS",       "bvos.blueview.ai")    { openUrl("bvos.blueview.ai") }
            AboutLinkRow("Paper",    "mypapertrail.co")     { openUrl("mypapertrail.co") }
            AboutLinkRow("Read2Me",  "read2me.co")          { openUrl("read2me.co") }
            AboutLinkRow("Web",      "blueview.ai")         { openUrl("blueview.ai") }

            Spacer(Modifier.height(8.dp))
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = TextMuted, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = BlueAccent)
        Icon(Icons.Default.OpenInBrowser, null, tint = TextMuted,
            modifier = Modifier.size(14.dp))
    }
}
