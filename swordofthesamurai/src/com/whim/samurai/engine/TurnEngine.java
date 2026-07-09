package com.whim.samurai.engine;

import com.whim.samurai.model.Clan;
import com.whim.samurai.model.FamilyMember;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;

import java.util.ArrayList;
import java.util.List;

/**
 * Advances the strategic clock by one season and runs the light background
 * simulation: rice-tax income, aging, and cheap clan/rival AI.
 *
 * The original runs in real time (design ref §1.3) with no explicit calendar;
 * we discretise it into seasonal turns for a clean turn/action structure and
 * clearly label that as our own approximation. Family matters (marriage, births,
 * succession) are deliberately NOT handled here — a sibling engine owns them.
 */
public final class TurnEngine {
    private TurnEngine() { }

    /** Convenience overload honouring the documented {@code advanceSeason(GameState)} signature. */
    public static String advanceSeason(GameState s) { return advanceSeason(s, new Rng()); }

    /**
     * Advance one season. Returns a short human-readable log of what happened,
     * which the map surfaces to the player.
     */
    public static String advanceSeason(GameState s, Rng rng) {
        StringBuilder log = new StringBuilder();
        Samurai you = s.player;

        // --- rice-tax income from directly-held fiefs (design ref §1.1) --------
        // Approximation: koku += sum(rice * development) / 10 across your fiefs.
        int income = 0;
        for (int fid : you.fiefs) {
            Province p = s.province(fid);
            if (p != null) income += p.rice * p.development / 10;
        }
        you.koku += income;
        if (income > 0) log.append("Rice tax +").append(income).append(" koku. ");

        // --- clock: advance one season; a WINTER step rolls the year over ------
        boolean yearRolls = s.calendar.season.rolloverToNext();
        s.calendar.advance();

        if (yearRolls) {
            you.age++;
            // Aging dulls the physical (arcade) skills past middle age (design ref §1.3, §4.4).
            if (you.age > 50 && rng.chance(0.5)) {
                if (you.swordsmanship > 3) you.swordsmanship--;
                log.append("The years dull your blade (swordsmanship -1). ");
            }
            // Enemy daimyo skirmish once a year: a strong house grabs a neutral neighbour.
            aiClanSkirmish(s, rng, log);
        }

        // --- rivals scheme every season (design ref §5, §6) -------------------
        rivalScheming(s, rng, log, you);

        return log.length() == 0 ? "The season passes quietly." : log.toString().trim();
    }

    /**
     * Light clan AI: the largest foreign daimyo annexes one adjacent unaligned
     * province via the shared conquest primitive (design ref §6.4). Kept to a
     * single flip per year to stay memory-light and slow-burning.
     */
    private static void aiClanSkirmish(GameState s, Rng rng, StringBuilder log) {
        Clan neutral = neutralClan(s);
        if (neutral == null) return;

        Clan aggressor = null;
        for (Clan c : s.clans) {
            if (c.isPlayer || c == neutral) continue;
            if (aggressor == null || c.provinces.size() > aggressor.provinces.size()) aggressor = c;
        }
        if (aggressor == null || aggressor.provinces.isEmpty()) return;

        // Find an unaligned province adjacent to any of the aggressor's holdings.
        List<Province> targets = new ArrayList<Province>();
        for (int pid : aggressor.provinces) {
            Province p = s.province(pid);
            if (p == null) continue;
            for (int nid : p.neighbors) {
                Province np = s.province(nid);
                if (np != null && np.ownerClanId == neutral.id && !targets.contains(np)) targets.add(np);
            }
        }
        if (targets.isEmpty()) return;
        Province prize = targets.get(rng.nextInt(targets.size()));
        PoliticsEngine.conquer(s, prize, aggressor.id);
        log.append("The ").append(aggressor.name).append(" seize ").append(prize.name).append(". ");
    }

    /**
     * Rivals raise hostility and occasionally strike: an in-clan rival may insult
     * you (a duel provocation, surfaced on the map through rising hostility), and
     * a foreign house may kidnap a family member while you are seated at home
     * (design ref §5.3). Only existing model fields are touched.
     */
    private static void rivalScheming(GameState s, Rng rng, StringBuilder log, Samurai you) {
        List<Rival> clanmates = s.livingRivalsInClan(0);
        if (!clanmates.isEmpty() && rng.chance(0.35)) {
            Rival r = clanmates.get(rng.nextInt(clanmates.size()));
            r.hostility = Math.min(100, r.hostility + rng.range(5, 20));
            if (r.hostility > 40) log.append(r.name).append(" openly slights your house. ");
        }
        // Kidnap: only if you have an un-taken child and a foreign house dares it.
        if (rng.chance(0.08)) {
            FamilyMember victim = null;
            for (FamilyMember m : you.children) {
                if (m.alive && !m.kidnapped) { victim = m; break; }
            }
            if (victim != null) {
                Clan foe = strongestForeignClan(s);
                if (foe != null) {
                    victim.kidnapped = true;
                    victim.captorClanId = foe.id;
                    log.append("The ").append(foe.name).append(" have seized ")
                       .append(victim.name).append("! ");
                }
            }
        }
    }

    private static Clan neutralClan(GameState s) {
        for (Clan c : s.clans) if ("Independent".equals(c.name)) return c;
        return null;
    }

    private static Clan strongestForeignClan(GameState s) {
        Clan best = null;
        for (Clan c : s.clans) {
            if (c.isPlayer || "Independent".equals(c.name)) continue;
            if (best == null || c.power > best.power) best = c;
        }
        return best;
    }
}
