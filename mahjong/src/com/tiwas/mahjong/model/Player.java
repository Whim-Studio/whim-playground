package com.tiwas.mahjong.model;

/**
 * A seat at the table. Holds the player's hand, current seat wind, running
 * score, and whether it is computer-controlled.
 */
public final class Player {

    private final String name;
    private final boolean ai;
    private final Hand hand = new Hand();
    private Wind seatWind;
    private int score;

    public Player(String name, boolean ai, Wind seatWind) {
        this.name = name;
        this.ai = ai;
        this.seatWind = seatWind;
        this.score = 0;
    }

    public String getName() {
        return name;
    }

    public boolean isAi() {
        return ai;
    }

    public boolean isHuman() {
        return !ai;
    }

    public Hand getHand() {
        return hand;
    }

    public Wind getSeatWind() {
        return seatWind;
    }

    public void setSeatWind(Wind seatWind) {
        this.seatWind = seatWind;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int delta) {
        this.score += delta;
    }

    @Override
    public String toString() {
        return name + " (" + seatWind.label() + ")";
    }
}
