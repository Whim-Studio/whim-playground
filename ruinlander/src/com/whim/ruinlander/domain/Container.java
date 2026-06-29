package com.whim.ruinlander.domain;

import java.awt.Color;

/** A lootable container entity (crate, locker, stash). */
public class Container implements Entity {
    private final Inventory loot;
    private boolean looted;
    private Position position;

    public Container(Position position) {
        this(position, new Inventory(100.0));
    }

    public Container(Position position, Inventory loot) {
        this.position = position;
        this.loot = loot;
    }

    public Inventory getLoot() { return loot; }
    public boolean isLooted() { return looted; }
    public void setLooted(boolean looted) { this.looted = looted; }

    @Override
    public EntityType getType() { return EntityType.CONTAINER; }

    @Override
    public Position getPosition() { return position; }

    @Override
    public void setPosition(Position p) { this.position = p; }

    @Override
    public String glyph() { return looted ? "□" : "▣"; }

    @Override
    public Color color() {
        return looted ? new Color(110, 110, 110) : new Color(210, 180, 110);
    }
}
