package com.whim.samurai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A clan on the map. One clan is the player's; the others are neutral or rival
 * daimyo houses competing for territory and, ultimately, the Shogunate.
 * The player begins as a retainer of {@link #daimyoName} (design ref §6).
 */
public class Clan implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public String daimyoName;
    public int colorIndex;
    public boolean isPlayer = false;

    public int honor = 100;
    public int power = 100;

    /** How the AI clan regards the player: -100 (war) .. +100 (allied). */
    public int relationToPlayer = 0;

    public final List<Integer> provinces = new ArrayList<Integer>();

    public Clan() { }
    public Clan(int id, String name, String daimyoName, int colorIndex) {
        this.id = id; this.name = name; this.daimyoName = daimyoName; this.colorIndex = colorIndex;
    }
}
