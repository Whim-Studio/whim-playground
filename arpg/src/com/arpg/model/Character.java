package com.arpg.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The player avatar. A {@link CombatParticipant} carrying attributes, level/XP
 * progression, equipped gear, an inventory, currency, and active buffs.
 *
 * <p>This class holds and aggregates data only. Derived pools (max health /
 * resource) are recomputed from persistent sources — base class stats, level,
 * attributes and equipped gear — via {@link #recalculateDerivedStats()}. It does
 * NOT resolve combat, roll loot, or decide when a level-up happens; those belong
 * to the engine.</p>
 */
public class Character implements CombatParticipant {

    private static final long serialVersionUID = 1L;

    private String name;
    private final CharacterClass characterClass;

    private int level;
    private long experience;
    private long experienceToNextLevel;
    private int unspentAttributePoints;

    private int strength;
    private int agility;
    private int intellect;
    private int vitality;

    private int currentHealth;
    private int maxHealth;
    private int currentResource;
    private int maxResource;

    private int gold;

    private final Inventory inventory;
    private final Map<EquipmentSlot, Equipment> equipped;
    private final List<BuffDebuff> activeBuffs;
    private final List<Ability> abilities;

    public Character(String name, CharacterClass characterClass, List<Ability> abilities) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Character name must not be blank");
        }
        if (characterClass == null) {
            throw new IllegalArgumentException("Character requires a CharacterClass");
        }
        this.name = name;
        this.characterClass = characterClass;
        this.level = 1;
        this.experience = 0L;
        this.experienceToNextLevel = experienceForLevel(2);
        this.unspentAttributePoints = 0;

        this.strength = characterClass.getBaseStrength();
        this.agility = characterClass.getBaseAgility();
        this.intellect = characterClass.getBaseIntellect();
        this.vitality = characterClass.getBaseVitality();

        this.gold = 0;
        this.inventory = new Inventory();
        this.equipped = new EnumMap<EquipmentSlot, Equipment>(EquipmentSlot.class);
        this.activeBuffs = new ArrayList<BuffDebuff>();
        this.abilities = new ArrayList<Ability>();
        if (abilities != null) {
            this.abilities.addAll(abilities);
        }

        recalculateDerivedStats();
        this.currentHealth = this.maxHealth;
        this.currentResource = this.maxResource;
    }

    /** Standard XP curve: quadratic growth. Pure data helper, floored to int-safe long. */
    public static long experienceForLevel(int targetLevel) {
        if (targetLevel <= 1) {
            return 0L;
        }
        long l = targetLevel;
        return 40L * (l - 1) * (l - 1) + 60L * (l - 1);
    }

    // ---- CombatParticipant ----

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCurrentHealth() {
        return currentHealth;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public int getCurrentResource() {
        return currentResource;
    }

    @Override
    public int getMaxResource() {
        return maxResource;
    }

    @Override
    public boolean isAlive() {
        return currentHealth > 0;
    }

    @Override
    public void applyDamage(int amount) {
        if (amount <= 0) {
            return;
        }
        currentHealth = Math.max(0, currentHealth - amount);
    }

    @Override
    public void applyHealing(int amount) {
        if (amount <= 0) {
            return;
        }
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    @Override
    public List<Ability> getAbilities() {
        return new ArrayList<Ability>(abilities);
    }

    @Override
    public List<BuffDebuff> getActiveBuffs() {
        return new ArrayList<BuffDebuff>(activeBuffs);
    }

    @Override
    public void addBuff(BuffDebuff b) {
        if (b != null) {
            activeBuffs.add(b);
        }
    }

    @Override
    public void removeBuff(BuffDebuff b) {
        activeBuffs.remove(b);
    }

    // ---- Resource management (data mutation, engine drives spend/regen) ----

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }

    public void spendResource(int amount) {
        if (amount <= 0) {
            return;
        }
        currentResource = Math.max(0, currentResource - amount);
    }

    public void restoreResource(int amount) {
        if (amount <= 0) {
            return;
        }
        currentResource = Math.min(maxResource, currentResource + amount);
    }

    /** Attack power derived from the class's primary attribute plus weapon/gear bonuses. */
    public int getAttackPower() {
        int base;
        StatType primary = characterClass.getPrimaryStat();
        if (primary == StatType.AGILITY) {
            base = getEffectiveAgility();
        } else if (primary == StatType.INTELLECT) {
            base = getEffectiveIntellect();
        } else if (primary == StatType.VITALITY) {
            base = getEffectiveVitality();
        } else {
            base = getEffectiveStrength();
        }
        return base + equipmentBonus(StatType.ATTACK_POWER) + equipmentBonus(StatType.SPELL_POWER);
    }

    /** Defense derived from vitality plus armor bonuses on equipped gear. */
    public int getDefense() {
        return getEffectiveVitality() / 2 + equipmentBonus(StatType.ARMOR);
    }

    public void setCurrentHealth(int value) {
        this.currentHealth = clamp(value, 0, maxHealth);
    }

    public void setCurrentResource(int value) {
        this.currentResource = clamp(value, 0, maxResource);
    }

    // ---- Attributes ----

    public int getStrength() {
        return strength;
    }

    public int getAgility() {
        return agility;
    }

    public int getIntellect() {
        return intellect;
    }

    public int getVitality() {
        return vitality;
    }

    /** Base attribute plus flat bonuses from equipped gear. */
    public int getEffectiveStrength() {
        return strength + equipmentBonus(StatType.STRENGTH);
    }

    public int getEffectiveAgility() {
        return agility + equipmentBonus(StatType.AGILITY);
    }

    public int getEffectiveIntellect() {
        return intellect + equipmentBonus(StatType.INTELLECT);
    }

    public int getEffectiveVitality() {
        return vitality + equipmentBonus(StatType.VITALITY);
    }

    public int getUnspentAttributePoints() {
        return unspentAttributePoints;
    }

    public void setUnspentAttributePoints(int points) {
        this.unspentAttributePoints = Math.max(0, points);
    }

    public void grantAttributePoints(int points) {
        if (points > 0) {
            this.unspentAttributePoints += points;
        }
    }

    /**
     * Spend one unspent point on the named attribute ("strength"/"agility"/
     * "intellect"/"vitality", case-insensitive). Returns true if applied.
     */
    public boolean allocateAttribute(String attribute) {
        if (attribute == null || unspentAttributePoints <= 0) {
            return false;
        }
        String a = attribute.trim().toLowerCase();
        if ("strength".equals(a)) {
            strength++;
        } else if ("agility".equals(a)) {
            agility++;
        } else if ("intellect".equals(a)) {
            intellect++;
        } else if ("vitality".equals(a)) {
            vitality++;
        } else {
            return false;
        }
        unspentAttributePoints--;
        recalculateDerivedStats();
        return true;
    }

    // ---- Progression ----

    public int getLevel() {
        return level;
    }

    public long getExperience() {
        return experience;
    }

    public long getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    public void setExperience(long experience) {
        this.experience = Math.max(0L, experience);
    }

    /** Add XP without auto-leveling — the engine checks {@link #canLevelUp()}. */
    public void addExperience(long amount) {
        if (amount > 0) {
            this.experience += amount;
        }
    }

    public boolean canLevelUp() {
        return experience >= experienceToNextLevel;
    }

    /**
     * Advance one level: bumps level, grants attribute points, recomputes pools
     * and refills them, and sets the next threshold. The engine decides WHEN to
     * call this (typically while {@link #canLevelUp()} is true).
     */
    public void levelUp() {
        level++;
        unspentAttributePoints += 3;
        experienceToNextLevel = experienceForLevel(level + 1);
        recalculateDerivedStats();
        currentHealth = maxHealth;
        currentResource = maxResource;
    }

    // ---- Currency ----

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount > 0) {
            gold += amount;
        }
    }

    public boolean spendGold(int amount) {
        if (amount <= 0 || amount > gold) {
            return false;
        }
        gold -= amount;
        return true;
    }

    // ---- Inventory & equipment ----

    public Inventory getInventory() {
        return inventory;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    public Map<EquipmentSlot, Equipment> getEquipped() {
        return new EnumMap<EquipmentSlot, Equipment>(equipped);
    }

    public Equipment getEquipped(EquipmentSlot slot) {
        return slot == null ? null : equipped.get(slot);
    }

    /**
     * Place a piece of gear in its slot, returning whatever was previously worn
     * there (or null). Recomputes derived stats. Does not touch the inventory —
     * the engine owns the move-between-containers policy.
     */
    public Equipment equip(Equipment item) {
        if (item == null) {
            return null;
        }
        Equipment previous = equipped.put(item.getSlot(), item);
        recalculateDerivedStats();
        clampCurrentToMax();
        return previous;
    }

    /** Remove and return the gear in the given slot (or null). Recomputes stats. */
    public Equipment unequip(EquipmentSlot slot) {
        if (slot == null) {
            return null;
        }
        Equipment removed = equipped.remove(slot);
        if (removed != null) {
            recalculateDerivedStats();
            clampCurrentToMax();
        }
        return removed;
    }

    public boolean meetsRequirement(Equipment item) {
        return item != null && level >= item.getLevelRequirement();
    }

    // ---- Derived stats ----

    public int equipmentBonus(StatType type) {
        int total = 0;
        for (Equipment e : equipped.values()) {
            if (e != null) {
                total += e.getStatModifier(type);
            }
        }
        return total;
    }

    /**
     * Recompute {@link #maxHealth} and {@link #maxResource} from base class
     * values, level, effective attributes, and equipment. Current values are
     * clamped to the new maxima. Persistent sources only — buffs are layered by
     * the engine at damage time, not baked into the pools.
     */
    public final void recalculateDerivedStats() {
        int effVit = getEffectiveVitality();
        int effInt = getEffectiveIntellect();
        this.maxHealth = characterClass.getBaseHealth()
                + (level - 1) * 12
                + effVit * 8
                + equipmentBonus(StatType.MAX_HEALTH);
        this.maxResource = characterClass.getBaseResource()
                + (level - 1) * 3
                + effInt * 3
                + equipmentBonus(StatType.MAX_RESOURCE);
        if (this.maxHealth < 1) {
            this.maxHealth = 1;
        }
        if (this.maxResource < 0) {
            this.maxResource = 0;
        }
        clampCurrentToMax();
    }

    private void clampCurrentToMax() {
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
        if (currentResource > maxResource) {
            currentResource = maxResource;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    @Override
    public String toString() {
        return name + " (Lv " + level + " " + characterClass.getDisplayName() + ")";
    }
}
