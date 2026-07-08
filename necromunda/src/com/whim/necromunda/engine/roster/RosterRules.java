package com.whim.necromunda.engine.roster;

import java.util.ArrayList;
import java.util.List;

import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Gang;

/**
 * Roster legality checks for gang creation (classic constraints):
 * exactly one Leader, a minimum gang size, a cap on Champions, and Juves capped
 * relative to Gangers. Returns human-readable reasons so the roster UI can show
 * exactly why a gang is illegal.
 */
public final class RosterRules {

    /** Minimum number of fighters a legal gang must field. */
    public static final int MIN_GANG_SIZE = 3;
    /** Maximum number of Gang Champions. */
    public static final int MAX_CHAMPIONS = 2;

    private RosterRules() {
    }

    /** The result of validating a roster: legal flag + reasons if not. */
    public static final class Result {
        private final boolean legal;
        private final List<String> problems;

        Result(boolean legal, List<String> problems) {
            this.legal = legal;
            this.problems = problems;
        }

        public boolean isLegal() { return legal; }
        public List<String> problems() { return problems; }
    }

    public static Result validate(Gang gang) {
        List<String> problems = new ArrayList<String>();

        int leaders = count(gang, FighterType.LEADER);
        int champions = count(gang, FighterType.CHAMPION);
        int gangers = count(gang, FighterType.GANGER);
        int juves = count(gang, FighterType.JUVE);
        int size = gang.roster().size();

        if (leaders != 1) {
            problems.add("A gang must have exactly one Leader (has " + leaders + ").");
        }
        if (size < MIN_GANG_SIZE) {
            problems.add("A gang must have at least " + MIN_GANG_SIZE
                    + " fighters (has " + size + ").");
        }
        if (champions > MAX_CHAMPIONS) {
            problems.add("A gang may have at most " + MAX_CHAMPIONS
                    + " Champions (has " + champions + ").");
        }
        if (juves > gangers) {
            problems.add("Juves may not outnumber Gangers ("
                    + juves + " Juves vs " + gangers + " Gangers).");
        }

        return new Result(problems.isEmpty(), problems);
    }

    private static int count(Gang gang, FighterType type) {
        int n = 0;
        for (Fighter f : gang.roster()) {
            if (f.type() == type) {
                n++;
            }
        }
        return n;
    }
}
