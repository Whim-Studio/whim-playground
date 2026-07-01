package com.whim.warroom.engine;

import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Route;
import com.whim.warroom.domain.SimSnapshot;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.Vec2;
import com.whim.warroom.domain.Waypoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Route-following 2D vector movement plus synchronized detonations.
 *
 * Each tick a unit moves toward its active waypoint, pacing its speed so it
 * arrives on the waypoint's {@code arrivalTick} (clamped to the unit's max
 * step). Terrain {@code moveCostMul} scales the max step. AI orders can override
 * route following with pursue/flee motion.
 *
 * Detonation waypoints fire a blast when their arrivalTick is reached; the blast
 * damages every unit inside the radius scaled by {@code 1 - dist/radius}.
 */
final class MovementSystem {

    /** World-unit distance at which a waypoint counts as reached. */
    static final double ARRIVE_EPS = 1.5;

    /**
     * Advance all units one tick.
     *
     * @param wpIndex        engine-owned per-unit index of the active waypoint
     * @param firedDeton     engine-owned set of "unitId#waypointIndex" already detonated
     * @param outBlasts      newly created blasts this tick are appended here
     */
    void update(List<Unit> units, MapState map, int tick,
                Map<Integer, AiController.Order> orders,
                Map<Integer, Integer> wpIndex,
                Set<String> firedDeton,
                List<SimSnapshot.BlastView> outBlasts) {

        // Detonations fire independently of unit movement / life.
        fireDetonations(units, tick, firedDeton, outBlasts);

        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.isAlive()) {
                continue;
            }
            double maxStep = u.getType().getSpeed() / SimEngineImpl.TICKS_PER_SECOND
                    * map.moveCostMulAtWorld(u.getPos().x, u.getPos().y);
            if (maxStep <= 0) {
                continue;
            }

            AiController.Order o = orders.get(u.getId());
            AiController.Mode mode = (o == null) ? AiController.Mode.ROUTE : o.mode;

            if (u.isRouted() || mode == AiController.Mode.FLEE) {
                fleeMove(u, map, o, maxStep);
            } else if (mode == AiController.Mode.PURSUE && o.target != null) {
                pursueMove(u, o.target, maxStep);
            } else {
                routeMove(u, tick, maxStep, wpIndex);
            }
        }
    }

    private void fireDetonations(List<Unit> units, int tick,
                                 Set<String> firedDeton,
                                 List<SimSnapshot.BlastView> outBlasts) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            Route r = u.getRoute();
            if (r == null || r.isEmpty()) {
                continue;
            }
            List<Waypoint> wps = r.getWaypoints();
            for (int w = 0; w < wps.size(); w++) {
                Waypoint wp = wps.get(w);
                if (!wp.isDetonation() || tick < wp.getArrivalTick()) {
                    continue;
                }
                String key = u.getId() + "#" + w;
                if (firedDeton.contains(key)) {
                    continue;
                }
                firedDeton.add(key);
                double radius = wp.getBlastRadius();
                double dmg = wp.getBlastDamage();
                Vec2 c = wp.getPos();
                // Apply damage to every unit in radius, scaled by proximity.
                for (int j = 0; j < units.size(); j++) {
                    Unit v = units.get(j);
                    if (!v.isAlive()) {
                        continue;
                    }
                    double d = v.getPos().dist(c);
                    if (d <= radius && radius > 0) {
                        double scaled = dmg * (1.0 - d / radius);
                        if (scaled > 0) {
                            v.setHealth(v.getHealth() - scaled);
                        }
                    }
                }
                // age 0.0 => freshly spawned this frame; engine fades it over its lifetime.
                outBlasts.add(new SimSnapshot.BlastView(c.x, c.y, radius, 0.0));
            }
        }
    }

    /** Follow the unit's route with arrival-tick pacing. */
    private void routeMove(Unit u, int tick, double maxStep, Map<Integer, Integer> wpIndex) {
        Route r = u.getRoute();
        if (r == null || r.isEmpty()) {
            return;
        }
        List<Waypoint> wps = r.getWaypoints();
        Integer idxBox = wpIndex.get(u.getId());
        int idx = (idxBox == null) ? 0 : idxBox.intValue();

        // Skip waypoints whose arrival tick has already passed.
        while (idx < wps.size() && wps.get(idx).getArrivalTick() < tick
                && u.getPos().dist(wps.get(idx).getPos()) < ARRIVE_EPS) {
            idx++;
        }
        if (idx >= wps.size()) {
            wpIndex.put(u.getId(), idx);
            return;
        }

        Waypoint wp = wps.get(idx);
        Vec2 to = wp.getPos();
        double dist = u.getPos().dist(to);
        if (dist <= ARRIVE_EPS) {
            u.setPos(to);
            wpIndex.put(u.getId(), idx + 1);
            return;
        }
        int remTicks = wp.getArrivalTick() - tick;
        double pace = (remTicks > 0) ? dist / remTicks : maxStep;
        double step = Math.min(pace, maxStep);
        step = Math.min(step, dist);

        Vec2 dir = to.sub(u.getPos()).normalized();
        u.setHeading(Math.atan2(dir.y, dir.x));
        u.setPos(u.getPos().add(dir.scale(step)));

        if (u.getPos().dist(to) <= ARRIVE_EPS) {
            u.setPos(to);
            wpIndex.put(u.getId(), idx + 1);
        } else {
            wpIndex.put(u.getId(), idx);
        }
    }

    /** Close toward an enemy but stop just inside weapon range so we can fire. */
    private void pursueMove(Unit u, Vec2 target, double maxStep) {
        double stop = u.getType().getRange() * 0.85;
        double dist = u.getPos().dist(target);
        if (dist <= stop) {
            return;
        }
        Vec2 dir = target.sub(u.getPos()).normalized();
        double step = Math.min(maxStep, dist - stop);
        u.setHeading(Math.atan2(dir.y, dir.x));
        u.setPos(u.getPos().add(dir.scale(step)));
    }

    /** Flee directly away from the nearest enemy (or hold if none). */
    private void fleeMove(Unit u, MapState map, AiController.Order o, double maxStep) {
        Vec2 dir;
        if (o != null && o.target != null) {
            dir = u.getPos().sub(o.target).normalized();
        } else {
            return;
        }
        if (dir.len() < 1e-9) {
            return;
        }
        u.setHeading(Math.atan2(dir.y, dir.x));
        Vec2 next = u.getPos().add(dir.scale(maxStep));
        // Clamp inside the world bounds.
        double x = Math.max(0, Math.min(map.worldWidth(), next.x));
        double y = Math.max(0, Math.min(map.worldHeight(), next.y));
        u.setPos(new Vec2(x, y));
    }
}
