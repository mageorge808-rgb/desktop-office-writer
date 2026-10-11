/* SPDX-License-Identifier: MPL-2.0 */
package org.desktopoffice.writer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Android-independent installer, tested with the same streams/files used on device. */
public final class FontInstaller {
    public interface Assets { InputStream open(String path) throws IOException; }
    private static final class Entry {
        final String name; final long size;
        Entry(String name, long size) { this.name = name; this.size = size; }
    }

    // Warm starts only read a small index and stat files: never recopy 272 fonts.
    public static synchronized boolean install(File data, Assets assets) throws IOException {
        String version;
        List<Entry> entries = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new InputStreamReader(
                assets.open("writer-fonts.index"), StandardCharsets.UTF_8))) {
            version = input.readLine();
            if (version == null || !version.matches("[a-f0-9]{64}")) throw new IOException("Invalid font version");
            String line;
            Set<String> names = new HashSet<>();
            while ((line = input.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                if (fields.length != 2 || (!fields[1].equals("@config") &&
                        !fields[1].matches("[A-Za-z0-9_.-]+\\.(ttf|otf)")) || !names.add(fields[1]))
                    throw new IOException("Invalid font index entry");
                long size;
                try { size = Long.parseLong(fields[0]); }
                catch (NumberFormatException error) { throw new IOException("Invalid font size", error); }
                if (size <= 0 || size > 64L * 1024 * 1024) throw new IOException("Invalid font size");
                entries.add(new Entry(fields[1], size));
            }
            if (!names.contains("@config") || entries.size() < 2) throw new IOException("Incomplete font index");
        }
        File marker = new File(data, "writer-fonts.version");
        boolean ready = marker.isFile() && marker.length() == 64 &&
                version.equals(new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8));
        for (Entry entry : entries) {
            File file = destination(data, entry.name);
            ready &= file.isFile() && file.length() == entry.size;
        }
        if (ready) return false;
        // A marker is committed only after every atomic replacement succeeds.
        for (Entry entry : entries) {
            String path = entry.name.equals("@config") ? "unpack/etc/fonts/fonts.conf" : "writer-fonts/" + entry.name;
            try (InputStream input = assets.open(path)) {
                replace(destination(data, entry.name), input, entry.size);
            }
        }
        replace(marker, new ByteArrayInputStream(version.getBytes(StandardCharsets.UTF_8)), 64);
        return true;
    }

    private static File destination(File data, String name) {
        return new File(data, name.equals("@config") ? "etc/fonts/fonts.conf" : "user/writer-fonts/" + name);
    }

    private static void replace(File target, InputStream input, long expected) throws IOException {
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
        Path temp = Files.createTempFile(parent.toPath(), ".writer-", ".tmp");
        try {
            long count = Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            if (count != expected) throw new IOException("Incomplete font/config asset: " + target.getName());
            try { Files.move(temp, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) {
                Files.move(temp, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }
    private FontInstaller() {}
}

