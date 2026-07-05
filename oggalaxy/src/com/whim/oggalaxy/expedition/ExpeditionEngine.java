package com.whim.oggalaxy.expedition;

import com.whim.oggalaxy.api.Catalog;
import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.ShipDef;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Resolves an expedition (a fleet sent to deep space, position 16 conceptually) into a
 * weighted random outcome. Uses a caller-supplied seeded {@link Random} so results are
 * reproducible from the save's master seed. Larger / stronger escorts shift the odds
 * toward good outcomes and raise the caps on how much can be found; the player class's
 * expedition bonus scales find sizes.
 *
 * The full weighted table is documented in {@code expedition/RESEARCH.md}.
 */
public final class ExpeditionEngine {

    private ExpeditionEngine() {
    }

    public enum Kind { RESOURCES, DARK_MATTER, FLEET_FOUND, NOTHING, DELAY, PIRATES, BLACK_HOLE }

    /** Result the engine applies to the returning fleet / owning empire. */
    public static final class Outcome {
        public Kind kind;
        public String outcome = "";
        public String detail = "";
        public Cost resourceGains = Cost.ZERO;
        public int darkMatter;
        public Map<Ids.ShipType, Integer> gainedShips = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public Map<Ids.ShipType, Integer> lostShips = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        public int extraDelayTicks;
    }

    public static Outcome resolve(Map<Ids.ShipType, Integer> fleet, double expeditionBonus,
                                  Random rng, Catalog catalog) {
        Outcome out = new Outcome();

        int totalShips = 0;
        double totalCargo = 0;
        double fleetPoints = 0;
        for (Map.Entry<Ids.ShipType, Integer> e : fleet.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            ShipDef d = catalog.ship(e.getKey());
            totalShips += e.getValue();
            totalCargo += d.cargo * e.getValue();
            fleetPoints += d.cost.structurePoints() * e.getValue();
        }
        // size tier 0..3 nudges the weights
        int tier = totalShips >= 200 ? 3 : totalShips >= 60 ? 2 : totalShips >= 15 ? 1 : 0;

        // base weights, then size-adjusted
        int wResources = 30 + 4 * tier;
        int wDarkMatter = 12 + tier;
        int wFleet = 8 + 2 * tier;
        int wNothing = 25;
        int wDelay = 10;
        int wPirates = Math.max(3, 10 - 2 * tier);
        int wBlackHole = Math.max(1, 2 - (tier >= 2 ? 1 : 0));

        int totalW = wResources + wDarkMatter + wFleet + wNothing + wDelay + wPirates + wBlackHole;
        int roll = rng.nextInt(totalW);

        int acc = 0;
        Kind kind;
        if (roll < (acc += wResources)) kind = Kind.RESOURCES;
        else if (roll < (acc += wDarkMatter)) kind = Kind.DARK_MATTER;
        else if (roll < (acc += wFleet)) kind = Kind.FLEET_FOUND;
        else if (roll < (acc += wNothing)) kind = Kind.NOTHING;
        else if (roll < (acc += wDelay)) kind = Kind.DELAY;
        else if (roll < (acc += wPirates)) kind = Kind.PIRATES;
        else kind = Kind.BLACK_HOLE;
        out.kind = kind;

        switch (kind) {
            case RESOURCES: {
                double cap = Math.max(1000, totalCargo);
                double factor = (0.15 + 0.45 * rng.nextDouble()) * Math.max(1.0, expeditionBonus);
                double amount = Math.min(cap, Math.floor(cap * factor));
                if (amount < 500) amount = 500;
                // split ~ 50/30/20
                double m = Math.floor(amount * 0.5);
                double c = Math.floor(amount * 0.3);
                double d = Math.floor(amount * 0.2);
                out.resourceGains = new Cost(m, c, d);
                out.outcome = "Resources found";
                out.detail = "Your expedition salvaged a derelict convoy: " + out.resourceGains + ".";
                break;
            }
            case DARK_MATTER: {
                int dm = (int) Math.floor((50 + rng.nextInt(451)) * Math.max(1.0, expeditionBonus));
                out.darkMatter = dm;
                out.outcome = "Dark Matter found";
                out.detail = "A strange energy signature yielded " + dm + " dark matter.";
                break;
            }
            case FLEET_FOUND: {
                // gain a handful of light escorts, scaled by size and capped
                int found = 1 + Math.min(20, tier * 3 + rng.nextInt(4));
                Ids.ShipType type = rng.nextBoolean() ? Ids.ShipType.LIGHT_FIGHTER : Ids.ShipType.SMALL_CARGO;
                out.gainedShips.put(type, found);
                out.outcome = "Fleet found";
                out.detail = "You recovered " + found + "x " + catalog.ship(type).name + " adrift in space.";
                break;
            }
            case NOTHING: {
                out.outcome = "Nothing";
                out.detail = "The expedition charted empty space and returned uneventfully.";
                break;
            }
            case DELAY: {
                out.extraDelayTicks = 1 + rng.nextInt(2);
                out.outcome = "Delayed";
                out.detail = "A spatial anomaly delayed the fleet by " + out.extraDelayTicks + "h.";
                break;
            }
            case PIRATES: {
                // small combat: lose a modest fraction of the escort
                double lossFrac = 0.03 + 0.12 * rng.nextDouble();
                out.lostShips = pickLosses(fleet, lossFrac, catalog);
                out.outcome = out.lostShips.isEmpty() ? "Pirates repelled" : "Pirates!";
                out.detail = out.lostShips.isEmpty()
                        ? "Space pirates ambushed the fleet but were driven off."
                        : "Space pirates ambushed the fleet; some ships were lost.";
                break;
            }
            case BLACK_HOLE:
            default: {
                out.lostShips = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
                for (Map.Entry<Ids.ShipType, Integer> e : fleet.entrySet()) {
                    if (e.getValue() != null && e.getValue() > 0) out.lostShips.put(e.getKey(), e.getValue());
                }
                out.outcome = "Fleet lost";
                out.detail = "The fleet was pulled into a black hole and never returned.";
                break;
            }
        }
        return out;
    }

    /** Destroy the given fraction of the fleet, preferring the weakest (cheapest) ships first. */
    private static Map<Ids.ShipType, Integer> pickLosses(Map<Ids.ShipType, Integer> fleet,
                                                         double frac, Catalog catalog) {
        Map<Ids.ShipType, Integer> losses = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        List<Ids.ShipType> order = new ArrayList<Ids.ShipType>(fleet.keySet());
        // cheapest first
        order.sort(new java.util.Comparator<Ids.ShipType>() {
            @Override public int compare(Ids.ShipType a, Ids.ShipType b) {
                double ca = catalog.ship(a).cost.structurePoints();
                double cb = catalog.ship(b).cost.structurePoints();
                return Double.compare(ca, cb);
            }
        });
        for (Ids.ShipType t : order) {
            int have = fleet.get(t) == null ? 0 : fleet.get(t);
            int lose = (int) Math.floor(have * frac);
            if (lose > 0) losses.put(t, Math.min(have, lose));
        }
        return losses;
    }
}
