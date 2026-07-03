package com.whim.ttr.api;

/**
 * Immutable rule constants for Ticket to Ride: Europe. Shared by every package
 * so the domain, engine, and UI never disagree on a magic number.
 */
public final class GameConstants {

    private GameConstants() { }

    /** Trains each player starts with. */
    public static final int TRAINS_PER_PLAYER = 45;

    /** Train stations each player starts with. */
    public static final int STATIONS_PER_PLAYER = 3;

    /** Card payment for the 1st / 2nd / 3rd station a player builds. */
    public static final int[] STATION_COST = { 1, 2, 3 };

    /** Bonus points for each unused station at game end. */
    public static final int UNUSED_STATION_BONUS = 4;

    /** Non-wild colors in the deck. */
    public static final int TRAIN_COLORS = 8;

    /** Cards per train color. */
    public static final int CARDS_PER_COLOR = 12;

    /** LOCOMOTIVE (wild) cards in the deck. */
    public static final int LOCOMOTIVE_CARDS = 14;

    /** Total train-car deck size: 8*12 + 14 = 110. */
    public static final int DECK_SIZE = TRAIN_COLORS * CARDS_PER_COLOR + LOCOMOTIVE_CARDS;

    /** Face-up card slots in the market. */
    public static final int FACE_UP_SLOTS = 5;

    /** If this many LOCOMOTIVEs are face-up at once, discard all 5 and redeal. */
    public static final int FACE_UP_LOCO_RESHUFFLE = 3;

    /** Number of cards flipped when resolving a TUNNEL. */
    public static final int TUNNEL_FLIP = 3;

    /** Train cards dealt to each player at the start. */
    public static final int STARTING_HAND = 4;

    /** Tickets offered at the very start, and the minimum a player must keep. */
    public static final int START_TICKETS_DEALT = 3;
    public static final int START_TICKETS_MIN_KEEP = 2;

    /** Tickets offered by the "draw tickets" action, and the minimum kept. */
    public static final int TICKETS_DEALT = 3;
    public static final int TICKETS_MIN_KEEP = 1;

    /** Endgame is armed once a player's train supply is at or below this. */
    public static final int ENDGAME_TRAIN_THRESHOLD = 2;

    /** European Express bonus for the single longest continuous path. */
    public static final int LONGEST_PATH_BONUS = 10;

    /** Points awarded by claimed route length (index = length). Index 0 unused. */
    public static final int[] ROUTE_POINTS = { 0, 1, 2, 4, 7, 10, 15, 18, 21 };

    /** Supported player counts. */
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;
}
