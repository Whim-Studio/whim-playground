package com.whim.ruinlander.domain;

import java.awt.Color;

/** A settlement entity belonging to a {@link Faction}. Reputation lives on Player. */
public class Settlement implements Entity {
    private final String name;
    private final Faction faction;
    private Position position;

    public Settlement(String name, Faction faction, Position position) {
        this.name = name;
        this.faction = faction;
        this.position = position;
    }

    public String getName() { return name; }
    public Faction getFaction() { return faction; }

    @Override
    public EntityType getType() { return EntityType.SETTLEMENT; }

    @Override
    public Position getPosition() { return position; }

    @Override
    public void setPosition(Position p) { this.position = p; }

    @Override
    public String glyph() { return "⌂"; }

    @Override
    public Color color() {
        switch (faction) {
            case SCAVENGERS: return new Color(200, 170, 90);
            case ENCLAVE: return new Color(90, 150, 220);
            case RAIDERS: return new Color(200, 70, 70);
            default: return new Color(220, 220, 220);
        }
    }
}
