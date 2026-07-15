package com.whim.ruinlander.engine;

import com.whim.ruinlander.domain.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/** A crafting/cooking recipe: a set of input item ids + quantities producing one output stack. */
public final class Recipe {

    private final String id;
    private final String name;
    private final Map<String, Integer> inputs;
    private final ItemStack output;

    public Recipe(String id, String name, Map<String, Integer> inputs, ItemStack output) {
        this.id = id;
        this.name = name;
        this.inputs = new LinkedHashMap<String, Integer>(inputs);
        this.output = output;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, Integer> getInputs() {
        return inputs;
    }

    public ItemStack getOutput() {
        return output;
    }

    /** Human-readable "2x Cloth + 1x Scrap → Bandage" style summary. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : inputs.entrySet()) {
            if (!first) {
                sb.append(" + ");
            }
            Object it = ItemDb.get(e.getKey());
            String label = (it == null) ? e.getKey() : ItemDb.get(e.getKey()).getName();
            sb.append(e.getValue()).append("x ").append(label);
            first = false;
        }
        sb.append(" → ").append(output.getQuantity()).append("x ").append(output.getItem().getName());
        return sb.toString();
    }
}
