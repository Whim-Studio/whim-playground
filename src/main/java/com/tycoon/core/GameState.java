package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The root aggregate: the player studio, the AI competitors, the absolute hour
 * counter, and the single seeded RNG that the sim must use for all randomness.
 */
public class GameState {
    private final GameStudio player;
    private final List<AiStudio> competitors;
    private final Random rng;
    private long hour = 0L;
    private boolean gameOver = false;

    public GameState(GameStudio player, List<AiStudio> competitors, long seed) {
        this.player = player;
        this.competitors = competitors;
        this.rng = new Random(seed);
    }

    public long hour() {
        return hour;
    }

    public int day() {
        return (int) (hour / 24);
    }

    public int week() {
        return day() / 7;
    }

    public GameStudio player() {
        return player;
    }

    public List<AiStudio> competitors() {
        return competitors;
    }

    /** Single seeded source — sim MUST use this, not new Random(). */
    public Random rng() {
        return rng;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /** ++hour; called by the loop, not by sim. */
    public void advanceHourCounter() {
        hour++;
    }

    private static final String[] FIRST_NAMES = {
        "Alex", "Sam", "Jordan", "Taylor", "Casey", "Morgan", "Riley", "Jamie"
    };
    private static final String[] STUDIO_WORDS = {
        "Pixel", "Quantum", "Neon", "Vortex", "Hyper", "Crimson", "Lunar", "Apex",
        "Iron", "Echo", "Nova", "Cobalt", "Phantom", "Turbo", "Cosmic", "Razor"
    };
    private static final String[] STUDIO_SUFFIX = {
        "Studios", "Games", "Interactive", "Soft", "Works", "Entertainment", "Labs"
    };

    /**
     * Build a fresh game: a player studio with a default 40x30 floor plan, a
     * handful of starting employees, and 100 AI competitors with varied strengths.
     */
    public static GameState newGame(long seed) {
        Random seedRng = new Random(seed);

        GameStudio player = new GameStudio("Player Studio");
        player.addCash(250000L);

        // A handful of starting employees with varied base skill.
        int startingEmployees = 4;
        for (int i = 0; i < startingEmployees; i++) {
            String id = "emp-" + i;
            String name = FIRST_NAMES[i % FIRST_NAMES.length] + " #" + i;
            int baseSkill = 25 + seedRng.nextInt(40); // 25..64
            player.employees().add(new Employee(id, name, baseSkill));
        }

        // 100 AI competitors with varied strengths.
        List<AiStudio> competitors = new ArrayList<AiStudio>();
        for (int i = 0; i < 100; i++) {
            String id = "ai-" + i;
            String word = STUDIO_WORDS[seedRng.nextInt(STUDIO_WORDS.length)];
            String suffix = STUDIO_SUFFIX[seedRng.nextInt(STUDIO_SUFFIX.length)];
            String name = word + " " + suffix + " " + (i + 1);
            double strength = seedRng.nextDouble(); // 0..1
            AiStudio ai = new AiStudio(id, name, strength);
            ai.addCash(50000L + (long) (strength * 200000L));
            competitors.add(ai);
        }

        return new GameState(player, competitors, seed);
    }
}
