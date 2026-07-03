package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Views;

/**
 * A carrier pigeon animating command lag: it flies from an origin to a target
 * carrying an {@link Order}, which applies only on arrival. Implements
 * {@link Views.PigeonView}. Coordinates are fractional tile units.
 */
public final class Pigeon implements Views.PigeonView {

    private final int targetCaptainId;
    private final double originX;
    private final double originY;
    private final double targetX;
    private final double targetY;
    private final Order order;
    private double progress; // 0..1

    public Pigeon(int targetCaptainId, double originX, double originY,
                  double targetX, double targetY, Order order) {
        this.targetCaptainId = targetCaptainId;
        this.originX = originX;
        this.originY = originY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.order = order;
    }

    public int targetCaptainId() { return targetCaptainId; }
    public Order carriedOrder() { return order; }
    public double originX() { return originX; }
    public double originY() { return originY; }
    public boolean arrived() { return progress >= 1.0; }

    public void setProgress(double p) {
        this.progress = p < 0 ? 0 : (p > 1 ? 1 : p);
    }
    public void advance(double delta) { setProgress(this.progress + delta); }

    // ---- Views.PigeonView ----
    @Override public double x() { return originX + (targetX - originX) * progress; }
    @Override public double y() { return originY + (targetY - originY) * progress; }
    @Override public double targetX() { return targetX; }
    @Override public double targetY() { return targetY; }
    @Override public CommandType order() { return order.type(); }
    @Override public double progress() { return progress; }
}
