#!/usr/bin/env python3
"""Reject shell-only APKs or missing engine/UI/license payloads."""
from pathlib import Path
import sys
import zipfile

def verify(path):
    with zipfile.ZipFile(path) as apk:
        names = apk.namelist()
        native = [name for name in names if name.startswith('lib/') and name.endswith('.so')]
        if not native or any(name.split('/')[1] != 'arm64-v8a' for name in native):
            raise ValueError('APK must contain ARM64 native libraries exclusively')
        for required in ['lib/arm64-v8a/liblo-native-code.so', 'assets/dist/cool.html',
                         'assets/license.html', 'assets/notice.txt',
                         'assets/templates/untitled.docx']:
            if required not in names:
                raise ValueError(f'Missing actual editor payload: {required}')
        for name in native:
            with apk.open(name) as library:
                header = library.read(20)
            if header[:5] != b'\x7fELF\x02' or int.from_bytes(header[18:20], 'little') != 183:
                raise ValueError(f'Not AArch64 ELF: {name}')

if __name__ == '__main__':
    paths = list(Path(sys.argv[1]).glob('*.apk'))
    if not paths:
        raise SystemExit('No APK produced')
    for path in paths:
        verify(path)
        print(f'ARM64 engine/UI/license payload verified: {path.name}')

