package com.whim.bc3k.save;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Tiny slot-based save store. State is a flat {@link Properties} map (the engine
 * owns the key schema); this class only handles file I/O under a save directory.
 * No third-party libraries — standard {@code java.util.Properties} text format.
 */
public final class SaveManager {

    private final File dir;

    public SaveManager(String dirPath) { this.dir = new File(dirPath); }

    private File slotFile(String slot) { return new File(dir, slot + ".sav"); }

    public boolean exists(String slot) { return slotFile(slot).exists(); }

    /** Persist a snapshot. Returns false on any I/O failure (never throws). */
    public boolean write(String slot, Properties state) {
        if (!dir.exists() && !dir.mkdirs()) return false;
        OutputStream os = null;
        try {
            os = new FileOutputStream(slotFile(slot));
            state.store(os, "Battlecruiser 3000AD save");
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            closeQuietly(os);
        }
    }

    /** Load a snapshot, or null if the slot is missing/unreadable. */
    public Properties read(String slot) {
        File f = slotFile(slot);
        if (!f.exists()) return null;
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(is);
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) { try { c.close(); } catch (IOException ignored) { } }
    }
}
