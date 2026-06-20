package com.tiwa.mahjong.engine;

/**
 * Result of the Fully Concealed Hand check (Section 5), exposed for the scorer (Task 3).
 * Carries the boolean verdict plus a human-readable reason for diagnostics/disputes.
 */
public final class FullyConcealedResult {

    private final boolean fullyConcealed;
    private final String reason;

    public FullyConcealedResult(boolean fullyConcealed, String reason) {
        this.fullyConcealed = fullyConcealed;
        this.reason = reason;
    }

    /** True if the hand qualifies for the Fully Concealed Hand bonus. */
    public boolean isFullyConcealed() {
        return fullyConcealed;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "FullyConcealedResult{fullyConcealed=" + fullyConcealed + ", reason='" + reason + "'}";
    }
}
