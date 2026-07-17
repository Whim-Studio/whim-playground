package com.whim.xcom.rules.def;

import java.util.List;

/**
 * A research project. Cost is in scientist-days; {@link #prerequisites()} and
 * {@link #unlocks()} form the tech tree the {@code Ruleset} exposes.
 */
public interface ResearchNode extends GameDef {

    /** Scientist-days of work required. */
    int researchCost();

    /** Ids of research that must be complete before this becomes available. */
    List<String> prerequisites();

    /** Ids of research/manufacture/other content this unlocks on completion. */
    List<String> unlocks();

    /** {@code true} if a live captive or recovered item is consumed to start it. */
    boolean needsCaptiveOrItem();
}
