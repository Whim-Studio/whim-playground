package com.arpg.engine;

import com.arpg.model.Character;
import com.arpg.model.Enemy;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.Pet;
import com.arpg.model.Realm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Engine-owned, read-only implementation of {@link GameStateSnapshot}. Lists are captured
 * as unmodifiable copies at construction so a handed-out snapshot never mutates under the UI.
 */
final class EngineSnapshot implements GameStateSnapshot {
    private final Character player;
    private final Realm currentRealm;
    private final List<Enemy> enemies;
    private final List<String> recentLog;
    private final boolean inCombat;
    private final Pet activePet;

    EngineSnapshot(Character player, Realm currentRealm, List<Enemy> enemies,
                   List<String> recentLog, boolean inCombat, Pet activePet) {
        this.player = player;
        this.currentRealm = currentRealm;
        this.enemies = Collections.unmodifiableList(new ArrayList<Enemy>(enemies));
        this.recentLog = Collections.unmodifiableList(new ArrayList<String>(recentLog));
        this.inCombat = inCombat;
        this.activePet = activePet;
    }

    public Character getPlayer() { return player; }
    public Realm getCurrentRealm() { return currentRealm; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<String> getRecentLog() { return recentLog; }
    public boolean isInCombat() { return inCombat; }
    public Pet getActivePet() { return activePet; }
}
