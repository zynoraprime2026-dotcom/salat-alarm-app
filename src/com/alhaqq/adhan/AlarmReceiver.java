package com.alhaqq.adhan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String prayer = intent.getStringExtra("prayer");
        Intent svc = new Intent(ctx, AdhanService.class);
        svc.putExtra("prayer", prayer == null ? "prayer" : prayer);
        try {
            AdhanService.start(ctx, svc);
        } catch (Exception ignored) { }
        // keep the chain alive for future days even if the app is never opened again
        try { PrayerScheduler.reschedule(ctx); } catch (Exception ignored) { }
    }
}
