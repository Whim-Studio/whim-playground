package com.whim.tippingpoint.domain;

public final class Player {
    private final String name;
    private final boolean ai;
    private int cash;
    private final StatusBoard board;
    private final CityTableau city;

    public Player(String name, boolean ai) {
        this.name = name;
        this.ai = ai;
        this.cash = Rules.START_CASH;
        this.board = new StatusBoard();
        this.city = new CityTableau();
    }

    public String getName() { return name; }
    public boolean isAi() { return ai; }

    public int getCash() { return cash; }
    public void addCash(int d) { cash += d; }
    public void spendCash(int d) { cash -= d; }        // caller must ensure affordable
    public boolean canAfford(int cost) { return cash >= cost; }

    public StatusBoard getBoard() { return board; }
    public CityTableau getCity() { return city; }
    public int getPopulation() { return city.populationCount(); }
}
