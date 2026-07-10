package com.whim.merchantprince.model;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * A competing Venetian merchant family — the player or an AI rival
 * (GAME_DESIGN_REFERENCE §1, §8). A family is identified by a surname and heraldic
 * crest colour rather than an individual name. It tracks liquid florins, held
 * offices, reputation (damaged by being caught at dirty tricks), and counts of
 * bribed senators/cardinals which contribute to final net worth (§7).
 */
public class Family implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int id;
    public String surname;
    /** ARGB crest colour, chosen at family creation. */
    public int crestColor;
    public final boolean human;

    public long florins;
    /** 0..100 standing in Venice; dirty tricks caught reduce it. */
    public int reputation = 50;

    public final EnumSet<Office> offices = EnumSet.noneOf(Office.class);
    /** Senators (Council of Ten members) bribed into the family's pocket. */
    public int senatorsBribed = 0;
    /** Cardinals influenced toward the family. */
    public int cardinalsBribed = 0;
    /** True once the family has built a "den of iniquities" for dirty tricks. */
    public boolean denOfIniquities = false;

    public boolean eliminated = false;

    public Family(int id, String surname, int crestColor, boolean human, long florins) {
        this.id = id;
        this.surname = surname;
        this.crestColor = crestColor;
        this.human = human;
        this.florins = florins;
    }

    public boolean hasOffice(Office o) { return offices.contains(o); }
}
