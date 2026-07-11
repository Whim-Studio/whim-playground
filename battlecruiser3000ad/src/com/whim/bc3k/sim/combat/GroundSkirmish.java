package com.whim.bc3k.sim.combat;

/**
 * A compact, deterministic planetary ground engagement (ATVs + marines vs. a
 * hostile ground force), promoting BC3K's ground combat from a bare launch count
 * into a playable skirmish. Two force pools trade fire over time; the player can
 * press an "assault" to deal a burst at some cost. Pure model — unit-testable.
 *
 * Magnitudes are labelled design approximations (see BC3K_Phase6_KnownIssues.md).
 */
public final class GroundSkirmish {

    private final double playerMax;
    private final double enemyMax;
    private double playerHp;
    private double enemyHp;
    private final double playerAtk;
    private final double enemyAtk;
    private boolean over;
    private boolean playerWon;

    /** @param atvs number of ATVs deployed to the surface (scales the player force). */
    public GroundSkirmish(int atvs) {
        int n = Math.max(1, atvs);
        this.playerMax = 40 + n * 30;
        this.enemyMax = 120;
        this.playerHp = playerMax;
        this.enemyHp = enemyMax;
        this.playerAtk = 8 + n * 4;
        this.enemyAtk = 11;
    }

    public int playerHp() { return (int) Math.round(playerHp); }
    public int playerMaxHp() { return (int) Math.round(playerMax); }
    public int enemyHp() { return (int) Math.round(enemyHp); }
    public int enemyMaxHp() { return (int) Math.round(enemyMax); }
    public boolean over() { return over; }
    public boolean playerWon() { return playerWon; }

    /** Passive exchange of fire. */
    public void tick(double dt) {
        if (over) return;
        enemyHp = Math.max(0, enemyHp - playerAtk * 0.4 * dt);
        playerHp = Math.max(0, playerHp - enemyAtk * 0.4 * dt);
        checkOver();
    }

    /** Player-ordered assault: a burst of damage at the cost of some casualties. */
    public double assault() {
        if (over) return 0;
        double dmg = playerAtk * 1.5;
        enemyHp = Math.max(0, enemyHp - dmg);
        playerHp = Math.max(0, playerHp - enemyAtk * 0.5);
        checkOver();
        return dmg;
    }

    private void checkOver() {
        if (enemyHp <= 0) { over = true; playerWon = true; }
        else if (playerHp <= 0) { over = true; playerWon = false; }
    }
}
