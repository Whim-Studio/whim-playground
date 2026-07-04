package com.rampart.model;

/**
 * Read-only snapshot of an enemy ship. Concrete class: {@link Ship}. Position is
 * expressed as sub-cell floating-point coordinates so the UI can render smooth
 * motion between grid cells.
 */
public interface ShipView {
    /** @return the ship's class */
    ShipType type();

    /** @return sub-cell column position (fractional x, in cell units) */
    double x();

    /** @return sub-cell row position (fractional y, in cell units) */
    double y();

    /** @return current hit points remaining */
    int health();

    /** @return the direction the ship is currently heading */
    Direction heading();

    /** @return {@code true} while the ship is afloat (not yet sunk) */
    boolean alive();
}
