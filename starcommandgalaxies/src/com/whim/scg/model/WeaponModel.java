package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;

/** Mutable weapon mounted in a WEAPONS room slot. */
public final class WeaponModel implements Views.WeaponView {
    public int slot;
    public String defId;
    public String name;
    public Enums.WeaponType type;
    public int damage;
    public int chargeMax;   // charge ticks required
    public double charge;   // current charge (ticks)
    public int powered;     // power allocated
    public int reqPower = 1; // power required to arm
    public int targetRoomId = -1;
    public int cost;
    public boolean piercesShields;

    public WeaponModel() {}

    @Override public int slot() { return slot; }
    @Override public String name() { return name; }
    @Override public Enums.WeaponType type() { return type; }
    @Override public int damage() { return damage; }
    @Override public int chargeMax() { return chargeMax; }
    @Override public int charge() { return (int) Math.floor(charge); }
    @Override public boolean ready() { return charge >= chargeMax && powered > 0; }
    @Override public int powered() { return powered; }
    @Override public int targetRoomId() { return targetRoomId; }
}
