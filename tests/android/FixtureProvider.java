/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer.smoke;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Test-APK-only provider; exposes a single disposable fixture, never user documents. */
public class FixtureProvider extends ContentProvider {
    private File file;
    @Override public boolean onCreate() {
        file = new File(getContext().getFilesDir(), "fixture.docx");
        if (!file.exists()) try (InputStream input = getContext().getAssets().open("fixture.docx");
                OutputStream output = new FileOutputStream(file)) {
            byte[] buffer = new byte[16384]; int count;
            while ((count=input.read(buffer))!=-1) output.write(buffer,0,count);
        } catch(IOException error) { throw new IllegalStateException(error); }
        return true;
    }
    private void validate(Uri uri) {
        if (!"/fixture.docx".equals(uri.getPath())) throw new IllegalArgumentException("Unknown test file");
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        validate(uri);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
    }
    @Override public String getType(Uri uri) { validate(uri); return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        validate(uri);
        String[] columns=projection==null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE} : projection;
        MatrixCursor cursor=new MatrixCursor(columns); Object[] row=new Object[columns.length];
        for(int i=0;i<columns.length;i++) row[i]=columns[i].equals(OpenableColumns.DISPLAY_NAME) ? file.getName() : columns[i].equals(OpenableColumns.SIZE) ? file.length() : null;
        cursor.addRow(row); return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
}

