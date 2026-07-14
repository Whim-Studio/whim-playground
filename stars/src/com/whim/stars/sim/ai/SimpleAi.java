package com.whim.stars.sim.ai;

import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.Waypoint;
import com.whim.stars.model.formulas.Formulas;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.ShipDesign;

/**
 * A basic, non-PRT-specific heuristic AI. It is <b>deterministic</b> (no RNG) so
 * a game stays reproducible, and it is issued as <i>orders</i> before the turn
 * resolves — {@link #takeTurn(Galaxy, Player)} sets production queues, loads and
 * dispatches colony ships, and sends idle scouts to explore. The
 * {@link com.whim.stars.sim.TurnEngine} then resolves those orders exactly as it
 * would a human's, so the AI never gets privileged mechanics.
 *
 * <p>Deliberately simple (expansion-focused, no military strategy) — enough to
 * make the Rival a moving, growing opponent. Smarter play is a later phase.
 */
public final class SimpleAi {

    // A laden colony ship burns fuel roughly with mass·warp², and colonists count
    // as mass, so a heavy load strands the ship at a warp-1 crawl. Keep the load
    // light enough that the ship retains useful range.
    private static final long COLONISTS_PER_SHIP = 1_000L;
    private static final long MIN_POP_TO_SEED = 30_000L;

    private final Galaxy galaxy;

    public SimpleAi(Galaxy galaxy) {
        this.galaxy = galaxy;
    }

    /** Run every AI player's turn planning. Call once before the turn resolves. */
    public void planAll() {
        for (Player p : galaxy.players()) {
            if (p.isAi()) {
                takeTurn(galaxy, p);
            }
        }
    }

    public void takeTurn(Galaxy galaxy, Player player) {
        manageProduction(galaxy, player);
        manageFleets(galaxy, player);
    }

    // --- Production: keep colonies growing and pump out colony ships. ----------
    private void manageProduction(Galaxy galaxy, Player player) {
        boolean wantsColonyShips = hasColonyTarget(galaxy, player) && colonyDesign(player) != null;
        for (Planet planet : galaxy.planetsOf(player)) {
            ensureAuto(planet, ProductionItem.Kind.AUTO_FACTORY);
            ensureAuto(planet, ProductionItem.Kind.AUTO_MINE);

            if (wantsColonyShips && planet.population() >= MIN_POP_TO_SEED
                    && !queueHasShip(planet)) {
                planet.productionQueue().add(ProductionItem.ship(colonyDesign(player), 1));
            }
        }
    }

    private void ensureAuto(Planet planet, ProductionItem.Kind kind) {
        for (ProductionItem item : planet.productionQueue()) {
            if (item.kind() == kind) {
                return;
            }
        }
        // Keep infrastructure entries at the front so they build first.
        planet.productionQueue().add(0, ProductionItem.auto(kind));
    }

    private boolean queueHasShip(Planet planet) {
        for (ProductionItem item : planet.productionQueue()) {
            if (item.kind() == ProductionItem.Kind.SHIP) {
                return true;
            }
        }
        return false;
    }

    // --- Fleets: load & dispatch colonizers, roam scouts. ----------------------
    private void manageFleets(Galaxy galaxy, Player player) {
        for (Fleet fleet : galaxy.fleetsOf(player)) {
            if (!fleet.waypoints().isEmpty()) {
                // Keep a laden colonizer cruising at the best warp its remaining
                // fuel allows, instead of letting a now-unaffordable fixed warp
                // drop it to a warp-1 crawl.
                if (fleet.canColonize()) {
                    fleet.nextWaypoint().setWarp(affordableWarp(fleet));
                }
                continue; // already has orders
            }
            if (fleet.canColonize()) {
                dispatchColonizer(galaxy, player, fleet);
            } else if (fleet.hasScanner()) {
                roam(galaxy, player, fleet);
            }
        }
    }

    private void dispatchColonizer(Galaxy galaxy, Player player, Fleet fleet) {
        // Load colonists if empty and sitting over one of our populous colonies.
        if (fleet.cargo().colonists() <= 0) {
            Planet here = ownedColonyAt(galaxy, player, fleet.x(), fleet.y());
            if (here != null && here.population() > MIN_POP_TO_SEED) {
                here.addPopulation(-COLONISTS_PER_SHIP);
                fleet.cargo().setColonists(COLONISTS_PER_SHIP);
            } else {
                return; // nowhere to pick up colonists this turn
            }
        }
        Planet target = nearestColonizable(galaxy, player, fleet.x(), fleet.y());
        if (target != null) {
            fleet.waypoints().add(Waypoint.toPlanet(target, affordableWarp(fleet), Waypoint.Task.COLONIZE));
        }
    }

    private void roam(Galaxy galaxy, Player player, Fleet fleet) {
        Planet target = nearestUnowned(galaxy, player, fleet.x(), fleet.y());
        if (target != null) {
            fleet.waypoints().add(new Waypoint(target.x(), target.y(), affordableWarp(fleet), Waypoint.Task.NONE));
        }
    }

    /**
     * Highest warp (up to 7) the fleet can sustain on its current fuel and mass.
     * Picking a warp the engine can't fuel would force a warp-1 crawl, so a laden
     * colony ship must throttle down to actually make progress.
     */
    private int affordableWarp(Fleet fleet) {
        int best = 1;
        int cap = Math.min(7, Math.max(1, fleet.maxWarp()));
        for (int w = 1; w <= cap; w++) {
            if (Formulas.fuelUsage(fleet.totalMass(), w) <= fleet.fuel()) {
                best = w;
            }
        }
        return best;
    }

    // --- Helpers ---------------------------------------------------------------
    private boolean hasColonyTarget(Galaxy galaxy, Player player) {
        Race race = player.race();
        for (Planet p : galaxy.planets()) {
            if (!p.isColonized() && p.habitability(race) > 0) {
                return true;
            }
        }
        return false;
    }

    private ShipDesign colonyDesign(Player player) {
        for (ShipDesign d : player.designs()) {
            if (d.canColonize()) {
                return d;
            }
        }
        return null;
    }

    private Planet ownedColonyAt(Galaxy galaxy, Player player, double x, double y) {
        for (Planet p : galaxy.planetsOf(player)) {
            if (Math.abs(p.x() - x) < 1.0 && Math.abs(p.y() - y) < 1.0) {
                return p;
            }
        }
        return null;
    }

    private Planet nearestColonizable(Galaxy galaxy, Player player, double x, double y) {
        Race race = player.race();
        Planet best = null;
        double bestDist = Double.MAX_VALUE;
        for (Planet p : galaxy.planets()) {
            if (p.isColonized() || p.habitability(race) <= 0) {
                continue;
            }
            double d = dist2(p, x, y);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private Planet nearestUnowned(Galaxy galaxy, Player player, double x, double y) {
        Planet best = null;
        double bestDist = Double.MAX_VALUE;
        for (Planet p : galaxy.planets()) {
            if (p.ownerId() == player.id()) {
                continue;
            }
            double d = dist2(p, x, y);
            // Skip the planet we're already sitting on.
            if (d < 1.0) {
                continue;
            }
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    private static double dist2(Planet p, double x, double y) {
        double dx = p.x() - x;
        double dy = p.y() - y;
        return dx * dx + dy * dy;
    }
}
