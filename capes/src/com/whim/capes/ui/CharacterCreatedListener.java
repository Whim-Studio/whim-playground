package com.whim.capes.ui;

import com.whim.capes.model.Character;

/** Notified when the creation view successfully builds and registers a Character. */
public interface CharacterCreatedListener {
    void onCharacterCreated(Character character);
}
