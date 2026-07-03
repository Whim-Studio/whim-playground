package com.arpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A hostile {@link CombatParticipant}. Also serves as a spawn <em>template</em>:
 * {@link #copy()} produces a fresh full-health instance so a {@link Realm} can be
 * re-entered without mutating the shared definition.
 */
public class Enemy implements CombatParticipant {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final int level;
    private int maxHealth;
    private int currentHealth;
    private int maxResource;
    private int currentResource;
    private int attackPower;
    private final boolean boss;
    private final List<Ability> abilities;
    private final List<BuffDebuff> activeBuffs;
    private final LootTable lootTable;
    private final int experienceReward;

    public Enemy(String id, String name, int level, int maxHealth, int maxResource,
                 int attackPower, boolean boss, List<Ability> abilities, LootTable lootTable,
                 int experienceReward) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Enemy id must not be blank");
        }
        this.id = id;
        this.name = name;
        this.level = Math.max(1, level);
        this.maxHealth = Math.max(1, maxHealth);
        this.currentHealth = this.maxHealth;
        this.maxResource = Math.max(0, maxResource);
        this.currentResource = this.maxResource;
        this.attackPower = Math.max(0, attackPower);
        this.boss = boss;
        this.abilities = new ArrayList<Ability>();
        if (abilities != null) {
            this.abilities.addAll(abilities);
        }
        this.activeBuffs = new ArrayList<BuffDebuff>();
        this.lootTable = lootTable;
        this.experienceReward = Math.max(0, experienceReward);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
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

    public int getAttackPower() {
        return attackPower;
    }

    public void setAttackPower(int attackPower) {
        this.attackPower = Math.max(0, attackPower);
    }

    public boolean isBoss() {
        return boss;
    }

    public LootTable getLootTable() {
        return lootTable;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public void setCurrentHealth(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > maxHealth) {
            value = maxHealth;
        }
        this.currentHealth = value;
    }

    public void setCurrentResource(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > maxResource) {
            value = maxResource;
        }
        this.currentResource = value;
    }

    public void spendResource(int amount) {
        if (amount > 0) {
            currentResource = Math.max(0, currentResource - amount);
        }
    }

    /** A fresh, full-health copy of this template (buffs cleared). */
    public Enemy copy() {
        return new Enemy(id, name, level, maxHealth, maxResource, attackPower, boss,
                new ArrayList<Ability>(abilities), lootTable, experienceReward);
    }

    @Override
    public String toString() {
        return (boss ? "[BOSS] " : "") + name + " (Lv " + level + ")";
    }
}
