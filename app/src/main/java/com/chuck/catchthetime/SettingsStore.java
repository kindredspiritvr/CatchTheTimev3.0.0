package com.chuck.catchthetime;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SettingsStore {
    public static final String PREFS = "catch_time_prefs";
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_PHRASE = "phrase";
    public static final String KEY_EMOJI = "emoji_enabled";
    public static final String KEY_HOME_COMPACT = "home_compact";
    public static final String KEY_MODE = "time_mode";
    public static final String KEY_CUSTOM_TIMES = "custom_times";
    public static final String KEY_PACK_REPEATS = "pack_repeats";
    public static final String KEY_PACK_ASC = "pack_ascending";
    public static final String KEY_PACK_DESC = "pack_descending";
    public static final String KEY_PACK_MIRROR = "pack_mirrors";
    public static final String KEY_NOTIFICATION_STYLE = "notification_style";
    public static final String KEY_FONT = "font_profile";

    private SettingsStore() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String phrase(Context c) {
        String v = prefs(c).getString(KEY_PHRASE, "Catch the time!");
        return v == null || v.trim().isEmpty() ? "Catch the time!" : v.trim();
    }

    public static boolean emojis(Context c) { return prefs(c).getBoolean(KEY_EMOJI, true); }
    public static boolean compactHome(Context c) { return prefs(c).getBoolean(KEY_HOME_COMPACT, false); }
    public static String mode(Context c) { return prefs(c).getString(KEY_MODE, "mixed"); }
    public static String notificationStyle(Context c) { return prefs(c).getString(KEY_NOTIFICATION_STYLE, "normal"); }
    public static String fontId(Context c) { return prefs(c).getString(KEY_FONT, "clean"); }

    public static boolean packEnabled(Context c, String pack) {
        String key;
        switch (pack) {
            case "ascending": key = KEY_PACK_ASC; break;
            case "descending": key = KEY_PACK_DESC; break;
            case "mirrors": key = KEY_PACK_MIRROR; break;
            default: key = KEY_PACK_REPEATS;
        }
        return prefs(c).getBoolean(key, true);
    }

    public static Set<String> customTimes(Context c) {
        return new HashSet<>(prefs(c).getStringSet(KEY_CUSTOM_TIMES, new HashSet<>()));
    }

    public static void setCustomTimes(Context c, Set<String> values) {
        prefs(c).edit().putStringSet(KEY_CUSTOM_TIMES, new HashSet<>(values)).apply();
    }

    public static String exportCode(Context c) throws Exception {
        SharedPreferences p = prefs(c);
        JSONObject j = new JSONObject();
        j.put("v", 3);
        j.put("phrase", phrase(c));
        j.put("emoji", emojis(c));
        j.put("compact", compactHome(c));
        j.put("mode", mode(c));
        j.put("style", notificationStyle(c));
        j.put("font", fontId(c));
        j.put("repeats", p.getBoolean(KEY_PACK_REPEATS, true));
        j.put("ascending", p.getBoolean(KEY_PACK_ASC, true));
        j.put("descending", p.getBoolean(KEY_PACK_DESC, true));
        j.put("mirrors", p.getBoolean(KEY_PACK_MIRROR, true));
        j.put("themePreset", p.getString(ThemeConfig.KEY_PRESET, "midnight"));
        String[] colorKeys = {
                ThemeConfig.KEY_BG, ThemeConfig.KEY_SURFACE, ThemeConfig.KEY_TEXT,
                ThemeConfig.KEY_TIME, ThemeConfig.KEY_MUTED, ThemeConfig.KEY_CURRENT,
                ThemeConfig.KEY_NEXT, ThemeConfig.KEY_ACCENT
        };
        JSONObject colors = new JSONObject();
        for (String key : colorKeys) if (p.contains(key)) colors.put(key, p.getString(key, ""));
        j.put("colors", colors);
        JSONArray custom = new JSONArray();
        for (String t : customTimes(c)) custom.put(t);
        j.put("custom", custom);
        return "CTT3:" + Base64.encodeToString(j.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    public static void importCode(Context c, String code) throws Exception {
        String raw = code == null ? "" : code.trim();
        if (raw.startsWith("CTT3:")) raw = raw.substring(5);
        else if (raw.startsWith("CTT2:")) raw = raw.substring(5);

        JSONObject j = new JSONObject(new String(Base64.decode(raw, Base64.DEFAULT), StandardCharsets.UTF_8));
        SharedPreferences.Editor e = prefs(c).edit();
        e.putString(KEY_PHRASE, j.optString("phrase", "Catch the time!"));
        e.putBoolean(KEY_EMOJI, j.optBoolean("emoji", true));
        e.putBoolean(KEY_HOME_COMPACT, j.optBoolean("compact", false));
        e.putString(KEY_MODE, j.optString("mode", "mixed"));
        e.putString(KEY_NOTIFICATION_STYLE, j.optString("style", "normal"));
        e.putString(KEY_FONT, j.optString("font", "clean"));
        e.putBoolean(KEY_PACK_REPEATS, j.optBoolean("repeats", true));
        e.putBoolean(KEY_PACK_ASC, j.optBoolean("ascending", true));
        e.putBoolean(KEY_PACK_DESC, j.optBoolean("descending", true));
        e.putBoolean(KEY_PACK_MIRROR, j.optBoolean("mirrors", true));
        e.putString(ThemeConfig.KEY_PRESET, j.optString("themePreset", "midnight"));

        JSONObject colors = j.optJSONObject("colors");
        if (colors != null) {
            String[] colorKeys = {
                    ThemeConfig.KEY_BG, ThemeConfig.KEY_SURFACE, ThemeConfig.KEY_TEXT,
                    ThemeConfig.KEY_TIME, ThemeConfig.KEY_MUTED, ThemeConfig.KEY_CURRENT,
                    ThemeConfig.KEY_NEXT, ThemeConfig.KEY_ACCENT
            };
            for (String key : colorKeys) if (colors.has(key)) e.putString(key, colors.optString(key));
        }

        JSONArray a = j.optJSONArray("custom");
        Set<String> custom = new HashSet<>();
        if (a != null) for (int i = 0; i < a.length(); i++) custom.add(a.optString(i));
        e.putStringSet(KEY_CUSTOM_TIMES, custom);
        e.apply();
    }

    public static List<String> customTimesSorted(Context c) {
        List<String> list = new ArrayList<>(customTimes(c));
        java.util.Collections.sort(list);
        return list;
    }
}
