package com.whim.xcom.meta;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;

/**
 * JSON save/load of the campaign meta-state via Gson. A {@link Snapshot} is a
 * flat, ruleset-free DTO (only ids + numbers), so a save survives across runs and
 * is rebuilt against the live {@link Ruleset} on load. The Geoscape's funds,
 * score and clock are carried alongside as primitives to keep this package free
 * of any dependency on the geo layer.
 */
public final class SaveGame {

    private SaveGame() {
    }

    /** Flat serialisable snapshot of a whole campaign. */
    public static final class Snapshot {
        public long funds;
        public int score;
        public long clockSeconds;
        public int scientists;
        public int engineers;
        public int consecutiveBadMonths;
        public boolean gameWon;
        public boolean gameLost;
        public List<String> completedResearch = new ArrayList<String>();
        public List<ResearchSnap> research = new ArrayList<ResearchSnap>();
        public List<ManufactureSnap> manufacture = new ArrayList<ManufactureSnap>();
        public Map<String, Integer> stores = new LinkedHashMap<String, Integer>();
        public List<SoldierSnap> soldiers = new ArrayList<SoldierSnap>();
    }

    public static final class ResearchSnap {
        public String id;
        public int scientists;
        public double progress;
    }

    public static final class ManufactureSnap {
        public String id;
        public int engineers;
        public int quantityRemaining;
        public double progressHours;
    }

    public static final class SoldierSnap {
        public String name;
        public int timeUnits;
        public int health;
        public int firingAccuracy;
        public int reactions;
        public int strength;
        public int rank;
        public int missions;
        public int kills;
        public int woundedDays;
        public String weaponId;
        public String armorId;
    }

    private static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    // ---- capture ------------------------------------------------------------

    public static Snapshot capture(Campaign c, long funds, int score, long clockSeconds) {
        return capture(c, funds, score, clockSeconds, 0, false, false);
    }

    public static Snapshot capture(Campaign c, long funds, int score, long clockSeconds,
                                   int consecutiveBadMonths, boolean gameWon, boolean gameLost) {
        Snapshot s = new Snapshot();
        s.funds = funds;
        s.score = score;
        s.clockSeconds = clockSeconds;
        s.consecutiveBadMonths = consecutiveBadMonths;
        s.gameWon = gameWon;
        s.gameLost = gameLost;
        s.scientists = c.scientists();
        s.engineers = c.engineers();
        s.completedResearch.addAll(c.completedResearch());
        for (ResearchProject p : c.activeResearch()) {
            if (p.complete()) {
                continue;
            }
            ResearchSnap r = new ResearchSnap();
            r.id = p.id();
            r.scientists = p.assignedScientists();
            r.progress = p.progressScientistDays();
            s.research.add(r);
        }
        for (ManufactureJob j : c.manufacturing()) {
            ManufactureSnap m = new ManufactureSnap();
            m.id = j.id();
            m.engineers = j.assignedEngineers();
            m.quantityRemaining = j.quantityRemaining();
            m.progressHours = j.progressHours();
            s.manufacture.add(m);
        }
        s.stores.putAll(c.stores());
        for (Soldier sol : c.roster().soldiers()) {
            SoldierSnap ss = new SoldierSnap();
            ss.name = sol.name();
            ss.timeUnits = sol.timeUnits();
            ss.health = sol.health();
            ss.firingAccuracy = sol.firingAccuracy();
            ss.reactions = sol.reactions();
            ss.strength = sol.strength();
            ss.rank = sol.rank();
            ss.missions = sol.missions();
            ss.kills = sol.kills();
            ss.woundedDays = sol.woundedDays();
            ss.weaponId = sol.weaponId();
            ss.armorId = sol.armorId();
            s.soldiers.add(ss);
        }
        return s;
    }

    /** Rebuild a {@link Campaign} from a snapshot against the live ruleset. */
    public static Campaign restoreCampaign(Snapshot s, Ruleset ruleset) {
        SoldierRoster roster = new SoldierRoster();
        for (SoldierSnap ss : s.soldiers) {
            Soldier sol = new Soldier(ss.name, ss.timeUnits, ss.health,
                    ss.firingAccuracy, ss.reactions, ss.strength);
            sol.restore(ss.rank, ss.missions, ss.kills, ss.woundedDays);
            sol.equip(ss.weaponId, ss.armorId);
            roster.add(sol);
        }
        Campaign c = new Campaign(s.scientists, s.engineers, roster);
        c.completedResearch().addAll(s.completedResearch);
        for (ResearchSnap r : s.research) {
            ResearchNode node = ruleset.research(r.id);
            if (node != null) {
                ResearchProject p = c.startResearch(node, r.scientists);
                p.restoreProgress(r.progress);
            }
        }
        for (ManufactureSnap m : s.manufacture) {
            ManufactureNode node = ruleset.manufacture(m.id);
            if (node != null) {
                ManufactureJob j = c.startManufacture(node, m.engineers, m.quantityRemaining);
                j.restoreProgress(m.progressHours);
            }
        }
        for (Map.Entry<String, Integer> e : s.stores.entrySet()) {
            c.addToStores(e.getKey(), e.getValue());
        }
        return c;
    }

    // ---- disk ---------------------------------------------------------------

    public static void write(Snapshot snapshot, File file) throws IOException {
        Writer w = new FileWriter(file);
        try {
            gson().toJson(snapshot, w);
        } finally {
            w.close();
        }
    }

    public static Snapshot read(File file) throws IOException {
        Reader r = new FileReader(file);
        try {
            return gson().fromJson(r, Snapshot.class);
        } finally {
            r.close();
        }
    }

    public static String toJson(Snapshot snapshot) {
        return gson().toJson(snapshot);
    }

    public static Snapshot fromJson(String json) {
        return gson().fromJson(json, Snapshot.class);
    }
}
