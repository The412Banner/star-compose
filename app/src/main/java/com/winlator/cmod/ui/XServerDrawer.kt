package com.winlator.cmod.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import com.winlator.cmod.R
import com.winlator.cmod.ui.theme.WinlatorTheme

fun setupComposeView(view: ComposeView) {
    view.setContent {
        WinlatorTheme {
            XServerDrawer()
        }
    }
}

@Composable
fun XServerDrawer() {
    val state = XServerDrawerState

    val isPaused            by state.isPaused.collectAsState()
    val isRelativeMouse     by state.isRelativeMouseMovement.collectAsState()
    val isMouseDisabled     by state.isMouseDisabled.collectAsState()
    val moveCursorToTouch   by state.moveCursorToTouchpoint.collectAsState()
    val showLogs            by state.showLogs.collectAsState()
    val showMagnifier       by state.showMagnifier.collectAsState()
    val cursorExpanded      by state.cursorExpanded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Bionic Star",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Keyboard ──────────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_keyboard,
            label = "Keyboard",
            onClick = { state.onKeyboard?.invoke(); state.onClose?.invoke() },
        )

        // ── Input Controls ────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_input_controls,
            label = "Input Controls",
            onClick = { state.onInputControls?.invoke(); state.onClose?.invoke() },
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Mouse & Cursor header (collapsible) ───────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { state.toggleCursorExpanded() }
                .padding(horizontal = 20.dp, vertical = 13.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.cursor),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = "Mouse and Cursor",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (cursorExpanded) Icons.Filled.ExpandLess else Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = cursorExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                DrawerCheckItem(
                    label = "Move Cursor to Touchpoint",
                    checked = moveCursorToTouch,
                    onClick = { state.onMoveCursorToTouchpoint?.invoke(); state.onClose?.invoke() },
                )
                DrawerCheckItem(
                    label = "Relative Mouse Movement",
                    checked = isRelativeMouse,
                    onClick = { state.onRelativeMouseMovement?.invoke(); state.onClose?.invoke() },
                )
                DrawerCheckItem(
                    label = "Disable Mouse",
                    checked = isMouseDisabled,
                    onClick = { state.onDisableMouse?.invoke(); state.onClose?.invoke() },
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Screen Effects ────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_screen_effect,
            label = "Screen Effects",
            onClick = { state.onScreenEffects?.invoke(); state.onClose?.invoke() },
        )

        // ── Graphic Engine ────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_settings,
            label = "Graphic Engine",
            onClick = { state.onGraphicEngine?.invoke(); state.onClose?.invoke() },
        )

        // ── Vibration ─────────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_input_controls,
            label = "Vibration",
            onClick = { state.onVibration?.invoke(); state.onClose?.invoke() },
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Toggle Fullscreen ─────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_fullscreen,
            label = "Toggle Fullscreen",
            onClick = { state.onToggleFullscreen?.invoke(); state.onClose?.invoke() },
        )

        // ── Pause / Resume ────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = if (isPaused) R.drawable.icon_play else R.drawable.icon_pause,
            label = if (isPaused) "Resume" else "Pause",
            onClick = { state.onPauseResume?.invoke(); state.onClose?.invoke() },
        )

        // ── Picture in Picture ────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.ic_picture_in_picture_alt,
            label = "Picture in Picture",
            onClick = { state.onPipMode?.invoke(); state.onClose?.invoke() },
        )

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Active Windows ────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_active_windows,
            label = "Active Windows",
            onClick = { state.onActiveWindows?.invoke(); state.onClose?.invoke() },
        )

        // ── Task Manager ──────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_task_manager,
            label = "Task Manager",
            onClick = { state.onTaskManager?.invoke(); state.onClose?.invoke() },
        )

        // ── Magnifier (conditional) ───────────────────────────────────────────
        if (showMagnifier) {
            DrawerMenuItem(
                iconRes = R.drawable.icon_magnifier,
                label = "Magnifier",
                onClick = { state.onMagnifier?.invoke(); state.onClose?.invoke() },
            )
        }

        // ── Logs (conditional) ────────────────────────────────────────────────
        if (showLogs) {
            DrawerMenuItem(
                iconRes = R.drawable.icon_debug,
                label = "Logs",
                onClick = { state.onLogs?.invoke(); state.onClose?.invoke() },
            )
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // ── Exit ──────────────────────────────────────────────────────────────
        DrawerMenuItem(
            iconRes = R.drawable.icon_exit,
            label = "Exit",
            onClick = { state.onExit?.invoke() },
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerMenuItem(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DrawerCheckItem(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 56.dp, end = 20.dp, top = 11.dp, bottom = 11.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
