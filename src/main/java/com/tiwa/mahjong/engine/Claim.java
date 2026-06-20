package com.tiwa.mahjong.engine;

/**
 * A single competing claim on a discard: who claimed (seat 0-3), what they claimed, and how long
 * after the discard the claim arrived.
 *
 * <p>The elapsed time makes the 6-second claim window (Section 3) deterministic and testable: no
 * real wall-clock or {@code Thread.sleep} is used by the engine. See {@link ClaimResolver}.</p>
 */
public final class Claim {

    private final int seatIndex;
    private final ClaimType type;
    private final long elapsedMillis;

    public Claim(int seatIndex, ClaimType type, long elapsedMillis) {
        if (seatIndex < 0 || seatIndex > 3) {
            throw new IllegalArgumentException("seatIndex must be 0-3, was " + seatIndex);
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("elapsedMillis must not be negative");
        }
        this.seatIndex = seatIndex;
        this.type = type;
        this.elapsedMillis = elapsedMillis;
    }

    /** Convenience constructor for a claim that arrived instantly (0 ms). */
    public Claim(int seatIndex, ClaimType type) {
        this(seatIndex, type, 0L);
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public ClaimType getType() {
        return type;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    @Override
    public String toString() {
        return "Claim{seat=" + seatIndex + ", type=" + type + ", elapsedMillis=" + elapsedMillis + '}';
    }
}
