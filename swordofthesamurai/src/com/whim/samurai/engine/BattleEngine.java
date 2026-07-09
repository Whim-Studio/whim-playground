package com.whim.samurai.engine;

import com.whim.samurai.app.Game;
import com.whim.samurai.model.Clan;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rank;
import com.whim.samurai.model.Samurai;

/**
 * Rules for the open-field BATTLE (design ref §2b). A readable tactical resolution,
 * not a full RTS: the player picks a named formation, then issues a command each
 * round (advance / hold / charge / retreat); strengths and morale attrite until one
 * side routs.
 *
 * <p>Design reference honoured:</p>
 * <ul>
 *   <li>Named formations — attack: <b>Hoshi, Kakuyoku, Katana</b>; defence:
 *       <b>Ganko, Koyaku, Engetsu</b> (§2b), with the counter relationships the
 *       manual describes (Ganko counters Hoshi; Koyaku swallows Hoshi/Kakuyoku;
 *       Engetsu counters a same-side Katana).</li>
 *   <li>Unit scale by rank — <b>6 / 60 / 250</b> soldiers per figure for
 *       samurai / hatamoto / daimyo (§2b, Appendix A).</li>
 *   <li>Damage weighted by strength, generalship, morale and luck (§2b).</li>
 *   <li>A side loses when its strength routs; on victory over an enemy province the
 *       province flips to the player's clan (§6.4).</li>
 * </ul>
 */
public class BattleEngine {

    // Attack formations (design ref §2b).
    public static final int HOSHI = 0;     // Arrowhead
    public static final int KAKUYOKU = 1;  // Crane's Wing
    public static final int KATANA = 2;    // Long Sword
    // Defence formations (design ref §2b).
    public static final int GANKO = 3;     // Birds in Flight
    public static final int KOYAKU = 4;    // The Yoke
    public static final int ENGETSU = 5;   // Half Moon

    public static final String[] FORMATION_NAMES = {
        "Hoshi (Arrowhead)", "Kakuyoku (Crane's Wing)", "Katana (Long Sword)",
        "Ganko (Birds in Flight)", "Koyaku (The Yoke)", "Engetsu (Half Moon)"
    };

    // Player commands each round.
    public static final int ADVANCE = 0;
    public static final int HOLD = 1;
    public static final int CHARGE = 2;
    public static final int RETREAT = 3;

    public final boolean playerAttacking;
    public final int soldiersPerFigure;   // 6 / 60 / 250 (design ref §2b)
    public double playerStrength;          // measured in "figures"
    public double enemyStrength;
    public double playerMorale = 100;
    public double enemyMorale = 100;
    public int playerFormation;
    public int enemyFormation;

    private final int generalship;
    private final Rng rng;
    private boolean fled;

    public BattleEngine(Samurai player, Province target, boolean playerAttacking, Rng rng) {
        this.rng = rng;
        this.playerAttacking = playerAttacking;
        this.generalship = player != null ? player.generalship : 8;
        this.soldiersPerFigure = soldiersPerFigure(player != null ? player.rank : Rank.SAMURAI);

        // Player army size derived from power (~APPROX — no explicit army count on Samurai).
        double figs = player != null ? Math.max(6, player.power / 4.0 + player.generalship) : 24;
        this.playerStrength = figs;
        // Enemy from the province garrison (defenders' strength, design ref §2b); practice default otherwise.
        this.enemyStrength = target != null ? Math.max(8, target.garrison * 0.6) : 22;

        // The enemy has already chosen; the player answers with the opposite category (§2b).
        this.enemyFormation = playerAttacking ? rng.range(GANKO, ENGETSU) : rng.range(HOSHI, KATANA);
    }

    private static int soldiersPerFigure(Rank r) {
        if (r == Rank.DAIMYO || r == Rank.SHOGUN) return 250;
        if (r == Rank.LORD) return 60;
        return 6;
    }

    /**
     * Formation match-up multiplier for the player (>1 favourable). Encodes the
     * manual's counter relationships (design ref §2b).
     */
    public double formationEdge() {
        // Attacker counters: Hoshi splits a Ganko center-heavy line; a flank Katana
        // beats a mismatched half-moon; Kakuyoku envelops the Yoke.
        if (playerAttacking) {
            if (playerFormation == HOSHI && enemyFormation == GANKO) return 0.9;   // Ganko counters Hoshi
            if (playerFormation == HOSHI && enemyFormation == KOYAKU) return 0.85; // Koyaku swallows Hoshi
            if (playerFormation == KAKUYOKU && enemyFormation == KOYAKU) return 0.85;
            if (playerFormation == KATANA && enemyFormation == ENGETSU) return 0.9; // Engetsu counters Katana
            if (playerFormation == HOSHI && enemyFormation == ENGETSU) return 1.15;
            if (playerFormation == KAKUYOKU && enemyFormation == GANKO) return 1.15;
            if (playerFormation == KATANA && enemyFormation == GANKO) return 1.15;
            return 1.0;
        } else {
            if (playerFormation == GANKO && enemyFormation == HOSHI) return 1.2;   // counters Hoshi
            if (playerFormation == KOYAKU && (enemyFormation == HOSHI || enemyFormation == KAKUYOKU)) return 1.2;
            if (playerFormation == ENGETSU && enemyFormation == KATANA) return 1.2;
            if (playerFormation == GANKO && enemyFormation == KATANA) return 0.85;
            return 1.0;
        }
    }

    /** True once a formation has been chosen for this side. */
    public boolean isAttackFormation(int f) { return f >= HOSHI && f <= KATANA; }

    /**
     * Resolve one round of exchange under the given command. Damage depends on
     * strength, generalship, morale, formation edge, the command, and luck (§2b).
     */
    public void resolveRound(int command) {
        if (isOver()) return;
        double edge = formationEdge();
        double genFactor = 0.8 + generalship * 0.04;             // trained armies hit harder (§2b)

        // Command shapes the trade of blows.
        double playerBite, enemyBite;
        switch (command) {
            case CHARGE:  playerBite = 0.34; enemyBite = 0.26; break; // high risk / high reward
            case HOLD:    playerBite = 0.14; enemyBite = 0.10; break; // defensive
            case RETREAT: playerBite = 0.04; enemyBite = 0.16; break; // disengage, take hits
            default:      playerBite = 0.22; enemyBite = 0.18;        // advance
        }

        double luckP = 0.85 + rng.nextDouble() * 0.4;
        double luckE = 0.85 + rng.nextDouble() * 0.4;

        double toEnemy = enemyStrength * playerBite * edge * genFactor
                * (playerMorale / 100.0) * luckP;
        double toPlayer = playerStrength * enemyBite / edge
                * (enemyMorale / 100.0) * luckE;

        enemyStrength = Math.max(0, enemyStrength - toEnemy);
        playerStrength = Math.max(0, playerStrength - toPlayer);

        // Morale: heavier losses and being pushed sap it; winning the exchange lifts it (§2b).
        enemyMorale = clampMorale(enemyMorale - toEnemy * 2.2 + (toPlayer > toEnemy ? 3 : 0));
        playerMorale = clampMorale(playerMorale - toPlayer * 2.0
                + (toEnemy > toPlayer ? 2 : 0) + generalship * 0.05
                - (command == RETREAT ? 6 : 0));

        if (command == RETREAT && (playerMorale < 25 || rng.chance(0.3))) fled = true;
    }

    private double clampMorale(double m) { return Math.max(0, Math.min(100, m)); }

    public boolean playerRouted() { return fled || playerStrength <= 2 || playerMorale <= 5; }
    public boolean enemyRouted()  { return enemyStrength <= 2 || enemyMorale <= 5; }
    public boolean isOver()       { return playerRouted() || enemyRouted(); }
    public boolean playerWon()    { return enemyRouted() && !playerRouted(); }

    /**
     * Apply the battle result to the world (design ref §6.4). {@code target} may be
     * {@code null} for a practice battle — then nothing lasting changes.
     */
    public void applyOutcome(Game game, Province target, boolean playerWon) {
        if (game == null || game.state == null) return;
        GameState st = game.state;
        Samurai me = st.player;

        if (playerWon) {
            if (me != null) {
                me.power += rng.range(6, 14);
                me.generalship = Math.min(20, me.generalship + (rng.chance(0.5) ? 1 : 0)); // battle trains (§4.2)
                st.dynastyScore += 60;
            }
            if (target != null) {
                Clan pc = st.playerClan();
                if (pc != null && target.ownerClanId != pc.id) {
                    Clan old = st.clan(target.ownerClanId);
                    if (old != null) old.provinces.remove(Integer.valueOf(target.id));
                    target.ownerClanId = pc.id;
                    if (!pc.provinces.contains(Integer.valueOf(target.id))) pc.provinces.add(target.id);
                    target.garrison = Math.max(10, (int) enemyStrength + 20); // regarrisoned
                }
            }
        } else {
            // Loss: casualties bleed power; a routed defence of your own land shames you (§3.3).
            if (me != null) {
                me.power = Math.max(0, me.power - rng.range(5, 12));
                if (!playerAttacking) me.honor = Math.max(0, me.honor - rng.range(4, 10));
            }
        }
    }
}
