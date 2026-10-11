/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import android.content.res.AssetManager;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import android.os.Bundle;

/** Keep the original JNI class, but request the desktop ribbon for every window size. */
public class WriterEditorActivity extends org.libreoffice.androidlib.LOActivity {
    @Override public void onCreate(Bundle state) {
        // Start with paper-like light mode; launcher exposes the persistent choice.
        android.content.SharedPreferences prefs = getSharedPreferences("EXPLORER_PREFS", MODE_PRIVATE);
        if (!prefs.contains("NIGHT_MODE")) prefs.edit().putInt("NIGHT_MODE", 1).commit();
        super.onCreate(state);
    }
    @Override public void createCOOLWSD(String dataDir, String cacheDir, String apkFile,
            AssetManager assets, String fileUrl, String ignoredMode, String userName) {
        // Install before fontconfig/native document initialization, including APK updates.
        try {
            // Upstream asset extraction is keyed to the engine commit, which stays unchanged
            // across our updates. Refresh this small config explicitly for existing installs.
            File config = new File(dataDir, "etc/fonts/fonts.conf");
            if (!config.getParentFile().isDirectory() && !config.getParentFile().mkdirs())
                throw new IOException("Cannot create font configuration directory");
            try (InputStream input = assets.open("unpack/etc/fonts/fonts.conf");
                 FileOutputStream output = new FileOutputStream(config)) {
                byte[] buffer = new byte[16384]; int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            File directory = new File(dataDir, "user/writer-fonts");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create font directory");
            for (String name : assets.list("writer-fonts")) {
                if (!name.matches("[A-Za-z0-9_.-]+\\.(ttf|otf)")) continue;
                try (InputStream input = assets.open("writer-fonts/" + name);
                     FileOutputStream output = new FileOutputStream(new File(directory, name))) {
                    byte[] buffer = new byte[16384]; int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot prepare Writer fonts", error);
        }
        super.createCOOLWSD(dataDir, cacheDir, apkFile, assets, fileUrl, "notebookbar", userName);
    }
}

