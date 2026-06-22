package com.whim.babylon5.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.ConflictType;

/**
 * Immutable outcome of a resolved {@link Conflict}.
 *
 * <p>Rulebook ("The Resolution Round", Step 1): "Total modified support must exceed total
 * modified opposition for the conflict to succeed; otherwise it fails." That STRICT
 * comparison is captured by {@link #initiatorWon()}.
 */
public final class ConflictResult {

    private final boolean initiatorWon;
    private final int supportTotal;
    private final int oppositionTotal;
    private final ConflictType type;
    private final List<Card> neutralized;
    private final String summary;

    public ConflictResult(boolean initiatorWon, int supportTotal, int oppositionTotal,
                          ConflictType type, List<Card> neutralized, String summary) {
        this.initiatorWon = initiatorWon;
        this.supportTotal = supportTotal;
        this.oppositionTotal = oppositionTotal;
        this.type = type;
        this.neutralized = Collections.unmodifiableList(
                new ArrayList<Card>(neutralized == null ? new ArrayList<Card>() : neutralized));
        this.summary = summary;
    }

    /** true iff modified support STRICTLY exceeds modified opposition. */
    public boolean initiatorWon() {
        return initiatorWon;
    }

    public int supportTotal() {
        return supportTotal;
    }

    public int oppositionTotal() {
        return oppositionTotal;
    }

    public ConflictType type() {
        return type;
    }

    /** Cards neutralized (damaged out) as a consequence of this conflict. */
    public List<Card> neutralized() {
        return neutralized;
    }

    /** Human-readable one-liner for the log. */
    public String summary() {
        return summary;
    }
}
