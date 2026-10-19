# Salat Alarm — native Android prayer-time alarm

Rings the adhan at every prayer time, even when the phone is locked and the app is closed.

- Offline prayer time calculation (MWL convention, PrayTimes.org algorithm) — matches ilm-api within 1–3 minutes
- Exact alarms via AlarmManager (SCHEDULE_EXACT_ALARM), survives reboot (BootReceiver) and re-arms itself after each alarm
- Bundled Makkah adhan audio, played as a foreground media service (USAGE_ALARM)
- GPS or city presets (Tarkwa, Accra, Kumasi, Tamale, Makkah, Cairo, London)
- Per-prayer toggles + master switch + test button

Build: `SDK=/path/to/android-sdk ./build.sh` (JDK 17, build-tools 34, platform 34).

NOTE: `release.keystore` signs the APK — keep it (pass: salat2026) so future updates install over this version.
