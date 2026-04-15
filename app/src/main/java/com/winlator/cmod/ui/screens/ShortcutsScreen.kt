package com.winlator.cmod.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.winlator.cmod.SettingsFragment
import com.winlator.cmod.XServerDisplayActivity
import com.winlator.cmod.XrActivity
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.Shortcut
import com.winlator.cmod.contentdialog.ShortcutSettingsDialog
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.ui.theme.Divider as DividerColor
import com.winlator.cmod.ui.theme.OnSurface
import com.winlator.cmod.ui.theme.OnSurfaceVariant
import com.winlator.cmod.ui.theme.Primary
import com.winlator.cmod.ui.theme.Surface
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileWriter
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

@Composable
fun ShortcutsScreen(vm: ShortcutsViewModel = viewModel()) {
    val shortcuts by vm.shortcuts.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    var confirmRemove by remember { mutableStateOf<Shortcut?>(null) }
    var cloneTarget by remember { mutableStateOf<Shortcut?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shortcuts.isEmpty()) {
            Text(
                text = "No shortcuts yet.",
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(shortcuts, key = { it.file.path }) { shortcut ->
                    ShortcutItem(
                        shortcut = shortcut,
                        onRun = { runShortcut(activity, shortcut) },
                        onSettings = {
                            ShortcutSettingsDialog(context, shortcut) { vm.refresh() }.show()
                        },
                        onRemove = { confirmRemove = shortcut },
                        onClone = { cloneTarget = shortcut },
                        onAddToHome = { addToHomeScreen(context, shortcut) },
                        onExport = { exportShortcut(context, shortcut) },
                        onProperties = { showProperties(context, shortcut) },
                    )
                    Divider(color = DividerColor)
                }
            }
        }
    }

    // Remove confirmation
    confirmRemove?.let { s ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove shortcut?") },
            text = { Text("Remove \"${s.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = vm.remove(s, context)
                    confirmRemove = null
                    Toast.makeText(
                        context,
                        if (ok) "Shortcut removed." else "Failed to remove shortcut.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } },
        )
    }

    // Clone-to-container dialog
    cloneTarget?.let { s ->
        val containers = vm.containers()
        AlertDialog(
            onDismissRequest = { cloneTarget = null },
            title = { Text("Select container") },
            text = {
                Column {
                    containers.forEach { c ->
                        Text(
                            text = c.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val ok = s.cloneToContainer(c)
                                    cloneTarget = null
                                    Toast.makeText(
                                        context,
                                        if (ok) "Shortcut cloned." else "Failed to clone shortcut.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    if (ok) vm.refresh()
                                }
                                .padding(vertical = 12.dp),
                            color = OnSurface,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { cloneTarget = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShortcutItem(
    shortcut: Shortcut,
    onRun: () -> Unit,
    onSettings: () -> Unit,
    onRemove: () -> Unit,
    onClone: () -> Unit,
    onAddToHome: () -> Unit,
    onExport: () -> Unit,
    onProperties: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .clickable(onClick = onRun)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Icon: Bitmap from shortcut or fallback
        if (shortcut.icon != null) {
            androidx.compose.foundation.Image(
                bitmap = shortcut.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.OpenInNew,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = shortcut.name, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
            Text(text = shortcut.container.name, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        // 3-dot menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = OnSurfaceVariant)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Settings") },
                    leadingIcon = { Icon(Icons.Filled.Settings, null) },
                    onClick = { menuExpanded = false; onSettings() },
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                    onClick = { menuExpanded = false; onRemove() },
                )
                DropdownMenuItem(
                    text = { Text("Clone to container") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = { menuExpanded = false; onClone() },
                )
                DropdownMenuItem(
                    text = { Text("Add to home screen") },
                    leadingIcon = { Icon(Icons.Filled.AddToHomeScreen, null) },
                    onClick = { menuExpanded = false; onAddToHome() },
                )
                DropdownMenuItem(
                    text = { Text("Export") },
                    leadingIcon = { Icon(Icons.Filled.Upload, null) },
                    onClick = { menuExpanded = false; onExport() },
                )
                DropdownMenuItem(
                    text = { Text("Properties") },
                    leadingIcon = { Icon(Icons.Filled.Info, null) },
                    onClick = { menuExpanded = false; onProperties() },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Non-composable helpers (ported from ShortcutsFragment inner methods)
// ---------------------------------------------------------------------------

private fun runShortcut(activity: Activity, shortcut: Shortcut) {
    if (!XrActivity.isEnabled(activity)) {
        val intent = Intent(activity, XServerDisplayActivity::class.java).apply {
            putExtra("container_id", shortcut.container.id)
            putExtra("shortcut_path", shortcut.file.path)
            putExtra("shortcut_name", shortcut.name)
            putExtra("disableXinput", shortcut.getExtra("disableXinput", "0"))
        }
        activity.startActivity(intent)
    } else {
        XrActivity.openIntent(activity, shortcut.container.id, shortcut.file.path)
    }
}

private fun addToHomeScreen(context: Context, shortcut: Shortcut) {
    if (shortcut.getExtra("uuid").isEmpty()) shortcut.genUUID()
    try {
        val sm = ContextCompat.getSystemService(context, ShortcutManager::class.java)
        if (sm != null && sm.isRequestPinShortcutSupported) {
            val intent = Intent(context, XServerDisplayActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra("container_id", shortcut.container.id)
                putExtra("shortcut_path", shortcut.file.path)
            }
            val info = ShortcutInfo.Builder(context, shortcut.getExtra("uuid"))
                .setShortLabel(shortcut.name)
                .setLongLabel(shortcut.name)
                .setIcon(Icon.createWithBitmap(shortcut.icon))
                .setIntent(intent)
                .build()
            sm.requestPinShortcut(info, null)
        }
    } catch (_: Exception) {}
}

private fun exportShortcut(context: Context, shortcut: Shortcut) {
    val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val uriString = prefs.getString("shortcuts_export_path_uri", null)

    val shortcutsDir: File = if (uriString != null) {
        val folderUri = Uri.parse(uriString)
        val pickedDir = DocumentFile.fromTreeUri(context, folderUri)
        if (pickedDir == null || !pickedDir.canWrite()) {
            Toast.makeText(context, "Cannot write to the selected folder", Toast.LENGTH_SHORT).show()
            return
        }
        File(FileUtils.getFilePathFromUri(context, folderUri))
    } else {
        File(SettingsFragment.DEFAULT_SHORTCUT_EXPORT_PATH)
    }

    if (!shortcutsDir.exists() && !shortcutsDir.mkdirs()) {
        Toast.makeText(context, "Failed to create default directory", Toast.LENGTH_SHORT).show()
        return
    }

    val exportFile = File(shortcutsDir, shortcut.file.name)
    val fileExists = exportFile.exists()

    try {
        val lines = mutableListOf<String>()
        var containerIdFound = false
        BufferedReader(FileReader(shortcut.file)).use { reader ->
            reader.lineSequence().forEach { line ->
                if (line.startsWith("container_id:")) {
                    lines += "container_id:${shortcut.container.id}"
                    containerIdFound = true
                } else {
                    lines += line
                }
            }
        }
        if (!containerIdFound) lines += "container_id:${shortcut.container.id}"

        FileWriter(exportFile, false).use { w ->
            lines.forEach { w.write("$it\n") }
        }

        Toast.makeText(
            context,
            if (fileExists) "Shortcut updated at ${exportFile.path}" else "Shortcut exported to ${exportFile.path}",
            Toast.LENGTH_LONG,
        ).show()
    } catch (_: IOException) {
        Toast.makeText(context, "Failed to export shortcut", Toast.LENGTH_LONG).show()
    }
}

private fun showProperties(context: Context, shortcut: Shortcut) {
    val playtimePrefs = context.getSharedPreferences("playtime_stats", Context.MODE_PRIVATE)
    val playtimeKey = "${shortcut.name}_playtime"
    val playCountKey = "${shortcut.name}_play_count"
    val totalMs = playtimePrefs.getLong(playtimeKey, 0L)
    val playCount = playtimePrefs.getInt(playCountKey, 0)

    val seconds = (totalMs / 1000) % 60
    val minutes = (totalMs / (1000 * 60)) % 60
    val hours   = (totalMs / (1000 * 60 * 60)) % 24
    val days    = (totalMs / (1000 * 60 * 60 * 24))
    val formatted = String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds)

    // Properties still uses the legacy ContentDialog XML layout — show it directly
    val dialog = com.winlator.cmod.contentdialog.ContentDialog(context, com.winlator.cmod.R.layout.shortcut_properties_dialog)
    dialog.setTitle("Properties")
    (dialog.findViewById<android.widget.TextView>(com.winlator.cmod.R.id.play_count))
        .setText("Number of times played: $playCount")
    (dialog.findViewById<android.widget.TextView>(com.winlator.cmod.R.id.playtime))
        .setText("Playtime: $formatted")
    dialog.findViewById<android.widget.Button>(com.winlator.cmod.R.id.reset_properties)
        .setOnClickListener {
            playtimePrefs.edit().remove(playtimeKey).remove(playCountKey).apply()
            Toast.makeText(context, "Properties reset.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    dialog.show()
}
