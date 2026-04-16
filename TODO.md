# Star-Compose — To-Do List

---

## 1. Goldberg Auto Patcher
**Status:** Under consideration
**Report:** `GOLDBERG_PATCH_REPORT.md`

Apply Goldberg Steam Emulator DLLs to installed Steam games so they can launch directly without the Steam client running.

**Tasks:**
- [ ] Source `steam_api.dll` + `steam_api64.dll` from `otavepto/gbe_fork` (latest release)
- [ ] Add both DLLs as APK assets under `assets/goldberg/`
- [ ] Create `GoldbergPatcher.kt` — recursive DLL finder, exclusion list, backup + replace logic, `steam_settings/steam_appid.txt` writer
- [ ] Add `goldberg_patched` flag to `SteamDatabase`
- [ ] Add "Apply Goldberg Patch" / "Restore Original" toggle in `SteamGameDetailActivity`
- [ ] Re-apply patch automatically if user had it enabled and game is re-downloaded
- [ ] Show warning in UI: works best with games that don't use additional DRM (Denuvo, CEG)

**Decision pending:** opt-in per game (recommended) vs auto on install

---

## 2. Steam Playtest Support
**Status:** Under consideration

Show Steam Playtest games (type `"beta"`) in the user's library alongside regular games.

**Tasks:**
- [ ] `SteamGamesActivity.kt:121` — change filter from `it.type == "game"` to `it.type == "game" || it.type == "beta"`
- [ ] Verify playtest entries display correctly (cover art, download, launch flow)
- [ ] Optional: add a "Playtest" badge/label on the game card so users can distinguish them from full games

**Notes:**
- Only shows playtests the user has already been granted access to — no API exists to browse or request access to new playtests programmatically
- One-line fix for the filter; the rest of the pipeline (download, install, launch) already works

---
