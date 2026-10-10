package com.chuck.catchthetime;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.time.LocalTime;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_COPY = "com.chuck.catchthetime.COPY";

    @Override public void onReceive(Context context, Intent intent) {
        if (!ACTION_COPY.equals(intent.getAction())) return;
        LocalTime time;
        try { time = LocalTime.parse(intent.getStringExtra(AlarmScheduler.EXTRA_TIME), CatchCatalog.HHMM); }
        catch (Exception e) { time = LocalTime.now().withSecond(0).withNano(0); }
        String message = ShareText.messageFor(context, time);
        ClipboardManager cb = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("Catch the time", message));
        Toast.makeText(context, "Catch copied ✓", Toast.LENGTH_SHORT).show();
    }
}
