package com.whim.settlers.economy;

import java.util.EnumMap;
import java.util.Map;

/** A tally of {@link Good}s. Backs the central stockpile and per-building buffers. */
public final class Inventory {

    private final EnumMap<Good, Integer> counts = new EnumMap<Good, Integer>(Good.class);

    public int get(Good g) {
        Integer v = counts.get(g);
        return v == null ? 0 : v;
    }

    public void add(Good g, int n) {
        counts.put(g, get(g) + n);
    }

    public boolean has(Good g, int n) {
        return get(g) >= n;
    }

    /** Remove {@code n} of {@code g} if available; returns true on success. */
    public boolean take(Good g, int n) {
        if (get(g) < n) return false;
        counts.put(g, get(g) - n);
        return true;
    }

    public Map<Good, Integer> snapshot() {
        return new EnumMap<Good, Integer>(counts);
    }

    public int total() {
        int t = 0;
        for (int v : counts.values()) t += v;
        return t;
    }
}
