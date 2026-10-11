import importlib.util
from pathlib import Path
import os
import subprocess
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('prebuilt', ROOT/'scripts/prebuilt.py')
prebuilt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(prebuilt)

class PrebuiltTests(unittest.TestCase):
    def test_android_uses_desktop_without_changing_native_transport(self):
        original = (ROOT/'tests/fixtures/prebuilt-global.js').read_text(encoding='utf-8')
        patched = prebuilt.desktop_js(original)
        start = patched.index('s.mode={')
        end = patched.index('}}}}class InitializerBase', start)
        mode = patched[start:end+2]+';'
        harness = '''
const assert=require('node:assert/strict');
const s={ThisIsTheAndroidApp:true,L:{Browser:{mobile:true,cypressTest:false}}};
const screen={width:480,height:320}; const y=false;
MODE
assert.equal(s.mode.getDeviceFormFactor(),'desktop');
assert.ok(!s.mode.isCODesktop());
s.ThisIsTheAndroidApp=false;
assert.equal(s.mode.getDeviceFormFactor(),'mobile');
screen.width=1200;screen.height=800;
assert.equal(s.mode.getDeviceFormFactor(),'tablet');
'''.replace('MODE',mode)
        subprocess.run([os.environ.get('NODE_BINARY','node'),'-e',harness],check=True)
        for native in ['window.ThisIsTheAndroidApp=!0', 'window.COOLMessageHandler.postMobileMessage(s)']:
            self.assertEqual(original.count(native),patched.count(native))
        self.assertIn('window.userInterfaceMode="notebookbar"',patched)

    def test_patch_fails_closed_on_binary_source_drift(self):
        original=(ROOT/'tests/fixtures/prebuilt-global.js').read_text(encoding='utf-8')
        with self.assertRaises(ValueError): prebuilt.desktop_js(original.replace('isDesktop:function(){','renamed:function(){'))
        with self.assertRaises(ValueError): prebuilt.desktop_js(original+original)

    def test_writer_manifest_only_exposes_docx_and_launcher(self):
        text='''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.collabora.libreoffice">
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
<application><activity android:name="org.libreoffice.androidlib.LOActivity" android:launchMode="singleTask"/>
<activity android:name="OldLauncher"><intent-filter><action android:name="android.intent.action.MAIN"/></intent-filter></activity>
</application></manifest>'''
        root=ET.fromstring(prebuilt.manifest_writer(text))
        self.assertFalse(root.findall('uses-permission'))
        activities=root.find('application').findall('activity')
        public=[a.get(prebuilt.ANDROID+'name') for a in activities if a.get(prebuilt.ANDROID+'exported')=='true']
        self.assertEqual(set(public),{prebuilt.EDITOR,prebuilt.LAUNCHER})
        mimes=[d.get(prebuilt.ANDROID+'mimeType') for d in root.iter('data')]
        self.assertEqual(mimes,[prebuilt.DOCX])
        self.assertEqual(root.get('package'),'com.collabora.libreoffice')

if __name__=='__main__': unittest.main()

