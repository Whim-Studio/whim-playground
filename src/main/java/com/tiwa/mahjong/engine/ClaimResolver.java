package com.tiwa.mahjong.engine;

import java.util.Collection;
import java.util.Optional;

/**
 * Resolves competing claims on a single discard (Section 3).
 *
 * <p>Rules implemented:</p>
 * <ul>
 *   <li>Claim window: over-the-board players have {@value #CLAIM_TIMEOUT_SECONDS} seconds to claim a
 *       discard. A claim whose {@link Claim#getElapsedMillis()} is at or beyond
 *       {@link #CLAIM_TIMEOUT_MILLIS} is too late and ignored. The window is injectable for testing;
 *       the default is {@link #CLAIM_TIMEOUT_MILLIS}.</li>
 *   <li>Strict priority: {@code MAHJONG > KONG > PUNG}.</li>
 *   <li>Multiple Mahjong (or equal-priority) claims are broken by turn order: the claimant nearest
 *       counter-clockwise to the discarder wins.</li>
 * </ul>
 *
 * <p>Seats are numbered clockwise (East=0, South=1, West=2, North=3 - see
 * {@link com.tiwa.mahjong.api.Wind}). Play proceeds counter-clockwise, i.e. by decreasing seat
 * index, so the player who plays immediately after the discarder is {@code (discarder + 3) % 4}.
 * "Nearest counter-clockwise" therefore means the smallest counter-clockwise step from the
 * discarder.</p>
 */
public final class ClaimResolver {

    /** The over-the-board claim window in seconds (Section 3). Not hardcoded inline anywhere else. */
    public static final int CLAIM_TIMEOUT_SECONDS = 6;

    /** The default claim window in milliseconds ({@value #CLAIM_TIMEOUT_SECONDS} seconds). */
    public static final long CLAIM_TIMEOUT_MILLIS = CLAIM_TIMEOUT_SECONDS * 1000L;

    private final long timeoutMillis;

    /** Resolver using the default {@link #CLAIM_TIMEOUT_MILLIS} window. */
    public ClaimResolver() {
        this(CLAIM_TIMEOUT_MILLIS);
    }

    /** Resolver with an injectable claim window (milliseconds), for deterministic tests. */
    public ClaimResolver(long timeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive, was " + timeoutMillis);
        }
        this.timeoutMillis = timeoutMillis;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * True if a claim arrived within the window. A claim exactly at the timeout boundary
     * (e.g. 6000 ms with the default window) is too late.
     */
    public boolean isWithinWindow(Claim claim) {
        return claim.getElapsedMillis() < timeoutMillis;
    }

    /**
     * Resolve the winning claim among competitors for one discard.
     *
     * @param claims        the competing claims (Pung/Kong/Mahjong); may be empty
     * @param discarderSeat the seat that discarded the tile (0-3)
     * @return the winning claim, or empty if no in-window claim was made
     */
    public Optional<Claim> resolve(Collection<Claim> claims, int discarderSeat) {
        if (discarderSeat < 0 || discarderSeat > 3) {
            throw new IllegalArgumentException("discarderSeat must be 0-3, was " + discarderSeat);
        }
        Claim best = null;
        for (Claim candidate : claims) {
            if (!isWithinWindow(candidate)) {
                continue;
            }
            if (candidate.getSeatIndex() == discarderSeat) {
                // The discarder cannot claim their own discard.
                continue;
            }
            if (best == null || beats(candidate, best, discarderSeat)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    /** True if {@code a} should win over the current best {@code b}. */
    private boolean beats(Claim a, Claim b, int discarderSeat) {
        int byPriority = a.getType().priority() - b.getType().priority();
        if (byPriority != 0) {
            return byPriority > 0;
        }
        // Equal priority (e.g. competing Mahjong claims): nearest counter-clockwise wins.
        return counterClockwiseDistance(discarderSeat, a.getSeatIndex())
                < counterClockwiseDistance(discarderSeat, b.getSeatIndex());
    }

    /**
     * Counter-clockwise steps from the discarder to the claimant (1-3). Play decreases seat index,
     * so stepping counter-clockwise from the discarder reaches {@code discarder-1}, {@code discarder-2},
     * {@code discarder-3}.
     */
    static int counterClockwiseDistance(int discarderSeat, int claimantSeat) {
        return ((discarderSeat - claimantSeat) % 4 + 4) % 4;
    }
}
