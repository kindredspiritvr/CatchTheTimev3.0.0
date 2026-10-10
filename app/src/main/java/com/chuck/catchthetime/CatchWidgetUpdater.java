package com.chuck.catchthetime;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.time.LocalDateTime;

public final class CatchWidgetUpdater {
    private CatchWidgetUpdater() {}

    public static void updateAll(Context c) {
        AppWidgetManager manager = AppWidgetManager.getInstance(c);
        int[] ids = manager.getAppWidgetIds(new ComponentName(c, CatchWidgetProvider.class));
        update(c, manager, ids);
    }

    public static void update(Context c, AppWidgetManager manager, int[] ids) {
        if (ids == null || ids.length == 0) return;
        ThemeConfig theme = ThemeConfig.load(c);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = CatchCatalog.nextAfter(c, now);
        int current = CatchCatalog.currentIndex(c, now);
        String label;
        String time;
        int timeColor;
        if (current >= 0) {
            label = "CATCH IT";
            time = CatchCatalog.HHMM.format(CatchCatalog.activeTimes(c).get(current));
            timeColor = theme.current;
        } else if (next == null) {
            label = "NO ACTIVE TIMES";
            time = "--:--";
            timeColor = theme.muted;
        } else {
            label = next.toLocalDate().equals(now.toLocalDate()) ? "NEXT CATCH" : "NEXT · TOMORROW";
            time = CatchCatalog.HHMM.format(next);
            timeColor = theme.next;
        }

        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 90, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_catch_time);
            rv.setInt(R.id.widget_root, "setBackgroundColor", theme.surface);
            rv.setTextColor(R.id.widget_label, theme.muted);
            rv.setTextColor(R.id.widget_time, timeColor);
            rv.setTextColor(R.id.widget_phrase, theme.text);
            rv.setTextViewText(R.id.widget_label, label);
            rv.setTextViewText(R.id.widget_time, time);
            rv.setTextViewText(R.id.widget_phrase, SettingsStore.phrase(c));
            rv.setOnClickPendingIntent(R.id.widget_root, pi);
            manager.updateAppWidget(id, rv);
        }
    }
}
