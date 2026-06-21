package com.whim.startrek.domain;

/**
 * Tradable / stockpiled resources. OFFICERS are special: they are crew, never a
 * market commodity, so {@link #isTradable()} returns false and the economy engine
 * must reject any attempt to buy or sell them.
 */
public enum ResourceType {
    DILITHIUM(true), DEUTERIUM(true), CREDITS(true), METALS(true), OFFICERS(false);

    private final boolean tradable;

    ResourceType(boolean tradable) {
        this.tradable = tradable;
    }

    public boolean isTradable() {
        return tradable; // OFFICERS are NEVER tradable
    }
}
