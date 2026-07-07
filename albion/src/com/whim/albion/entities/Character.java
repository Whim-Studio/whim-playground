package com.whim.albion.entities;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Views.CharacterView;
import com.whim.albion.api.Views.ItemView;
import com.whim.albion.api.Views.SpellView;
import com.whim.albion.items.Inventory;
import com.whim.albion.magic.SpellBook;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A playable party member: the 8 primary stats, 4 trainable skills, level/xp,
 * LP/SP pools, an {@link Inventory} of five equip slots + backpack, a
 * {@link SpellBook}, and a profession label. Doubles as its own read-only
 * {@link CharacterView}. Combat participation is via {@link PartyCombatant},
 * which delegates its mutable pools straight back here so battle damage sticks.
 */
public final class Character implements CharacterView {

    /** Base unarmed attack when no weapon is equipped. */
    private static final int FIST_ATTACK = 2;

    private final String name;
    private final String portraitKey;
    private final String profession;

    private int level;
    private int xp;

    private final Map<StatType, Integer> baseStats = new EnumMap<StatType, Integer>(StatType.class);
    private final Map<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);

    private int maxLp;
    private int lp;
    private int maxSp;
    private int sp;

    private final Inventory inventory;
    private final SpellBook spellBook;

    private Character(Builder b) {
        this.name = b.name;
        this.portraitKey = b.portraitKey;
        this.profession = b.profession;
        this.level = b.level;
        this.baseStats.putAll(b.stats);
        this.skills.putAll(b.skills);
        this.inventory = new Inventory(b.content);
        this.spellBook = new SpellBook(b.content, b.schools);
        // Derive pools from stats, then start full.
        this.maxLp = 20 + statBase(StatType.STAMINA) * 2 + (level - 1) * 5;
        this.maxSp = casterCapable() ? 8 + statBase(StatType.MAGIC_TALENT) + (level - 1) * 3 : 0;
        this.lp = maxLp;
        this.sp = maxSp;
        this.xp = xpForLevel(level);
    }

    // ------------------------------------------------------------- accessors

    public Inventory inventoryModel() { return inventory; }
    public SpellBook spellBook() { return spellBook; }

    private int statBase(StatType t) {
        Integer v = baseStats.get(t);
        return v == null ? 0 : v;
    }

    private boolean casterCapable() {
        for (SpellSchool s : SpellSchool.values()) {
            if (spellBook != null && spellBook.canCast(s)) return true;
        }
        // spellBook not yet assigned during pool init: fall back on schools presence
        return false;
    }

    // --------------------------------------------------------- CharacterView

    @Override public String name() { return name; }
    @Override public String portraitKey() { return portraitKey; }
    @Override public String profession() { return profession; }
    @Override public int level() { return level; }
    @Override public int xp() { return xp; }

    @Override public int xpToNext() {
        int need = xpForLevel(level + 1) - xp;
        return Math.max(0, need);
    }

    @Override public int lp() { return lp; }
    @Override public int maxLp() { return maxLp; }
    @Override public int sp() { return sp; }
    @Override public int maxSp() { return maxSp; }
    @Override public boolean alive() { return lp > 0; }

    @Override public int stat(StatType type) {
        return statBase(type) + inventory.statBonus(type);
    }

    @Override public int skill(SkillType type) {
        Integer v = skills.get(type);
        return v == null ? 0 : v;
    }

    @Override public List<ItemView> inventory() { return inventory.asViews(); }

    @Override public ItemView equipped(EquipSlot slot) { return inventory.equippedView(slot); }

    @Override public List<SpellView> spells() {
        List<SpellView> out = new ArrayList<SpellView>();
        for (SpellDef d : spellBook.knownDefs()) {
            out.add(new CharacterSpellView(d, this));
        }
        return out;
    }

    @Override public boolean canCast(SpellSchool school) { return spellBook.canCast(school); }

    /** True if the caster meets SP/level/talent to cast the given spell right now. */
    public boolean canCastNow(SpellDef d) {
        if (d == null || !canCast(d.school)) return false;
        return sp >= d.spCost
                && level >= d.levelReq
                && stat(StatType.MAGIC_TALENT) >= d.talentReq;
    }

    // ----------------------------------------------------------- combat glue

    public int attackPower() { return Math.max(FIST_ATTACK, inventory.equipAttack()); }
    public DamageType attackType() { return inventory.weaponDamageType(); }
    public int defense() { return inventory.equipDefense() + statBase(StatType.STAMINA) / 4; }
    public boolean ranged() { return skill(SkillType.RANGED) > skill(SkillType.MELEE); }

    public void damage(int amount) { lp = Math.max(0, lp - amount); }

    public int healLp(int amount) {
        int gain = Math.min(Math.max(0, amount), maxLp - lp);
        lp += gain;
        return gain;
    }

    public boolean spendSp(int amount) {
        if (amount <= 0) return true;
        if (sp < amount) return false;
        sp -= amount;
        return true;
    }

    public void restoreSp(int amount) { sp = Math.min(maxSp, sp + Math.max(0, amount)); }

    public void restoreAll() { lp = maxLp; sp = maxSp; }

    // --------------------------------------------------------- items/spells

    public ActionResult equip(String itemId) { return inventory.equip(itemId); }

    public ActionResult unequip(EquipSlot slot) { return inventory.unequip(slot); }

    /** Use a consumable/scroll from the pack, applying its effect. */
    public ActionResult useItem(String itemId) {
        ItemDef d = inventory.takeConsumable(itemId);
        if (d == null) return ActionResult.fail("Not carried.");
        StringBuilder msg = new StringBuilder();
        boolean did = false;
        if (d.healAmount > 0) {
            int g = healLp(d.healAmount);
            msg.append(name).append(" recovers ").append(g).append(" LP. ");
            did = true;
        }
        if (d.manaAmount > 0) {
            int before = sp;
            restoreSp(d.manaAmount);
            msg.append(name).append(" recovers ").append(sp - before).append(" SP. ");
            did = true;
        }
        if (d.spellId != null && spellBook.learn(d.spellId)) {
            msg.append(name).append(" studies ").append(d.name).append(". ");
            did = true;
        }
        if (!did) {
            // Non-usable item: put it back and refuse.
            inventory.add(itemId, 1);
            return ActionResult.fail("Nothing happens.");
        }
        return ActionResult.ok(msg.toString().trim());
    }

    // ----------------------------------------------------------- progression

    /** Cumulative XP required to reach {@code targetLevel} (level 1 == 0). */
    public static int xpForLevel(int targetLevel) {
        int l = targetLevel - 1;
        return 100 * (l * (l + 1)) / 2;
    }

    /** Add XP and apply any resulting level-ups. Returns levels gained. */
    public int addXp(int amount) {
        if (amount <= 0 || !alive()) return 0;
        xp += amount;
        int gained = 0;
        while (xp >= xpForLevel(level + 1)) {
            levelUp();
            gained++;
        }
        return gained;
    }

    private void levelUp() {
        level++;
        maxLp += 5 + statBase(StatType.STAMINA) / 3;
        if (maxSp > 0 || casterCapable()) {
            maxSp += 3 + statBase(StatType.MAGIC_TALENT) / 4;
        }
        // Modest primary-stat growth so late-game characters feel stronger.
        bump(StatType.STRENGTH, 1);
        bump(StatType.STAMINA, 1);
        // Small skill improvement.
        skills.put(SkillType.MELEE, Math.min(100, skill(SkillType.MELEE) + 2));
        restoreAll();
    }

    private void bump(StatType t, int by) { baseStats.put(t, statBase(t) + by); }

    // -------------------------------------------------------------- builder

    public static final class Builder {
        private final String name;
        private final Content content;
        private String portraitKey = "";
        private String profession = "Adventurer";
        private int level = 1;
        private final Map<StatType, Integer> stats = new EnumMap<StatType, Integer>(StatType.class);
        private final Map<SkillType, Integer> skills = new EnumMap<SkillType, Integer>(SkillType.class);
        private Set<SpellSchool> schools;

        public Builder(String name, Content content) {
            this.name = name;
            this.content = content;
            for (StatType t : StatType.values()) stats.put(t, 10);
            for (SkillType s : SkillType.values()) skills.put(s, 5);
        }

        public Builder portrait(String k) { this.portraitKey = k; return this; }
        public Builder profession(String p) { this.profession = p; return this; }
        public Builder level(int l) { this.level = Math.max(1, l); return this; }
        public Builder stat(StatType t, int v) { this.stats.put(t, v); return this; }
        public Builder skill(SkillType s, int v) { this.skills.put(s, v); return this; }
        public Builder schools(Set<SpellSchool> s) { this.schools = s; return this; }

        public Character build() { return new Character(this); }
    }
}
