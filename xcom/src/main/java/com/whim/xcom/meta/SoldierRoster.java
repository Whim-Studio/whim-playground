package com.whim.xcom.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * The base's persistent squad. Provides the deployable soldiers for an assault
 * and applies casualties/experience when a mission returns.
 */
public final class SoldierRoster {

    private final List<Soldier> soldiers = new ArrayList<Soldier>();

    public List<Soldier> soldiers() {
        return soldiers;
    }

    public void add(Soldier s) {
        soldiers.add(s);
    }

    public int size() {
        return soldiers.size();
    }

    public Soldier byName(String name) {
        for (Soldier s : soldiers) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    /** Up to {@code max} deployable (un-wounded) soldiers. */
    public List<Soldier> deployable(int max) {
        List<Soldier> out = new ArrayList<Soldier>();
        for (Soldier s : soldiers) {
            if (s.deployable()) {
                out.add(s);
                if (out.size() >= max) {
                    break;
                }
            }
        }
        return out;
    }

    /** Advance the infirmary by one day for every wounded soldier. */
    public void restDay() {
        for (Soldier s : soldiers) {
            s.restDay();
        }
    }

    /** Kill the named soldiers (mission KIA), removing them from the roster. */
    public void removeByName(String name) {
        soldiers.removeIf(s -> s.name().equals(name));
    }
}
