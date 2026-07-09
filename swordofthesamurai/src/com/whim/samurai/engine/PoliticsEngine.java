package com.whim.samurai.engine;

import com.whim.samurai.model.Clan;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rank;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;

import java.util.ArrayList;
import java.util.List;

/**
 * The political layer: favour &amp; promotion up the feudal ladder, province
 * conquest (flipping ownership), and the Shogunate victory / defeat checks
 * (design ref §4.3, §6, §8). Pure rules over {@code GameState} — no Swing.
 */
public final class PoliticsEngine {
    private PoliticsEngine() { }

    // Promotion thresholds are our own calibration of the manual's qualitative
    // rule that "army size and honor have the greatest influence" (design ref §3).
    private static final int HATAMOTO_HONOR = 150;
    private static final int DAIMYO_HONOR   = 250;
    private static final int DAIMYO_POWER   = 200;

    // Shogun requires >= 24 provinces including Omi/Kyoto (design ref §6.5, §8.1).
    private static final int SHOGUN_PROVINCES = 24;

    // ---- conquest -------------------------------------------------------------

    /**
     * Flip a single province to a new owner, keeping both clans' province lists
     * consistent (design ref §6.4). The canonical conquest primitive, used by the
     * clan AI and by the battle system at integration.
     */
    public static void conquer(GameState s, Province p, int newOwnerClanId) {
        if (p == null || p.ownerClanId == newOwnerClanId) return;
        Clan old = s.clan(p.ownerClanId);
        if (old != null) old.provinces.remove(Integer.valueOf(p.id));
        p.ownerClanId = newOwnerClanId;
        Clan now = s.clan(newOwnerClanId);
        if (now != null && !now.provinces.contains(p.id)) now.provinces.add(p.id);
    }

    /**
     * Beating a rival daimyo outright transfers ALL of his provinces at once —
     * "you step into his place as ruler of all provinces he controlled"
     * (design ref §6.4).
     */
    public static void conquerAllOf(GameState s, int loserClanId, int newOwnerClanId) {
        Clan loser = s.clan(loserClanId);
        if (loser == null) return;
        List<Integer> ids = new ArrayList<Integer>(loser.provinces);
        for (int id : ids) conquer(s, s.province(id), newOwnerClanId);
    }

    // ---- promotion ------------------------------------------------------------

    /**
     * Promote the player when favour warrants it (design ref §4.3):
     * <ul>
     *   <li>gokenin → hatamoto when he outranks every living clan rival in
     *       combined standing and clears the honour bar (honour weighs heaviest);</li>
     *   <li>hatamoto → daimyo when both honour and power are high enough.</li>
     * </ul>
     * Returns {@code true} if a promotion occurred.
     */
    public static boolean checkPromotion(GameState s) {
        Samurai you = s.player;
        int status = you.honor + you.power;

        if (you.rank == Rank.SAMURAI) {
            boolean topOfClan = true;
            for (Rival r : s.livingRivalsInClan(s.playerClan().id)) {
                if (r.honor + r.power > status) { topOfClan = false; break; }
            }
            if (topOfClan && you.honor >= HATAMOTO_HONOR) {
                you.rank = Rank.LORD; // LORD models the hatamoto (lieutenant) rank
                return true;
            }
        } else if (you.rank == Rank.LORD) {
            if (you.honor >= DAIMYO_HONOR && you.power >= DAIMYO_POWER) {
                you.rank = Rank.DAIMYO;
                return true;
            }
        }
        return false;
    }

    // ---- endgame --------------------------------------------------------------

    /**
     * Victory: a daimyo holding the imperial heartland (Omi) plus enough of Japan
     * is proclaimed Shogun (design ref §6.5, §8.1). The exact "24 of 48" bar is
     * kept faithfully against our modelled 48-province map.
     */
    public static boolean checkVictory(GameState s) {
        Samurai you = s.player;
        if (you.rank != Rank.DAIMYO && you.rank != Rank.SHOGUN) return false;

        int pc = s.playerClan().id;
        int held = s.provinceCountFor(pc);
        Province omi = findByName(s, "Omi");
        boolean holdsOmi = omi != null && omi.ownerClanId == pc;

        if (held >= SHOGUN_PROVINCES && holdsOmi) {
            you.rank = Rank.SHOGUN;
            s.victory = true;
            s.gameOver = true;
            s.gameOverReason = "Holding Omi and " + held + " provinces, you are proclaimed "
                    + "Shogun of all Japan!";
            return true;
        }
        return false;
    }

    /**
     * Defeat by conquest: a daimyo whose clan has lost every province swears
     * allegiance to the victor and the game ends (design ref §6.4, §8.2). The
     * heirless game-over is owned by the family engine, not here.
     */
    public static boolean checkDefeat(GameState s) {
        Samurai you = s.player;
        if (you.rank == Rank.DAIMYO && s.provinceCountFor(s.playerClan().id) == 0) {
            s.gameOver = true;
            s.gameOverReason = "Your clan's last province has fallen; you swear allegiance to the victor.";
            return true;
        }
        return false;
    }

    private static Province findByName(GameState s, String name) {
        for (Province p : s.provinces) if (name.equals(p.name)) return p;
        return null;
    }
}
