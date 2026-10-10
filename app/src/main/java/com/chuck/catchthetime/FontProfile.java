package com.chuck.catchthetime;

import android.content.Context;
import android.graphics.Typeface;

public final class FontProfile {
    public final String id;
    public final String label;
    public final Typeface display;
    public final Typeface body;
    public final Typeface medium;
    public final Typeface time;
    public final float titleSpacing;
    public final float eyebrowSpacing;
    public final float timeSpacing;

    private FontProfile(String id, String label, Typeface display, Typeface body,
                        Typeface medium, Typeface time, float titleSpacing,
                        float eyebrowSpacing, float timeSpacing) {
        this.id = id;
        this.label = label;
        this.display = display;
        this.body = body;
        this.medium = medium;
        this.time = time;
        this.titleSpacing = titleSpacing;
        this.eyebrowSpacing = eyebrowSpacing;
        this.timeSpacing = timeSpacing;
    }

    public static FontProfile load(Context context) {
        return forId(SettingsStore.fontId(context));
    }

    public static FontProfile forId(String id) {
        String safe = id == null ? "clean" : id;
        switch (safe) {
            case "neo":
                return new FontProfile(
                        "neo", "Neo",
                        Typeface.create("sans-serif-condensed", Typeface.BOLD),
                        Typeface.create("sans-serif", Typeface.NORMAL),
                        Typeface.create("sans-serif-medium", Typeface.NORMAL),
                        Typeface.create("sans-serif-light", Typeface.NORMAL),
                        0.12f, 0.18f, 0.055f
                );
            case "retro":
                return new FontProfile(
                        "retro", "Retro",
                        Typeface.create("monospace", Typeface.BOLD),
                        Typeface.create("monospace", Typeface.NORMAL),
                        Typeface.create("monospace", Typeface.BOLD),
                        Typeface.create("monospace", Typeface.BOLD),
                        0.035f, 0.08f, 0.015f
                );
            case "classic":
                return new FontProfile(
                        "classic", "Classic",
                        Typeface.create("serif", Typeface.BOLD),
                        Typeface.create("sans-serif", Typeface.NORMAL),
                        Typeface.create("sans-serif-medium", Typeface.NORMAL),
                        Typeface.create("serif", Typeface.BOLD),
                        0.02f, 0.11f, 0.01f
                );
            default:
                return new FontProfile(
                        "clean", "Clean",
                        Typeface.create("sans-serif", Typeface.BOLD),
                        Typeface.create("sans-serif", Typeface.NORMAL),
                        Typeface.create("sans-serif-medium", Typeface.NORMAL),
                        Typeface.create("sans-serif", Typeface.BOLD),
                        0.055f, 0.14f, 0.01f
                );
        }
    }
}
