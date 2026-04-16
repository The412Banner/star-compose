@file:OptIn(ExperimentalMaterial3Api::class)
package com.winlator.cmod.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winlator.cmod.MainActivity
import com.winlator.cmod.R
import com.winlator.cmod.contentdialog.AddEnvVarDialog
import com.winlator.cmod.contentdialog.DXVKConfigDialog
import com.winlator.cmod.contentdialog.FPSCounterConfigDialog
import com.winlator.cmod.contentdialog.GraphicsDriverConfigDialog
import com.winlator.cmod.contentdialog.WineD3DConfigDialog
import com.winlator.cmod.core.AppUtils
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.StringUtils
import com.winlator.cmod.core.WineThemeManager
import com.winlator.cmod.container.Container
import com.winlator.cmod.widget.ColorPickerView
import com.winlator.cmod.widget.CPUListView
import com.winlator.cmod.widget.EnvVarsView

// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ContainerDetailScreen(
    containerId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ContainerDetailViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(containerId) { viewModel.init(containerId) }

    // Dummy Views for dialog config storage (need Activity context)
    val graphicsDriverConfigView = remember(context) { View(context).also { it.tag = viewModel.graphicsDriverConfig } }
    val dxWrapperConfigView      = remember(context) { View(context).also { it.tag = viewModel.dxWrapperConfig      } }
    val fpsCounterConfigView     = remember(context) { View(context).also { it.tag = viewModel.fpsCounterConfig     } }

    // Keep dummy view tags in sync with VM state (initial load)
    SideEffect {
        if (graphicsDriverConfigView.tag.toString() != viewModel.graphicsDriverConfig)
            graphicsDriverConfigView.tag = viewModel.graphicsDriverConfig
        if (dxWrapperConfigView.tag.toString() != viewModel.dxWrapperConfig)
            dxWrapperConfigView.tag = viewModel.dxWrapperConfig
        if (fpsCounterConfigView.tag.toString() != viewModel.fpsCounterConfig)
            fpsCounterConfigView.tag = viewModel.fpsCounterConfig
    }

    // AndroidView references for custom views
    val envVarsViewRef      = remember { mutableStateOf<EnvVarsView?>(null)      }
    val cpuListViewRef      = remember { mutableStateOf<CPUListView?>(null)      }
    val cpuListWoW64Ref     = remember { mutableStateOf<CPUListView?>(null)      }
    val colorPickerViewRef  = remember { mutableStateOf<ColorPickerView?>(null)  }

    val tabTitles = listOf(
        stringResource(R.string.wine_configuration),
        stringResource(R.string.win_components),
        stringResource(R.string.environment_variables),
        stringResource(R.string.drives),
        stringResource(R.string.advanced),
        stringResource(R.string.xr)
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.confirm(
                        resolvedGraphicsDriverConfig = graphicsDriverConfigView.tag?.toString() ?: "",
                        resolvedDXWrapperConfig      = dxWrapperConfigView.tag?.toString() ?: "",
                        resolvedFPSCounterConfig     = fpsCounterConfigView.tag?.toString() ?: "",
                        resolvedEnvVars      = envVarsViewRef.value?.envVars ?: viewModel.envVarsStr,
                        resolvedCPUList      = cpuListViewRef.value?.checkedCPUListAsString ?: viewModel.cpuList,
                        resolvedCPUListWoW64 = cpuListWoW64Ref.value?.checkedCPUListAsString ?: viewModel.cpuListWoW64,
                        resolvedColorAsString = colorPickerViewRef.value?.colorAsString ?: "#0277bd",
                        onDone = onNavigateBack
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top-level fields ───────────────────────────────────────────────
            TopLevelFields(
                viewModel = viewModel,
                graphicsDriverConfigView = graphicsDriverConfigView,
                dxWrapperConfigView = dxWrapperConfigView,
                fpsCounterConfigView = fpsCounterConfigView
            )

            // ── Tabs ───────────────────────────────────────────────────────────
            ScrollableTabRow(
                selectedTabIndex = viewModel.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 0.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = viewModel.selectedTab == index,
                        onClick = { viewModel.selectedTab = index },
                        text = { Text(title, fontSize = 12.sp) }
                    )
                }
            }

            // ── Tab content ────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                when (viewModel.selectedTab) {
                    0 -> WineConfigTab(viewModel, colorPickerViewRef)
                    1 -> WinComponentsTab(viewModel)
                    2 -> EnvVarsTab(viewModel, envVarsViewRef)
                    3 -> DrivesTab(viewModel)
                    4 -> AdvancedTab(viewModel, cpuListViewRef, cpuListWoW64Ref)
                    5 -> XRTab(viewModel)
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // room for FAB
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TopLevelFields(
    viewModel: ContainerDetailViewModel,
    graphicsDriverConfigView: View,
    dxWrapperConfigView: View,
    fpsCounterConfigView: View,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

        // Name
        OutlinedTextField(
            value = viewModel.containerName,
            onValueChange = { viewModel.containerName = it },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        // Screen Size
        LabeledDropdown(
            label = stringResource(R.string.screen_size),
            options = viewModel.screenSizeEntries,
            selectedOption = viewModel.selectedScreenSize,
            onSelect = { viewModel.selectedScreenSize = it }
        )
        if (viewModel.selectedScreenSize.equals("custom", ignoreCase = true)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.customWidth,
                    onValueChange = { viewModel.customWidth = it },
                    label = { Text(stringResource(R.string.width)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = viewModel.customHeight,
                    onValueChange = { viewModel.customHeight = it },
                    label = { Text(stringResource(R.string.height)) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Wine Version
        LabeledDropdown(
            label = stringResource(R.string.wine_version),
            options = viewModel.wineVersionEntries,
            selectedOption = viewModel.selectedWineVersion,
            enabled = viewModel.wineVersionEnabled,
            onSelect = { viewModel.onWineVersionChanged(it) }
        )
        Spacer(Modifier.height(8.dp))

        // Graphics Driver + config button
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LabeledDropdown(
                label = stringResource(R.string.graphics_driver),
                options = viewModel.graphicsDriverEntries,
                selectedOption = viewModel.selectedGraphicsDriver,
                onSelect = { viewModel.selectedGraphicsDriver = it },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val driver = StringUtils.parseIdentifier(viewModel.selectedGraphicsDriver)
                GraphicsDriverConfigDialog(graphicsDriverConfigView, driver, null).show()
            }) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))

        // DX Wrapper + config button
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                LabeledDropdown(
                    label = stringResource(R.string.dxwrapper),
                    options = viewModel.dxWrapperEntries,
                    selectedOption = viewModel.selectedDXWrapper,
                    onSelect = { viewModel.selectedDXWrapper = it },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    AppUtils.showHelpBox(context, View(context), R.string.dxwrapper_help_content)
                }) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = {
                val wrapper = StringUtils.parseIdentifier(viewModel.selectedDXWrapper)
                if (wrapper.contains("dxvk")) {
                    DXVKConfigDialog(dxWrapperConfigView, viewModel.isArm64EC).show()
                } else {
                    WineD3DConfigDialog(dxWrapperConfigView).show()
                }
            }) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Audio Driver
        LabeledDropdown(
            label = stringResource(R.string.audio_driver),
            options = viewModel.audioDriverEntries,
            selectedOption = viewModel.selectedAudioDriver,
            onSelect = { viewModel.selectedAudioDriver = it }
        )
        Spacer(Modifier.height(8.dp))

        // Emulator (arm64ec only)
        if (viewModel.isArm64EC) {
            LabeledDropdown(
                label = "Emulator",
                options = viewModel.emulatorEntries,
                selectedOption = viewModel.selectedEmulator,
                enabled = viewModel.emulatorEnabled,
                onSelect = { viewModel.selectedEmulator = it }
            )
            Spacer(Modifier.height(8.dp))
        }

        // MIDI Sound Font
        LabeledDropdown(
            label = stringResource(R.string.midi_sound_font),
            options = viewModel.midiEntries,
            selectedOption = viewModel.midiEntries.getOrElse(viewModel.selectedMidiIndex) { "" },
            onSelect = { opt -> viewModel.selectedMidiIndex = viewModel.midiEntries.indexOf(opt).coerceAtLeast(0) }
        )
        Spacer(Modifier.height(8.dp))

        // Show FPS + config
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = viewModel.showFPS,
                onCheckedChange = { viewModel.showFPS = it }
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.show_fps), modifier = Modifier.weight(1f))
            IconButton(onClick = {
                FPSCounterConfigDialog.show(context, fpsCounterConfigView)
            }) {
                Icon(Icons.Default.Settings, contentDescription = null)
            }
        }

        // Fullscreen Stretched
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = viewModel.fullscreenStretched,
                onCheckedChange = { viewModel.fullscreenStretched = it }
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.fullscreen_stretched))
        }

        // LC_ALL
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = viewModel.lcAll,
                onValueChange = { viewModel.lcAll = it },
                label = { Text("LC_ALL") },
                modifier = Modifier.weight(1f)
            )
            var showLcMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showLcMenu = true }) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
            }
            DropdownMenu(expanded = showLcMenu, onDismissRequest = { showLcMenu = false }) {
                viewModel.lcAllEntries.forEach { lc ->
                    DropdownMenuItem(
                        text = { Text("$lc.UTF-8") },
                        onClick = { viewModel.lcAll = "$lc.UTF-8"; showLcMenu = false }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WineConfigTab(
    viewModel: ContainerDetailViewModel,
    colorPickerViewRef: MutableState<ColorPickerView?>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Desktop section
        SectionBox(title = "Desktop") {
            LabeledDropdown(
                label = stringResource(R.string.theme),
                options = listOf("Light", "Dark"),
                selectedOption = listOf("Light", "Dark").getOrElse(viewModel.desktopThemeIndex) { "Light" },
                onSelect = { opt -> viewModel.desktopThemeIndex = listOf("Light", "Dark").indexOf(opt).coerceAtLeast(0) }
            )
            Spacer(Modifier.height(8.dp))
            LabeledDropdown(
                label = stringResource(R.string.background),
                options = listOf("Image", "Solid Color"),
                selectedOption = listOf("Image", "Solid Color").getOrElse(viewModel.desktopBgTypeIndex) { "Image" },
                onSelect = { opt -> viewModel.desktopBgTypeIndex = listOf("Image", "Solid Color").indexOf(opt).coerceAtLeast(0) }
            )
            // Color picker (visible when Solid Color selected)
            if (viewModel.desktopBgTypeIndex == WineThemeManager.BackgroundType.COLOR.ordinal) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Background Color", modifier = Modifier.weight(1f))
                    AndroidView(
                        factory = { ctx ->
                            ColorPickerView(ctx).also { cpv ->
                                cpv.setColor(viewModel.desktopBgColorInt)
                                colorPickerViewRef.value = cpv
                            }
                        },
                        update = { cpv -> cpv.setColor(viewModel.desktopBgColorInt) },
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // DirectInput section
        SectionBox(title = "DirectInput") {
            LabeledDropdown(
                label = stringResource(R.string.mouse_warp_override),
                options = viewModel.mouseWarpEntries,
                selectedOption = viewModel.mouseWarpEntries.getOrElse(viewModel.selectedMouseWarpIndex) { "" },
                onSelect = { opt -> viewModel.selectedMouseWarpIndex = viewModel.mouseWarpEntries.indexOf(opt).coerceAtLeast(0) }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WinComponentsTab(viewModel: ContainerDetailViewModel) {
    val directxItems = remember(viewModel.winComponents.size) {
        viewModel.winComponents.filter { it.key.startsWith("direct") }
    }
    val generalItems = remember(viewModel.winComponents.size) {
        viewModel.winComponents.filterNot { it.key.startsWith("direct") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (directxItems.isNotEmpty()) {
            SectionBox(title = "DirectX") {
                directxItems.forEach { comp ->
                    WinComponentRow(comp) { idx ->
                        val i = viewModel.winComponents.indexOfFirst { it.key == comp.key }
                        if (i >= 0) viewModel.winComponents[i] = viewModel.winComponents[i].copy(selectedIndex = idx)
                    }
                }
            }
        }
        if (generalItems.isNotEmpty()) {
            SectionBox(title = "General") {
                generalItems.forEach { comp ->
                    WinComponentRow(comp) { idx ->
                        val i = viewModel.winComponents.indexOfFirst { it.key == comp.key }
                        if (i >= 0) viewModel.winComponents[i] = viewModel.winComponents[i].copy(selectedIndex = idx)
                    }
                }
            }
        }
    }
}

@Composable
private fun WinComponentRow(comp: WinComponentEntry, onSelect: (Int) -> Unit) {
    val options = listOf("Builtin (Wine)", "Native (Windows)")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(comp.label, modifier = Modifier.weight(1f))
        CompactDropdown(
            options = options,
            selectedOption = options.getOrElse(comp.selectedIndex) { options[0] },
            onSelect = { opt -> onSelect(options.indexOf(opt).coerceAtLeast(0)) }
        )
    }
    Spacer(Modifier.height(4.dp))
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EnvVarsTab(
    viewModel: ContainerDetailViewModel,
    envVarsViewRef: MutableState<EnvVarsView?>
) {
    val context = LocalContext.current
    Column {
        AndroidView(
            factory = { ctx ->
                EnvVarsView(ctx).also { ev ->
                    ev.setDarkMode(true)
                    ev.setEnvVars(com.winlator.cmod.core.EnvVars(viewModel.envVarsStr))
                    envVarsViewRef.value = ev
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { envVarsViewRef.value?.let { AddEnvVarDialog(context, it).show() } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add) + " " + stringResource(R.string.environment_variables))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DrivesTab(viewModel: ContainerDetailViewModel) {
    val context = LocalContext.current
    var pendingDriveUid by remember { mutableStateOf<Long?>(null) }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null && pendingDriveUid != null) {
                val path = FileUtils.getFilePathFromUri(context, uri)
                if (path != null) {
                    viewModel.updateDrivePath(pendingDriveUid!!, path)
                }
            }
        }
        pendingDriveUid = null
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (viewModel.drives.isEmpty()) {
            Text(
                stringResource(R.string.no_items_to_display),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        viewModel.drives.forEach { drive ->
            DriveRow(
                drive = drive,
                letterOptions = viewModel.driveLetterOptions,
                onLetterChange = { viewModel.updateDriveLetter(drive.uid, it) },
                onPathChange   = { viewModel.updateDrivePath(drive.uid, it)   },
                onBrowse = {
                    pendingDriveUid = drive.uid
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                            Uri.fromFile(Environment.getExternalStorageDirectory()))
                    }
                    dirPickerLauncher.launch(intent)
                },
                onRemove = { viewModel.removeDrive(drive.uid) }
            )
        }
        Button(
            onClick = { viewModel.addDrive() },
            enabled = viewModel.drives.size < Container.MAX_DRIVE_LETTERS,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add) + " " + stringResource(R.string.drives))
        }
    }
}

@Composable
private fun DriveRow(
    drive: DriveEntry,
    letterOptions: List<String>,
    onLetterChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onBrowse: () -> Unit,
    onRemove: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CompactDropdown(
            options = letterOptions,
            selectedOption = "${drive.letter}:",
            onSelect = { onLetterChange(it.trimEnd(':')) },
            modifier = Modifier.width(64.dp)
        )
        OutlinedTextField(
            value = drive.path,
            onValueChange = onPathChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Path") }
        )
        IconButton(onClick = onBrowse) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = null)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AdvancedTab(
    viewModel: ContainerDetailViewModel,
    cpuListViewRef: MutableState<CPUListView?>,
    cpuListWoW64Ref: MutableState<CPUListView?>
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Box64 section
        SectionBox(title = "Box64") {
            LabeledDropdown(
                label = stringResource(R.string.box64_version),
                options = viewModel.box64VersionEntries,
                selectedOption = viewModel.selectedBox64Version,
                onSelect = { viewModel.selectedBox64Version = it }
            )
            Spacer(Modifier.height(8.dp))
            LabeledDropdown(
                label = stringResource(R.string.box64_preset),
                options = viewModel.box64PresetEntries,
                selectedOption = viewModel.box64PresetEntries.getOrElse(viewModel.selectedBox64PresetIndex) { "" },
                onSelect = { opt -> viewModel.selectedBox64PresetIndex = viewModel.box64PresetEntries.indexOf(opt).coerceAtLeast(0) }
            )
        }

        // FEXCore section (arm64ec only)
        if (viewModel.isArm64EC) {
            SectionBox(title = "FEXCore") {
                LabeledDropdown(
                    label = stringResource(R.string.fexcore_version),
                    options = viewModel.fexCoreVersionEntries,
                    selectedOption = viewModel.selectedFEXCoreVersion,
                    onSelect = { viewModel.selectedFEXCoreVersion = it }
                )
                Spacer(Modifier.height(8.dp))
                LabeledDropdown(
                    label = stringResource(R.string.fexcore_preset),
                    options = viewModel.fexCorePresetEntries,
                    selectedOption = viewModel.fexCorePresetEntries.getOrElse(viewModel.selectedFEXCorePresetIndex) { "" },
                    onSelect = { opt -> viewModel.selectedFEXCorePresetIndex = viewModel.fexCorePresetEntries.indexOf(opt).coerceAtLeast(0) }
                )
            }
        }

        // Game Controller section
        SectionBox(title = stringResource(R.string.game_controller)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = viewModel.enableXInput,
                    onCheckedChange = { viewModel.enableXInput = it },
                    enabled = viewModel.exclusiveXInput
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.enable_xinput_for_wine_game), modifier = Modifier.weight(1f))
                IconButton(onClick = { AppUtils.showHelpBox(context, View(context), R.string.help_xinput) }) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = viewModel.enableDInput,
                    onCheckedChange = { viewModel.enableDInput = it },
                    enabled = viewModel.exclusiveXInput
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.enable_dinput_for_wine_game), modifier = Modifier.weight(1f))
                IconButton(onClick = { AppUtils.showHelpBox(context, View(context), R.string.help_dinput) }) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = viewModel.exclusiveXInput,
                    onCheckedChange = { viewModel.onExclusiveXInputChanged(it) }
                )
                Spacer(Modifier.width(8.dp))
                Text("Exclusive Input", modifier = Modifier.weight(1f))
                IconButton(onClick = { AppUtils.showHelpBox(context, View(context), R.string.help_exclusive_xinput) }) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Startup Selection
        LabeledDropdown(
            label = stringResource(R.string.startup_selection),
            options = viewModel.startupSelectionEntries,
            selectedOption = viewModel.startupSelectionEntries.getOrElse(viewModel.selectedStartupSelection) { "" },
            onSelect = { opt -> viewModel.selectedStartupSelection = viewModel.startupSelectionEntries.indexOf(opt).coerceAtLeast(0) }
        )

        // Processor Affinity
        SectionBox(title = stringResource(R.string.processor_affinity)) {
            Text(
                stringResource(R.string.processor_affinity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            AndroidView(
                factory = { ctx ->
                    CPUListView(ctx).also { cpv ->
                        cpv.setCheckedCPUList(viewModel.cpuList)
                        cpuListViewRef.value = cpv
                    }
                },
                update = {},
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            )
            if (viewModel.isArm64EC) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.processor_affinity_32_bit_apps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                AndroidView(
                    factory = { ctx ->
                        CPUListView(ctx).also { cpv ->
                            cpv.setCheckedCPUList(viewModel.cpuListWoW64)
                            cpuListWoW64Ref.value = cpv
                        }
                    },
                    update = {},
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun XRTab(viewModel: ContainerDetailViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Primary controller
        LabeledDropdown(
            label = stringResource(R.string.primary_controller),
            options = viewModel.primaryControllerEntries,
            selectedOption = viewModel.primaryControllerEntries.getOrElse(viewModel.selectedPrimaryController) { "" },
            onSelect = { opt -> viewModel.selectedPrimaryController = viewModel.primaryControllerEntries.indexOf(opt).coerceAtLeast(0) }
        )

        // Controller button mappings
        SectionBox(title = "Controller Mapping") {
            viewModel.xrMappingLabels.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(label, modifier = Modifier.weight(1f))
                    CompactDropdown(
                        options = viewModel.xrKeycodeNames,
                        selectedOption = viewModel.xrKeycodeNames.getOrElse(viewModel.xrMappingIndices.getOrElse(i) { 0 }) { "" },
                        onSelect = { opt ->
                            val idx = viewModel.xrKeycodeNames.indexOf(opt).coerceAtLeast(0)
                            if (i < viewModel.xrMappingIndices.size) viewModel.xrMappingIndices[i] = idx
                        },
                        modifier = Modifier.width(160.dp)
                    )
                }
                if (i < viewModel.xrMappingLabels.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionBox(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), content = content)
        }
    }
}

@Composable
private fun LabeledDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun CompactDropdown(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
        }
    }
}
