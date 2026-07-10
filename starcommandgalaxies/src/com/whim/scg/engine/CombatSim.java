package com.whim.scg.engine;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Vec2;
import com.whim.scg.model.CombatModel;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.GameState;
import com.whim.scg.model.ProjectileModel;
import com.whim.scg.model.RoomModel;
import com.whim.scg.model.ShipModel;
import com.whim.scg.model.WeaponModel;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** Real-time (pausable) space-combat simulation. */
final class CombatSim {
    private final Random rng;

    // normalized-space anchors (UI scales these)
    static final double PLAYER_X = 0.16, ENEMY_X = 0.84, MID_Y = 0.5;
    private static final double CHARGE_RATE = 3.2;   // charge units / sec at power 1, eff 1
    private static final double PROJ_SPEED = 0.55;   // normalized / sec
    private static final double FIRE_DPS = 0.9;
    private static final double FIRE_DURATION = 9.0;
    private static final double BREACH_DURATION = 7.0;

    CombatSim(Random rng) { this.rng = rng; }

    void tick(GameState st, double dt) {
        CombatModel cb = st.combat;
        if (cb == null || cb.over) return;
        ShipModel player = cb.playerShip;
        ShipModel enemy = cb.enemyShip;

        chargeAndFire(player, enemy, cb, dt, true);
        chargeAndFire(enemy, player, cb, dt, false);
        moveProjectiles(st, cb, dt);
        regen(player, dt);
        regen(enemy, dt);
        hazards(st, player, dt);
        hazards(st, enemy, dt);
        enemyAi(enemy, player, dt);

        if (enemy.hull <= 0 && !cb.over) {
            cb.over = true;
            cb.playerWon = true;
            enemy.hull = 0;
        } else if (player.hull <= 0 && !cb.over) {
            cb.over = true;
            cb.playerWon = false;
            player.hull = 0;
        }
    }

    // ---- weapon charge + auto fire --------------------------------------
    private void chargeAndFire(ShipModel shooter, ShipModel target, CombatModel cb, double dt, boolean fromPlayer) {
        double weaponsEff = Rules.effectiveness(shooter, Enums.RoomType.WEAPONS);
        double mult = 0.6 + 0.6 * weaponsEff; // manned weapons room speeds charge
        for (WeaponModel w : shooter.weapons) {
            if (w.powered <= 0) { continue; }
            w.charge += dt * CHARGE_RATE * w.powered * mult;
            if (w.charge >= w.chargeMax) {
                // auto-fire if a target is set (or pick one)
                int targetRoom = w.targetRoomId;
                if (targetRoom < 0) targetRoom = pickTargetRoom(target);
                if (targetRoom >= 0) {
                    spawnProjectile(cb, w, fromPlayer, targetRoom);
                    w.charge = 0;
                }
            }
        }
    }

    /** Manual fire path (fireWeapon intent). Returns true if it launched. */
    boolean manualFire(CombatModel cb, int slot) {
        if (cb == null || cb.over) return false;
        ShipModel player = cb.playerShip;
        WeaponModel w = weapon(player, slot);
        if (w == null || w.powered <= 0 || w.charge < w.chargeMax) return false;
        int tr = w.targetRoomId >= 0 ? w.targetRoomId : pickTargetRoom(cb.enemyShip);
        if (tr < 0) return false;
        spawnProjectile(cb, w, true, tr);
        w.charge = 0;
        return true;
    }

    private void spawnProjectile(CombatModel cb, WeaponModel w, boolean fromPlayer, int targetRoomId) {
        ProjectileModel p = new ProjectileModel();
        p.type = w.type;
        p.fromPlayer = fromPlayer;
        p.targetRoomId = targetRoomId;
        p.damage = w.damage;
        p.piercesShields = w.piercesShields;
        double sx = fromPlayer ? PLAYER_X : ENEMY_X;
        double ex = fromPlayer ? ENEMY_X : PLAYER_X;
        double sy = MID_Y + (rng.nextDouble() - 0.5) * 0.18;
        p.pos = new Vec2(sx, sy);
        double dx = ex - sx;
        double ey = MID_Y + (rng.nextDouble() - 0.5) * 0.12;
        Vec2 dir = new Vec2(dx, ey - sy).norm();
        p.vel = dir.scale(PROJ_SPEED);
        cb.projectiles.add(p);
    }

    private void moveProjectiles(GameState st, CombatModel cb, double dt) {
        Iterator<ProjectileModel> it = cb.projectiles.iterator();
        while (it.hasNext()) {
            ProjectileModel p = it.next();
            p.pos = p.pos.add(p.vel.scale(dt));
            boolean arrived = p.fromPlayer ? p.pos.x >= ENEMY_X : p.pos.x <= PLAYER_X;
            if (arrived) {
                ShipModel target = p.fromPlayer ? cb.enemyShip : cb.playerShip;
                resolveHit(st, target, p);
                it.remove();
            } else if (p.pos.x < -0.1 || p.pos.x > 1.1) {
                it.remove();
            }
        }
    }

    private void resolveHit(GameState st, ShipModel target, ProjectileModel p) {
        // evasion (engines) may cause a miss
        if (rng.nextDouble() < Rules.evasion(target)) {
            if (p.fromPlayer) st.logLine(target.name + " evaded the shot");
            return;
        }
        int dmg = p.damage;
        // shields absorb non-piercing damage
        if (!p.piercesShields && target.shields > 0) {
            int absorbed = Math.min(target.shields, dmg);
            target.shields -= absorbed;
            dmg -= absorbed;
            if (dmg <= 0) return;
        }
        RoomModel room = target.room(p.targetRoomId);
        if (room == null) room = pickAnyRoom(target);
        if (room != null) {
            room.hp = Math.max(0, room.hp - dmg);
            maybeIgnite(target, room, p);
        }
        target.hull = Math.max(0, target.hull - dmg);
        if (p.fromPlayer) st.logLine("Hit " + target.name + (room != null ? " " + room.type : "") + " (-" + dmg + ")");
    }

    private void maybeIgnite(ShipModel ship, RoomModel room, ProjectileModel p) {
        double fireChance = p.type == Enums.WeaponType.LASER || p.type == Enums.WeaponType.BEAM ? 0.28 : 0.14;
        double breachChance = p.type == Enums.WeaponType.PLASMA_TORPEDO || p.type == Enums.WeaponType.MISSILE ? 0.30 : 0.12;
        if (!room.onFire && rng.nextDouble() < fireChance && ship.oxygen > 5) {
            room.onFire = true;
            room.fireTime = 0;
        }
        if (!room.breached && rng.nextDouble() < breachChance) {
            room.breached = true;
            room.fireTime = 0;
            if (room.onFire) room.onFire = false; // vacuum snuffs fire
        }
    }

    // ---- shields / oxygen / medbay regen --------------------------------
    private void regen(ShipModel ship, double dt) {
        double shieldEff = Rules.effectiveness(ship, Enums.RoomType.SHIELDS);
        if (shieldEff > 0 && ship.shields < ship.maxShields) {
            ship.shieldAccum += dt * (0.35 + 0.5 * shieldEff);
            while (ship.shieldAccum >= 1.0 && ship.shields < ship.maxShields) {
                ship.shieldAccum -= 1.0;
                ship.shields++;
            }
        }
        // oxygen recovery when OXYGEN room works and no active breach
        boolean breach = false;
        for (RoomModel r : ship.rooms) if (r.breached) { breach = true; break; }
        double oxyEff = Rules.effectiveness(ship, Enums.RoomType.OXYGEN);
        if (!breach && ship.oxygen < 100) {
            double rate = 3.0 * (0.4 + oxyEff);
            ship.oxygen = Math.min(100, ship.oxygen + rate * dt);
        }
        // medbay heals living crew stationed in medbay
        RoomModel med = ship.firstRoom(Enums.RoomType.MEDBAY);
        double medEff = Rules.effectiveness(ship, med);
        if (medEff > 0) {
            for (Integer id : med.crewIds) {
                CrewModel c = ship.crewById(id);
                if (c != null && c.alive() && c.hp < c.maxHp) c.heal(4 * medEff * dt);
            }
        }
    }

    // ---- fires / breaches -----------------------------------------------
    private void hazards(GameState st, ShipModel ship, double dt) {
        for (RoomModel room : ship.rooms) {
            if (room.onFire) {
                room.fireTime += dt;
                room.dmgAccum += FIRE_DPS * dt;
                while (room.dmgAccum >= 1.0 && room.hp > 0) { room.hp--; room.dmgAccum -= 1.0; }
                ship.oxygen = Math.max(0, ship.oxygen - 2.0 * dt);
                damageOccupants(ship, room, 3.0 * dt);
                // spread to a neighbouring room
                if (rng.nextDouble() < 0.10 * dt && ship.oxygen > 5) {
                    RoomModel n = adjacentRoom(ship, room);
                    if (n != null && !n.onFire && !n.breached) { n.onFire = true; n.fireTime = 0; }
                }
                if (room.fireTime > FIRE_DURATION || ship.oxygen <= 0) { room.onFire = false; room.dmgAccum = 0; }
            }
            if (room.breached) {
                room.fireTime += dt;
                ship.oxygen = Math.max(0, ship.oxygen - 4.0 * dt);
                damageOccupants(ship, room, 2.0 * dt);
                if (room.fireTime > BREACH_DURATION) { room.breached = false; room.fireTime = 0; }
            }
        }
        // suffocation damage when oxygen is gone
        if (ship.oxygen <= 0) {
            for (CrewModel c : ship.crew) if (c.alive()) c.hurt(2.0 * dt);
        }
    }

    private void damageOccupants(ShipModel ship, RoomModel room, double dmg) {
        for (Integer id : room.crewIds) {
            CrewModel c = ship.crewById(id);
            if (c != null && c.alive()) c.hurt(dmg);
        }
    }

    private RoomModel adjacentRoom(ShipModel ship, RoomModel room) {
        RoomModel best = null;
        for (RoomModel r : ship.rooms) {
            if (r == room || r.origin == null || room.origin == null) continue;
            boolean near = Math.abs(r.origin.x - room.origin.x) <= room.w + 1
                    && Math.abs(r.origin.y - room.origin.y) <= room.h + 1;
            if (near) { best = r; if (rng.nextBoolean()) break; }
        }
        return best;
    }

    // ---- enemy AI -------------------------------------------------------
    private void enemyAi(ShipModel enemy, ShipModel player, double dt) {
        enemy.shieldAccum += 0; // no-op keeps field referenced
        // retarget weapons periodically toward the player's key systems
        for (WeaponModel w : enemy.weapons) {
            if (w.targetRoomId < 0 || player.room(w.targetRoomId) == null) {
                w.targetRoomId = pickTargetRoom(player);
            }
        }
    }

    // ---- helpers --------------------------------------------------------
    private int pickTargetRoom(ShipModel ship) {
        // prefer shields, then weapons, then any powered room, else any room
        RoomModel r = ship.firstRoom(Enums.RoomType.SHIELDS);
        if (r == null || r.hp <= 0) r = ship.firstRoom(Enums.RoomType.WEAPONS);
        if (r == null || r.hp <= 0) r = pickAnyRoom(ship);
        return r == null ? -1 : r.id;
    }

    private RoomModel pickAnyRoom(ShipModel ship) {
        List<RoomModel> rooms = ship.rooms;
        if (rooms.isEmpty()) return null;
        return rooms.get(rng.nextInt(rooms.size()));
    }

    private static WeaponModel weapon(ShipModel ship, int slot) {
        for (WeaponModel w : ship.weapons) if (w.slot == slot) return w;
        return null;
    }
}
