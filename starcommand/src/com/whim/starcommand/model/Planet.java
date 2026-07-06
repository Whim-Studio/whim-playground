package com.whim.starcommand.model;

import java.io.Serializable;

/** A scannable body sitting in a sector. */
public class Planet implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind { STARPORT, WORLD, PIRATE_BASE, HIVE, DERELICT, EMPTY }

    public String name;
    public Kind kind;
    public boolean scanned = false;
    public String scanReport = "";

    public Planet() { }

    public Planet(String name, Kind kind) {
        this.name = name;
        this.kind = kind;
    }
}
