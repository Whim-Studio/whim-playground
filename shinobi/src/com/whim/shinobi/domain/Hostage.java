// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** A tied hostage the player rescues by touching. */
public class Hostage implements Views.HostageView {
    public final Aabb box;
    public Enums.Plane plane;
    public boolean rescued = false;
    public Enums.RescueReward reward = Enums.RescueReward.POINTS;

    public Hostage(double x, double y, Enums.Plane plane, Enums.RescueReward reward) {
        this.box = new Aabb(x, y, 20, 40);
        this.plane = plane;
        this.reward = reward;
    }

    public Aabb box() { return box; }
    @Override public double x() { return box.x; }
    @Override public double y() { return box.y; }
    @Override public double w() { return box.w; }
    @Override public double h() { return box.h; }
    @Override public Enums.Plane plane() { return plane; }
    @Override public boolean rescued() { return rescued; }
}
