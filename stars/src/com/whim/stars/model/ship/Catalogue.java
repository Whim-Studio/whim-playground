package com.whim.stars.model.ship;

import com.whim.stars.model.TechField;

/**
 * A small starting catalogue of hulls and components — enough to design the
 * ships the demo and self-tests need (scout, colonizer, warship, freighter,
 * starbase). The full Stars! component tables are later-phase data; every entry
 * here is a representative, clearly-simplified sample.
 */
public final class Catalogue {

    private Catalogue() {
    }

    // --- Engines ---
    public static final Component QUICK_JUMP_5 = Component.builder("Quick Jump 5", ComponentCategory.ENGINE)
            .cost(3, 0, 1, 3).mass(4).engine(5, 0).tech(TechField.PROPULSION, 0).build();
    public static final Component LONG_HUMP_6 = Component.builder("Long Hump 6", ComponentCategory.ENGINE)
            .cost(5, 0, 1, 6).mass(9).engine(6, 0).tech(TechField.PROPULSION, 3).build();

    // --- Armor / shields ---
    public static final Component TRITANIUM = Component.builder("Tritanium", ComponentCategory.ARMOR)
            .cost(5, 0, 0, 9).mass(60).armor(50).tech(TechField.CONSTRUCTION, 0).build();
    public static final Component MOLE_SKIN = Component.builder("Mole-skin Shield", ComponentCategory.SHIELD)
            .cost(1, 0, 1, 4).mass(1).shield(25).tech(TechField.ENERGY, 0).build();

    // --- Weapons ---
    public static final Component LASER = Component.builder("Laser", ComponentCategory.WEAPON)
            .cost(0, 6, 0, 5).mass(1).weapon(10, 1).tech(TechField.WEAPONS, 0).build();
    public static final Component X_RAY = Component.builder("X-Ray Laser", ComponentCategory.WEAPON)
            .cost(0, 6, 0, 6).mass(1).weapon(16, 1).tech(TechField.WEAPONS, 3).build();

    // --- Utility ---
    public static final Component BAT_SCANNER = Component.builder("Bat Scanner", ComponentCategory.SCANNER)
            .cost(1, 0, 1, 1).mass(2).scan(50).tech(TechField.ELECTRONICS, 0).build();
    public static final Component FUEL_TANK = Component.builder("Fuel Tank", ComponentCategory.MECHANICAL)
            .cost(6, 0, 0, 4).mass(3).fuel(250).tech(TechField.CONSTRUCTION, 0).build();
    public static final Component COLONIZATION_MODULE = Component.builder("Colonization Module", ComponentCategory.MECHANICAL)
            .cost(12, 10, 10, 10).mass(32).tech(TechField.CONSTRUCTION, 0).build();
    public static final Component CARGO_POD = Component.builder("Cargo Pod", ComponentCategory.MECHANICAL)
            .cost(5, 0, 2, 10).mass(5).cargo(50).tech(TechField.CONSTRUCTION, 3).build();

    // --- Hulls ---
    public static HullType scoutHull() {
        return HullType.builder("Scout").mass(8).fuel(50).cost(4, 2, 4, 10)
                .tech(TechField.CONSTRUCTION, 0)
                .slot(ComponentCategory.ENGINE, 1)
                .slot(ComponentCategory.SCANNER, 1)
                .slot(ComponentCategory.GENERAL, 1)
                .build();
    }

    public static HullType colonyHull() {
        return HullType.builder("Colony Ship").mass(20).fuel(200).cost(10, 2, 8, 24)
                .tech(TechField.CONSTRUCTION, 0)
                .slot(ComponentCategory.ENGINE, 1)
                .slot(ComponentCategory.MECHANICAL, 1)
                .build();
    }

    public static HullType frigateHull() {
        return HullType.builder("Frigate").mass(8).armor(45).fuel(125).cost(4, 2, 4, 12)
                .tech(TechField.CONSTRUCTION, 2)
                .slot(ComponentCategory.ENGINE, 1)
                .slot(ComponentCategory.WEAPON, 1)
                .slot(ComponentCategory.SHIELD, 1)
                .build();
    }

    public static HullType destroyerHull() {
        return HullType.builder("Destroyer").mass(30).armor(200).fuel(280).cost(15, 3, 5, 35)
                .tech(TechField.CONSTRUCTION, 3)
                .slot(ComponentCategory.ENGINE, 1)
                .slot(ComponentCategory.WEAPON, 2)
                .slot(ComponentCategory.ARMOR, 1)
                .slot(ComponentCategory.SHIELD, 1)
                .slot(ComponentCategory.ELECTRONICS, 1)
                .build();
    }

    public static HullType smallFreighterHull() {
        return HullType.builder("Small Freighter").mass(25).fuel(130).cargo(70).cost(12, 0, 17, 20)
                .tech(TechField.CONSTRUCTION, 0)
                .slot(ComponentCategory.ENGINE, 1)
                .slot(ComponentCategory.MECHANICAL, 1)
                .build();
    }

    public static HullType spaceStationHull() {
        return HullType.builder("Space Station").mass(0).armor(500).cost(0, 0, 0, 0)
                .tech(TechField.CONSTRUCTION, 0).starbase(true)
                .slot(ComponentCategory.WEAPON, 4)
                .slot(ComponentCategory.SHIELD, 2)
                .slot(ComponentCategory.ORBITAL, 1)
                .build();
    }

    // --- Ready-made designs ---
    public static ShipDesign scoutDesign() {
        ShipDesign d = new ShipDesign("Scout", scoutHull());
        d.place(0, QUICK_JUMP_5);
        d.place(1, BAT_SCANNER);
        return d;
    }

    public static ShipDesign colonyDesign() {
        ShipDesign d = new ShipDesign("Santa Maria", colonyHull());
        d.place(0, QUICK_JUMP_5);
        d.place(1, COLONIZATION_MODULE);
        return d;
    }

    public static ShipDesign frigateDesign() {
        ShipDesign d = new ShipDesign("Stalwart", frigateHull());
        d.place(0, QUICK_JUMP_5);
        d.place(1, LASER);
        d.place(2, MOLE_SKIN);
        return d;
    }

    /** Fresh instances of every catalogue hull — for the ship-design screen. */
    public static java.util.List<HullType> hulls() {
        return java.util.Arrays.asList(
                scoutHull(), colonyHull(), frigateHull(), destroyerHull(),
                smallFreighterHull(), spaceStationHull());
    }

    /** Every catalogue component — for populating slot pickers. */
    public static java.util.List<Component> components() {
        return java.util.Arrays.asList(
                QUICK_JUMP_5, LONG_HUMP_6, TRITANIUM, MOLE_SKIN, LASER, X_RAY,
                BAT_SCANNER, FUEL_TANK, COLONIZATION_MODULE, CARGO_POD);
    }
}
