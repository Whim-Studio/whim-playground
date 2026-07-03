package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;

/**
 * The fighting force led by a {@link Captain}. Mutable — the engine adjusts
 * strength, food, posture, order and destination each tick.
 */
public final class ArmyBloc {

    private int strength;           // fighting men
    private int food;               // rations carried
    private Posture posture = Posture.NEUTRAL;
    private CommandType currentOrder = CommandType.MOVE;

    private boolean hasDestination;
    private double destX;
    private double destY;

    public ArmyBloc(int strength, int food) {
        this.strength = strength;
        this.food = food;
    }

    public int strength() { return strength; }
    public int food() { return food; }
    public Posture posture() { return posture; }
    public CommandType currentOrder() { return currentOrder; }

    public boolean hasDestination() { return hasDestination; }
    public double destX() { return destX; }
    public double destY() { return destY; }

    public void setStrength(int strength) { this.strength = strength < 0 ? 0 : strength; }
    public void addStrength(int delta) { setStrength(this.strength + delta); }
    public void setFood(int food) { this.food = food < 0 ? 0 : food; }
    public void addFood(int delta) { setFood(this.food + delta); }
    public void setPosture(Posture posture) { this.posture = posture; }
    public void setCurrentOrder(CommandType currentOrder) { this.currentOrder = currentOrder; }

    public void setDestination(double x, double y) {
        this.destX = x;
        this.destY = y;
        this.hasDestination = true;
    }

    public void clearDestination() {
        this.hasDestination = false;
    }
}
