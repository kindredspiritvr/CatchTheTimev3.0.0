package com.chuck.catchthetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalTime;

public class TimeAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!AlarmScheduler.isEnabled(context)) return;
        String raw = intent.getStringExtra(AlarmScheduler.EXTRA_TIME);
        LocalTime time;
        try { time = LocalTime.parse(raw, CatchCatalog.HHMM); }
        catch (Exception e) { time = LocalTime.now().withSecond(0).withNano(0); }

        StatsStore.record(context, time);
        NotificationHelper.show(context, time, false);
        CatchWidgetUpdater.updateAll(context);
        AlarmScheduler.scheduleNext(context);
    }
}
