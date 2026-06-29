package com.whim.nextrun.engine;

import com.whim.nextrun.domain.HeroClass;
import com.whim.nextrun.domain.Player;

/**
 * The heart of the action economy: how many TURNS an action costs, derived
 * dynamically from the player's stats and class passives. Higher dexterity
 * lowers crafting/building time, magic lowers sneaking, etc. Costs never drop
 * below 1 — time is always spent.
 */
public final class ActionCosts {

    private ActionCosts() {}

    public static int cost(ActionType action, Player p) {
        HeroClass hc = p.heroClass;
        int c;
        switch (action) {
            case MOVE:
                c = 1; // movement is the baseline tick of the clock
                break;
            case GATHER:
                // base 3, faster with dexterity
                c = 3 - p.dexterity / 3;
                if (hc == HeroClass.DRUID) c -= 1;
                break;
            case LOOT:
                c = 1;
                break;
            case EXPLORE:
                c = 2;
                break;
            case FIGHT:
                c = 2;
                break;
            case BRIBE:
                c = 1;
                break;
            case SNEAK:
                // base 3, magic makes you slip faster
                c = 3 - p.magic / 3;
                if (hc == HeroClass.MAGE) c -= 1;
                break;
            case CRAFT_WEAPON:
            case CRAFT_ARMOR:
                // base 5, dexterity speeds the work
                c = 5 - p.dexterity / 2;
                if (hc == HeroClass.ARTISAN) c -= 2;
                break;
            case BUILD:
                // base 8, the slowest action; leaders/artisans speed it
                c = 8 - p.dexterity / 3;
                if (hc == HeroClass.LEADER) c -= 2;
                if (hc == HeroClass.ARTISAN) c -= 1;
                break;
            case REST:
                c = 4;
                break;
            default:
                c = 1;
        }
        return Math.max(1, c);
    }
}
