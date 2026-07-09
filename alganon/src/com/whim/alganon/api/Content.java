package com.whim.alganon.api;

import com.whim.alganon.api.Defs.AbilityDef;
import com.whim.alganon.api.Defs.ClassDef;
import com.whim.alganon.api.Defs.FamilyDef;
import com.whim.alganon.api.Defs.GatherNodeDef;
import com.whim.alganon.api.Defs.ItemDef;
import com.whim.alganon.api.Defs.MobDef;
import com.whim.alganon.api.Defs.QuestDef;
import com.whim.alganon.api.Defs.RaceDef;
import com.whim.alganon.api.Defs.RecipeDef;
import com.whim.alganon.api.Defs.ZoneMeta;
import com.whim.alganon.api.Enums.FamilyArchetype;

import java.util.List;

/**
 * Immutable content registry authored by Task 1. All lookups are id-based; list
 * methods feed the character-creation wizard and the crafting/auction panels.
 */
public interface Content {
    // ----- creation wizard -----
    List<RaceDef> races();
    List<FamilyDef> families();                 // all; filter by race's faction in UI
    List<ClassDef> classes();

    // ----- id lookups -----
    RaceDef race(String id);
    FamilyDef family(String id);
    ClassDef clazz(String classId);
    AbilityDef ability(String id);
    ItemDef item(String id);
    MobDef mob(String id);
    QuestDef quest(String id);
    RecipeDef recipe(String id);
    GatherNodeDef gatherNode(String id);
    ZoneMeta zone(String id);

    // ----- bulk -----
    List<QuestDef> staticQuests();
    List<RecipeDef> recipes();
    List<ZoneMeta> zones();
    String startingZoneId(String raceId);

    /**
     * Dynamic quest generator (single-player procedural quests). Returns a fresh
     * procedural QuestDef appropriate to the level, biased by the family archetype.
     */
    QuestDef generateQuest(int level, FamilyArchetype archetype, java.util.Random rng);
}
