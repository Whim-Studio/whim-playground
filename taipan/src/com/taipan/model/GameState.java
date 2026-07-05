package com.taipan.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * The complete mutable game state (the Model). All UI screens read from and
 * mutate this object only through the {@link com.taipan.controller.GameController};
 * no business logic lives here beyond simple derived getters.
 */
public class GameState {

    private String taipanName;
    private String firmName;

    private PortCity location = PortCity.HONG_KONG;

    private long cash;
    private long bank;
    private long debt;

    private final Ship ship;

    private int month;
    private int year;

    /** Current asking prices at {@link #location}. */
    private final Map<Good, Long> prices = new EnumMap<Good, Long>(Good.class);

    /** True once Li Yuen has been paid off this voyage cycle (grants protection). */
    private boolean liYuenFriendly = false;

    private boolean retired = false;
    private boolean gameOver = false;

    private final Random rng;

    public GameState(String taipanName, String firmName, Random rng) {
        this.taipanName = taipanName;
        this.firmName = firmName;
        this.rng = rng;
        this.cash = GameConstants.START_CASH;
        this.bank = GameConstants.START_BANK;
        this.debt = GameConstants.START_DEBT;
        this.ship = new Ship(GameConstants.START_CAPACITY, GameConstants.START_GUNS);
        this.month = GameConstants.START_MONTH;
        this.year = GameConstants.START_YEAR;
        for (Good g : Good.values()) {
            prices.put(g, g.basePrice());
        }
    }

    public String getTaipanName() {
        return taipanName;
    }

    public String getFirmName() {
        return firmName;
    }

    public PortCity getLocation() {
        return location;
    }

    public void setLocation(PortCity location) {
        this.location = location;
    }

    public long getCash() {
        return cash;
    }

    public void setCash(long cash) {
        this.cash = cash;
    }

    public long getBank() {
        return bank;
    }

    public void setBank(long bank) {
        this.bank = bank;
    }

    public long getDebt() {
        return debt;
    }

    public void setDebt(long debt) {
        this.debt = Math.max(0, debt);
    }

    public Ship getShip() {
        return ship;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    /** Advance the calendar one month; interest is applied by the controller. */
    public void advanceMonth() {
        month++;
        if (month > 12) {
            month = 1;
            year++;
        }
    }

    /** Whole months elapsed since the start of the game (used for scoring). */
    public int monthsElapsed() {
        return (year - GameConstants.START_YEAR) * 12 + (month - GameConstants.START_MONTH);
    }

    public long getPrice(Good g) {
        return prices.get(g);
    }

    public void setPrice(Good g, long price) {
        prices.put(g, Math.max(1L, price));
    }

    public boolean isLiYuenFriendly() {
        return liYuenFriendly;
    }

    public void setLiYuenFriendly(boolean liYuenFriendly) {
        this.liYuenFriendly = liYuenFriendly;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public Random rng() {
        return rng;
    }

    /**
     * Net worth = cash + bank - debt + value of cargo currently held (valued at
     * each good's base/reference price).
     */
    public long netWorth() {
        long worth = cash + bank - debt;
        for (Good g : Good.values()) {
            worth += (long) ship.getCargo(g) * g.basePrice();
        }
        return worth;
    }
}
