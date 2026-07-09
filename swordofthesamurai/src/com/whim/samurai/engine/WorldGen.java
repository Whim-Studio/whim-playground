package com.whim.samurai.engine;

import com.whim.samurai.model.Calendar;
import com.whim.samurai.model.Clan;
import com.whim.samurai.model.FamilyMember;
import com.whim.samurai.model.GameState;
import com.whim.samurai.model.Province;
import com.whim.samurai.model.Rank;
import com.whim.samurai.model.Rival;
import com.whim.samurai.model.Samurai;

/**
 * Builds a fresh strategic world: a stylised map of central Japan with a handful
 * of provinces divided among rival clans, the player installed as a low-ranking
 * samurai serving a liege daimyo, and a roster of rival samurai (design ref §6).
 *
 * NOTE: this is the baseline generator. The campaign/politics task may enrich the
 * province graph, clan AI dispositions and rival rosters.
 */
public class WorldGen {

    private final Rng rng;
    public WorldGen(Rng rng) { this.rng = rng; }

    private static final String[] PROVINCE_NAMES = {
        "Owari", "Mino", "Ise", "Omi", "Yamato", "Kii", "Settsu", "Kawachi",
        "Tamba", "Harima", "Bizen", "Mikawa", "Totomi", "Suruga", "Kai", "Shinano"
    };
    private static final String[] CLAN_NAMES = {
        "Takeda", "Oda", "Imagawa", "Asai", "Mori", "Uesugi"
    };
    private static final String[] GIVEN = {
        "Nobu", "Hide", "Masa", "Kage", "Yoshi", "Katsu", "Uji", "Tada", "Naga", "Toki", "Haru", "Mune"
    };
    private static final String[] SUFFIX = {
        "hide", "naga", "masa", "tora", "yuki", "moto", "teru", "katsu", "tada", "hisa"
    };

    public GameState generate(String playerName, String clanName) {
        GameState s = new GameState();
        s.calendar = new Calendar();

        // --- provinces on a 4x4 stylised grid --------------------------------
        int cols = 4, rows = 4;
        int ox = 120, oy = 90, dx = 150, dy = 110;
        for (int i = 0; i < PROVINCE_NAMES.length; i++) {
            int c = i % cols, r = i / cols;
            Province p = new Province(i, PROVINCE_NAMES[i],
                    ox + c * dx + rng.range(-16, 16),
                    oy + r * dy + rng.range(-16, 16));
            p.rice = rng.range(60, 160);
            p.garrison = rng.range(25, 60);
            s.provinces.add(p);
        }
        // 4-connected neighbours
        for (int i = 0; i < s.provinces.size(); i++) {
            int c = i % cols, r = i / cols;
            Province p = s.provinces.get(i);
            if (c > 0)        p.neighbors.add(i - 1);
            if (c < cols - 1) p.neighbors.add(i + 1);
            if (r > 0)        p.neighbors.add(i - cols);
            if (r < rows - 1) p.neighbors.add(i + cols);
        }

        // --- clans -----------------------------------------------------------
        // Player's own clan is index 0.
        Clan player = new Clan(0, clanName, "You & your liege", 0);
        player.isPlayer = true;
        s.clans.add(player);
        int nRivalClans = 3;
        for (int k = 0; k < nRivalClans; k++) {
            Clan c = new Clan(k + 1, CLAN_NAMES[k % CLAN_NAMES.length],
                    randName(), (k + 1) % 8);
            c.relationToPlayer = rng.range(-30, 20);
            s.clans.add(c);
        }

        // distribute provinces round-robin among clans
        for (int i = 0; i < s.provinces.size(); i++) {
            Province p = s.provinces.get(i);
            int owner = i % s.clans.size();
            p.ownerClanId = owner;
            s.clan(owner).provinces.add(p.id);
        }

        // --- player samurai --------------------------------------------------
        Samurai you = new Samurai();
        you.name = playerName;
        you.clanName = clanName;
        you.rank = Rank.SAMURAI;
        you.age = 20;
        you.honor = 120; you.power = 40;
        you.swordsmanship = rng.range(7, 11);
        you.generalship = rng.range(6, 10);
        you.stealth = rng.range(5, 9);
        you.koku = 200;
        // seat the player in the first province of their own clan
        Province seat = null;
        for (Province p : s.provinces) if (p.ownerClanId == 0) { seat = p; break; }
        if (seat != null) {
            you.fiefs.add(seat.id);
            seat.development = 2;
            s.currentProvinceId = seat.id;
        }
        s.player = you;
        s.liegeClanId = 0;

        // --- rival samurai within the player's clan (compete for favour) -----
        int rivalsInClan = 3;
        for (int i = 0; i < rivalsInClan; i++) {
            Rival r = new Rival(randName(), 0);
            r.swordsmanship = rng.range(6, 12);
            r.honor = rng.range(80, 160);
            r.power = rng.range(40, 100);
            r.hostility = rng.range(-10, 30);
            s.rivals.add(r);
        }
        // a few champions in enemy clans
        for (int cid = 1; cid < s.clans.size(); cid++) {
            Rival r = new Rival(randName(), cid);
            r.swordsmanship = rng.range(8, 14);
            r.honor = rng.range(90, 180);
            r.power = rng.range(80, 160);
            s.rivals.add(r);
        }

        return s;
    }

    private String randName() {
        return rng.pick(GIVEN) + rng.pick(SUFFIX);
    }
}
