package com.whim.alganon.ui.stub;

import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Canned committed character implementing {@link Views.CharacterView} for the UI stub. */
final class StubCharacter implements Views.CharacterView {

    final Defs.RaceDef race;
    final Defs.FamilyDef family;
    final Defs.ClassDef clazz;
    final String name;

    GridPos pos = new GridPos(16, 12);
    int level = 3;
    long xp = 420, xpToNext = 1000;
    int hp = 165, maxHp = 210;
    int resource, maxResource = 100;
    long gold = 137;
    Stance stance = Stance.BALANCE;
    School school = School.NONE;
    SkillType studying = SkillType.WEAPON;

    final List<StubAbility> abilities = new ArrayList<StubAbility>();
    private final Map<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
    private final Map<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);
    private final List<Views.ItemView> inventory = new ArrayList<Views.ItemView>();
    private final Map<EquipSlot, Views.ItemView> equipped = new EnumMap<EquipSlot, Views.ItemView>(EquipSlot.class);

    StubCharacter(StubContent c, String raceId, String familyId, String classId, String name) {
        this.race = c.race(raceId);
        this.family = c.family(familyId);
        this.clazz = c.clazz(classId);
        this.name = name;
        this.resource = clazz != null && clazz.resource == ResourceType.FURY ? 20 : 70;

        // base stats + racial mods
        for (StatType s : StatType.values()) stats.put(s, 10);
        if (race != null) for (Map.Entry<StatType, Integer> e : race.statMods.entrySet())
            stats.put(e.getKey(), stats.get(e.getKey()) + e.getValue());

        skills.put(SkillType.WEAPON, 14); skills.put(SkillType.DEFENSE, 9); skills.put(SkillType.CASTING, 6);
        skills.put(SkillType.HEALING, 4); skills.put(SkillType.GATHERING, 12); skills.put(SkillType.PROCESSING, 7);
        skills.put(SkillType.CRAFTING, 5);

        if (clazz != null) {
            for (String aid : clazz.abilityIds) {
                Defs.AbilityDef d = c.ability(aid);
                if (d != null && d.levelReq <= level) abilities.add(new StubAbility(d));
            }
        }

        // starter inventory + equipment
        addItem(c.item("healing_draught"), 3);
        addItem(c.item("copper_ore"), 6);
        addItem(c.item("padded_vest"), 1);
        Defs.ItemDef sword = c.item("rusted_sword");
        if (sword != null) equipped.put(EquipSlot.WEAPON, new ItemV(sword, 1));
        Defs.ItemDef vest = c.item("padded_vest");
        if (vest != null) equipped.put(EquipSlot.CHEST, new ItemV(vest, 1));
    }

    private void addItem(Defs.ItemDef d, int qty) { if (d != null) inventory.add(new ItemV(d, qty)); }

    StubAbility ability(String id) {
        for (StubAbility a : abilities) if (a.id.equals(id)) return a;
        return null;
    }

    // ---- CharacterView ----
    public String name() { return name; }
    public Faction faction() { return race != null ? race.faction : Faction.ASHARR; }
    public String raceName() { return race != null ? race.name : "?"; }
    public String familyName() { return family != null ? family.name : "?"; }
    public FamilyArchetype archetype() { return family != null ? family.archetype : FamilyArchetype.ACHIEVER; }
    public String className() { return clazz != null ? clazz.name : "?"; }
    public int level() { return level; }
    public long xp() { return xp; }
    public long xpToNext() { return xpToNext; }
    public int hp() { return hp; }
    public int maxHp() { return maxHp; }
    public ResourceType resourceType() { return clazz != null ? clazz.resource : ResourceType.MANA; }
    public int resource() { return resource; }
    public int maxResource() { return maxResource; }
    public Stance stance() { return stance; }
    public School school() { return school; }
    public Map<StatType, Integer> stats() { return Collections.unmodifiableMap(stats); }
    public Map<SkillType, Integer> skills() { return Collections.unmodifiableMap(skills); }
    public long gold() { return gold; }
    public List<Views.AbilityView> abilities() { return new ArrayList<Views.AbilityView>(abilities); }
    public List<Views.ItemView> inventory() { return Collections.unmodifiableList(inventory); }
    public Map<EquipSlot, Views.ItemView> equipped() { return Collections.unmodifiableMap(equipped); }
    public GridPos pos() { return pos; }
    public String zoneId() { return "frontier"; }

    Views.StudyView studyView() { return new StudyV(this); }

    // ---- item view ----
    static final class ItemV implements Views.ItemView {
        private final Defs.ItemDef d; private final int qty;
        ItemV(Defs.ItemDef d, int qty) { this.d = d; this.qty = qty; }
        public String id() { return d.id; } public String name() { return d.name; } public String description() { return d.description; }
        public com.whim.alganon.api.Enums.ItemType type() { return d.type; } public EquipSlot slot() { return d.slot; }
        public int quantity() { return qty; } public int value() { return d.value; } public int power() { return d.power; }
    }

    // ---- study view ----
    static final class StudyV implements Views.StudyView {
        private final StubCharacter c;
        StudyV(StubCharacter c) { this.c = c; }
        public List<SkillType> studyableSkills() { return Arrays.asList(SkillType.values()); }
        public SkillType assignedSkill() { return c.studying; }
        public int studySlots() { return 1; }
        public double bankedHours() { return 3.5; }
        public double capHours() { return 8.0; }
        public double progressToNextPoint() { return 0.62; }
        public Map<SkillType, Integer> skillLevels() { return c.skills(); }
    }
}
