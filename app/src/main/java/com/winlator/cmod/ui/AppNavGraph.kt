package com.winlator.cmod.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.winlator.cmod.ContainerDetailFragment
import com.winlator.cmod.InputControlsFragment
import com.winlator.cmod.SavesFragment
import com.winlator.cmod.SettingsFragment
import com.winlator.cmod.ui.screens.AdrenoToolsScreen
import com.winlator.cmod.ui.screens.ContainersScreen
import com.winlator.cmod.ui.screens.ContentsScreen
import com.winlator.cmod.ui.screens.FileManagerScreen
import com.winlator.cmod.ui.screens.FragmentScreen
import com.winlator.cmod.ui.screens.ShortcutsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    selectedInputProfileId: Int,
    startRoute: String = Screen.Containers.route,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as FragmentActivity

    NavHost(navController = navController, startDestination = startRoute, modifier = modifier) {

        composable(Screen.Containers.route) {
            ContainersScreen(
                onNavigateToDetail = { containerId ->
                    val route = if (containerId != null) {
                        "container_detail?id=$containerId"
                    } else {
                        "container_detail?id=-1"
                    }
                    navController.navigate(route)
                },
            )
        }

        composable(
            route = "container_detail?id={id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: -1
            FragmentScreen(activity = activity) {
                if (id == -1) ContainerDetailFragment() else ContainerDetailFragment(id)
            }
        }

        composable(Screen.Shortcuts.route) {
            ShortcutsScreen()
        }

        composable(Screen.Contents.route) {
            ContentsScreen()
        }

        composable(Screen.InputControls.route) {
            FragmentScreen(activity = activity) { InputControlsFragment(selectedInputProfileId) }
        }

        composable(Screen.AdrenoTools.route) {
            AdrenoToolsScreen()
        }

        composable(Screen.FileManager.route) {
            FileManagerScreen()
        }

        composable(Screen.Settings.route) {
            FragmentScreen(activity = activity) { SettingsFragment() }
        }

        composable(Screen.Saves.route) {
            FragmentScreen(activity = activity) { SavesFragment() }
        }
    }
}
