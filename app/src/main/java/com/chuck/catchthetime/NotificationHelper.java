package com.chuck.catchthetime;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.time.LocalTime;

public final class NotificationHelper {
    private NotificationHelper() {}

    public static void show(Context context, LocalTime time, boolean isTest) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        String style = SettingsStore.notificationStyle(context);
        String channelId = "catch_times_" + style;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannel(context, nm, channelId, style);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 10, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String formatted = CatchCatalog.HHMM.format(time);

        Intent copyIntent = new Intent(context, NotificationActionReceiver.class)
                .setAction(NotificationActionReceiver.ACTION_COPY)
                .putExtra(AlarmScheduler.EXTRA_TIME, formatted);
        PendingIntent copyPi = PendingIntent.getBroadcast(context, 11, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent shareIntent = new Intent(context, ShareActivity.class)
                .putExtra(AlarmScheduler.EXTRA_TIME, formatted);
        PendingIntent sharePi = PendingIntent.getActivity(context, 12, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = isTest ? SettingsStore.phrase(context) + " · test" : SettingsStore.phrase(context);
        String body = ShareText.messageFor(context, time);

        Notification.Builder b = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ThemeConfig.load(context).accent)
                .setContentTitle(title)
                .setContentText(body)
                .setTicker(body)
                .setAutoCancel(true)
                .setContentIntent(openPi)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setShowWhen(true)
                .addAction(new Notification.Action.Builder(R.drawable.ic_copy, "Copy", copyPi).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_share, "Share", sharePi).build());

        if ("celebrate".equals(style)) b.setPriority(Notification.PRIORITY_HIGH);
        else if ("quiet".equals(style)) b.setPriority(Notification.PRIORITY_LOW);

        nm.notify((int) (System.currentTimeMillis() & 0x7fffffff), b.build());
    }

    public static void ensureAllChannels(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        ensureChannel(context, nm, "catch_times_normal", "normal");
        ensureChannel(context, nm, "catch_times_quiet", "quiet");
        ensureChannel(context, nm, "catch_times_celebrate", "celebrate");
    }

    private static void ensureChannel(Context context, NotificationManager nm, String id, String style) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        int importance = "quiet".equals(style) ? NotificationManager.IMPORTANCE_LOW
                : "celebrate".equals(style) ? NotificationManager.IMPORTANCE_HIGH
                : NotificationManager.IMPORTANCE_DEFAULT;
        String name = "Catch the time" + ("normal".equals(style) ? "" : " · " + style);
        NotificationChannel ch = new NotificationChannel(id, name, importance);
        ch.setDescription("Catch-time notifications.");
        if ("quiet".equals(style)) {
            ch.enableVibration(false);
            ch.setSound(null, null);
        } else if ("celebrate".equals(style)) {
            ch.enableVibration(true);
            ch.setVibrationPattern(new long[]{0, 90, 70, 120});
        }
        nm.createNotificationChannel(ch);
    }
}
