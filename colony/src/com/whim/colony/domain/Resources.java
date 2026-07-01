package com.whim.colony.domain;

/**
 * The colony's shared stockpile counts. All amounts are non-negative integers;
 * {@link #consume} helpers guard against dropping below zero.
 */
public final class Resources {
    private int food;
    private int steel;
    private int wood;

    public Resources() {
        this(0, 0, 0);
    }

    public Resources(int food, int steel, int wood) {
        this.food = Math.max(0, food);
        this.steel = Math.max(0, steel);
        this.wood = Math.max(0, wood);
    }

    public int getFood() {
        return food;
    }

    public int getSteel() {
        return steel;
    }

    public int getWood() {
        return wood;
    }

    public void addFood(int amount) {
        food = add(food, amount);
    }

    public void addSteel(int amount) {
        steel = add(steel, amount);
    }

    public void addWood(int amount) {
        wood = add(wood, amount);
    }

    /** @return true if at least {@code amount} food was available and consumed. */
    public boolean consumeFood(int amount) {
        if (amount <= 0 || food < amount) {
            return amount <= 0;
        }
        food -= amount;
        return true;
    }

    /** @return true if at least {@code amount} steel was available and consumed. */
    public boolean consumeSteel(int amount) {
        if (amount <= 0 || steel < amount) {
            return amount <= 0;
        }
        steel -= amount;
        return true;
    }

    /** @return true if at least {@code amount} wood was available and consumed. */
    public boolean consumeWood(int amount) {
        if (amount <= 0 || wood < amount) {
            return amount <= 0;
        }
        wood -= amount;
        return true;
    }

    /** Add a (possibly negative) amount, clamping the result at zero. */
    private static int add(int current, int amount) {
        long sum = (long) current + amount;
        if (sum < 0) {
            return 0;
        }
        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) sum;
    }
}
