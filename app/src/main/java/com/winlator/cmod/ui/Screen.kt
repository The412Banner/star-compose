package com.winlator.cmod.ui

sealed class Screen(val route: String, val label: String, val iconName: String) {
    object Containers    : Screen("containers",     "Containers",     "folder")
    object Shortcuts     : Screen("shortcuts",      "Shortcuts",      "shortcut")
    object Contents      : Screen("contents",       "Contents",       "inventory_2")
    object InputControls : Screen("input_controls", "Input Controls", "sports_esports")
    object AdrenoTools   : Screen("adreno_tools",   "Adreno Tools",   "memory")
    object FileManager   : Screen("file_manager",   "File Manager",   "folder_open")
    object Settings      : Screen("settings",       "Settings",       "settings")

    // Store items — these launch Activities via Intent, not Compose nav routes
    object Gog    : Screen("gog",    "GOG",    "storefront")
    object Epic   : Screen("epic",   "Epic",   "storefront")
    object Amazon : Screen("amazon", "Amazon", "storefront")
    object Steam  : Screen("steam",  "Steam",  "storefront")

    // Sub-screens (not in drawer)
    object ContainerDetail : Screen("container_detail?id={id}", "Container", "")

    companion object {
        val drawerItems by lazy {
            listOf(Containers, Shortcuts, Contents, InputControls, AdrenoTools, FileManager, Settings)
        }
        val storeItems by lazy {
            listOf(Gog, Epic, Amazon, Steam)
        }
    }
}
