package com.whim.digitallife.model;

import java.awt.Color;

/**
 * The five personality dimensions scored by the quiz.
 *
 * <p>This is a light, friendly re-interpretation of the well-known "Big Five"
 * (OCEAN) model. Each trait carries a human-readable display name, a short
 * one-line blurb used on the results screen, and a theme color used by the
 * bar-chart visualization so every dimension is visually distinct.</p>
 */
public enum Trait {

    OPENNESS("Openness", "Curious, imaginative, open to new ideas and experiences.",
            new Color(0x6C5CE7)),
    CONSCIENTIOUSNESS("Conscientiousness", "Organized, dependable, and goal-driven.",
            new Color(0x00B894)),
    EXTRAVERSION("Extraversion", "Energized by people, social, and outgoing.",
            new Color(0xE17055)),
    AGREEABLENESS("Agreeableness", "Warm, cooperative, and considerate of others.",
            new Color(0x0984E3)),
    STABILITY("Emotional Stability", "Calm, resilient, and steady under pressure.",
            new Color(0xFDCB6E));

    private final String displayName;
    private final String blurb;
    private final Color color;

    Trait(String displayName, String blurb, Color color) {
        this.displayName = displayName;
        this.blurb = blurb;
        this.color = color;
    }

    /** @return the friendly label shown in the UI. */
    public String getDisplayName() {
        return displayName;
    }

    /** @return a short one-line description of what this trait means. */
    public String getBlurb() {
        return blurb;
    }

    /** @return the theme color associated with this trait. */
    public Color getColor() {
        return color;
    }
}
