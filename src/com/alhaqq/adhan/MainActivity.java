package com.alhaqq.adhan;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {
    static final Map<String, String[]> CITIES = new LinkedHashMap<String, String[]>() {{
        put("Tarkwa, Ghana", new String[]{"5.30", "-2.00"});
        put("Accra, Ghana", new String[]{"5.60", "-0.19"});
        put("Kumasi, Ghana", new String[]{"6.69", "-1.62"});
        put("Tamale, Ghana", new String[]{"9.40", "-0.84"});
        put("Makkah", new String[]{"21.42", "39.83"});
        put("Cairo", new String[]{"30.04", "31.24"});
        put("London", new String[]{"51.51", "-0.13"});
    }};

    TextView status, times;
    Prefs prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("🕌 Salat Alarm");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(8), 0, dp(12));
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        times = new TextView(this);
        times.setTextSize(16);
        times.setPadding(0, dp(8), 0, dp(12));
        root.addView(times);

        Button gps = new Button(this);
        gps.setText("📍 Use GPS location");
        gps.setOnClickListener(v -> useGps());
        root.addView(gps);

        LinearLayout cityRow = new LinearLayout(this);
        cityRow.setOrientation(LinearLayout.HORIZONTAL);
        Button[] cityBtns = new Button[2];
        for (int i = 0; i < 2; i++) {
            cityBtns[i] = new Button(this);
            cityRow.addView(cityBtns[i], new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        cityBtns[0].setText("Tarkwa");
        cityBtns[1].setText("Accra");
        cityBtns[0].setOnClickListener(v -> setCity("Tarkwa, Ghana"));
        cityBtns[1].setOnClickListener(v -> setCity("Accra, Ghana"));
        root.addView(cityRow);

        CheckBox master = new CheckBox(this);
        master.setText("Salat alarm ON");
        master.setTextSize(18);
        master.setChecked(prefs.masterOn());
        master.setOnCheckedChangeListener((CompoundButton btn, boolean on) -> {
            prefs.setMaster(on);
            rescheduleAndRefresh();
        });
        master.setPadding(0, dp(10), 0, dp(6));
        root.addView(master);

        for (final String k : PrayerScheduler.ALARMED) {
            CheckBox cb = new CheckBox(this);
            cb.setText("  " + AdhanService.pretty(k));
            cb.setTextSize(17);
            cb.setChecked(prefs.enabled(k));
            cb.setOnCheckedChangeListener((CompoundButton btn, boolean on) -> {
                prefs.setEnabled(k, on);
                rescheduleAndRefresh();
            });
            root.addView(cb);
        }

        Button test = new Button(this);
        test.setText("🔊 Test adhan now");
        test.setOnClickListener(v -> {
            Intent svc = new Intent(this, AdhanService.class);
            svc.putExtra("prayer", "Test");
            AdhanService.start(this, svc);
            Toast.makeText(this, "Playing adhan — check notification to stop", Toast.LENGTH_LONG).show();
        });
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(12);
        root.addView(test, tp);

        Button perm = new Button(this);
        perm.setText("⚙️ Allow exact alarms & notifications");
        perm.setOnClickListener(v -> openPermissionSettings());
        root.addView(perm);

        ScrollView sc = new ScrollView(this);
        sc.addView(root);
        setContentView(sc);

        askNotificationPermission();
    }

    void setCity(String city) {
        String[] v = CITIES.get(city);
        prefs.setLocation(city, Double.parseDouble(v[0]), Double.parseDouble(v[1]));
        rescheduleAndRefresh();
    }

    void useGps() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return;
        }
        Location last = null;
        for (String p : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try { Location l = lm.getLastKnownLocation(p); if (l != null && (last == null || l.getTime() > last.getTime())) last = l; } catch (SecurityException ignored) {}
        }
        if (last != null) {
            prefs.setLocation("My location", (double) Math.round(last.getLatitude() * 100) / 100, (double) Math.round(last.getLongitude() * 100) / 100);
            rescheduleAndRefresh();
        } else {
            Toast.makeText(this, "No last known location — open a map app once, or pick a city", Toast.LENGTH_LONG).show();
        }
    }

    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
    }

    void openPermissionSettings() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)); } catch (Exception e) { }
        } else {
            Toast.makeText(this, "Exact alarms already allowed", Toast.LENGTH_SHORT).show();
        }
        askNotificationPermission();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == 2 && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) useGps();
    }

    void rescheduleAndRefresh() {
        PrayerScheduler.reschedule(this);
        refresh();
    }

    void refresh() {
        HashMap<String, Calendar> t = PrayerScheduler.timesFor(this, Calendar.getInstance());
        SimpleDateFormat tf = new SimpleDateFormat("h:mm a");
        SimpleDateFormat df = new SimpleDateFormat("EEEE, d MMMM");
        StringBuilder sb = new StringBuilder();
        sb.append(prefs.city()).append("\n").append(df.format(Calendar.getInstance().getTime())).append("\n\n");
        for (String k : PrayerScheduler.PRAYERS) {
            sb.append(k.equalsIgnoreCase("sunrise") ? "   Sunrise   " : AdhanService.pretty(k)).append(":  ")
              .append(tf.format(t.get(k).getTime())).append("\n");
        }
        times.setText(sb.toString());

        boolean exactOk = true;
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            exactOk = am.canScheduleExactAlarms();
        }
        boolean notifOk = Build.VERSION.SDK_INT < 33 || checkSelfPermission("android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED;
        status.setText(prefs.masterOn()
            ? (exactOk ? "✅ Alarm active" : "⚠️ Allow exact alarms (button below)")
            : "Alarm OFF");
        if (!notifOk) status.append(" · allow notifications");
    }

    @Override
    protected void onResume() { super.onResume(); rescheduleAndRefresh(); }

    int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
}
