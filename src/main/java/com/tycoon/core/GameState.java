package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class GameState {
    private final GameStudio player;
    private final List<AiStudio> competitors;
    private final Random rng;
    private long hour;
    private boolean gameOver;
    public GameState(GameStudio player, List<AiStudio> competitors, long seed) {
        this.player = player; this.competitors = competitors; this.rng = new Random(seed);
    }
    public long hour() { return hour; }
    public int day() { return (int) (hour / 24); }
    public int week() { return day() / 7; }
    public GameStudio player() { return player; }
    public List<AiStudio> competitors() { return competitors; }
    public Random rng() { return rng; }
    public boolean isGameOver() { return gameOver; }
    public void advanceHourCounter() { hour++; }
    public static GameState newGame(long seed) {
        GameStudio player = new GameStudio("Player Studio");
        List<AiStudio> ais = new ArrayList<AiStudio>();
        Random r = new Random(seed);
        for (int i = 0; i < 100; i++) {
            ais.add(new AiStudio("ai" + i, "Studio " + i, r.nextDouble()));
        }
        return new GameState(player, ais, seed);
    }
}
