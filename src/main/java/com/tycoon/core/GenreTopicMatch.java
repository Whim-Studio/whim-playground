package com.tycoon.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentic Mad Games Tycoon-style genre/topic fit table.
 *
 * <p>In Mad Games Tycoon a game's genre and topic have to suit each other: an
 * RPG set in a Fantasy world is a natural hit, while a Sport game with a Fantasy
 * topic is a hard sell. This class bakes a curated fit matrix directly into the
 * domain so the simulation rewards sensible pairings.</p>
 *
 * <p>Each pairing resolves to a {@link Rating} carrying a review-score
 * {@link Rating#multiplier multiplier}. Pairings not explicitly listed default
 * to {@link Rating#OK} (a neutral 1.0x).</p>
 */
public final class GenreTopicMatch {

    /** Qualitative fit plus the multiplier the sim applies to the review base. */
    public enum Rating {
        PERFECT("Perfect fit", 1.30),
        GREAT("Great fit", 1.18),
        GOOD("Good fit", 1.08),
        OK("Workable", 1.00),
        POOR("Poor fit", 0.80),
        BAD("Bad fit", 0.62);

        private final String label;
        private final double multiplier;

        Rating(String label, double multiplier) {
            this.label = label;
            this.multiplier = multiplier;
        }

        public String label() {
            return label;
        }

        public double multiplier() {
            return multiplier;
        }
    }

    private static final Map<String, Rating> TABLE = new HashMap<String, Rating>();

    private static String key(Genre g, Topic t) {
        return g.name() + "|" + t.name();
    }

    private static void put(Genre g, Topic t, Rating r) {
        TABLE.put(key(g, t), r);
    }

    static {
        // ---- RPG: thrives on rich worlds -----------------------------------
        put(Genre.RPG, Topic.FANTASY, Rating.PERFECT);
        put(Genre.RPG, Topic.MEDIEVAL, Rating.GREAT);
        put(Genre.RPG, Topic.SCIENCE_FICTION, Rating.GREAT);
        put(Genre.RPG, Topic.SPACE, Rating.GOOD);
        put(Genre.RPG, Topic.CYBERPUNK, Rating.GREAT);
        put(Genre.RPG, Topic.POST_APOCALYPSE, Rating.GOOD);
        put(Genre.RPG, Topic.SOCCER, Rating.BAD);
        put(Genre.RPG, Topic.CARS, Rating.POOR);

        // ---- Action: war, monsters, capes ----------------------------------
        put(Genre.ACTION, Topic.MILITARY, Rating.PERFECT);
        put(Genre.ACTION, Topic.ZOMBIES, Rating.GREAT);
        put(Genre.ACTION, Topic.SUPERHEROES, Rating.GREAT);
        put(Genre.ACTION, Topic.CYBERPUNK, Rating.GOOD);
        put(Genre.ACTION, Topic.HORROR, Rating.GOOD);
        put(Genre.ACTION, Topic.POST_APOCALYPSE, Rating.GOOD);
        put(Genre.ACTION, Topic.ECONOMY, Rating.POOR);
        put(Genre.ACTION, Topic.COMEDY, Rating.POOR);

        // ---- Adventure: story-driven settings ------------------------------
        put(Genre.ADVENTURE, Topic.DETECTIVE, Rating.PERFECT);
        put(Genre.ADVENTURE, Topic.HORROR, Rating.GREAT);
        put(Genre.ADVENTURE, Topic.PIRATES, Rating.GREAT);
        put(Genre.ADVENTURE, Topic.FANTASY, Rating.GOOD);
        put(Genre.ADVENTURE, Topic.COMEDY, Rating.GOOD);
        put(Genre.ADVENTURE, Topic.SOCCER, Rating.BAD);

        // ---- Strategy: maps, armies, economies -----------------------------
        put(Genre.STRATEGY, Topic.MILITARY, Rating.PERFECT);
        put(Genre.STRATEGY, Topic.ECONOMY, Rating.GREAT);
        put(Genre.STRATEGY, Topic.SPACE, Rating.GREAT);
        put(Genre.STRATEGY, Topic.MEDIEVAL, Rating.GOOD);
        put(Genre.STRATEGY, Topic.POST_APOCALYPSE, Rating.GOOD);
        put(Genre.STRATEGY, Topic.COMEDY, Rating.POOR);
        put(Genre.STRATEGY, Topic.SOCCER, Rating.POOR);

        // ---- Simulation: systems and life ----------------------------------
        put(Genre.SIMULATION, Topic.ECONOMY, Rating.PERFECT);
        put(Genre.SIMULATION, Topic.CARS, Rating.GREAT);
        put(Genre.SIMULATION, Topic.SPACE, Rating.GOOD);
        put(Genre.SIMULATION, Topic.MILITARY, Rating.GOOD);
        put(Genre.SIMULATION, Topic.HORROR, Rating.POOR);
        put(Genre.SIMULATION, Topic.SUPERHEROES, Rating.POOR);

        // ---- Sport: realism wins -------------------------------------------
        put(Genre.SPORT, Topic.SOCCER, Rating.PERFECT);
        put(Genre.SPORT, Topic.CARS, Rating.GOOD);
        put(Genre.SPORT, Topic.FANTASY, Rating.BAD);
        put(Genre.SPORT, Topic.SPACE, Rating.BAD);
        put(Genre.SPORT, Topic.HORROR, Rating.BAD);
        put(Genre.SPORT, Topic.ZOMBIES, Rating.POOR);

        // ---- Racing: speed --------------------------------------------------
        put(Genre.RACING, Topic.CARS, Rating.PERFECT);
        put(Genre.RACING, Topic.SCIENCE_FICTION, Rating.GOOD);
        put(Genre.RACING, Topic.CYBERPUNK, Rating.GOOD);
        put(Genre.RACING, Topic.MEDIEVAL, Rating.BAD);
        put(Genre.RACING, Topic.DETECTIVE, Rating.POOR);
        put(Genre.RACING, Topic.ECONOMY, Rating.POOR);

        // ---- Skill / Puzzle: light, broad-appeal themes --------------------
        put(Genre.PUZZLE, Topic.COMEDY, Rating.GREAT);
        put(Genre.PUZZLE, Topic.FANTASY, Rating.GOOD);
        put(Genre.PUZZLE, Topic.MILITARY, Rating.POOR);
        put(Genre.PUZZLE, Topic.HORROR, Rating.POOR);
        put(Genre.SKILL, Topic.COMEDY, Rating.GREAT);
        put(Genre.SKILL, Topic.CARS, Rating.GOOD);
        put(Genre.SKILL, Topic.MILITARY, Rating.POOR);
    }

    private GenreTopicMatch() {
    }

    /**
     * Fit for a genre/topic pairing. Unknown pairings (and any with a null
     * genre or topic) resolve to {@link Rating#OK} so an unspecified project
     * is never penalised.
     */
    public static Rating rate(Genre genre, Topic topic) {
        if (genre == null || topic == null) {
            return Rating.OK;
        }
        Rating r = TABLE.get(key(genre, topic));
        return r != null ? r : Rating.OK;
    }

    /** Convenience: the review-score multiplier for a pairing. */
    public static double multiplier(Genre genre, Topic topic) {
        return rate(genre, topic).multiplier();
    }
}
