package com.chuck.catchthetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RescheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (AlarmScheduler.isEnabled(context)) AlarmScheduler.scheduleNext(context);
        CatchWidgetUpdater.updateAll(context);
    }
}
