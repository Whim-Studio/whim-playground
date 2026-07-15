package com.whim.settlers.military;

import java.awt.Color;

/** Player identities and colours for the (currently two-player) game. */
public final class Players {

    public static final int HUMAN = 0;
    public static final int ENEMY = 1;

    private static final Color[] COLORS = {
        new Color(0x4A90D9), // human — blue
        new Color(0xD9534A), // enemy — red
        new Color(0x5AB85A), // spare — green
        new Color(0xD9B84A)  // spare — gold
    };

    private Players() { }

    public static Color color(int id) {
        return COLORS[((id % COLORS.length) + COLORS.length) % COLORS.length];
    }

    public static String name(int id) {
        return id == HUMAN ? "You" : "Enemy " + id;
    }
}
