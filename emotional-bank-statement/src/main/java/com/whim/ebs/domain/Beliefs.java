package com.whim.ebs.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static catalog of the available {@link Belief} values.
 *
 * <p>The catalog is built once from a fixed, ordered list and exposed as an
 * unmodifiable {@link List}.</p>
 */
public final class Beliefs {

    private static final List<Belief> ALL;
    private static final List<String> ALL_NAMES;

    static {
        String[] names = new String[] {
            "Accountability", "Altruism", "Achievement", "Adaptability", "Adventure",
            "Ambition", "Authenticity", "Balance", "Beauty", "Being the best",
            "Belonging", "Career", "Caring", "Collaboration", "Commitment",
            "Community", "Compassion", "Competence", "Confidence", "Connection",
            "Contentment", "Contribution", "Cooperation", "Courage", "Creativity",
            "Curiosity", "Dignity", "Diversity", "Experience", "Efficiency",
            "Equality", "Ethics", "Excellence", "Fairness", "Faith",
            "Family", "Financial stability", "Forgiveness", "Freedom", "Friendship",
            "Fun", "Future generations", "Generosity", "Giving back", "Grace",
            "Gratitude", "Growth", "Harmony", "Health", "Home",
            "Honesty", "Hope", "Humility", "Humor", "Inclusion",
            "Independence", "Initiative", "Integrity", "Intuition", "Joy",
            "Justice", "Kindness", "Knowledge", "Leadership", "Learning",
            "Legacy", "Leisure", "Love", "Loyalty", "Making a difference",
            "Nature", "Openness", "Optimism", "Order", "Parenting",
            "Patience", "Patriotism", "Peace", "Perseverance", "Personal fulfillment",
            "Power", "Pride", "Recognition", "Reliability", "Resourcefulness",
            "Respect", "Responsibility", "Risk-taking", "Safety", "Security",
            "Self-discipline", "Self-expression", "Self-respect", "Serenity", "Service",
            "Simplicity"
        };
        List<Belief> beliefs = new ArrayList<Belief>(names.length);
        List<String> beliefNames = new ArrayList<String>(names.length);
        for (int i = 0; i < names.length; i++) {
            beliefs.add(new Belief(names[i]));
            beliefNames.add(names[i]);
        }
        ALL = Collections.unmodifiableList(beliefs);
        ALL_NAMES = Collections.unmodifiableList(beliefNames);
    }

    private Beliefs() {
    }

    /**
     * Returns the full, ordered, unmodifiable list of beliefs.
     *
     * @return all beliefs
     */
    public static List<Belief> all() {
        return ALL;
    }

    /**
     * Returns the full, ordered, unmodifiable list of belief names.
     *
     * @return all belief names
     */
    public static List<String> allNames() {
        return ALL_NAMES;
    }
}
