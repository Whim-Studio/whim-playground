package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;
import com.whim.powermonger.api.Views;

/**
 * A warlord leading an {@link ArmyBloc}. This is the selectable, orderable unit.
 * Implements {@link Views.CaptainView}; position is in fractional tile units.
 */
public final class Captain implements Views.CaptainView {

    private final int id;
    private final String name;
    private double x;
    private double y;
    private final Allegiance allegiance;
    private final ArmyBloc bloc;
    private boolean selected;
    private boolean alive = true;
    private final boolean supremeCommander;
    private final CommandQueue commandQueue = new CommandQueue();

    public Captain(int id, String name, double x, double y,
                   Allegiance allegiance, ArmyBloc bloc, boolean supremeCommander) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.allegiance = allegiance;
        this.bloc = bloc;
        this.supremeCommander = supremeCommander;
    }

    public ArmyBloc bloc() { return bloc; }
    public CommandQueue commandQueue() { return commandQueue; }

    // ---- Views.CaptainView ----
    @Override public int id() { return id; }
    @Override public String name() { return name; }
    @Override public double x() { return x; }
    @Override public double y() { return y; }
    @Override public Allegiance allegiance() { return allegiance; }
    @Override public Posture posture() { return bloc.posture(); }
    @Override public int strength() { return bloc.strength(); }
    @Override public int food() { return bloc.food(); }
    @Override public CommandType currentOrder() { return bloc.currentOrder(); }
    @Override public boolean hasDestination() { return bloc.hasDestination(); }
    @Override public double destX() { return bloc.destX(); }
    @Override public double destY() { return bloc.destY(); }
    @Override public boolean selected() { return selected; }
    @Override public boolean alive() { return alive; }
    @Override public boolean supremeCommander() { return supremeCommander; }

    // ---- mutators (engine / controller) ----
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
