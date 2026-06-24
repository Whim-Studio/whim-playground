package com.tycoon.core;

/**
 * Authentic Mad Games Tycoon-style topics (settings/themes) a game can use.
 * The genre/topic pairing drives the quality bonus applied at review time —
 * see {@link GenreTopicMatch}.
 */
public enum Topic {
    FANTASY("Fantasy"),
    SCIENCE_FICTION("Science Fiction"),
    SPACE("Space"),
    HORROR("Horror"),
    MILITARY("Military"),
    MEDIEVAL("Medieval"),
    CYBERPUNK("Cyberpunk"),
    POST_APOCALYPSE("Post-Apocalypse"),
    DETECTIVE("Detective"),
    PIRATES("Pirates"),
    ZOMBIES("Zombies"),
    SUPERHEROES("Superheroes"),
    SOCCER("Soccer"),
    CARS("Cars"),
    ECONOMY("Economy"),
    COMEDY("Comedy");

    private final String display;

    Topic(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    @Override
    public String toString() {
        return display;
    }
}
