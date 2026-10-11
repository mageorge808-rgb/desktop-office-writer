/* SPDX-License-Identifier: MPL-2.0 */
import org.desktopoffice.writer.FontInstaller;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FontInstallerTest {
    static class Source implements FontInstaller.Assets {
        Map<String, byte[]> files = new HashMap<>();
        int payloadReads;
        Source(char version, String font) {
            put("unpack/etc/fonts/fonts.conf", "<fontconfig/>");
            put("writer-fonts/Noto.ttf", font);
            put("writer-fonts.index", String.join("", Collections.nCopies(64, String.valueOf(version))) +
                "\n13\t@config\n" + font.length() + "\tNoto.ttf\n");
        }
        void put(String path, String text) { files.put(path, text.getBytes(StandardCharsets.UTF_8)); }
        public InputStream open(String path) throws IOException {
            if (!path.equals("writer-fonts.index")) payloadReads++;
            if (!files.containsKey(path)) throw new IOException("Missing asset");
            return new ByteArrayInputStream(files.get(path));
        }
    }
    static void check(boolean condition, String text) { if (!condition) throw new AssertionError(text); }
    static void rejects(File dir, Source source) throws Exception {
        try { FontInstaller.install(dir, source); throw new AssertionError("Should reject"); }
        catch (IOException expected) {}
    }
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        Source source = new Source('a', "FONT");
        check(FontInstaller.install(dir, source), "Cold install");
        check(source.payloadReads == 2, "One copy per payload");
        source.payloadReads = 0;
        check(!FontInstaller.install(dir, source), "Warm start uses cache");
        check(source.payloadReads == 0, "Warm start opens no font/config streams");
        Files.delete(new File(dir, "user/writer-fonts/Noto.ttf").toPath());
        check(FontInstaller.install(dir, source), "Missing font repaired");
        Source update = new Source('b', "NEWFONT");
        check(FontInstaller.install(dir, update), "Updated payload installed");
        File font = new File(dir, "user/writer-fonts/Noto.ttf");
        check(new String(Files.readAllBytes(font.toPath()), StandardCharsets.UTF_8).equals("NEWFONT"), "Updated contents");
        Source broken = new Source('c', "SHORT");
        broken.put("writer-fonts/Noto.ttf", "X");
        rejects(dir, broken);
        check(font.length() == 7, "Failed copy preserves prior valid font");
        check(new String(Files.readAllBytes(new File(dir,"writer-fonts.version").toPath()),StandardCharsets.UTF_8).startsWith("b"), "Failed install does not commit marker");
        Source traversal = new Source('d', "FONT");
        traversal.put("writer-fonts.index", String.join("",Collections.nCopies(64,"d"))+"\n13\t@config\n4\t../Noto.ttf\n");
        rejects(dir, traversal);
        System.out.println("Font installer: cold/warm, missing-file recovery, update, truncated-copy recovery and traversal rejection passed.");
    }
}

