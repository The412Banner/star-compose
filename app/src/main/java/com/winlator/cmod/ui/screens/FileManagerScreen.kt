package com.winlator.cmod.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.winlator.cmod.ui.theme.OnSurfaceVariant

/**
 * Placeholder — File Manager screen (Phase 2 will replace with full Compose implementation).
 */
@Composable
fun FileManagerScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "File Manager — coming soon", color = OnSurfaceVariant)
    }
}
