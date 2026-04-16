# Star-Compose: Jetpack Compose Migration Report & Developer Guide

**Repo:** https://github.com/The412Banner/star-compose  
**Branch:** `main`  
**Final commit:** `6537038`  
**Date:** 2026-04-16  
**Last updated:** 2026-04-16

---

## Overview

Full replacement of the XML/Java UI layer with Jetpack Compose + Material 3. The Wine engine, JNI, Box64/FEX, and container logic were left completely untouched. Every navigation screen and every dialog triggered from a Compose screen is now native Compose.

This document serves two purposes:
1. A complete record of what was replaced and with what
2. A practical guide any Winlator fork developer can follow to do the same

---

## Part A — Developer Guide (How To Do This Yourself)

### A1. Gradle Setup

Add Compose to your existing `build.gradle` (app module) without breaking NDK/JNI:

```groovy
android {
    buildFeatures {
        compose true
        viewBinding true   // keep this — some Java code still uses it
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Compose BOM — pins all Compose library versions together
    def composeBom = platform('androidx.compose:compose-bom:2024.06.00')
    implementation composeBom
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.activity:activity-compose:1.9.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0'
    implementation 'androidx.navigation:navigation-compose:2.7.7'
    debugImplementation 'androidx.compose.ui:ui-tooling'
}
```

**Important:** Do not remove `minifyEnabled`, `ndk`, or any JNI config — Compose sits entirely in the UI layer and doesn't touch those.

---

### A2. Order of Operations

Convert in this order. Going outermost → inward avoids situations where you're converting a dialog that depends on a screen you haven't converted yet.

1. **MainActivity** — replace XML drawer + nav graph with Compose `NavHost` + `ModalNavigationDrawer`
2. **Simple list screens first** — Containers, Shortcuts, Contents (these have the most predictable Fragment → Screen mapping)
3. **Complex detail screens** — ContainerDetail (many tabs + sub-dialogs)
4. **Dialogs belonging to each screen** — convert these as part of the same PR as the screen that owns them
5. **Shared dialogs** — promote to `internal fun` so both screens can use them
6. **Cleanup** — delete dead Java files only after confirming CI builds clean

---

### A3. Fragment → Screen Pattern

Every `Fragment` becomes a `@Composable` function + a `ViewModel`. The ViewModel holds the data, the Composable holds the UI.

**Before (Java Fragment):**
```java
public class ContainersFragment extends Fragment {
    private ContainerManager containerManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.containers_fragment, container, false);
        ListView listView = view.findViewById(R.id.LVContainers);
        // populate list...
        return view;
    }
}
```

**After (Kotlin Compose):**
```kotlin
// ViewModel
class ContainersViewModel(application: Application) : AndroidViewModel(application) {
    private val _containers = MutableStateFlow<List<Container>>(emptyList())
    val containers: StateFlow<List<Container>> = _containers

    fun refresh() {
        _containers.value = ContainerManager(getApplication()).containers
    }
}

// Screen
@Composable
fun ContainersScreen(vm: ContainersViewModel = viewModel()) {
    val containers by vm.containers.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn {
        items(containers) { container ->
            ContainerItem(container)
            Divider()
        }
    }
}
```

---

### A4. ContentDialog → AlertDialog Pattern

Almost every dialog in Winlator extends `ContentDialog`. The replacement is always a Compose `AlertDialog` driven by a state variable.

**Before (Java):**
```java
ContentDialog dialog = new ContentDialog(context, R.layout.my_dialog);
dialog.setTitle("Confirm");
dialog.findViewById(R.id.BTConfirm).setOnClickListener(v -> {
    doSomething();
    dialog.dismiss();
});
dialog.show();
```

**After (Compose):**
```kotlin
var showDialog by remember { mutableStateOf(false) }

Button(onClick = { showDialog = true }) { Text("Open") }

if (showDialog) {
    AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text("Confirm") },
        text = { Text("Are you sure?") },
        confirmButton = {
            TextButton(onClick = { doSomething(); showDialog = false }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog = false }) { Text("Cancel") }
        }
    )
}
```

The key insight: **the dialog is always in the composition tree, shown/hidden by a state variable**. There is no `dialog.show()` or `dialog.dismiss()` call.

---

### A5. Full-Width / Full-Height Dialog

The standard `AlertDialog` is constrained to ~280dp wide by Material. For settings dialogs that need more space (like ShortcutSettings with tabs), use `Dialog` directly with `DialogProperties`:

```kotlin
Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f),
        shape = RoundedCornerShape(12.dp)
    ) {
        // your content — TabRow, scrollable Column, etc.
    }
}
```

---

### A6. Async Data Loading (Spinners / Dropdowns)

Winlator spinners often load data from disk (Box64 versions, FEXCore versions, controls profiles). Do this off the main thread with `LaunchedEffect`:

```kotlin
var box64Versions by remember { mutableStateOf<List<String>>(emptyList()) }
var isArm64EC by remember { mutableStateOf(false) }

LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
        val cm = ContentsManager(context)
        cm.syncProfiles()
        val wineInfo = WineInfo.fromIdentifier(context, cm, shortcut.container.wineVersion)
        isArm64EC = wineInfo.isArm64EC()
        val versions = ContainerDetailHelper.loadBox64VersionList(context, cm, isArm64EC)
        withContext(Dispatchers.Main) {
            box64Versions = versions
        }
    }
}

// Use in a dropdown
LabeledDropdown(
    label = "Box64 Version",
    options = box64Versions,
    selected = selectedBox64Version,
    onSelect = { selectedBox64Version = it }
)
```

---

### A7. Bridging Java Views That Have No Compose Equivalent

Some Winlator views are too complex to rewrite (e.g. `EnvVarsView`, `CPUListView`). Use `AndroidView` to embed them inside Compose:

```kotlin
val envVarsViewRef = remember { mutableStateOf<EnvVarsView?>(null) }

AndroidView(
    factory = { ctx ->
        EnvVarsView(ctx).also { ev ->
            ev.setDarkMode(true)
            ev.setEnvVars(EnvVars(shortcut.getExtra("envVars")))
            envVarsViewRef.value = ev
        }
    },
    modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 150.dp)
)

// Read the value back when saving:
val envVarsStr = envVarsViewRef.value?.envVars?.toString() ?: ""
```

---

### A8. Icon / File Picker (replaces startActivityForResult)

`startActivityForResult` is deprecated. In Compose use `rememberLauncherForActivityResult`:

```kotlin
val iconLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
) { uri ->
    uri?.let {
        val bmp = BitmapFactory.decodeStream(context.contentResolver.openInputStream(it))
        // save bmp to shortcut icon file
    }
}

Button(onClick = { iconLauncher.launch("image/*") }) {
    Text("Pick Icon")
}
```

---

### A9. Sharing Composables Between Screens

If a dialog (e.g. DxvkConfigDialog) is needed in both ContainerDetail and ShortcutSettings, define it **once** and make it `internal` (visible across the module but not public API):

```kotlin
// In ContainerDetailScreen.kt
@Composable
internal fun DxvkConfigDialog(
    config: KeyValueSet,
    onConfirm: (KeyValueSet) -> Unit,
    onDismiss: () -> Unit
) { /* ... */ }
```

Then call it from ShortcutsScreen.kt directly — no duplication needed.

---

### A10. Keeping a Java Fragment Inside a Compose NavGraph

For fragments too complex to rewrite (InputControlsFragment, SettingsFragment), use a `FragmentScreen` wrapper:

```kotlin
@Composable
fun FragmentScreen(fragmentClass: Class<out Fragment>) {
    val fragmentManager = (LocalContext.current as FragmentActivity).supportFragmentManager
    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = View.generateViewId()
            }
        },
        update = { view ->
            if (fragmentManager.findFragmentById(view.id) == null) {
                fragmentManager.beginTransaction()
                    .replace(view.id, fragmentClass.getDeclaredConstructor().newInstance())
                    .commitNow()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Usage in NavGraph:
composable(Screen.InputControls.route) {
    FragmentScreen(InputControlsFragment::class.java)
}
```

**Gotcha:** `getSupportActionBar()` returns null inside fragments hosted this way. Replace any `requireActivity().supportActionBar?.title = "..."` calls with your own Compose top bar.

---

### A11. Gotchas and Fixes

These are the non-obvious problems encountered during this migration. Knowing them upfront will save hours.

#### Gotcha 1: AppTheme is Light — dialogs render white
Winlator's `AppTheme` is based on a Light Material theme. Any `ContentDialog` or XML dialog will have a white background. This also affects Compose dialogs if you're not careful.

**Fix:** In your Compose theme, force dark surface colors:
```kotlin
MaterialTheme(
    colorScheme = darkColorScheme(
        surface = Color(0xFF1E1E2E),
        background = Color(0xFF121212),
        // etc.
    )
) { /* app content */ }
```
Or set a transparent window background on any remaining XML dialogs:
```xml
<style name="TransparentDialog" parent="Theme.AppCompat.Dialog">
    <item name="android:windowBackground">@android:color/transparent</item>
</style>
```

#### Gotcha 2: getSupportActionBar() is null in Compose-hosted fragments
When a `Fragment` is hosted inside a Compose `AndroidView`/`FragmentContainerView`, the Activity's ActionBar is not wired up to that fragment's lifecycle the normal way.

**Fix:** Remove all `requireActivity().supportActionBar?.title = ...` calls from fragments that will be Compose-hosted. Manage the title in your Compose TopAppBar instead.

#### Gotcha 3: isDarkMode must be hardcoded true in remaining Java code
Some Java dialogs (e.g. the win components spinner backgrounds) check `isDarkMode` to pick drawable resources. Since the app is now always dark-themed via Compose, just hardcode it:

```java
// Before
spinner.setPopupBackgroundResource(isDarkMode
    ? R.drawable.content_dialog_background_dark
    : R.drawable.content_dialog_background);

// After — always dark
spinner.setPopupBackgroundResource(R.drawable.content_dialog_background_dark);
```

#### Gotcha 4: WinHandler flag constants are byte in Java, Int in Kotlin
Java defines these as `public static final byte FLAG_INPUT_TYPE_XINPUT = 1;`. Kotlin bitwise ops require `Int`.

```kotlin
// This crashes with type mismatch:
if (inputType and WinHandler.FLAG_INPUT_TYPE_XINPUT != 0)

// Fix — coerce to Int:
if (inputType and WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt() != 0)
```

#### Gotcha 5: Java getters become Kotlin properties
Java `getGraphicsDriver()` in a Java class is accessible as `.graphicsDriver` in Kotlin. Don't call `.getGraphicsDriver()` — Kotlin will warn and it reads wrong.

```kotlin
// Wrong (works but ugly):
container.getGraphicsDriver()

// Right (idiomatic Kotlin):
container.graphicsDriver
```

#### Gotcha 6: StringUtils.parseIdentifier() strips the display label
Winlator spinner entries like `"800x600 (4:3)"` have a display label. `StringUtils.parseIdentifier()` strips it to return just `"800x600"`. Always run dropdown selections through this before saving to container/shortcut data:

```kotlin
val raw = StringUtils.parseIdentifier(selectedDisplayString) // "800x600"
```

#### Gotcha 7: Screen size "Custom" needs special handling
The screen size spinner has a "Custom" entry. Detect it by comparing the raw value, then show width/height text fields:

```kotlin
var selectedScreenSize by remember { mutableStateOf(container.screenSize) }
var customWidth by remember { mutableStateOf("") }
var customHeight by remember { mutableStateOf("") }
val isCustom = selectedScreenSize == "custom"

LabeledDropdown(options = screenSizeOptions, selected = ..., onSelect = { v ->
    selectedScreenSize = StringUtils.parseIdentifier(v)
})

if (isCustom) {
    Row {
        OutlinedTextField(value = customWidth, onValueChange = { customWidth = it }, label = { Text("Width") })
        OutlinedTextField(value = customHeight, onValueChange = { customHeight = it }, label = { Text("Height") })
    }
}
```

#### Gotcha 8: colorPrimary is near-black — never use it as text color on dark backgrounds
Winlator's `colors.xml` defines `colorPrimary = #262626`. Any XML layout that uses `android:textColor="@color/colorPrimary"` will render as near-invisible dark grey text on a dark background.

**Affects:** `external_controller_list_item.xml` (controller name), `input_controls_fragment.xml` (External Controllers section header), and potentially other XML layouts that haven't been converted to Compose yet.

**Fix:** Replace with a light color like `#E6E6E6`, or use `@android:color/white`:
```xml
<!-- Before -->
android:textColor="@color/colorPrimary"

<!-- After -->
android:textColor="#E6E6E6"
```

**When doing your own migration:** grep every remaining XML layout for `@color/colorPrimary` as a text color and fix them all at once.

#### Gotcha 9: Don't delete Java "dialog" classes that are actually utility classes
Some files in `contentdialog/` look like dialogs but are actually **static utility classes** whose methods are called by the engine layer. Deleting them breaks the build silently if you're not careful.

**Check before deleting** — search all Java and Kotlin files for static method calls:
```bash
grep -rn "GraphicsDriverConfigDialog\." app/src/main/java/
grep -rn "DXVKConfigDialog\." app/src/main/java/
```

If static methods like `parseGraphicsDriverConfig()`, `setEnvVars()`, `getVersion()` show up outside the dialog file itself, keep the file. You can delete the constructor and any UI-building methods, but the statics must stay.

#### Gotcha 10: ContainerManager.reload() must be called before getContainers()
In ViewModel `init` or `refresh()`, always call `reload()` first:

```kotlin
fun refresh() {
    val mgr = ContainerManager(getApplication())
    mgr.reload()  // without this, getContainers() returns stale/empty list
    _containers.value = mgr.containers
}
```

#### Gotcha 11: ExposedDropdownMenuBox + clickable container = double-fire bug
When using `ExposedDropdownMenuBox`, the `menuAnchor()` modifier already intercepts taps and calls `onExpandedChange`. If the composable it is applied to also has its own `onClick`, both fire on the same tap — the dropdown opens and immediately closes.

**Symptom:** Drive letter dropdown (or any `CompactDropdown`) appears unresponsive. Tapping does nothing.

**Wrong:**
```kotlin
OutlinedCard(
    onClick = { expanded = !expanded },    // fires second, cancels menuAnchor
    modifier = Modifier.menuAnchor()       // fires first, sets expanded = true
) { ... }
```

**Fix:** Remove `onClick` from the card. Let `menuAnchor()` be the sole touch handler:
```kotlin
OutlinedCard(
    modifier = Modifier.menuAnchor()       // handles all tap events
) { ... }
```

#### Gotcha 12: Reflection needed to update Shortcut.file after rename
The `Shortcut.file` field is `final`. If you rename a shortcut and need to update the file reference in-memory:

```kotlin
private fun renameShortcut(shortcut: Shortcut, newName: String) {
    val oldFile = shortcut.file
    val newFile = File(oldFile.parent, "$newName.lnk")
    if (oldFile.renameTo(newFile)) {
        val field = Shortcut::class.java.getDeclaredField("file")
        field.isAccessible = true
        field.set(shortcut, newFile)
    }
}
```

---

### A12. Theme / Color Setup

Compose needs its own theme defined before any screen is rendered. Create two files: `Color.kt` and `Theme.kt`.

**Color.kt** — define your palette as top-level constants:
```kotlin
package com.winlator.cmod.ui.theme

import androidx.compose.ui.graphics.Color

val Primary       = Color(0xFF6650A4)
val OnPrimary     = Color(0xFFFFFFFF)
val Surface       = Color(0xFF1E1E2E)
val OnSurface     = Color(0xFFE6E6E6)
val Background    = Color(0xFF121212)
val OnBackground  = Color(0xFFE6E6E6)
val DividerColor  = Color(0xFF2E2E3E)
val OnSurfaceVariant = Color(0xFF9E9EA8)
```

**Theme.kt** — wrap everything in a `MaterialTheme` with a forced dark color scheme:
```kotlin
package com.winlator.cmod.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary         = Primary,
    onPrimary       = OnPrimary,
    surface         = Surface,
    onSurface       = OnSurface,
    background      = Background,
    onBackground    = OnBackground,
)

@Composable
fun WinlatorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
```

**Apply it in MainActivity:**
```kotlin
setContent {
    WinlatorTheme {
        AppNavGraph()
    }
}
```

**Why this matters:** Without an explicit dark color scheme, Compose inherits the XML `AppTheme` which is Light. Every `Surface`, `AlertDialog`, and `Card` will render white. Defining `darkColorScheme()` here fixes all dialogs globally — no per-dialog workarounds needed.

---

### A13. Screen Sealed Class + NavGraph Wiring

Every screen needs a route. Define them in a sealed class, then wire them into a `NavHost`.

**Screen.kt:**
```kotlin
sealed class Screen(val route: String) {
    object Containers    : Screen("containers")
    object Shortcuts     : Screen("shortcuts")
    object ContainerDetail : Screen("container_detail/{containerId}") {
        fun route(id: Int) = "container_detail/$id"
    }
    object Contents      : Screen("contents")
    object Saves         : Screen("saves")
    object AdrenoTools   : Screen("adrenotools")
    object InputControls : Screen("input_controls")
    object Settings      : Screen("settings")
}
```

**NavGraph.kt:**
```kotlin
@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                // drawer items — navigate on click
                NavigationDrawerItem(
                    label = { Text("Containers") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Containers.route) }
                )
                // ... other items
            }
        }
    ) {
        NavHost(navController = navController, startDestination = Screen.Containers.route) {
            composable(Screen.Containers.route)     { ContainersScreen(navController) }
            composable(Screen.Shortcuts.route)      { ShortcutsScreen() }
            composable(Screen.Contents.route)       { ContentsScreen() }
            composable(Screen.Saves.route)          { SavesScreen() }
            composable(Screen.AdrenoTools.route)    { AdrenoToolsScreen() }
            composable(Screen.InputControls.route)  { FragmentScreen(InputControlsFragment::class.java) }
            composable(Screen.Settings.route)       { FragmentScreen(SettingsFragment::class.java) }
            composable(
                route = Screen.ContainerDetail.route,
                arguments = listOf(navArgument("containerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("containerId") ?: return@composable
                ContainerDetailScreen(containerId = id)
            }
        }
    }
}
```

**Navigating to ContainerDetail from ContainersScreen:**
```kotlin
navController.navigate(Screen.ContainerDetail.route(container.id))
```

---

### A14. Launching XServerDisplayActivity (Running a Shortcut/Container)

This is the most critical integration point — the moment Compose hands control to the Wine engine. Build the Intent with all required extras and start the Activity.

```kotlin
fun runShortcut(activity: Activity, shortcut: Shortcut) {
    val container = shortcut.container
    val intent = Intent(activity, XServerDisplayActivity::class.java).apply {
        putExtra("container_id", container.id)
        putExtra("shortcut_path", shortcut.file.path)

        // Graphics
        putExtra("graphics_driver", container.graphicsDriver)
        putExtra("graphics_driver_config", shortcut.getExtra(
            "graphicsDriverConfig", container.graphicsDriverConfig))

        // DX wrapper
        putExtra("dxwrapper", shortcut.getExtra("dxwrapper", container.dxwrapper))
        putExtra("dxwrapper_config", shortcut.getExtra("dxwrapperConfig", container.dxwrapperConfig))

        // Display
        putExtra("screen_size", shortcut.getExtra("screenSize", container.screenSize))
        putExtra("display_scale", container.displayScale)

        // Wine / Box64
        putExtra("wine_version", container.wineVersion)
        putExtra("box64_version", shortcut.getExtra("box64Version", container.box64Version))

        // Win components + env vars
        putExtra("wincomponents", shortcut.getExtra("wincomponents", container.winComponents))
        putExtra("envvars", shortcut.getExtra("envVars", container.envVars))

        // CPU
        putExtra("cpu_list", shortcut.getExtra("cpuList", container.cpuList))
        putExtra("cpu_affinity_mask", container.cpuAffinityMask)
    }
    activity.startActivity(intent)
}
```

**Calling it from a Compose screen:**
```kotlin
val activity = LocalContext.current as Activity

Button(onClick = { runShortcut(activity, shortcut) }) {
    Text("Run")
}
```

**Note:** `XServerDisplayActivity` reads all its config from Intent extras on startup. Check `XServerDisplayActivity.java` `onCreate()` to see exactly which extra keys it expects — the list above covers the common ones but your fork may have additions.

---

### A15. The Engine Boundary — What to Never Touch

The Compose migration only affects the UI layer. Everything below this line must not be renamed, restructured, or moved:

**JNI / Native — package paths map to `.so` symbol names:**
```
com.winlator.cmod.xserver.*       — X server native bridge
com.winlator.cmod.winhandler.*    — Wine message handler
com.winlator.cmod.xenvironment.*  — container environment setup
com.winlator.cmod.math.*          — native math bridge
com.winlator.cmod.widget.*        — native surface views
```

If you rename a class in any of these packages, the JNI symbol lookup in the prebuilt `.so` libraries will fail at runtime with `UnsatisfiedLinkError` and Wine will not start.

**Safe to modify UI of, but not rename/move:**
```
XServerDisplayActivity.java       — Wine session host Activity
XServerView.java                  — native surface
InputControlsFragment.java        — complex input view hierarchy
SettingsFragment.java             — PreferenceFragmentCompat
```

**Engine data classes — never rename fields (serialized to JSON/XML):**
```
container/Container.java          — container config, fields map to saved XML keys
container/Shortcut.java           — shortcut config, fields map to .lnk file keys
core/EnvVars.java                 — environment variable store
core/KeyValueSet.java             — general key=value parser
```

**Binaries — never delete or rename:**
```
assets/imagefs/                   — Wine filesystem image
app/src/main/jniLibs/             — prebuilt .so libraries (Box64, FEX, Wine JNI)
```

**Rule of thumb:** If a file has a `native` method or is in a package that a `.so` library references by name, it is off-limits for renaming. The UI layer (Fragments, Activities that only show UI, Dialogs, ViewModels) is safe to rewrite. The engine layer is not.

---

### A16. Post-Migration Testing Checklist

After building the APK, go through this list. These are the areas most likely to have subtle regressions that compile cleanly but break at runtime.

#### Container Management
- [ ] Create a new container — all tabs save correctly (General, Graphics, Audio, CPU, Advanced)
- [ ] Edit an existing container — values load correctly into all dropdowns and fields
- [ ] Screen size "Custom" — width/height fields appear, save, and reload correctly
- [ ] Delete a container — confirmation dialog appears, container is removed from list
- [ ] Container list refreshes after create/edit/delete without requiring app restart

#### Shortcut Management
- [ ] Open ShortcutSettings — all three tabs load (Win Components, Env Vars, Advanced)
- [ ] Win Components — each spinner shows correct value on open, saves on OK
- [ ] Env Vars — existing vars display in EnvVarsView, Add button opens AddEnvVarComposable, new var appears in list
- [ ] Advanced tab — Box64 version dropdown populates (async load), preset applies values
- [ ] Icon picker — selecting an image from gallery updates the shortcut icon
- [ ] Rename shortcut — new name shows in list immediately, `.lnk` file renamed on disk
- [ ] Export shortcut — file written, toast shows path
- [ ] Shortcut properties — play count and playtime display correctly, Reset clears values live without dismissing dialog
- [ ] Clone shortcut — clone appears in list under correct container
- [ ] Delete shortcut — confirmation shows, shortcut removed

#### Graphics Config Dialogs
- [ ] GraphicsDriverConfigDialog opens from ContainerDetail — values load and save
- [ ] GraphicsDriverConfigDialog opens from ShortcutSettings — same behavior
- [ ] DxvkConfigDialog — framerate, async, VKD3D feature level all save
- [ ] WineD3DConfigDialog — GPU name dropdown populates from gpu_cards.json, values save
- [ ] FPS counter config — values save to container

#### Env Vars
- [ ] Add env var with name from Presets dropdown — name auto-fills
- [ ] Add env var with custom name — saves correctly
- [ ] Duplicate name rejected (no duplicate added)
- [ ] Env vars persist after closing and reopening ShortcutSettings

#### Running a Container / Shortcut
- [ ] "Run" button on a shortcut launches XServerDisplayActivity
- [ ] Wine session starts (X server surface appears)
- [ ] Return to app after session — shortcut list is intact

#### Contents Screen
- [ ] Component list loads and displays
- [ ] Download/install flow completes for a small component
- [ ] ContentInfoDialog shows component details
- [ ] ContentUntrustedDialog appears for unsigned components

#### Saves Screen
- [ ] Save list loads for a container
- [ ] Create new save — appears in list
- [ ] Edit save name — updates in list
- [ ] Export save — file written

#### Splash / First Run
- [ ] Fresh install shows SplashScreen with progress bar
- [ ] imagefs installs without hanging
- [ ] App proceeds to ContainersScreen after install

#### Fragments (kept as Java)
- [ ] InputControls screen loads — no ActionBar crash
- [ ] Settings screen loads — no ActionBar crash
- [ ] Settings changes persist (dark mode toggle, etc.)

#### XML Layout Text Colours (common regression)
- [ ] Input Controls → "External Controllers" section header is readable (not dark on dark)
- [ ] Input Controls → connected controller name (e.g. "Microsoft X-Box 360 pad") is readable
- [ ] Grep remaining XML layouts for `@color/colorPrimary` as `textColor` — fix any found

#### Drives Tab (ContainerDetail)
- [ ] Add a drive — row appears with letter dropdown, path field, browse + remove buttons
- [ ] Drive letter dropdown opens on tap (ExposedDropdownMenuBox + menuAnchor only, no competing onClick)
- [ ] Changing letter updates the drive entry
- [ ] Browse button opens folder picker, selected path populates field
- [ ] Remove button removes the drive row

---

## Part C — Post-Report Bug Fixes

Bugs found during device testing after the initial migration was declared complete. Included here so future migrators know what to watch for.

| Commit | File | Bug | Root Cause | Fix |
|---|---|---|---|---|
| `85b1e57` | `external_controller_list_item.xml` | Controller name text invisible on dark background | `TVTitle` used `android:textColor="@color/colorPrimary"` which is `#262626` (near-black) | Changed to `#E6E6E6` |
| `85b1e57` | `ContainerDetailScreen.kt` | Drive letter dropdown unresponsive, never opened | `CompactDropdown` used `OutlinedCard(onClick = { expanded = !expanded })` + `menuAnchor()` — both handlers fired on same tap, cancelling each other | Removed `onClick` from `OutlinedCard`; `menuAnchor()` is now sole click handler |
| `6537038` | `input_controls_fragment.xml` | "External Controllers" section header invisible on dark background | Header `TextView` used `android:textColor="@color/colorPrimary"` (`#262626`) | Changed to `#E6E6E6` |

### Pattern: colorPrimary as text colour
All three visual bugs had the same root cause: `@color/colorPrimary = #262626` used as `android:textColor` in XML layouts. When the Compose dark theme is applied to the Activity, the background is dark but these XML-rendered text views inherited a near-black text colour from the legacy colour palette.

**Grep to catch them all before testing:**
```bash
grep -rn 'textColor.*colorPrimary\|colorPrimary.*textColor' app/src/main/res/layout/
```

Fix every match by replacing with `#E6E6E6` or a dedicated string resource.

---

## Part B — What Was Replaced and With What

### B1. Navigation Screens Replaced

| Removed (Java Fragment + XML) | Replaced With (Kotlin Compose) |
|---|---|
| `MainActivity.java` + XML drawer/nav | `MainActivity.kt` + `NavGraph.kt` + Compose NavDrawer |
| `ContainersFragment.java` + `containers_fragment.xml` | `ContainersScreen.kt` + `ContainersViewModel.kt` |
| `ShortcutsFragment.java` + `shortcuts_fragment.xml` | `ShortcutsScreen.kt` + `ShortcutsViewModel.kt` |
| `ContainerDetailFragment.java` + `container_detail_fragment.xml` | `ContainerDetailScreen.kt` + `ContainerDetailViewModel.kt` |
| `ContentsFragment.java` + `contents_fragment.xml` | `ContentsScreen.kt` + `ContentsViewModel.kt` |
| `SavesFragment.java` + `saves_fragment.xml` | `SavesScreen.kt` + `SavesViewModel.kt` |
| `AdrenotoolsFragment.java` + `adrenotools_fragment.xml` | `AdrenoToolsScreen.kt` |
| `Box86_64RCFragment.java` + `box86_64_rc_fragment.xml` | Removed entirely (dead feature) |
| — | `SplashScreen.kt` + `SplashViewModel.kt` (new, replaced `DownloadProgressDialog`) |
| — | `FileManagerScreen.kt` (new Compose file picker) |

**Kept as Java Fragments (too complex to convert, hosted via `FragmentScreen`):**
- `InputControlsFragment.java`
- `SettingsFragment.java`

---

### B2. Dialogs Replaced

#### Container Detail Dialogs

| Removed (Java + XML Layout) | Replaced With (Compose) | Location |
|---|---|---|
| `GraphicsDriverConfigDialog.java` (UI) + `graphics_driver_config_dialog.xml` | `internal fun GraphicsDriverConfigDialog()` | `ContainerDetailScreen.kt` |
| `DXVKConfigDialog.java` (UI) + `dxvk_config_dialog.xml` | `internal fun DxvkConfigDialog()` | `ContainerDetailScreen.kt` |
| `VKD3DConfigDialog.java` + `vkd3d_config_dialog.xml` | Merged into `DxvkConfigDialog()` composable | `ContainerDetailScreen.kt` |
| `WineD3DConfigDialog.java` (UI) + `wined3d_config_dialog.xml` | `internal fun WineD3DConfigDialog()` | `ContainerDetailScreen.kt` |
| `VirGLConfigDialog.java` + `virgl_config_dialog.xml` | Removed (VirGL deprecated) | — |
| `FPSCounterConfigDialog.java` + `fps_counter_config_dialog.xml` | `internal fun FpsCounterConfigDialog()` | `ContainerDetailScreen.kt` |
| `AddEnvVarDialog.java` + `add_env_var_dialog.xml` | `internal fun AddEnvVarComposable()` | `ContainerDetailScreen.kt` |
| `ExtensionPickerDialog` (inline in fragment) | `internal fun ExtensionPickerDialog()` | `ContainerDetailScreen.kt` |

#### Shortcut Dialogs

| Removed (Java + XML Layout) | Replaced With (Compose) | Location |
|---|---|---|
| `ShortcutSettingsDialog.java` + `shortcut_settings_dialog.xml` | `ShortcutSettingsDialogScreen()` — full 3-tab Compose Dialog | `ShortcutsScreen.kt` |
| `shortcut_properties_dialog.xml` + inline XML dialog in `showProperties()` | Inline Compose `AlertDialog` with `propertiesShortcut` state | `ShortcutsScreen.kt` |

**ShortcutSettingsDialog tabs (all Compose):**
- **Win Components** — `ScWinComponentsTab()` with DirectX/General sections
- **Env Vars** — `ScEnvVarsTab()` with `AndroidView(EnvVarsView)` + `AddEnvVarComposable`
- **Advanced** — `ScAdvancedTab()` with Box64, FEXCore, controls profile, `AndroidView(CPUListView)`, sharpness sliders

#### Contents / Store Dialogs

| Removed (Java + XML Layout) | Replaced With (Compose) | Location |
|---|---|---|
| `ContentInfoDialog.java` + `content_info_dialog.xml` | Inline Compose `AlertDialog` two-phase flow | `ContentsScreen.kt` |
| `ContentUntrustedDialog.java` + `content_untrusted_dialog.xml` | Inline Compose `AlertDialog` | `ContentsScreen.kt` |
| `StorageInfoDialog.java` + `container_storage_info_dialog.xml` | Inline Compose `AlertDialog` | `ContentsScreen.kt` |
| `winetricks_content_dialog.xml` | Removed (Winetricks UI not carried forward) | — |

#### Saves Dialogs

| Removed (Java + XML Layout) | Replaced With (Compose) | Location |
|---|---|---|
| `SaveEditDialog.java` + `save_edit_dialog.xml` | Inline Compose `AlertDialog` | `SavesScreen.kt` |
| `SaveSettingsDialog.java` + `save_settings_dialog.xml` | Inline Compose `AlertDialog` | `SavesScreen.kt` |
| `saves_list_item.xml` | Compose `LazyColumn` item | `SavesScreen.kt` |

#### Splash / Install Dialog

| Removed (Java + XML Layout) | Replaced With (Compose) | Location |
|---|---|---|
| `PreloaderDialog.java` + `preloader_dialog.xml` + `download_progress_dialog.xml` | Full-screen Compose overlay (`SplashScreen.kt`) | `SplashScreen.kt` |

#### Gamepad / Input Dialogs

| Removed (Java + XML Layout) | Replaced With | Notes |
|---|---|---|
| `GamepadConfiguratorDialog.java` + `dialog_gamepad_configurator.xml` | Removed | Feature not carried forward |
| `ImportGroupDialog.java` + `box86_64_rc_groups_dialog.xml` | Removed | Was part of Box86_64RC (removed screen) |
| `gyro_config_dialog.xml` | Removed | — |
| `touchpad_help_dialog.xml` | Removed | — |

---

### B3. Helper / Utility Classes Deleted

| Deleted File | Reason |
|---|---|
| `ContainerDetailHelper.java` | Was a bridge for `ShortcutSettingsDialog`; no callers once dialog was replaced |
| `TerminalActivity.java` | Dead activity, not reachable from any Compose screen |
| `RestoreActivity.java` | Dead activity |
| `WinetricksFloatingView.java` | Winetricks UI not carried forward |
| `saves/CustomFilePickerActivity.java` + `saves/FileAdapter.java` | Replaced by Compose `FileManagerScreen.kt` + system file picker |
| `saves/Save.java` + `saves/SaveManager.java` | Replaced by `SavesViewModel.kt` logic |
| `box86_64/rc/` (5 files) | Entire Box86_64RC feature removed |
| `xenvironment/components/VortekRendererComponent.java` | Dead component |
| `xenvironment/components/BionicProgramLauncherComponent.java` | Dead component |
| `xenvironment/components/GlibcProgramLauncherComponent.java` | Dead component |
| `xenvironment/components/NetworkInfoUpdateComponent.java` | Dead component |
| `core/Win32AppWorkarounds.java` | Dead utility |
| `inputcontrols/PreferenceKeys.java` | Dead utility |
| `store/LudashiLaunchBridge.java` | Dead bridge |

---

### B4. What Remains (Intentionally)

#### Java files kept — utility static methods still in use

| File | Used By | What It Provides |
|---|---|---|
| `ContentDialog.java` | `InputControlsFragment`, `SettingsFragment`, `XServerDisplayActivity`, `TaskManagerDialog` | Base dialog class + static helpers (`confirm`, `prompt`, `alert`) |
| `GraphicsDriverConfigDialog.java` | `AdrenotoolsManager`, `XServerDisplayActivity`, `ContainerDetailViewModel` | Static: `parseGraphicsDriverConfig()`, `toGraphicsDriverConfig()`, `getVersion()` |
| `DXVKConfigDialog.java` | `ContainerDetailViewModel`, `ShortcutsScreen` | Static: `parseConfig()`, `setEnvVars()`, `loadDxvkVersionList()`, `compareVersion()` |
| `WineD3DConfigDialog.java` | `ContainerDetailViewModel` | Static: `parseConfig()`, `setEnvVars()`, `loadGpuNames()` |

#### Java dialogs kept — only called from `XServerDisplayActivity` (in-game, not Compose)

| File | Trigger |
|---|---|
| `ActiveWindowsDialog.java` | In-game window list button |
| `DebugDialog.java` | In-game debug log overlay |
| `ScreenEffectDialog.java` | In-game screen effects button |
| `FSRControlFloatingDialog.java` | In-game FSR floating overlay |

#### Fragments kept — hosted via `FragmentScreen` wrapper

| File | Reason |
|---|---|
| `InputControlsFragment.java` | Complex custom view hierarchy; kept as-is |
| `SettingsFragment.java` | PreferenceFragmentCompat; kept as-is |

---

### B5. Reusable Internal Composables

Composables promoted to `internal` visibility so they can be shared across screens within the same module:

| Composable | Defined In | Used In |
|---|---|---|
| `SectionBox()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `LabeledDropdown()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `GraphicsDriverConfigDialog()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `DxvkConfigDialog()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `WineD3DConfigDialog()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `FpsCounterConfigDialog()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen` |
| `AddEnvVarComposable()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen`, `ShortcutsScreen` |
| `ExtensionPickerDialog()` | `ContainerDetailScreen.kt` | `ContainerDetailScreen` |

---

### B6. Key Compose Patterns Used

| Pattern | Used For |
|---|---|
| `LazyColumn` | All list screens (containers, shortcuts, contents, saves) |
| `AlertDialog` | All confirmation, settings, and info dialogs |
| `Dialog(DialogProperties(usePlatformDefaultWidth = false))` | Full-screen dialogs (ShortcutSettings, SplashScreen) |
| `TabRow` + `HorizontalPager` | ContainerDetail tabs, ShortcutSettings tabs |
| `AndroidView` | `EnvVarsView`, `CPUListView` — Java views with no Compose equivalent |
| `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` | Icon picker in ShortcutSettings |
| `LaunchedEffect + withContext(Dispatchers.IO)` | Async spinner loading (Box64 versions, FEXCore, controls profiles, MIDI) |
| `SnapshotStateList` | Win components state in ShortcutSettings |
| `collectAsState()` on `StateFlow` | All ViewModels → screen state binding |
| `internal fun` composables | Shared dialogs reused across multiple screens |

---

### B7. Summary Stats

| Metric | Count |
|---|---|
| Java Fragments deleted | 7 |
| Fragment XML layouts deleted | 8 |
| Java Dialog classes deleted | 11 |
| Dialog XML layouts deleted | 15 |
| Other Java/utility files deleted | 14 |
| New Kotlin Compose screens | 10 |
| New Kotlin ViewModels | 6 |
| Internal reusable composables | 8 |
| Total lines of Java/XML removed | ~5,000+ |
