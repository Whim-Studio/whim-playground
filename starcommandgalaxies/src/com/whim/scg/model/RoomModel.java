package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.GridPos;
import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable ship room / system. */
public final class RoomModel implements Views.RoomView {
    public int id;
    public Enums.RoomType type;
    public GridPos origin;
    public int w, h;
    public int power;
    public int maxPower;
    public int hp;
    public int maxHp;
    public boolean onFire;
    public boolean breached;
    /** Fire lifetime accumulator (seconds). */
    public double fireTime;
    /** Fractional hp-damage accumulator (fire/breach apply sub-integer damage). */
    public double dmgAccum;
    public final List<Integer> crewIds = new ArrayList<Integer>();

    public RoomModel() {}

    public RoomModel(int id, Enums.RoomType type, GridPos origin, int w, int h, int maxPower, int maxHp) {
        this.id = id; this.type = type; this.origin = origin;
        this.w = w; this.h = h; this.maxPower = maxPower;
        this.maxHp = maxHp; this.hp = maxHp;
    }

    /** True when the system is intact enough and has power to function. */
    public boolean operational() { return hp > 0 && power > 0; }

    @Override public int id() { return id; }
    @Override public Enums.RoomType type() { return type; }
    @Override public GridPos origin() { return origin; }
    @Override public int w() { return w; }
    @Override public int h() { return h; }
    @Override public int power() { return power; }
    @Override public int maxPower() { return maxPower; }
    @Override public int hp() { return hp; }
    @Override public int maxHp() { return maxHp; }
    @Override public boolean onFire() { return onFire; }
    @Override public boolean breached() { return breached; }
    @Override public List<Integer> crewIds() { return crewIds; }

    @Override public boolean contains(GridPos p) {
        if (p == null || origin == null) return false;
        return p.x >= origin.x && p.x < origin.x + w && p.y >= origin.y && p.y < origin.y + h;
    }
}
