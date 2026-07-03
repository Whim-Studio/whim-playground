package com.arpg.engine;

import com.arpg.model.Enemy;
import com.arpg.model.GameContent;
import com.arpg.model.Realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Exploration: lists realms, resolves encounter nodes and spawns fresh enemy instances.
 * Combat/elite nodes draw weighted spawns from the realm; boss nodes spawn the flagged boss.
 */
final class RealmEngine {
    private final Random rng;

    RealmEngine(Random rng) {
        this.rng = rng;
    }

    List<Realm> getRealms() {
        return GameContent.getRealms();
    }

    Realm getRealm(String id) {
        return GameContent.getRealm(id);
    }

    int encounterCount(Realm realm) {
        return realm == null ? 0 : realm.getEncounters().size();
    }

    boolean hasEncounter(Realm realm, int index) {
        return realm != null && index >= 0 && index < realm.getEncounters().size();
    }

    Realm.EncounterDef encounterAt(Realm realm, int index) {
        if (!hasEncounter(realm, index)) return null;
        return realm.getEncounters().get(index);
    }

    boolean isBossEncounter(Realm realm, int index) {
        Realm.EncounterDef e = encounterAt(realm, index);
        return e != null && e.getType() == Realm.EncounterType.BOSS;
    }

    /** Spawn fresh, independent enemy instances for the given encounter (empty for non-combat nodes). */
    List<Enemy> spawnEncounter(Realm realm, int index) {
        List<Enemy> spawned = new ArrayList<Enemy>();
        Realm.EncounterDef enc = encounterAt(realm, index);
        if (enc == null) return spawned;
        switch (enc.getType()) {
            case BOSS: {
                Enemy boss = GameContent.spawnEnemy(enc.getBossEnemyId());
                if (boss != null) spawned.add(boss);
                break;
            }
            case ELITE: {
                Enemy e = pickFresh(realm);
                if (e != null) spawned.add(e);
                break;
            }
            case COMBAT: {
                int count = 1 + rng.nextInt(2); // 1-2 foes
                for (int i = 0; i < count; i++) {
                    Enemy e = pickFresh(realm);
                    if (e != null) spawned.add(e);
                }
                break;
            }
            default:
                // TREASURE / REST / EVENT — no combat.
                break;
        }
        return spawned;
    }

    private Enemy pickFresh(Realm realm) {
        int total = realm.getSpawnTotalWeight();
        if (total <= 0) return null;
        Enemy picked = realm.pickSpawn(rng.nextInt(total));
        return picked == null ? null : picked.copy();
    }
}
