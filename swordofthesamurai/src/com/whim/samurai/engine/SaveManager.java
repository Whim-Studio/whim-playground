package com.whim.samurai.engine;

import com.whim.samurai.model.GameState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Save/load via plain Java serialisation of the whole {@link GameState} tree
 * (all model classes implement {@link java.io.Serializable}). One slot on disk
 * under ./saves/ (design ref, ARCHITECTURE.md — "simple serialization is fine").
 */
public final class SaveManager {
    private SaveManager() { }

    private static final File DIR = new File("saves");
    private static final File SLOT = new File(DIR, "sword_save.ser");

    public static boolean saveExists() { return SLOT.isFile(); }

    public static void save(GameState state) throws IOException {
        if (!DIR.exists()) DIR.mkdirs();
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SLOT));
        try {
            out.writeObject(state);
        } finally {
            out.close();
        }
    }

    public static GameState load() throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(SLOT));
        try {
            return (GameState) in.readObject();
        } finally {
            in.close();
        }
    }
}
