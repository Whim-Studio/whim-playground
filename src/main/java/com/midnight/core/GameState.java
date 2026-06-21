package com.midnight.core;

import com.midnight.ai.NightResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The central engine object for The Lords of Midnight. It owns the {@link Map},
 * the roster of {@link Character}s, the day/night cycle, and the rules of
 * movement, recruitment, and victory.
 *
 * <p>Free lords act only during {@link Phase#DAY}: they look, move (paying
 * terrain-based hours, less if mounted), and recruit independents. At dusk the
 * player calls {@link #endDay(NightResolver)} — the engine hands the live state
 * to Doomdark's night brain exactly once, then breaks to a fresh dawn, resetting
 * each living lord's daylight hours and recovering a little stamina.
 *
 * <p>Victory: Morkin destroying the Ice Crown wins the Adventure; a Free lord
 * holding Ushgarak wins the Wargame; Luxor's death or the fall of Xajorkith
 * loses the game.
 */
public class GameState {

    /** Daylight hours every living lord receives at dawn. */
    public static final int DAY_HOURS = 24;
    /** Stamina recovered by each living lord at dawn. */
    private static final int DAWN_ENERGY_RECOVERY = 12;
    /** Stamina spent making a single move. */
    private static final int MOVE_ENERGY_COST = 2;

    private final Map map;
    private final List<Character> characters;
    private int day;
    private Phase phase;
    private Character selected;
    private final Location iceCrownRest;
    private boolean iceCrownDestroyed;

    private GameState(Map map, List<Character> characters, Location iceCrownRest) {
        this.map = map;
        this.characters = characters;
        this.day = 1;
        this.phase = Phase.DAY;
        this.iceCrownRest = iceCrownRest;
        this.iceCrownDestroyed = false;
    }

    /**
     * A fresh standard game: the canonical map, dawn of Day 1, Free to act. Seeds
     * Luxor, Morkin, Corleth and Rorthron (recruited and free), a handful of
     * independent free lords, and Doomdark's armies massing near Ushgarak. The
     * Ice Crown rests in the Tower of Doom in the far north.
     */
    public static GameState newGame() {
        Map map = Map.standard();
        List<Character> roster = new ArrayList<Character>();

        // The four companions of the Moonprince, free and already recruited,
        // mustering at Xajorkith in the south.
        Character luxor = lord("Luxor", Side.FREE, 28, 37, Direction.NORTH, 120, 110, 0, 1000, true);
        Character morkin = lord("Morkin", Side.FREE, 29, 37, Direction.NORTH, 127, 100, 0, 100, true);
        Character corleth = lord("Corleth the Fey", Side.FREE, 27, 37, Direction.NORTH, 110, 90, 0, 600, true);
        Character rorthron = lord("Rorthron the Wise", Side.FREE, 28, 36, Direction.NORTH, 110, 95, 0, 300, true);
        roster.add(luxor);
        roster.add(morkin);
        roster.add(corleth);
        roster.add(rorthron);

        // Independent free lords scattered across the middle lands, awaiting
        // recruitment (isRecruited() == false).
        roster.add(lord("Lord of the Dawn", Side.FREE, 20, 22, Direction.SOUTH, 100, 80, 200, 400, false));
        roster.add(lord("Lord Blood", Side.FREE, 38, 24, Direction.SOUTH, 100, 100, 100, 500, false));
        roster.add(lord("Lord Brith", Side.FREE, 15, 30, Direction.SOUTH, 100, 70, 0, 300, false));
        roster.add(lord("Lord Gard", Side.FREE, 45, 28, Direction.SOUTH, 100, 75, 150, 350, false));
        roster.add(lord("Lord Shimeril", Side.FREE, 33, 32, Direction.SOUTH, 100, 85, 100, 450, false));

        // Doomdark's armies massing in the wastes near Ushgarak in the north.
        roster.add(army("Doomguard of Ushgarak", 29, 4, 1500, 500));
        roster.add(army("Doomguard of the Plains", 33, 5, 1200, 800));
        roster.add(army("Doomguard of Kor", 16, 6, 1000, 400));

        // Reset hours to a full first day for every living lord.
        for (int i = 0; i < roster.size(); i++) {
            roster.get(i).setHoursRemaining(DAY_HOURS);
        }

        // The Ice Crown rests in the Tower of Doom in the far north.
        Location tower = Location.of(40, 1);
        GameState state = new GameState(map, roster, tower);
        state.selected = luxor;
        return state;
    }

    private static Character lord(String name, Side side, int x, int y, Direction facing,
                                  int energy, int courage, int riders, int warriors, boolean recruited) {
        Character c = new Character(name, side, Location.of(x, y), facing);
        c.setEnergy(energy);
        c.setCourage(courage);
        c.setRiders(riders);
        c.setWarriors(warriors);
        c.setRecruited(recruited);
        return c;
    }

    private static Character army(String name, int x, int y, int warriors, int riders) {
        Character c = new Character(name, Side.DOOMDARK, Location.of(x, y), Direction.SOUTH);
        c.setWarriors(warriors);
        c.setRiders(riders);
        c.setCourage(100);
        return c;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Map map() {
        return map;
    }

    public int day() {
        return day;
    }

    public Phase phase() {
        return phase;
    }

    public List<Character> characters() {
        return Collections.unmodifiableList(characters);
    }

    public List<Character> charactersOf(Side s) {
        List<Character> out = new ArrayList<Character>();
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.side() == s) {
                out.add(c);
            }
        }
        return out;
    }

    /** The player's lords: FREE, recruited, and still alive. */
    public List<Character> playerLords() {
        List<Character> out = new ArrayList<Character>();
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.side() == Side.FREE && c.isRecruited() && c.isAlive()) {
                out.add(c);
            }
        }
        return out;
    }

    /** Every living character standing on {@code loc}. */
    public List<Character> charactersAt(Location loc) {
        List<Character> out = new ArrayList<Character>();
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.isAlive() && c.location().equals(loc)) {
                out.add(c);
            }
        }
        return out;
    }

    public Character selected() {
        return selected;
    }

    public void select(Character c) {
        this.selected = c;
    }

    /** Where the Ice Crown currently rests — Morkin's tile while he bears it, else the Tower of Doom. */
    public Location iceCrownLocation() {
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.isAlive() && c.carriesIceCrown()) {
                return c.location();
            }
        }
        return iceCrownRest;
    }

    // ------------------------------------------------------------------
    // Day actions
    // ------------------------------------------------------------------

    /** True when {@code c} could legally step in direction {@code d} right now. */
    public boolean canMove(Character c, Direction d) {
        if (c == null || phase != Phase.DAY || !c.isAlive()) {
            return false;
        }
        if (c.side() != Side.FREE || !c.isRecruited()) {
            return false;
        }
        Location dest = c.location().neighbor(d);
        if (!map.isPassable(dest)) {
            return false;
        }
        return c.hoursRemaining() >= moveCost(c, d);
    }

    /** Daylight hours to step in direction {@code d}; mounted lords pay three-quarters. */
    public int moveCost(Character c, Direction d) {
        Location dest = c.location().neighbor(d);
        int base = map.terrainAt(dest).moveCost();
        if (c.isMounted()) {
            return (base * 3) / 4;
        }
        return base;
    }

    /**
     * Apply a step in direction {@code d}: deduct the hours, move the lord, face
     * him that way, and drain a little stamina. Returns false (changing nothing)
     * if the move is illegal — wrong phase, not a recruited free lord, off the
     * map, into impassable terrain, or short of hours.
     */
    public boolean move(Character c, Direction d) {
        if (!canMove(c, d)) {
            return false;
        }
        int cost = moveCost(c, d);
        c.setHoursRemaining(c.hoursRemaining() - cost);
        c.setLocation(c.location().neighbor(d));
        c.setFacing(d);
        c.setEnergy(c.energy() - MOVE_ENERGY_COST);
        return true;
    }

    /** Turn the lord to face {@code d} without moving — a free action, costing nothing. */
    public void look(Character c, Direction d) {
        if (c != null && c.isAlive()) {
            c.setFacing(d);
        }
    }

    /**
     * Recruit the independent lord {@code lord} if a recruiter — any recruited,
     * living free lord — stands on the same tile. The recruit joins the FREE side
     * under the player's command. Returns false if {@code lord} is already
     * recruited, not alive, or has no recruiter beside it.
     */
    public boolean tryRecruit(Character lord) {
        if (lord == null || !lord.isAlive() || lord.isRecruited()) {
            return false;
        }
        for (int i = 0; i < characters.size(); i++) {
            Character other = characters.get(i);
            if (other != lord && other.isAlive() && other.side() == Side.FREE
                    && other.isRecruited() && other.location().equals(lord.location())) {
                lord.setSide(Side.FREE);
                lord.setRecruited(true);
                return true;
            }
        }
        return false;
    }

    /**
     * If Morkin — and only Morkin — stands at the Ice Crown's resting place in the
     * Tower of Doom, he destroys it, winning the Adventure. Returns true on
     * success.
     */
    public boolean tryDestroyIceCrown() {
        if (iceCrownDestroyed) {
            return false;
        }
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.isMorkin() && c.isAlive() && c.location().equals(iceCrownLocation())) {
                c.setCarriesIceCrown(true);
                iceCrownDestroyed = true;
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Turn cycle
    // ------------------------------------------------------------------

    public boolean isDay() {
        return phase == Phase.DAY;
    }

    /**
     * End the day. The state turns to {@link Phase#NIGHT}, Doomdark's
     * {@code resolver} runs his entire night exactly once (moving armies and
     * resolving combat through core setters), and then dawn breaks: the day
     * advances, the phase returns to {@link Phase#DAY}, and every living lord has
     * his daylight hours restored and a little stamina recovered.
     *
     * @throws IllegalArgumentException if {@code resolver} is null
     */
    public void endDay(NightResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("NightResolver must not be null");
        }
        phase = Phase.NIGHT;
        resolver.resolveNight(this);

        // Dawn of the next day.
        day++;
        phase = Phase.DAY;
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.isAlive()) {
                c.setHoursRemaining(DAY_HOURS);
                c.setEnergy(c.energy() + DAWN_ENERGY_RECOVERY);
            }
        }
    }

    // ------------------------------------------------------------------
    // Victory
    // ------------------------------------------------------------------

    /** The current state of the war. */
    public Outcome outcome() {
        if (iceCrownDestroyed) {
            return Outcome.FREE_ADVENTURE_WIN;
        }
        List<Stronghold> holds = map.strongholds();
        for (int i = 0; i < holds.size(); i++) {
            Stronghold s = holds.get(i);
            if (s.isUshgarak() && s.owner() == Side.FREE) {
                return Outcome.FREE_WARGAME_WIN;
            }
        }
        for (int i = 0; i < holds.size(); i++) {
            Stronghold s = holds.get(i);
            if (s.isXajorkith() && s.owner() == Side.DOOMDARK) {
                return Outcome.DOOMDARK_WIN;
            }
        }
        if (luxorIsDead()) {
            return Outcome.DOOMDARK_WIN;
        }
        return Outcome.ONGOING;
    }

    public boolean isOver() {
        return outcome() != Outcome.ONGOING;
    }

    private boolean luxorIsDead() {
        for (int i = 0; i < characters.size(); i++) {
            Character c = characters.get(i);
            if (c.isLuxor()) {
                return !c.isAlive();
            }
        }
        // No Luxor on the board at all counts as slain.
        return true;
    }
}
