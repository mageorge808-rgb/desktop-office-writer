#!/usr/bin/env bash
# SPDX-License-Identifier: MPL-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
WORK="$ROOT/build/smoke"
mkdir -p "$WORK/classes" "$WORK/dex" "$WORK/assets" build/apk/device-test
trap 'adb logcat -d > build/apk/device-test/logcat.txt || true' EXIT
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
JAR="$ANDROID_HOME/platforms/android-35/android.jar"
unzip -p build/cloud/original.apk assets/templates/untitled.docx > "$WORK/assets/fixture.docx"
javac --release 8 -classpath "$JAR" -d "$WORK/classes" tests/android/*.java
mapfile -t CLASSES < <(find "$WORK/classes" -type f -name '*.class')
"$TOOLS/d8" --min-api 26 --lib "$JAR" --output "$WORK/dex" "${CLASSES[@]}"
"$TOOLS/aapt2" link -I "$JAR" --manifest tests/android/AndroidManifest.xml -A "$WORK/assets" -o "$WORK/test-unsigned.apk"
python3 - <<'PY'
import zipfile
with zipfile.ZipFile('build/smoke/test-unsigned.apk','a') as archive:
    archive.write('build/smoke/dex/classes.dex','classes.dex')
PY
"$TOOLS/zipalign" -f 4 "$WORK/test-unsigned.apk" "$WORK/test-aligned.apk"
"$TOOLS/apksigner" sign --ks build/signing/writer-development.jks --ks-pass pass:android --key-pass pass:android --out "$WORK/test.apk" "$WORK/test-aligned.apk"
adb shell getprop ro.product.cpu.abilist | tee build/apk/device-test/abis.txt
if ! grep -q arm64-v8a build/apk/device-test/abis.txt; then
    echo 'This emulator image cannot execute the shipping ARM64 APK.' >&2
    exit 1
fi
adb shell dumpsys webviewupdate > build/apk/device-test/webview.txt
adb shell wm size 1280x854
adb shell wm density 160
adb install --no-streaming -r build/apk/Desktop-Office-Writer-arm64.apk
adb install --no-streaming -r "$WORK/test.apk"
adb logcat -c
for phase in edit reopen readonly; do
    adb shell am force-stop com.collabora.libreoffice
    timeout 420 adb shell am instrument -w -e phase "$phase" org.desktopoffice.writer.smoke/.WriterSmoke | tee "build/apk/device-test/$phase.txt"
    grep -q "status=PASS $phase" "build/apk/device-test/$phase.txt"
    adb pull "/sdcard/Android/data/com.collabora.libreoffice/files/native-$phase.png" "build/apk/device-test/$phase.png"
done
adb logcat -d > build/apk/device-test/logcat.txt
echo 'Native Android open/edit/table/save/reopen checks passed.'

