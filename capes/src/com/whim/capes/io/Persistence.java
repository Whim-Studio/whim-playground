package com.whim.capes.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.whim.capes.model.GameState;

/**
 * Save/load of the entire {@link GameState} to disk (Phase 5). Uses Java object
 * serialization — zero external dependencies, per the build constraints. The
 * whole model graph is {@code Serializable}; the {@link com.whim.capes.model.EventLog}
 * listeners are transient and re-registered by the UI after a load.
 */
public final class Persistence {
    public static final String EXTENSION = ".capes";

    private Persistence() {}

    public static void save(GameState state, File file) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            out.writeObject(state);
        } finally {
            out.close();
        }
    }

    public static GameState load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
        try {
            return (GameState) in.readObject();
        } finally {
            in.close();
        }
    }
}
