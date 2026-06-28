package com.whim.nobunaga.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * One fief (province) on the world map. Holds its economy (gold/rice),
 * governance levers (tax, flood control, cultivation), peasant loyalty, the
 * stationed garrison, ownership, and the set of geographically adjacent
 * provinces (used for war and resource transfer). Layout coords {@code (x,y)}
 * live in a 0..1000 virtual space that the UI scales to the panel.
 */
public final class Province {
    private final int id;
    private final String name;
    private int x;
    private int y;

    private int ownerId = -1;   // daimyo id, or -1 for neutral
    private int gold;
    private int rice;
    private int loyalty = 70;
    private int taxRate = 40;
    private int floodControl = 20;
    private int cultivation = 20;
    private int soldiers;

    private final List<Integer> adjacent = new ArrayList<Integer>();

    public Province(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getRice() {
        return rice;
    }

    public void setRice(int rice) {
        this.rice = rice;
    }

    public int getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(int loyalty) {
        this.loyalty = loyalty;
    }

    public int getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(int taxRate) {
        this.taxRate = taxRate;
    }

    public int getFloodControl() {
        return floodControl;
    }

    public void setFloodControl(int floodControl) {
        this.floodControl = floodControl;
    }

    public int getCultivation() {
        return cultivation;
    }

    public void setCultivation(int cultivation) {
        this.cultivation = cultivation;
    }

    public int getSoldiers() {
        return soldiers;
    }

    public void setSoldiers(int soldiers) {
        this.soldiers = soldiers;
    }

    /** Mutable list of reachable (adjacent) province ids. Populated by ProvinceData. */
    public List<Integer> getAdjacent() {
        return adjacent;
    }

    public boolean isNeutral() {
        return ownerId == -1;
    }
}
