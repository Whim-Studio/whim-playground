package com.whim.samurai.engine;

import com.whim.samurai.app.Game;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Samurai;

/**
 * Rules for the ninja / castle-infiltration MELEE variant (design ref §2c). Stealth
 * resolution only — {@code NinjaScreen} owns the top-down movement, guard patrols
 * and rendering.
 *
 * <p>Design reference honoured:</p>
 * <ul>
 *   <li>Melee falls the hero at <b>two wounds</b> (§2c, Appendix A).</li>
 *   <li>The alarm rises when a guard gets a good look at you; once it maxes, guards
 *       "swarm out to look for you" (§2c).</li>
 *   <li><b>Success</b> (assassination / sabotage) weakens the garrison and RAISES
 *       power but LOWERS honor — a dishonourable act (design ref §3.3, §3.4).</li>
 *   <li><b>Failure</b> (caught / cut down) wounds or kills the player (§2c); being
 *       killed with no heir ends the game (§8).</li>
 *   <li>Stealth skill sharpens detection resistance and shuriken supply (§4.1).</li>
 * </ul>
 */
public class StealthEngine {

    /** Wounds that fell the hero in a melee (design ref §2c). */
    public static final int MAX_WOUNDS = 2;

    /** Alarm level at which the castle is fully roused (design ref §2c). */
    public static final double ALARM_MAX = 100;

    public final int stealthSkill;
    public int wounds;
    public int shuriken;
    public double alarm;

    private final Rng rng;

    public StealthEngine(Samurai player, Rng rng) {
        this.rng = rng;
        this.stealthSkill = player != null ? player.stealth : 6;
        // Poisoned shuriken supply scales loosely with stealth (design ref §2c; ~APPROX count).
        this.shuriken = 2 + stealthSkill / 4;
    }

    /**
     * Advance the alarm while a guard has line-of-sight. Higher stealth slows the
     * build-up; distance further tempers it (design ref §2c).
     */
    public void seenTick(double distance) {
        double gain = 3.5 - stealthSkill * 0.12;                 // ~APPROX per-tick rouse rate
        if (gain < 0.6) gain = 0.6;
        double near = Math.max(0.3, 1.4 - distance / 160.0);
        alarm = Math.min(ALARM_MAX, alarm + gain * near);
    }

    /** With no guard watching, the castle slowly settles again. */
    public void calmTick() { alarm = Math.max(0, alarm - 0.8); }

    public boolean fullyAlarmed() { return alarm >= ALARM_MAX; }

    public boolean canThrow() { return shuriken > 0; }

    /** Attempt a shuriken; consumes one and hits on a stealth-weighted chance. */
    public boolean throwShuriken() {
        if (shuriken <= 0) return false;
        shuriken--;
        return rng.chance(0.6 + stealthSkill * 0.02);
    }

    public void takeWound() { wounds++; }
    public boolean down() { return wounds >= MAX_WOUNDS; }

    /**
     * Apply the mission result (design ref §2c, §3). {@code target} may be
     * {@code null} for a practice infiltration — then nothing lasting changes.
     */
    public void applyOutcome(Game game, Province target, boolean success) {
        if (game == null || game.state == null) return;
        GameState st = game.state;
        Samurai me = st.player;
        if (me == null) return;

        if (success) {
            // Dishonourable but effective: power up, honor down (design ref §3.3–§3.4).
            me.power += rng.range(8, 16);
            me.honor = Math.max(0, me.honor - rng.range(10, 20));
            me.stealth = Math.min(20, me.stealth + (rng.chance(0.5) ? 1 : 0));
            st.dynastyScore += 25;
            if (target != null) {
                target.garrison = Math.max(5, target.garrison - rng.range(8, 18)); // weakened / assassinated
            }
        } else {
            // Caught: wounded, and on a bad roll cut down (design ref §2c, §8).
            if (down() && rng.chance(0.5)) {
                me.alive = false;
                st.gameOver = true;
                st.victory = false;
                st.gameOverReason = "Your infiltration was discovered — the guards cut you down.";
            } else {
                me.honor = Math.max(0, me.honor - rng.range(3, 8)); // the attempt was noticed
            }
        }
    }
}
