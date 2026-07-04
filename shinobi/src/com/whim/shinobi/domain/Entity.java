// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** Base mutable game entity. Physics + AI mutate the public fields directly. */
public abstract class Entity implements Views.EntityView {
    public final Aabb box = new Aabb(0, 0, Config.ENTITY_W, Config.ENTITY_H);
    public double vx, vy;
    public Enums.Plane plane = Enums.Plane.LOWER;
    public Enums.Facing facing = Enums.Facing.RIGHT;
    public Enums.EntityState state = Enums.EntityState.IDLE;
    public boolean alive = true;
    public boolean grounded = false;

    @Override public double x() { return box.x; }
    @Override public double y() { return box.y; }
    @Override public double w() { return box.w; }
    @Override public double h() { return box.h; }
    @Override public Enums.Plane plane() { return plane; }
    @Override public Enums.Facing facing() { return facing; }
    @Override public Enums.EntityState state() { return state; }
    @Override public boolean alive() { return alive; }
}
