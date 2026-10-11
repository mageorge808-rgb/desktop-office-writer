#!/usr/bin/env bash
# SPDX-License-Identifier: MPL-2.0
# Package precompiled engine plus two Java classes. Never compile LibreOffice.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"
: "${ANDROID_HOME:?Android SDK must be available on the hosted runner}"
TOOLS="$ANDROID_HOME/build-tools/35.0.0"
ANDROID_JAR="$ANDROID_HOME/platforms/android-35/android.jar"
WORK="$ROOT/build/cloud"
mkdir -p "$WORK" build/apk
python3 - <<'PY'
import hashlib,json,pathlib,urllib.request
lock=json.load(open('prebuilt.lock.json'))
for url_key, hash_key, filename in [('url','sha256','original.apk'),('apktool_url','apktool_sha256','apktool.jar')]:
    target=pathlib.Path('build/cloud')/filename
    if not target.exists(): urllib.request.urlretrieve(lock[url_key],target)
    with target.open('rb') as stream: digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if digest!=lock[hash_key]: raise SystemExit(f'Checksum mismatch: {filename}')
PY
python3 scripts/prebuilt.py verify-original "$WORK/original.apk"
java -jar "$WORK/apktool.jar" decode --no-src --force --output "$WORK/decoded" "$WORK/original.apk"
python3 scripts/prebuilt.py patch "$WORK/decoded"
mkdir -p "$WORK/stubs" "$WORK/classes" "$WORK/dex"
javac --release 8 -classpath "$ANDROID_JAR" -d "$WORK/stubs" cloud/stubs/org/libreoffice/androidlib/LOActivity.java
javac --release 8 -classpath "$ANDROID_JAR:$WORK/stubs" -d "$WORK/classes" cloud/java/org/desktopoffice/writer/*.java
# The native Activity stub is a classpath dependency, never a packaged program input.
mapfile -t CLASSES < <(find "$WORK/classes" -name '*.class' -type f)
"$TOOLS/d8" --min-api 26 --lib "$ANDROID_JAR" --classpath "$WORK/stubs" --output "$WORK/dex" "${CLASSES[@]}"
python3 - <<'PY'
import pathlib,re,shutil
root=pathlib.Path('build/cloud/decoded')
indices=[int(re.fullmatch(r'classes(\d*)\.dex',p.name)[1] or 1) for p in root.glob('classes*.dex')]
shutil.copyfile('build/cloud/dex/classes.dex',root/f'classes{max(indices)+1}.dex')
PY
java -jar "$WORK/apktool.jar" build "$WORK/decoded" --output "$WORK/unsigned.apk"
"$TOOLS/zipalign" -P 16 -f 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"
KEY="$ROOT/build/signing/writer-development.jks"
mkdir -p "$(dirname "$KEY")"
if [[ ! -f "$KEY" ]]; then
    keytool -genkeypair -keystore "$KEY" -alias writer -keyalg RSA -keysize 2048 \
        -validity 10000 -storepass android -keypass android -dname 'CN=Desktop Office Writer Development'
fi
"$TOOLS/apksigner" sign --ks "$KEY" --ks-key-alias writer --ks-pass pass:android \
    --key-pass pass:android --out build/apk/Desktop-Office-Writer-arm64.apk "$WORK/aligned.apk"
"$TOOLS/apksigner" verify --verbose --print-certs build/apk/Desktop-Office-Writer-arm64.apk > build/apk/signature.txt
"$TOOLS/zipalign" -c -P 16 -v 4 build/apk/Desktop-Office-Writer-arm64.apk > build/apk/alignment.txt
python3 scripts/prebuilt.py verify-result "$WORK/original.apk" build/apk/Desktop-Office-Writer-arm64.apk
python3 scripts/verify-apk.py build/apk
python3 - <<'PY'
import hashlib,json,pathlib,zipfile,xml.etree.ElementTree as ET
root=pathlib.Path('build/apk'); apk=root/'Desktop-Office-Writer-arm64.apk'
with apk.open('rb') as stream: digest=hashlib.file_digest(stream,'sha256').hexdigest()
(root/'SHA256SUMS').write_text(digest+'  '+apk.name+'\n')
lock=json.load(open('prebuilt.lock.json'))
strings=ET.parse('build/cloud/decoded/res/values/strings.xml').getroot()
lock['app_source_build_hash']=next((s.text for s in strings.findall('string') if s.get('name')=='version_hash'),None)
print('Original Android app source build hash:',lock['app_source_build_hash'])
with zipfile.ZipFile('build/cloud/original.apk') as original:
    for path,name in [('assets/license.html','license.html'),('assets/notice.txt','NOTICE-engine.txt')]:
        (root/name).write_bytes(original.read(path))
lock['customization']='DOCX launcher, inherited native Activity ribbon override, desktop Android UI assets'
(root/'provenance.json').write_text(json.dumps(lock,indent=2))
PY
tar -czf build/apk/writer-modified-source.tar.gz cloud scripts/prebuilt.py scripts/build-writer-cloud.sh \
    prebuilt.lock.json android/common/src/main/res/drawable/office_icon.xml LICENSE NOTICE.md README.md
echo 'Writer packaging, signature, alignment and unchanged engine/DEX checks passed.'

