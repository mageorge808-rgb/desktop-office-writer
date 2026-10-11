import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

class FontInstallerTests(unittest.TestCase):
    def test_real_installer_cache_and_recovery(self):
        javac = shutil.which('javac')
        java = shutil.which('java')
        if not javac or not java:
            if os.environ.get('CI'): self.fail('CI must provide a JDK for installer tests')
            self.skipTest('No local JDK; executed on the cloud runner')
        with tempfile.TemporaryDirectory(dir=ROOT) as temp:
            subprocess.run([javac, '--release', '8', '-d', temp,
                str(ROOT/'cloud/java/org/desktopoffice/writer/FontInstaller.java'),
                str(ROOT/'tests/FontInstallerTest.java')], check=True)
            subprocess.run([java, '-cp', temp, 'FontInstallerTest', str(Path(temp)/'data')], check=True)

