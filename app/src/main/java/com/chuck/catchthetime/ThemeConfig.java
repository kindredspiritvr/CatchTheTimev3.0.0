package com.chuck.catchthetime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public final class ThemeConfig {
    public static final String KEY_PRESET = "theme_preset";
    public static final String KEY_BG = "theme_bg";
    public static final String KEY_SURFACE = "theme_surface";
    public static final String KEY_TEXT = "theme_text";
    public static final String KEY_TIME = "theme_time";
    public static final String KEY_MUTED = "theme_muted";
    public static final String KEY_CURRENT = "theme_current";
    public static final String KEY_NEXT = "theme_next";
    public static final String KEY_ACCENT = "theme_accent";

    public final int background;
    public final int surface;
    public final int text;
    public final int time;
    public final int muted;
    public final int current;
    public final int next;
    public final int accent;

    private ThemeConfig(int background, int surface, int text, int time, int muted,
                        int current, int next, int accent) {
        this.background = background;
        this.surface = surface;
        this.text = text;
        this.time = time;
        this.muted = muted;
        this.current = current;
        this.next = next;
        this.accent = accent;
    }

    public static ThemeConfig load(Context context) {
        SharedPreferences p = context.getSharedPreferences(SettingsStore.PREFS, Context.MODE_PRIVATE);
        String preset = p.getString(KEY_PRESET, "midnight");
        if (!"custom".equals(preset)) return preset(preset);
        ThemeConfig fallback = preset("midnight");
        return new ThemeConfig(
                parse(p.getString(KEY_BG, hex(fallback.background)), fallback.background),
                parse(p.getString(KEY_SURFACE, hex(fallback.surface)), fallback.surface),
                parse(p.getString(KEY_TEXT, hex(fallback.text)), fallback.text),
                parse(p.getString(KEY_TIME, hex(fallback.time)), fallback.time),
                parse(p.getString(KEY_MUTED, hex(fallback.muted)), fallback.muted),
                parse(p.getString(KEY_CURRENT, hex(fallback.current)), fallback.current),
                parse(p.getString(KEY_NEXT, hex(fallback.next)), fallback.next),
                parse(p.getString(KEY_ACCENT, hex(fallback.accent)), fallback.accent)
        );
    }

    public static ThemeConfig preset(String id) {
        String safe = id == null ? "midnight" : id;
        switch (safe) {
            case "pocket":
                return of("#202A22", "#2D392F", "#D9E1C8", "#E7F0D4", "#829078", "#B8D94A", "#E5B94B", "#A7C957");
            case "neon":
                return of("#071118", "#0C1A24", "#E9FAFF", "#FFFFFF", "#7593A2", "#39F2C3", "#64B8FF", "#A675FF");
            case "terminal":
                return of("#07120B", "#0D1C12", "#E9FFF0", "#B8FFC9", "#6F9E7C", "#65F28C", "#D4F75C", "#65F28C");
            case "amber":
                return of("#120D08", "#1C150D", "#FFF4E5", "#FFD08A", "#A98E6D", "#7CF2A3", "#FFB547", "#FFB547");
            case "paper":
            case "cream":
                return of("#F3EEE4", "#FFF9F0", "#29251F", "#332C24", "#7F7569", "#287B55", "#B86525", "#6759D6");
            case "vapor":
                return of("#120B20", "#1B1230", "#FFF2FF", "#F7D7FF", "#A88FB8", "#63F5D2", "#FFB15C", "#D574FF");
            case "ocean":
                return of("#07131A", "#0C1E28", "#EAF9FF", "#C9F1FF", "#779BAA", "#5EE6A8", "#69B7FF", "#69B7FF");
            default:
                return of("#090D12", "#121821", "#F3F6FA", "#FFFFFF", "#8993A1", "#4CDB7D", "#FFB547", "#7CF2A3");
        }
    }

    private static ThemeConfig of(String bg, String surface, String text, String time,
                                  String muted, String current, String next, String accent) {
        return new ThemeConfig(
                Color.parseColor(bg), Color.parseColor(surface), Color.parseColor(text),
                Color.parseColor(time), Color.parseColor(muted), Color.parseColor(current),
                Color.parseColor(next), Color.parseColor(accent)
        );
    }

    public boolean isDark() {
        double luma = 0.2126 * Color.red(background) + 0.7152 * Color.green(background) + 0.0722 * Color.blue(background);
        return luma < 145;
    }

    public static String hex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    public static int parse(String value, int fallback) {
        try { return Color.parseColor(value); }
        catch (Exception e) { return fallback; }
    }
}
