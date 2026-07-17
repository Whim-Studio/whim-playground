package com.whim.xcom.geo;

import java.util.ArrayList;
import java.util.List;

import com.whim.xcom.rules.def.FacilityDef;

/**
 * A player base at a fixed world position with a set of built facilities. The
 * base's radar reach and per-tick detection chance are derived from its radar
 * facilities, so upgrading radar (data-driven) improves UFO detection with no
 * engine change.
 */
public final class Base {

    private final String name;
    private final double x;
    private final double y;
    private final List<FacilityDef> facilities = new ArrayList<FacilityDef>();

    public Base(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String name() { return name; }
    public double x() { return x; }
    public double y() { return y; }
    public List<FacilityDef> facilities() { return facilities; }

    public void addFacility(FacilityDef f) {
        if (f != null) {
            facilities.add(f);
        }
    }

    /** Best radar range on the base, normalised to world units (0..1). */
    public double radarRangeNorm() {
        int best = 0;
        for (FacilityDef f : facilities) {
            best = Math.max(best, f.detectionRange());
        }
        return best / 2000.0;
    }

    /** Best per-30-min detection chance percent across the base's radars. */
    public int detectionChancePercent() {
        int best = 0;
        for (FacilityDef f : facilities) {
            best = Math.max(best, f.detectionChancePercent());
        }
        return best;
    }

    /** True if a facility with this id is built at the base. */
    public boolean hasFacility(String id) {
        for (FacilityDef f : facilities) {
            if (f.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Total live-alien holding capacity from Alien Containment facilities. */
    public int containmentCapacity() {
        int total = 0;
        for (FacilityDef f : facilities) {
            if ("alien_containment".equals(f.id())) {
                total += f.capacity();
            }
        }
        return total;
    }

    public int monthlyMaintenance() {
        int total = 0;
        for (FacilityDef f : facilities) {
            total += f.monthlyMaintenanceDollars();
        }
        return total;
    }
}
