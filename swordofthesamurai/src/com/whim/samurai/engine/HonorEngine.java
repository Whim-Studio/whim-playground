package com.whim.samurai.engine;

import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Samurai;

/**
 * The Honor vs Power dual axis — the design's spine (design ref §3).
 *
 * <p>Honor is the bushido/virtue axis (the daimyo's favour, your family's legacy);
 * Power is land, armies and koku. This engine owns the *descriptive bands* used to
 * label those raw integers on the character sheet, a few consequence helpers, and
 * the multi-generational {@code dynastyScore} that blends both axes at the end
 * ("Verdict of History", design ref §3.5 / §8.3).</p>
 *
 * <p>Numbers here are calibrated to the skeleton's stat ranges (Samurai honor/power
 * are "roughly 0..1000 each", see {@code Samurai}). Where a constant is invented for
 * flavour it is flagged as an approximation.</p>
 */
public final class HonorEngine {
    private HonorEngine() { }

    // Band thresholds for HONOR. Approximation: the original shows honor as an icon
    // scale, not a number, so these cut-points are a best-effort mapping (design ref §3.2/§3.3).
    private static final String[] HONOR_BANDS = {
        "Disgraced",   // reviled — treachery caught, cowardice
        "Dishonored",
        "Tarnished",
        "Common",
        "Respected",
        "Honorable",
        "Esteemed",
        "Exemplary",
        "Paragon of Bushido" // legendary virtue
    };

    // Band thresholds for POWER (land + armies + wealth). Approximation as above.
    private static final String[] POWER_BANDS = {
        "Landless",
        "Minor",
        "Modest",
        "Established",
        "Formidable",
        "Mighty",
        "Dominant",
        "Overlord of Provinces"
    };

    /** Descriptive band for an honor value (~0..1000). See design ref §3. */
    public static String honorBand(int honor) {
        return band(honor, HONOR_BANDS);
    }

    /** Descriptive band for a power value (~0..1000). See design ref §3. */
    public static String powerBand(int power) {
        return band(power, POWER_BANDS);
    }

    private static String band(int value, String[] bands) {
        if (value < 0) value = 0;
        // Spread the bands across a 0..800 working range; anything above tops out.
        int span = 800 / bands.length;               // approximation
        int idx = value / Math.max(1, span);
        if (idx >= bands.length) idx = bands.length - 1;
        return bands[idx];
    }

    /**
     * Is this honor low enough that the lord would expect atonement? Used by the
     * family/game-over flow to hint at seppuku (design ref §3.3 / §5.4).
     */
    public static boolean isDishonored(int honor) { return honor < 90; }

    /** A short consequence note describing where the honor level leaves you. */
    public static String honorConsequence(int honor) {
        if (honor < 40)  return "Your name is a byword for treachery; only seppuku can wipe the stain.";
        if (honor < 90)  return "Whispers of dishonour follow you; the daimyo doubts your loyalty.";
        if (honor < 300) return "A samurai of ordinary standing — deeds of arms will raise you.";
        if (honor < 550) return "A respected retainer; the daimyo marks your name for promotion.";
        return "Your honour is renowned across the province — a model of the Way.";
    }

    /** A short consequence note for the power level. */
    public static String powerConsequence(int power) {
        if (power < 60)  return "Few warriors answer to you; grow your fief before you make enemies.";
        if (power < 250) return "A modest force at your back — enough to defend, not yet to conquer.";
        if (power < 550) return "A formidable host; rivals think twice before crossing you.";
        return "Armies march at your word — the Shogunate is within a bold man's reach.";
    }

    /**
     * The multi-generational "Verdict of History" score (design ref §3.5 / §8.3).
     *
     * <p>The manual lists seven weighted factors (Honor, Generalship, Army Size, Land,
     * Province Control, low Rival Armies, and Dynasty/heir). We do not have every one of
     * those as a discrete field on the skeleton model, so this blends the axes we do have:
     * personal honor + power, provinces the player's clan controls, and the number of
     * generations survived. The exact weights below are an <b>approximation</b> of the
     * original formula (which is undocumented); the community-reported maximum is ~12,600.</p>
     */
    public static long dynastyScore(GameState s) {
        if (s == null) return 0;
        long score = 0;
        Samurai p = s.player;
        if (p != null) {
            score += p.honor * 8L;                    // honor weighted heaviest (design ref §3)
            score += p.power * 4L;
            score += (p.generalship + p.swordsmanship + p.stealth) * 20L;
            score += p.fiefs.size() * 200L;
            score += p.hasHeir() ? 1500L : 0L;        // "Your Dynasty" — an heir founds a lasting line
        }
        // Province control by the player's clan (design ref §8.3 factor 5).
        if (s.playerClan() != null) {
            score += s.provinceCountFor(s.playerClan().id) * 300L;
        }
        // Accumulated family honor carried across the dynasty + generations survived.
        score += s.dynastyScore;
        score += (s.generation - 1) * 500L;
        // Victory (named Shogun) is the crowning multiplier.
        if (s.victory) score = (long) (score * 1.5);
        return Math.max(0, score);
    }

    /** A one-line verdict on how long the dynasty will endure, keyed to the score. */
    public static String dynastyVerdict(long score) {
        if (score >= 10000) return "A dynasty to rival the Tokugawa — your line rules for three centuries.";
        if (score >= 6000)  return "A strong dynasty; your descendants hold power for generations.";
        if (score >= 3000)  return "Your house endures a while, then fades as rivals reassert themselves.";
        if (score >= 1200)  return "A brief ascendancy — remembered, but soon overthrown.";
        return "Your name flickers and is forgotten; the age of war grinds on without you.";
    }
}
