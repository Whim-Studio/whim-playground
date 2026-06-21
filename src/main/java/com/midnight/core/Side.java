package com.midnight.core;

/**
 * The two warring sides of Midnight. {@code FREE} is the player — the Lords of
 * Light led by Luxor the Moonprince. {@code DOOMDARK} is the Witchking and his
 * foul armies, who march south under cover of night.
 */
public enum Side {
    /** The player's Lords of Light. */
    FREE,
    /** The Witchking's armies. */
    DOOMDARK;

    /** The other side. */
    public Side opponent() {
        return this == FREE ? DOOMDARK : FREE;
    }
}
