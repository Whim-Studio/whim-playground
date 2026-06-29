package com.whim.nextrun.engine;

import com.whim.nextrun.domain.Enemy;
import com.whim.nextrun.domain.HeroClass;
import com.whim.nextrun.domain.Player;

import java.util.Random;

/**
 * The three paths of resolution against an enemy. Each is a self-contained
 * formula reading the player and enemy state; combat mutates hp, the others
 * report success/failure. All randomness flows through the supplied RNG so a
 * run is reproducible from its seed.
 */
public final class Resolution {

    /** Outcome of one resolution attempt. */
    public static final class Outcome {
        public final boolean resolved;   // enemy dealt with (dead / bribed / bypassed)
        public final boolean playerDied;
        public final String message;
        public Outcome(boolean resolved, boolean playerDied, String message) {
            this.resolved = resolved;
            this.playerDied = playerDied;
            this.message = message;
        }
    }

    private final Random rng;

    public Resolution(Random rng) {
        this.rng = rng;
    }

    /** One exchange of blows. Player strikes, survivor strikes back. */
    public Outcome fight(Player p, Enemy e) {
        int atk = p.effectiveAttack();
        if (p.heroClass == HeroClass.WARRIOR) atk += 2;       // Bloodlust
        if (p.heroClass == HeroClass.NECROMANCER) atk += 1;   // Dread

        int dmg = Math.max(1, atk - e.defense) + rng.nextInt(3);
        e.hp -= dmg;
        StringBuilder sb = new StringBuilder();
        sb.append("You hit the ").append(e.name).append(" for ").append(dmg).append(".");

        if (e.isDead()) {
            return new Outcome(true, false, sb.append(" It falls!").toString());
        }
        int incoming = Math.max(1, e.attack - p.effectiveDefense()) + rng.nextInt(2);
        p.damage(incoming);
        sb.append(" It strikes back for ").append(incoming).append(".");
        if (p.isDead()) {
            return new Outcome(false, true, sb.append(" You are slain.").toString());
        }
        return new Outcome(false, false, sb.toString());
    }

    /** Pay gold for safe passage. Lords pay a discount. */
    public Outcome bribe(Player p, Enemy e) {
        int cost = e.bribeCost;
        if (p.heroClass == HeroClass.LORD) cost = (int) Math.round(cost * 0.6);
        if (p.gold < cost) {
            return new Outcome(false, false,
                "The " + e.name + " demands " + cost + " gold — you cannot pay.");
        }
        p.gold -= cost;
        return new Outcome(true, false,
            "You pay the " + e.name + " " + cost + " gold and it lets you pass.");
    }

    /** Slip past unseen. Chance scales with magic/dex vs the enemy's sneak DC. */
    public Outcome sneak(Player p, Enemy e) {
        int skill = p.dexterity + p.magic;
        if (p.heroClass == HeroClass.MAGE) skill += 4;        // Arcana
        int roll = 1 + rng.nextInt(20) + skill;
        if (roll >= e.sneakDc + 10) {
            return new Outcome(true, false,
                "You slip past the " + e.name + " unnoticed.");
        }
        // Failure: the enemy gets a free swing.
        int incoming = Math.max(1, e.attack - p.effectiveDefense());
        p.damage(incoming);
        if (p.isDead()) {
            return new Outcome(false, true,
                "The " + e.name + " catches you (" + incoming + " dmg). You are slain.");
        }
        return new Outcome(false, false,
            "The " + e.name + " spots you and lands a blow (" + incoming + " dmg).");
    }
}
