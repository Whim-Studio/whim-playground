package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
public class AiStudio {
    private final String id, name;
    private final double strength;
    private long cash;
    private int releasedGames;
    private Integer lastReviewScore;
    public AiStudio(String id, String name, double strength) {
        this.id = id; this.name = name; this.strength = strength;
    }
    public String id() { return id; }
    public String name() { return name; }
    public double strength() { return strength; }
    public long cash() { return cash; }
    public void addCash(long delta) { cash += delta; }
    public int releasedGames() { return releasedGames; }
    public void incrementReleasedGames() { releasedGames++; }
    public Integer lastReviewScore() { return lastReviewScore; }
    public void setLastReviewScore(int score) { lastReviewScore = score; }
}
