package com.railroad.model;

/**
 * An immutable record of one cargo delivery: what was delivered, how far it was
 * carried, the revenue it earned and the in-game day it happened. {@link GameState}
 * keeps a running list of these so Phase 3's finance screens can build an income
 * statement without the model needing to know about that phase yet.
 */
public final class DeliveryRecord {

    private final CargoType cargo;
    private final int carloads;
    private final int distanceTiles;
    private final long revenue;
    private final double gameDay;

    public DeliveryRecord(CargoType cargo, int carloads, int distanceTiles, long revenue, double gameDay) {
        this.cargo = cargo;
        this.carloads = carloads;
        this.distanceTiles = distanceTiles;
        this.revenue = revenue;
        this.gameDay = gameDay;
    }

    public CargoType getCargo() {
        return cargo;
    }

    public int getCarloads() {
        return carloads;
    }

    public int getDistanceTiles() {
        return distanceTiles;
    }

    public long getRevenue() {
        return revenue;
    }

    public double getGameDay() {
        return gameDay;
    }
}
