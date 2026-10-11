/* SPDX-License-Identifier: MPL-2.0 */
package org.libreoffice.androidlib;

/** Compile-only signature. NEVER packaged. The real class remains in the original DEX. */
public class LOActivity extends android.app.Activity {
    public native void createCOOLWSD(String dataDir, String cacheDir, String apkFile,
            android.content.res.AssetManager assets, String fileUrl, String uiMode, String userName);
}

