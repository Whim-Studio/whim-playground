package com.taipan.model;

/**
 * Central table of tunable game constants. Values were reconciled from two
 * widely-available descriptions of the original:
 *
 *  - Jay Link's C port "taipan" (itself derived from the Apple II original by
 *    Ronald Cain / Karl Hassel, based on Art Canfil's 1979 game), and
 *  - the general community wiki / playthrough descriptions of the Apple II and
 *    MS-DOS versions.
 *
 * Where the sources disagreed on an exact figure (they frequently do), an
 * internally-consistent value playable in a modern sitting was chosen. See
 * README "Faithfulness notes" for the deliberate deviations.
 */
public final class GameConstants {

    private GameConstants() {
    }

    // --- Identity defaults ---
    public static final String DEFAULT_TAIPAN = "Taipan";
    public static final String DEFAULT_FIRM = "Instant Profits";

    // --- Starting finances ---
    public static final long START_CASH = 400L;
    public static final long START_BANK = 0L;
    public static final long START_DEBT = 5000L;      // owed to Elder Brother Wu

    // --- Starting ship ---
    public static final int START_CAPACITY = 60;      // cargo units
    public static final int START_GUNS = 5;
    public static final int GUN_HOLD_SPACE = 10;      // each gun occupies this much hold
    public static final long GUN_PRICE = 1000L;

    // --- Calendar ---
    public static final int START_MONTH = 1;          // January
    public static final int START_YEAR = 1860;

    // --- Interest (applied once per voyage / month) ---
    public static final double DEBT_INTEREST = 0.10;  // Wu's debt grows 10% a month
    public static final double BANK_INTEREST = 0.005; // bank pays a modest 0.5%

    // --- Shipyard ---
    public static final int CAPACITY_UPGRADE_AMOUNT = 10;
    public static final long CAPACITY_UPGRADE_PRICE = 5000L;

    // --- Price fluctuation band (multiplied by Good.basePrice) ---
    public static final double PRICE_MIN_FACTOR = 0.45;
    public static final double PRICE_MAX_FACTOR = 2.6;
    // A rarer "price event" can push beyond the band:
    public static final double PRICE_GLUT_FACTOR = 0.25;   // very cheap
    public static final double PRICE_SHORTAGE_FACTOR = 3.5; // very dear
    public static final double PRICE_EVENT_CHANCE = 0.18;

    // --- Random event odds per voyage ---
    public static final double PIRATE_CHANCE = 0.40;
    public static final double STORM_CHANCE = 0.20;
    public static final double LI_YUEN_CHANCE = 0.20;   // extortion demand
    public static final double OPIUM_SEIZE_CHANCE = 0.12; // only if carrying opium

    // --- Storm ---
    public static final int STORM_MIN_DAMAGE = 3;
    public static final int STORM_MAX_DAMAGE = 30;
    public static final double STORM_SINK_CHANCE = 0.05;      // if already badly damaged
    public static final double STORM_BLOWN_OFF_COURSE = 0.25; // diverted to a random port

    // --- Combat ---
    public static final int MAX_ENEMY_SHIPS = 12;
    public static final double GUN_HIT_CHANCE = 0.65;
    public static final double GUN_SINK_CHANCE = 0.35;   // per hit, chance to sink a ship
    public static final int ENEMY_DAMAGE_PER_SHIP_MIN = 1;
    public static final int ENEMY_DAMAGE_PER_SHIP_MAX = 4;
    public static final long BOOTY_PER_SHIP = 250L;

    // --- Ship damage ---
    public static final int MAX_DAMAGE = 100; // at 100% the ship is lost

    // --- End game ---
    public static final long RETIRE_TARGET = 1_000_000L; // Wu names you a Taipan at this net worth
}
