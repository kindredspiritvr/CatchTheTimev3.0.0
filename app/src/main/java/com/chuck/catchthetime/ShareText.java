package com.chuck.catchthetime;

import android.content.Context;

import java.time.LocalDate;
import java.time.LocalTime;

public final class ShareText {
    private static final String[] NIGHT = {"🌙", "✨", "🦉", "🌌"};
    private static final String[] MORNING = {"☀️", "🌤️", "☕", "🌅"};
    private static final String[] DAY = {"🕰️", "✨", "😎", "☀️"};
    private static final String[] EVENING = {"🌆", "🌙", "✨", "💫"};

    private ShareText() {}

    public static String messageFor(Context context, LocalTime time) {
        String base = SettingsStore.phrase(context) + " " + CatchCatalog.HHMM.format(time);
        if (!SettingsStore.emojis(context)) return base;
        return base + " " + emojiFor(time);
    }

    private static String emojiFor(LocalTime time) {
        String[] pool;
        int h = time.getHour();
        if (h < 6) pool = NIGHT;
        else if (h < 11) pool = MORNING;
        else if (h < 18) pool = DAY;
        else pool = EVENING;
        int seed = LocalDate.now().getDayOfYear() + time.getHour() * 61 + time.getMinute() * 17;
        return pool[Math.floorMod(seed, pool.length)];
    }
}
