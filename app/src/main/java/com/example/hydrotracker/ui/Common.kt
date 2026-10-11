package com.example.hydrotracker.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.hydrotracker.logic.HealthTip
import com.example.hydrotracker.logic.TIP_SOURCES
import com.example.hydrotracker.logic.TipSource

@Composable
fun NumberDialog(title: String, label: String, initialText: String,
                 transform: (Int) -> Int = { it },
                 onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    val parsed = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(5) },
                label = { Text(label) },
                isError = parsed == null,
                supportingText = { if (parsed == null) Text("Enter a whole number") },
                singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let { onConfirm(transform(it)) } },
                enabled = parsed != null && parsed > 0) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TipCard(tip: HealthTip, onNext: () -> Unit, modifier: Modifier = Modifier) {
    var showSources by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(onClick = onNext, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tip.emoji, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f)) {
                    Text(tip.title, style = MaterialTheme.typography.titleSmall)
                    Text(tip.text, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next tip",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = { showSources = !showSources },
                contentPadding = PaddingValues()) {
                Text(if (showSources) "Hide sources ▴" else "Sources & further reading ▾",
                    style = MaterialTheme.typography.labelMedium)
            }
            androidx.compose.animation.AnimatedVisibility(visible = showSources) {
                SourceLinks(tip)
            }
        }
    }
}

@Composable
fun SourceLinks(tip: HealthTip, compact: Boolean = false) {
    val sources = TIP_SOURCES[tip.id].orEmpty()
    if (sources.isEmpty()) return
    Column {
        if (!compact) Text("Sources", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        sources.forEach { SourceRow(it, compact) }
    }
}

@Composable
fun SourceRow(source: TipSource, compact: Boolean = false) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    Text(
        "${source.kind.icon} ${source.title}",
        style = if (compact) MaterialTheme.typography.labelSmall
                else MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 2.dp).clickable {
            try { uriHandler.openUri(source.url) }
            catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            }
        }
    )
}
