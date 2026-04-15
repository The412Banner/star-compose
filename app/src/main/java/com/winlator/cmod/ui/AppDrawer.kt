package com.winlator.cmod.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
    Screen.FileManager   -> Icons.Filled.FolderOpen
    Screen.Settings      -> Icons.Filled.Settings
    Screen.Gog, Screen.Epic, Screen.Amazon, Screen.Steam -> Icons.Filled.Storefront
    else                 -> Icons.Filled.FolderOpen
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
            .background(Surface),
    ) {
        Text(
            text = "Winlator",
            style = MaterialTheme.typography.headlineSmall,
            color = Primary,
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 16.dp),
        )

        Divider(color = DividerColor)

        Screen.drawerItems.forEach { screen ->
            val selected = currentRoute == screen.route
            DrawerItem(
                label = screen.label,
                icon = iconFor(screen),
                selected = selected,
                onClick = { onNavigate(screen) },
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(top = 8.dp))

        Text(
            text = "STORES",
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
        )

        Screen.storeItems.forEach { screen ->
            DrawerItem(
                label = screen.label,
                icon = iconFor(screen),
                selected = false,
                onClick = { onLaunchStore(screen) },
            )
        }

        Divider(color = DividerColor, modifier = Modifier.padding(top = 8.dp))

        DrawerItem(
            label = "About",
            icon = Icons.Filled.Info,
            selected = false,
            onClick = onAbout,
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) Primary.copy(alpha = 0.15f) else Background.copy(alpha = 0f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Primary else OnSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Primary else OnSurface,
        )
    }
}
