package com.whim.samurai.model;

import java.io.Serializable;

/**
 * Strategic-layer clock. One "turn" = one season; four seasons = one year.
 * The original runs in real time across the Sengoku period; we discretise it
 * into seasonal turns for a clean turn/action structure (see design ref §1).
 */
public class Calendar implements Serializable {
    private static final long serialVersionUID = 1L;

    public int year;          // e.g. 1560
    public Season season = Season.SPRING;

    public Calendar() { this.year = 1560; }

    /** Advance one season, rolling the year over after winter. */
    public void advance() {
        if (season.rolloverToNext()) year++;
        season = season.next();
    }

    public String label() { return season.en + " " + year + " (" + season.jp + ")"; }
}
