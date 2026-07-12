package com.whim.capes.model;

/**
 * The two kinds of Conflict (rulebook pp.28-29).
 * <ul>
 *   <li>EVENT: declares something that <em>will</em> happen; the Resolver
 *       narrates <em>how</em>. May be vetoed by any player before it is
 *       established.</li>
 *   <li>GOAL: declares that a character is <em>trying</em> to do something;
 *       the Resolver narrates <em>whether</em> they succeed. Veto is
 *       restricted (p.29).</li>
 * </ul>
 */
public enum ConflictType {
    EVENT("Event", "Says what will occur. The Resolver narrates how it happens."),
    GOAL("Goal", "Says someone is trying to do something. The Resolver narrates whether they succeed.");

    private final String label;
    private final String blurb;

    ConflictType(String label, String blurb) {
        this.label = label;
        this.blurb = blurb;
    }

    public String label() { return label; }
    public String blurb() { return blurb; }
}
