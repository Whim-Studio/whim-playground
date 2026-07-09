package com.whim.samurai.engine;

import com.whim.samurai.app.Game;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;

/**
 * Rules for the one-on-one katana DUEL (design ref §2a). This is pure resolution
 * logic — no Swing, no rendering; {@code DuelScreen} owns the timing/animation
 * and calls into this for every strike and for the final outcome.
 *
 * <p>Key rules honoured from the design reference:</p>
 * <ul>
 *   <li>A combatant falls at <b>four wounds</b> ("when either combatant takes four
 *       wounds, he falls" — design ref §2a, Appendix A).</li>
 *   <li>You can only parry an attack on the side you are defending (§2a).</li>
 *   <li>The charged over-the-shoulder strike <b>cannot be parried</b> and does more
 *       damage — modelled here as 2 wounds vs a normal 1 (design ref §2a; the exact
 *       "2 vs 1" figure is community-sourced / UNVERIFIED, Appendix A).</li>
 *   <li>A prior melee wound carries into the duel as a +1 starting wound (§2a).</li>
 *   <li>Landing odds scale with swordsmanship: player vs opponent (§4).</li>
 * </ul>
 */
public class DuelEngine {

    /** Wounds that fell a combatant (design ref §2a). */
    public static final int MAX_WOUNDS = 4;

    // Attack lines / parry sides.
    public static final int LINE_HIGH = 0;
    public static final int LINE_MID = 1;
    public static final int LINE_LOW = 2;
    public static final int OVERHEAD = 3; // charged over-the-shoulder, unblockable (§2a)

    public final int playerSkill;
    public final int foeSkill;
    public int playerWounds;
    public int foeWounds;

    private final Rng rng;

    public DuelEngine(int playerSkill, int foeSkill, Rng rng, int playerStartWound) {
        this.playerSkill = playerSkill;
        this.foeSkill = foeSkill;
        this.rng = rng;
        // Carry-over penalty from a preceding melee wound (design ref §2a).
        this.playerWounds = Math.max(0, playerStartWound);
    }

    /**
     * Does an attack on {@code line} land against a defender holding {@code parrySide}?
     * A correct-side parry blocks a normal blow; the charged overhead is unblockable.
     * Otherwise the blow lands on a skill-weighted probability (design ref §2a, §4).
     */
    public boolean lands(int line, boolean charged, int parrySide, int atkSkill, int defSkill) {
        if (!charged && parrySide == line) return false;        // parried on the defended side (§2a)
        // Base hit chance; the overhead is committed and near-certain but slow (screen enforces wind-up).
        double p = charged ? 0.92 : 0.70;
        // ~APPROX: each point of swordsmanship advantage shifts the odds ~2% (design ref §4).
        p += (atkSkill - defSkill) * 0.02;
        if (p < 0.12) p = 0.12;
        if (p > 0.96) p = 0.96;
        return rng.chance(p);
    }

    /** Wound severity: the charged overhead does more damage (design ref §2a). */
    public int woundValue(boolean charged) { return charged ? 2 : 1; }

    public void woundFoe(boolean charged)    { foeWounds += woundValue(charged); }
    public void woundPlayer(boolean charged) { playerWounds += woundValue(charged); }

    public boolean foeDown()    { return foeWounds >= MAX_WOUNDS; }
    public boolean playerDown() { return playerWounds >= MAX_WOUNDS; }

    // --- Foe AI (design ref §2a: Sid Meier helped tune the dueling) ---------

    /**
     * Choose the foe's next intent. Higher-skill foes attack more decisively and,
     * when defending, guess the player's line more often. Returns one of the LINE_*
     * / OVERHEAD constants for an attack, or {@code -1000 - side} to signal a parry
     * of {@code side}.
     */
    public int chooseFoeAttack() {
        // A rare committed overhead when confident.
        if (rng.chance(0.10 + foeSkill * 0.004)) return OVERHEAD;
        return rng.range(LINE_HIGH, LINE_LOW);
    }

    /** Foe's parry guess against a known/likely player line, sharpened by skill. */
    public int chooseFoeParry(int likelyPlayerLine) {
        double read = 0.25 + foeSkill * 0.03;                    // skill → better read (§4)
        if (likelyPlayerLine >= 0 && rng.chance(read)) return likelyPlayerLine;
        return rng.range(LINE_HIGH, LINE_LOW);
    }

    // --- Outcome application (design ref §2a honor, §8 death) ---------------

    /**
     * Apply the resolved duel to the world. {@code target} may be {@code null} for a
     * standalone practice bout, in which case no lasting state changes.
     */
    public void applyOutcome(Game game, Rival target, boolean toDeath, boolean playerWon) {
        if (game == null || game.state == null) return;
        GameState st = game.state;
        Samurai me = st.player;

        if (playerWon) {
            if (target != null && toDeath) {
                target.alive = false;                            // a kill (§2a)
            }
            if (me != null) {
                // A duel won for honour raises standing; the dynasty score benefits too (§3.2).
                me.honor += rng.range(15, 30);
                me.power += rng.range(2, 6);
                me.swordsmanship = Math.min(20, me.swordsmanship + (rng.chance(0.5) ? 1 : 0)); // real duels train (§4.2)
                st.dynastyScore += 40;
            }
        } else {
            // The player lost. To-the-death duels are fatal; first-blood bouts merely shame.
            if (me != null) {
                if (toDeath) {
                    me.alive = false;
                    st.gameOver = true;
                    st.victory = false;
                    st.gameOverReason = (target != null ? target.name : "A rival")
                            + " cut you down in a duel to the death.";
                } else {
                    me.honor = Math.max(0, me.honor - rng.range(8, 16));
                }
            }
        }
    }
}
