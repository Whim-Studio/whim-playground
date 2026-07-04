package com.whim.swd6.ui;

import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.ContentProvider;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RpgEngine;
import com.whim.swd6.api.CharacterRepository;

/**
 * The shared services + mutable session state handed to every hub panel. MainFrame
 * implements this; panels read/write the single active {@link PlayerCharacter} and
 * reach the engine / content / repository only through these interface accessors.
 *
 * Owned by Task 3 (ui).
 */
public interface AppContext {

    ContentProvider content();

    RpgEngine engine();

    CharacterRepository repository();

    /** A fresh combat tracker for a new encounter (via the injected supplier). */
    CombatTracker newTracker();

    /** The active character, or null if none has been created/loaded yet. */
    PlayerCharacter character();

    /** Set the active character and notify all panels to refresh. */
    void setCharacter(PlayerCharacter pc);

    /** Switch the hub to a named card: "Create", "Sheet", "Dice", "Combat", "Adventure". */
    void showCard(String name);
}
