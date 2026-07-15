package com.whim.settlers.transport;

import com.whim.settlers.economy.Good;

/**
 * One good in transit toward a destination flag. As it relays flag-to-flag it is
 * re-queued at each intermediate flag; when it reaches {@link #destFlagId} the
 * {@link #onArrive} callback runs (deposit into a stockpile or a building buffer).
 */
public final class Shipment {

    private final Good good;
    private final int destFlagId;
    private final Runnable onArrive;

    public Shipment(Good good, int destFlagId, Runnable onArrive) {
        this.good = good;
        this.destFlagId = destFlagId;
        this.onArrive = onArrive;
    }

    public Good good()      { return good; }
    public int destFlagId() { return destFlagId; }
    public void deliver()   { onArrive.run(); }
}
