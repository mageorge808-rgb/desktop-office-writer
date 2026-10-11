#!/usr/bin/env python3
"""Apply a guarded overlay to an unmodified, pinned Collabora checkout."""
import argparse
import json
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
PRODUCTS = {
    'writer': ('Desktop Office Writer', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'),
    'calc': ('Desktop Office Calc', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'),
    'impress': ('Desktop Office Impress', 'application/vnd.openxmlformats-officedocument.presentationml.presentation'),
}

def replace_once(text, before, after):
    if text.count(before) != 1:
        raise ValueError(f'Upstream source drift: expected one occurrence of {before!r}')
    return text.replace(before, after, 1)

def desktop_global(text):
    # Change UI form factor only. Keep ThisIsTheAndroidApp/native socket/clipboard paths.
    for method, value in [('isSmallScreenDevice', 'false'), ('isTablet', 'false'), ('isDesktop', 'true')]:
        start = f'{method}: function() {{'
        text = replace_once(text, start, start + f'\n\t\t\t\tif (global.ThisIsTheAndroidApp) return {value}; // Desktop Office')
    return text

def desktop_activity(text):
    text = replace_once(text,
        'String uiMode = (isLargeScreen() && !isChromeOS()) ? "notebookbar" : "classic";',
        'String uiMode = "notebookbar"; // Desktop Office: every screen size')
    text = replace_once(text,
        'if (isLargeScreen() && !isChromeOS())\n            finalUrlToLoad += "&userinterfacemode=notebookbar";',
        'finalUrlToLoad += "&userinterfacemode=notebookbar";')
    return text

def prepare(source, product):
    changes = []
    for relative, transform in [
        ('browser/js/global.js', desktop_global),
        ('android/lib/src/main/java/org/libreoffice/androidlib/LOActivity.java', desktop_activity),
    ]:
        path = source / relative
        changes.append((path, transform(path.read_text())))
    library = source / 'android/lib/build.gradle'
    changes.append((library, replace_once(library.read_text(),
        'minSdk 26', "minSdk 26\n        ndk { abiFilters 'arm64-v8a' } // Desktop Office")))
    # Validate every patch before modifying any upstream source file.
    for path, contents in changes:
        path.write_text(contents, encoding='utf-8')
    shutil.copytree(ROOT / 'android/common', source / 'android/common', dirs_exist_ok=True)
    for name in PRODUCTS:
        shutil.copytree(ROOT / f'android/{name}', source / f'android/{name}', dirs_exist_ok=True)
    (source / 'android/settings.gradle').write_text(
        f"include ':lib', ':common', ':{product}'\n", encoding='utf-8')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source', type=Path)
    parser.add_argument('--product', choices=PRODUCTS, default='writer')
    args = parser.parse_args()
    prepare(args.source.resolve(), args.product)

if __name__ == '__main__':
    main()

