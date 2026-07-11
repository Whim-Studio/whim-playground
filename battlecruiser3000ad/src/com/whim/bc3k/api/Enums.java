package com.whim.bc3k.api;

/**
 * All shared enumerations for the Battlecruiser 3000AD recreation.
 * The app shell, engine, and (future) console tasks all compile against these.
 * See BC3K_Phase1_Design.md for the sourcing behind each concept; anything not
 * verifiable from an accessible source is a labelled design approximation.
 */
public final class Enums {
    private Enums() {}

    /**
     * Top-level screen the shell renders. The bridge is not one screen but a set
     * of switchable consoles (matching BC3K's "one console visible at a time"),
     * so each bridge console is its own Mode reached by a function key.
     */
    public enum Mode {
        MENU,
        NAV, TACTICAL, ENGINEERING, POWER, COMMS, CARGO, PERSONNEL, FLIGHTDECK,
        GROUND, GAME_OVER;

        /** True for the eight in-bridge consoles (not MENU/GROUND/GAME_OVER). */
        public boolean isConsole() { return this != MENU && this != GAME_OVER && this != GROUND; }
    }

    /**
     * The three primary game modes of BC3K. A session runs in exactly one; it is
     * a property of the running game, not a screen. (Verified: Wikipedia.)
     */
    public enum GameMode {
        FREE_FLIGHT("Free Flight", "Explore and learn the controls. No hostiles."),
        XTREME_CARNAGE("Xtreme Carnage", "Combat simulator. Fight until destroyed."),
        CAMPAIGN("Advanced Campaign", "Dynamic, persistent GALCOM vs. Gammulan war.");

        private final String title;
        private final String blurb;
        GameMode(String title, String blurb) { this.title = title; this.blurb = blurb; }
        public String title() { return title; }
        public String blurb() { return blurb; }
    }

    /** Reactor-power consumers. The PWR console distributes a finite budget across these. */
    public enum PowerSystem {
        SHIELDS("Shields"), WEAPONS("Weapons"), ENGINES("Engines"),
        LIFE_SUPPORT("Life Support"), SENSORS("Sensors");

        private final String label;
        PowerSystem(String label) { this.label = label; }
        public String label() { return label; }
    }

    /** Bridge alert condition, shown in the always-on HUD. */
    public enum Alert { GREEN, YELLOW, RED }

    /** Launchable small craft carried by the battlecruiser. (Verified: brief.) */
    public enum CraftType { FIGHTER, SHUTTLE, ATV }

    /** A movement/station order the player can issue to a crew member. */
    public enum CrewOrder {
        REST("send to quarters"),
        EAT("send to galley"),
        TO_BRIDGE("station on bridge"),
        TO_ENGINEERING("station in engineering"),
        TO_TACTICAL("station at tactical");

        private final String label;
        CrewOrder(String label) { this.label = label; }
        public String label() { return label; }
    }
}
