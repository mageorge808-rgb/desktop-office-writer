import os
from pathlib import Path
import subprocess
import unittest

ROOT=Path(__file__).resolve().parents[1]

class DesktopShortcutTests(unittest.TestCase):
    def test_table_properties_is_dispatched_once_only_when_editable_and_available(self):
        script=(ROOT/'cloud/ui/writer-desktop.js').read_text()
        harness='''
const assert=require('node:assert/strict');
let keydown, enabled=true, edit=true, dialog=false, sends=[];
const window={ThisIsTheAndroidApp:true,addEventListener:(type,cb)=>{if(type==='keydown')keydown=cb;},
app:{map:{isEditMode:()=>edit,stateChangeHandler:{getItemValue:cmd=>{assert.equal(cmd,'.uno:TableDialog');return enabled?'enabled':'disabled';}},sendUnoCommand:cmd=>sends.push(cmd)}}};
const document={querySelector:()=>dialog};
SCRIPT
const event=()=>({ctrlKey:true,altKey:true,shiftKey:false,code:'KeyT',repeat:false,isComposing:false,preventDefault(){this.prevented=true;},stopPropagation(){this.stopped=true;}});
let good=event();keydown(good);assert.deepEqual(sends,['.uno:TableDialog']);assert.ok(good.prevented&&good.stopped);
let android=event();android.code='';android.key='t';keydown(android);assert.ok(android.prevented);assert.equal(sends.length,2);
for(const property of ['repeat','isComposing','shiftKey']){let e=event();e[property]=true;keydown(e);assert.ok(!e.prevented);}
enabled=false;keydown(event());enabled=true;edit=false;keydown(event());edit=true;dialog=true;keydown(event());dialog=false;
let altgr=event();altgr.getModifierState=key=>key==='AltGraph';keydown(altgr);assert.ok(!altgr.prevented);
window.ThisIsTheAndroidApp=false;keydown(event());
assert.equal(sends.length,2);
'''.replace('SCRIPT',script)
        subprocess.run([os.environ.get('NODE_BINARY','node'),'-e',harness],check=True)

