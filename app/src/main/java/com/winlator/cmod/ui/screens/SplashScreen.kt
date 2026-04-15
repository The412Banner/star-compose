package com.winlator.cmod.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.cmod.R

/**
 * Full-screen overlay shown while the system image is being installed on first launch.
 * Displayed on top of the main app shell; disappears when [progress] is no longer relevant
 * (controlled by [SplashViewModel.isInstalling]).
 */
@Composable
fun SplashScreen(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Winlator",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Text(
                text = "Installing system files",
                fontSize = 13.sp,
                color = Color(0xFF888888),
            )

            Spacer(Modifier.height(32.dp))

            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFCCCCCC),
                trackColor = Color(0xFF333333),
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "$progress%",
                fontSize = 13.sp,
                color = Color(0xFFAAAAAA),
            )
        }
    }
}
