#!/usr/bin/env python3
# SPDX-License-Identifier: MPL-2.0
"""Collect licensed distro fonts in CI only, recording exact package versions and hashes."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

PACKAGES = ['fonts-noto-core', 'fonts-dejavu-core']

def package_fonts(decoded, output):
    target = Path(decoded) / 'assets/writer-fonts'
    target.mkdir(parents=True, exist_ok=True)
    reports = Path(output)
    license_dir = reports / 'font-licenses'
    license_dir.mkdir(parents=True, exist_ok=True)
    inventory = []
    for package in PACKAGES:
        version = subprocess.check_output(['dpkg-query', '-W', '-f=${Version}', package], text=True)
        license_file = Path('/usr/share/doc') / package / 'copyright'
        if not license_file.is_file(): raise ValueError('Missing font license: ' + package)
        shutil.copyfile(license_file, license_dir / (package + '-copyright.txt'))
        paths = subprocess.check_output(['dpkg', '-L', package], text=True).splitlines()
        for path in paths:
            source = Path(path)
            if source.suffix.lower() not in ('.ttf', '.otf') or not source.is_file(): continue
            # Validate the real font parser, not just the extension.
            family = subprocess.check_output(['fc-scan', '--format', '%{family}', path], text=True)
            if not family.strip(): raise ValueError('Invalid font: ' + path)
            destination = target / source.name
            if destination.exists(): raise ValueError('Duplicate font filename: ' + source.name)
            shutil.copyfile(source, destination)
            inventory.append(dict(file=source.name, family=family, package=package, version=version, size=source.stat().st_size,
                                  sha256=hashlib.sha256(source.read_bytes()).hexdigest()))
    if not inventory: raise ValueError('No fonts packaged')
    (reports / 'fonts.json').write_text(json.dumps(inventory, indent=2) + '\n')
    # Embed licensing and provenance in the APK as well as the downloadable archive.
    shutil.copytree(license_dir, target.parent / 'writer-font-licenses', dirs_exist_ok=True)
    shutil.copyfile(reports / 'fonts.json', target.parent / 'writer-fonts.json')
    config = Path(decoded) / 'assets/unpack/etc/fonts/fonts.conf'
    identity = hashlib.sha256((reports / 'fonts.json').read_bytes() + config.read_bytes()).hexdigest()
    index = identity + '\n' + str(config.stat().st_size) + '\t@config\n'
    index += ''.join(str(font['size']) + '\t' + font['file'] + '\n' for font in inventory)
    (target.parent / 'writer-fonts.index').write_text(index, encoding='utf-8')
    print(f'Validated and packaged {len(inventory)} fonts')

if __name__ == '__main__': package_fonts(*sys.argv[1:])

