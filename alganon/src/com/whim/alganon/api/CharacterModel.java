package com.whim.alganon.api;

import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;

import java.util.List;
import java.util.Map;

/**
 * Mutable player domain model authored by Task 1. Pure state + simple invariants
 * (clamping, stacking); higher-level rules (xp curve application, combat, study
 * accrual) live in the Task 2 engine, which reads and mutates this. Also acts as
 * the player {@link Combatant}.
 */
public interface CharacterModel extends Combatant {
    // identity
    String getName(); void setName(String name);
    Faction faction();
    String raceId(); String familyId(); String classId();

    // progression
    int level(); void setLevel(int level);
    long xp(); void setXp(long xp);
    ResourceType resourceType();
    int resource(); int maxResource();
    void setResource(int value); void setMaxResource(int value);
    void setMaxHp(int value); void setHp(int value);

    // spec
    Stance stance(); void setStance(Stance stance);
    School school(); void setSchool(School school);

    // stats & skills
    Map<StatType, Integer> stats();
    int skill(SkillType s); void addSkillProgress(SkillType s, int points);

    // abilities known (ids from Content); engine gates usage by level
    List<String> knownAbilityIds();
    void learnAbility(String abilityId);

    // economy / inventory
    long gold(); void addGold(long delta);
    /** itemId -> quantity, plus equipped map. Task 1 owns the concrete container. */
    Map<String, Integer> inventory();
    void addItem(String itemId, int qty);
    boolean removeItem(String itemId, int qty);
    Map<EquipSlot, String> equipped();     // slot -> itemId
    void equip(EquipSlot slot, String itemId);
    String unequip(EquipSlot slot);

    // study / offline progression persistent fields
    SkillType studyAssignment();            // null if none
    void setStudyAssignment(SkillType s);
    double bankedStudyProgress();           // fractional points banked toward assigned skill
    void setBankedStudyProgress(double v);

    // position
    GridPos pos(); void setPos(GridPos p);
    String zoneId(); void setZoneId(String zoneId);
}
