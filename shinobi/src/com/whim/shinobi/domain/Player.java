// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** The player-controlled shinobi. */
public class Player extends Entity implements Views.PlayerView {
    public int lives = Config.START_LIVES;
    public int score = 0;
    public int ninjutsu = Config.START_NINJUTSU;
    public Enums.Weapon weapon = Enums.Weapon.SHURIKEN;
    public Enums.AttackMode lastAttack = Enums.AttackMode.MELEE;
    public boolean crouch = false;

    /** Ticks until the player can attack again (weapon cooldown / swing recovery). */
    public int attackCooldown = 0;
    /** Ticks of temporary invulnerability after taking a hit. */
    public int invuln = 0;

    @Override public int lives() { return lives; }
    @Override public int score() { return score; }
    @Override public Enums.Weapon weapon() { return weapon; }
    @Override public int ninjutsu() { return ninjutsu; }
    @Override public Enums.AttackMode lastAttack() { return lastAttack; }
}
