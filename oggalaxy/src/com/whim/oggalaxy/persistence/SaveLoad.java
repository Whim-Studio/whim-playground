package com.whim.oggalaxy.persistence;

import com.whim.oggalaxy.model.GameState;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;

/**
 * Java-serialization save/load of the entire {@link GameState} graph. The {@code Catalog}
 * is intentionally NOT part of the graph — the model holds no reference to it, so on load
 * the engine simply rebuilds it via {@code Catalog.standard()}. That keeps save files small
 * and immune to catalog changes between versions.
 */
public final class SaveLoad {

    private SaveLoad() {
    }

    public static void save(File file, GameState state) throws IOException {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            out.writeObject(state);
            out.flush();
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) { }
            }
        }
    }

    public static GameState load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object obj = in.readObject();
            if (!(obj instanceof GameState)) {
                throw new IOException("Save file does not contain a GameState");
            }
            return (GameState) obj;
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) { }
            }
        }
    }
}
