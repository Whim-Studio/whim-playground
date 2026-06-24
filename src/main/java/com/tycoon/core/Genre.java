package com.tycoon.core;

/**
 * Authentic Mad Games Tycoon-style game genres. These are the high-level
 * gameplay categories a {@link GameProject} is built around; how well a genre
 * pairs with a {@link Topic} is decided by {@link GenreTopicMatch}.
 */
public enum Genre {
    SKILL("Skill"),
    ACTION("Action"),
    ADVENTURE("Adventure"),
    RPG("Role-Playing"),
    STRATEGY("Strategy"),
    SIMULATION("Simulation"),
    SPORT("Sport"),
    RACING("Racing"),
    PUZZLE("Puzzle");

    private final String display;

    Genre(String display) {
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
