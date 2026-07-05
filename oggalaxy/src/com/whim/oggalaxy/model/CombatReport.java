package com.whim.oggalaxy.model;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Views;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** A resolved deterministic combat report. Implements {@link Views.CombatReportView}. */
public final class CombatReport implements Views.CombatReportView, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    public int tick;
    public String attackerName;
    public String defenderName;
    public int[] location = new int[3];
    public List<String> roundSummaries = new ArrayList<String>();
    public Map<Ids.ShipType, Integer> attackerLosses = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
    public Map<Ids.ShipType, Integer> defenderShipLosses = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
    public Map<Ids.DefenseType, Integer> defenderDefenseLosses = new EnumMap<Ids.DefenseType, Integer>(Ids.DefenseType.class);
    public Cost debris = Cost.ZERO;
    public Cost plunder = Cost.ZERO;
    public boolean moonCreated;
    public String outcome = "";

    public CombatReport() {
    }

    @Override public String id() { return id; }
    @Override public int tick() { return tick; }
    @Override public String attackerName() { return attackerName; }
    @Override public String defenderName() { return defenderName; }
    @Override public int[] location() { return location; }
    @Override public List<String> roundSummaries() { return new ArrayList<String>(roundSummaries); }
    @Override public Map<Ids.ShipType, Integer> attackerLosses() {
        return new EnumMap<Ids.ShipType, Integer>(attackerLosses);
    }
    @Override public Map<Ids.ShipType, Integer> defenderShipLosses() {
        return new EnumMap<Ids.ShipType, Integer>(defenderShipLosses);
    }
    @Override public Map<Ids.DefenseType, Integer> defenderDefenseLosses() {
        return new EnumMap<Ids.DefenseType, Integer>(defenderDefenseLosses);
    }
    @Override public Cost debris() { return debris; }
    @Override public Cost plunder() { return plunder; }
    @Override public boolean moonCreated() { return moonCreated; }
    @Override public String outcome() { return outcome; }

    @Override public String fullText() {
        StringBuilder sb = new StringBuilder();
        sb.append(attackerName).append(" attacks ").append(defenderName)
                .append(" at ").append(location[0]).append(":").append(location[1]).append(":").append(location[2]).append("\n");
        for (String r : roundSummaries) sb.append(r).append("\n");
        sb.append("Outcome: ").append(outcome).append("\n");
        if (debris.structurePoints() > 0) sb.append("Debris: ").append(debris).append("\n");
        if (plunder.structurePoints() > 0) sb.append("Plunder: ").append(plunder).append("\n");
        if (moonCreated) sb.append("A moon formed from the debris!\n");
        return sb.toString();
    }
}
