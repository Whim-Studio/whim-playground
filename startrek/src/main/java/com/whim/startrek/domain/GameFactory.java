package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a ready-to-play galaxy for a chosen player race. Seeds a player empire and
 * a few AI empires, scatters star systems and hazards, and lays down at least one
 * wormhole pair. The returned {@link GameState} is immediately usable by the engine
 * and UI.
 */
public final class GameFactory {

    private GameFactory() {
    }

    /** AI races used to populate the galaxy (the player's race is excluded). */
    private static final Race[] AI_POOL = {
        Race.FEDERATION, Race.KLINGON, Race.ROMULAN, Race.DOMINION,
        Race.XINDI, Race.THOLIAN, Race.BREEN, Race.AKAALI, Race.OCAMPA
    };

    private static final String[] SYSTEM_NAMES = {
        "Sol", "Vulcan", "Andoria", "Qo'noS", "Romulus", "Bajor", "Cardassia",
        "Ferenginar", "Betazed", "Risa", "Trill", "Tellar", "Deneb", "Rigel",
        "Wolf 359", "Vega", "Arcturus", "Antares", "Ceti Alpha", "Organia",
        "Talos", "Eminiar", "Janus", "Memory Alpha", "Khitomer", "Nimbus",
        "Dyson", "Argelius", "Benecia", "Tarsus"
    };

    /**
     * @param playerRace the race the human player controls
     * @param rows       galaxy grid height (clamped to >= 4)
     * @param cols       galaxy grid width (clamped to >= 4)
     */
    public static GameState newGame(Race playerRace, int rows, int cols) {
        if (rows < 4) {
            rows = 4;
        }
        if (cols < 4) {
            cols = 4;
        }

        Random rng = new Random();
        GalaxyMap map = new GalaxyMap(rows, cols);
        List<Empire> empires = new ArrayList<Empire>();

        // --- Empires ------------------------------------------------------------
        Empire player = buildEmpire(playerRace == null ? Race.FEDERATION : playerRace, rng);
        player.setPlayer(true);
        empires.add(player);

        int aiCount = Math.min(3, Math.max(2, (rows * cols) / 20));
        List<Race> usedRaces = new ArrayList<Race>();
        usedRaces.add(player.getRace());
        for (int i = 0; i < aiCount; i++) {
            Race r = pickAiRace(usedRaces, rng);
            if (r == null) {
                break;
            }
            usedRaces.add(r);
            empires.add(buildEmpire(r, rng));
        }

        // --- Star systems -------------------------------------------------------
        int totalCells = rows * cols;
        int systemCount = Math.max(empires.size() + 2, totalCells / 6);
        scatterSystems(map, systemCount, rng);

        // --- Home systems + starting fleets ------------------------------------
        List<StarSystem> systems = map.allSystems();
        int nameIdx = 0;
        for (Empire e : empires) {
            StarSystem home = systems.isEmpty() ? null : systems.get(nameIdx % systems.size());
            nameIdx++;
            if (home != null && home.getOwner() == null) {
                home.setOwner(e.getRace());
                home.setPopulation(50_000_000L);
                home.setFacility(FacilityType.SHIPYARD, 1);
                home.setFacility(FacilityType.STARBASE, 1);
                home.setFacility(FacilityType.RESEARCH_FACILITY, 1);
                home.setProduction(ResourceType.CREDITS, 1000L);
                home.setProduction(ResourceType.METALS, 800L);
                home.setProduction(ResourceType.DEUTERIUM, 600L);
                home.setProduction(ResourceType.DILITHIUM, 200L);
                home.setProduction(ResourceType.OFFICERS, 5L);
                e.getSystems().add(home);
            }

            Fleet fleet = buildStartingFleet(e, rng);
            if (home != null) {
                fleet.setCell(home.getRow(), home.getCol());
                fleet.setDestination(home.getRow(), home.getCol());
                GridCell cell = map.getCell(home.getRow(), home.getCol());
                if (cell != null) {
                    cell.getFleets().add(fleet);
                }
            } else {
                int r = rng.nextInt(rows);
                int c = rng.nextInt(cols);
                fleet.setCell(r, c);
                fleet.setDestination(r, c);
                map.getCell(r, c).getFleets().add(fleet);
            }
            e.getFleets().add(fleet);
        }

        // --- Hazards ------------------------------------------------------------
        scatterHazards(map, rng);

        // --- Wormhole pair(s) ---------------------------------------------------
        placeWormholePair(map, rng);
        if (totalCells >= 64 && rng.nextBoolean()) {
            placeWormholePair(map, rng);
        }

        return new GameState(map, empires);
    }

    private static Race pickAiRace(List<Race> used, Random rng) {
        List<Race> avail = new ArrayList<Race>();
        for (Race r : AI_POOL) {
            if (!used.contains(r)) {
                avail.add(r);
            }
        }
        if (avail.isEmpty()) {
            return null;
        }
        return avail.get(rng.nextInt(avail.size()));
    }

    private static Empire buildEmpire(Race race, Random rng) {
        Empire e = new Empire(race);
        e.setStatus(EmpireStatus.PEACE);

        // Starting treasury scales loosely with civilization level.
        long scale = race.getCivLevel();
        e.setTreasury(ResourceType.CREDITS, 5000L + scale * 1000L);
        e.setTreasury(ResourceType.METALS, 2000L + scale * 500L);
        e.setTreasury(ResourceType.DEUTERIUM, 1500L + scale * 400L);
        e.setTreasury(ResourceType.DILITHIUM, 500L + scale * 150L);
        e.setTreasury(ResourceType.OFFICERS, 10L + scale * 4L); // non-tradable crew pool

        // Distribute the tech-point pool across trees, clamped to each tree's cap.
        distributeTech(e, race, rng);
        return e;
    }

    private static void distributeTech(Empire e, Race race, Random rng) {
        int pool = race.getTechPointPool();
        TechType[] trees = TechType.values();
        // Seed every tree with a baseline level scaled to civ level.
        int baseline = Math.max(0, race.getCivLevel() - 1);
        for (TechType t : trees) {
            e.setTechLevel(t, Math.min(baseline, race.getCap(t)));
            pool -= e.getTechLevel(t);
        }
        // Spend the remaining pool randomly, respecting caps.
        int guard = 0;
        while (pool > 0 && guard < 1000) {
            guard++;
            TechType t = trees[rng.nextInt(trees.length)];
            int cur = e.getTechLevel(t);
            if (cur < race.getCap(t)) {
                e.setTechLevel(t, cur + 1);
                pool--;
            } else if (allCapped(e, race)) {
                break;
            }
        }
    }

    private static boolean allCapped(Empire e, Race race) {
        for (TechType t : TechType.values()) {
            if (e.getTechLevel(t) < race.getCap(t)) {
                return false;
            }
        }
        return true;
    }

    private static int fleetCounter = 0;

    private static Fleet buildStartingFleet(Empire e, Random rng) {
        Fleet fleet = new Fleet(++fleetCounter, e.getRace());
        boolean canCloak = e.getRace() == Race.ROMULAN || e.getRace() == Race.KLINGON;

        Ship flagship = new Ship(e.getRace().name() + " Flagship", "Cruiser", e.getRace());
        flagship.setMaxHull(140);
        flagship.setHull(140);
        flagship.setMaxShields(80);
        flagship.setShields(80);
        flagship.setWeaponDamage(18);
        flagship.setWeaponRange(7);
        flagship.setOfficersRequired(3);
        flagship.setCloakCapable(canCloak);
        flagship.setCloaked(false);
        fleet.addShip(flagship);

        int escorts = 1 + rng.nextInt(2);
        for (int i = 0; i < escorts; i++) {
            Ship escort = new Ship(e.getRace().name() + " Escort " + (i + 1), "Destroyer", e.getRace());
            escort.setMaxHull(90);
            escort.setHull(90);
            escort.setMaxShields(45);
            escort.setShields(45);
            escort.setWeaponDamage(11);
            escort.setWeaponRange(5);
            escort.setOfficersRequired(2);
            escort.setCloakCapable(canCloak);
            fleet.addShip(escort);
        }
        return fleet;
    }

    private static void scatterSystems(GalaxyMap map, int count, Random rng) {
        int rows = map.getRows();
        int cols = map.getCols();
        int placed = 0;
        int guard = 0;
        int maxGuard = rows * cols * 4;
        while (placed < count && guard < maxGuard) {
            guard++;
            int r = rng.nextInt(rows);
            int c = rng.nextInt(cols);
            GridCell cell = map.getCell(r, c);
            if (cell.getType() == MapObjectType.EMPTY && cell.getSystem() == null) {
                String name = SYSTEM_NAMES[placed % SYSTEM_NAMES.length];
                if (placed >= SYSTEM_NAMES.length) {
                    name = name + " " + (placed / SYSTEM_NAMES.length + 1);
                }
                StarSystem sys = new StarSystem(name, r, c);
                sys.setPopulation(1_000_000L + (long) rng.nextInt(20_000_000));
                sys.setProduction(ResourceType.CREDITS, 100L + rng.nextInt(400));
                sys.setProduction(ResourceType.METALS, 80L + rng.nextInt(300));
                cell.setType(MapObjectType.SOLAR_SYSTEM);
                cell.setSystem(sys);
                placed++;
            }
        }
    }

    private static void scatterHazards(GalaxyMap map, Random rng) {
        int rows = map.getRows();
        int cols = map.getCols();
        MapObjectType[] hazards = {
            MapObjectType.NEBULA, MapObjectType.ENERGY_STORM, MapObjectType.SUPERNOVA,
            MapObjectType.BLACK_HOLE, MapObjectType.SUPER_BLACK_HOLE
        };
        int hazardCount = Math.max(2, (rows * cols) / 12);
        int placed = 0;
        int guard = 0;
        int maxGuard = rows * cols * 4;
        while (placed < hazardCount && guard < maxGuard) {
            guard++;
            int r = rng.nextInt(rows);
            int c = rng.nextInt(cols);
            GridCell cell = map.getCell(r, c);
            if (cell.getType() == MapObjectType.EMPTY && cell.getFleets().isEmpty()) {
                cell.setType(hazards[rng.nextInt(hazards.length)]);
                placed++;
            }
        }
    }

    private static void placeWormholePair(GalaxyMap map, Random rng) {
        GridCell a = findEmptyCell(map, rng);
        GridCell b = findEmptyCell(map, rng);
        if (a == null || b == null || a == b) {
            return;
        }
        // First wormhole stable, second unstable, for variety.
        a.setType(MapObjectType.STABLE_WORMHOLE);
        b.setType(rng.nextBoolean() ? MapObjectType.STABLE_WORMHOLE : MapObjectType.UNSTABLE_WORMHOLE);
        a.setWormholeLink(b.getRow(), b.getCol());
        b.setWormholeLink(a.getRow(), a.getCol());
    }

    private static GridCell findEmptyCell(GalaxyMap map, Random rng) {
        int rows = map.getRows();
        int cols = map.getCols();
        for (int attempt = 0; attempt < rows * cols * 4; attempt++) {
            int r = rng.nextInt(rows);
            int c = rng.nextInt(cols);
            GridCell cell = map.getCell(r, c);
            if (cell.getType() == MapObjectType.EMPTY && cell.getSystem() == null
                    && cell.getFleets().isEmpty()) {
                return cell;
            }
        }
        return null;
    }
}
