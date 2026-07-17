package com.whim.xcom.rules.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.whim.xcom.rules.def.ManufactureNode;

/** Immutable {@link ManufactureNode} backed by data-pack fields. */
public final class DataManufactureNode implements ManufactureNode {

    private String id;
    private String name;
    private String requiredResearchId;
    private int engineerHours;
    private int costDollars;
    private int workspace;
    private Map<String, Integer> inputs = new HashMap<String, Integer>();
    private String outputItemId;
    private int outputQuantity = 1;

    public DataManufactureNode(String id, String name, String requiredResearchId,
                               int engineerHours, int costDollars, int workspace,
                               Map<String, Integer> inputs, String outputItemId, int outputQuantity) {
        this.id = id;
        this.name = name;
        this.requiredResearchId = requiredResearchId;
        this.engineerHours = engineerHours;
        this.costDollars = costDollars;
        this.workspace = workspace;
        if (inputs != null) this.inputs = inputs;
        this.outputItemId = outputItemId;
        this.outputQuantity = outputQuantity;
    }

    DataManufactureNode() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public String requiredResearchId() { return requiredResearchId; }
    @Override public int engineerHours() { return engineerHours; }
    @Override public int costDollars() { return costDollars; }
    @Override public int workspace() { return workspace; }
    @Override public String outputItemId() { return outputItemId; }
    @Override public int outputQuantity() { return outputQuantity; }

    @Override
    public Map<String, Integer> inputs() {
        return Collections.unmodifiableMap(inputs == null ? new HashMap<String, Integer>() : inputs);
    }
}
