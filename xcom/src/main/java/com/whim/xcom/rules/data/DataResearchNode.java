package com.whim.xcom.rules.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.xcom.rules.def.ResearchNode;

/** Immutable {@link ResearchNode} backed by data-pack fields. */
public final class DataResearchNode implements ResearchNode {

    private String id;
    private String name;
    private int researchCost;
    private List<String> prerequisites = new ArrayList<String>();
    private List<String> unlocks = new ArrayList<String>();
    private List<String> requiredItems = new ArrayList<String>();
    private boolean needsCaptiveOrItem;

    public DataResearchNode(String id, String name, int researchCost,
                            List<String> prerequisites, List<String> unlocks,
                            boolean needsCaptiveOrItem) {
        this.id = id;
        this.name = name;
        this.researchCost = researchCost;
        if (prerequisites != null) this.prerequisites = prerequisites;
        if (unlocks != null) this.unlocks = unlocks;
        this.needsCaptiveOrItem = needsCaptiveOrItem;
    }

    DataResearchNode() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int researchCost() { return researchCost; }
    @Override public boolean needsCaptiveOrItem() { return needsCaptiveOrItem; }

    @Override
    public List<String> prerequisites() {
        return Collections.unmodifiableList(prerequisites == null ? new ArrayList<String>() : prerequisites);
    }

    @Override
    public List<String> unlocks() {
        return Collections.unmodifiableList(unlocks == null ? new ArrayList<String>() : unlocks);
    }

    @Override
    public List<String> requiredItems() {
        return Collections.unmodifiableList(requiredItems == null ? new ArrayList<String>() : requiredItems);
    }
}
