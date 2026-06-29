package com.whim.ruinlander.engine;

/** Immutable result of a single combat action (player attack, move, or enemy attack). */
public final class AttackOutcome {

    private final boolean hit;
    private final int damage;
    private final boolean targetKilled;
    private final String message;

    public AttackOutcome(boolean hit, int damage, boolean targetKilled, String message) {
        this.hit = hit;
        this.damage = damage;
        this.targetKilled = targetKilled;
        this.message = message;
    }

    public boolean isHit() {
        return hit;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isTargetKilled() {
        return targetKilled;
    }

    public String getMessage() {
        return message;
    }
}
