#!/bin/bash
# Build SalatAlarm.apk — requires Android SDK (build-tools 34, platform 34) and JDK 17
set -e
SDK=${SDK:-$HOME/android-sdk}
BT=$SDK/build-tools/34.0.0
rm -rf build && mkdir -p build/compiled build/gen build/classes build/dex
$BT/aapt2 compile --dir res -o build/compiled/res.zip
$BT/aapt2 link -o build/base.apk -I $SDK/platforms/android-34/android.jar \
  --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 24 --target-sdk-version 34 build/compiled/res.zip
find build/gen src -name "*.java" > build/sources.txt
javac --release 8 -classpath $SDK/platforms/android-34/android.jar -d build/classes @build/sources.txt
$BT/d8 --release --lib $SDK/platforms/android-34/android.jar --min-api 24 \
  --output build/dex $(find build/classes -name "*.class")
cp build/base.apk build/app-unsigned.apk
python3 -c "import zipfile; z=zipfile.ZipFile('build/app-unsigned.apk','a'); z.write('build/dex/classes.dex','classes.dex',compress_type=zipfile.ZIP_STORED); z.close()"
$BT/zipalign -f 4 build/app-unsigned.apk build/app-aligned.apk
$BT/apksigner sign --ks release.keystore --ks-pass pass:salat2026 --key-pass pass:salat2026 --out SalatAlarm.apk build/app-aligned.apk
$BT/apksigner verify SalatAlarm.apk && echo "BUILD OK: SalatAlarm.apk"
