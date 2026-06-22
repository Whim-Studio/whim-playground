package com.whim.civ.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cascading tech tree. Each tech lists 0..2 prerequisite techs. Reaching SPACE_FLIGHT
 * (and beyond) is the long-term goal; this subset forms a valid acyclic graph.
 *
 * <p>Enum constants are declared in a topological order (every prerequisite appears before
 * any tech that depends on it). This is REQUIRED in Java: an enum constant's constructor
 * arguments cannot forward-reference a constant declared later, so the contract's listing
 * order is reordered here into dependency order while preserving the exact set of techs and
 * their prerequisites.
 *
 * <p>COMMUNISM government note (see {@link Government}): the COMMUNISM government keeps a
 * {@code null} prereq, so no COMMUNISM tech constant is invented here.
 */
public enum TechType {
    // --- roots (no prerequisites) ---
    ALPHABET,
    BRONZE_WORKING,
    POTTERY,
    CEREMONIAL_BURIAL,
    HORSEBACK_RIDING,
    MASONRY,
    THE_WHEEL,
    // --- tier 1 ---
    WRITING(ALPHABET),
    CODE_OF_LAWS(ALPHABET),
    IRON_WORKING(BRONZE_WORKING),
    MATHEMATICS(ALPHABET, MASONRY),
    CURRENCY(BRONZE_WORKING),
    // --- tier 2 ---
    MONARCHY(CEREMONIAL_BURIAL, CODE_OF_LAWS),
    THE_REPUBLIC(CODE_OF_LAWS, WRITING),
    LITERACY(CODE_OF_LAWS, WRITING),
    TRADE(CURRENCY, WRITING),
    CONSTRUCTION(CURRENCY, MASONRY),
    // --- tier 3 ---
    ENGINEERING(THE_WHEEL, CONSTRUCTION),
    UNIVERSITY(MATHEMATICS, LITERACY),
    // --- tier 4 ---
    INVENTION(WRITING, ENGINEERING),
    COMPUTERS(MATHEMATICS, UNIVERSITY),
    // --- tier 5 ---
    GUNPOWDER(IRON_WORKING, INVENTION),
    DEMOCRACY(LITERACY, INVENTION),
    SPACE_FLIGHT(COMPUTERS, UNIVERSITY);

    private final List<TechType> prereqs;

    TechType(TechType... prereqs) {
        List<TechType> list = new ArrayList<TechType>();
        for (TechType t : prereqs) {
            list.add(t);
        }
        this.prereqs = Collections.unmodifiableList(list);
    }

    /** Unmodifiable, never null. */
    public java.util.List<TechType> getPrereqs() { return prereqs; }

    /**
     * Research beakers required. Deterministic and {@code > 0}: scales with the size of the
     * tech's full prerequisite closure, so deeper techs cost more. Roots cost 40.
     */
    public int getBaseCost() {
        Set<TechType> closure = new HashSet<TechType>();
        collectClosure(this, closure);
        return 40 + 20 * closure.size();
    }

    private static void collectClosure(TechType t, Set<TechType> acc) {
        for (TechType p : t.prereqs) {
            if (acc.add(p)) {
                collectClosure(p, acc);
            }
        }
    }
}
