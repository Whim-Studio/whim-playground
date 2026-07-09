package com.whim.alganon.api;

/**
 * Task 1's single entry point, constructed by {@code Main} and handed to the engine.
 * Must have a public no-arg constructor.
 */
public interface ModelFactory {
    /** The shared immutable content registry (safe to build once). */
    Content content();

    /**
     * Create a fresh, unplaced game model for the given seed. The engine calls
     * {@code player}-configuration methods (race/family/class/name) during character
     * creation, then {@link GameModel#loadZone} to enter the world.
     */
    GameModel newGame(long seed);

    /**
     * Apply a character-creation selection to a new model, wiring race/family/class,
     * starting stats, starting abilities, starting inventory, and starting zone/pos.
     * Kept on the factory so all "how a character is built" rules live in Task 1.
     */
    void applyCreation(GameModel model, String raceId, String familyId, String classId, String name);
}
