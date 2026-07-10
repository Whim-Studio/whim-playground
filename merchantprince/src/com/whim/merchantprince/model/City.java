package com.whim.merchantprince.model;

import java.io.Serializable;

/**
 * A tradeable location on the Afro-Eurasian map (GAME_DESIGN_REFERENCE §2). Cities
 * hold a local stock level per good and a slowly-drifting local base price per good;
 * the live buy/sell price shown to the player is derived from these by the pricing
 * engine (supply/demand). Many cities begin {@link #open}=false and must be bribed
 * or forced open before foreign traders may deal there.
 *
 * <p>Stock and basePrice are flat arrays indexed by {@link Good#ordinal()}.
 * {@code x,y} are abstract map coordinates used for straight-line travel distance.
 */
public class City implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int id;
    public final String name;
    public final String region;
    public final int x;
    public final int y;
    /** True if this city sits on the coast and can be reached by ships. */
    public final boolean coastal;
    /** True once foreign traders may deal here (Venice starts open). */
    public boolean open;

    /** Local inventory per good (units available to buy). */
    public final int[] stock = new int[Good.COUNT];
    /** Local base price per good in florins (drifts over time toward nominal). */
    public final double[] basePrice = new double[Good.COUNT];
    /** Whether the city produces (sells cheap) or demands (buys dear) each good. */
    public final boolean[] produces = new boolean[Good.COUNT];

    public City(int id, String name, String region, int x, int y, boolean coastal, boolean open) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.x = x;
        this.y = y;
        this.coastal = coastal;
        this.open = open;
    }

    /** Euclidean map distance to another city — the basis for travel time. */
    public double distanceTo(City o) {
        double dx = x - o.x, dy = y - o.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
