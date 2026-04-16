# Goldberg Steam Emulator — Integration Report
**Project:** star-compose (`winlator-test` / `main` branch)
**Date:** 2026-04-15
**Status:** Under consideration — not yet implemented

---

## What Is Goldberg?

Goldberg Steam Emulator is an open-source replacement for `steam_api.dll` / `steam_api64.dll` — the DLLs every Steam game links against to talk to the Steam client. By replacing these DLLs with Goldberg's implementation, the game thinks Steam is running locally without requiring the actual Steam client to be present.

**What it emulates:**
- Steam "is running" check — game launches without Steam in the background
- Achievements and stats (stored locally)
- Cloud save stubs
- DLC ownership checks (all DLC reported as owned)
- LAN multiplayer (via local broadcast, no Steam servers)

**Active maintained fork:** `otavepto/gbe_fork` (GitHub) — supports a wide range of Steam API interface versions, actively updated.

**DLL sizes:** ~500 KB each (x86 + x64). Suitable for bundling as APK assets.

---

## Why It's Relevant to Star-Compose

Games installed via `SteamDepotDownloader` are real Steam depots — the game files are identical to what the Steam client would install. However, they still contain the real `steam_api.dll` which expects the Steam client to be running. Without Goldberg:

- The game must launch through Steam (heavy Wine overhead, Steam background process required)
- Offline play may be blocked depending on the title
- Direct shortcut launch (bypassing Steam entirely) fails for many titles

With Goldberg applied post-install, games can launch directly as a Wine shortcut with no Steam client dependency.

---

## Proposed Implementation

### Hook Point

`SteamDepotDownloader.kt` — `onDownloadCompleted` callback (line ~275):

```kotlin
override fun onDownloadCompleted(item: DownloadItem) {
    db.markInstalled(appId, installDir.absolutePath, finalBytes)
    repo.emit("DownloadComplete:$appId")
    // Hook: GoldbergPatcher.patch(ctx, appId, installDir)
}
```

The install directory (`ctx.filesDir/imagefs/steam_games/{gameName}`) and `appId` are both available at this point.

### Assets to Bundle

```
assets/goldberg/steam_api.dll      — x86  Goldberg DLL (~500 KB)
assets/goldberg/steam_api64.dll    — x64  Goldberg DLL (~500 KB)
```

### Patch Logic (`GoldbergPatcher` object)

1. Walk install directory recursively
2. Find every `steam_api.dll` and `steam_api64.dll`
3. Skip files inside known non-game subdirectories:
   - `_CommonRedist`, `directx`, `vcredist`, `dotnet`, `physx`, `setup`
4. For each target DLL found:
   - Back up original as `steam_api.dll.bak` / `steam_api64.dll.bak`
   - Copy Goldberg version from assets in its place
   - Create `steam_settings/` folder alongside the DLL
   - Write `steam_settings/steam_appid.txt` containing the game's AppID
5. Record patched state (flag in DB or a marker file in install dir)

### Exposure in UI

- **Auto on install** (optional — see risks below)
- **Manual toggle** in game detail screen: "Apply Goldberg Patch" / "Restore Original"
- Re-apply automatically if game is re-installed or resumed from a partial download

---

## Risks and Issues

### 1. Legal / Distribution

- Goldberg itself is open source and legal to use and distribute
- However, bundling it inside a publicly distributed APK could draw attention from Valve, as it makes the app look explicitly like a piracy-enablement tool — even when users own their games legitimately
- The app is already in a grey area (Steam client running inside Wine on Android); adding auto-patching increases that surface area
- Google Play would reject this outright (not currently relevant, but worth noting for future distribution options)

### 2. Compatibility — Biggest Real Risk

Not every game works with Goldberg. Specific failure cases:

| Scenario | Result |
|---|---|
| Game uses Denuvo DRM on top of Steam | Goldberg does nothing — Denuvo check still fails |
| Game uses Steam's CEG (Custom Executable Generation) | Game binary itself is encrypted — patching DLL is irrelevant |
| Game verifies its own DLL integrity at startup | Crash or silent failure on launch |
| Game uses non-standard Steam API interfaces | Goldberg DLL may not implement that interface version |
| Multiplayer-only game (requires Steam auth server) | Online features broken, game may be unplayable |

Silent failure is the worst outcome — the user sees the game not launching with no explanation and attributes it to the app being broken.

### 3. User Experience

- If auto-patching is applied and breaks a game, the user has no obvious path to diagnose the problem
- Without a backup, the only fix is re-installing the game
- No UI feedback on whether patching succeeded or was skipped

### 4. Patch Durability

- If the user resumes a partial download or re-installs, the patch is overwritten by DepotDownloader and must be reapplied
- Games with active mod communities or manual update workflows may ship a fresh `steam_api.dll` that overwrites Goldberg

### 5. DLL Filtering Accuracy

The subdirectory exclusion list needs to be comprehensive. A missed `_CommonRedist` entry is harmless; accidentally skipping the real game DLL (e.g. a game that installs into an unusual subdirectory) means the patch silently does nothing.

---

## Mitigations

| Risk | Mitigation |
|---|---|
| Breaks incompatible games silently | Make it opt-in per game, not auto on install |
| No recovery path | Always back up original DLL before replacing |
| User confusion on failure | Show clear warning in UI before applying |
| Patch overwritten on resume/reinstall | Check marker file + reapply automatically if needed |
| Wrong DLL targeted | Maintain and expand exclusion path list |
| Compatibility unknown | Add a note in UI: "Works best with games that don't use additional DRM (Denuvo, CEG)" |

---

## Why Not a Lightweight Steam Client?

A natural question is whether a lightweight Steam client could bridge the gap — something smaller than the full Steam client that still maintains a real Steam connection. The honest answer is: **no such thing exists.**

### What Actually Exists

**Category 1 — Steam API Emulators (no real Steam connectivity)**

These replace `steam_api.dll` so the game thinks Steam is running, but there is no actual connection to Steam's servers. No real account authentication, no real online features.

| Tool | Notes |
|---|---|
| Goldberg (this report) | Best maintained, wide API version coverage |
| SmartSteamEmu (SSE) | Older, structured more like a mini launcher, less maintained |
| ColdAPI Steam | Similar to Goldberg, less active development |
| CreamAPI | DLC unlocking only, not a full emulator |

All of these are the same category — fake Steam, no real connectivity.

**Category 2 — The Real Steam Client, Made Lighter**

If real Steam connectivity is required (actual online multiplayer, real cloud saves, Steam friends), the full Steam client is the only option. Its resource footprint can be reduced with launch flags:

| Flag | Effect |
|---|---|
| `-silent` | Starts minimised with no window |
| `-no-browser` | Disables the built-in Chromium browser — largest single RAM saving |
| `-no-cef-sandbox` | Improves compatibility inside Wine |
| Small Mode | Minimal Steam UI, less rendering overhead |

These help, but Steam is still a multi-hundred MB process running inside Wine. There is no official or community-built slim client that maintains a real Steam connection.

### The Real Decision

There is no middle ground. The choice is binary:

| Approach | Connectivity | Overhead |
|---|---|---|
| Goldberg / SSE | None — fully emulated locally | ~1 MB, zero background process |
| Full Steam client (with flags) | Real — account, online, cloud saves | ~300–500 MB RAM inside Wine |

**For star-compose this means:**
- Users who only need to launch their owned game offline → Goldberg is the right answer
- Users who need real online multiplayer via Steam → must run the full Steam client; no shortcut exists
- Users who need real Steam cloud saves → must run the full Steam client

Goldberg does not compete with the real Steam client for online use cases. It only replaces it for the offline / direct-launch use case.

---

## Recommendation

**Do not auto-apply on install.** Instead:

1. Add an opt-in "Apply Goldberg Patch" button in the game detail screen
2. Show a short warning explaining what it does and that some games may not work
3. Always back up the original DLL
4. Expose a "Restore Original DLLs" option alongside it
5. Re-apply automatically only if the user had previously opted in and the game is re-downloaded

This approach gives users who want direct-launch capability a clean path to it, while not silently breaking games for users who don't know what Goldberg is.

---

## Files That Would Be Touched

| File | Change |
|---|---|
| `SteamDepotDownloader.kt` | Call `GoldbergPatcher.patch()` from `onDownloadCompleted` |
| `SteamGameDetailActivity.kt` | Add patch/restore toggle UI |
| `GoldbergPatcher.kt` | New file — patch logic |
| `assets/goldberg/steam_api.dll` | New asset |
| `assets/goldberg/steam_api64.dll` | New asset |
| `SteamDatabase.kt` (or equivalent) | Store `goldberg_patched` flag per game |

---

*Report prepared for star-compose — implementation pending decision.*
