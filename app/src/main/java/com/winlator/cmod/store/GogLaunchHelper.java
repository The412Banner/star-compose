package com.winlator.cmod.store;

import android.app.Activity;

/**
 * Launch bridge for GOG games.
 * Delegates to LudashiLaunchBridge, forwarding the game's cover art URL
 * so the shortcut shows artwork in the Shortcuts screen.
 */
public final class GogLaunchHelper {

    private GogLaunchHelper() {}

    /** Add a GOG game to the launcher with cover art. */
    public static void addToLauncher(Activity activity, String gameName,
                                     String exePath, String coverArtUrl) {
        LudashiLaunchBridge.addToLauncher(activity, gameName, exePath, coverArtUrl);
    }

    /** Backwards-compatible overload without cover art. */
    public static void addToLauncher(Activity activity, String gameName, String exePath) {
        LudashiLaunchBridge.addToLauncher(activity, gameName, exePath, null);
    }
}
