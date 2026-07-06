package com.whim.starcommand.engine;

import com.whim.starcommand.model.GameState;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Save/load via Java serialization of the whole {@link GameState} tree. Chosen
 * over JSON to keep the vertical slice a zero-dependency JDK build; the model is
 * a plain-POJO graph so a JSON backend can be swapped in later.
 */
public class SaveManager {

    public static File defaultSaveFile() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".starcommand");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "save.dat");
    }

    public static void save(GameState gs, File file) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        try {
            out.writeObject(gs);
        } finally {
            out.close();
        }
    }

    public static GameState load(File file) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        try {
            return (GameState) in.readObject();
        } finally {
            in.close();
        }
    }

    public static boolean saveExists() {
        return defaultSaveFile().exists();
    }
}
