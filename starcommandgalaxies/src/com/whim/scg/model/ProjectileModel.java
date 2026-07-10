package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Vec2;
import com.whim.scg.api.Views;

/** In-flight weapon projectile in continuous combat space (0..1 normalized). */
public final class ProjectileModel implements Views.ProjectileView {
    public Vec2 pos;
    public Vec2 vel;
    public Enums.WeaponType type;
    public boolean fromPlayer;
    public int targetRoomId;
    public int damage;
    public boolean piercesShields;
    public boolean alive = true;

    @Override public Vec2 pos() { return pos; }
    @Override public Enums.WeaponType type() { return type; }
    @Override public boolean fromPlayer() { return fromPlayer; }
}
