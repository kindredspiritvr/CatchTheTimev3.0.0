package com.chuck.catchthetime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class CatchWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        CatchWidgetUpdater.update(context, appWidgetManager, appWidgetIds);
    }
}
