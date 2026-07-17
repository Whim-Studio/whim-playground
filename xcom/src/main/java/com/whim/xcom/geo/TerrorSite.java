package com.whim.xcom.geo;

/**
 * An alien <b>terror mission</b> on the Geoscape: a landed force attacking a city.
 * Unlike a UFO it does not fly and is always visible once it appears. It must be
 * responded to — if left until it {@linkplain Status#EXPIRED expires} the aliens
 * complete their mission and X-COM takes a heavy score/funding penalty
 * ({@link GeoGame#TERROR_IGNORE_PENALTY}). It can instead be assaulted as a
 * Battlescape via {@link GeoGame#buildTerrorAssault}. Position is normalised
 * {@code (x,y)} on the equirectangular world, matching {@link Ufo}.
 */
public final class TerrorSite {

    public enum Status { ACTIVE, ASSAULTED, EXPIRED }

    private final String id;
    private final String cityName;
    private final double x;
    private final double y;
    private final long appearedAtSeconds;
    private final long expiresAtSeconds;
    private Status status = Status.ACTIVE;

    public TerrorSite(String id, String cityName, double x, double y,
                      long appearedAtSeconds, long expiresAtSeconds) {
        this.id = id;
        this.cityName = cityName;
        this.x = x;
        this.y = y;
        this.appearedAtSeconds = appearedAtSeconds;
        this.expiresAtSeconds = expiresAtSeconds;
    }

    public String id() { return id; }
    public String cityName() { return cityName; }
    public double x() { return x; }
    public double y() { return y; }
    public long appearedAtSeconds() { return appearedAtSeconds; }
    public long expiresAtSeconds() { return expiresAtSeconds; }
    public Status status() { return status; }
    public void setStatus(Status s) { this.status = s; }

    public boolean active() { return status == Status.ACTIVE; }

    /** Whole game-hours until the aliens finish (0 once due). */
    public long hoursRemaining(long nowSeconds) {
        return Math.max(0, (expiresAtSeconds - nowSeconds) / 3600L);
    }
}
