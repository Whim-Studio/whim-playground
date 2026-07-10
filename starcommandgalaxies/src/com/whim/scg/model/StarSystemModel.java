package com.whim.scg.model;

import com.whim.scg.api.Enums;
import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable node in the galaxy graph. */
public final class StarSystemModel implements Views.StarSystemView {
    public int id;
    public String name;
    public int x, y;
    public boolean visited;
    public boolean hasStarport;
    public Enums.EventType pendingEvent = Enums.EventType.NOTHING;
    public boolean isBoss;
    public boolean scanned;
    public final List<Integer> links = new ArrayList<Integer>();

    @Override public int id() { return id; }
    @Override public String name() { return name; }
    @Override public int x() { return x; }
    @Override public int y() { return y; }
    @Override public boolean visited() { return visited; }
    @Override public boolean hasStarport() { return hasStarport; }
    @Override public Enums.EventType pendingEvent() { return pendingEvent; }
    @Override public List<Integer> links() { return links; }
}
