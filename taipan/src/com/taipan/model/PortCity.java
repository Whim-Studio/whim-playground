package com.taipan.model;

/**
 * The seven ports of call. Hong Kong is the home port: the bank, the
 * money-lender (Elder Brother Wu), the shipyard and McHenry the ship-fitter
 * are all found there. The other six are pure trading stops.
 */
public enum PortCity {
    HONG_KONG("Hong Kong"),
    SHANGHAI("Shanghai"),
    NAGASAKI("Nagasaki"),
    SAIGON("Saigon"),
    MANILA("Manila"),
    SINGAPORE("Singapore"),
    BATAVIA("Batavia");

    private final String display;

    PortCity(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    public boolean isHome() {
        return this == HONG_KONG;
    }
}
