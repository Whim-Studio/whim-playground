package com.taipan.controller;

import com.taipan.model.GameConstants;
import com.taipan.model.GameState;
import com.taipan.model.Good;
import com.taipan.model.PortCity;
import com.taipan.model.Ship;

import java.util.Random;

/**
 * All game logic: price generation, trading, banking, the shipyard, voyage
 * event resolution and scoring. The Swing views delegate every rule to this
 * class and never mutate the model directly.
 *
 * Methods that a player can misuse (buy/sell/borrow/...) return {@code null} on
 * success or a human-readable error string on failure, so the UI can show the
 * message and let the player retry — nothing ever throws at the player.
 */
public class GameController {

    private final GameState state;
    private final Random rng;

    public GameController(String taipanName, String firmName, Long seed) {
        Random r = (seed == null) ? new Random() : new Random(seed);
        this.state = new GameState(taipanName, firmName, r);
        this.rng = r;
        generatePrices();
    }

    public GameState getState() {
        return state;
    }

    // ------------------------------------------------------------------ prices

    /** Roll fresh prices for every good at the current port. */
    public void generatePrices() {
        for (Good g : Good.values()) {
            double factor;
            if (rng.nextDouble() < GameConstants.PRICE_EVENT_CHANCE) {
                factor = rng.nextBoolean()
                        ? GameConstants.PRICE_GLUT_FACTOR
                        : GameConstants.PRICE_SHORTAGE_FACTOR;
            } else {
                factor = GameConstants.PRICE_MIN_FACTOR
                        + rng.nextDouble()
                        * (GameConstants.PRICE_MAX_FACTOR - GameConstants.PRICE_MIN_FACTOR);
            }
            state.setPrice(g, Math.max(1L, Math.round(g.basePrice() * factor)));
        }
    }

    /** Human-readable "the price of X is unusually cheap/dear" flavour, or null. */
    public String priceFlavour() {
        for (Good g : Good.values()) {
            double ratio = (double) state.getPrice(g) / g.basePrice();
            if (ratio <= GameConstants.PRICE_GLUT_FACTOR + 0.05) {
                return g.display() + " is going cheap here in " + state.getLocation().display() + "!";
            }
            if (ratio >= GameConstants.PRICE_SHORTAGE_FACTOR - 0.1) {
                return g.display() + " commands a fortune here in " + state.getLocation().display() + "!";
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ trading

    public String buy(Good g, int qty) {
        if (qty <= 0) {
            return "Enter a quantity of 1 or more.";
        }
        long price = state.getPrice(g);
        long cost = price * qty;
        if (cost > state.getCash()) {
            long affordable = state.getCash() / price;
            return "You can't afford that. You can buy at most " + affordable + " unit(s).";
        }
        if (qty > state.getShip().freeHold()) {
            return "Not enough hold space. You have room for " + state.getShip().freeHold() + " unit(s).";
        }
        state.setCash(state.getCash() - cost);
        state.getShip().addCargo(g, qty);
        return null;
    }

    public String sell(Good g, int qty) {
        if (qty <= 0) {
            return "Enter a quantity of 1 or more.";
        }
        if (qty > state.getShip().getCargo(g)) {
            return "You only have " + state.getShip().getCargo(g) + " unit(s) of " + g.display() + ".";
        }
        state.setCash(state.getCash() + state.getPrice(g) * qty);
        state.getShip().addCargo(g, -qty);
        return null;
    }

    // ------------------------------------------------------------- Hong Kong ops

    private boolean atHome() {
        return state.getLocation().isHome();
    }

    public String deposit(long amount) {
        if (!atHome()) {
            return "The bank is only in Hong Kong.";
        }
        if (amount <= 0) {
            return "Enter an amount of 1 or more.";
        }
        if (amount > state.getCash()) {
            return "You only have $" + state.getCash() + " in hand.";
        }
        state.setCash(state.getCash() - amount);
        state.setBank(state.getBank() + amount);
        return null;
    }

    public String withdraw(long amount) {
        if (!atHome()) {
            return "The bank is only in Hong Kong.";
        }
        if (amount <= 0) {
            return "Enter an amount of 1 or more.";
        }
        if (amount > state.getBank()) {
            return "You only have $" + state.getBank() + " in the bank.";
        }
        state.setBank(state.getBank() - amount);
        state.setCash(state.getCash() + amount);
        return null;
    }

    public String borrow(long amount) {
        if (!atHome()) {
            return "Elder Brother Wu lends only in Hong Kong.";
        }
        if (amount <= 0) {
            return "Enter an amount of 1 or more.";
        }
        state.setCash(state.getCash() + amount);
        state.setDebt(state.getDebt() + amount);
        return null;
    }

    public String repay(long amount) {
        if (!atHome()) {
            return "Elder Brother Wu collects only in Hong Kong.";
        }
        if (amount <= 0) {
            return "Enter an amount of 1 or more.";
        }
        if (amount > state.getCash()) {
            return "You only have $" + state.getCash() + " in hand.";
        }
        long applied = Math.min(amount, state.getDebt());
        state.setCash(state.getCash() - applied);
        state.setDebt(state.getDebt() - applied);
        return null;
    }

    // --------------------------------------------------------------- shipyard

    public long repairCost() {
        Ship s = state.getShip();
        return Math.round(s.getDamage() / 100.0 * s.getCapacity() * 60.0);
    }

    public String repairShip() {
        if (!atHome()) {
            return "McHenry the ship-fitter works only in Hong Kong.";
        }
        if (state.getShip().getDamage() == 0) {
            return "Your ship is in fine shape — no repairs needed.";
        }
        long cost = repairCost();
        if (cost > state.getCash()) {
            return "Repairs cost $" + cost + " but you only have $" + state.getCash() + ".";
        }
        state.setCash(state.getCash() - cost);
        state.getShip().repair(GameConstants.MAX_DAMAGE);
        return null;
    }

    public String upgradeCapacity() {
        if (!atHome()) {
            return "The shipyard is only in Hong Kong.";
        }
        if (GameConstants.CAPACITY_UPGRADE_PRICE > state.getCash()) {
            return "A larger hold costs $" + GameConstants.CAPACITY_UPGRADE_PRICE
                    + " but you only have $" + state.getCash() + ".";
        }
        state.setCash(state.getCash() - GameConstants.CAPACITY_UPGRADE_PRICE);
        state.getShip().addCapacity(GameConstants.CAPACITY_UPGRADE_AMOUNT);
        return null;
    }

    public String buyGun() {
        if (!atHome()) {
            return "Guns are fitted only in the Hong Kong shipyard.";
        }
        if (GameConstants.GUN_PRICE > state.getCash()) {
            return "A gun costs $" + GameConstants.GUN_PRICE
                    + " but you only have $" + state.getCash() + ".";
        }
        if (state.getShip().freeHold() < GameConstants.GUN_HOLD_SPACE) {
            return "A gun needs " + GameConstants.GUN_HOLD_SPACE
                    + " hold units; you only have " + state.getShip().freeHold() + " free.";
        }
        state.setCash(state.getCash() - GameConstants.GUN_PRICE);
        state.getShip().addGuns(1);
        return null;
    }

    // ---------------------------------------------------------------- voyaging

    /**
     * Begin a voyage: advance the calendar, apply interest, and roll random
     * events. Any interactive follow-up (Li Yuen tribute, combat) is attached to
     * the returned {@link VoyageResult}; the arrival itself is finalised later by
     * {@link #arrive(PortCity)} once those are resolved.
     */
    public VoyageResult beginVoyage(PortCity destination) {
        VoyageResult r = new VoyageResult(destination);

        state.advanceMonth();

        // Interest on Wu's debt, and a little interest paid on the bank balance.
        if (state.getDebt() > 0) {
            long before = state.getDebt();
            state.setDebt(Math.round(state.getDebt() * (1 + GameConstants.DEBT_INTEREST)));
            r.log.add("Elder Brother Wu adds interest: your debt grows from $"
                    + before + " to $" + state.getDebt() + ".");
        }
        if (state.getBank() > 0) {
            state.setBank(Math.round(state.getBank() * (1 + GameConstants.BANK_INTEREST)));
        }

        // Opium seizure by the authorities.
        if (state.getShip().getCargo(Good.OPIUM) > 0
                && rng.nextDouble() < GameConstants.OPIUM_SEIZE_CHANCE) {
            int seized = state.getShip().getCargo(Good.OPIUM);
            long fine = Math.min(state.getCash(), (long) seized * Good.OPIUM.basePrice() / 2);
            state.getShip().addCargo(Good.OPIUM, -seized);
            state.setCash(state.getCash() - fine);
            r.log.add("The authorities seize your " + seized
                    + " unit(s) of opium and fine you $" + fine + "!");
        }

        // Li Yuen: either extortion, or friendly protection if already paid.
        if (rng.nextDouble() < GameConstants.LI_YUEN_CHANCE) {
            if (state.isLiYuenFriendly()) {
                r.log.add("Li Yuen's fleet waves you through — his protection holds.");
            } else {
                long demand = Math.max(50, (state.getCash() + state.getBank()) / 6
                        + rng.nextInt(200));
                r.liYuenDemand = demand;
            }
        }

        // Pirates.
        if (rng.nextDouble() < GameConstants.PIRATE_CHANCE) {
            int ships = enemyCount();
            if (ships > 0) {
                r.combat = new CombatSession(state, ships);
                r.log.add("Pirates! " + ships + " hostile ship(s) bear down on you.");
            }
        }

        // Storm.
        if (rng.nextDouble() < GameConstants.STORM_CHANCE) {
            int dmg = GameConstants.STORM_MIN_DAMAGE
                    + rng.nextInt(GameConstants.STORM_MAX_DAMAGE - GameConstants.STORM_MIN_DAMAGE + 1);
            if (state.getShip().getDamage() > 70 && rng.nextDouble() < GameConstants.STORM_SINK_CHANCE) {
                state.getShip().addDamage(GameConstants.MAX_DAMAGE);
                state.setGameOver(true);
                r.gameOver = true;
                r.log.add("A typhoon overwhelms your crippled ship and sends it to the bottom!");
                return r;
            }
            state.getShip().addDamage(dmg);
            r.log.add("A storm batters your ship for " + dmg + "% damage (now "
                    + state.getShip().getDamage() + "%).");
            if (rng.nextDouble() < GameConstants.STORM_BLOWN_OFF_COURSE) {
                PortCity diverted = randomOtherPort(destination);
                r.actualArrival = diverted;
                r.log.add("The wind blows you off course — you make landfall at "
                        + diverted.display() + " instead.");
            }
        }

        return r;
    }

    /** Finalise arrival at a port: set location, reset Li Yuen protection, roll prices. */
    public void arrive(PortCity port) {
        state.setLocation(port);
        state.setLiYuenFriendly(false); // protection lasts a single voyage
        generatePrices();
    }

    public String payLiYuen(long amount) {
        if (amount <= 0) {
            return "Enter an amount of 1 or more.";
        }
        if (amount > state.getCash()) {
            return "You only have $" + state.getCash() + " in hand.";
        }
        state.setCash(state.getCash() - amount);
        state.setLiYuenFriendly(true);
        return null;
    }

    private int enemyCount() {
        long worth = Math.max(0, state.getCash() + state.getBank());
        int scaled = (int) Math.min(GameConstants.MAX_ENEMY_SHIPS, 1 + worth / 20000L);
        return 1 + rng.nextInt(Math.max(1, scaled));
    }

    private PortCity randomOtherPort(PortCity not) {
        PortCity[] all = PortCity.values();
        PortCity choice;
        do {
            choice = all[rng.nextInt(all.length)];
        } while (choice == not);
        return choice;
    }

    // ------------------------------------------------------------------ scoring

    public void retire() {
        state.setRetired(true);
        state.setGameOver(true);
    }

    /** Final net worth used for scoring (bank + cash + cargo - debt). */
    public long finalScore() {
        return state.netWorth();
    }

    /** A ranking title, roughly mirroring the original's end report. */
    public String rank() {
        long w = state.netWorth();
        if (state.getShip().isSunk() && !state.isRetired()) {
            return "Lost at Sea";
        }
        if (w < 0) {
            return "Bankrupt";
        }
        if (w < 10000) {
            return "Penniless Peddler";
        }
        if (w < 100000) {
            return "Compradore";
        }
        if (w < 500000) {
            return "Merchant";
        }
        if (w < GameConstants.RETIRE_TARGET) {
            return "Master Merchant";
        }
        return "Ma Tsu (Taipan!)";
    }
}
