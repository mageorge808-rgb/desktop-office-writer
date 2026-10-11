/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import android.content.res.AssetManager;
import java.io.File;
import java.io.IOException;
import android.os.Bundle;
import android.content.Intent;
import android.view.KeyEvent;
import android.net.Uri;
import android.os.Build;
import java.io.InputStream;
import java.lang.reflect.Method;

/** Keep the original JNI class, but request the desktop ribbon for every window size. */
public class WriterEditorActivity extends org.libreoffice.androidlib.LOActivity {
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        Uri document = getIntent().getData();
        if (Build.VERSION.SDK_INT < 33 && permissions.length == 1 &&
                "android.permission.WRITE_EXTERNAL_STORAGE".equals(permissions[0]) &&
                document != null && "content".equals(document.getScheme()) &&
                (getIntent().getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            // SAF already authorizes this URI. The pinned base class has a redundant
            // pre-Android-13 storage gate. Resume its loader without inventing a
            // platform permission or widening access; the provider still enforces I/O.
            try (InputStream input = getContentResolver().openInputStream(document)) {
                if (input == null) throw new IOException("Document access was revoked");
                Method loader = org.libreoffice.androidlib.LOActivity.class.getDeclaredMethod("loadDocument");
                loader.setAccessible(true);
                loader.invoke(this);
            } catch (IOException | ReflectiveOperationException | SecurityException error) {
                android.widget.Toast.makeText(this, "Could not open this document: " + error.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show();
                finish();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, results);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.isCtrlPressed() && !event.isAltPressed() && !event.isShiftPressed() &&
                (event.getKeyCode() == KeyEvent.KEYCODE_O || event.getKeyCode() == KeyEvent.KEYCODE_N)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0)
                startActivity(new Intent(this, WriterLauncherActivity.class)
                        .putExtra("writer.pick", event.getKeyCode() == KeyEvent.KEYCODE_N ? "new" : "open"));
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void startActivity(Intent intent) {
        // Upstream document switching explicitly reopens its base Activity. Keep our
        // native ribbon/font integration when its save-and-restart path is used.
        if (intent.getComponent() != null &&
                intent.getComponent().getClassName().equals("org.libreoffice.androidlib.LOActivity"))
            intent = new Intent(intent).setClass(this, WriterEditorActivity.class);
        super.startActivity(intent);
    }
    @Override public void onCreate(Bundle state) {
        // Apply the requested dark default on upgrade; preserve later user choices.
        android.content.SharedPreferences prefs = getSharedPreferences("EXPLORER_PREFS", MODE_PRIVATE);
        if (!prefs.getBoolean("WRITER_DARK_DEFAULT_V2", false))
            prefs.edit().putInt("NIGHT_MODE", 2).putBoolean("WRITER_DARK_DEFAULT_V2", true).apply();
        super.onCreate(state);
    }
    @Override public void createCOOLWSD(String dataDir, String cacheDir, String apkFile,
            AssetManager assets, String fileUrl, String ignoredMode, String userName) {
        // Install before fontconfig/native document initialization, including APK updates.
        try {
            FontInstaller.install(new File(dataDir), assets::open);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot prepare Writer fonts", error);
        }
        super.createCOOLWSD(dataDir, cacheDir, apkFile, assets, fileUrl, "notebookbar", userName);
    }
}

