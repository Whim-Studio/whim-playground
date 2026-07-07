package com.whim.b5wars.model;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Mutable in-play instance built from a {@link ShipClass}. */
public final class Ship {
    private final ShipClass type;
    private final Side side;

    private Hex pos;
    private Facing facing;
    private int speed;
    private int straightHexes;

    private final Map<Facing, Integer> armor;
    private final Map<Section, Integer> structure;

    private int ewOffensive;
    private int ewDefensive;
    private int thrustAvailable;

    private final Map<Integer, Integer> reloadReadyTurn;

    private boolean destroyed;
    private boolean crippled;

    public Ship(ShipClass type, Side side, Hex pos, Facing facing, int speed) {
        this.type = type;
        this.side = side;
        this.pos = pos;
        this.facing = facing;
        this.speed = speed;
        this.straightHexes = 0;

        this.armor = new EnumMap<Facing, Integer>(Facing.class);
        if (type != null) {
            this.armor.putAll(type.getArmor());
        }
        this.structure = new EnumMap<Section, Integer>(Section.class);
        if (type != null) {
            this.structure.putAll(type.getStructure());
        }

        this.thrustAvailable = type == null ? 0 : type.getThrust();
        this.reloadReadyTurn = new HashMap<Integer, Integer>();
    }

    public ShipClass getType() {
        return type;
    }

    public Side getSide() {
        return side;
    }

    public Hex getPos() {
        return pos;
    }

    public void setPos(Hex h) {
        this.pos = h;
    }

    public Facing getFacing() {
        return facing;
    }

    public void setFacing(Facing f) {
        this.facing = f;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int s) {
        this.speed = s;
    }

    /** Hexes travelled straight since the last facing change (for turn-mode checks). */
    public int getStraightHexes() {
        return straightHexes;
    }

    public void setStraightHexes(int n) {
        this.straightHexes = n;
    }

    /** Current defense layer per facing (starts == class armor; SHIELD regenerates each turn). */
    public Map<Facing, Integer> getArmor() {
        return armor;
    }

    /** Current structure remaining per section (starts == class structure). */
    public Map<Section, Integer> getStructure() {
        return structure;
    }

    public int getEwOffensive() {
        return ewOffensive;
    }

    public void setEwOffensive(int n) {
        this.ewOffensive = n;
    }

    public int getEwDefensive() {
        return ewDefensive;
    }

    public void setEwDefensive(int n) {
        this.ewDefensive = n;
    }

    public int getThrustAvailable() {
        return thrustAvailable;
    }

    public void setThrustAvailable(int n) {
        this.thrustAvailable = n;
    }

    /** Weapon i cannot fire until this turn index (0 if never set). */
    public int getReloadReadyTurn(int weaponIndex) {
        Integer v = reloadReadyTurn.get(Integer.valueOf(weaponIndex));
        return v == null ? 0 : v.intValue();
    }

    public void setReloadReadyTurn(int weaponIndex, int turn) {
        reloadReadyTurn.put(Integer.valueOf(weaponIndex), Integer.valueOf(turn));
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean b) {
        this.destroyed = b;
    }

    public boolean isCrippled() {
        return crippled;
    }

    public void setCrippled(boolean b) {
        this.crippled = b;
    }

    /** Sum of current structure across all sections. */
    public int totalStructureRemaining() {
        int total = 0;
        for (Integer v : structure.values()) {
            if (v != null) {
                total += v.intValue();
            }
        }
        return total;
    }
}
