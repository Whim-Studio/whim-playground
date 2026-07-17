package com.whim.xcom.model;

/**
 * The three firing modes of 1994 UFO: Enemy Unknown. Each weapon publishes an
 * accuracy multiplier and a TU cost (as a percentage of the shooter's max TUs)
 * per available mode; not every weapon supports every mode.
 */
public enum FireMode {
    /** Fast, low accuracy. */
    SNAP,
    /** Slow, high accuracy, single shot. */
    AIMED,
    /** Burst of shots, low per-shot accuracy. */
    AUTO
}
