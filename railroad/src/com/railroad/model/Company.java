package com.railroad.model;

/**
 * The player's railroad company. Phase 1 only tracks a name and a cash treasury.
 *
 * <p>Loans, bonds and stock are explicitly out of scope for Phase 1. The only
 * hooks left for them are these clearly-named cash methods, so later phases can
 * add {@code issueBond()} / {@code takeLoan()} that funnel through the same
 * treasury without reworking spending call sites.
 */
public final class Company {

    private final String name;
    private long cash;

    public Company(String name, long startingCash) {
        this.name = name;
        this.cash = startingCash;
    }

    public String getName() {
        return name;
    }

    public long getCash() {
        return cash;
    }

    public boolean canAfford(long amount) {
        return cash >= amount;
    }

    /**
     * Deducts {@code amount} if affordable.
     *
     * @return true if the spend succeeded, false if funds were insufficient
     *         (treasury unchanged).
     */
    public boolean spend(long amount) {
        if (amount < 0 || cash < amount) {
            return false;
        }
        cash -= amount;
        return true;
    }

    /** Adds revenue (or any credit) to the treasury. */
    public void earn(long amount) {
        cash += amount;
    }
}
