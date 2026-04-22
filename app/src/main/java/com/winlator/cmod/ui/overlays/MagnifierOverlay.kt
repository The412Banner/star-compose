package com.winlator.cmod.ui.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import com.winlator.cmod.ui.XServerDialogState

@Composable
fun MagnifierOverlay(state: XServerDialogState) {
    val zoom by state.magnifierZoom.collectAsState()

    var offsetX by remember { mutableFloatStateOf(100f) }
    var offsetY by remember { mutableFloatStateOf(100f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Magnifier",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(zoom * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { state.onMagnifierZoom?.invoke(-0.25f) }) {
                        Text("−")
                    }
                    Spacer(Modifier.width(4.dp))
                    FilledTonalButton(onClick = { state.onMagnifierZoom?.invoke(0.25f) }) {
                        Text("+")
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = { state.onMagnifierHide?.run() }) {
                        Text("✕", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
