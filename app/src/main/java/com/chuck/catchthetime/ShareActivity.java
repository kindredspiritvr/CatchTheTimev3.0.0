package com.chuck.catchthetime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.time.LocalTime;

public class ShareActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalTime time;
        try { time = LocalTime.parse(getIntent().getStringExtra(AlarmScheduler.EXTRA_TIME), CatchCatalog.HHMM); }
        catch (Exception e) { time = LocalTime.now().withSecond(0).withNano(0); }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, ShareText.messageFor(this, time));
        startActivity(Intent.createChooser(send, "Share catch time"));
        finish();
    }
}
