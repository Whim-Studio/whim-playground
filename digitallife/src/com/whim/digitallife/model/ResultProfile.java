package com.whim.digitallife.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * The computed outcome of a completed quiz: per-trait scores plus the derived
 * dominant trait and a friendly, generated summary paragraph.
 *
 * <p>Instances are immutable snapshots. The results screen and the save/load
 * layer both consume this type, so it deliberately contains no UI logic.</p>
 */
public final class ResultProfile {

    private final Map<Trait, Integer> scores;
    private final Trait dominant;
    private final int maxPossiblePerTrait;
    private final String summary;

    /**
     * @param scores              raw accumulated score for every {@link Trait}
     * @param maxPossiblePerTrait the theoretical maximum any single trait could
     *                            reach, used to normalize the bar chart to 0-100%
     */
    public ResultProfile(Map<Trait, Integer> scores, int maxPossiblePerTrait) {
        this.scores = new EnumMap<Trait, Integer>(scores);
        this.maxPossiblePerTrait = Math.max(1, maxPossiblePerTrait);
        this.dominant = computeDominant(this.scores);
        this.summary = buildSummary(this.dominant, this.scores);
    }

    private static Trait computeDominant(Map<Trait, Integer> scores) {
        Trait best = Trait.OPENNESS;
        int bestScore = Integer.MIN_VALUE;
        // Iterating over the enum (not the map) keeps ties resolved by a stable,
        // predictable declaration order rather than map iteration order.
        for (Trait trait : Trait.values()) {
            int value = scores.containsKey(trait) ? scores.get(trait) : 0;
            if (value > bestScore) {
                bestScore = value;
                best = trait;
            }
        }
        return best;
    }

    private static String buildSummary(Trait dominant, Map<Trait, Integer> scores) {
        // Find the runner-up (second highest) to give the paragraph a bit of nuance.
        Trait runnerUp = null;
        int runnerScore = Integer.MIN_VALUE;
        for (Trait trait : Trait.values()) {
            if (trait == dominant) {
                continue;
            }
            int value = scores.containsKey(trait) ? scores.get(trait) : 0;
            if (value > runnerScore) {
                runnerScore = value;
                runnerUp = trait;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Your standout dimension is ").append(dominant.getDisplayName())
                .append(". ").append(dominant.getBlurb());
        if (runnerUp != null) {
            sb.append(" You also show a strong streak of ")
                    .append(runnerUp.getDisplayName()).append(", which balances it out. ");
        }
        sb.append("Remember: this is a lighthearted, private snapshot generated only from ")
                .append("your own answers on this device — not a clinical assessment.");
        return sb.toString();
    }

    /** @return an unmodifiable map of raw scores per trait. */
    public Map<Trait, Integer> getScores() {
        return Collections.unmodifiableMap(scores);
    }

    /** @return the highest-scoring trait. */
    public Trait getDominant() {
        return dominant;
    }

    /** @return the theoretical per-trait maximum used for normalization. */
    public int getMaxPossiblePerTrait() {
        return maxPossiblePerTrait;
    }

    /** @return a generated, human-friendly summary paragraph. */
    public String getSummary() {
        return summary;
    }

    /**
     * Normalizes a trait's raw score to a 0-100 percentage of the maximum.
     *
     * @param trait the trait to look up
     * @return an integer percentage in the range [0, 100]
     */
    public int getPercent(Trait trait) {
        int value = scores.containsKey(trait) ? scores.get(trait) : 0;
        int pct = (int) Math.round((100.0 * value) / maxPossiblePerTrait);
        return Math.max(0, Math.min(100, pct));
    }
}
