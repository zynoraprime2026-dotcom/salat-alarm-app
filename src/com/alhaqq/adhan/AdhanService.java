package com.alhaqq.adhan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

public class AdhanService extends Service {
    static final String CHANNEL = "adhan";
    static final String[] NAMES = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
    private MediaPlayer player;

    static void start(Context ctx, Intent svc) {
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
        else ctx.startService(svc);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String key = intent != null ? intent.getStringExtra("prayer") : "prayer";
        String name = pretty(key);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Salat Alarm", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Adhan plays at each prayer time");
            nm.createNotificationChannel(ch);
        }
        PendingIntent stopPi = PendingIntent.getBroadcast(this, 9,
            new Intent(this, StopReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder nb;
        if (Build.VERSION.SDK_INT >= 26) nb = new Notification.Builder(this, CHANNEL);
        else nb = new Notification.Builder(this);
        Notification n = nb
            .setContentTitle("Salat Alarm — " + name)
            .setContentText("It is time for " + name + ". Tap to stop.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .addAction(new Notification.Action.Builder(null, "Stop adhan", stopPi).build())
            .setOngoing(true)
            .build();
        startForeground(1, n);

        if (player != null) { player.release(); player = null; }
        try {
            player = MediaPlayer.create(this, R.raw.adhan);
            if (player == null) { stopSelf(); return START_NOT_STICKY; }
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnCompletionListener(mp -> stopSelf());
            player.start();
        } catch (Exception e) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    static String pretty(String key) {
        for (String s : NAMES) if (key != null && key.equalsIgnoreCase(s)) return s;
        return "Prayer";
    }

    @Override
    public void onDestroy() {
        if (player != null) { try { player.stop(); } catch (Exception ignored) {} player.release(); player = null; }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
