/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/** DOCX-only launcher; all editing, clipboard and save-back remain in the real editor. */
public class WriterLauncherActivity extends Activity {
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final int OPEN = 10, CREATE = 11;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this);
        title.setText("Desktop Office Writer\nOffline DOCX editor · mouse and keyboard");
        layout.addView(title);
        Button open = new Button(this);
        open.setText("Open DOCX…  (Ctrl+O)");
        open.setOnClickListener(view -> pick(false));
        layout.addView(open);
        Button create = new Button(this);
        create.setText("New DOCX…  (Ctrl+N)");
        create.setOnClickListener(view -> pick(true));
        layout.addView(create);
        setContentView(layout);
        open.requestFocus();
    }

    private void pick(boolean create) {
        Intent picker = new Intent(create ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE).setType(DOCX)
                .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (create) picker.putExtra(Intent.EXTRA_TITLE, "Untitled.docx");
        try { startActivityForResult(picker, create ? CREATE : OPEN); }
        catch (android.content.ActivityNotFoundException error) { show("Android document picker unavailable."); }
    }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed()) {
            if (code == KeyEvent.KEYCODE_O || code == KeyEvent.KEYCODE_N) {
                if (event.getRepeatCount() == 0) pick(code == KeyEvent.KEYCODE_N);
                return true;
            }
        }
        return super.onKeyDown(code, event);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if ((request != OPEN && request != CREATE) || result != RESULT_OK || data == null || data.getData() == null)
            return;
        Uri uri = data.getData();
        int grants = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if ((grants & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) { show("Read access was not granted."); return; }
        try { getContentResolver().takePersistableUriPermission(uri, grants); }
        catch (SecurityException ignored) { /* Session-only provider grant remains valid. */ }
        if (request == CREATE) {
            if ((grants & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == 0) { show("Write access was not granted."); return; }
            try (InputStream input = getAssets().open("templates/untitled.docx");
                 OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new IOException("Provider returned no output stream");
                byte[] buffer = new byte[16384];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            } catch (IOException | SecurityException error) { show("Could not create the document: " + error.getMessage()); return; }
        }
        Intent editor = new Intent(Intent.ACTION_EDIT).setClass(this, WriterEditorActivity.class)
                .setDataAndType(uri, DOCX).addFlags(grants);
        editor.setClipData(ClipData.newRawUri("document", uri));
        startActivity(editor);
    }

    private void show(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}

