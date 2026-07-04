package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * A thrown/fired shot. Extends {@link Entity} with who fired it, which
 * {@link Enums.Weapon} spawned it (drives speed/damage/visual), and a lifespan
 * countdown in ticks. Implements {@link Views.ProjectileView}.
 *
 * Movement + expiry are applied by the engine (Task 2); this holds the state.
 */
public class Projectile extends Entity implements Views.ProjectileView {
    private final boolean fromPlayer;
    private final Enums.Weapon weapon;
    /** Ticks remaining before the projectile despawns. */
    private int lifespan;

    public Projectile(Aabb box, Enums.Plane plane, boolean fromPlayer,
                      Enums.Weapon weapon, int lifespan) {
        super(box, plane);
        this.fromPlayer = fromPlayer;
        this.weapon = weapon;
        this.lifespan = lifespan;
        this.hp = 1;
    }

    // ---- Views.ProjectileView ----
    @Override public boolean fromPlayer() { return fromPlayer; }
    @Override public Enums.Weapon weapon() { return weapon; }

    // ---- Lifespan ----
    public int lifespan() { return lifespan; }
    public void setLifespan(int ticks) { this.lifespan = ticks; }
    /** Decrement lifespan; returns true once it has expired. */
    public boolean tickLifespan() {
        if (lifespan > 0) lifespan--;
        return lifespan <= 0;
    }
    public boolean expired() { return lifespan <= 0; }
}
