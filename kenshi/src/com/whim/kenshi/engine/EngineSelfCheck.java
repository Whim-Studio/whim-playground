package com.whim.kenshi.engine;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.api.Enums.WeaponClass;
import com.whim.kenshi.api.Views;
import com.whim.kenshi.domain.Character;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Headless proof that the engine works end to end: a staged one-on-one duel that
 * exercises the hit-chance formula, weighted body-part damage, bleed, skill XP
 * and the DOWNED/DEAD transitions, followed by a live run of the full tick loop
 * (AI + pathfinding + combat + survival) for a few seconds against a real world.
 *
 * <p>Run: {@code java -cp <out> com.whim.kenshi.engine.EngineSelfCheck}</p>
 */
public final class EngineSelfCheck {

    private EngineSelfCheck() {}

    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 20260705L;
        System.out.println("=== Kenshi Engine self-check (seed " + seed + ") ===");

        stagedDuel(seed);
        liveWorld(seed);

        System.out.println("\n=== self-check complete ===");
    }

    // ------------------------------------------------------------------
    // 1. A deterministic duel to prove the combat + survival maths.
    // ------------------------------------------------------------------
    private static void stagedDuel(long seed) {
        System.out.println("\n--- Sample fight: a Dust Bandit ambushes a lone Drifter ---");
        Random rng = new Random(seed);
        EventLog log = new EventLog();
        CombatSystem combat = new CombatSystem(rng, log);
        SurvivalSystem survival = new SurvivalSystem(log);

        Character bandit = fighter("bandit_x", "Grisly Ned", FactionId.DUST_BANDITS, 0, 0,
                WeaponClass.TWO_HANDED, 32, 30, 40);
        Character drifter = fighter("drifter_x", "Wandering Kral", FactionId.DRIFTERS, 10, 0,
                WeaponClass.ONE_HANDED, 14, 12, 18);

        double hc = combat.hitChance(bandit, drifter);
        System.out.printf("  %s (MeleeAtk %d) vs %s (MeleeDef %d) -> base hit chance %.0f%%%n",
                bandit.name(), bandit.skills().level(SkillType.MELEE_ATTACK),
                drifter.name(), drifter.skills().level(SkillType.MELEE_DEFENCE), hc * 100.0);

        long tick = 0;
        int round = 0;
        while (round < 40) {
            round++;
            // The bandit presses the attack; the drifter fights back while able.
            combat.resolveSwing(tick, bandit, drifter);
            if (drifter.moveState() != MoveState.DEAD && drifter.moveState() != MoveState.DOWNED) {
                combat.resolveSwing(tick, drifter, bandit);
            }
            // Blood/bleed/heal tick for both (both are "in combat").
            survival.step(tick, bandit, Config.WORLD_SECONDS_PER_TICK, true);
            survival.step(tick, drifter, Config.WORLD_SECONDS_PER_TICK, true);
            tick++;
            MoveState ds = drifter.moveState();
            if (ds == MoveState.DEAD || ds == MoveState.DOWNED) {
                break;
            }
        }

        for (EventLog.Line ln : log.copy()) {
            System.out.println("    [" + ln.tick + "] " + ln.text);
        }
        System.out.println("  Result after " + round + " exchanges:");
        printAnatomy("    " + drifter.name(), drifter);
        printAnatomy("    " + bandit.name(), bandit);
        System.out.printf("    %s trained MeleeAttack %d->%d, Strength ->%d; %s trained Toughness ->%d%n",
                bandit.name(), 32, bandit.skills().level(SkillType.MELEE_ATTACK),
                bandit.skills().level(SkillType.STRENGTH),
                drifter.name(), drifter.skills().level(SkillType.TOUGHNESS));
    }

    private static Character fighter(String id, String name, FactionId f, double x, double y,
                                     WeaponClass weapon, int atk, int def, int str) {
        Character c = new Character(id, name, f, x, y);
        c.setWeapon(weapon);
        c.setBlood(Config.BLOOD_MAX);
        c.setHunger(Config.HUNGER_MAX);
        c.skills().setLevel(SkillType.MELEE_ATTACK, atk);
        c.skills().setLevel(SkillType.MELEE_DEFENCE, def);
        c.skills().setLevel(SkillType.STRENGTH, str);
        c.skills().setLevel(SkillType.TOUGHNESS, 20);
        return c;
    }

    private static void printAnatomy(String label, Character c) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" [").append(c.moveState()).append("] blood ")
          .append(Math.round(c.blood())).append('/').append((int) Config.BLOOD_MAX)
          .append(" bleed ").append(String.format("%.1f", c.bleedRate())).append(" | ");
        BodyPart[] parts = BodyPart.values();
        for (int i = 0; i < parts.length; i++) {
            BodyPart p = parts[i];
            sb.append(p.label().substring(0, Math.min(3, p.label().length())))
              .append(' ').append(Math.round(c.anatomy().hp(p)));
            if (c.anatomy().disabled(p)) {
                sb.append('x');
            }
            if (i < parts.length - 1) {
                sb.append(", ");
            }
        }
        System.out.println(sb.toString());
    }

    // ------------------------------------------------------------------
    // 2. Run the real engine loop against a real world for a few seconds.
    // ------------------------------------------------------------------
    private static void liveWorld(long seed) throws InterruptedException {
        System.out.println("\n--- Live world: full tick loop for ~3s at 4x speed ---");
        GameEngine engine = new GameEngine();
        engine.newGame(seed);

        Views.GameStateView s0 = engine.state();
        System.out.println("  World built: " + s0.characters().size() + " characters, "
                + s0.squads().size() + " squads, " + s0.nodes().size() + " nodes, "
                + s0.map().tiles() + "x" + s0.map().tiles() + " tiles.");
        System.out.println("  Faction census: " + factionCensus(s0));

        // Drive the full seam: select the player squad and order them onto the
        // nearest hostile so the live loop exercises orders -> pursue -> path ->
        // combat (not just idle wandering).
        List<String> playerIds = playerIds(s0);
        String target = nearestHostileToPlayers(s0, playerIds);
        engine.setSelection(playerIds);
        if (target != null) {
            engine.orderAttack(playerIds, target);
            System.out.println("  Ordered player squad " + playerIds + " to attack " + target + ".");
        }

        engine.setGameSpeed(4);
        engine.setPaused(false);
        engine.start();

        long wallStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - wallStart < 3000L) {
            Thread.sleep(200L);
        }
        engine.stop();

        Views.GameStateView s = engine.state();
        System.out.printf("  Ran to tick %d (%.0f world-seconds ~ %.1f in-world hours).%n",
                s.tick(), s.worldSeconds(), s.worldSeconds() / 3600.0);
        System.out.println("  Phase: " + s.phase() + ", gameSpeed: " + s.gameSpeed());
        System.out.println("  State census: " + stateCensus(s));

        System.out.println("  Recent events:");
        List<Views.LogView> log = s.log();
        int from = Math.max(0, log.size() - 12);
        for (int i = from; i < log.size(); i++) {
            System.out.println("    [" + log.get(i).tick() + "] " + log.get(i).text());
        }

        // Show one player unit's body chart as the UI would read it.
        Views.CharacterView sample = firstPlayer(s);
        if (sample != null) {
            System.out.println("  Sample player unit body chart:");
            printViewAnatomy("    " + sample.name(), sample);
        }
    }

    private static List<String> playerIds(Views.GameStateView s) {
        java.util.List<String> ids = new java.util.ArrayList<String>();
        List<Views.CharacterView> cs = s.characters();
        for (int i = 0; i < cs.size(); i++) {
            if (cs.get(i).faction() == FactionId.PLAYER) {
                ids.add(cs.get(i).id());
            }
        }
        return ids;
    }

    /** Nearest character hostile to PLAYER, measured from the player centroid. */
    private static String nearestHostileToPlayers(Views.GameStateView s, List<String> playerIds) {
        double cx = 0, cy = 0;
        int n = 0;
        List<Views.CharacterView> cs = s.characters();
        for (int i = 0; i < cs.size(); i++) {
            if (cs.get(i).faction() == FactionId.PLAYER) {
                cx += cs.get(i).x();
                cy += cs.get(i).y();
                n++;
            }
        }
        if (n == 0) {
            return null;
        }
        cx /= n; cy /= n;

        // Which factions are hostile to PLAYER?
        java.util.Set<FactionId> hostile = new java.util.HashSet<FactionId>();
        List<Views.FactionView> fvs = s.factions();
        for (int i = 0; i < fvs.size(); i++) {
            Views.FactionView fv = fvs.get(i);
            if (fv.relationTo(FactionId.PLAYER) == com.whim.kenshi.api.Enums.Relation.HOSTILE) {
                hostile.add(fv.id());
            }
        }

        String best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < cs.size(); i++) {
            Views.CharacterView c = cs.get(i);
            if (!hostile.contains(c.faction()) || !c.alive() || c.downed()) {
                continue;
            }
            double dx = c.x() - cx;
            double dy = c.y() - cy;
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = c.id();
            }
        }
        return best;
    }

    private static String factionCensus(Views.GameStateView s) {
        Map<FactionId, Integer> counts = new LinkedHashMap<FactionId, Integer>();
        List<Views.CharacterView> cs = s.characters();
        for (int i = 0; i < cs.size(); i++) {
            FactionId f = cs.get(i).faction();
            Integer n = counts.get(f);
            counts.put(f, n == null ? 1 : n + 1);
        }
        return counts.toString();
    }

    private static String stateCensus(Views.GameStateView s) {
        int alive = 0, downed = 0, dead = 0, crawling = 0, moving = 0;
        List<Views.CharacterView> cs = s.characters();
        for (int i = 0; i < cs.size(); i++) {
            MoveState ms = cs.get(i).moveState();
            if (ms == MoveState.DEAD) { dead++; }
            else { alive++; }
            if (ms == MoveState.DOWNED) { downed++; }
            if (ms == MoveState.CRAWLING) { crawling++; }
            if (ms == MoveState.MOVING) { moving++; }
        }
        return "alive=" + alive + " (downed=" + downed + ", crawling=" + crawling
                + ", moving=" + moving + "), dead=" + dead;
    }

    private static Views.CharacterView firstPlayer(Views.GameStateView s) {
        List<Views.CharacterView> cs = s.characters();
        for (int i = 0; i < cs.size(); i++) {
            if (cs.get(i).faction() == FactionId.PLAYER) {
                return cs.get(i);
            }
        }
        return null;
    }

    private static void printViewAnatomy(String label, Views.CharacterView c) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" [").append(c.moveState()).append("/").append(c.aiState())
          .append("] hunger ").append(Math.round(c.hunger())).append('/')
          .append((int) c.hungerMax()).append(" blood ").append(Math.round(c.blood()))
          .append(" | ");
        BodyPart[] parts = BodyPart.values();
        for (int i = 0; i < parts.length; i++) {
            BodyPart p = parts[i];
            sb.append(p.label()).append(' ').append(Math.round(c.partHp(p)));
            if (i < parts.length - 1) {
                sb.append(", ");
            }
        }
        System.out.println(sb.toString());
    }
}
