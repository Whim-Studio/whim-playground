package com.whim.samurai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The complete, serialisable snapshot of a game in progress. Every screen reads
 * and mutates this object; save/load simply serialises it (design ref, ARCHITECTURE.md).
 */
public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public Samurai player;
    public Calendar calendar = new Calendar();

    public final List<Clan> clans = new ArrayList<Clan>();
    public final List<Province> provinces = new ArrayList<Province>();
    public final List<Rival> rivals = new ArrayList<Rival>();

    /** Id of the clan the player currently serves as a retainer (their liege). */
    public int liegeClanId = -1;
    /** Province the player is currently located in (their estate / seat). */
    public int currentProvinceId = 0;

    /** Family-honor score accumulated across the whole dynasty (design ref §3). */
    public long dynastyScore = 0;
    /** Generation number; increments when an heir takes over. */
    public int generation = 1;

    public boolean gameOver = false;
    public String gameOverReason = "";
    public boolean victory = false;

    // ---- lookup helpers -------------------------------------------------

    public Clan clan(int id) {
        for (Clan c : clans) if (c.id == id) return c;
        return null;
    }

    public Clan playerClan() {
        for (Clan c : clans) if (c.isPlayer) return c;
        return null;
    }

    public Province province(int id) {
        for (Province p : provinces) if (p.id == id) return p;
        return null;
    }

    public int provinceCountFor(int clanId) {
        int n = 0;
        for (Province p : provinces) if (p.ownerClanId == clanId) n++;
        return n;
    }

    public List<Rival> livingRivalsInClan(int clanId) {
        List<Rival> out = new ArrayList<Rival>();
        for (Rival r : rivals) if (r.alive && r.clanId == clanId) out.add(r);
        return out;
    }
}
