package com.alhaqq.adhan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.HashMap;

/** Schedules exact alarms for all remaining prayers today and all five tomorrow. */
public class PrayerScheduler {
    public static final String[] PRAYERS = {"fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"};
    public static final String[] ALARMED = {"fajr", "dhuhr", "asr", "maghrib", "isha"};

    public static HashMap<String, Calendar> timesFor(Context ctx, Calendar day) {
        Prefs p = new Prefs(ctx);
        PrayerCalc.lat = p.lat();
        PrayerCalc.lng = p.lng();
        double tz = Calendar.getInstance().getTimeZone().getRawOffset() / 3600000.0
                  + (day.getTimeZone().inDaylightTime(day.getTime()) ? 1.0 : 0.0);
        HashMap<String, Double> t = PrayerCalc.compute(
            day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH), tz);
        HashMap<String, Calendar> out = new HashMap<>();
        for (String k : PRAYERS) {
            Calendar c = (Calendar) day.clone();
            double h = t.get(k);
            c.set(Calendar.HOUR_OF_DAY, (int) Math.floor(h));
            c.set(Calendar.MINUTE, (int) Math.round((h - Math.floor(h)) * 60) % 60);
            if ((int) Math.round((h - Math.floor(h)) * 60) == 60) c.add(Calendar.HOUR_OF_DAY, 1);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            out.put(k, c);
        }
        return out;
    }

    public static void reschedule(Context ctx) {
        Prefs p = new Prefs(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        // cancel everything, then set afresh
        for (int i = 0; i < ALARMED.length; i++) {
            PendingIntent pi = pending(ctx, i, null);
            if (pi != null) am.cancel(pi);
        }
        if (!p.masterOn()) return;
        Calendar now = Calendar.getInstance();
        int scheduled = 0;
        for (int dayOffset = 0; dayOffset < 2 && scheduled < 6; dayOffset++) {
            Calendar day = (Calendar) now.clone();
            day.add(Calendar.DAY_OF_YEAR, dayOffset);
            HashMap<String, Calendar> times = timesFor(ctx, day);
            for (int i = 0; i < ALARMED.length; i++) {
                String k = ALARMED[i];
                if (!p.enabled(k)) continue;
                Calendar t = times.get(k);
                if (t == null || t.before(now) || t.after(day)) continue;
                if (t.get(Calendar.DAY_OF_YEAR) != day.get(Calendar.DAY_OF_YEAR)) continue;
                PendingIntent pi = pending(ctx, i, k);
                if (pi == null) continue;
                if (Build.VERSION.SDK_INT >= 31) {
                    try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi); }
                    catch (SecurityException e) { am.setWindow(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), 60000, pi); }
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pi);
                }
                scheduled++;
            }
        }
    }

    static PendingIntent pending(Context ctx, int idx, String prayer) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("prayer", prayer == null ? ALARMED[idx] : prayer);
        return PendingIntent.getBroadcast(ctx, 100 + idx, i,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
