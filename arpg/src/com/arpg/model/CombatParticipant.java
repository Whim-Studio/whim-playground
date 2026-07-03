package com.arpg.model;

/**
 * Anything that can take part in combat: the player {@link Character}, an
 * {@link Enemy}, or a {@link Pet}. This is the shared cross-layer contract the
 * engine and UI code against — the signatures here are binding.
 */
public interface CombatParticipant extends java.io.Serializable {

    String getName();

    int getCurrentHealth();

    int getMaxHealth();

    int getCurrentResource();

    int getMaxResource();

    boolean isAlive();

    /** Reduce current health by {@code amount}; result is clamped to &gt;= 0. */
    void applyDamage(int amount);

    /** Restore health by {@code amount}; result is clamped to &lt;= maxHealth. */
    void applyHealing(int amount);

    java.util.List<Ability> getAbilities();

    java.util.List<BuffDebuff> getActiveBuffs();

    void addBuff(BuffDebuff b);

    void removeBuff(BuffDebuff b);
}
