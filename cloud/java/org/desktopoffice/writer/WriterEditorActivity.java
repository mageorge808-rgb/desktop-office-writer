/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import android.content.res.AssetManager;
import java.io.File;
import java.io.IOException;
import android.os.Bundle;

/** Keep the original JNI class, but request the desktop ribbon for every window size. */
public class WriterEditorActivity extends org.libreoffice.androidlib.LOActivity {
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

