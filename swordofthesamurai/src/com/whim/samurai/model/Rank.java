package com.whim.samurai.model;

/**
 * Social rank in the feudal hierarchy. Progression samurai -> daimyo -> Shogun
 * mirrors the original's central ambition. See GAME_DESIGN_REFERENCE.md §4/§6.
 */
public enum Rank {
    RONIN("Ronin"),
    SAMURAI("Samurai"),
    LORD("Lord (Kashira)"),
    DAIMYO("Daimyo"),
    SHOGUN("Shogun");

    public final String title;
    Rank(String title) { this.title = title; }

    public Rank next() {
        Rank[] v = values();
        return this == SHOGUN ? SHOGUN : v[ordinal() + 1];
    }
}
