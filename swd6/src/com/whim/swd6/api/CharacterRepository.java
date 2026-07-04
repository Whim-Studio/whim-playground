package com.whim.swd6.api;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Save/load characters to local files. Implemented by the persistence layer
 * (Task 1) using hand-rolled JSON (JDK-only, no external libraries), consumed by
 * the UI (Task 3).
 *
 * Owned by the orchestrator (api).
 */
public interface CharacterRepository {

    /** Serialize a character to the given file (JSON). */
    void save(PlayerCharacter pc, File file) throws IOException;

    /** Deserialize a character from the given file. */
    PlayerCharacter load(File file) throws IOException;

    /** The directory characters are saved to by default (created if missing). */
    File defaultDirectory();

    /** List saved character files in the default directory. */
    List<File> listSaved();
}
