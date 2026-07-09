package com.whim.alganon.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * File/slot handling for saves. Writes {@link SaveGame} snapshots as human-readable text
 * (via {@link SaveCodec}) under {@code ~/.alganon/saves}. Zero external dependencies.
 */
public final class SaveManager {

    public static final int SLOT_COUNT = 3;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final File dir;

    public SaveManager() {
        this(defaultDir());
    }

    public SaveManager(File dir) {
        this.dir = dir;
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
    }

    private static File defaultDir() {
        String home = System.getProperty("user.home", ".");
        return new File(new File(home, ".alganon"), "saves");
    }

    public File slotFile(int slot) {
        return new File(dir, "slot" + slot + ".sav");
    }

    public boolean exists(int slot) {
        return slotFile(slot).isFile();
    }

    /** One label per slot for the load/save menu, e.g. "Slot 1: Lv3 Kaelen". */
    public List<String> slotLabels() {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < SLOT_COUNT; i++) {
            out.add("Slot " + (i + 1) + ": " + describe(i));
        }
        return out;
    }

    private String describe(int slot) {
        if (!exists(slot)) return "(empty)";
        try {
            SaveGame s = read(slot);
            String name = s.name == null || s.name.isEmpty() ? "?" : s.name;
            return "Lv" + s.level + " " + name;
        } catch (IOException e) {
            return "(corrupt)";
        }
    }

    public void write(int slot, SaveGame game) throws IOException {
        String text = SaveCodec.encode(game);
        FileOutputStream fos = new FileOutputStream(slotFile(slot));
        Writer w = new OutputStreamWriter(fos, UTF8);
        try {
            w.write(text);
            w.flush();
        } finally {
            w.close();
        }
    }

    public SaveGame read(int slot) throws IOException {
        File f = slotFile(slot);
        if (!f.isFile()) throw new IOException("No save in slot " + slot);
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(f), UTF8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            r.close();
        }
        return SaveCodec.decode(sb.toString());
    }
}
