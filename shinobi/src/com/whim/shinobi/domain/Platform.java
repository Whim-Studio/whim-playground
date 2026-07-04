// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** A solid terrain segment on one plane. */
public class Platform implements Views.PlatformView {
    public final Aabb box;
    public Enums.Plane plane;

    public Platform(double x, double y, double w, double h, Enums.Plane plane) {
        this.box = new Aabb(x, y, w, h);
        this.plane = plane;
    }

    public Aabb box() { return box; }
    @Override public double x() { return box.x; }
    @Override public double y() { return box.y; }
    @Override public double w() { return box.w; }
    @Override public double h() { return box.h; }
    @Override public Enums.Plane plane() { return plane; }
}
