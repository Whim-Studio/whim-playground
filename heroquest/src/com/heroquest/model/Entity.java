package com.heroquest.model;

import java.awt.Color;

/** Common state and behaviour for anything that occupies a square and fights. */
public abstract class Entity {
    protected final String name;
    protected int bodyPoints;
    protected int maxBodyPoints;
    protected int mindPoints;
    protected int maxMindPoints;
    protected int attackDice;
    protected int defendDice;
    protected Point position;

    protected Entity(String name, int body, int mind, int attackDice, int defendDice) {
        this.name = name;
        this.bodyPoints = body;
        this.maxBodyPoints = body;
        this.mindPoints = mind;
        this.maxMindPoints = mind;
        this.attackDice = attackDice;
        this.defendDice = defendDice;
    }

    public String getName() {
        return name;
    }

    public int getBodyPoints() {
        return bodyPoints;
    }

    public int getMaxBodyPoints() {
        return maxBodyPoints;
    }

    public int getMindPoints() {
        return mindPoints;
    }

    public int getMaxMindPoints() {
        return maxMindPoints;
    }

    public int getAttackDice() {
        return attackDice;
    }

    public int getDefendDice() {
        return defendDice;
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public boolean isAlive() {
        return bodyPoints > 0;
    }

    /** Applies unblocked Skulls as Body Point loss. Returns damage actually taken. */
    public int wound(int amount) {
        int dealt = Math.max(0, amount);
        bodyPoints = Math.max(0, bodyPoints - dealt);
        return dealt;
    }

    public void heal(int amount) {
        bodyPoints = Math.min(maxBodyPoints, bodyPoints + Math.max(0, amount));
    }

    /** Which combat-die face this entity counts as a successful block when defending. */
    public abstract CombatDie defendingShield();

    /** Base movement allowance for this entity's turn. */
    public abstract int baseMovement();

    public abstract Color getColor();

    public abstract String getKindLabel();
}
