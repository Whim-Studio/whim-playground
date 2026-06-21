package com.tycoon.core;

/**
 * A simulated competitor studio (≈100 of them).
 */
public class AiStudio {
    private final String id;
    private final String name;
    private final double strength;
    private long cash = 0L;
    private int releasedGames = 0;
    private Integer lastReviewScore = null;

    public AiStudio(String id, String name, double strength) {
        this.id = id;
        this.name = name;
        this.strength = strength;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** 0..1 quality bias. */
    public double strength() {
        return strength;
    }

    public long cash() {
        return cash;
    }

    public void addCash(long delta) {
        this.cash += delta;
    }

    public int releasedGames() {
        return releasedGames;
    }

    public void incrementReleasedGames() {
        this.releasedGames++;
    }

    public Integer lastReviewScore() {
        return lastReviewScore;
    }

    public void setLastReviewScore(int score) {
        this.lastReviewScore = score;
    }
}
