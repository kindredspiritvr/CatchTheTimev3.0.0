package com.chuck.catchthetime;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.time.LocalTime;

public final class StatsStore {
    private static final String KEY_TOTAL = "stats_total";
    private static final String KEY_TODAY_DATE = "stats_today_date";
    private static final String KEY_TODAY_COUNT = "stats_today_count";
    private static final String KEY_LAST = "stats_last";

    private StatsStore() {}

    public static void record(Context c, LocalTime time) {
        SharedPreferences p = SettingsStore.prefs(c);
        String today = LocalDate.now().toString();
        int todayCount = today.equals(p.getString(KEY_TODAY_DATE, "")) ? p.getInt(KEY_TODAY_COUNT, 0) : 0;
        p.edit()
                .putInt(KEY_TOTAL, p.getInt(KEY_TOTAL, 0) + 1)
                .putString(KEY_TODAY_DATE, today)
                .putInt(KEY_TODAY_COUNT, todayCount + 1)
                .putString(KEY_LAST, CatchCatalog.HHMM.format(time))
                .apply();
    }

    public static int total(Context c) { return SettingsStore.prefs(c).getInt(KEY_TOTAL, 0); }

    public static int today(Context c) {
        SharedPreferences p = SettingsStore.prefs(c);
        return LocalDate.now().toString().equals(p.getString(KEY_TODAY_DATE, "")) ? p.getInt(KEY_TODAY_COUNT, 0) : 0;
    }

    public static String last(Context c) { return SettingsStore.prefs(c).getString(KEY_LAST, "—"); }
}
