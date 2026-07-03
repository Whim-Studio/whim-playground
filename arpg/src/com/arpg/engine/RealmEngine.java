package com.arpg.engine;

import com.arpg.model.Enemy;
import com.arpg.model.GameContent;
import com.arpg.model.Realm;

import java.util.ArrayList;
import java.util.List;

/**
 * Exploration: lists realms, resolves encounters and spawns fresh enemy instances from a realm's
 * encounter definitions. Boss trigger = the encounter is flagged as a boss encounter.
 */
final class RealmEngine {
    private final GameContent content;

    RealmEngine(GameContent content) {
        this.content = content;
    }

    List<Realm> getRealms() {
        return content.getRealms();
    }

    Realm getRealm(String id) {
        return content.getRealm(id);
    }

    boolean hasEncounter(Realm realm, int index) {
        return realm != null && index >= 0 && index < realm.getEncounterCount();
    }

    Realm.Encounter encounterAt(Realm realm, int index) {
        if (!hasEncounter(realm, index)) return null;
        return realm.getEncounters().get(index);
    }

    boolean isBossEncounter(Realm realm, int index) {
        Realm.Encounter e = encounterAt(realm, index);
        return e != null && e.isBoss();
    }

    /** Spawn fresh, independent enemy instances for the given encounter (empty if none/invalid). */
    List<Enemy> spawnEncounter(Realm realm, int index) {
        List<Enemy> spawned = new ArrayList<Enemy>();
        Realm.Encounter enc = encounterAt(realm, index);
        if (enc == null) return spawned;
        List<String> ids = enc.getEnemyTemplateIds();
        for (int i = 0; i < ids.size(); i++) {
            Enemy template = content.getEnemyTemplate(ids.get(i));
            if (template != null) spawned.add(template.spawnCopy());
        }
        return spawned;
    }
}
