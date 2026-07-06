package com.whim.starcommand.engine;

import com.whim.starcommand.model.Character;

/**
 * Character generation. Stats are rolled 3d6 (range 3..18) then a chosen role
 * nudges the relevant aptitude, mirroring how the original's training schools
 * bias a character toward a specialty.
 */
public class CharacterGen {

    private final Rng rng;

    public CharacterGen(Rng rng) { this.rng = rng; }

    /** Roll a fresh character with the given name and role. */
    public Character roll(String name, String role) {
        Character c = new Character();
        c.name = (name == null || name.trim().isEmpty()) ? randomName() : name.trim();
        c.role = role;
        c.strength   = rng.roll(3, 6);
        c.speed      = rng.roll(3, 6);
        c.accuracy   = rng.roll(3, 6);
        c.intellect  = rng.roll(3, 6);
        c.leadership = rng.roll(3, 6);
        c.willpower  = rng.roll(3, 6);
        applyRoleBias(c);
        c.deriveHp();
        return c;
    }

    private void applyRoleBias(Character c) {
        if ("Marine".equals(c.role))      { c.strength += 2; c.accuracy += 2; }
        else if ("Pilot".equals(c.role))  { c.speed += 2; c.accuracy += 1; }
        else if ("Esper".equals(c.role))  { c.willpower += 3; c.intellect += 1; }
        else if ("Medic".equals(c.role))  { c.intellect += 2; c.willpower += 1; }
        else if ("Engineer".equals(c.role)){ c.intellect += 3; }
        else if ("Scout".equals(c.role))  { c.speed += 2; c.leadership += 1; }
        cap(c);
    }

    private void cap(Character c) {
        c.strength = Math.min(20, c.strength);
        c.speed = Math.min(20, c.speed);
        c.accuracy = Math.min(20, c.accuracy);
        c.intellect = Math.min(20, c.intellect);
        c.leadership = Math.min(20, c.leadership);
        c.willpower = Math.min(20, c.willpower);
    }

    private static final String[] FIRST = {
            "Dax", "Ryn", "Vex", "Kael", "Mira", "Sol", "Tarn", "Ivo",
            "Nova", "Corin", "Zara", "Bran", "Lyle", "Esa", "Orin", "Juno"
    };
    private static final String[] LAST = {
            "Cole", "Vance", "Idris", "Okonkwo", "Reyes", "Salt", "Vaughn",
            "Marsh", "Kade", "Pell", "Voss", "Ash", "Quinn", "Doric"
    };

    public String randomName() {
        return FIRST[rng.nextInt(FIRST.length)] + " " + LAST[rng.nextInt(LAST.length)];
    }
}
