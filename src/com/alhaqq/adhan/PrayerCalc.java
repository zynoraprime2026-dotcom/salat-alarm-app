package com.alhaqq.adhan;

import java.util.HashMap;

/** Astronomical prayer time calculation (PrayTimes.org algorithm, MWL convention). */
public class PrayerCalc {
    static double lat, lng;
    static double jBase;
    static final double FAJR_ANGLE = 18.0;   // Muslim World League
    static final double ISHA_ANGLE = 17.0;
    static final double SUNRISE_SUNSET_ANGLE = 0.833;

    public static HashMap<String, Double> compute(int y, int m, int d, double tz) {
        jBase = julian(y, m, d) - lng / (15.0 * 24.0);
        // iterate twice for accuracy
        double fajr = 5.0/24, sunrise = 6.0/24, dhuhr = 12.0/24, asr = 13.0/24, maghrib = 18.0/24, isha = 18.5/24;
        for (int i = 0; i < 2; i++) {
            fajr = sunAngleTime(FAJR_ANGLE, fajr, true);
            sunrise = sunAngleTime(SUNRISE_SUNSET_ANGLE, sunrise, true);
            dhuhr = midDay(dhuhr);
            asr = asrTime(1.0, asr);          // Shafi'i shadow factor
            maghrib = sunAngleTime(SUNRISE_SUNSET_ANGLE, maghrib, false);
            isha = sunAngleTime(ISHA_ANGLE, isha, false);
        }
        double off = tz - lng / 15.0;
        HashMap<String, Double> r = new HashMap<>();
        r.put("fajr", fajr + off);
        r.put("sunrise", sunrise + off);
        r.put("dhuhr", dhuhr + off + 1.0/60); // small safety margin
        r.put("asr", asr + off);
        r.put("maghrib", maghrib + off + 1.0/60);
        r.put("isha", isha + off);
        return r;
    }

    static double midDay(double t) {
        double eqt = sunPosition(jBase + t)[1];
        return fixHour(12 - eqt);
    }
    static double sunAngleTime(double angle, double t, boolean ccw) {
        double d = sunPosition(jBase + t)[0];
        double z = midDay(t);
        double inner = (-dsin(angle) - dsin(d) * dsin(lat)) / (dcos(d) * dcos(lat));
        if (inner < -1 || inner > 1) return ccw ? 0 : 24; // high-latitude guard
        double v = darccos(inner) / 15.0;
        return z + (ccw ? -v : v);
    }
    static double asrTime(double factor, double t) {
        double d = sunPosition(jBase + t)[0];
        double angle = -darccot(factor + dtan(Math.abs(lat - d)));
        return sunAngleTime(angle, t, false);
    }
    static double[] sunPosition(double jd) {
        double D = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * D);
        double q = fixAngle(280.459 + 0.98564736 * D);
        double L = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g));
        double e = 23.439 - 0.00000036 * D;
        double d = darcsin(dsin(e) * dsin(L));
        double RA = darctan2(dcos(e) * dsin(L), dcos(L)) / 15.0;
        RA = fixHour(RA);
        return new double[]{d, q / 15.0 - RA};
    }
    static double julian(int y, int m, int d) {
        if (m <= 2) { y--; m += 12; }
        double A = Math.floor(y / 100.0);
        double B = 2 - A + Math.floor(A / 4.0);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
    }
    static double dsin(double a) { return Math.sin(Math.toRadians(a)); }
    static double dcos(double a) { return Math.cos(Math.toRadians(a)); }
    static double dtan(double a) { return Math.tan(Math.toRadians(a)); }
    static double darcsin(double x) { return Math.toDegrees(Math.asin(x)); }
    static double darccos(double x) { return Math.toDegrees(Math.acos(x)); }
    static double darctan2(double y, double x) { return Math.toDegrees(Math.atan2(y, x)); }
    static double darccot(double x) { return Math.toDegrees(Math.atan(1.0 / x)); }
    static double fixAngle(double a) { return a - 360 * Math.floor(a / 360); }
    static double fixHour(double a) { return a - 24 * Math.floor(a / 24); }
}
