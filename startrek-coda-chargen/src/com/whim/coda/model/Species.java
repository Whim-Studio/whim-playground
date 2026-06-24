package com.whim.coda.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A playable species: attribute modifiers, racial abilities, and bonus Courage. */
public class Species {

    private final String name;
    private final Map<Attribute, Integer> mods;
    private final List<Ability> abilities;
    private final int bonusCourage;

    public Species(String name, Map<Attribute, Integer> mods,
                   List<Ability> abilities, int bonusCourage) {
        this.name = name;
        this.mods = new EnumMap<Attribute, Integer>(Attribute.class);
        if (mods != null) {
            this.mods.putAll(mods);
        }
        this.abilities = new ArrayList<Ability>();
        if (abilities != null) {
            this.abilities.addAll(abilities);
        }
        this.bonusCourage = bonusCourage;
    }

    public String getName() {
        return name;
    }

    /** 0 if none. */
    public int getMod(Attribute a) {
        Integer v = mods.get(a);
        return v == null ? 0 : v.intValue();
    }

    public Map<Attribute, Integer> getMods() {
        return Collections.unmodifiableMap(mods);
    }

    public List<Ability> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    /** Bajoran Pagh = 1, else 0. */
    public int getBonusCourage() {
        return bonusCourage;
    }

    @Override
    public String toString() {
        return name;
    }
}
