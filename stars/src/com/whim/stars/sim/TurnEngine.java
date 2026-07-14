package com.whim.stars.sim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.whim.stars.model.Cargo;
import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.TechField;
import com.whim.stars.model.Waypoint;
import com.whim.stars.model.formulas.Formulas;
import com.whim.stars.model.production.ProductionItem;
import com.whim.stars.model.race.Race;
import com.whim.stars.model.ship.ShipDesign;

/**
 * The deterministic turn resolver — the Model's core. One call to
 * {@link #generateTurn()} advances the whole galaxy exactly one year in a fixed,
 * documented order. No randomness and no UI: identical input state always yields
 * identical output state (verified by the sim self-test).
 *
 * <p><b>Resolution order</b> (a simplification of the full Stars! order,
 * documented against the design reference — see notes per step):
 * <ol>
 *   <li>Fleet movement + waypoint tasks (fuel spent, colonize on arrival)</li>
 *   <li>Production (spend resources/minerals down each planet's queue)</li>
 *   <li>Mining (extract minerals, deplete concentration)</li>
 *   <li>Population growth / die-off</li>
 *   <li>Research (bank budget, level up, retarget field)</li>
 *   <li>Combat (co-located mutual enemies fight)</li>
 *   <li>Advance the year</li>
 * </ol>
 */
public final class TurnEngine {

    /** Resources banked toward research this year, per player id. */
    private final Map<Integer, Long> researchBank = new HashMap<Integer, Long>();

    private final Galaxy galaxy;

    public TurnEngine(Galaxy galaxy) {
        this.galaxy = galaxy;
    }

    public void generateTurn() {
        researchBank.clear();
        resolveMovement();
        resolveProduction();
        resolveMining();
        resolveGrowth();
        resolveResearch();
        resolveCombat();
        galaxy.advanceYear();
    }

    // --- Step 1: movement + waypoint tasks -------------------------------------

    private void resolveMovement() {
        for (Fleet fleet : galaxy.fleets()) {
            Waypoint wp = fleet.nextWaypoint();
            if (wp == null) {
                continue;
            }
            int warp = Math.max(1, Math.min(wp.warp(), fleet.maxWarp() == 0 ? 1 : fleet.maxWarp()));
            double dx = wp.x() - fleet.x();
            double dy = wp.y() - fleet.y();
            double remaining = Math.sqrt(dx * dx + dy * dy);

            long fuelNeeded = Formulas.fuelUsage(fleet.totalMass(), warp);
            int reach = Formulas.warpDistance(warp);
            if (fuelNeeded > fleet.fuel()) {
                // Out of fuel for this warp: crawl at warp 1 (free).
                warp = 1;
                reach = Formulas.warpDistance(1);
                fuelNeeded = 0;
            }

            if (remaining <= reach) {
                // Arrive this year.
                fleet.setPosition(wp.x(), wp.y());
                fleet.setFuel(fleet.fuel() - Formulas.fuelUsage(fleet.totalMass(), warp));
                performWaypointTask(fleet, wp);
                if (!fleet.waypoints().isEmpty()) {
                    fleet.waypoints().remove(0);
                }
            } else {
                double frac = reach / remaining;
                fleet.setPosition(fleet.x() + dx * frac, fleet.y() + dy * frac);
                fleet.setFuel(fleet.fuel() - fuelNeeded);
            }
        }
    }

    private void performWaypointTask(Fleet fleet, Waypoint wp) {
        if (wp.task() == Waypoint.Task.COLONIZE) {
            Planet target = wp.targetPlanetId() >= 0 ? galaxy.planet(wp.targetPlanetId()) : planetAt(fleet.x(), fleet.y());
            if (target != null && !target.isColonized() && fleet.canColonize() && fleet.cargo().colonists() > 0) {
                target.setOwnerId(fleet.ownerId());
                target.addPopulation(fleet.cargo().colonists());
                fleet.cargo().setColonists(0);
                // The colonizer is consumed becoming the colony (Stars! behavior).
                galaxy.removeFleet(fleet);
            }
        }
        // TRANSPORT/REMOTE_MINE/LAY_MINES tasks are stubbed for this build.
    }

    private Planet planetAt(double x, double y) {
        for (Planet p : galaxy.planets()) {
            if (Math.abs(p.x() - x) < 0.5 && Math.abs(p.y() - y) < 0.5) {
                return p;
            }
        }
        return null;
    }

    // --- Step 2: production ----------------------------------------------------

    private void resolveProduction() {
        for (Planet planet : galaxy.planets()) {
            if (!planet.isColonized()) {
                continue;
            }
            Player owner = galaxy.player(planet.ownerId());
            if (owner == null) {
                continue;
            }
            Race race = owner.race();

            int totalResources = Economy.resources(planet, race);
            int researchShare = totalResources * owner.researchBudgetPercent() / 100;
            int prodResources = totalResources - researchShare;
            bankResearch(owner, researchShare);

            buildQueue(planet, race, prodResources);
        }
    }

    private static final int BUILT = 1;    // one unit completed; remainingResources holds leftover
    private static final int PARTIAL = 0;  // all resources banked toward next unit
    private static final int BLOCKED = -1; // mineral-blocked; resources untouched

    private void buildQueue(Planet planet, Race race, int resourcesAvailable) {
        Iterator<ProductionItem> it = planet.productionQueue().iterator();
        int resources = resourcesAvailable;
        while (it.hasNext() && resources > 0) {
            ProductionItem item = it.next();
            boolean move = false;
            while (resources > 0 && !item.isComplete()) {
                int status = tryBuildOne(planet, race, item, resources);
                if (status == BUILT) {
                    resources = remainingResources;
                    if (item.kind().isAuto() && !autoStillNeeded(planet, race, item)) {
                        move = true;
                        break;
                    }
                } else if (status == PARTIAL) {
                    resources = 0; // everything banked into this item's partial progress
                    break;
                } else { // BLOCKED
                    move = true; // leave resources for later items
                    break;
                }
            }
            if (item.isComplete()) {
                it.remove();
            }
            if (!move && resources <= 0) {
                break;
            }
        }
    }

    // Scratch value passed back from tryBuildOne (kept as a field to avoid an
    // allocation-heavy return type in this hot loop).
    private int remainingResources;

    private int tryBuildOne(Planet planet, Race race, ProductionItem item, int resources) {
        int resCost;
        long irCost = 0, boCost = 0, geCost = 0;
        switch (item.kind()) {
            case FACTORY:
            case AUTO_FACTORY:
                resCost = 10; // resources per factory (confirmed community value)
                geCost = race.factoryCost();
                break;
            case MINE:
            case AUTO_MINE:
                resCost = race.mineCost();
                break;
            case DEFENSE:
                resCost = 15;
                boCost = 5;
                break;
            case PLANETARY_SCANNER:
                resCost = 30;
                geCost = 10;
                break;
            case SHIP:
                ShipDesign d = item.design();
                if (d == null) return BLOCKED;
                resCost = d.resourceCost();
                Cargo mc = d.mineralCost();
                irCost = mc.ironium(); boCost = mc.boranium(); geCost = mc.germanium();
                break;
            default:
                return BLOCKED;
        }

        // Bank partial resources; only complete when enough are accrued and
        // minerals are on hand.
        int banked = item.partialResources() + resources;
        if (banked < resCost) {
            item.setPartialResources(banked);
            remainingResources = 0; // consumed all available into partial progress
            return PARTIAL;
        }
        if (planet.surface(Mineral.IRONIUM) < irCost
                || planet.surface(Mineral.BORANIUM) < boCost
                || planet.surface(Mineral.GERMANIUM) < geCost) {
            return BLOCKED; // mineral-blocked; leave resources for later items
        }

        // Complete one unit.
        item.setPartialResources(0);
        remainingResources = banked - resCost;
        planet.setSurface(Mineral.IRONIUM, planet.surface(Mineral.IRONIUM) - irCost);
        planet.setSurface(Mineral.BORANIUM, planet.surface(Mineral.BORANIUM) - boCost);
        planet.setSurface(Mineral.GERMANIUM, planet.surface(Mineral.GERMANIUM) - geCost);
        applyBuild(planet, item);
        return BUILT;
    }

    private void applyBuild(Planet planet, ProductionItem item) {
        switch (item.kind()) {
            case FACTORY:
            case AUTO_FACTORY:
                planet.setFactories(planet.factories() + 1);
                break;
            case MINE:
            case AUTO_MINE:
                planet.setMines(planet.mines() + 1);
                break;
            case DEFENSE:
                planet.setDefenses(planet.defenses() + 1);
                break;
            case PLANETARY_SCANNER:
                planet.setPlanetaryScanner(true);
                break;
            case SHIP:
                buildShip(planet, item.design());
                break;
            default:
                break;
        }
        if (!item.kind().isAuto()) {
            item.decrement();
        }
    }

    private void buildShip(Planet planet, ShipDesign design) {
        // Add the ship to a fleet stationed over the planet, or make a new one.
        for (Fleet f : galaxy.fleetsOf(galaxy.player(planet.ownerId()))) {
            if (Math.abs(f.x() - planet.x()) < 0.5 && Math.abs(f.y() - planet.y()) < 0.5) {
                f.addShips(design, 1);
                f.setFuel(f.fuelCapacity());
                return;
            }
        }
        Fleet f = galaxy.newFleet(planet.ownerId(), design.name() + " Fleet", planet.x(), planet.y());
        f.addShips(design, 1);
        f.setFuel(f.fuelCapacity());
    }

    private boolean autoStillNeeded(Planet planet, Race race, ProductionItem item) {
        // Auto entries stop once the colony can't staff more of that building.
        int popCap = (int) (planet.population() / 10_000.0
                * (item.kind() == ProductionItem.Kind.AUTO_FACTORY ? race.factoriesPer10kPop() : race.minesPer10kPop()));
        int current = item.kind() == ProductionItem.Kind.AUTO_FACTORY ? planet.factories() : planet.mines();
        return current < Math.max(popCap, 10);
    }

    // --- Step 3: mining --------------------------------------------------------

    private void resolveMining() {
        for (Planet planet : galaxy.planets()) {
            if (!planet.isColonized()) {
                continue;
            }
            Race race = galaxy.player(planet.ownerId()).race();
            for (Mineral m : Mineral.values()) {
                long mined = Economy.miningOutput(planet, race, m);
                if (mined > 0) {
                    planet.addSurface(m, mined);
                    // Concentration slowly depletes as the planet is worked.
                    if (planet.concentration(m) > 1 && (galaxy.year() % 10) == 0) {
                        planet.setConcentration(m, planet.concentration(m) - 1);
                    }
                }
            }
        }
    }

    // --- Step 4: growth --------------------------------------------------------

    private void resolveGrowth() {
        for (Planet planet : galaxy.planets()) {
            if (!planet.isColonized()) {
                continue;
            }
            Race race = galaxy.player(planet.ownerId()).race();
            double hab = planet.habitability(race);
            long maxPop = planet.maxPopulation(race);
            long delta = Formulas.populationGrowth(race, planet.population(), maxPop, hab);
            planet.addPopulation(delta);
        }
    }

    // --- Step 5: research ------------------------------------------------------

    private void bankResearch(Player owner, long resources) {
        Long current = researchBank.get(owner.id());
        researchBank.put(owner.id(), (current == null ? 0 : current) + resources);
    }

    private void resolveResearch() {
        for (Player player : galaxy.players()) {
            Long banked = researchBank.get(player.id());
            if (banked == null || banked <= 0) {
                continue;
            }
            TechField field = player.currentResearch();
            player.addResearchPoints(field, banked);

            // Level up as many times as the banked points allow.
            boolean levelled = true;
            while (levelled) {
                levelled = false;
                int level = player.tech().get(field);
                long cost = Formulas.researchCost(field, level, player.tech().total(), player.race().researchCostFactor(field));
                if (player.researchPoints(field) >= cost) {
                    player.setResearchPoints(field, player.researchPoints(field) - cost);
                    player.tech().increment(field);
                    levelled = true;
                    field = retarget(player);
                }
            }
        }
    }

    private TechField retarget(Player player) {
        switch (player.nextFieldPolicy()) {
            case SAME:
                return player.currentResearch();
            case NEXT_CHEAPEST: {
                TechField best = player.currentResearch();
                long bestCost = Long.MAX_VALUE;
                for (TechField f : TechField.values()) {
                    long cost = Formulas.researchCost(f, player.tech().get(f), player.tech().total(),
                            player.race().researchCostFactor(f));
                    if (cost < bestCost) {
                        bestCost = cost;
                        best = f;
                    }
                }
                player.setCurrentResearch(best);
                return best;
            }
            case LOWEST:
            default: {
                TechField lowest = TechField.ENERGY;
                for (TechField f : TechField.values()) {
                    if (player.tech().get(f) < player.tech().get(lowest)) {
                        lowest = f;
                    }
                }
                player.setCurrentResearch(lowest);
                return lowest;
            }
        }
    }

    // --- Step 6: combat --------------------------------------------------------

    /**
     * SIMPLIFIED combat: at every location where mutually-hostile fleets meet,
     * each side deals its total firepower to the other in one exchange, and
     * ships die as accumulated damage exceeds their armor+shield. This is NOT
     * the real 10x10 token/initiative grid with range, accuracy and jammers —
     * that tactical model is a later pass. It is deterministic and enough to
     * make armed fleets meaningful.
     */
    private void resolveCombat() {
        Map<String, List<Fleet>> byLocation = new HashMap<String, List<Fleet>>();
        for (Fleet f : galaxy.fleets()) {
            String key = Math.round(f.x()) + "," + Math.round(f.y());
            List<Fleet> here = byLocation.get(key);
            if (here == null) {
                here = new ArrayList<Fleet>();
                byLocation.put(key, here);
            }
            here.add(f);
        }

        for (List<Fleet> here : byLocation.values()) {
            if (here.size() < 2) {
                continue;
            }
            if (!hasHostilePair(here)) {
                continue;
            }
            // Snapshot each fleet's incoming damage before applying any, so the
            // exchange is simultaneous and order-independent.
            Map<Fleet, Long> incoming = new HashMap<Fleet, Long>();
            for (Fleet target : here) {
                long dmg = 0;
                for (Fleet shooter : here) {
                    if (shooter != target && areEnemies(shooter, target)) {
                        dmg += shooter.totalWeaponPower();
                    }
                }
                incoming.put(target, dmg);
            }
            for (Map.Entry<Fleet, Long> e : incoming.entrySet()) {
                applyDamage(e.getKey(), e.getValue());
            }
            // Remove wiped-out fleets.
            for (Fleet f : here) {
                if (f.isEmpty()) {
                    galaxy.removeFleet(f);
                }
            }
        }
    }

    private boolean hasHostilePair(List<Fleet> here) {
        for (Fleet a : here) {
            for (Fleet b : here) {
                if (a != b && areEnemies(a, b) && (a.isArmed() || b.isArmed())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean areEnemies(Fleet a, Fleet b) {
        if (a.ownerId() == b.ownerId()) {
            return false;
        }
        Player pa = galaxy.player(a.ownerId());
        Player pb = galaxy.player(b.ownerId());
        if (pa == null || pb == null) {
            return false;
        }
        return pa.relationTo(b.ownerId()) == Player.Relation.ENEMY
                || pb.relationTo(a.ownerId()) == Player.Relation.ENEMY;
    }

    private void applyDamage(Fleet fleet, long damage) {
        if (damage <= 0) {
            return;
        }
        List<ShipDesign> stacks = new ArrayList<ShipDesign>(fleet.ships().keySet());
        for (ShipDesign design : stacks) {
            int count = fleet.ships().get(design);
            long hpPerShip = Math.max(1, design.totalArmor() + design.totalShield());
            long killable = damage / hpPerShip;
            int killed = (int) Math.min(count, killable);
            if (killed > 0) {
                fleet.removeShips(design, killed);
                damage -= killed * hpPerShip;
            }
            if (damage <= 0) {
                break;
            }
        }
    }
}
