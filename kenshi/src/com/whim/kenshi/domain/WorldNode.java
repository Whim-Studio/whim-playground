package com.whim.kenshi.domain;

import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.NodeType;

/**
 * An interactable location on the map (town, bar, shop, camp, ruin). Static
 * position + radius; the UI draws it and the player can right-click to
 * INTERACT within {@link #radius()}.
 */
public final class WorldNode {

    private final String id;
    private final String name;
    private final NodeType type;
    private final FactionId owner;
    private final double x;
    private final double y;
    private final double radius;

    public WorldNode(String id, String name, NodeType type, FactionId owner,
                     double x, double y, double radius) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public String id() { return id; }
    public String name() { return name; }
    public NodeType type() { return type; }
    public FactionId owner() { return owner; }
    public double x() { return x; }
    public double y() { return y; }
    public double radius() { return radius; }

    /** True if world point (px,py) lies within this node's radius. */
    public boolean contains(double px, double py) {
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}
