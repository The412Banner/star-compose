package com.winlator.cmod.ui.screens

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.container.Shortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

enum class ShortcutSortOrder { NAME_ASC, NAME_DESC, CONTAINER }

class ShortcutsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("shortcuts_prefs", Context.MODE_PRIVATE)

    private val _shortcuts = MutableStateFlow<List<Shortcut>>(emptyList())

    private val _sortOrder = MutableStateFlow(
        ShortcutSortOrder.entries[
            prefs.getInt("sort_order", ShortcutSortOrder.NAME_ASC.ordinal)
                .coerceIn(0, ShortcutSortOrder.entries.size - 1)
        ]
    )
    val sortOrder: StateFlow<ShortcutSortOrder> = _sortOrder

    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", false))
    val isGridView: StateFlow<Boolean> = _isGridView

    val shortcuts: kotlinx.coroutines.flow.Flow<List<Shortcut>> =
        combine(_shortcuts, _sortOrder) { list, order ->
            when (order) {
                ShortcutSortOrder.NAME_ASC   -> list.sortedBy { it.name.lowercase() }
                ShortcutSortOrder.NAME_DESC  -> list.sortedByDescending { it.name.lowercase() }
                ShortcutSortOrder.CONTAINER  -> list.sortedBy { (it.container?.name ?: "").lowercase() }
            }
        }

    private val manager = ContainerManager(app)

    init {
        refresh()
    }

    fun setSortOrder(order: ShortcutSortOrder) {
        _sortOrder.value = order
        prefs.edit().putInt("sort_order", order.ordinal).apply()
    }

    fun setGridView(grid: Boolean) {
        _isGridView.value = grid
        prefs.edit().putBoolean("is_grid_view", grid).apply()
    }

    fun importShortcut(containerIndex: Int, uri: Uri, context: Context) {
        val containers = manager.getContainers()
        if (containerIndex >= containers.size) return
        val container = containers[containerIndex]
        val destDir = container.getDesktopDir()
        if (!destDir.exists()) destDir.mkdirs()
        val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: "imported.desktop"
        val dest = File(destDir, fileName)
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            val lines = dest.readLines().map { line ->
                if (line.startsWith("container_id:")) "container_id:${container.id}" else line
            }
            dest.writeText(lines.joinToString("\n") + "\n")
        }
        refresh()
    }

    fun refresh() {
        val raw = manager.loadShortcuts()
        // filter out corrupted entries (matches original Fragment logic)
        _shortcuts.value = raw.filter { it != null && it.file != null && it.file.name.isNotEmpty() }
    }

    fun remove(shortcut: Shortcut, context: Context): Boolean {
        val deleted = shortcut.file.delete()
        val lnkPath = shortcut.file.path.substringBeforeLast('.') + ".lnk"
        val lnk = File(lnkPath)
        if (lnk.exists()) lnk.delete()
        if (deleted) {
            disableOnScreen(context, shortcut)
            refresh()
        }
        return deleted
    }

    fun cloneToContainer(shortcut: Shortcut, containerIndex: Int): Boolean {
        val containers = manager.getContainers()
        if (containerIndex >= containers.size) return false
        val result = shortcut.cloneToContainer(containers[containerIndex])
        if (result) refresh()
        return result
    }

    fun containers() = manager.getContainers()

    companion object {
        fun disableOnScreen(context: Context, shortcut: Shortcut) {
            try {
                val sm = ContextCompat.getSystemService(context, ShortcutManager::class.java)
                sm?.disableShortcuts(
                    Collections.singletonList(shortcut.getExtra("uuid")),
                    context.getString(com.winlator.cmod.R.string.shortcut_not_available),
                )
            } catch (_: Exception) {}
        }
    }
}
