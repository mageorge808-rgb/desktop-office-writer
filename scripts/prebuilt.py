#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0
"""Guarded customization of a pinned binary. Never replace its native libraries or original DEX."""
import hashlib
import json
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', ANDROID[1:-1])
DOCX = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
BASE = 'org.libreoffice.androidlib.LOActivity'
EDITOR = 'org.desktopoffice.writer.WriterEditorActivity'
LAUNCHER = 'org.desktopoffice.writer.WriterLauncherActivity'

def once(text, before, after):
    if text.count(before) != 1:
        raise ValueError(f'Binary source drift: expected one occurrence of {before!r}')
    return text.replace(before, after, 1)

def desktop_js(text):
    # Use Android's own bridge, not any desktop OS bridge. Preserve real mobile/native behavior.
    for method, value in [('isSmallScreenDevice', '!1'), ('isTablet', '!1'), ('isDesktop', '!0')]:
        anchor = f'{method}:function(){{'
        text = once(text, anchor, anchor + f'if(s.ThisIsTheAndroidApp)return {value};')
    # Only the Android initializer: other platforms and the native transport stay unchanged.
    start = text.index('class AndroidAppInitializer ')
    end = text.index('class EMSCRIPTENAppInitializer ', start)
    part = once(text[start:end], 'window.userInterfaceMode=window.coolParams.get("userinterfacemode")',
                'window.userInterfaceMode="notebookbar"')
    return text[:start] + part + text[end:]

def manifest_writer(text):
    root = ET.fromstring(text)
    if root.get('package') != 'com.collabora.libreoffice':
        raise ValueError('Unexpected package: native/font paths must retain upstream identity')
    app = root.find('application')
    if app is None: raise ValueError('Missing application')
    app.set(ANDROID + 'label', '@string/writer_app_name')
    app.set(ANDROID + 'icon', '@drawable/writer_icon')
    app.set(ANDROID + 'allowBackup', 'false')
    app.set(ANDROID + 'resizeableActivity', 'true')
    activities = app.findall('activity')
    base = [a for a in activities if a.get(ANDROID + 'name') == BASE]
    if len(base) != 1: raise ValueError('Expected the real Collabora editing activity')
    for activity in activities:
        for item in list(activity.findall('intent-filter')):
            activity.remove(item)
        activity.set(ANDROID + 'exported', 'false')
    editor = ET.SubElement(app, 'activity', dict(base[0].attrib))
    editor.set(ANDROID + 'name', EDITOR)
    editor.set(ANDROID + 'exported', 'true')
    editor.set(ANDROID + 'resizeableActivity', 'true')
    editor.set(ANDROID + 'windowSoftInputMode', 'adjustResize')
    filt = ET.SubElement(editor, 'intent-filter')
    for action in ['VIEW', 'EDIT']:
        ET.SubElement(filt, 'action', {ANDROID+'name': 'android.intent.action.'+action})
    ET.SubElement(filt, 'category', {ANDROID+'name': 'android.intent.category.DEFAULT'})
    ET.SubElement(filt, 'data', {ANDROID+'scheme': 'content', ANDROID+'mimeType': DOCX})
    launcher = ET.SubElement(app, 'activity', {ANDROID+'name': LAUNCHER, ANDROID+'exported': 'true'})
    filt = ET.SubElement(launcher, 'intent-filter')
    ET.SubElement(filt, 'action', {ANDROID+'name': 'android.intent.action.MAIN'})
    ET.SubElement(filt, 'category', {ANDROID+'name': 'android.intent.category.LAUNCHER'})
    for permission in list(root.findall('uses-permission')):
        if permission.get(ANDROID+'name') in ['android.permission.MANAGE_EXTERNAL_STORAGE',
                                            'android.permission.WRITE_EXTERNAL_STORAGE']:
            root.remove(permission)
    return ET.tostring(root, encoding='unicode')

def dex_methods(data):
    """Read method signatures from DEX tables, without decompiling any upstream code."""
    if not data.startswith(b'dex\n'): raise ValueError('Not a DEX file')
    def pair(offset): return struct.unpack_from('<II', data, offset)
    def uint(offset): return struct.unpack_from('<I', data, offset)[0]
    def string(offset):
        # Skip ULEB128 UTF-16 length; method/class descriptors are ASCII.
        while data[offset] & 128: offset += 1
        offset += 1
        return data[offset:data.index(0, offset)].decode('utf-8', errors='replace')
    count, offset = pair(56)
    strings = [string(uint(offset + i*4)) for i in range(count)]
    count, offset = pair(64)
    types = [strings[uint(offset + i*4)] for i in range(count)]
    count, offset = pair(72)
    protos = []
    for i in range(count):
        _, result, parameters = struct.unpack_from('<III', data, offset+i*12)
        args = ''
        if parameters:
            args = ''.join(types[struct.unpack_from('<H', data, parameters+4+j*2)[0]]
                           for j in range(uint(parameters)))
        protos.append('('+args+')'+types[result])
    count, offset = pair(88)
    methods = set()
    for i in range(count):
        owner, proto, name = struct.unpack_from('<HHI', data, offset+i*8)
        methods.add(types[owner]+'->'+strings[name]+protos[proto])
    return methods

def verify_original(apk):
    lock = json.loads((ROOT / 'prebuilt.lock.json').read_text())
    with open(apk, 'rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    if digest != lock['sha256']: raise ValueError('Official APK SHA-256 mismatch')
    with zipfile.ZipFile(apk) as archive:
        methods = set()
        for name in archive.namelist():
            if re.fullmatch(r'classes\d*\.dex', name): methods.update(dex_methods(archive.read(name)))
        signature = 'Lorg/libreoffice/androidlib/LOActivity;->createCOOLWSD(' + 'Ljava/lang/String;'*3 + \
                    'Landroid/content/res/AssetManager;' + 'Ljava/lang/String;'*3 + ')V'
        if signature not in methods: raise ValueError('JNI method no longer matches the compile-only stub')
        # Validate the customization anchors before the decode/build job spends resources.
        desktop_js(archive.read('assets/dist/global.js').decode('utf-8'))

def patch_decoded(directory):
    directory = Path(directory)
    js = directory / 'assets/dist/global.js'
    manifest = directory / 'AndroidManifest.xml'
    changed_js = desktop_js(js.read_text(encoding='utf-8'))
    changed_manifest = manifest_writer(manifest.read_text(encoding='utf-8'))
    js.write_text(changed_js, encoding='utf-8')
    for name in ['writer-desktop.css', 'writer-desktop.js']:
        (directory / 'assets/dist' / name).write_bytes((ROOT / 'cloud/ui' / name).read_bytes())
    with js.open('a', encoding='utf-8') as stream:
        stream.write('\n' + (ROOT / 'cloud/ui/writer-desktop.js').read_text(encoding='utf-8'))
    fonts_config = directory / 'assets/unpack/etc/fonts/fonts.conf'
    config = fonts_config.read_text(encoding='utf-8')
    config = once(config, '</fontconfig>', '<dir>/data/data/com.collabora.libreoffice/user/writer-fonts</dir>\n</fontconfig>')
    fonts_config.write_text(config, encoding='utf-8')
    manifest.write_text(changed_manifest, encoding='utf-8')
    values = directory / 'res/values/writer.xml'
    values.write_text('<resources><string name="writer_app_name">Desktop Office Writer</string></resources>', encoding='utf-8')
    icon = ROOT / 'android/common/src/main/res/drawable/office_icon.xml'
    (directory / 'res/drawable/writer_icon.xml').write_text(icon.read_text(), encoding='utf-8')

def verify_payload(original, result):
    with zipfile.ZipFile(original) as before, zipfile.ZipFile(result) as after:
        preserved = [n for n in before.namelist() if n.startswith('lib/') or
                     re.fullmatch(r'classes\d*\.dex', n) or n in ['assets/license.html', 'assets/notice.txt']]
        for name in preserved:
            if before.getinfo(name).CRC != after.getinfo(name).CRC or before.read(name) != after.read(name):
                raise ValueError(f'Original engine/DEX/license changed: {name}')
        names = after.namelist()
        if any(n.startswith('lib/') and n.split('/')[1] != 'arm64-v8a' for n in names):
            raise ValueError('Unexpected native architecture')
        methods = set()
        for name in names:
            if re.fullmatch(r'classes\d*\.dex', name): methods.update(dex_methods(after.read(name)))
        for name in names:
            if re.fullmatch(r'classes\d+\.dex', name) and name not in before.namelist():
                if 'Lorg/libreoffice/androidlib/LOActivity;' in dex_classes(after.read(name)):
                    raise ValueError('Compile-only stub accidentally packaged')
        for owner in ['WriterLauncherActivity', 'WriterEditorActivity']:
            if not any(m.startswith('Lorg/desktopoffice/writer/'+owner+';->') for m in methods):
                raise ValueError('Missing Writer integration class: '+owner)
        for path in ['assets/templates/untitled.docx', 'assets/dist/cool.html']:
            after.getinfo(path)
        inventory = json.loads(after.read('assets/writer-fonts.json'))
        if not inventory: raise ValueError('Missing additional fonts')
        index = after.read('assets/writer-fonts.index').decode('utf-8').splitlines()
        expected = hashlib.sha256(after.read('assets/writer-fonts.json') + after.read('assets/unpack/etc/fonts/fonts.conf')).hexdigest()
        if not index or index[0] != expected: raise ValueError('Invalid font cache identity')
        for font in inventory:
            payload = after.read('assets/writer-fonts/' + font['file'])
            if len(payload) != font['size'] or str(font['size']) + '\t' + font['file'] not in index:
                raise ValueError('Font size/index mismatch: ' + font['file'])
            if hashlib.sha256(payload).hexdigest() != font['sha256']:
                raise ValueError('Font checksum mismatch: ' + font['file'])
            after.getinfo('assets/writer-font-licenses/' + font['package'] + '-copyright.txt')
        config = after.read('assets/unpack/etc/fonts/fonts.conf').decode('utf-8')
        if '/data/data/com.collabora.libreoffice/user/writer-fonts' not in config:
            raise ValueError('Native font configuration does not reference added fonts')
        for name in ['writer-desktop.js', 'writer-desktop.css']:
            if after.read('assets/dist/' + name) != (ROOT / 'cloud/ui' / name).read_bytes():
                raise ValueError('Missing desktop styling: ' + name)

def dex_classes(data, with_flags=False):
    def uint(offset): return struct.unpack_from('<I', data, offset)[0]
    string_offset, type_offset = uint(60), uint(68)
    count, offset = struct.unpack_from('<II', data, 96)
    result = {}
    for i in range(count):
        type_id = uint(offset+i*32)
        string_id = uint(type_offset+type_id*4)
        position = uint(string_offset+string_id*4)
        while data[position] & 128: position += 1
        position += 1
        result[data[position:data.index(0, position)].decode('utf-8')] = uint(offset+i*32+4)
    return result if with_flags else set(result)

if __name__ == '__main__':
    import sys
    command, *args = sys.argv[1:]
    if command == 'verify-original': verify_original(*args)
    elif command == 'patch': patch_decoded(*args)
    elif command == 'verify-result': verify_payload(*args)
    else: raise SystemExit('Unknown command')

