package com.winlator.cmod.ui.screens

import android.app.Application
import android.content.Context
import android.content.pm.ShortcutManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.container.Shortcut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Collections

class ShortcutsViewModel(app: Application) : AndroidViewModel(app) {

    private val _shortcuts = MutableStateFlow<List<Shortcut>>(emptyList())
    val shortcuts: StateFlow<List<Shortcut>> = _shortcuts

    private val manager = ContainerManager(app)

    init {
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
