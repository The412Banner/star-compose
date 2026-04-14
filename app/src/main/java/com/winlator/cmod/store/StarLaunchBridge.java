package com.winlator.cmod.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.container.Shortcut;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Launch bridge for store integrations (GOG / Epic / Amazon).
 *
 * Presents a container picker dialog after a game is downloaded, writes a
 * .desktop shortcut into the chosen Wine container's desktop directory, and
 * optionally downloads + saves the game's cover art so the shortcut shows
 * artwork in the Shortcuts screen.
 *
 * Shortcut format (Winlator .desktop):
 *   [Desktop Entry]
 *   Name=<game name>
 *   Exec=wine <Z:\path\to\game.exe>
 *   Icon=
 *   Type=Application
 *   StartupWMClass=explorer
 *
 *   [Extra Data]
 *   customCoverArtPath=<absolute path to PNG>
 */
public final class StarLaunchBridge {

    private static final String TAG = "BH_BRIDGE";

    private StarLaunchBridge() {}

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Show a container picker, write a shortcut, then download cover art.
     *
     * @param activity     calling Activity
     * @param gameName     display name (used as shortcut filename and title)
     * @param exePath      absolute Android path to the .exe (under imagefs/)
     * @param coverArtUrl  URL of the game's cover art image, or null to skip
     */
    public static void addToLauncher(Activity activity,
                                     String gameName,
                                     String exePath,
                                     String coverArtUrl) {
        Handler h = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                ContainerManager manager = new ContainerManager(activity);
                ArrayList<Container> containers = manager.getContainers();

                if (containers == null || containers.isEmpty()) {
                    h.post(() -> Toast.makeText(activity,
                            "No Wine container found — create one first in the Containers screen.",
                            Toast.LENGTH_LONG).show());
                    return;
                }

                String[] names = new String[containers.size()];
                for (int i = 0; i < containers.size(); i++) {
                    String n = containers.get(i).getName();
                    names[i] = (n != null && !n.isEmpty()) ? n : "Container " + (i + 1);
                }

                h.post(() -> new AlertDialog.Builder(activity)
                        .setTitle("Add \"" + gameName + "\" to…")
                        .setItems(names, (dialog, which) ->
                                writeShortcut(activity, containers.get(which),
                                        gameName, exePath, coverArtUrl, h))
                        .setNegativeButton("Cancel", null)
                        .show());

            } catch (Exception e) {
                Log.e(TAG, "addToLauncher failed", e);
                h.post(() -> Toast.makeText(activity,
                        "Error loading containers: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }, "store-launcher-picker").start();
    }

    /**
     * Convenience overload without cover art.
     */
    public static void addToLauncher(Activity activity, String gameName, String exePath) {
        addToLauncher(activity, gameName, exePath, null);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static void writeShortcut(Activity activity,
                                      Container container,
                                      String gameName,
                                      String exePath,
                                      String coverArtUrl,
                                      Handler h) {
        new Thread(() -> {
            try {
                File desktopDir = container.getDesktopDir();
                if (desktopDir == null) {
                    h.post(() -> Toast.makeText(activity,
                            "Container desktop directory not found.",
                            Toast.LENGTH_LONG).show());
                    return;
                }
                if (!desktopDir.exists() && !desktopDir.mkdirs()) {
                    h.post(() -> Toast.makeText(activity,
                            "Could not create container desktop directory.",
                            Toast.LENGTH_LONG).show());
                    return;
                }

                // Sanitise game name → safe filename
                String safeName = gameName.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                if (safeName.isEmpty()) safeName = "game";

                File shortcutFile = new File(desktopDir, safeName + ".desktop");

                // Derive path relative to imagefs root (forward slashes)
                String imageFsRoot = new java.io.File(activity.getFilesDir(), "imagefs").getAbsolutePath();
                String relPath = exePath.startsWith(imageFsRoot)
                        ? exePath.substring(imageFsRoot.length()) : exePath;
                if (relPath.startsWith("/")) relPath = relPath.substring(1);

                // Convert to Windows path — match Winlator's native shortcut format
                // (handleManualShortcutAddition). No WINEPREFIX in Exec=; Winlator
                // derives WINEPREFIX from the container object via container_id.
                // Use 4 backslashes per separator so StringUtils.unescape() produces
                // a valid Z:\path\to\game.exe after its two-pass strip.
                String windowsPath = relPath.replace("/", "\\\\\\\\");

                String content = "[Desktop Entry]\n"
                        + "Name=" + gameName + "\n"
                        + "Exec=wine Z:\\\\\\\\" + windowsPath + "\n"
                        + "Icon=\n"
                        + "Type=Application\n"
                        + "StartupWMClass=explorer\n"
                        + "\n"
                        + "[Extra Data]\n";

                try (FileWriter fw = new FileWriter(shortcutFile)) {
                    fw.write(content);
                }

                Log.d(TAG, "Wrote shortcut: " + shortcutFile.getPath());

                // Download and save cover art
                if (coverArtUrl != null && !coverArtUrl.isEmpty()) {
                    saveCoverArt(activity, container, shortcutFile, safeName, coverArtUrl);
                }

                h.post(() -> Toast.makeText(activity,
                        "\"" + gameName + "\" added to Shortcuts.\n"
                                + "Open the side menu → Shortcuts to launch and configure it.",
                        Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "writeShortcut failed for " + gameName, e);
                h.post(() -> Toast.makeText(activity,
                        "Failed to add shortcut: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }, "store-write-shortcut").start();
    }

    /**
     * Downloads cover art from {@code url} and saves it via
     * {@link Shortcut#saveCustomCoverArt(Bitmap)} so the Shortcuts screen
     * shows the artwork immediately.
     */
    private static void saveCoverArt(Context ctx, Container container,
                                     File shortcutFile, String safeName,
                                     String url) {
        try {
            Log.d(TAG, "Downloading cover art: " + url);
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(20_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                Log.w(TAG, "Cover art HTTP " + code + " for " + url);
                conn.disconnect();
                return;
            }
            Bitmap bmp;
            try (InputStream is = conn.getInputStream()) {
                bmp = BitmapFactory.decodeStream(is);
            }
            conn.disconnect();
            if (bmp == null) {
                Log.w(TAG, "Cover art decode returned null for " + url);
                return;
            }

            // Construct the Shortcut model to call saveCustomCoverArt
            Shortcut shortcut = new Shortcut(container, shortcutFile);
            shortcut.saveCustomCoverArt(bmp);
            Log.d(TAG, "Cover art saved for " + safeName);
        } catch (Exception e) {
            // Non-fatal: shortcut still works, just no art
            Log.w(TAG, "Cover art save failed for " + safeName + ": " + e.getMessage());
        }
    }
}
