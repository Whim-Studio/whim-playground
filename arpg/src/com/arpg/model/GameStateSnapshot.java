package com.arpg.model;

/**
 * A read-only view of the world at a moment in time, handed to the UI so it can
 * render without touching engine internals. Binding cross-layer contract.
 */
public interface GameStateSnapshot {

    Character getPlayer();

    Realm getCurrentRealm();

    java.util.List<Enemy> getEnemies();

    java.util.List<String> getRecentLog();

    boolean isInCombat();

    Pet getActivePet();
}
