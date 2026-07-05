package com.whim.oggalaxy.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete static game database: every building, technology, ship, defense and
 * player class with its numbers. This is the orchestrator-owned single source of
 * truth. Both the simulation and the UI read {@link #standard()} so a value shown on
 * screen is exactly the value the engine uses.
 *
 * NUMBERS: base costs, combat stats and rapid-fire follow the classic OGame family
 * (well documented) since OG Galaxy is explicitly modelled on OGame. Values unique to
 * OG Galaxy (the LEVIATHAN flagship, class bonuses, deterministic-combat tunables) are
 * clearly-labelled approximations — see the Known Limitations section of the delivery.
 */
public final class Catalog implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Ids.BuildingType, BuildingDef> buildings;
    private final Map<Ids.TechType, TechDef> techs;
    private final Map<Ids.ShipType, ShipDef> ships;
    private final Map<Ids.DefenseType, DefenseDef> defenses;
    private final Map<Ids.PlayerClass, ClassDef> classes;

    private Catalog(Map<Ids.BuildingType, BuildingDef> b, Map<Ids.TechType, TechDef> t,
                    Map<Ids.ShipType, ShipDef> s, Map<Ids.DefenseType, DefenseDef> d,
                    Map<Ids.PlayerClass, ClassDef> c) {
        buildings = Collections.unmodifiableMap(b);
        techs = Collections.unmodifiableMap(t);
        ships = Collections.unmodifiableMap(s);
        defenses = Collections.unmodifiableMap(d);
        classes = Collections.unmodifiableMap(c);
    }

    public BuildingDef building(Ids.BuildingType t) { return buildings.get(t); }
    public TechDef tech(Ids.TechType t) { return techs.get(t); }
    public ShipDef ship(Ids.ShipType t) { return ships.get(t); }
    public DefenseDef defense(Ids.DefenseType t) { return defenses.get(t); }
    public ClassDef playerClass(Ids.PlayerClass t) { return classes.get(t); }

    public Map<Ids.BuildingType, BuildingDef> buildings() { return buildings; }
    public Map<Ids.TechType, TechDef> techs() { return techs; }
    public Map<Ids.ShipType, ShipDef> ships() { return ships; }
    public Map<Ids.DefenseType, DefenseDef> defenses() { return defenses; }
    public Map<Ids.PlayerClass, ClassDef> classes() { return classes; }

    // ------------------------------------------------------------------
    // small construction helpers
    // ------------------------------------------------------------------
    private static List<Requirement> reqs(Requirement... r) {
        return new ArrayList<Requirement>(Arrays.asList(r));
    }

    private static Map<Ids.ShipType, Integer> rf(Object... pairs) {
        Map<Ids.ShipType, Integer> m = new HashMap<Ids.ShipType, Integer>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Ids.ShipType) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    private static Map<Ids.DefenseType, Integer> rfd(Object... pairs) {
        Map<Ids.DefenseType, Integer> m = new HashMap<Ids.DefenseType, Integer>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Ids.DefenseType) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    // ------------------------------------------------------------------
    // THE standard game database
    // ------------------------------------------------------------------
    private static Catalog STANDARD;

    public static synchronized Catalog standard() {
        if (STANDARD == null) {
            STANDARD = build();
        }
        return STANDARD;
    }

    private static Catalog build() {
        Map<Ids.BuildingType, BuildingDef> b = new EnumMap<Ids.BuildingType, BuildingDef>(Ids.BuildingType.class);
        Map<Ids.TechType, TechDef> t = new EnumMap<Ids.TechType, TechDef>(Ids.TechType.class);
        Map<Ids.ShipType, ShipDef> s = new EnumMap<Ids.ShipType, ShipDef>(Ids.ShipType.class);
        Map<Ids.DefenseType, DefenseDef> d = new EnumMap<Ids.DefenseType, DefenseDef>(Ids.DefenseType.class);
        Map<Ids.PlayerClass, ClassDef> c = new EnumMap<Ids.PlayerClass, ClassDef>(Ids.PlayerClass.class);

        // ---------------- Buildings ----------------
        // roleBase meaning: mines -> per-level production base; solar/fusion -> energy base;
        // stores -> unused (capacity is by Formulas.storageCapacity); facilities -> unused.
        b.put(Ids.BuildingType.METAL_MINE, new BuildingDef(Ids.BuildingType.METAL_MINE,
                "Metal Mine", "Extracts metal, the backbone of every fleet and structure.",
                new Cost(60, 15, 0), 1.5, 30, 0, false, null));
        b.put(Ids.BuildingType.CRYSTAL_MINE, new BuildingDef(Ids.BuildingType.CRYSTAL_MINE,
                "Crystal Mine", "Mines crystal, needed for electronics, drives and advanced ships.",
                new Cost(48, 24, 0), 1.6, 20, 0, false, null));
        b.put(Ids.BuildingType.DEUTERIUM_SYNTHESIZER, new BuildingDef(Ids.BuildingType.DEUTERIUM_SYNTHESIZER,
                "Deuterium Synthesizer", "Synthesizes deuterium fuel; output rises on colder worlds.",
                new Cost(225, 75, 0), 1.5, 10, 0, false, null));
        b.put(Ids.BuildingType.SOLAR_PLANT, new BuildingDef(Ids.BuildingType.SOLAR_PLANT,
                "Solar Plant", "Generates energy to power your mines. Free but capped by level.",
                new Cost(75, 30, 0), 1.5, 20, 0, false, null));
        b.put(Ids.BuildingType.FUSION_REACTOR, new BuildingDef(Ids.BuildingType.FUSION_REACTOR,
                "Fusion Reactor", "Burns deuterium for large amounts of energy; scales with Energy Tech.",
                new Cost(900, 360, 180), 1.8, 30, 0, false,
                reqs(Requirement.of(Ids.BuildingType.DEUTERIUM_SYNTHESIZER, 5),
                        Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 3))));
        b.put(Ids.BuildingType.METAL_STORAGE, new BuildingDef(Ids.BuildingType.METAL_STORAGE,
                "Metal Storage", "Increases the maximum metal your planet can hold.",
                new Cost(1000, 0, 0), 2.0, 0, 0, false, null));
        b.put(Ids.BuildingType.CRYSTAL_STORAGE, new BuildingDef(Ids.BuildingType.CRYSTAL_STORAGE,
                "Crystal Storage", "Increases the maximum crystal your planet can hold.",
                new Cost(1000, 500, 0), 2.0, 0, 0, false, null));
        b.put(Ids.BuildingType.DEUTERIUM_TANK, new BuildingDef(Ids.BuildingType.DEUTERIUM_TANK,
                "Deuterium Tank", "Increases the maximum deuterium your planet can hold.",
                new Cost(1000, 1000, 0), 2.0, 0, 0, false, null));
        b.put(Ids.BuildingType.ROBOTICS_FACTORY, new BuildingDef(Ids.BuildingType.ROBOTICS_FACTORY,
                "Robotics Factory", "Speeds up building and shipyard construction.",
                new Cost(400, 120, 200), 2.0, 0, 0, false, null));
        b.put(Ids.BuildingType.SHIPYARD, new BuildingDef(Ids.BuildingType.SHIPYARD,
                "Shipyard", "Builds ships and defenses. Higher levels build faster.",
                new Cost(400, 200, 100), 2.0, 0, 0, false,
                reqs(Requirement.of(Ids.BuildingType.ROBOTICS_FACTORY, 2))));
        b.put(Ids.BuildingType.RESEARCH_LAB, new BuildingDef(Ids.BuildingType.RESEARCH_LAB,
                "Research Lab", "Enables and accelerates technology research.",
                new Cost(200, 400, 200), 2.0, 0, 0, false, null));
        b.put(Ids.BuildingType.NANITE_FACTORY, new BuildingDef(Ids.BuildingType.NANITE_FACTORY,
                "Nanite Factory", "Halves construction time for every level. Extremely expensive.",
                new Cost(1000000, 500000, 100000), 2.0, 0, 0, false,
                reqs(Requirement.of(Ids.BuildingType.ROBOTICS_FACTORY, 10),
                        Requirement.of(Ids.TechType.COMPUTER_TECHNOLOGY, 10))));
        b.put(Ids.BuildingType.TERRAFORMER, new BuildingDef(Ids.BuildingType.TERRAFORMER,
                "Terraformer", "Adds usable building fields to a planet.",
                new Cost(0, 50000, 100000, 1000), 2.0, 0, 0, false,
                reqs(Requirement.of(Ids.BuildingType.NANITE_FACTORY, 1))));
        b.put(Ids.BuildingType.MISSILE_SILO, new BuildingDef(Ids.BuildingType.MISSILE_SILO,
                "Missile Silo", "Stores interplanetary and anti-ballistic missiles.",
                new Cost(20000, 20000, 1000), 2.0, 0, 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 1))));
        b.put(Ids.BuildingType.LUNAR_BASE, new BuildingDef(Ids.BuildingType.LUNAR_BASE,
                "Lunar Base", "Adds fields to a moon so other moon buildings can be built.",
                new Cost(20000, 40000, 20000), 2.0, 0, 0, true, null));
        b.put(Ids.BuildingType.SENSOR_PHALANX, new BuildingDef(Ids.BuildingType.SENSOR_PHALANX,
                "Sensor Phalanx", "Scans enemy fleet movements from a moon.",
                new Cost(20000, 40000, 20000), 2.0, 0, 0, true,
                reqs(Requirement.of(Ids.BuildingType.LUNAR_BASE, 1))));
        b.put(Ids.BuildingType.JUMP_GATE, new BuildingDef(Ids.BuildingType.JUMP_GATE,
                "Jump Gate", "Instantly teleports fleets between two moons.",
                new Cost(2000000, 4000000, 2000000), 2.0, 0, 1, true,
                reqs(Requirement.of(Ids.BuildingType.LUNAR_BASE, 1),
                        Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 7))));

        // ---------------- Technologies ----------------
        t.put(Ids.TechType.ENERGY_TECHNOLOGY, new TechDef(Ids.TechType.ENERGY_TECHNOLOGY,
                "Energy Technology", "Improves energy output and unlocks advanced tech.",
                new Cost(0, 800, 400), 2.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 1))));
        t.put(Ids.TechType.LASER_TECHNOLOGY, new TechDef(Ids.TechType.LASER_TECHNOLOGY,
                "Laser Technology", "Foundational weapon tech for light/heavy lasers and cruisers.",
                new Cost(200, 100, 0), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 2),
                        Requirement.of(Ids.BuildingType.RESEARCH_LAB, 1))));
        t.put(Ids.TechType.ION_TECHNOLOGY, new TechDef(Ids.TechType.ION_TECHNOLOGY,
                "Ion Technology", "Enables ion cannons and reduces deconstruction cost.",
                new Cost(1000, 300, 100), 2.0, 0, reqs(Requirement.of(Ids.TechType.LASER_TECHNOLOGY, 5),
                        Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 4))));
        t.put(Ids.TechType.HYPERSPACE_TECHNOLOGY, new TechDef(Ids.TechType.HYPERSPACE_TECHNOLOGY,
                "Hyperspace Technology", "Gateway to hyperspace drives, big hulls and jump gates.",
                new Cost(0, 4000, 2000), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 5),
                        Requirement.of(Ids.TechType.ION_TECHNOLOGY, 5))));
        t.put(Ids.TechType.PLASMA_TECHNOLOGY, new TechDef(Ids.TechType.PLASMA_TECHNOLOGY,
                "Plasma Technology", "Unlocks plasma turrets and boosts mine output.",
                new Cost(2000, 4000, 1000), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 8),
                        Requirement.of(Ids.TechType.LASER_TECHNOLOGY, 10),
                        Requirement.of(Ids.TechType.ION_TECHNOLOGY, 5))));
        t.put(Ids.TechType.COMBUSTION_DRIVE, new TechDef(Ids.TechType.COMBUSTION_DRIVE,
                "Combustion Drive", "Basic sublight drive; +10% speed per level for combustion ships.",
                new Cost(400, 0, 600), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 1),
                        Requirement.of(Ids.BuildingType.RESEARCH_LAB, 1))));
        t.put(Ids.TechType.IMPULSE_DRIVE, new TechDef(Ids.TechType.IMPULSE_DRIVE,
                "Impulse Drive", "Mid-tier drive; +20% speed per level for impulse ships.",
                new Cost(2000, 4000, 600), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 1))));
        t.put(Ids.TechType.HYPERSPACE_DRIVE, new TechDef(Ids.TechType.HYPERSPACE_DRIVE,
                "Hyperspace Drive", "Top-tier drive for capital ships; +30% speed per level.",
                new Cost(10000, 20000, 6000), 2.0, 0, reqs(Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 3))));
        t.put(Ids.TechType.ESPIONAGE_TECHNOLOGY, new TechDef(Ids.TechType.ESPIONAGE_TECHNOLOGY,
                "Espionage Technology", "Better probe reports and counter-espionage.",
                new Cost(200, 1000, 200), 2.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 3))));
        t.put(Ids.TechType.COMPUTER_TECHNOLOGY, new TechDef(Ids.TechType.COMPUTER_TECHNOLOGY,
                "Computer Technology", "Each level adds one more simultaneous fleet slot.",
                new Cost(0, 400, 600), 2.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 1))));
        t.put(Ids.TechType.ASTROPHYSICS, new TechDef(Ids.TechType.ASTROPHYSICS,
                "Astrophysics", "Enables expeditions and unlocks new colony slots (ceil(level/2)).",
                new Cost(4000, 8000, 4000), 1.75, 0, reqs(Requirement.of(Ids.TechType.ESPIONAGE_TECHNOLOGY, 4),
                        Requirement.of(Ids.TechType.IMPULSE_DRIVE, 3))));
        t.put(Ids.TechType.GRAVITON_TECHNOLOGY, new TechDef(Ids.TechType.GRAVITON_TECHNOLOGY,
                "Graviton Technology", "Enormous energy cost; unlocks the Death Star.",
                new Cost(0, 0, 0, 300000), 3.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 12))));
        t.put(Ids.TechType.WEAPONS_TECHNOLOGY, new TechDef(Ids.TechType.WEAPONS_TECHNOLOGY,
                "Weapons Technology", "+10% weapon power for all ships and defenses per level.",
                new Cost(800, 200, 0), 2.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 4))));
        t.put(Ids.TechType.SHIELDING_TECHNOLOGY, new TechDef(Ids.TechType.SHIELDING_TECHNOLOGY,
                "Shielding Technology", "+10% shield strength for all ships and defenses per level.",
                new Cost(200, 600, 0), 2.0, 0, reqs(Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 3),
                        Requirement.of(Ids.BuildingType.RESEARCH_LAB, 6))));
        t.put(Ids.TechType.ARMOUR_TECHNOLOGY, new TechDef(Ids.TechType.ARMOUR_TECHNOLOGY,
                "Armour Technology", "+10% hull strength for all ships and defenses per level.",
                new Cost(1000, 0, 0), 2.0, 0, reqs(Requirement.of(Ids.BuildingType.RESEARCH_LAB, 2))));

        // ---------------- Ships ----------------
        // Rapid-fire tables use classic OGame values. Civil ships flagged true.
        Map<Ids.ShipType, Integer> cargoRf = rf(Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5);
        s.put(Ids.ShipType.SMALL_CARGO, new ShipDef(Ids.ShipType.SMALL_CARGO,
                "Small Cargo", "Fast hauler for moving resources between planets.",
                5, 10, 4000, 5000, 5000, 10, new Cost(2000, 2000, 0), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 2), Requirement.of(Ids.TechType.COMBUSTION_DRIVE, 2)),
                cargoRf, null));
        s.put(Ids.ShipType.LARGE_CARGO, new ShipDef(Ids.ShipType.LARGE_CARGO,
                "Large Cargo", "Bulk freighter with five times the hold of a small cargo.",
                5, 25, 12000, 25000, 7500, 50, new Cost(6000, 6000, 0), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 4), Requirement.of(Ids.TechType.COMBUSTION_DRIVE, 6)),
                cargoRf, null));
        s.put(Ids.ShipType.LIGHT_FIGHTER, new ShipDef(Ids.ShipType.LIGHT_FIGHTER,
                "Light Fighter", "Cheap, fast interceptor. Strong in numbers, fragile alone.",
                50, 10, 4000, 50, 12500, 20, new Cost(3000, 1000, 0), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 1), Requirement.of(Ids.TechType.COMBUSTION_DRIVE, 1)),
                null, null));
        s.put(Ids.ShipType.HEAVY_FIGHTER, new ShipDef(Ids.ShipType.HEAVY_FIGHTER,
                "Heavy Fighter", "Armoured fighter with rapid fire against cargoes and probes.",
                150, 25, 10000, 100, 10000, 75, new Cost(6000, 4000, 0), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 3), Requirement.of(Ids.TechType.ARMOUR_TECHNOLOGY, 2),
                        Requirement.of(Ids.TechType.IMPULSE_DRIVE, 2)),
                rf(Ids.ShipType.SMALL_CARGO, 3, Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5), null));
        s.put(Ids.ShipType.CRUISER, new ShipDef(Ids.ShipType.CRUISER,
                "Cruiser", "Balanced warship with rapid fire against light fighters and rockets.",
                400, 50, 27000, 800, 15000, 300, new Cost(20000, 7000, 2000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 5), Requirement.of(Ids.TechType.IMPULSE_DRIVE, 4),
                        Requirement.of(Ids.TechType.ION_TECHNOLOGY, 2)),
                rf(Ids.ShipType.LIGHT_FIGHTER, 6, Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5),
                rfd(Ids.DefenseType.ROCKET_LAUNCHER, 10)));
        s.put(Ids.ShipType.BATTLESHIP, new ShipDef(Ids.ShipType.BATTLESHIP,
                "Battleship", "Backbone capital ship. High hull, no rapid fire weaknesses.",
                1000, 200, 60000, 1500, 10000, 500, new Cost(45000, 15000, 0), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 7), Requirement.of(Ids.TechType.HYPERSPACE_DRIVE, 4)),
                rf(Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5), null));
        s.put(Ids.ShipType.BATTLECRUISER, new ShipDef(Ids.ShipType.BATTLECRUISER,
                "Battlecruiser", "Fast raider with heavy rapid fire against most warships and cargoes.",
                700, 400, 70000, 750, 10000, 250, new Cost(30000, 40000, 15000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 8), Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 5),
                        Requirement.of(Ids.TechType.LASER_TECHNOLOGY, 12)),
                rf(Ids.ShipType.SMALL_CARGO, 3, Ids.ShipType.LARGE_CARGO, 3, Ids.ShipType.HEAVY_FIGHTER, 4,
                        Ids.ShipType.CRUISER, 4, Ids.ShipType.BATTLESHIP, 7,
                        Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5), null));
        s.put(Ids.ShipType.BOMBER, new ShipDef(Ids.ShipType.BOMBER,
                "Bomber", "Siege ship with devastating rapid fire against planetary defenses.",
                1000, 500, 75000, 500, 4000, 1000, new Cost(50000, 25000, 15000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 8), Requirement.of(Ids.TechType.PLASMA_TECHNOLOGY, 5),
                        Requirement.of(Ids.TechType.IMPULSE_DRIVE, 6)),
                rf(Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5),
                rfd(Ids.DefenseType.ROCKET_LAUNCHER, 20, Ids.DefenseType.LIGHT_LASER, 20,
                        Ids.DefenseType.HEAVY_LASER, 10, Ids.DefenseType.ION_CANNON, 10,
                        Ids.DefenseType.GAUSS_CANNON, 5, Ids.DefenseType.PLASMA_TURRET, 5)));
        s.put(Ids.ShipType.DESTROYER, new ShipDef(Ids.ShipType.DESTROYER,
                "Destroyer", "Heaviest standard warship; shreds battlecruisers and light lasers.",
                2000, 500, 110000, 2000, 5000, 1000, new Cost(60000, 50000, 15000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 9), Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 5),
                        Requirement.of(Ids.TechType.PLASMA_TECHNOLOGY, 5)),
                rf(Ids.ShipType.BATTLECRUISER, 2, Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5),
                rfd(Ids.DefenseType.LIGHT_LASER, 10)));
        s.put(Ids.ShipType.REAPER, new ShipDef(Ids.ShipType.REAPER,
                "Reaper", "Battlefield harvester: auto-collects part of the debris it creates.",
                2800, 700, 140000, 10000, 7000, 1100, new Cost(85000, 55000, 20000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 10), Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 6),
                        Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 6)),
                rf(Ids.ShipType.BATTLESHIP, 2, Ids.ShipType.BOMBER, 2, Ids.ShipType.DESTROYER, 1,
                        Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5), null));
        s.put(Ids.ShipType.PATHFINDER, new ShipDef(Ids.ShipType.PATHFINDER,
                "Pathfinder", "Explorer cruiser: big expedition bonus and can harvest debris.",
                200, 100, 23000, 10000, 12000, 300, new Cost(8000, 15000, 8000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 5), Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 4),
                        Requirement.of(Ids.TechType.HYPERSPACE_DRIVE, 2)),
                rf(Ids.ShipType.LIGHT_FIGHTER, 3, Ids.ShipType.HEAVY_FIGHTER, 2, Ids.ShipType.CRUISER, 3,
                        Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5), null));
        s.put(Ids.ShipType.LEVIATHAN, new ShipDef(Ids.ShipType.LEVIATHAN,
                "Leviathan", "OG Galaxy flagship. A mobile fortress rivalled only by the Death Star. (stats approximated)",
                50000, 20000, 1500000, 200000, 3000, 2000, new Cost(2000000, 1500000, 800000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 12), Requirement.of(Ids.TechType.HYPERSPACE_DRIVE, 8),
                        Requirement.of(Ids.TechType.PLASMA_TECHNOLOGY, 10)),
                rf(Ids.ShipType.LIGHT_FIGHTER, 10, Ids.ShipType.HEAVY_FIGHTER, 10, Ids.ShipType.CRUISER, 10,
                        Ids.ShipType.BATTLESHIP, 5, Ids.ShipType.BATTLECRUISER, 5, Ids.ShipType.BOMBER, 5,
                        Ids.ShipType.DESTROYER, 3, Ids.ShipType.ESPIONAGE_PROBE, 5, Ids.ShipType.SOLAR_SATELLITE, 5),
                rfd(Ids.DefenseType.ROCKET_LAUNCHER, 50, Ids.DefenseType.LIGHT_LASER, 50,
                        Ids.DefenseType.HEAVY_LASER, 20, Ids.DefenseType.GAUSS_CANNON, 10)));
        s.put(Ids.ShipType.DEATHSTAR, new ShipDef(Ids.ShipType.DEATHSTAR,
                "Death Star", "Planet-sized battlestation with rapid fire against everything. Barely moves.",
                200000, 50000, 9000000, 1000000, 100, 1, new Cost(5000000, 4000000, 1000000), false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 12), Requirement.of(Ids.TechType.HYPERSPACE_DRIVE, 7),
                        Requirement.of(Ids.TechType.HYPERSPACE_TECHNOLOGY, 6),
                        Requirement.of(Ids.TechType.GRAVITON_TECHNOLOGY, 1)),
                rf(Ids.ShipType.LIGHT_FIGHTER, 200, Ids.ShipType.HEAVY_FIGHTER, 100, Ids.ShipType.CRUISER, 33,
                        Ids.ShipType.BATTLESHIP, 30, Ids.ShipType.BATTLECRUISER, 15, Ids.ShipType.BOMBER, 25,
                        Ids.ShipType.DESTROYER, 5, Ids.ShipType.SMALL_CARGO, 250, Ids.ShipType.LARGE_CARGO, 250,
                        Ids.ShipType.RECYCLER, 250, Ids.ShipType.ESPIONAGE_PROBE, 1250,
                        Ids.ShipType.SOLAR_SATELLITE, 1250, Ids.ShipType.COLONY_SHIP, 250, Ids.ShipType.PATHFINDER, 30),
                rfd(Ids.DefenseType.ROCKET_LAUNCHER, 200, Ids.DefenseType.LIGHT_LASER, 200,
                        Ids.DefenseType.HEAVY_LASER, 100, Ids.DefenseType.ION_CANNON, 100,
                        Ids.DefenseType.GAUSS_CANNON, 50)));
        s.put(Ids.ShipType.RECYCLER, new ShipDef(Ids.ShipType.RECYCLER,
                "Recycler", "Harvests debris fields left behind after battles.",
                1, 10, 16000, 20000, 2000, 300, new Cost(10000, 6000, 2000), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 4), Requirement.of(Ids.TechType.COMBUSTION_DRIVE, 6),
                        Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 2)),
                null, null));
        s.put(Ids.ShipType.ESPIONAGE_PROBE, new ShipDef(Ids.ShipType.ESPIONAGE_PROBE,
                "Espionage Probe", "Cheap, blazing-fast scout used to gather intel.",
                1, 0.01, 1000, 5, 100000000, 1, new Cost(0, 1000, 0), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 3), Requirement.of(Ids.TechType.COMBUSTION_DRIVE, 3),
                        Requirement.of(Ids.TechType.ESPIONAGE_TECHNOLOGY, 2)),
                null, null));
        s.put(Ids.ShipType.SOLAR_SATELLITE, new ShipDef(Ids.ShipType.SOLAR_SATELLITE,
                "Solar Satellite", "Cheap orbital energy source. Defenseless; destroyed easily.",
                1, 1, 2000, 0, 0, 0, new Cost(0, 2000, 500), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 1)),
                null, null));
        s.put(Ids.ShipType.COLONY_SHIP, new ShipDef(Ids.ShipType.COLONY_SHIP,
                "Colony Ship", "Settles an empty planet slot, founding a new colony.",
                50, 100, 30000, 7500, 2500, 1000, new Cost(10000, 20000, 10000), true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 4), Requirement.of(Ids.TechType.IMPULSE_DRIVE, 3)),
                null, null));

        // ---------------- Defenses ----------------
        d.put(Ids.DefenseType.ROCKET_LAUNCHER, new DefenseDef(Ids.DefenseType.ROCKET_LAUNCHER,
                "Rocket Launcher", "Cheap defensive line. Overwhelmed by capital ships.",
                80, 20, 2000, new Cost(2000, 0, 0), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 1))));
        d.put(Ids.DefenseType.LIGHT_LASER, new DefenseDef(Ids.DefenseType.LIGHT_LASER,
                "Light Laser", "Reliable mid-cost defense with decent shielding.",
                100, 25, 2000, new Cost(1500, 500, 0), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 2), Requirement.of(Ids.TechType.LASER_TECHNOLOGY, 3))));
        d.put(Ids.DefenseType.HEAVY_LASER, new DefenseDef(Ids.DefenseType.HEAVY_LASER,
                "Heavy Laser", "Hard-hitting laser platform.",
                250, 100, 8000, new Cost(6000, 2000, 0), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 4), Requirement.of(Ids.TechType.LASER_TECHNOLOGY, 6),
                        Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 3))));
        d.put(Ids.DefenseType.ION_CANNON, new DefenseDef(Ids.DefenseType.ION_CANNON,
                "Ion Cannon", "Enormous shields; soaks damage while other guns fire.",
                150, 500, 8000, new Cost(2000, 6000, 0), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 4), Requirement.of(Ids.TechType.ION_TECHNOLOGY, 4))));
        d.put(Ids.DefenseType.GAUSS_CANNON, new DefenseDef(Ids.DefenseType.GAUSS_CANNON,
                "Gauss Cannon", "High-damage kinetic cannon effective against capitals.",
                1100, 200, 35000, new Cost(20000, 15000, 2000), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 6), Requirement.of(Ids.TechType.WEAPONS_TECHNOLOGY, 3),
                        Requirement.of(Ids.TechType.ENERGY_TECHNOLOGY, 6), Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 1))));
        d.put(Ids.DefenseType.PLASMA_TURRET, new DefenseDef(Ids.DefenseType.PLASMA_TURRET,
                "Plasma Turret", "The ultimate defense emplacement.",
                3000, 300, 100000, new Cost(50000, 50000, 30000), 0, false,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 8), Requirement.of(Ids.TechType.PLASMA_TECHNOLOGY, 7))));
        d.put(Ids.DefenseType.SMALL_SHIELD_DOME, new DefenseDef(Ids.DefenseType.SMALL_SHIELD_DOME,
                "Small Shield Dome", "Planet-wide shield. Only one may exist.",
                1, 2000, 20000, new Cost(10000, 10000, 0), 1, true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 1), Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 2))));
        d.put(Ids.DefenseType.LARGE_SHIELD_DOME, new DefenseDef(Ids.DefenseType.LARGE_SHIELD_DOME,
                "Large Shield Dome", "A vastly stronger planetary shield. Only one may exist.",
                1, 10000, 100000, new Cost(50000, 50000, 0), 1, true,
                reqs(Requirement.of(Ids.BuildingType.SHIPYARD, 6), Requirement.of(Ids.TechType.SHIELDING_TECHNOLOGY, 6))));

        // ---------------- Player classes (approximated OG Galaxy bonuses) ----------------
        c.put(Ids.PlayerClass.EXPLORER, new ClassDef(Ids.PlayerClass.EXPLORER,
                "Explorer", "Master of deep space: bigger expedition finds, faster fleets, cheaper fuel.",
                1.0, 1.0, 1.10, 0.75, 1.0, 2.0, 0.75));
        c.put(Ids.PlayerClass.MINER, new ClassDef(Ids.PlayerClass.MINER,
                "Miner", "Economic powerhouse: greatly boosted mine and energy output, bigger storage.",
                1.25, 1.10, 1.0, 1.0, 1.0, 1.0, 1.0));
        c.put(Ids.PlayerClass.GENERAL, new ClassDef(Ids.PlayerClass.GENERAL,
                "General", "Warlord: stronger weapons in battle, faster warships, cheaper fuel.",
                1.0, 1.0, 1.10, 0.75, 1.10, 1.0, 1.0));

        return new Catalog(b, t, s, d, c);
    }
}
