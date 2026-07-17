package com.whim.xcom.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.whim.xcom.model.FireMode;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.def.AlienDef;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.FacilityDef;
import com.whim.xcom.rules.def.UfoDef;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * The in-game encyclopedia. Turns the live ruleset content into readable
 * {@link Entry entries} whose descriptions are generated from the data pack, and
 * marks each as unlocked or not against a {@link Campaign}'s research and stores —
 * so entries open up as the player researches tech and captures aliens. This class
 * is pure (no Swing); the UFOpaedia screen renders {@link #unlockedEntries}.
 *
 * <p>Gating is deliberately forgiving so the book always has content: basic-issue
 * gear and your own base facilities are always readable, alien dossiers open once
 * you have autopsied/interrogated a specimen (or, for the general races, once
 * <em>Alien Origins</em> is known), and captured tech opens on the unlocking
 * research or once an example sits in stores.</p>
 */
public final class Ufopaedia {

    private Ufopaedia() {
    }

    public enum Category { WEAPONS, EQUIPMENT, ALIENS, UFOS, FACILITIES }

    /** One encyclopedia article. */
    public static final class Entry {
        private final Category category;
        private final String id;
        private final String title;
        private final String body;
        private final boolean unlocked;

        Entry(Category category, String id, String title, String body, boolean unlocked) {
            this.category = category;
            this.id = id;
            this.title = title;
            this.body = body;
            this.unlocked = unlocked;
        }

        public Category category() { return category; }
        public String id() { return id; }
        public String title() { return title; }
        public String body() { return body; }
        public boolean unlocked() { return unlocked; }
    }

    private static final Set<String> BASIC_WEAPONS = new LinkedHashSet<String>(
            Arrays.asList("pistol", "rifle", "heavy_cannon", "auto_cannon", "stun_rod"));

    /** All entries, each flagged unlocked/locked for the given campaign. */
    public static List<Entry> entries(Ruleset rs, Campaign c) {
        List<Entry> out = new ArrayList<Entry>();
        for (WeaponDef w : rs.weapons()) {
            out.add(new Entry(Category.WEAPONS, w.id(), w.name(), weaponBody(w), weaponUnlocked(w, c)));
        }
        for (ArmorDef a : rs.armors()) {
            if ("none".equals(a.id())) {
                continue; // the jumpsuit needs no dossier
            }
            out.add(new Entry(Category.EQUIPMENT, a.id(), a.name(), armorBody(a), armorUnlocked(a, c)));
        }
        for (AlienDef a : rs.aliens()) {
            out.add(new Entry(Category.ALIENS, a.id(), a.name(), alienBody(a), alienUnlocked(a, c)));
        }
        for (UfoDef u : rs.ufos()) {
            out.add(new Entry(Category.UFOS, u.id(), u.name(), ufoBody(u), true));
        }
        for (FacilityDef f : rs.facilities()) {
            out.add(new Entry(Category.FACILITIES, f.id(), f.name(), facilityBody(f), true));
        }
        return out;
    }

    /** Only the entries currently readable for this campaign. */
    public static List<Entry> unlockedEntries(Ruleset rs, Campaign c) {
        List<Entry> out = new ArrayList<Entry>();
        for (Entry e : entries(rs, c)) {
            if (e.unlocked()) {
                out.add(e);
            }
        }
        return out;
    }

    // ---- gating -------------------------------------------------------------

    private static boolean researched(Campaign c, String id) {
        return c != null && c.completedResearch().contains(id);
    }

    private static boolean inStores(Campaign c, String itemId) {
        if (c == null) {
            return false;
        }
        Integer n = c.stores().get(itemId);
        return n != null && n > 0;
    }

    private static boolean weaponUnlocked(WeaponDef w, Campaign c) {
        String id = w.id();
        if (BASIC_WEAPONS.contains(id)) {
            return true;
        }
        if ("laser_rifle".equals(id) || "laser_pistol".equals(id)) {
            return researched(c, "laser_weapons") || inStores(c, id);
        }
        if ("heavy_plasma".equals(id)) {
            return researched(c, "the_martian_solution") || inStores(c, id);
        }
        return inStores(c, id);
    }

    private static boolean armorUnlocked(ArmorDef a, Campaign c) {
        if ("personal_armor".equals(a.id())) {
            return researched(c, "alien_alloys") || inStores(c, a.id());
        }
        return inStores(c, a.id());
    }

    private static boolean alienUnlocked(AlienDef a, Campaign c) {
        String id = a.id();
        if (inStores(c, "live_" + id)) {
            return true;
        }
        if (id.startsWith("sectoid")) {
            return researched(c, "sectoid_autopsy")
                    || researched(c, "interrogate_sectoid_soldier")
                    || researched(c, "interrogate_sectoid_leader");
        }
        if ("alien_brain".equals(id)) {
            return researched(c, "the_martian_solution") || researched(c, "cydonia_or_bust");
        }
        // General races become known once alien intelligence is understood.
        return researched(c, "alien_origins");
    }

    // ---- description text (generated from the data pack) --------------------

    private static String weaponBody(WeaponDef w) {
        StringBuilder b = new StringBuilder();
        b.append("Power ").append(w.power()).append(" (").append(w.damageType()).append(").  ")
                .append(w.twoHanded() ? "Two-handed." : "One-handed.")
                .append("  Weight ").append(w.weight());
        if (w.clipSize() > 0) {
            b.append(", clip ").append(w.clipSize());
        }
        b.append('.').append('\n');
        for (FireMode m : FireMode.values()) {
            if (w.supports(m)) {
                b.append("  ").append(cap(m.name())).append(": ")
                        .append(w.accuracyPercent(m)).append("% acc, ")
                        .append(w.tuPercent(m)).append("% TU");
                if (w.shots(m) > 1) {
                    b.append(", ").append(w.shots(m)).append(" shots");
                }
                b.append('\n');
            }
        }
        return b.toString();
    }

    private static String armorBody(ArmorDef a) {
        return String.format(
                "Personal protection.%nArmour  front %d / side %d / rear %d / under %d.",
                a.front(), a.side(), a.rear(), a.under());
    }

    private static String alienBody(AlienDef a) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("Health %d, Time Units %d, Reactions %d, Firing %d.%n",
                a.health(), a.timeUnits(), a.reactions(), a.firingAccuracy()));
        b.append(String.format("Strength %d, innate armour %d.", a.strength(), a.frontArmor()));
        if (a.psiStrength() > 0) {
            b.append(String.format("%nPsionic strength %d — can assault a soldier's mind.", a.psiStrength()));
        }
        b.append(String.format("%nCouncil score value when neutralised: %d.", a.scoreValue()));
        return b.toString();
    }

    private static String ufoBody(UfoDef u) {
        return String.format(
                "Hull %d, cruise speed %d.%nCrew %d–%d.  Weapon power %d.%nCrash-site map %d tiles.",
                u.hullPoints(), u.speed(), u.minCrew(), u.maxCrew(), u.weaponPower(), u.mapSize());
    }

    private static String facilityBody(FacilityDef f) {
        StringBuilder b = new StringBuilder();
        b.append(String.format("Build cost $%,d over %d days.  Upkeep $%,d/month.",
                f.buildCostDollars(), f.buildTimeDays(), f.monthlyMaintenanceDollars()));
        if (f.detectionChancePercent() > 0) {
            b.append(String.format("%nRadar: %d%% detection per 30-min tick, range %d.",
                    f.detectionChancePercent(), f.detectionRange()));
        }
        if (f.capacity() > 0) {
            b.append(String.format("%nCapacity: %d.", f.capacity()));
        }
        return b.toString();
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
