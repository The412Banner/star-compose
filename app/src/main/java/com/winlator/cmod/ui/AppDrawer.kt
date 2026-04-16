package com.winlator.cmod.ui

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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.cmod.R
import com.winlator.cmod.ui.theme.Background
import com.winlator.cmod.ui.theme.Divider as DividerColor
import com.winlator.cmod.ui.theme.OnSurface
import com.winlator.cmod.ui.theme.OnSurfaceVariant
import com.winlator.cmod.ui.theme.Primary
import com.winlator.cmod.ui.theme.Surface

private fun iconFor(screen: Screen): ImageVector = when (screen) {
    Screen.Containers    -> Icons.Filled.FolderOpen
    Screen.Shortcuts     -> Icons.Filled.OpenInNew
    Screen.Contents      -> Icons.Filled.Inventory2
    Screen.InputControls -> Icons.Filled.SportsEsports
    Screen.AdrenoTools   -> Icons.Filled.Memory
    Screen.Saves         -> Icons.Filled.Save
    Screen.FileManager   -> Icons.Filled.FolderOpen
    Screen.Settings      -> Icons.Filled.Settings
    else                 -> Icons.Filled.Storefront
}

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigate: (Screen) -> Unit,
    onLaunchStore: (Screen) -> Unit,
    onAbout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Surface)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Logo header ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Bionic Star",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        Divider(color = DividerColor)

        // ── Emulation section ─────────────────────────────────────────────────
        SectionHeader("Emulation")
        DrawerItem(Screen.Shortcuts,     currentRoute, onNavigate)
        DrawerItem(Screen.Containers,    currentRoute, onNavigate)
        DrawerItem(Screen.Settings,      currentRoute, onNavigate)

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        // ── Tools section ─────────────────────────────────────────────────────
        SectionHeader("Tools")
        DrawerItem(Screen.InputControls, currentRoute, onNavigate)
        DrawerItem(Screen.Contents,      currentRoute, onNavigate)
        DrawerItem(Screen.AdrenoTools,   currentRoute, onNavigate)
        DrawerItem(Screen.Saves,         currentRoute, onNavigate)

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        // ── Game Stores section ───────────────────────────────────────────────
        SectionHeader("Game Stores")
        Screen.storeItems.forEach { screen ->
            DrawerStoreItem(screen, onLaunchStore)
        }

        Divider(color = DividerColor, modifier = Modifier.padding(top = 4.dp))

        // ── About And Support section ─────────────────────────────────────────
        SectionHeader("About And Support")
        DrawerIconItem(
            label = "About",
            icon = Icons.Filled.Info,
            onClick = onAbout,
        )
        DrawerIconItem(
            label = "Help and Support",
            icon = Icons.Filled.HelpOutline,
            onClick = { /* TODO: open help URL or dialog */ },
        )

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun DrawerItem(screen: Screen, currentRoute: String, onNavigate: (Screen) -> Unit) {
    val selected = currentRoute == screen.route
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate(screen) }
            .background(if (selected) Primary.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = iconFor(screen),
            contentDescription = null,
            tint = if (selected) Primary else OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = screen.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Primary else OnSurface,
        )
    }
}

@Composable
private fun DrawerStoreItem(screen: Screen, onLaunchStore: (Screen) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLaunchStore(screen) }
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Storefront,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = screen.label,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
        )
    }
}

@Composable
private fun DrawerIconItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurface,
        )
    }
}
