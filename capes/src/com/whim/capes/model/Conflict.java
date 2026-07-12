package com.whim.capes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A Conflict card on the table (pp.26-30): a titled situation with (at least)
 * two {@link ConflictSide}s that characters vie to Control. May be an EVENT or a
 * GOAL. Tracks who created it (relevant for Story Token routing on Resolve,
 * p.30) and whether it has been Resolved.
 *
 * <p>Extended participant types (Non-persons/Things/Phenomena/Situations, Ch.5)
 * attach via {@link #characterConflictOwnerId}: when such a Conflict Resolves,
 * that participant leaves the Scene (p.103).
 */
public final class Conflict implements java.io.Serializable {
    private final String id;
    private String title;
    private ConflictType type;
    private final String creatorPlayerId;
    private final List<ConflictSide> sides = new ArrayList<ConflictSide>();
    private boolean resolved;

    /** Non-null when this Conflict is the built-in Character Conflict of a Ch.5 participant. */
    private String characterConflictOwnerId;

    public Conflict(String id, String title, ConflictType type, String creatorPlayerId) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.creatorPlayerId = creatorPlayerId;
        // A fresh Conflict has two undefined sides, each with a die at 1 (p.26).
        sides.add(new ConflictSide(null));
        sides.add(new ConflictSide(null));
    }

    public String id() { return id; }
    public String title() { return title; }
    public void setTitle(String t) { this.title = t; }
    public ConflictType type() { return type; }
    public void setType(ConflictType t) { this.type = t; }
    public String creatorPlayerId() { return creatorPlayerId; }
    public List<ConflictSide> sides() { return sides; }
    public boolean isResolved() { return resolved; }
    public void markResolved() { this.resolved = true; }

    public String characterConflictOwnerId() { return characterConflictOwnerId; }
    public void setCharacterConflictOwnerId(String id) { this.characterConflictOwnerId = id; }

    /** Adds a further side (used when a player Splits off to found a new side, p.37). */
    public ConflictSide addSide(String resolutionStatement) {
        ConflictSide s = new ConflictSide(resolutionStatement);
        sides.add(s);
        return s;
    }

    /** The side with the strictly-highest total Controls; null if tied (p.26). */
    public ConflictSide controllingSide() {
        ConflictSide best = null;
        boolean tie = false;
        for (ConflictSide s : sides) {
            if (best == null || s.total() > best.total()) { best = s; tie = false; }
            else if (s.total() == best.total()) { tie = true; }
        }
        return tie ? null : best;
    }

    public boolean isTied() { return controllingSide() == null; }
}
