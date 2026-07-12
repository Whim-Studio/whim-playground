package com.whim.firetop.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A player-controlled adventurer. Tracks the three Fighting Fantasy attributes
 * (SKILL, STAMINA, LUCK), each as an initial (maximum) and current value, plus
 * gold, provisions, inventory and current board position.
 */
public final class Character implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;

    private final int skillInitial;
    private int skillCurrent;
    private final int staminaInitial;
    private int staminaCurrent;
    private final int luckInitial;
    private int luckCurrent;

    private int gold;
    private int provisions;
    private final List<Item> inventory = new ArrayList<Item>();

    private int roomId;
    private boolean alive = true;

    /**
     * @param name    adventurer name
     * @param skill   initial SKILL (also current)
     * @param stamina initial STAMINA (also current)
     * @param luck    initial LUCK (also current)
     */
    public Character(String name, int skill, int stamina, int luck) {
        this.name = name;
        this.skillInitial = skill;
        this.skillCurrent = skill;
        this.staminaInitial = stamina;
        this.staminaCurrent = stamina;
        this.luckInitial = luck;
        this.luckCurrent = luck;
        this.provisions = 2;
        this.gold = 0;
    }

    public String getName() { return name; }

    public int getSkillInitial() { return skillInitial; }
    public int getSkillCurrent() { return skillCurrent; }
    public int getStaminaInitial() { return staminaInitial; }
    public int getStaminaCurrent() { return staminaCurrent; }
    public int getLuckInitial() { return luckInitial; }
    public int getLuckCurrent() { return luckCurrent; }

    public int getGold() { return gold; }
    public int getProvisions() { return provisions; }
    public List<Item> getInventory() { return inventory; }
    public int getRoomId() { return roomId; }
    public boolean isAlive() { return alive; }

    public void setRoomId(int roomId) { this.roomId = roomId; }

    // --- STAMINA ---------------------------------------------------------

    /** Reduces STAMINA (never below 0); marks the adventurer dead at 0. */
    public void loseStamina(int amount) {
        staminaCurrent -= amount;
        if (staminaCurrent <= 0) {
            staminaCurrent = 0;
            alive = false;
        }
    }

    /** Restores STAMINA up to the initial maximum. */
    public void gainStamina(int amount) {
        staminaCurrent += amount;
        if (staminaCurrent > staminaInitial) {
            staminaCurrent = staminaInitial;
        }
    }

    // --- LUCK ------------------------------------------------------------

    /** Spends one point of LUCK (each Test Your Luck), never below 0. */
    public void spendLuck() {
        if (luckCurrent > 0) {
            luckCurrent--;
        }
    }

    /** Restores LUCK up to the initial maximum. */
    public void gainLuck(int amount) {
        luckCurrent += amount;
        if (luckCurrent > luckInitial) {
            luckCurrent = luckInitial;
        }
    }

    // --- SKILL -----------------------------------------------------------

    public void gainSkill(int amount) {
        skillCurrent += amount;
        if (skillCurrent > skillInitial) {
            skillCurrent = skillInitial;
        }
    }

    public void loseSkill(int amount) {
        skillCurrent -= amount;
        if (skillCurrent < 0) {
            skillCurrent = 0;
        }
    }

    // --- Resources -------------------------------------------------------

    public void addGold(int amount) { gold += amount; if (gold < 0) gold = 0; }

    public void addProvisions(int amount) { provisions += amount; if (provisions < 0) provisions = 0; }

    /** Adds a provision, or if it is another kind of item, stores it in inventory. */
    public void addItem(Item item) {
        if (item.getType() == ItemType.PROVISION) {
            provisions += Math.max(1, item.getMagnitude() == 0 ? 1 : 1);
        } else {
            inventory.add(item);
        }
    }

    /**
     * Eats one provision if available, restoring {@code healAmount} STAMINA.
     *
     * @return true if a provision was consumed
     */
    public boolean eatProvision(int healAmount) {
        if (provisions <= 0 || !alive) {
            return false;
        }
        provisions--;
        gainStamina(healAmount);
        return true;
    }

    @Override
    public String toString() {
        return name + " [S" + skillCurrent + "/St" + staminaCurrent + "/L" + luckCurrent + "]";
    }
}
