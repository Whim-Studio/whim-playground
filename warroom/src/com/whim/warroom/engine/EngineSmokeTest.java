package com.whim.warroom.engine;

import com.whim.warroom.domain.Era;
import com.whim.warroom.domain.Faction;
import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Route;
import com.whim.warroom.domain.SandboxState;
import com.whim.warroom.domain.SimSnapshot;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.UnitType;
import com.whim.warroom.domain.Vec2;
import com.whim.warroom.domain.Waypoint;

/**
 * Dependency-free headless self-check of the engine math. Drives the simulation
 * synchronously via {@link SimEngineImpl#snapshotAt(int)} (no thread timing) so
 * results are deterministic. Prints PASS/FAIL lines and exits non-zero on any
 * failure.
 *
 * Run: {@code java -cp out com.whim.warroom.engine.EngineSmokeTest}
 */
public final class EngineSmokeTest {

    private static int failures = 0;

    public static void main(String[] args) {
        testWaypointArrival();
        testDetonationDamage();
        testIsolatedUnitRouts();
        testDeterminism();

        System.out.println("----------------------------------------");
        if (failures == 0) {
            System.out.println("ALL PASS");
            System.exit(0);
        } else {
            System.out.println(failures + " FAILURE(S)");
            System.exit(1);
        }
    }

    // 1. A routed-follow unit reaches its waypoint by its arrivalTick (+/-1 tick).
    private static void testWaypointArrival() {
        MapState map = new MapState(40, 40); // all GRASSLAND, moveCostMul 1.0
        SandboxState s = new SandboxState(map, 1234L);
        UnitType scout = new UnitType("scout", "Scout", Era.MODERN,
                100, 5, 5, 400, 40, 100); // speed 400 world/s
        Unit u = new Unit(s.nextUnitId(), scout, Faction.BLUE, new Vec2(100, 100));
        u.setStance(Stance.DEFENSIVE);
        Route r = new Route();
        r.add(new Waypoint(new Vec2(400, 100), 60)); // 300 units over 60 ticks
        u.setRoute(r);
        s.addUnit(u);

        SimEngineImpl eng = new SimEngineImpl();
        eng.loadScenario(s);
        SimSnapshot snap = eng.snapshotAt(60);
        eng.shutdown();

        SimSnapshot.UnitView v = findUnit(snap, u.getId());
        double dx = v.x - 400.0;
        double dy = v.y - 100.0;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double oneStep = 400.0 / SimEngineImpl.TICKS_PER_SECOND; // one tick of travel
        report("waypoint arrival by arrivalTick (dist=" + fmt(dist)
                + " <= " + fmt(oneStep) + ")", dist <= oneStep + 0.5);
    }

    // 2. A detonation damages a unit inside its radius.
    private static void testDetonationDamage() {
        MapState map = new MapState(40, 40);
        SandboxState s = new SandboxState(map, 77L);
        UnitType inf = new UnitType("inf", "Infantry", Era.MODERN,
                100, 10, 10, 50, 30, 100);

        Unit victim = new Unit(s.nextUnitId(), inf, Faction.RED, new Vec2(200, 200));
        victim.setStance(Stance.DEFENSIVE);
        s.addUnit(victim);

        // Carrier far away so no direct combat; its route drops a timed charge on the victim.
        UnitType sapper = new UnitType("sapper", "Sapper", Era.MODERN,
                100, 1, 10, 10, 20, 100);
        Unit carrier = new Unit(s.nextUnitId(), sapper, Faction.BLUE, new Vec2(900, 900));
        carrier.setStance(Stance.DEFENSIVE);
        Route r = new Route();
        Waypoint det = new Waypoint(new Vec2(200, 200), 10);
        det.setDetonation(true);
        det.setBlastRadius(120);
        det.setBlastDamage(50);
        r.add(det);
        carrier.setRoute(r);
        s.addUnit(carrier);

        SimEngineImpl eng = new SimEngineImpl();
        eng.loadScenario(s);
        SimSnapshot before = eng.snapshotAt(9);
        SimSnapshot after = eng.snapshotAt(12);
        eng.shutdown();

        double hpBefore = findUnit(before, victim.getId()).health;
        double hpAfter = findUnit(after, victim.getId()).health;
        report("detonation damages in-radius unit (hp " + fmt(hpBefore)
                + " -> " + fmt(hpAfter) + ")", hpAfter <= hpBefore - 40.0);
    }

    // 3. An isolated unit under fire eventually routs.
    private static void testIsolatedUnitRouts() {
        SimEngineImpl eng = buildFirefight(999L);
        SimSnapshot snap = eng.snapshotAt(600); // 10 seconds
        eng.shutdown();
        SimSnapshot.UnitView red = firstOfFaction(snap, Faction.RED);
        report("isolated unit under fire routs (morale=" + fmt(red.morale)
                + " routed=" + red.routed + ")", red.routed);
    }

    // 4. snapshotAt(t) is deterministic: same tick -> identical positions,
    //    even after reset + replay.
    private static void testDeterminism() {
        SimEngineImpl eng = buildFirefight(4242L);
        SimSnapshot first = eng.snapshotAt(300);
        eng.reset();
        SimSnapshot second = eng.snapshotAt(300);
        eng.shutdown();

        boolean identical = first.getUnits().size() == second.getUnits().size();
        double maxDelta = 0;
        for (int i = 0; identical && i < first.getUnits().size(); i++) {
            SimSnapshot.UnitView a = first.getUnits().get(i);
            SimSnapshot.UnitView b = second.getUnits().get(i);
            if (a.id != b.id || a.x != b.x || a.y != b.y || a.health != b.health) {
                identical = false;
            }
            maxDelta = Math.max(maxDelta, Math.abs(a.x - b.x) + Math.abs(a.y - b.y));
        }
        report("determinism: reset+replay reproduces tick 300 (maxDelta="
                + fmt(maxDelta) + ")", identical);
    }

    // A small isolated-RED firefight: 3 BLUE guns hammering 1 RED unit at close range.
    private static SimEngineImpl buildFirefight(long seed) {
        MapState map = new MapState(40, 40);
        SandboxState s = new SandboxState(map, seed);
        UnitType gun = new UnitType("gun", "Gun Team", Era.MODERN,
                100, 30, 10, 20, 220, 100);
        UnitType target = new UnitType("target", "Rifle Team", Era.MODERN,
                120, 12, 12, 20, 160, 100);

        Unit red = new Unit(s.nextUnitId(), target, Faction.RED, new Vec2(500, 500));
        red.setStance(Stance.DEFENSIVE);
        s.addUnit(red);

        double[][] spots = {{380, 500}, {620, 500}, {500, 380}};
        for (int i = 0; i < spots.length; i++) {
            Unit b = new Unit(s.nextUnitId(), gun, Faction.BLUE,
                    new Vec2(spots[i][0], spots[i][1]));
            b.setStance(Stance.DEFENSIVE);
            s.addUnit(b);
        }

        SimEngineImpl eng = new SimEngineImpl();
        eng.loadScenario(s);
        return eng;
    }

    // ---- helpers ----

    private static SimSnapshot.UnitView findUnit(SimSnapshot snap, int id) {
        for (int i = 0; i < snap.getUnits().size(); i++) {
            if (snap.getUnits().get(i).id == id) {
                return snap.getUnits().get(i);
            }
        }
        throw new IllegalStateException("unit " + id + " not in snapshot");
    }

    private static SimSnapshot.UnitView firstOfFaction(SimSnapshot snap, Faction f) {
        for (int i = 0; i < snap.getUnits().size(); i++) {
            if (snap.getUnits().get(i).faction == f) {
                return snap.getUnits().get(i);
            }
        }
        throw new IllegalStateException("no unit of faction " + f);
    }

    private static void report(String name, boolean ok) {
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "PASS" : "FAIL") + " - " + name);
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }
}
