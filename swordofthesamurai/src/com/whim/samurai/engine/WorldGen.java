package com.whim.samurai.engine;

import com.whim.samurai.model.Calendar;
import com.whim.samurai.model.Clan;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rank;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;

/**
 * Builds a fresh strategic world: a stylised map of the lower three islands of
 * Sengoku-era Japan (Kyushu, Shikoku, Honshu) with the ~48 historical provinces
 * of the original (design ref §0, §1.4 — "a map of Japan and its forty-eight
 * provinces"), divided among rival daimyo houses with a gray unaligned belt.
 *
 * The player is seated as a low-ranking clan samurai (a <b>gokenin</b>, design
 * ref §1.1) holding a single fief and serving a liege daimyo, with rival
 * clansmen competing for favour and enemy champions defending foreign provinces.
 *
 * Public signature kept: {@code GameState generate(String, String)}.
 */
public class WorldGen {

    private final Rng rng;
    public WorldGen(Rng rng) { this.rng = rng; }

    // --- map canvas rectangle: MUST match MapScreen's MAP_* constants ---------
    static final int MAP_X0 = 40, MAP_Y0 = 150, MAP_W = 560, MAP_H = 300;
    private static final int GRID_W = 15, GRID_H = 7;

    /**
     * The 48 kuni (old provinces) of the lower three islands, laid out on a
     * stylised 16x8 grid roughly following geography from Kyushu (south-west)
     * through Shikoku and the Chugoku/Kinai heartland to the Tokai in the east.
     * Columns/rows are APPROXIMATE and deliberately stylised (design ref §1.4);
     * they only need to give a coherent adjacency graph and a Japan-ish shape.
     * Format: { name, gridX (0..15), gridY (0..7) }.
     */
    private static final Object[][] KUNI = {
        // Kyushu (far south-west)
        {"Satsuma", 1, 7}, {"Osumi", 2, 7}, {"Hyuga", 3, 6}, {"Higo", 1, 6},
        {"Hizen", 0, 5}, {"Chikugo", 2, 5}, {"Bungo", 3, 5}, {"Chikuzen", 1, 4},
        {"Buzen", 3, 4},
        // Shikoku (south-centre)
        {"Iyo", 4, 6}, {"Tosa", 5, 7}, {"Sanuki", 5, 6}, {"Awa", 6, 6},
        // Chugoku (western Honshu)
        {"Nagato", 4, 4}, {"Suo", 5, 4}, {"Aki", 5, 3}, {"Iwami", 4, 2},
        {"Izumo", 5, 2}, {"Hoki", 6, 2}, {"Inaba", 6, 1}, {"Bingo", 6, 3},
        {"Bitchu", 7, 3}, {"Bizen", 7, 4}, {"Mimasaka", 7, 2}, {"Tajima", 8, 1},
        {"Harima", 8, 3},
        // Kinai / central heartland
        {"Tamba", 8, 2}, {"Tango", 9, 1}, {"Settsu", 9, 3}, {"Izumi", 9, 4},
        {"Kawachi", 10, 4}, {"Yamato", 10, 5}, {"Kii", 10, 6}, {"Yamashiro", 10, 3},
        {"Wakasa", 10, 1}, {"Omi", 11, 3}, {"Echizen", 11, 1}, {"Iga", 11, 4},
        {"Ise", 12, 4},
        // Tokai / Tosando (east)
        {"Mino", 12, 2}, {"Owari", 12, 3}, {"Hida", 11, 2}, {"Mikawa", 13, 3},
        {"Totomi", 14, 3}, {"Suruga", 15, 3}, {"Kai", 14, 2}, {"Shinano", 13, 1},
        {"Kozuke", 15, 1}
    };

    // Rival daimyo houses and the province each is seated in (historical seats).
    private static final String[] RIVAL_CLANS = { "Takeda", "Mori", "Shimazu", "Imagawa", "Uesugi" };
    private static final String[] RIVAL_SEATS = { "Kai", "Aki", "Satsuma", "Suruga", "Echizen" };
    private static final String PLAYER_SEAT   = "Owari"; // the player-clan's home province

    private static final String[] GIVEN = {
        "Nobu", "Hide", "Masa", "Kage", "Yoshi", "Katsu", "Uji", "Tada", "Naga", "Toki", "Haru", "Mune"
    };
    private static final String[] SUFFIX = {
        "hide", "naga", "masa", "tora", "yuki", "moto", "teru", "katsu", "tada", "hisa"
    };

    public GameState generate(String playerName, String clanName) {
        GameState s = new GameState();
        s.calendar = new Calendar(); // Spring 1560 — the eve of Okehazama

        buildProvinces(s);
        buildAdjacency(s);
        int neutralClanId = buildClans(s, clanName);
        assignOwnership(s, clanName, neutralClanId);
        seatPlayer(s, playerName, clanName);
        buildRivals(s);
        scoreClans(s);

        return s;
    }

    // ---- provinces ------------------------------------------------------------
    private void buildProvinces(GameState s) {
        for (int i = 0; i < KUNI.length; i++) {
            String name = (String) KUNI[i][0];
            int gx = (Integer) KUNI[i][1], gy = (Integer) KUNI[i][2];
            int px = MAP_X0 + gx * MAP_W / GRID_W + rng.range(-7, 7);
            int py = MAP_Y0 + gy * MAP_H / GRID_H + rng.range(-7, 7);
            Province p = new Province(i, name, px, py);
            p.rice = rng.range(60, 170);
            p.garrison = rng.range(20, 70);
            p.development = 1;
            s.provinces.add(p);
        }
    }

    /**
     * Adjacency by grid-Chebyshev proximity (neighbours differ by at most 1 in
     * both axes). This yields a coherent connected graph whose few sea crossings
     * (e.g. the Kanmon strait Buzen–Nagato) fall out naturally (design ref §1.4).
     */
    private void buildAdjacency(GameState s) {
        int n = s.provinces.size();
        for (int i = 0; i < n; i++) {
            int agx = (Integer) KUNI[i][1], agy = (Integer) KUNI[i][2];
            Province pi = s.provinces.get(i);
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                int bgx = (Integer) KUNI[j][1], bgy = (Integer) KUNI[j][2];
                if (Math.abs(agx - bgx) <= 1 && Math.abs(agy - bgy) <= 1) {
                    if (!pi.neighbors.contains(j)) pi.neighbors.add(j);
                }
            }
        }
        // Safety net: no province should be an island (connect to nearest).
        for (int i = 0; i < n; i++) {
            Province pi = s.provinces.get(i);
            if (!pi.neighbors.isEmpty()) continue;
            int best = -1; double bd = Double.MAX_VALUE;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double d = dist(pi, s.provinces.get(j));
                if (d < bd) { bd = d; best = j; }
            }
            if (best >= 0) { pi.neighbors.add(best); s.provinces.get(best).neighbors.add(i); }
        }
    }

    private static double dist(Province a, Province b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ---- clans ---------------------------------------------------------------
    /** Returns the id of the neutral ("Independent") clan holding unaligned land. */
    private int buildClans(GameState s, String clanName) {
        Clan player = new Clan(0, clanName, randName(), 0); // colour index 0 (cinnabar)
        player.isPlayer = true;
        s.clans.add(player);
        for (int k = 0; k < RIVAL_CLANS.length; k++) {
            Clan c = new Clan(k + 1, RIVAL_CLANS[k], randName(), k + 1);
            c.relationToPlayer = rng.range(-40, 10);
            s.clans.add(c);
        }
        int neutralId = RIVAL_CLANS.length + 1; // 6
        Clan neutral = new Clan(neutralId, "Independent", "(unaligned)", 7); // slate/gray
        s.clans.add(neutral);
        return neutralId;
    }

    /**
     * Voronoi-style ownership: each province joins the nearest daimyo seat within
     * grid-distance 2, otherwise stays unaligned (design ref §1.4 "unaligned
     * provinces are gray"). The imperial heartland — Omi (Kyoto) and Yamashiro —
     * is forced unaligned: the Emperor grants the Shogunate but wields no real
     * power here (design ref §1.2, §6.5), so Omi becomes the prize to capture.
     */
    private void assignOwnership(GameState s, String clanName, int neutralId) {
        int[] seed = new int[RIVAL_CLANS.length + 1]; // seed[clanId] = province index
        seed[0] = indexOf(PLAYER_SEAT);
        for (int k = 0; k < RIVAL_SEATS.length; k++) seed[k + 1] = indexOf(RIVAL_SEATS[k]);

        final int RADIUS = 2; // approximation: keeps clusters modest & leaves a gray belt
        for (int i = 0; i < s.provinces.size(); i++) {
            int gx = (Integer) KUNI[i][1], gy = (Integer) KUNI[i][2];
            int owner = neutralId, bestD = Integer.MAX_VALUE;
            for (int cid = 0; cid < seed.length; cid++) {
                int sgx = (Integer) KUNI[seed[cid]][1], sgy = (Integer) KUNI[seed[cid]][2];
                int d = Math.max(Math.abs(gx - sgx), Math.abs(gy - sgy)); // Chebyshev
                if (d < bestD) { bestD = d; owner = (d <= RADIUS) ? cid : neutralId; }
            }
            Province p = s.provinces.get(i);
            p.ownerClanId = owner;
        }
        // Force the imperial heartland unaligned (the capture target for victory).
        forceNeutral(s, "Omi", neutralId);
        forceNeutral(s, "Yamashiro", neutralId);

        // Populate each clan's province list from the resolved ownership.
        for (Province p : s.provinces) {
            Clan c = s.clan(p.ownerClanId);
            if (c != null && !c.provinces.contains(p.id)) c.provinces.add(p.id);
        }
    }

    private void forceNeutral(GameState s, String name, int neutralId) {
        Province p = s.province(indexOf(name));
        if (p != null) p.ownerClanId = neutralId;
    }

    // ---- player --------------------------------------------------------------
    private void seatPlayer(GameState s, String playerName, String clanName) {
        Samurai you = new Samurai();
        you.name = playerName;
        you.clanName = clanName;
        you.rank = Rank.SAMURAI;      // a gokenin (design ref §1.1)
        you.age = 15;                 // just after gempuku (design ref §1.1 / Appendix A)
        you.honor = 90;               // a modest but respectable young samurai
        you.power = 30;
        you.swordsmanship = rng.range(6, 10);
        you.generalship = rng.range(5, 9);
        you.stealth = rng.range(4, 8);
        you.koku = 200;

        Province seat = s.province(indexOf(PLAYER_SEAT));
        if (seat != null) {
            you.fiefs.add(seat.id);   // a gokenin holds a single fief (design ref §1.1)
            seat.development = 2;
            s.currentProvinceId = seat.id;
        }
        s.player = you;
        s.liegeClanId = 0;            // the player serves their own clan's daimyo
    }

    // ---- rivals --------------------------------------------------------------
    private void buildRivals(GameState s) {
        // Rival vassals inside your own clan competing for the daimyo's favour.
        for (int i = 0; i < 3; i++) {
            Rival r = new Rival(randName(), 0);
            r.swordsmanship = rng.range(6, 12);
            r.honor = rng.range(80, 160);
            r.power = rng.range(40, 100);
            r.hostility = rng.range(-10, 30);
            s.rivals.add(r);
        }
        // A champion leading each foreign daimyo's garrisons.
        for (int cid = 1; cid < s.clans.size() - 1; cid++) { // exclude the neutral clan
            Rival r = new Rival(randName(), cid);
            r.swordsmanship = rng.range(8, 14);
            r.honor = rng.range(90, 180);
            r.power = rng.range(80, 160);
            r.hostility = rng.range(0, 40);
            s.rivals.add(r);
        }
    }

    private void scoreClans(GameState s) {
        for (Clan c : s.clans) {
            int n = c.provinces.size();
            c.power = 40 + n * 18;               // power tracks territory (design ref §3)
            c.honor = rng.range(90, 150);
        }
    }

    // ---- helpers -------------------------------------------------------------
    private int indexOf(String name) {
        for (int i = 0; i < KUNI.length; i++) if (KUNI[i][0].equals(name)) return i;
        return 0;
    }

    private String randName() { return rng.pick(GIVEN) + rng.pick(SUFFIX); }
}
