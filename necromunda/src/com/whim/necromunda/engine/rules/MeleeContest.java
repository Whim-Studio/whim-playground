package com.whim.necromunda.engine.rules;

/**
 * The close-combat contest (faithful ORB method, not a WHFB to-hit chart):
 * each fighter rolls one D6 per Attack, takes their <em>highest</em> die, and
 * adds Weapon Skill plus any bonuses (e.g. +1 for charging, weapon bonuses).
 * The higher total wins and gets to wound; equal totals are a draw (treated as
 * both sides parrying — no wound dealt).
 */
public final class MeleeContest {

    /** Outcome of a contest from the attacker's point of view. */
    public enum Outcome {
        ATTACKER_WINS,
        DEFENDER_WINS,
        DRAW
    }

    private MeleeContest() {
    }

    /** A fighter's combat score: highest of their attack dice + WS + bonus. */
    public static int score(int[] attackDice, int weaponSkill, int bonus) {
        int highest = 0;
        for (int d : attackDice) {
            if (d > highest) {
                highest = d;
            }
        }
        return highest + weaponSkill + bonus;
    }

    /** Compare two already-computed scores. */
    public static Outcome resolve(int attackerScore, int defenderScore) {
        if (attackerScore > defenderScore) {
            return Outcome.ATTACKER_WINS;
        }
        if (defenderScore > attackerScore) {
            return Outcome.DEFENDER_WINS;
        }
        return Outcome.DRAW;
    }
}
