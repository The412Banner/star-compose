# Star-Compose — Jetpack Compose Migration Plan
**Repo:** https://github.com/The412Banner/star-compose (main branch)
**Date:** 2026-04-15

---

## Current State

### Already Done ✅
- `MainActivity.kt` + `AppNavGraph` + `AppDrawer` + `AppTopBar` — full Compose shell
- **7 primary screens** fully Compose: `ContainersScreen`, `ContentsScreen`, `ShortcutsScreen`, `AdrenoToolsScreen`, `FileManagerScreen`, `SplashScreen`, `PreloaderOverlay`
- All ViewModels in Kotlin with StateFlow

### Just Cleaned Up (this session)
- Deleted `AdrenotoolsFragment.java` — replaced by `AdrenoToolsScreen.kt`
- Deleted `ContainersFragment.java` — replaced by `ContainersScreen.kt`
- Deleted `ContentsFragment.java` — replaced by `ContentsScreen.kt`
- Deleted `ShortcutsFragment.java` — replaced by `ShortcutsScreen.kt`
- Deleted 4 matching XML layouts: `adrenotools_fragment.xml`, `containers_fragment.xml`, `contents_fragment.xml`, `shortcuts_fragment.xml`
- Removed legacy `ShortcutSettingsDialog(ShortcutsFragment, Shortcut)` constructor — only Compose-friendly `(Context, Shortcut, Runnable)` remains
- Removed dead navigationView/FLFragmentContainer branch from `SettingsFragment`

### What Remains
- **5 fragments** still wrapped via `FragmentScreen` (not native Compose)
- **16 dialogs** all Java/XML AlertDialog
- **6 standalone Activities** fully legacy
- **Store UIs** (Steam/GOG/Epic/Amazon) — Kotlin but View-based, not Compose
- **81 XML layouts** still present (dialogs + list items + preferences + activity layouts)

---

## Migration Phases (Prioritized)

---

### Phase 1 — High-Impact Dialogs (do first)
**Why first:** Dialogs are called from Compose screens right now. Converting them unlocks Material3 theming, proper dark mode, and removes XML inflation from the hottest paths.

| Dialog | File | Priority |
|---|---|---|
| `GraphicsDriverConfigDialog` | `GraphicsDriverConfigDialog.java` | High |
| `DXVKConfigDialog` | `DXVKConfigDialog.java` | High |
| `ShortcutSettingsDialog` | `ShortcutSettingsDialog.java` | High |
| `FPSCounterConfigDialog` | `FPSCounterConfigDialog.java` | Medium |
| `ScreenEffectDialog` | `ScreenEffectDialog.java` | Medium |
| `WineD3DConfigDialog` | `WineD3DConfigDialog.java` | Medium |
| `SaveEditDialog` | `SaveEditDialog.java` | Medium |
| `SaveSettingsDialog` | `SaveSettingsDialog.java` | Medium |
| `ContentInfoDialog` | `ContentInfoDialog.java` | Medium |
| `ContentUntrustedDialog` | `ContentUntrustedDialog.java` | Medium |
| `ActiveWindowsDialog` | `ActiveWindowsDialog.java` | Low |
| `DebugDialog` | `DebugDialog.java` | Low |
| `StorageInfoDialog` | `StorageInfoDialog.java` | Low |
| `FSRControlFloatingDialog` | `FSRControlFloatingDialog.java` | Low |
| `AddEnvVarDialog` | `AddEnvVarDialog.java` | Low |
| `DownloadProgressDialog` | `DownloadProgressDialog.java` | Low |

**XML layouts to delete after each conversion:** matching `*_dialog.xml` file

**Effort:** ~2–3 weeks

---

### Phase 2 — Unwrap FragmentScreen Fragments
**Why second:** These are already in the Compose nav graph via `FragmentScreen`. Replacing them with native Compose screens removes the last XML fragment dependencies from the main nav.

| Fragment | Layout | Notes |
|---|---|---|
| ~~`ContainerDetailFragment`~~ | ~~`container_detail_fragment.xml`~~ | ✅ Replaced by ContainerDetailScreen.kt + ContainerDetailViewModel.kt |
| ~~`SavesFragment`~~ | ~~`saves_fragment.xml`~~ | ✅ Replaced by SavesScreen.kt (commit `b42ff8b`) |
| `InputControlsFragment` | `input_controls_fragment.xml` | Complex custom input UI — hardest of the 5 |
| `SettingsFragment` | `preference_list_fragment.xml` | `PreferenceFragmentCompat` — replace with Compose preference list |
| ~~`AdrenotoolsFragment`~~ | ~~`adrenotools_fragment.xml`~~ | ✅ Already deleted |

**After each conversion:** delete the Java Fragment file + XML layout + remove `FragmentScreen` wrapper from `AppNavGraph`

**Effort:** ~2–3 weeks (InputControlsFragment is the hardest — allow extra time)

---

### Phase 3 — Standalone Legacy Activities
**Why third:** These are separate flows not in the main nav. Lower urgency but they use old XML layouts and don't respect the app's Compose theming.

| Activity | Layout | Notes |
|---|---|---|
| `BigPictureActivity` | `big_picture_activity.xml` | TV mode gallery — convert to Compose `setContent` |
| `ControlsEditorActivity` | `controls_editor_activity.xml` | Controller mapping canvas — complex custom drawing |
| `ExternalControllerBindingsActivity` | `external_controller_bindings_activity.xml` | Gamepad binding list |
| `ShortcutPickerActivity` | *(programmatic)* | Shortcut selection — convert to Compose dialog |
| `XServerDisplayActivity` | `xserver_display_activity.xml` | X Server surface — likely stays native (SurfaceView) |
| `XrActivity` | *(programmatic)* | VR/XR mode — likely stays native |

**Effort:** ~3–4 weeks

---

### Phase 4 — Store UIs (Optional / Lower Priority)
**Why last:** Store UIs are self-contained subflows. They work today and are Kotlin even if not Compose. Migrate when the rest of the app is done.

| Screen | File | Notes |
|---|---|---|
| `SteamGamesActivity` | `SteamGamesActivity.kt` | ListView → `LazyColumn` |
| `SteamGameDetailActivity` | `SteamGameDetailActivity.kt` | Detail layout → Compose |
| `SteamLoginActivity` | `SteamLoginActivity.kt` | Login form → Compose |
| `SteamMainActivity` | `SteamMainActivity.kt` | Entry screen → Compose |
| GOG screens | `GogMainActivity`, `GogGamesActivity`, `GogLoginActivity` | Same pattern |
| Epic screens | `EpicMainActivity`, `EpicGamesActivity`, `EpicLoginActivity` | Same pattern |
| Amazon screens | `AmazonMainActivity`, `AmazonGamesActivity`, `AmazonLoginActivity` | Same pattern |

**Effort:** ~2–3 weeks

---

## Summary

| Phase | Work | Effort | Priority |
|---|---|---|---|
| ~~Cleanup~~ | ~~Delete 4 dead fragments + 4 XML layouts~~ | ~~Done~~ | ~~Done~~ |
| Phase 1 | Convert 16 dialogs to Compose | 2–3 weeks | High |
| Phase 2 | Unwrap 4 remaining FragmentScreen fragments | 2–3 weeks | High |
| Phase 3 | Convert 5 standalone Activities | 3–4 weeks | Medium |
| Phase 4 | Convert Store UIs | 2–3 weeks | Low |

**Total remaining: ~9–13 weeks for full migration**
**Core app only (Phase 1 + 2): ~4–6 weeks**

---

## Also Pending (see TODO.md)
- Goldberg Auto Patcher
- Steam Playtest Support (`it.type == "beta"` filter fix)
