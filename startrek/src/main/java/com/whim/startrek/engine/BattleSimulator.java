package com.whim.startrek.engine;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.whim.startrek.domain.Race;
import com.whim.startrek.domain.Ship;

/**
 * Real-time 2D top-down battle resolver.
 *
 * <p>The UI owns a {@link javax.swing.Timer} (in its own module) that repeatedly calls
 * {@link #step(double)} and repaints; this class owns all the math: ships steer toward the nearest
 * enemy, fire phasers and torpedoes on independent cooldowns, projectiles fly as vectors, and hits are
 * resolved with hit-box collisions that drain shields before biting into hull.
 *
 * <p>A side wins when every ship on the opposing side is destroyed.
 */
public class BattleSimulator {

    /** Collision radius (arena units) used for projectile-vs-ship hit tests. */
    private static final double SHIP_RADIUS = 16.0;
    private static final double PHASER_SPEED = 420.0;
    private static final double TORPEDO_SPEED = 190.0;
    private static final double PHASER_COOLDOWN = 0.55;
    private static final double TORPEDO_COOLDOWN = 1.4;
    /** Engagement range, in arena units, per point of a ship's (cell-based) weapon range. */
    private static final double RANGE_PER_POINT = 45.0;
    private static final double DEFAULT_RANGE = 140.0;

    private final List<Ship> sideA;
    private final List<Ship> sideB;
    private final double arenaWidth;
    private final double arenaHeight;

    private final List<Projectile> projectiles = new ArrayList<Projectile>();
    private final Map<Ship, Double> phaserCd = new IdentityHashMap<Ship, Double>();
    private final Map<Ship, Double> torpedoCd = new IdentityHashMap<Ship, Double>();
    private final Map<Ship, Integer> side = new IdentityHashMap<Ship, Integer>();

    private final Race raceA;
    private final Race raceB;

    private boolean finished;
    private Race winner;

    public BattleSimulator(List<Ship> sideA, List<Ship> sideB, double arenaWidth, double arenaHeight) {
        this.sideA = new ArrayList<Ship>(sideA);
        this.sideB = new ArrayList<Ship>(sideB);
        this.arenaWidth = arenaWidth;
        this.arenaHeight = arenaHeight;
        this.raceA = this.sideA.isEmpty() ? null : this.sideA.get(0).getOwner();
        this.raceB = this.sideB.isEmpty() ? null : this.sideB.get(0).getOwner();
        layoutSide(this.sideA, 0, 0.18);
        layoutSide(this.sideB, 1, 0.82);
        // An empty side means the other already won.
        evaluateFinished();
    }

    /** Spread a side's ships vertically along one edge of the arena. */
    private void layoutSide(List<Ship> ships, int sideIndex, double xFraction) {
        double x = arenaWidth * xFraction;
        int n = ships.size();
        for (int i = 0; i < n; i++) {
            Ship ship = ships.get(i);
            side.put(ship, sideIndex);
            phaserCd.put(ship, 0.0);
            torpedoCd.put(ship, TORPEDO_COOLDOWN * 0.5);
            double y = arenaHeight * (n == 1 ? 0.5 : (0.15 + 0.7 * i / (double) (n - 1)));
            ship.setPosition(x, y);
        }
    }

    public void step(double dtSeconds) {
        if (finished || dtSeconds <= 0) {
            return;
        }
        moveAndFire(sideA, sideB, dtSeconds);
        moveAndFire(sideB, sideA, dtSeconds);
        advanceProjectiles(dtSeconds);
        evaluateFinished();
    }

    private void moveAndFire(List<Ship> own, List<Ship> enemies, double dt) {
        for (Ship ship : own) {
            if (ship.isDestroyed()) {
                continue;
            }
            Ship target = nearestAlive(ship, enemies);
            if (target == null) {
                continue;
            }
            double dx = target.getX() - ship.getX();
            double dy = target.getY() - ship.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double range = engagementRange(ship);

            // Close in until comfortably inside weapon range, then hold station.
            if (dist > range * 0.7 && dist > 1e-6) {
                double speed = ship.getSpeed() > 0 ? ship.getSpeed() : 60.0;
                double move = Math.min(speed * dt, dist);
                ship.setPosition(ship.getX() + dx / dist * move, ship.getY() + dy / dist * move);
                dx = target.getX() - ship.getX();
                dy = target.getY() - ship.getY();
                dist = Math.sqrt(dx * dx + dy * dy);
            }

            tickCooldowns(ship, dt);
            if (dist <= range) {
                fireIfReady(ship, target, dist, dx, dy, range);
            }
        }
    }

    private void tickCooldowns(Ship ship, double dt) {
        phaserCd.put(ship, phaserCd.get(ship) - dt);
        torpedoCd.put(ship, torpedoCd.get(ship) - dt);
    }

    private void fireIfReady(Ship ship, Ship target, double dist, double dx, double dy, double range) {
        if (dist < 1e-6) {
            dist = 1e-6;
        }
        int baseDmg = ship.getWeaponDamage() > 0 ? ship.getWeaponDamage() : 8;
        double maxTravel = range * 1.5;
        if (torpedoCd.get(ship) <= 0) {
            double ux = dx / dist, uy = dy / dist;
            projectiles.add(new Projectile(ship.getX(), ship.getY(), ux * TORPEDO_SPEED, uy * TORPEDO_SPEED,
                    true, ship.getOwner(), Math.max(1, baseDmg), maxTravel));
            torpedoCd.put(ship, TORPEDO_COOLDOWN);
        }
        if (phaserCd.get(ship) <= 0) {
            double ux = dx / dist, uy = dy / dist;
            projectiles.add(new Projectile(ship.getX(), ship.getY(), ux * PHASER_SPEED, uy * PHASER_SPEED,
                    false, ship.getOwner(), Math.max(1, baseDmg / 2), maxTravel));
            phaserCd.put(ship, PHASER_COOLDOWN);
        }
    }

    private void advanceProjectiles(double dt) {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.advance(dt);
            if (!p.isSpent()) {
                resolveCollision(p);
            }
            if (p.isSpent() || outOfArena(p)) {
                projectiles.remove(i);
            }
        }
    }

    private void resolveCollision(Projectile p) {
        // A projectile only damages ships on the side opposite its owner.
        List<Ship> targets = (raceA != null && p.getOwner() == raceA) ? sideB
                : (raceB != null && p.getOwner() == raceB) ? sideA : null;
        if (targets == null) {
            // Fall back: hit whichever ship is not owned by the firer.
            targets = new ArrayList<Ship>();
            targets.addAll(sideA);
            targets.addAll(sideB);
        }
        for (Ship ship : targets) {
            if (ship.isDestroyed() || ship.getOwner() == p.getOwner()) {
                continue;
            }
            double dx = ship.getX() - p.getX();
            double dy = ship.getY() - p.getY();
            if (dx * dx + dy * dy <= SHIP_RADIUS * SHIP_RADIUS) {
                applyDamage(ship, p.getDamage());
                p.markSpent();
                return;
            }
        }
    }

    /** Shields soak damage first; the overflow chews into hull. */
    private void applyDamage(Ship ship, int damage) {
        int shields = ship.getShields();
        if (damage <= shields) {
            ship.setShields(shields - damage);
            return;
        }
        int overflow = damage - shields;
        ship.setShields(0);
        ship.setHull(Math.max(0, ship.getHull() - overflow));
    }

    private boolean outOfArena(Projectile p) {
        double margin = 40.0;
        return p.getX() < -margin || p.getY() < -margin
                || p.getX() > arenaWidth + margin || p.getY() > arenaHeight + margin;
    }

    private Ship nearestAlive(Ship from, List<Ship> candidates) {
        Ship best = null;
        double bestD = Double.MAX_VALUE;
        for (Ship c : candidates) {
            if (c.isDestroyed()) {
                continue;
            }
            double dx = c.getX() - from.getX();
            double dy = c.getY() - from.getY();
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    private double engagementRange(Ship ship) {
        int r = ship.getWeaponRange();
        if (r <= 0) {
            return DEFAULT_RANGE;
        }
        return r * RANGE_PER_POINT;
    }

    private void evaluateFinished() {
        boolean aAlive = anyAlive(sideA);
        boolean bAlive = anyAlive(sideB);
        if (aAlive && bAlive) {
            return;
        }
        finished = true;
        if (aAlive) {
            winner = raceA;
        } else if (bAlive) {
            winner = raceB;
        } else {
            winner = null; // mutual annihilation
        }
    }

    private boolean anyAlive(List<Ship> ships) {
        for (Ship s : ships) {
            if (!s.isDestroyed()) {
                return true;
            }
        }
        return false;
    }

    public List<Projectile> getProjectiles() {
        return projectiles;
    }

    /** Both sides combined, in side-A-then-side-B order, for rendering. */
    public List<Ship> getShips() {
        List<Ship> all = new ArrayList<Ship>(sideA.size() + sideB.size());
        all.addAll(sideA);
        all.addAll(sideB);
        return all;
    }

    public boolean isFinished() {
        return finished;
    }

    public Race getWinner() {
        return winner;
    }
}
