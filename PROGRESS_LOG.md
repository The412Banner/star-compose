# Star-Compose — Progress Log

**Repo:** https://github.com/The412Banner/star-compose (main branch)  
**Mirror:** https://github.com/kalteatz24/winlator-test (star-compose branch)  
**Local:** `/data/data/com.termux/files/home/winlator-test`  
**Always push to both remotes after every commit:**
```
git push star-compose star-compose:main
git push kalteatz24 star-compose:star-compose
```
**Then trigger CI:**
```
gh workflow run "Any branch compilation." --repo The412Banner/star-compose --ref main
```

---

## How to Resume a Session

1. Read this file top to bottom
2. Find the **Current Job** section — it tells you exactly what to do next
3. Check the last commit hash matches what's on GitHub before continuing
4. Run CI after every commit. Do not continue to the next job until CI is green.

---

## Completed Work (Pre-Plan)

Full Jetpack Compose migration of all screens and dialogs is complete.  
See `COMPOSE_MIGRATION_REPORT.md` for the full record.

**Last migration commit:** `6dff28e`  
**Bug fixes after migration:**
- `85b1e57` — controller name text + drive letter dropdown fix
- `6537038` — External Controllers header text fix
- `3323810` — Customizable theme: 8 presets + HSV color picker (AppearanceScreen)
- `beee77b` — Appearance entry missing from nav drawer (AppDrawer hardcoded)

**Latest commit:** `beee77b`  
**Latest CI:** run `24568759383` — in progress at time of writing

---

## Feedback Fix Plan

Source: Developer feedback comparing v1.1 (old Java/XML) vs Compose version.  
8 issues identified. Listed in execution order (smallest/highest impact first).

---

### Job 1 — Help and Support (BROKEN)
**Status:** ✅ COMPLETE — commit `93d0326`, CI run `24569312463`  
**File:** `app/src/main/java/com/winlator/cmod/ui/AppDrawer.kt`  
**Problem:** `onClick = { /* TODO: open help URL or dialog */ }` — tapping does nothing  
**Fix:** Replace the TODO with a Compose `AlertDialog` containing:
- GitHub repo link: https://github.com/The412Banner/star-compose
- Issue tracker link
- A "Close" button
Or alternatively open a URL via `Intent(Intent.ACTION_VIEW, Uri.parse(url))`.  
**Effort:** 30 min  
**Commit message:** `fix: implement Help and Support dialog`

---

### Job 2 — About Dialog (MISSING CONTENT)
**Status:** ✅ COMPLETE — commit `d18cae6`, CI run `24569669122`  
**File:** `app/src/main/java/com/winlator/cmod/MainActivity.kt` — `AboutDialog()` at bottom of file  
**Problem:** Current dialog is 4 lines of plain text. Missing: app icon/logo, version name, Wine/Box64/FEX versions, credits list.  
**Fix:** Rebuild `AboutDialog()` as a proper Compose `Dialog` (not AlertDialog — needs more space) with:
- App icon (R.mipmap.ic_launcher_foreground)
- App name + version (read from `BuildConfig.VERSION_NAME` + `BuildConfig.VERSION_CODE`)
- Powered-by section: Wine, Box64, FEX-Emu, Turnip
- Credits section with contributor names
- Close button  
**Effort:** 45 min  
**Commit message:** `feat: rebuild About dialog with logo, version, credits`

---

### Job 3 — Container Creation Loading Indicator
**Status:** ✅ COMPLETE — commit `2e5f4a1`, CI run `24570142005`  
**Files:**
- `app/src/main/java/com/winlator/cmod/ui/screens/ContainerDetailScreen.kt` — Save button / confirm action
- `app/src/main/java/com/winlator/cmod/ui/screens/ContainerDetailViewModel.kt` — `saveContainer()` or equivalent
**Problem:** When user taps Save on a new container, it creates silently with no progress feedback. On slow devices this looks like a freeze.  
**Fix:**
1. Add `isCreating: StateFlow<Boolean>` to `ContainerDetailViewModel`
2. Set it true before container creation starts, false when done
3. In `ContainerDetailScreen`, show a full-screen semi-transparent overlay with `CircularProgressIndicator` + "Creating container…" text when `isCreating == true`
4. Disable the Save button while creating  
**Effort:** 45 min  
**Commit message:** `feat: add loading overlay during container creation`

---

### Job 4 — Settings Theme Mismatch (Dark Mode Toggle Broken)
**Status:** ✅ COMPLETE — commit `44a4bdb`, CI run `24571445525`  
**Files:**
- `app/src/main/java/com/winlator/cmod/ui/theme/AppThemeState.kt`
- `app/src/main/java/com/winlator/cmod/ui/theme/ThemePreset.kt`
- `app/src/main/java/com/winlator/cmod/ui/theme/Theme.kt`
- `app/src/main/java/com/winlator/cmod/MainActivity.kt`
**Problem (two parts):**
1. `SettingsFragment` uses Light XML AppTheme while the rest of the app is dark Compose — mismatched look inside the Settings screen
2. The `dark_mode` SharedPreferences toggle in SettingsFragment has no effect on the Compose UI — `WinlatorTheme` always uses `darkColorScheme()`  
**Fix:**
1. Read `PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_mode", false)` in `AppThemeState.init()` and store it as `isDarkMode: StateFlow<Boolean>`
2. Add a light variant to each `ThemePreset` (or use Material3 `lightColorScheme()` as the light base)
3. `AppThemeState.colorScheme` flow emits light or dark scheme based on `isDarkMode`
4. Register a `SharedPreferences.OnSharedPreferenceChangeListener` so toggling dark mode in Settings updates the flow in real time without restart
5. For SettingsFragment XML mismatch: set `android:theme="@style/Theme.AppCompat.DayNight"` on the fragment's parent or override the fragment background to match Compose surface color  
**Effort:** 1.5 hours  
**Commit message:** `fix: wire dark_mode preference to Compose theme + fix Settings appearance`

---

### Job 5 — Sort Shortcut List
**Status:** ✅ COMPLETE — commit `00dc6a5`, CI run `24571836336`  
**File:** `app/src/main/java/com/winlator/cmod/ui/screens/ShortcutsScreen.kt`  
**Problem:** No sort option — shortcuts always appear in filesystem order  
**Fix:**
1. Add a sort icon button in the top bar or a sort dropdown in the shortcuts screen
2. Sort options: Name A→Z, Name Z→A, Last Played, Container
3. Store selected sort in `ShortcutsViewModel` (persisted to SharedPreferences)
4. Apply sort to the `shortcuts` StateFlow before emitting  
**Effort:** 1 hour  
**Commit message:** `feat: add sort options to shortcuts list`

---

### Job 6 — Import/Export Container
**Status:** ✅ COMPLETE — commit `8477b65`, CI run `24572308670`  
**Files:**
- `app/src/main/java/com/winlator/cmod/ui/screens/ContainersScreen.kt`
- `app/src/main/java/com/winlator/cmod/ui/screens/ContainersViewModel.kt`
**Problem:** The old `ContainersFragment` had import/export container options. These are missing from the Compose version.  
**Fix:**
1. Add "Import Container" and "Export Container" options to the container long-press context menu (already has Duplicate/Delete)
2. Check original `ContainersFragment.java` (deleted) — refer to git history if needed, or find the logic in `ContainerManager.java`
3. Export: zip the container directory → write to Downloads or user-picked location via `ActivityResultContracts.CreateDocument`
4. Import: user picks a zip via `ActivityResultContracts.GetContent` → unzip to containers directory → reload list  
**Check ContainerManager.java for existing import/export methods first** — they likely already exist.  
**Effort:** 1.5 hours  
**Commit message:** `feat: add import/export container to containers screen`

---

### Job 7 — Add Shortcut from External Storage
**Status:** ✅ COMPLETE — commit `546d25e`, CI run `24577265773`  
**Files:** `ShortcutsViewModel.kt`, `ShortcutsScreen.kt`

---

### Job 8 — Shortcut List Layout Toggle (Grid / List)
**Status:** ✅ COMPLETE — commit `546d25e`, CI run `24577265773`  
**Files:** `ShortcutsViewModel.kt`, `ShortcutsScreen.kt`

---

## Execution Order

```
Job 1 → Job 2 → Job 3 → Job 4 → Job 5 → Job 6 → Job 7 → Job 8
```

Each job: implement → commit → push both remotes → trigger CI → wait for green → update this log → proceed.

---

## Build Log

| Job | Commit | CI Run | Result | Date |
|---|---|---|---|---|
| Pre-plan: Appearance drawer fix | `beee77b` | `24568759383` | ✅ green | 2026-04-17 |
| Job 1: Help and Support dialog | `93d0326` | `24569312463` | ✅ green | 2026-04-17 |
| Job 2: About dialog rebuild | `d18cae6` | `24569669122` | ✅ green | 2026-04-17 |
| Job 3: Container creation loading overlay | `2e5f4a1` | `24570142005` | ✅ green (fix: `67844d2`) | 2026-04-17 |
| Job 4: Dark mode pref + Settings theme fix | `44a4bdb` | `24571445525` | ✅ green | 2026-04-17 |
| Job 5: Sort shortcuts list | `00dc6a5` | `24571836336` | ✅ green | 2026-04-17 |
| Job 6: Import/Export container | `8477b65` | `24572308670` | ✅ green | 2026-04-17 |
| Job 7+8: Import shortcut + grid/list toggle | `546d25e` | `24577265773` | ✅ green | 2026-04-17 |
| fix: skip enumerateExtensions for AdrenoTools (wrong approach) | `fc2b422` | — | superseded | 2026-04-19 |
| fix: guard atVersionsLoaded race (wrong approach) | `113b483` | — | superseded | 2026-04-19 |
| fix: restore enumerateExtensions on main thread | `8035420` | `24661167232` | ✅ green (partial — isDriverSupported still on IO) | 2026-04-20 |
| fix: move isDriverSupported() to main thread — full fix | `e22815c` | `24662739330` | ✅ green ✅ confirmed working | 2026-04-20 |

---

## Current Job

**→ ALL 8 JOBS COMPLETE + AdrenoTools/Turnip driver SIGSEGV fixed** ✅

---

## Recent Work (2026-04-22)

| Commit | Description | CI Run | Result |
|---|---|---|---|
| `dfd9ba6` | Fix null icon crash when pinning shortcut | — | pre-existing |
| `d1be7af` | Fancy splash screen + fix custom accent theme colors | `24788064210` | ❌ failed (missing clipRect import) |
| `3ee9fee` | Merge 5 upstream commits from kalteatz24/winlator-test | — | — |
| `65712d6` | fix: add missing clipRect import in SplashScreen | `24788390969` | ✅ green |

### Splash Screen enhancements (`SplashScreen.kt`)
- White 4-point sparkles floating around logo
- Logo pulse animation (scale 1.0→1.07)
- Glowing progress bar with shimmer sweep
- Cycling status text with animated dots
- Smooth animated percentage counter
- Proceed button fades/scales in on completion

### Theme fix (`AppThemeState.kt`)
- Custom accent now inherits background/surface from the active preset instead of resetting to gray

### Upstream merge (5 commits from kalteatz24)
- Remove numControllers event file pre-creation (XServerDisplayActivity)
- Add PlugPlay to changeServicesStatus exclusion list (WineUtils)
- Remove softRelease() method (FakeInputWriter)
- Switch controller disconnect from softRelease to destroy (WinHandler)
- Fix busy-loop + dead-file detection in native read() hook (fakeinput.cpp)

| `e8acb58` | fix: wire drawer and top bar to MaterialTheme colors | `24790554006` | 🔄 in progress |

**Last commit:** `e8acb58`  
**Last CI:** `24790554006` 🔄 in progress (2026-04-22)

---

## AdrenoTools/Turnip Driver Fix (2026-04-20)

**Root cause:** The Compose migration moved `GPUInformation` JNI calls onto background coroutine threads (`Dispatchers.IO`). The AdrenoTools `hook_android_dlopen_ext` is not reentrant across threads — concurrent invocations from main + IO thread cause SIGSEGV.

**All `GPUInformation` native methods must run on the main thread:**
- `enumerateExtensions()` — moved to main thread via `LaunchedEffect(version)` (no `withContext`)
- `isDriverSupported()` / `getRenderer()` — moved to main thread in `LaunchedEffect(Unit)`, outside `withContext(Dispatchers.IO)`
- Pure file I/O (`enumarateInstalledDrivers()`, `gpu_cards.json`) stays on `Dispatchers.IO`

**File:** `ContainerDetailScreen.kt` → `GraphicsDriverConfigDialog` composable
