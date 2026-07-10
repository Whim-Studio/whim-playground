package com.whim.scg.model;

import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable galaxy graph. */
public final class GalaxyModel implements Views.GalaxyView {
    public int width, height;
    public int currentSystem;
    public final List<StarSystemModel> systems = new ArrayList<StarSystemModel>();

    public StarSystemModel system(int id) {
        for (StarSystemModel s : systems) if (s.id == id) return s;
        return null;
    }

    public StarSystemModel current() { return system(currentSystem); }

    @Override public int width() { return width; }
    @Override public int height() { return height; }
    @Override public int currentSystem() { return currentSystem; }

    @Override public List<Views.StarSystemView> systems() {
        List<Views.StarSystemView> out = new ArrayList<Views.StarSystemView>(systems);
        return out;
    }
}
