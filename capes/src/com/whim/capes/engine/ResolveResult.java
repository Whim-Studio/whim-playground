package com.whim.capes.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of a Resolve (or Deadlock/Gloat) for logging and UI feedback. Records
 * the token/inspiration movements so the audit log can report exactly what the
 * math produced.
 */
public final class ResolveResult {
    public enum Kind { NORMAL, DEADLOCK, GLOAT }

    public final Kind kind;
    public final String conflictTitle;
    public final String resolverPlayerId;
    public final List<String> lines = new ArrayList<String>(); // human-readable movement log

    public int storyTokensAwarded;
    public int storyTokensDiscarded;
    public int debtReturned;
    public int resolverInspirations;
    public int opposingInspirations;

    public ResolveResult(Kind kind, String conflictTitle, String resolverPlayerId) {
        this.kind = kind;
        this.conflictTitle = conflictTitle;
        this.resolverPlayerId = resolverPlayerId;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(kind).append(" \"").append(conflictTitle).append("\": ");
        sb.append(debtReturned).append(" Debt returned, ");
        sb.append(storyTokensAwarded).append(" Story Tokens awarded");
        if (storyTokensDiscarded > 0) sb.append(" (").append(storyTokensDiscarded).append(" discarded)");
        sb.append(", ").append(resolverInspirations).append("+").append(opposingInspirations)
          .append(" Inspirations.");
        return sb.toString();
    }
}
