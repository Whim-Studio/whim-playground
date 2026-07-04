package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * Joe Musashi. Extends {@link Entity} with the player-only resources the HUD and
 * engine track: lives, score, current {@link Enums.Weapon}, Ninjutsu charges, an
 * attack-cooldown timer, the last resolved attack mode (for pose rendering), and
 * a post-hit invulnerability timer. Implements {@link Views.PlayerView}.
 *
 * Mutators here only <em>record</em> state; the engine (Task 2) decides when to
 * call them.
 */
public class Player extends Entity implements Views.PlayerView {
    private int lives = Config.START_LIVES;
    private int score = 0;
    private Enums.Weapon weapon = Enums.Weapon.SHURIKEN;
    private int ninjutsu = Config.START_NINJUTSU;

    /** Ticks until the next attack is allowed (engine decrements each tick). */
    private int attackCooldown = 0;
    /** How the most recent attack resolved, for recovery-pose rendering. */
    private Enums.AttackMode lastAttack = Enums.AttackMode.MELEE;
    /** Ticks of post-hit invulnerability remaining (engine decrements). */
    private int invulnTimer = 0;

    public Player(Aabb box, Enums.Plane plane) {
        super(box, plane);
        this.hp = 1;
    }

    // ---- Views.PlayerView ----
    @Override public int lives() { return lives; }
    @Override public int score() { return score; }
    @Override public Enums.Weapon weapon() { return weapon; }
    @Override public int ninjutsu() { return ninjutsu; }
    @Override public Enums.AttackMode lastAttack() { return lastAttack; }

    // ---- Score / lives ----
    public void addScore(int points) { this.score += points; }

    public void loseLife() { if (lives > 0) lives--; }
    public void addLife() { lives++; }
    public void setLives(int lives) { this.lives = lives; }
    public boolean isGameOver() { return lives <= 0; }

    // ---- Weapon ----
    public void setWeapon(Enums.Weapon weapon) { this.weapon = weapon; }
    /** Advance the weapon along SHURIKEN -> KNIFE -> GUN (GUN is terminal). */
    public void upgradeWeapon() { this.weapon = this.weapon.upgrade(); }

    // ---- Ninjutsu ----
    public void addNinjutsu() { ninjutsu++; }
    public void addNinjutsu(int n) { ninjutsu += n; }
    /** Spend one charge if available; returns true if a charge was consumed. */
    public boolean useNinjutsu() {
        if (ninjutsu > 0) { ninjutsu--; return true; }
        return false;
    }
    public boolean hasNinjutsu() { return ninjutsu > 0; }

    // ---- Attack cooldown ----
    public int attackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int ticks) { this.attackCooldown = ticks; }
    public boolean canAttack() { return attackCooldown <= 0; }
    /** Start cooldown at the current weapon's cadence. */
    public void startAttackCooldown() { this.attackCooldown = weapon.cooldownTicks(); }
    public void tickAttackCooldown() { if (attackCooldown > 0) attackCooldown--; }

    public void setLastAttack(Enums.AttackMode mode) { this.lastAttack = mode; }

    // ---- Invulnerability (i-frames) ----
    public int invulnTimer() { return invulnTimer; }
    public void setInvulnTimer(int ticks) { this.invulnTimer = ticks; }
    public boolean isInvulnerable() { return invulnTimer > 0; }
    public void tickInvuln() { if (invulnTimer > 0) invulnTimer--; }
}
