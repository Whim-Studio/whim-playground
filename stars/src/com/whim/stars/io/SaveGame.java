package com.whim.stars.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.whim.stars.model.Galaxy;

/**
 * Save and load the entire game to a single file.
 *
 * <p><b>Format choice (justified):</b> built-in Java {@code Serializable} — the
 * whole model is one {@link Galaxy} object graph, so a single {@code writeObject}
 * captures it and the serialization reference graph preserves shared-object
 * identity automatically (critical: a {@code ShipDesign} referenced by both a
 * player's design list and its fleets must remain the <i>same</i> instance after
 * reload). This keeps the app dependency-free.
 *
 * <p>The stream is GZIP-compressed and prefixed with a magic number and a format
 * version so a wrong or corrupt file fails cleanly with a clear message instead
 * of an opaque {@link ClassNotFoundException} deep in the stream.
 */
public final class SaveGame {

    /** ASCII "STAR" as a big-endian int — the file's magic prefix. */
    private static final int MAGIC = 0x53544152;
    private static final int FORMAT_VERSION = 1;
    public static final String EXTENSION = ".starsave";

    private SaveGame() {
    }

    public static void save(Galaxy galaxy, File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));
            dos.writeInt(MAGIC);
            dos.writeInt(FORMAT_VERSION);
            dos.flush();
            GZIPOutputStream gz = new GZIPOutputStream(dos);
            ObjectOutputStream oos = new ObjectOutputStream(gz);
            oos.writeObject(galaxy);
            oos.flush();
            gz.finish();
            dos.flush();
        } finally {
            fos.close();
        }
    }

    public static Galaxy load(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        try {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            int magic = dis.readInt();
            if (magic != MAGIC) {
                throw new IOException("Not a Stars! save file (bad magic number)");
            }
            int version = dis.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported save format version " + version
                        + " (this build reads version " + FORMAT_VERSION + ")");
            }
            GZIPInputStream gz = new GZIPInputStream(dis);
            ObjectInputStream ois = new ObjectInputStream(gz);
            Object obj = ois.readObject();
            if (!(obj instanceof Galaxy)) {
                throw new IOException("Save file did not contain a galaxy");
            }
            return (Galaxy) obj;
        } catch (ClassNotFoundException e) {
            throw new IOException("Save file references unknown classes: " + e.getMessage(), e);
        } finally {
            fis.close();
        }
    }

    /** Ensure a chosen file has the {@code .starsave} extension. */
    public static File withExtension(File file) {
        if (file.getName().toLowerCase().endsWith(EXTENSION)) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + EXTENSION);
    }
}
