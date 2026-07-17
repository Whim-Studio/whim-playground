package com.whim.xcom.rules;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.whim.xcom.rules.def.GameDef;

/**
 * Small insertion-ordered id → def map shared by every content category. Keeps
 * {@link Ruleset} implementations tiny and gives deterministic iteration order
 * (important for reproducible UI listings and tests).
 */
public final class DefRegistry<T extends GameDef> {

    private final String category;
    private final Map<String, T> byId = new LinkedHashMap<String, T>();

    public DefRegistry(String category) {
        this.category = category;
    }

    public void add(T def) {
        if (def == null || def.id() == null) {
            throw new IllegalArgumentException(category + ": def and id must be non-null");
        }
        byId.put(def.id(), def);
    }

    public T get(String id) {
        T def = byId.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown " + category + " id: " + id);
        }
        return def;
    }

    public boolean has(String id) {
        return byId.containsKey(id);
    }

    public Collection<T> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public int size() {
        return byId.size();
    }
}
