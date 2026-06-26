package com.whim.jutsutrainer.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An immutable record of a single jutsu: its name, chakra nature, rank, a
 * notable user, a description, and the ordered sequence of hand seals required
 * to perform it.
 */
public final class Jutsu {

    private final String name;
    private final ChakraNature nature;
    private final String rank;
    private final String user;
    private final String description;
    private final List<HandSeal> seals;

    /**
     * @param name        jutsu name
     * @param nature      chakra nature transformation
     * @param rank        rank, e.g. "A", "S", or "—" if unknown
     * @param user        notable user, or "" if none
     * @param description short description
     * @param seals       ordered hand-seal sequence (may be empty); defensively copied
     */
    public Jutsu(String name, ChakraNature nature, String rank, String user,
                 String description, List<HandSeal> seals) {
        this.name = name;
        this.nature = nature;
        this.rank = rank;
        this.user = user;
        this.description = description;
        List<HandSeal> copy = new ArrayList<HandSeal>(seals == null ? new ArrayList<HandSeal>() : seals);
        this.seals = Collections.unmodifiableList(copy);
    }

    public String getName() {
        return name;
    }

    public ChakraNature getNature() {
        return nature;
    }

    /** Rank, e.g. "A", "S", or "—" if unknown. */
    public String getRank() {
        return rank;
    }

    /** Notable user, or "" if none. */
    public String getUser() {
        return user;
    }

    public String getDescription() {
        return description;
    }

    /** Unmodifiable, ordered hand-seal sequence; may be empty. */
    public List<HandSeal> getSeals() {
        return seals;
    }

    @Override
    public String toString() {
        return name;
    }
}
