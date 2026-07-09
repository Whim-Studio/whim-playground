package com.whim.alganon.model;

import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.ItemDef;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.GridPos;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable player model and player-side {@link com.whim.alganon.api.Combatant}. Holds
 * pure state plus low-level invariants (clamping, stacking, gear-derived attack/defense);
 * the XP curve, combat resolution and study accrual live in the Task 2 engine.
 *
 * <p>Attack/defense derive from the class primary stat, equipped gear and the active
 * stance so the numbers stay coherent even before the engine layers on its own math.
 * [Gap — my design] the exact stat→combat formulas are original tuning.</p>
 */
public final class AlganonCharacter implements CharacterModel {

    private final Content content;

    private String name = "Adventurer";
    private Faction faction = Faction.ASHARR;
    private String raceId, familyId, classId;

    private int level = 1;
    private long xp = 0;
    private ResourceType resourceType = ResourceType.MANA;
    private int resource = 0, maxResource = 100;
    private int hp = 100, maxHp = 100;

    private Stance stance = Stance.BALANCE;
    private School school = School.NONE;

    private final Map<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
    private final Map<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);
    private final List<String> knownAbilities = new ArrayList<String>();

    private long gold = 0;
    private final Map<String, Integer> inventory = new LinkedHashMap<String, Integer>();
    private final Map<EquipSlot, String> equipped = new EnumMap<EquipSlot, String>(EquipSlot.class);

    private SkillType studyAssignment = null;
    private double bankedStudyProgress = 0.0;

    private GridPos pos = new GridPos(0, 0);
    private String zoneId;

    public AlganonCharacter(Content content) {
        this.content = content;
        for (StatType s : StatType.values()) stats.put(s, 10);
        for (SkillType s : SkillType.values()) skills.put(s, 0);
    }

    // ------------------------------------------------------------ identity
    @Override public String getName() { return name; }
    @Override public void setName(String name) { if (name != null && !name.trim().isEmpty()) this.name = name.trim(); }
    @Override public Faction faction() { return faction; }
    public void setFaction(Faction f) { this.faction = f; }
    @Override public String raceId() { return raceId; }
    public void setRaceId(String id) { this.raceId = id; }
    @Override public String familyId() { return familyId; }
    public void setFamilyId(String id) { this.familyId = id; }
    @Override public String classId() { return classId; }
    public void setClassId(String id) { this.classId = id; }

    // --------------------------------------------------------- progression
    @Override public int level() { return level; }
    @Override public void setLevel(int level) { this.level = Math.max(1, level); }
    @Override public long xp() { return xp; }
    @Override public void setXp(long xp) { this.xp = Math.max(0, xp); }
    @Override public ResourceType resourceType() { return resourceType; }
    public void setResourceType(ResourceType r) { this.resourceType = r; }
    @Override public int resource() { return resource; }
    @Override public int maxResource() { return maxResource; }
    @Override public void setResource(int value) { this.resource = clamp(value, 0, maxResource); }
    @Override public void setMaxResource(int value) { this.maxResource = Math.max(0, value); this.resource = clamp(resource, 0, maxResource); }
    @Override public void setMaxHp(int value) { this.maxHp = Math.max(1, value); this.hp = clamp(hp, 0, maxHp); }
    @Override public void setHp(int value) { this.hp = clamp(value, 0, maxHp); }

    // ----------------------------------------------------------------- spec
    @Override public Stance stance() { return stance; }
    @Override public void setStance(Stance stance) { if (stance != null) this.stance = stance; }
    @Override public School school() { return school; }
    @Override public void setSchool(School school) { if (school != null) this.school = school; }

    // ------------------------------------------------------- stats & skills
    @Override public Map<StatType, Integer> stats() { return stats; }
    @Override public int skill(SkillType s) { Integer v = skills.get(s); return v == null ? 0 : v; }
    @Override public void addSkillProgress(SkillType s, int points) {
        if (s == null) return;
        skills.put(s, Math.max(0, skill(s) + points));
    }

    // ------------------------------------------------------------ abilities
    @Override public List<String> knownAbilityIds() { return knownAbilities; }
    @Override public void learnAbility(String abilityId) {
        if (abilityId != null && !knownAbilities.contains(abilityId)) knownAbilities.add(abilityId);
    }

    // ---------------------------------------------------- economy/inventory
    @Override public long gold() { return gold; }
    @Override public void addGold(long delta) { gold = Math.max(0, gold + delta); }
    @Override public Map<String, Integer> inventory() { return inventory; }
    @Override public void addItem(String itemId, int qty) {
        if (itemId == null || qty <= 0) return;
        Integer cur = inventory.get(itemId);
        inventory.put(itemId, (cur == null ? 0 : cur) + qty);
    }
    @Override public boolean removeItem(String itemId, int qty) {
        if (itemId == null || qty <= 0) return false;
        Integer cur = inventory.get(itemId);
        if (cur == null || cur < qty) return false;
        int left = cur - qty;
        if (left == 0) inventory.remove(itemId); else inventory.put(itemId, left);
        return true;
    }
    @Override public Map<EquipSlot, String> equipped() { return equipped; }
    @Override public void equip(EquipSlot slot, String itemId) {
        if (slot == null || itemId == null) return;
        equipped.put(slot, itemId);
    }
    @Override public String unequip(EquipSlot slot) {
        if (slot == null) return null;
        return equipped.remove(slot);
    }

    // ----------------------------------------------------------------- study
    @Override public SkillType studyAssignment() { return studyAssignment; }
    @Override public void setStudyAssignment(SkillType s) { this.studyAssignment = s; }
    @Override public double bankedStudyProgress() { return bankedStudyProgress; }
    @Override public void setBankedStudyProgress(double v) { this.bankedStudyProgress = Math.max(0, v); }

    // -------------------------------------------------------------- position
    @Override public GridPos pos() { return pos; }
    @Override public void setPos(GridPos p) { if (p != null) this.pos = p; }
    @Override public String zoneId() { return zoneId; }
    @Override public void setZoneId(String zoneId) { this.zoneId = zoneId; }

    // -------------------------------------------------------------- Combatant
    @Override public String name() { return name; }
    @Override public boolean isPlayer() { return true; }
    @Override public int hp() { return hp; }
    @Override public int maxHp() { return maxHp; }
    @Override public boolean alive() { return hp > 0; }

    @Override public int attackPower() {
        int atk = 4 + effectiveStat(primaryStat());
        String w = equipped.get(EquipSlot.WEAPON);
        ItemDef wd = w == null ? null : content.item(w);
        if (wd != null) atk += wd.power;
        if (stance == Stance.POWER) atk += 3;
        else if (stance == Stance.DEFENSE) atk -= 2;
        return Math.max(1, atk);
    }

    @Override public int defense() {
        int def = effectiveStat(StatType.STAMINA) / 2;
        for (String itemId : equipped.values()) {
            ItemDef d = content.item(itemId);
            if (d != null && d.type == ItemType.ARMOR) def += d.power;
        }
        if (stance == Stance.DEFENSE) def += 3;
        else if (stance == Stance.POWER) def -= 2;
        return Math.max(0, def);
    }

    @Override public int takeDamage(int amount, DamageType type) {
        int dealt = Math.max(1, amount - defense() / 2);
        hp = clamp(hp - dealt, 0, maxHp);
        return dealt;
    }

    @Override public void heal(int amount) {
        if (amount <= 0) return;
        hp = clamp(hp + amount, 0, maxHp);
    }

    // ---------------------------------------------------------------- helpers

    private StatType primaryStat() {
        switch (resourceType) {
            case FURY:  return StatType.MIGHT;
            case FOCUS: return StatType.FINESSE;
            case MANA:  return StatType.INTELLECT;
            default:    return StatType.MIGHT;
        }
    }

    /** Base stat plus equipped-gear stat mods. */
    private int effectiveStat(StatType s) {
        int v = statBase(s);
        for (String itemId : equipped.values()) {
            ItemDef d = content.item(itemId);
            if (d != null) {
                Integer m = d.statMods.get(s);
                if (m != null) v += m;
            }
        }
        return v;
    }

    private int statBase(StatType s) { Integer v = stats.get(s); return v == null ? 0 : v; }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
