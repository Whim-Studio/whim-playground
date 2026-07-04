// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** A thrown weapon or enemy shot. */
public class Projectile extends Entity implements Views.ProjectileView {
    public boolean fromPlayer;
    public Enums.Weapon weapon = Enums.Weapon.SHURIKEN;
    public int damage = 1;
    /** Ticks remaining before the projectile despawns. */
    public int life = 240;

    public Projectile() {
        box.w = 12; box.h = 8;
    }

    @Override public boolean fromPlayer() { return fromPlayer; }
    @Override public Enums.Weapon weapon() { return weapon; }
}
