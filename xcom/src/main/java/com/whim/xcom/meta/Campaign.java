package com.whim.xcom.meta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;

/**
 * The persistent meta state of a campaign that lives above the Geoscape: the
 * science and engineering effort, the tech tree progress, the manufacturing
 * queue, the item stores and the soldier roster. Time is fed in from the
 * Geoscape clock via {@link #advance(int)}, which returns human-readable events
 * (research completed, items built) for the log.
 */
public final class Campaign {

    private int scientists;
    private int engineers;

    private final List<ResearchProject> research = new ArrayList<ResearchProject>();
    private final Set<String> completedResearch = new LinkedHashSet<String>();
    private final List<ManufactureJob> manufacture = new ArrayList<ManufactureJob>();
    private final TreeMap<String, Integer> stores = new TreeMap<String, Integer>();
    private final SoldierRoster roster;

    private double dayAccumulator;

    public Campaign(int scientists, int engineers, SoldierRoster roster) {
        this.scientists = scientists;
        this.engineers = engineers;
        this.roster = roster;
    }

    public int scientists() { return scientists; }
    public int engineers() { return engineers; }
    public void setScientists(int n) { scientists = Math.max(0, n); }
    public void setEngineers(int n) { engineers = Math.max(0, n); }

    public List<ResearchProject> activeResearch() { return research; }
    public List<ManufactureJob> manufacturing() { return manufacture; }
    public Set<String> completedResearch() { return completedResearch; }
    public TreeMap<String, Integer> stores() { return stores; }
    public SoldierRoster roster() { return roster; }

    // ---- planning -----------------------------------------------------------

    /** Research not yet done, not already queued, whose prerequisites are all met. */
    public List<ResearchNode> availableResearch(Ruleset ruleset) {
        List<ResearchNode> out = new ArrayList<ResearchNode>();
        for (ResearchNode n : ruleset.researchTree()) {
            if (completedResearch.contains(n.id()) || isQueued(n.id())) {
                continue;
            }
            boolean ready = true;
            for (String pre : n.prerequisites()) {
                if (!completedResearch.contains(pre)) {
                    ready = false;
                    break;
                }
            }
            if (ready && hasRequiredItems(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /** True if every store item this project consumes (e.g. a live captive) is on hand. */
    public boolean hasRequiredItems(ResearchNode n) {
        for (String item : n.requiredItems()) {
            Integer have = stores.get(item);
            if (have == null || have <= 0) {
                return false;
            }
        }
        return true;
    }

    private boolean isQueued(String id) {
        for (ResearchProject p : research) {
            if (p.id().equals(id) && !p.complete()) {
                return true;
            }
        }
        return false;
    }

    public ResearchProject startResearch(ResearchNode node, int scientistsAssigned) {
        // Interrogations and item-gated projects consume their captive/artifact.
        for (String item : node.requiredItems()) {
            consumeFromStores(item, 1);
        }
        ResearchProject p = new ResearchProject(node, scientistsAssigned);
        research.add(p);
        return p;
    }

    /** Remove up to {@code qty} of an item from stores (clamped at zero, entry dropped at zero). */
    public void consumeFromStores(String itemId, int qty) {
        Integer cur = stores.get(itemId);
        if (cur == null) {
            return;
        }
        int left = cur - qty;
        if (left > 0) {
            stores.put(itemId, left);
        } else {
            stores.remove(itemId);
        }
    }

    public boolean researchUnlocksManufacture(ManufactureNode node) {
        return node.requiredResearchId() == null
                || completedResearch.contains(node.requiredResearchId());
    }

    public ManufactureJob startManufacture(ManufactureNode node, int engineersAssigned, int quantity) {
        ManufactureJob j = new ManufactureJob(node, engineersAssigned, quantity);
        manufacture.add(j);
        return j;
    }

    // ---- time ---------------------------------------------------------------

    /** Advance research, manufacturing and infirmary by {@code seconds} of game time. */
    public List<String> advance(int seconds) {
        List<String> events = new ArrayList<String>();
        double days = seconds / 86400.0;
        double hours = seconds / 3600.0;

        for (ResearchProject p : research) {
            if (p.advance(days)) {
                completedResearch.add(p.id());
                events.add("Research complete: " + p.name());
                for (String u : p.node().unlocks()) {
                    events.add("  unlocked: " + u);
                }
            }
        }

        Iterator<ManufactureJob> it = manufacture.iterator();
        while (it.hasNext()) {
            ManufactureJob j = it.next();
            int built = j.advance(hours);
            if (built > 0) {
                addToStores(j.node().outputItemId(), built * j.node().outputQuantity());
                events.add("Manufactured " + built + "x " + j.name());
            }
            if (j.done()) {
                it.remove();
            }
        }

        // Infirmary: heal on whole-day boundaries.
        dayAccumulator += days;
        while (dayAccumulator >= 1.0) {
            dayAccumulator -= 1.0;
            roster.restDay();
        }
        return events;
    }

    public void addToStores(String itemId, int qty) {
        Integer cur = stores.get(itemId);
        stores.put(itemId, (cur == null ? 0 : cur) + qty);
    }

    /** Basic-issue weapons available to every soldier without manufacturing. */
    private static final Set<String> BASIC_WEAPONS = new LinkedHashSet<String>(
            java.util.Arrays.asList("pistol", "rifle", "heavy_cannon", "auto_cannon", "stun_rod"));

    /**
     * Weapons a soldier may equip: the basic issue plus anything of that kind
     * sitting in stores (e.g. a manufactured laser rifle). Ids are resolved
     * against the live ruleset so unknown store items are ignored.
     */
    public List<String> equipableWeapons(Ruleset ruleset) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        for (String id : BASIC_WEAPONS) {
            if (ruleset.hasWeapon(id)) {
                ids.add(id);
            }
        }
        for (String id : stores.keySet()) {
            if (ruleset.hasWeapon(id)) {
                ids.add(id);
            }
        }
        return new ArrayList<String>(ids);
    }

    /** Armours a soldier may equip: "none" plus any manufactured armour in stores. */
    public List<String> equipableArmors(Ruleset ruleset) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        if (ruleset.hasArmor("none")) {
            ids.add("none");
        }
        for (String id : stores.keySet()) {
            if (ruleset.hasArmor(id)) {
                ids.add(id);
            }
        }
        return new ArrayList<String>(ids);
    }
}
