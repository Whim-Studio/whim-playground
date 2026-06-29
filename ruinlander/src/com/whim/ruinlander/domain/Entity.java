package com.whim.ruinlander.domain;

/** Data-only interface for anything that occupies a {@link Tile}. */
public interface Entity {
    EntityType getType();
    Position getPosition();
    void setPosition(Position p);

    /** Single Unicode/ASCII char used for rendering, e.g. "R", "M", "☣". */
    String glyph();

    /** Suggested render color (AWT permitted in domain as a render hint only). */
    java.awt.Color color();
}
