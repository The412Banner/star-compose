package com.winlator.cmod.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView

/**
 * Hosts a legacy [Fragment] inside a Compose NavHost destination.
 *
 * Each call creates a unique container ID so that multiple fragment
 * destinations can coexist in the back stack without ID collisions.
 */
@Composable
fun FragmentScreen(
    activity: FragmentActivity,
    fragmentFactory: () -> Fragment,
) {
    val containerId = remember { android.view.View.generateViewId() }

    DisposableEffect(containerId) {
        val fragment = fragmentFactory()
        val fm = activity.supportFragmentManager
        fm.beginTransaction()
            .replace(containerId, fragment)
            .commit()

        onDispose {
            val existing = fm.findFragmentById(containerId)
            if (existing != null) {
                fm.beginTransaction().remove(existing).commitAllowingStateLoss()
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            FragmentContainerView(ctx).apply { id = containerId }
        },
    )
}
