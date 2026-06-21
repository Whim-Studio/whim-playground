package com.midnight.core;

/**
 * The half of the cycle currently in play. Free lords act only during
 * {@link #DAY}; at {@link #NIGHT} they are frozen while Doomdark's armies march.
 */
public enum Phase {
    DAY,
    NIGHT
}
