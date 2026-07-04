package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/**
 * A tied-up hostage to be freed. Holds its box, plane, rescued flag, and the
 * {@link Enums.RescueReward} granted on rescue. Implements {@link Views.HostageView}.
 *
 * The engine (Task 2) detects the rescue and applies the reward; this class only
 * records whether it has been freed.
 */
public class Hostage implements Views.HostageView {
    private final Aabb box;
    private final Enums.Plane plane;
    private final Enums.RescueReward reward;
    private boolean rescued = false;

    public Hostage(Aabb box, Enums.Plane plane, Enums.RescueReward reward) {
        this.box = box;
        this.plane = plane;
        this.reward = reward;
    }

    // ---- Views.BoxView / HostageView ----
    @Override public double x() { return box.x(); }
    @Override public double y() { return box.y(); }
    @Override public double w() { return box.w(); }
    @Override public double h() { return box.h(); }
    @Override public Enums.Plane plane() { return plane; }
    @Override public boolean rescued() { return rescued; }

    // ---- Domain access ----
    public Aabb box() { return box; }
    public Enums.RescueReward reward() { return reward; }
    public void setRescued(boolean rescued) { this.rescued = rescued; }
}
