package com.whim.alganon.api;

/**
 * Root mutable game state authored by Task 1 and driven by the Task 2 engine.
 * Bundles the player, the loaded world, and the content registry, plus the two
 * persistent bookkeeping fields the offline-Study calculation needs.
 */
public interface GameModel {
    Content content();
    CharacterModel player();

    WorldModel world();                       // currently loaded zone (never null once created)
    WorldModel loadZone(String zoneId);       // build + set as current, returns it

    long seed();

    /** Epoch millis of the last save (0 for a brand-new character). Used to grant offline Study. */
    long lastSaveEpochMillis();
    void setLastSaveEpochMillis(long millis);

    /** Simulated faction-war persistent scores (single-player Towers/Keeps substitute). */
    int asharrWarScore(); int kujixWarScore();
    void setWarScores(int asharr, int kujix);
}
