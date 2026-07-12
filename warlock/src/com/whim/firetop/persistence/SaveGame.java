package com.whim.firetop.persistence;

import com.whim.firetop.model.GameState;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;

/**
 * Saves and loads a {@link GameState} to a local file using Java serialization.
 * Fully offline; no external dependencies.
 */
public final class SaveGame {

    private SaveGame() { }

    /** Writes the game state to {@code file}. */
    public static void save(GameState state, File file) throws IOException {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(state);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /** Reads a game state previously written by {@link #save}. */
    public static GameState load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(file));
            return (GameState) in.readObject();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
