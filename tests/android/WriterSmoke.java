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
        while(SystemClock.uptimeMillis()<deadline) {
            runOnMainSync(()->web=findWeb(editor.getWindow().getDecorView()));
            String mode=phase.equals("readonly") ? "!app.map.isEditMode()" : "app.map.isEditMode()";
            if(web!=null && "true".equals(eval("Boolean(window.app && app.map && app.map._docLoaded && app.map._docLayer && app.map.getDocType()==='text' && "+mode+")")))return;
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
                eval("app.map.sendUnoCommand('.uno:InsertText',{Text:{type:'string',value:'CI_WRITER_LOCAL_SAVE'}}); true");
                Thread.sleep(1500);
                eval("app.map.sendUnoCommand('.uno:InsertTable?Columns=3&Rows=2'); true");
                Thread.sleep(2000);
                eval("app.map.save(false,false); true");
                long deadline=SystemClock.uptimeMillis()+90000;
                boolean saved=false;
                while(SystemClock.uptimeMillis()<deadline) {
                    // The provider may briefly expose the in-progress copy; poll until
                    // a complete DOCX appears rather than failing on a transient ZIP read.
                    try {
                        String xml=documentXML();
                        if(xml.contains("CI_WRITER_LOCAL_SAVE") && xml.contains("<w:tbl")) {saved=true;break;}
                    } catch(IOException pendingSave) {}
                    Thread.sleep(1000);
                }
                if(!saved)throw new IOException("Native edit/table/save did not persist through the content provider");
            } else if(!documentXML().contains("CI_WRITER_LOCAL_SAVE"))throw new IOException("Saved marker missing on reopen");
            Bitmap screenshot=getUiAutomation().takeScreenshot();
            if(screenshot!=null)try(OutputStream output=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"native-"+phase+".png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG,100,output); screenshot.recycle();
            }
            results.putString("status","PASS "+phase); finish(Activity.RESULT_OK,results);
        } catch(Throwable error) { results.putString("status","FAIL "+phase+": "+error); finish(Activity.RESULT_CANCELED,results); }
    }
}

