import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]
def module(name, relative):
    spec = importlib.util.spec_from_file_location(name, ROOT / relative)
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result

overlay = module('overlay', 'scripts/prepare.py')
apk_check = module('apk_check', 'scripts/verify-apk.py')

class OverlayTests(unittest.TestCase):
    def test_upstream_form_factor_runtime(self):
        original = (ROOT / 'tests/fixtures/upstream-global.js').read_text()
        patched = overlay.desktop_global(original)
        # Evaluate the actual upstream mode object, including clipboard/platform methods.
        mode = patched[patched.index('global.mode = {'):]
        mode = mode[:mode.index('\n\t\t};') + len('\n\t\t};')]
        harness = '''
const assert = require('node:assert/strict');
const global = {L:{Browser:{mobile:true,cypressTest:false}},ThisIsTheAndroidApp:true};
const screen = {width:480,height:320}; const chromebook = false;
MODE
assert.equal(global.mode.getDeviceFormFactor(), 'desktop');
assert.equal(global.mode.isCODesktop(), undefined);
assert.ok(!global.mode.hasEngineClipboardProvider());
global.ThisIsTheAndroidApp = false;
assert.equal(global.mode.getDeviceFormFactor(), 'mobile');
screen.width=1200; screen.height=800;
assert.equal(global.mode.getDeviceFormFactor(), 'tablet');
global.L.Browser.mobile=false;
assert.equal(global.mode.getDeviceFormFactor(), 'desktop');
'''.replace('MODE', mode)
        subprocess.run([os.environ.get('NODE_BINARY', 'node'), '-e', harness], check=True)

    def test_source_drift_is_rejected(self):
        with self.assertRaises(ValueError):
            overlay.desktop_global('not the upstream source')
        original = (ROOT / 'tests/fixtures/upstream-global.js').read_text()
        with self.assertRaises(ValueError):
            overlay.desktop_global(original + original)

    def test_native_save_and_clipboard_paths_preserved(self):
        original = (ROOT / 'tests/fixtures/upstream-LOActivity.java').read_text()
        patched = overlay.desktop_activity(original)
        self.assertIn('String uiMode = "notebookbar";', patched)
        for text in ['case "SAVE":', 'copyTempBackToIntent();', 'return performPaste();',
                     'mWebView.addJavascriptInterface(this, "COOLMessageHandler");',
                     'if (isDocEditable)', 'createCOOLWSD(dataDir']:
            self.assertEqual(original.count(text), patched.count(text))

    def test_rejects_shell_only_apk(self):
        with tempfile.TemporaryDirectory(dir=ROOT) as directory:
            path = Path(directory) / 'mock.apk'
            with zipfile.ZipFile(path, 'w') as archive:
                archive.writestr('AndroidManifest.xml', 'empty')
            with self.assertRaises(ValueError):
                apk_check.verify(path)

    def test_rejects_wrong_elf_architecture(self):
        with tempfile.TemporaryDirectory(dir=ROOT) as directory:
            path = Path(directory) / 'bad.apk'
            with zipfile.ZipFile(path, 'w') as archive:
                for name in ['assets/dist/cool.html', 'assets/license.html', 'assets/notice.txt',
                             'assets/templates/untitled.docx']:
                    archive.writestr(name, 'test')
                archive.writestr('lib/arm64-v8a/liblo-native-code.so', b'not ELF')
            with self.assertRaises(ValueError):
                apk_check.verify(path)

if __name__ == '__main__':
    unittest.main()

