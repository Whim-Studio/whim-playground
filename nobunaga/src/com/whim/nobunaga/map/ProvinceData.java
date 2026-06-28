package com.whim.nobunaga.map;

import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Static world data: builds the 50-province {@link GameState} for a new game.
 *
 * <p>Provinces use real Sengoku-era kuni names. Layout coords live in a 0..1000
 * virtual space arranged so Tohoku sits top-right and Kyushu bottom-left, giving
 * the recognizable NE&rarr;SW sweep of Honshu into Shikoku and Kyushu. The
 * adjacency graph is hand-built, connected, and symmetric; war and resource
 * transfer travel only along it.</p>
 *
 * <p>The 8 roster daimyo and their home province ids are fixed by the contract.
 * Each daimyo holds its home plus one or two nearby provinces; every other
 * province is neutral with a light garrison. Ownership is kept consistent on
 * both {@link Province#getOwnerId()} and {@link Daimyo#getProvinceIds()}.</p>
 */
public final class ProvinceData {

    private ProvinceData() {
    }

    /** Build a fresh 50-province world for the chosen daimyo and seed. */
    public static GameState newGame(int playerDaimyoId, long seed) {
        List<Province> provinces = buildProvinces();
        buildAdjacency(provinces);

        List<Daimyo> daimyos = buildDaimyos();

        // ownerId / provinceIds — home + 1-2 nearby per daimyo.
        assignOwnership(provinces, daimyos, 0, new int[] {20, 21, 22});  // Oda: Owari, Mino, Mikawa
        assignOwnership(provinces, daimyos, 1, new int[] {17, 18, 19});  // Takeda: Kai, Shinano, Suruga
        assignOwnership(provinces, daimyos, 2, new int[] {14, 15});      // Uesugi: Echigo, Etchu
        assignOwnership(provinces, daimyos, 3, new int[] {11, 10, 12});  // Hojo: Sagami, Musashi, Kazusa
        assignOwnership(provinces, daimyos, 4, new int[] {38, 39, 40});  // Mori: Aki, Bingo, Nagato
        assignOwnership(provinces, daimyos, 5, new int[] {48, 49, 43});  // Shimazu: Satsuma, Hyuga, Bungo
        assignOwnership(provinces, daimyos, 6, new int[] {5, 4, 1});     // Date: Mutsu-S, Iwashiro, Mutsu
        assignOwnership(provinces, daimyos, 7, new int[] {44, 45, 47});  // Chosokabe: Tosa, Iyo, Awa

        GameState state = new GameState(provinces, daimyos, playerDaimyoId, seed);

        // Economy + garrisons, using the single shared RNG so a seed reproduces.
        for (int i = 0; i < provinces.size(); i++) {
            Province p = provinces.get(i);
            if (p.isNeutral()) {
                p.setCultivation(15 + state.rng.nextInt(20));
                p.setLoyalty(55 + state.rng.nextInt(20));
                p.setTaxRate(40);
                p.setFloodControl(15 + state.rng.nextInt(15));
                p.setGold(80 + state.rng.nextInt(140));
                p.setRice(120 + state.rng.nextInt(160));
                p.setSoldiers(80 + state.rng.nextInt(140));
            }
        }
        for (int d = 0; d < daimyos.size(); d++) {
            Daimyo dm = daimyos.get(d);
            List<Integer> owned = dm.getProvinceIds();
            for (int j = 0; j < owned.size(); j++) {
                Province p = provinces.get(owned.get(j).intValue());
                boolean home = (j == 0);
                p.setCultivation(38 + state.rng.nextInt(15));
                p.setLoyalty(70 + state.rng.nextInt(12));
                p.setTaxRate(40);
                p.setFloodControl(25 + state.rng.nextInt(15));
                p.setGold(400 + state.rng.nextInt(300));
                p.setRice(500 + state.rng.nextInt(400));
                p.setSoldiers((home ? 600 : 350) + state.rng.nextInt(250));
            }
        }

        state.daimyo(playerDaimyoId).setPlayer(true);
        return state;
    }

    // ------------------------------------------------------------------
    // Provinces (id, name, x, y) — x grows east, y grows south, 0..1000.
    // ------------------------------------------------------------------
    private static List<Province> buildProvinces() {
        List<Province> ps = new ArrayList<Province>(50);
        // Tohoku
        ps.add(new Province(0,  "Dewa",        780, 70));
        ps.add(new Province(1,  "Mutsu",       900, 40));
        ps.add(new Province(2,  "Ugo",         820, 140));
        ps.add(new Province(3,  "Rikuchu",     910, 150));
        ps.add(new Province(4,  "Iwashiro",    800, 250));
        ps.add(new Province(5,  "Mutsu-S",     880, 230));
        // Kanto
        ps.add(new Province(6,  "Hitachi",     865, 330));
        ps.add(new Province(7,  "Shimotsuke",  800, 310));
        ps.add(new Province(8,  "Kozuke",      740, 320));
        ps.add(new Province(9,  "Shimosa",     875, 380));
        ps.add(new Province(10, "Musashi",     790, 390));
        ps.add(new Province(11, "Sagami",      780, 440));
        ps.add(new Province(12, "Kazusa",      870, 430));
        ps.add(new Province(13, "Awa-Boso",    885, 480));
        // Hokuriku
        ps.add(new Province(14, "Echigo",      690, 250));
        ps.add(new Province(15, "Etchu",       600, 300));
        ps.add(new Province(16, "Kaga",        560, 330));
        // Chubu / Tokai
        ps.add(new Province(17, "Kai",         720, 420));
        ps.add(new Province(18, "Shinano",     650, 390));
        ps.add(new Province(19, "Suruga",      710, 470));
        ps.add(new Province(20, "Owari",       590, 460));
        ps.add(new Province(21, "Mino",        580, 410));
        ps.add(new Province(22, "Mikawa",      630, 470));
        ps.add(new Province(23, "Totomi",      670, 475));
        ps.add(new Province(24, "Hida",        600, 360));
        ps.add(new Province(25, "Echizen",     540, 390));
        ps.add(new Province(26, "Ise",         560, 500));
        // Kinki
        ps.add(new Province(27, "Omi",         540, 450));
        ps.add(new Province(28, "Wakasa",      510, 400));
        ps.add(new Province(29, "Yamashiro",   500, 470));
        ps.add(new Province(30, "Yamato",      500, 520));
        ps.add(new Province(31, "Settsu",      470, 480));
        ps.add(new Province(32, "Kawachi",     480, 510));
        ps.add(new Province(33, "Izumi",       470, 535));
        ps.add(new Province(34, "Kii",         490, 575));
        ps.add(new Province(35, "Tanba",       460, 440));
        // Chugoku / San'yo
        ps.add(new Province(36, "Harima",      430, 490));
        ps.add(new Province(37, "Bizen",       390, 500));
        ps.add(new Province(38, "Aki",         300, 530));
        ps.add(new Province(39, "Bingo",       340, 515));
        ps.add(new Province(40, "Nagato",      230, 560));
        // Kyushu (north/east)
        ps.add(new Province(41, "Buzen",       200, 590));
        ps.add(new Province(42, "Chikuzen",    150, 600));
        ps.add(new Province(43, "Bungo",       195, 650));
        // Shikoku
        ps.add(new Province(44, "Tosa",        370, 660));
        ps.add(new Province(45, "Iyo",         320, 620));
        ps.add(new Province(46, "Sanuki",      410, 600));
        ps.add(new Province(47, "Awa-Shikoku", 440, 620));
        // Kyushu (south)
        ps.add(new Province(48, "Satsuma",     130, 760));
        ps.add(new Province(49, "Hyuga",       210, 720));
        return ps;
    }

    // ------------------------------------------------------------------
    // Connected, symmetric adjacency graph of geographic neighbors.
    // ------------------------------------------------------------------
    private static void buildAdjacency(List<Province> ps) {
        int[][] edges = {
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 4},
            {3, 5},
            {4, 5}, {4, 6}, {4, 8},
            {5, 6},
            {6, 7}, {6, 9},
            {7, 8}, {7, 9}, {7, 10},
            {8, 10}, {8, 14}, {8, 18},
            {9, 10}, {9, 12},
            {10, 11}, {10, 12}, {10, 17},
            {11, 17}, {11, 19},
            {12, 13},
            {14, 15}, {14, 18},
            {15, 16}, {15, 24},
            {16, 24}, {16, 25},
            {17, 18}, {17, 19},
            {18, 21}, {18, 24},
            {19, 23},
            {20, 21}, {20, 22}, {20, 26},
            {21, 24}, {21, 27},
            {22, 23},
            {24, 25},
            {25, 27}, {25, 28},
            {26, 27}, {26, 30}, {26, 34},
            {27, 28}, {27, 29}, {27, 35},
            {29, 30}, {29, 31}, {29, 35},
            {30, 32}, {30, 34},
            {31, 32}, {31, 35}, {31, 36},
            {32, 33},
            {33, 34},
            {34, 47},
            {35, 36},
            {36, 37}, {36, 46},
            {37, 39}, {37, 46},
            {38, 39}, {38, 40}, {38, 45},
            {39, 40}, {39, 45},
            {40, 41},
            {41, 42}, {41, 43},
            {42, 43},
            {43, 49},
            {44, 45}, {44, 46}, {44, 47},
            {45, 46},
            {46, 47},
            {48, 49},
        };
        for (int i = 0; i < edges.length; i++) {
            int a = edges[i][0];
            int b = edges[i][1];
            link(ps, a, b);
        }
    }

    private static void link(List<Province> ps, int a, int b) {
        List<Integer> la = ps.get(a).getAdjacent();
        List<Integer> lb = ps.get(b).getAdjacent();
        if (!la.contains(Integer.valueOf(b))) {
            la.add(Integer.valueOf(b));
        }
        if (!lb.contains(Integer.valueOf(a))) {
            lb.add(Integer.valueOf(a));
        }
    }

    // ------------------------------------------------------------------
    // Roster (FIXED ids / abbrev / colors per contract).
    // ------------------------------------------------------------------
    private static List<Daimyo> buildDaimyos() {
        List<Daimyo> d = new ArrayList<Daimyo>(8);
        d.add(new Daimyo(0, "Oda Nobunaga",       "ODA", new Color(200, 40, 40),  26, 90));
        d.add(new Daimyo(1, "Takeda Shingen",     "TAK", new Color(40, 80, 200),  39, 90));
        d.add(new Daimyo(2, "Uesugi Kenshin",     "UES", new Color(70, 160, 210), 30, 90));
        d.add(new Daimyo(3, "Hojo Ujiyasu",       "HOJ", new Color(225, 140, 30), 46, 90));
        d.add(new Daimyo(4, "Mori Motonari",      "MOR", new Color(40, 150, 70),  55, 85));
        d.add(new Daimyo(5, "Shimazu Yoshihisa",  "SHI", new Color(170, 60, 170), 27, 90));
        d.add(new Daimyo(6, "Date Terumune",      "DAT", new Color(120, 90, 160), 26, 90));
        d.add(new Daimyo(7, "Chosokabe Kunichika", "CHO", new Color(210, 190, 40), 36, 88));
        return d;
    }

    /** First id in {@code provinceIds} is the home province. */
    private static void assignOwnership(List<Province> ps, List<Daimyo> ds,
                                        int daimyoId, int[] provinceIds) {
        Daimyo dm = ds.get(daimyoId);
        for (int i = 0; i < provinceIds.length; i++) {
            int pid = provinceIds[i];
            ps.get(pid).setOwnerId(daimyoId);
            dm.getProvinceIds().add(Integer.valueOf(pid));
        }
    }
}
