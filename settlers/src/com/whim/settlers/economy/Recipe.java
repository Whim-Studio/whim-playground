package com.whim.settlers.economy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * What a building consumes and produces. A recipe with no {@link #inputs} and an
 * {@link ExtractorNeed} other than {@code NONE} is a harvester (woodcutter,
 * mine, …); one with inputs is a processor (sawmill, bakery, …).
 *
 * <p>{@link #requiredTool} gates <em>staffing</em>: a building only gets a worker
 * once that tool exists in the stockpile (the deliberate bottleneck from the
 * design). {@link #consumesFood} marks the mines, which eat one food per cycle.
 * A null {@link #output} marks a special producer resolved in code (the forester
 * replants trees; the tool maker picks a tool by priority; the blacksmith
 * alternates sword/shield).
 */
public final class Recipe {

    /** Terrain a harvester needs near/under it to produce. */
    public enum ExtractorNeed { NONE, FOREST, STONE_ROCK, WATER, FARMLAND, MOUNTAIN }

    private final Map<Good, Integer> inputs;
    private final Good output;
    private final int outputQty;
    private final float seconds;
    private final Good requiredTool;   // nullable
    private final boolean consumesFood;
    private final ExtractorNeed extractorNeed;
    private final Profession profession;

    Recipe(Map<Good, Integer> inputs, Good output, int outputQty, float seconds,
           Good requiredTool, boolean consumesFood, ExtractorNeed extractorNeed,
           Profession profession) {
        this.inputs = inputs == null ? Collections.<Good, Integer>emptyMap()
                                     : new EnumMap<Good, Integer>(inputs);
        this.output = output;
        this.outputQty = outputQty;
        this.seconds = seconds;
        this.requiredTool = requiredTool;
        this.consumesFood = consumesFood;
        this.extractorNeed = extractorNeed;
        this.profession = profession;
    }

    public Map<Good, Integer> inputs() { return inputs; }
    public Good output()               { return output; }
    public int outputQty()             { return outputQty; }
    public float seconds()             { return seconds; }
    public Good requiredTool()         { return requiredTool; }
    public boolean consumesFood()      { return consumesFood; }
    public ExtractorNeed extractorNeed(){ return extractorNeed; }
    public Profession profession()     { return profession; }
    public boolean isExtractor()       { return extractorNeed != ExtractorNeed.NONE; }
}
