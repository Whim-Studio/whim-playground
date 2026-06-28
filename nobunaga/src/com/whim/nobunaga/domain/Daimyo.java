package com.whim.nobunaga.domain;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * A warlord (daimyo). Owns a set of provinces, has a banner {@link Color} and
 * abbreviation used everywhere on the map, and ages/health that the event and
 * AI engines read. The chosen daimyo has {@link #isPlayer()} set true.
 */
public final class Daimyo {
    private final int id;
    private final String name;
    private final String abbrev;
    private final Color color;

    private int age;
    private int health = 90;
    private boolean player;

    private final List<Integer> provinceIds = new ArrayList<Integer>();

    public Daimyo(int id, String name, String abbrev, Color color, int age, int health) {
        this.id = id;
        this.name = name;
        this.abbrev = abbrev;
        this.color = color;
        this.age = age;
        this.health = health;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public Color getColor() {
        return color;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isPlayer() {
        return player;
    }

    public void setPlayer(boolean player) {
        this.player = player;
    }

    /** Mutable list of owned province ids. Kept consistent with Province.ownerId. */
    public List<Integer> getProvinceIds() {
        return provinceIds;
    }

    public boolean isAlive() {
        return health > 0 && !provinceIds.isEmpty();
    }
}
