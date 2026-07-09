package com.whim.samurai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A province node on the strategic map. Owned by a clan; produces rice income
 * and holds a garrison that must be beaten in the open-field BATTLE sequence to
 * conquer it (design ref §2b, §6).
 */
public class Province implements Serializable {
    private static final long serialVersionUID = 1L;

    public int id;
    public String name;
    public int x, y;                 // map pixel coordinates (drawn procedurally)
    public int ownerClanId;
    public int rice = 100;           // seasonal income potential
    public int garrison = 40;        // defenders' strength for BATTLE
    public int development = 1;      // 1..5, raised by improving the estate
    public boolean fortified = false;
    public final List<Integer> neighbors = new ArrayList<Integer>();

    public Province() { }
    public Province(int id, String name, int x, int y) {
        this.id = id; this.name = name; this.x = x; this.y = y;
    }
}
