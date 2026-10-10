package com.chuck.catchthetime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class AlarmScheduler {
    public static final String EXTRA_TIME = "time";
    private static final int REQUEST_CODE = 4242;

    private AlarmScheduler() {}

    public static boolean isEnabled(Context c) {
        return SettingsStore.prefs(c).getBoolean(SettingsStore.KEY_ENABLED, false);
    }

    public static void setEnabled(Context c, boolean enabled) {
        SettingsStore.prefs(c).edit().putBoolean(SettingsStore.KEY_ENABLED, enabled).apply();
        if (enabled) scheduleNext(c); else cancel(c);
        CatchWidgetUpdater.updateAll(c);
    }

    public static boolean canScheduleExact(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
    }

    public static LocalDateTime scheduleNext(Context c) {
        if (!isEnabled(c)) return null;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return null;

        LocalDateTime next = CatchCatalog.nextAfter(c, LocalDateTime.now());
        if (next == null) {
            cancel(c);
            CatchWidgetUpdater.updateAll(c);
            return null;
        }

        Intent i = new Intent(c, TimeAlarmReceiver.class)
                .putExtra(EXTRA_TIME, CatchCatalog.HHMM.format(next));
        PendingIntent pi = PendingIntent.getBroadcast(c, REQUEST_CODE, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long when = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        CatchWidgetUpdater.updateAll(c);
        return next;
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getBroadcast(c, REQUEST_CODE,
                new Intent(c, TimeAlarmReceiver.class),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            am.cancel(pi);
            pi.cancel();
        }
    }
}
