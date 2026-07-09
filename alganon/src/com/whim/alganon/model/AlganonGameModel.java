package com.whim.alganon.model;

import com.whim.alganon.api.Content;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.content.AlganonContent;
import com.whim.alganon.data.ZoneBlueprint;

/**
 * Root mutable game state: the player, the currently loaded world, the content
 * registry, and the two persistence bookkeeping fields (last-save epoch, war scores)
 * the offline-Study and faction-war systems read.
 */
public final class AlganonGameModel implements GameModel {

    private final AlganonContent content;
    private final long seed;
    private final AlganonCharacter player;

    private AlganonWorld world;
    private long lastSaveEpochMillis = 0L;
    private int asharrWarScore = 50, kujixWarScore = 50;

    public AlganonGameModel(AlganonContent content, long seed) {
        this.content = content;
        this.seed = seed;
        this.player = new AlganonCharacter(content);
    }

    @Override public Content content() { return content; }
    @Override public AlganonCharacter player() { return player; }

    @Override public WorldModel world() { return world; }

    @Override public WorldModel loadZone(String zoneId) {
        ZoneBlueprint bp = content.blueprint(zoneId);
        if (bp == null) throw new IllegalArgumentException("Unknown zone id: " + zoneId);
        this.world = new AlganonWorld(bp, content);
        return world;
    }

    @Override public long seed() { return seed; }

    @Override public long lastSaveEpochMillis() { return lastSaveEpochMillis; }
    @Override public void setLastSaveEpochMillis(long millis) { this.lastSaveEpochMillis = millis; }

    @Override public int asharrWarScore() { return asharrWarScore; }
    @Override public int kujixWarScore() { return kujixWarScore; }
    @Override public void setWarScores(int asharr, int kujix) {
        this.asharrWarScore = asharr;
        this.kujixWarScore = kujix;
    }
}
