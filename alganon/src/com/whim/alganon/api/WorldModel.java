package com.whim.alganon.api;

import com.whim.alganon.api.Enums.TileType;

import java.util.List;

/**
 * Mutable loaded-zone model authored by Task 1. Holds the tile grid plus live NPC,
 * mob, and gather-node entities. The engine steps the player, spawns/removes mobs,
 * and depletes/respawns nodes through this interface.
 */
public interface WorldModel {
    String zoneId();
    String zoneName();
    int width();
    int height();
    TileType tileAt(int x, int y);
    boolean walkable(int x, int y);

    List<NpcEntity> npcs();
    List<MobEntity> mobs();
    List<NodeEntity> nodes();
    List<Portal> portals();

    /** NPC with position, vendor/quest-giver flags, and a procedural sprite key. */
    interface NpcEntity {
        String id(); String name(); GridPos pos();
        boolean questGiver(); boolean vendor(); String spriteKey();
    }

    /** Live mob entity — a Combatant with a world position and simple AI state. */
    interface MobEntity extends Combatant {
        String id(); String defId(); GridPos pos(); void setPos(GridPos p);
        int level(); String spriteKey();
        boolean inCombat(); void setInCombat(boolean v);
    }

    interface NodeEntity {
        String id(); String name(); GridPos pos();
        boolean depleted(); void setDepleted(boolean v);
        double respawnRemaining(); void setRespawnRemaining(double sec);
    }

    interface Portal { GridPos pos(); String targetZoneId(); String label(); }
}
