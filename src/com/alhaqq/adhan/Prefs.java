package com.alhaqq.adhan;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private final SharedPreferences sp;
    Prefs(Context c) { sp = c.getSharedPreferences("adhan", Context.MODE_PRIVATE); }

    double lat() { return Double.longBitsToDouble(sp.getLong("lat", Double.doubleToRawLongBits(5.30))); }
    double lng() { return Double.longBitsToDouble(sp.getLong("lng", Double.doubleToRawLongBits(-2.00))); }
    String city() { return sp.getString("city", "Tarkwa, Ghana"); }
    boolean masterOn() { return sp.getBoolean("master", true); }

    void setLocation(String city, double lat, double lng) {
        sp.edit().putLong("lat", Double.doubleToRawLongBits(lat))
                 .putLong("lng", Double.doubleToRawLongBits(lng))
                 .putString("city", city).apply();
    }
    void setMaster(boolean on) { sp.edit().putBoolean("master", on).apply(); }
    boolean enabled(String prayer) { return sp.getBoolean("p_" + prayer, true); }
    void setEnabled(String prayer, boolean on) { sp.edit().putBoolean("p_" + prayer, on).apply(); }
}
