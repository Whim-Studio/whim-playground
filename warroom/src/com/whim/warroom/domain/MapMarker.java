package com.whim.warroom.domain;

import java.awt.Color;

/** A commander-dropped annotation marker on the battlefield. */
public class MapMarker {
    private final Vec2 pos;
    private final String label;
    private final Color color;

    public MapMarker(Vec2 pos, String label, Color color) {
        this.pos = pos;
        this.label = label;
        this.color = color;
    }

    public Vec2 getPos() {
        return pos;
    }

    public String getLabel() {
        return label;
    }

    public Color getColor() {
        return color;
    }
}
