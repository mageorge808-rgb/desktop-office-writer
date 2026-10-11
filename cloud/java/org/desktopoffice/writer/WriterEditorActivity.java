/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import android.content.res.AssetManager;

/** Keep the original JNI class, but request the desktop ribbon for every window size. */
public class WriterEditorActivity extends org.libreoffice.androidlib.LOActivity {
    @Override public void createCOOLWSD(String dataDir, String cacheDir, String apkFile,
            AssetManager assets, String fileUrl, String ignoredMode, String userName) {
        super.createCOOLWSD(dataDir, cacheDir, apkFile, assets, fileUrl, "notebookbar", userName);
    }
}

