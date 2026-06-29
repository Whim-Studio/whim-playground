package com.whim.nextrun.engine;

import com.whim.nextrun.domain.Enemy;
import com.whim.nextrun.domain.EntityType;
import com.whim.nextrun.domain.GridMap;
import com.whim.nextrun.domain.HeroClass;
import com.whim.nextrun.domain.Player;
import com.whim.nextrun.domain.Position;
import com.whim.nextrun.domain.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The authority over a run. The UI only reads this object and calls its action
 * methods — all rules (turn costs, the doom/wave clock, resolution, victory and
 * defeat) live here so the presentation layer stays calculation-free.
 */
public final class GameState {

    public enum Status { PLAYING, WON, LOST }

    // ---- victory thresholds (the four paths to win) ----
    public static final int GOLD_GOAL = 160;       // economy
    public static final int FORGE_GOAL = 6;        // crafting (weapon+armor bonus)
    public static final int BUILD_GOAL = 4;        // settlement (structures)
    // escape: reach the portal. combat: implicit via surviving the waves.

    public final GridMap map;
    public final Player player;
    private final Random rng;
    private final Resolution resolution;

    public long turn = 0;
    public int waveNumber = 0;
    public int turnsUntilWave;
    private int waveInterval;

    public Status status = Status.PLAYING;
    public String outcome = "";

    private final List<String> log = new ArrayList<String>();

    public GameState(HeroClass heroClass, int width, int height, long seed) {
        this.rng = new Random(seed);
        Position start = new Position(width / 2, height / 2);
        this.map = new MapGenerator(rng).generate(width, height, start);
        this.player = new Player(heroClass, start);
        this.resolution = new Resolution(rng);
        this.map.at(start).type = EntityType.EMPTY;
        this.map.reveal(start, 2);
        this.waveInterval = 22;
        this.turnsUntilWave = waveInterval;
        log("You are a " + heroClass.label() + ". " + heroClass.passive());
        log("Survive the doom waves. Find gold, forge gear, build, or reach the portal.");
    }

    public List<String> log() { return log; }

    private void log(String msg) {
        log.add(msg);
        // keep the log bounded
        while (log.size() > 200) log.remove(0);
    }

    /** Advance the clock by {@code turns}; trigger a wave when the counter expires. */
    private void spend(int turns) {
        turn += turns;
        turnsUntilWave -= turns;
        while (turnsUntilWave <= 0 && status == Status.PLAYING) {
            triggerWave();
            turnsUntilWave += waveInterval;
        }
    }

    private void triggerWave() {
        waveNumber++;
        int tier = 1 + waveNumber; // exponential strength comes from WaveSpawner
        int count = 3 + waveNumber;
        int placed = WaveSpawner.spawnWave(map, tier, count, rng, player.pos);
        // each wave comes a little faster — the noose tightens
        waveInterval = Math.max(10, waveInterval - 1);
        log("*** WAVE " + waveNumber + "! " + placed
            + " tier-" + tier + " fiends erupt across the land. ***");
    }

    // ---------------------------------------------------------------- actions

    public boolean canAct() { return status == Status.PLAYING; }

    public void move(int dx, int dy) {
        if (!canAct()) return;
        Position target = player.pos.translate(dx, dy);
        if (!map.inBounds(target)) { log("The world ends there."); return; }
        Tile t = map.at(target);
        if (t.type == EntityType.ENEMY) {
            log("A " + t.enemy.name + " blocks the way — Fight, Bribe, or Sneak.");
            return;
        }
        player.pos = target;
        map.reveal(target, 2);
        spend(ActionCosts.cost(ActionType.MOVE, player));
        if (t.type == EntityType.PORTAL) {
            win("escaped through the portal");
            return;
        }
        afterAction();
    }

    public void gather() {
        if (!canAct()) return;
        Tile t = map.at(player.pos);
        if (t.type != EntityType.RESOURCE) { log("Nothing to gather here."); return; }
        int amount = t.payload;
        if (player.heroClass == HeroClass.PEASANT) amount += 2; // Scavenger
        player.materials += amount;
        t.type = EntityType.EMPTY;
        t.payload = 0;
        spend(ActionCosts.cost(ActionType.GATHER, player));
        log("Gathered " + amount + " materials (total " + player.materials + ").");
        afterAction();
    }

    public void loot() {
        if (!canAct()) return;
        Tile t = map.at(player.pos);
        if (t.type != EntityType.GOLD_PILE) { log("No gold here."); return; }
        player.gold += t.payload;
        spend(ActionCosts.cost(ActionType.LOOT, player));
        log("Looted " + t.payload + " gold (total " + player.gold + ").");
        t.type = EntityType.EMPTY;
        t.payload = 0;
        afterAction();
    }

    public void explore() {
        if (!canAct()) return;
        Tile t = map.at(player.pos);
        if (t.type != EntityType.RUIN) { log("No ruin to explore here."); return; }
        spend(ActionCosts.cost(ActionType.EXPLORE, player));
        int roll = rng.nextInt(3);
        if (roll == 0) {
            int g = 10 + rng.nextInt(20);
            player.gold += g;
            log("The ruin yields a cache of " + g + " gold.");
        } else if (roll == 1) {
            int m = 3 + rng.nextInt(4);
            player.materials += m;
            log("The ruin yields " + m + " rare materials.");
        } else {
            player.maxHp += 2;
            player.heal(player.maxHp);
            log("An old shrine restores you fully and raises max HP by 2.");
        }
        t.type = EntityType.EMPTY;
        afterAction();
    }

    public void craftWeapon() {
        if (!canAct()) return;
        if (player.materials < 3) { log("Need 3 materials to forge a weapon."); return; }
        player.materials -= 3;
        player.weaponBonus += 2;
        spend(ActionCosts.cost(ActionType.CRAFT_WEAPON, player));
        log("Forged a sharper weapon (+2 attack, now +" + player.weaponBonus + ").");
        checkForgeVictory();
        afterAction();
    }

    public void craftArmor() {
        if (!canAct()) return;
        if (player.materials < 3) { log("Need 3 materials to forge armor."); return; }
        player.materials -= 3;
        player.armorBonus += 2;
        spend(ActionCosts.cost(ActionType.CRAFT_ARMOR, player));
        log("Forged sturdier armor (+2 defense, now +" + player.armorBonus + ").");
        checkForgeVictory();
        afterAction();
    }

    public void build() {
        if (!canAct()) return;
        Tile t = map.at(player.pos);
        if (t.type != EntityType.EMPTY) { log("Can only build on open ground."); return; }
        if (player.materials < 4 || player.gold < 10) {
            log("A structure needs 4 materials and 10 gold.");
            return;
        }
        player.materials -= 4;
        player.gold -= 10;
        player.structuresBuilt++;
        int reward = player.heroClass == HeroClass.LEADER ? 6 : 4; // Rally
        player.maxHp += reward;
        player.heal(reward);
        t.type = EntityType.STRUCTURE;
        t.structureName = "Outpost " + player.structuresBuilt;
        spend(ActionCosts.cost(ActionType.BUILD, player));
        log("Raised " + t.structureName + " (+" + reward + " max HP). "
            + player.structuresBuilt + "/" + BUILD_GOAL + " toward a settlement.");
        if (player.structuresBuilt >= BUILD_GOAL) { win("forged a thriving settlement"); return; }
        afterAction();
    }

    public void rest() {
        if (!canAct()) return;
        int heal = 4 + player.maxHp / 5;
        player.heal(heal);
        spend(ActionCosts.cost(ActionType.REST, player));
        log("You rest and recover " + heal + " HP.");
        afterAction();
    }

    // ---- enemy resolution (acts on an adjacent enemy) ----

    private Tile adjacentEnemyTile() {
        int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };
        for (int[] d : dirs) {
            Position p = player.pos.translate(d[0], d[1]);
            if (map.inBounds(p) && map.at(p).type == EntityType.ENEMY) {
                return map.at(p);
            }
        }
        return null;
    }

    public void fight() { resolveAdjacent(ActionType.FIGHT); }
    public void bribe() { resolveAdjacent(ActionType.BRIBE); }
    public void sneak() { resolveAdjacent(ActionType.SNEAK); }

    private void resolveAdjacent(ActionType how) {
        if (!canAct()) return;
        Tile t = adjacentEnemyTile();
        if (t == null) { log("No adjacent enemy to face."); return; }
        Enemy e = t.enemy;
        Resolution.Outcome o;
        switch (how) {
            case BRIBE: o = resolution.bribe(player, e); break;
            case SNEAK: o = resolution.sneak(player, e); break;
            default:    o = resolution.fight(player, e); break;
        }
        log(o.message);
        spend(ActionCosts.cost(how, player));
        if (o.playerDied) { lose("slain by the " + e.name); return; }
        if (o.resolved) {
            // reward depends on how it ended
            if (how == ActionType.FIGHT) {
                int g = 4 + e.tier * 3;
                player.gold += g;
                player.maxHp += 1;
                log("Spoils: " + g + " gold.");
            }
            t.type = EntityType.EMPTY;
            t.enemy = null;
        }
        afterAction();
    }

    // ---------------------------------------------------------------- checks

    private void afterAction() {
        if (status != Status.PLAYING) return;
        if (player.isDead()) { lose("your wounds overcame you"); return; }
        if (player.gold >= GOLD_GOAL) { win("amassed a dragon's hoard"); return; }
    }

    private void checkForgeVictory() {
        if (player.weaponBonus + player.armorBonus >= FORGE_GOAL * 2
                && player.weaponBonus >= FORGE_GOAL && player.armorBonus >= FORGE_GOAL) {
            win("forged a legendary panoply");
        }
    }

    private void win(String how) {
        status = Status.WON;
        outcome = "VICTORY — you " + how + " after " + turn
                + " turns and " + waveNumber + " waves.";
        log("*** " + outcome + " ***");
    }

    private void lose(String how) {
        status = Status.LOST;
        outcome = "DEFEAT — " + how + " on turn " + turn
                + " (wave " + waveNumber + ").";
        log("*** " + outcome + " ***");
    }

    // ---------------------------------------------------------------- helpers for UI

    public boolean onResource() { return map.at(player.pos).type == EntityType.RESOURCE; }
    public boolean onGold()     { return map.at(player.pos).type == EntityType.GOLD_PILE; }
    public boolean onRuin()     { return map.at(player.pos).type == EntityType.RUIN; }
    public boolean onEmpty()    { return map.at(player.pos).type == EntityType.EMPTY; }
    public boolean enemyAdjacent() { return adjacentEnemyTile() != null; }

    public Enemy adjacentEnemy() {
        Tile t = adjacentEnemyTile();
        return t == null ? null : t.enemy;
    }
}
