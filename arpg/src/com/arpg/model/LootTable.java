package com.arpg.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A weighted table of possible drops. The engine performs the actual weighted
 * roll; this class only stores the entries and exposes their combined weight so
 * a roll can be implemented as {@code pick(rng.nextInt(getTotalWeight()))}.
 */
public final class LootTable implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** One row of a {@link LootTable}: an item template and its relative weight. */
    public static final class Entry implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final Item item;
        private final int weight;

        public Entry(Item item, int weight) {
            if (item == null) {
                throw new IllegalArgumentException("LootTable.Entry item must not be null");
            }
            this.item = item;
            this.weight = Math.max(1, weight);
        }

        public Item getItem() {
            return item;
        }

        public int getWeight() {
            return weight;
        }
    }

    private final String id;
    private final List<Entry> entries;
    private final int goldMin;
    private final int goldMax;

    public LootTable(String id, int goldMin, int goldMax) {
        this.id = id;
        this.entries = new ArrayList<Entry>();
        this.goldMin = Math.max(0, goldMin);
        this.goldMax = Math.max(this.goldMin, goldMax);
    }

    public String getId() {
        return id;
    }

    public LootTable addEntry(Item item, int weight) {
        entries.add(new Entry(item, weight));
        return this;
    }

    public List<Entry> getEntries() {
        return new ArrayList<Entry>(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int getTotalWeight() {
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            total += entries.get(i).getWeight();
        }
        return total;
    }

    public int getGoldMin() {
        return goldMin;
    }

    public int getGoldMax() {
        return goldMax;
    }

    /**
     * Resolve a weighted pick given a roll in {@code [0, getTotalWeight())}.
     * Pure lookup — the caller supplies the random number so the model stays
     * deterministic and logic-free. Returns {@code null} only if the table is empty.
     */
    public Item pick(int roll) {
        if (entries.isEmpty()) {
            return null;
        }
        int cursor = roll;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            if (cursor < e.getWeight()) {
                return e.getItem();
            }
            cursor -= e.getWeight();
        }
        return entries.get(entries.size() - 1).getItem();
    }
}
