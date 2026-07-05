package com.taipan.model;

/**
 * The four tradeable goods of the Far East trade, with their reference
 * ("base") prices. Actual buy/sell prices fluctuate around these per port
 * per visit (see {@link com.taipan.controller.GameController}).
 *
 * Base prices follow the classic Taipan! ordering: General Cargo cheapest,
 * Opium most valuable (and most likely to be seized by the authorities).
 */
public enum Good {
    GENERAL("General Cargo", 90L),
    ARMS("Arms", 250L),
    SILK("Silk", 500L),
    OPIUM("Opium", 1000L);

    private final String display;
    private final long basePrice;

    Good(String display, long basePrice) {
        this.display = display;
        this.basePrice = basePrice;
    }

    public String display() {
        return display;
    }

    public long basePrice() {
        return basePrice;
    }
}
