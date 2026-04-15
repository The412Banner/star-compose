package com.winlator.cmod.ui.screens

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ContainersViewModel(app: Application) : AndroidViewModel(app) {

    private val _containers = MutableStateFlow<List<Container>>(emptyList())
    val containers: StateFlow<List<Container>> = _containers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val manager: ContainerManager = ContainerManager(app)

    init {
        refresh()
    }

    fun refresh() {
        manager.reload()
        _containers.value = manager.getContainers().toList()
    }

    fun duplicate(container: Container, onDone: () -> Unit) {
        _isLoading.value = true
        // duplicateContainerAsync posts its callback on the main Handler internally
        manager.duplicateContainerAsync(container) {
            _isLoading.value = false
            refresh()
            onDone()
        }
    }

    fun remove(container: Container, context: Context, onDone: () -> Unit) {
        // Disable any home-screen shortcuts pinned for this container before removing it
        manager.loadShortcuts()
            .filter { it.container == container }
            .forEach { ShortcutsViewModel.disableOnScreen(context, it) }

        _isLoading.value = true
        // removeContainerAsync posts its callback on the main Handler internally
        manager.removeContainerAsync(container) {
            _isLoading.value = false
            refresh()
            onDone()
        }
    }
}
