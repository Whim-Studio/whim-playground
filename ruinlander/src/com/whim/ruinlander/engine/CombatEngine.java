package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.Armor;
import com.whim.ruinlander.domain.Enemy;
import com.whim.ruinlander.domain.Inventory;
import com.whim.ruinlander.domain.Player;
import com.whim.ruinlander.domain.Position;
import com.whim.ruinlander.domain.StatType;
import com.whim.ruinlander.domain.Weapon;
import com.whim.ruinlander.domain.WeaponClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Turn-based tactical combat math. All randomness flows through the single
 * seeded {@link Random} supplied at construction; no Swing imports.
 *
 * <p>Hit chance = clamp(weaponAccuracy − distancePenalty, 5%, 95%), damage is
 * reduced by the target's armor reduction. Melee requires adjacency; firearms
 * have a range and consume ammo from the inventory.
 */
public class CombatEngine {

    /** Action points the player receives at the start of each combat round. */
    public static final int PLAYER_AP_PER_ROUND = 8;

    private static final int MOVE_AP_COST = 1;
    private static final double MIN_HIT = 0.05;
    private static final double MAX_HIT = 0.95;
    private static final double FIREARM_RANGE_PENALTY = 0.07; // per tile beyond the first

    private final Random rng;

    public CombatEngine(Random rng) {
        this.rng = rng;
    }

    /** Begin an encounter: lay out the player and enemies on the combat grid. */
    public CombatState start(Player p, List<Enemy> enemies) {
        Position playerPos = new Position(1, CombatState.HEIGHT / 2);
        // Spread enemies down the right column of the arena.
        int n = enemies.size();
        int top = Math.max(0, (CombatState.HEIGHT - n) / 2);
        for (int i = 0; i < n; i++) {
            int ex = CombatState.WIDTH - 2;
            int ey = Math.min(CombatState.HEIGHT - 1, top + i);
            enemies.get(i).setPosition(new Position(ex, ey));
        }
        p.setActionPoints(PLAYER_AP_PER_ROUND);
        CombatState s = new CombatState(enemies, playerPos);
        s.setPlayerTurn(true);
        s.log("Encounter! " + describe(enemies) + " block your path.");
        return s;
    }

    /** Move the player one tile in combat (dx,dy in {-1,0,1}); costs 1 AP. */
    public AttackOutcome playerMove(CombatState s, Player p, int dx, int dy) {
        if (p.getActionPoints() < MOVE_AP_COST) {
            return new AttackOutcome(false, 0, false, "No action points left.");
        }
        Position cur = s.getPlayerPos();
        int nx = cur.x + dx;
        int ny = cur.y + dy;
        if (nx < 0 || ny < 0 || nx >= CombatState.WIDTH || ny >= CombatState.HEIGHT) {
            return new AttackOutcome(false, 0, false, "You can't move there.");
        }
        if (occupied(s, nx, ny)) {
            return new AttackOutcome(false, 0, false, "An enemy blocks that tile.");
        }
        s.setPlayerPos(new Position(nx, ny));
        p.setActionPoints(p.getActionPoints() - MOVE_AP_COST);
        return new AttackOutcome(false, 0, false, "You reposition.");
    }

    /** Resolve a player attack against {@code target}. */
    public AttackOutcome playerAttack(CombatState s, Player p, Enemy target) {
        if (target == null || target.isDead()) {
            return new AttackOutcome(false, 0, false, "No valid target.");
        }
        Weapon w = p.getEquippedWeapon();
        if (w == null) {
            return new AttackOutcome(false, 0, false, "You have no weapon equipped.");
        }
        if (p.getActionPoints() < w.getApCost()) {
            return new AttackOutcome(false, 0, false, "Not enough AP for " + w.getName() + ".");
        }

        int distance = s.getPlayerPos().manhattan(target.getPosition());
        if (distance > w.getRange()) {
            return new AttackOutcome(false, 0, false,
                    "Out of range (" + distance + " > " + w.getRange() + "). Move closer.");
        }

        // Firearms consume ammo.
        if (w.getWeaponClass() == WeaponClass.FIREARM && w.usesAmmo()) {
            Inventory inv = p.getInventory();
            if (inv.count(w.getAmmoItemId()) <= 0) {
                return new AttackOutcome(false, 0, false, "Out of ammo for the " + w.getName() + "!");
            }
            inv.remove(w.getAmmoItemId(), 1);
        }

        // Spend AP regardless of hit/miss.
        p.setActionPoints(p.getActionPoints() - w.getApCost());

        double penalty = (w.getWeaponClass() == WeaponClass.FIREARM)
                ? Math.max(0, distance - 1) * FIREARM_RANGE_PENALTY
                : 0.0;
        double hitChance = clamp(w.getAccuracy() - penalty, MIN_HIT, MAX_HIT);

        if (rng.nextDouble() > hitChance) {
            return new AttackOutcome(false, 0, false, "You attack " + target.getName() + " and miss.");
        }

        int dmg = (int) Math.round(w.getDamage() * (1.0 - target.getArmorReduction()));
        dmg = Math.max(1, dmg);
        target.setHp(target.getHp() - dmg);
        boolean killed = target.isDead();
        String msg = "You hit " + target.getName() + " for " + dmg
                + (killed ? " — killed!" : " (" + target.getHp() + " HP left).");
        return new AttackOutcome(true, dmg, killed, msg);
    }

    /**
     * Run the enemies' turn: each living enemy advances toward the player and
     * attacks when adjacent. Returns the per-action outcomes in order. Refreshes
     * the player's AP for the next round.
     */
    public List<AttackOutcome> enemyTurn(CombatState s, Player p) {
        List<AttackOutcome> outcomes = new ArrayList<AttackOutcome>();
        double playerReduction = playerArmorReduction(p);

        for (Enemy e : s.aliveEnemies()) {
            int dist = s.getPlayerPos().manhattan(e.getPosition());
            if (dist > 1) {
                stepToward(s, e);
                dist = s.getPlayerPos().manhattan(e.getPosition());
            }
            if (dist <= 1) {
                double hitChance = clamp(e.getAccuracy(), MIN_HIT, MAX_HIT);
                if (rng.nextDouble() <= hitChance) {
                    int dmg = (int) Math.round(e.getAttack() * (1.0 - playerReduction));
                    dmg = Math.max(1, dmg);
                    p.addStat(StatType.HEALTH, -dmg);
                    outcomes.add(new AttackOutcome(true, dmg, p.isDead(),
                            e.getName() + " hits you for " + dmg + "."));
                } else {
                    outcomes.add(new AttackOutcome(false, 0, false, e.getName() + " lunges and misses."));
                }
            } else {
                outcomes.add(new AttackOutcome(false, 0, false, e.getName() + " closes in."));
            }
            if (p.isDead()) {
                break;
            }
        }

        s.nextRound();
        p.setActionPoints(PLAYER_AP_PER_ROUND);
        s.setPlayerTurn(true);
        for (AttackOutcome o : outcomes) {
            s.log(o.getMessage());
        }
        return outcomes;
    }

    public boolean playerWon(CombatState s) {
        return s.aliveEnemies().isEmpty();
    }

    public boolean playerLost(Player p) {
        return p.isDead();
    }

    // ---- helpers -----------------------------------------------------------

    private void stepToward(CombatState s, Enemy e) {
        Position pp = s.getPlayerPos();
        Position ep = e.getPosition();
        int dx = Integer.signum(pp.x - ep.x);
        int dy = Integer.signum(pp.y - ep.y);
        // Prefer the larger axis gap; fall back to the other if blocked.
        if (Math.abs(pp.x - ep.x) >= Math.abs(pp.y - ep.y)) {
            if (tryMoveEnemy(s, e, ep.x + dx, ep.y)) return;
            if (tryMoveEnemy(s, e, ep.x, ep.y + dy)) return;
        } else {
            if (tryMoveEnemy(s, e, ep.x, ep.y + dy)) return;
            if (tryMoveEnemy(s, e, ep.x + dx, ep.y)) return;
        }
    }

    private boolean tryMoveEnemy(CombatState s, Enemy e, int nx, int ny) {
        if (nx < 0 || ny < 0 || nx >= CombatState.WIDTH || ny >= CombatState.HEIGHT) {
            return false;
        }
        if (s.getPlayerPos().x == nx && s.getPlayerPos().y == ny) {
            return false; // don't step onto the player; attack happens from adjacency
        }
        if (occupied(s, nx, ny)) {
            return false;
        }
        e.setPosition(new Position(nx, ny));
        return true;
    }

    private boolean occupied(CombatState s, int x, int y) {
        for (Enemy e : s.aliveEnemies()) {
            Position ep = e.getPosition();
            if (ep != null && ep.x == x && ep.y == y) {
                return true;
            }
        }
        return false;
    }

    private double playerArmorReduction(Player p) {
        Armor a = p.getEquippedArmor();
        return a == null ? 0.0 : a.getDamageReduction();
    }

    private String describe(List<Enemy> enemies) {
        if (enemies.size() == 1) {
            return "A " + enemies.get(0).getName();
        }
        return enemies.size() + " hostiles";
    }

    private double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
