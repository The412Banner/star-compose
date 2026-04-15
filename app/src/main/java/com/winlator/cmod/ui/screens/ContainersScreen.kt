package com.winlator.cmod.ui.screens

import android.app.Activity
import android.content.Intent
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winlator.cmod.XServerDisplayActivity
import com.winlator.cmod.XrActivity
import com.winlator.cmod.container.Container
import com.winlator.cmod.contentdialog.StorageInfoDialog
import com.winlator.cmod.ui.theme.Divider as DividerColor
import com.winlator.cmod.ui.theme.OnSurface
import com.winlator.cmod.ui.theme.OnSurfaceVariant
import com.winlator.cmod.ui.theme.Primary
import com.winlator.cmod.ui.theme.Surface
import com.winlator.cmod.xenvironment.ImageFs

@Composable
fun ContainersScreen(
    onNavigateToDetail: (containerId: Int?) -> Unit,
    vm: ContainersViewModel = viewModel(),
) {
    val containers by vm.containers.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    // Refresh list whenever this screen resumes (e.g. returning from ContainerDetail)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Confirm-dialog state
    var confirmDialog by remember { mutableStateOf<ConfirmAction?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (containers.isEmpty() && !isLoading) {
            Text(
                text = "No containers yet. Tap + to create one.",
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(containers, key = { it.id }) { container ->
                    ContainerItem(
                        container = container,
                        onRun = {
                            if (!XrActivity.isEnabled(context)) {
                                val intent = Intent(context, XServerDisplayActivity::class.java)
                                intent.putExtra("container_id", container.id)
                                context.startActivity(intent)
                            } else {
                                XrActivity.openIntent(activity, container.id, null)
                            }
                        },
                        onEdit = { onNavigateToDetail(container.id) },
                        onDuplicate = {
                            confirmDialog = ConfirmAction.Duplicate(container)
                        },
                        onRemove = {
                            confirmDialog = ConfirmAction.Remove(container)
                        },
                        onInfo = {
                            // StorageInfoDialog is still a Java dialog — needs Activity
                            StorageInfoDialog(activity, container).show()
                        },
                    )
                    Divider(color = DividerColor)
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (ImageFs.find(context).isValid()) onNavigateToDetail(null)
            },
            containerColor = Primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add container", tint = androidx.compose.ui.graphics.Color.White)
        }

        // Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
    }

    // Confirm dialogs
    confirmDialog?.let { action ->
        when (action) {
            is ConfirmAction.Duplicate -> {
                AlertDialog(
                    onDismissRequest = { confirmDialog = null },
                    title = { Text("Duplicate container?") },
                    text = { Text("Duplicate \"${action.container.name}\"?") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDialog = null
                            vm.duplicate(action.container) {}
                        }) { Text("Duplicate") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDialog = null }) { Text("Cancel") }
                    },
                )
            }
            is ConfirmAction.Remove -> {
                AlertDialog(
                    onDismissRequest = { confirmDialog = null },
                    title = { Text("Remove container?") },
                    text = { Text("Remove \"${action.container.name}\" permanently?") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDialog = null
                            vm.remove(action.container, context) {}
                        }) { Text("Remove") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDialog = null }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}

@Composable
private fun ContainerItem(
    container: Container,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit,
    onInfo: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(36.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = container.name,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
            modifier = Modifier.weight(1f),
        )
        // Run button
        IconButton(onClick = onRun) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Run",
                tint = Primary,
            )
        }
        // 3-dot menu
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = OnSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                    onClick = { menuExpanded = false; onEdit() },
                )
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, null) },
                    onClick = { menuExpanded = false; onDuplicate() },
                )
                DropdownMenuItem(
                    text = { Text("Remove") },
                    leadingIcon = { Icon(Icons.Filled.Delete, null) },
                    onClick = { menuExpanded = false; onRemove() },
                )
                DropdownMenuItem(
                    text = { Text("Info") },
                    leadingIcon = { Icon(Icons.Filled.Info, null) },
                    onClick = { menuExpanded = false; onInfo() },
                )
            }
        }
    }
}

private sealed class ConfirmAction {
    data class Duplicate(val container: Container) : ConfirmAction()
    data class Remove(val container: Container) : ConfirmAction()
}
