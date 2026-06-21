package com.whim.hbdi.domain;

/** Immutable {@link QuadrantScore} implementation. */
public final class DefaultQuadrantScore implements QuadrantScore {

    private final Quadrant quadrant;
    private final double rawScore;
    private final double percentage;

    public DefaultQuadrantScore(Quadrant quadrant, double rawScore, double percentage) {
        if (quadrant == null) {
            throw new IllegalArgumentException("quadrant must not be null");
        }
        this.quadrant = quadrant;
        this.rawScore = rawScore;
        this.percentage = percentage;
    }

    public Quadrant getQuadrant() {
        return quadrant;
    }

    public double getRawScore() {
        return rawScore;
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    public String toString() {
        return quadrant + "(" + quadrant.getLabel() + ") raw=" + rawScore
                + " pct=" + percentage;
    }
}
