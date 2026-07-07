package com.whim.b5wars.model;

/** A named special system (interceptors, jump engine, hangar, ...). */
public final class Special {
    private final String id;
    private final String name;
    private final int value;

    public Special(String id, String name, int value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Rating if applicable (e.g. interceptor rating). */
    public int getValue() {
        return value;
    }
}
