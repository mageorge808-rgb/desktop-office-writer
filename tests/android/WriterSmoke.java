/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer.smoke;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.WebView;
import android.graphics.Bitmap;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class WriterSmoke extends Instrumentation {
    private Activity editor;
    private WebView web;
    private String phase;
    private final Uri fixture=Uri.parse("content://org.desktopoffice.writer.smoke.fixture/fixture.docx");
    @Override public void onCreate(Bundle arguments) { phase=arguments.getString("phase","edit"); start(); }
    private WebView findWeb(View view) {
        if(view instanceof WebView) return (WebView)view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            WebView found=findWeb(((ViewGroup)view).getChildAt(i)); if(found!=null)return found;
        }
        return null;
    }
    private String eval(String script) throws Exception {
        CountDownLatch done=new CountDownLatch(1); AtomicReference<String> result=new AtomicReference<>();
        runOnMainSync(()->web.evaluateJavascript(script,value->{result.set(value);done.countDown();}));
        if(!done.await(15,TimeUnit.SECONDS))throw new IOException("WebView evaluation timed out");
        return result.get();
    }
    private void keyChord(int keyCode, boolean alt) {
        int meta=KeyEvent.META_CTRL_ON|KeyEvent.META_CTRL_LEFT_ON;
        if(alt)meta|=KeyEvent.META_ALT_ON|KeyEvent.META_ALT_LEFT_ON;
        long now=SystemClock.uptimeMillis();
        sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_CTRL_LEFT,0,meta));
        if(alt)sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ALT_LEFT,0,meta));
        sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,keyCode,0,meta));
        sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_UP,keyCode,0,meta));
        if(alt)sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_ALT_LEFT,0,KeyEvent.META_CTRL_ON));
        sendKeySync(new KeyEvent(now,now,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_CTRL_LEFT,0,0));
    }
    private void waitSaved(String marker, boolean table) throws Exception {
        long deadline=SystemClock.uptimeMillis()+90000;
        while(SystemClock.uptimeMillis()<deadline) {
            // The provider may briefly expose the in-progress copy.
            try {
                String xml=documentXML();
                if(documentText(xml).contains(marker) && (!table || xml.contains("<w:tbl")))return;
            } catch(IOException | org.xml.sax.SAXException pendingSave) {}
            Thread.sleep(1000);
        }
        throw new IOException("Native editing/save did not persist marker: "+marker);
    }
    private Document parsedDocument(String xml) throws Exception {
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
    private String documentText(String xml) throws Exception {
        NodeList text=parsedDocument(xml).getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main","t");
        StringBuilder result=new StringBuilder();
        for(int i=0;i<text.getLength();i++)result.append(text.item(i).getTextContent());
        return result.toString();
    }
    private boolean markerIsBold(String marker) throws Exception {
        Document document=parsedDocument(documentXML());
        String namespace="http://schemas.openxmlformats.org/wordprocessingml/2006/main";
        NodeList runs=document.getElementsByTagNameNS(namespace,"r");
        StringBuilder plain=new StringBuilder(), boldMask=new StringBuilder();
        for(int i=0;i<runs.getLength();i++) {
            Element run=(Element)runs.item(i);
            NodeList bold=run.getElementsByTagNameNS(namespace,"b");
            boolean enabled=false;
            if(bold.getLength()>0) {
                String value=((Element)bold.item(0)).getAttributeNS(namespace,"val");
                enabled=!value.equals("0") && !value.equals("false") && !value.equals("off");
            }
            NodeList text=run.getElementsByTagNameNS(namespace,"t");
            for(int j=0;j<text.getLength();j++) {
                String value=text.item(j).getTextContent(); plain.append(value);
                for(int k=0;k<value.length();k++)boldMask.append(enabled?'1':'0');
            }
        }
        int start=plain.indexOf(marker);
        if(start<0)return false;
        for(int i=start;i<start+marker.length();i++)if(boldMask.charAt(i)!='1')return false;
        return true;
    }
    private void capture() {
        try {
            File directory=getTargetContext().getExternalFilesDir(null);
            if(web!=null)try(OutputStream output=new FileOutputStream(new File(directory,"native-"+phase+"-state.txt"))) {
                String state=eval("JSON.stringify({app:!!window.app,map:!!(window.app&&app.map),loaded:window.app&&app.map&&app.map._docLoaded,mode:window.app&&app.map&&app.map.isEditMode(),dialogs:Array.from(document.querySelectorAll('.jsdialog-window')).map(el=>({id:el.id,visible:!!el.offsetWidth})),keys:window.__writerSmokeKeys||[],text:document.body.innerText.slice(-1600)})");
                output.write(state.getBytes("UTF-8"));
            }
            // Keep the tiny generated document alongside the screenshot so a
            // rendering regression has a reproducible fixture, not just a log.
            try(InputStream input=getTargetContext().getContentResolver().openInputStream(fixture);
                OutputStream output=new FileOutputStream(new File(directory,"native-fixture.docx"))) {
                byte[] bytes=new byte[8192]; int count;
                while((count=input.read(bytes))!=-1)output.write(bytes,0,count);
            }
            Bitmap screenshot=getUiAutomation().takeScreenshot();
            if(screenshot!=null)try(OutputStream output=new FileOutputStream(new File(directory,"native-"+phase+".png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG,100,output); screenshot.recycle();
            }
        } catch(Exception captureFailure) {}
    }
    private String documentXML() throws IOException {
        try(ZipInputStream zip=new ZipInputStream(getTargetContext().getContentResolver().openInputStream(fixture))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) if(entry.getName().equals("word/document.xml")) {
                ByteArrayOutputStream output=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int count;
                while((count=zip.read(buffer))!=-1)output.write(buffer,0,count);
                return output.toString("UTF-8");
            }
        }
        throw new IOException("Saved DOCX missing document.xml");
    }
    private void waitReady() throws Exception {
        long deadline=SystemClock.uptimeMillis()+240000;
        // Match the painter's page-layout path. Its generic haveAllTilesInView()
        // includes off-document coordinates and omits Writer's active tile mode.
        String painted="(()=>{const layout=app.activeDocument.activeLayout; if(layout.type==='ViewLayoutMultiPage') {const coords=layout.getCurrentCoordList(); return coords.length>0&&coords.every(c=>{const t=RenderManager.get(c);return !!t&&t.isReadyToDraw();});} const section=app.map._docLayer._painter._tilesSection;let needed=0,ready=true;section.forEachTileInView(Math.round(app.map.getZoom()),app.map._docLayer.getSelectedPart(),app.activeDocument.activeModes[0],section.sectionProperties.tsManager._paintContext(),(tile,coords)=>{if(RenderManager.isValidTile(coords)){needed++;if(!tile||!tile.isReadyToDraw())ready=false;}return true;});return needed>0&&ready;})()";
        while(SystemClock.uptimeMillis()<deadline) {
            runOnMainSync(()->web=findWeb(editor.getWindow().getDecorView()));
            String mode=phase.equals("readonly") ? "!app.map.isEditMode()" : "app.map.isEditMode()";
            if(web!=null && "true".equals(eval("Boolean(window.app && app.map && app.map._docLoaded && app.map._docLayer && app.map.getDocType()==='text' && "+mode+" && app.map._docLayer._painter && app.map._docLayer._painter._tilesSection && "+painted+")")))return;
            Thread.sleep(1000);
        }
        throw new IOException("Native Writer document did not become editable in four minutes");
    }
    @Override public void onStart() {
        Bundle results=new Bundle();
        try {
            Intent intent=new Intent(Intent.ACTION_EDIT).setClassName("com.collabora.libreoffice","org.desktopoffice.writer.WriterEditorActivity")
                .setDataAndType(fixture,"application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(!phase.equals("readonly"))intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("fixture",fixture));
            long opened=SystemClock.uptimeMillis();
            editor=startActivitySync(intent); waitReady();
            results.putLong("load_ms",SystemClock.uptimeMillis()-opened);
            if(!"true".equals(eval("document.documentElement.getAttribute('data-theme')==='dark' || document.body.getAttribute('data-theme')==='dark'")))
                throw new IOException("Dark theme is not active");
            if(phase.equals("edit")) {
                long fontsDeadline=SystemClock.uptimeMillis()+30000;
                boolean fonts=false;
                while(SystemClock.uptimeMillis()<fontsDeadline) {
                    if("true".equals(eval("(()=>{const names=Object.keys(app.map.getToolbarCommandValues('.uno:CharFontName')||{});return names.includes('Noto Sans')&&names.includes('DejaVu Sans');})()"))) {fonts=true;break;}
                    Thread.sleep(500);
                }
                if(!fonts)throw new IOException("The real engine font list is missing packaged Noto/DejaVu fonts");
                results.putString("fonts","Noto Sans; DejaVu Sans");
                if(!"true".equals(eval("(()=>{const font=Array.from(document.querySelectorAll('#home-font-content input, #home-font-content select, #home-font-content [role=combobox]')).some(el=>el.offsetWidth>0);const paragraph=Array.from(document.querySelectorAll('#home-paragraph-content button')).some(el=>el.offsetWidth>0);return font&&paragraph;})()")))
                    throw new IOException("Font/Paragraph controls are hidden in the full-width desktop Home ribbon");
                if(!"true".equals(eval("document.querySelectorAll('#Home-container #stylesview .ui-iconview-entry').length>0")))
                    throw new IOException("The original Styles gallery did not receive live entries");
                eval("app.map.sendUnoCommand('.uno:InsertText',{Text:{type:'string',value:'CI_WRITER_LOCAL_SAVE'}}); true");
                Thread.sleep(1500);
                eval("app.map.sendUnoCommand('.uno:InsertTable?Columns=3&Rows=2'); true");
                Thread.sleep(2000);
                eval("app.map.save(false,false); true");
                waitSaved("CI_WRITER_LOCAL_SAVE",true);
            } else if(!documentXML().contains("CI_WRITER_LOCAL_SAVE"))throw new IOException("Saved marker missing on reopen");
            if(phase.equals("keyboard")) {
                runOnMainSync(()->web.requestFocus());
                eval("window.__writerSmokeKeys=[];window.addEventListener('keydown',e=>{if(__writerSmokeKeys.length<80)__writerSmokeKeys.push({key:e.key,code:e.code,keyCode:e.keyCode,ctrl:e.ctrlKey,alt:e.altKey,altGraph:e.getModifierState('AltGraph')});},true);true");
                // Emulated devices can report only an on-screen keyboard. Accept
                // input explicitly, as a real click into the document does.
                eval("app.map.focus(true); true");
                long focusDeadline=SystemClock.uptimeMillis()+15000;
                boolean focused=false;
                while(SystemClock.uptimeMillis()<focusDeadline) {
                    if("true".equals(eval("document.activeElement===app.map._textInput._textArea"))) {focused=true;break;}
                    Thread.sleep(250);
                }
                if(!focused)throw new IOException("Native keyboard test could not focus the real document input");
                keyChord(KeyEvent.KEYCODE_MOVE_END,false);
                keyChord(KeyEvent.KEYCODE_B,false);
                KeyEvent[] typing=KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD).getEvents(" ci_keyboard ".toCharArray());
                if(typing==null)throw new IOException("Emulator has no keyboard event mapping");
                for(KeyEvent event:typing)sendKeySync(event);
                keyChord(KeyEvent.KEYCODE_B,false);
                keyChord(KeyEvent.KEYCODE_S,false);
                waitSaved("ci_keyboard",true);
                if(!markerIsBold("ci_keyboard"))throw new IOException("Ctrl+B did not persist bold keyboard text");
                // Ctrl+End can put the caret in the paragraph after the table.
                // Navigate back into the existing table before testing its dialog.
                eval("app.map.sendUnoCommand('.uno:GoToStartOfDoc'); app.map.sendUnoCommand('.uno:JumpToNextTable'); true");
                long tableDeadline=SystemClock.uptimeMillis()+15000;
                boolean inTable=false;
                while(SystemClock.uptimeMillis()<tableDeadline) {
                    if("true".equals(eval("app.map.stateChangeHandler.getItemValue('.uno:TableDialog')==='enabled'"))) {inTable=true;break;}
                    Thread.sleep(250);
                }
                if(!inTable)throw new IOException("Table Properties command is unavailable inside the table");
                eval("app.map.focus(true); true");
                keyChord(KeyEvent.KEYCODE_T,true);
                long dialogDeadline=SystemClock.uptimeMillis()+15000;
                boolean dialog=false;
                while(SystemClock.uptimeMillis()<dialogDeadline) {
                    if("true".equals(eval("Boolean(Array.from(document.querySelectorAll('.jsdialog-window:not(.modalpopup), .lokdialog_container')).find(el=>el.offsetWidth>0))"))) {dialog=true;break;}
                    Thread.sleep(500);
                }
                if(!dialog)throw new IOException("Ctrl+Alt+T did not open the native Table Properties dialog");
            }
            capture();
            results.putString("status","PASS "+phase); finish(Activity.RESULT_OK,results);
        } catch(Throwable error) { capture(); results.putString("status","FAIL "+phase+": "+error); finish(Activity.RESULT_CANCELED,results); }
    }
}

