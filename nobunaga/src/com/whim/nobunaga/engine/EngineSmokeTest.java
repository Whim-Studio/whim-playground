package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;
import com.whim.nobunaga.domain.Season;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-free self-check for the engine math. Builds a tiny two-daimyo world
 * by hand (no ProvinceData / UI) and asserts the headline formulas and a battle
 * outcome. Run: {@code java -cp out com.whim.nobunaga.engine.EngineSmokeTest}
 */
public class EngineSmokeTest {

    private static int checks = 0;

    public static void main(String[] args) {
        testEconomyFormulas();
        testMacroActions();
        testEndSeasonDeterminism();
        testBattle();
        System.out.println("OK — all " + checks + " engine checks passed.");
    }

    // --- helpers -----------------------------------------------------------

    private static void check(boolean cond, String msg) {
        checks++;
        if (!cond) {
            throw new AssertionError("FAILED: " + msg);
        }
    }

    private static void eq(long actual, long expected, String msg) {
        check(actual == expected, msg + " (expected " + expected + ", got " + actual + ")");
    }

    /** Two daimyos, three provinces (A0,A1 owned by 0; B2 owned by 1), A0-B2 adjacent. */
    private static GameState world(long seed) {
        List<Province> ps = new ArrayList<Province>();
        Province a0 = new Province(0, "Owari", 500, 500);
        Province a1 = new Province(1, "Mino", 480, 460);
        Province b2 = new Province(2, "Kai", 560, 480);
        ps.add(a0); ps.add(a1); ps.add(b2);
        a0.getAdjacent().add(1); a0.getAdjacent().add(2);
        a1.getAdjacent().add(0);
        b2.getAdjacent().add(0);

        List<Daimyo> ds = new ArrayList<Daimyo>();
        Daimyo oda = new Daimyo(0, "Oda", "ODA", new Color(200, 40, 40), 27, 90);
        Daimyo tak = new Daimyo(1, "Takeda", "TAK", new Color(40, 80, 200), 40, 90);
        ds.add(oda); ds.add(tak);

        a0.setOwnerId(0); oda.getProvinceIds().add(0);
        a1.setOwnerId(0); oda.getProvinceIds().add(1);
        b2.setOwnerId(1); tak.getProvinceIds().add(2);

        return new GameState(ps, ds, 0, seed);
    }

    // --- tests -------------------------------------------------------------

    private static void testEconomyFormulas() {
        EconomyEngine e = new EconomyEngine();
        Province p = new Province(0, "Test", 0, 0);
        p.setCultivation(50);
        p.setLoyalty(80);
        p.setTaxRate(40);
        // harvest = round(50*12*(0.6+0.8)) = round(600*1.4)=840
        eq(e.harvest(p), 840, "harvest formula");
        // tax = round(50*40*0.05*0.8)= round(80)=80
        eq(e.taxYield(p), 80, "tax formula");
        // upkeep(250) = ceil(250/100*4)=ceil(10)=10
        eq(e.upkeep(250), 10, "upkeep formula");
        // drift toward 100-40=60 from 80 → -5 → 75
        e.driftLoyalty(p);
        eq(p.getLoyalty(), 75, "loyalty drift toward 100-tax");
    }

    private static void testMacroActions() {
        GameEngineImpl g = new GameEngineImpl();
        GameState s = world(1L);
        Province a0 = s.province(0);
        a0.setGold(500);
        a0.setRice(500);

        int cult0 = a0.getCultivation();
        g.cultivate(s, 0);
        eq(a0.getCultivation(), cult0 + GameEngineImpl.CULTIVATE_GAIN, "cultivate gain");
        eq(a0.getGold(), 500 - GameEngineImpl.CULTIVATE_COST, "cultivate cost");

        String taxMsg = g.setTax(s, 0, 55);
        eq(a0.getTaxRate(), 55, "setTax applied");
        check(taxMsg.contains("55"), "setTax message");

        // recruit: 100 soldiers = 100 gold + 100 rice
        int gold = a0.getGold(), rice = a0.getRice(), sold = a0.getSoldiers();
        g.recruit(s, 0, 100);
        eq(a0.getSoldiers(), sold + 100, "recruit soldiers");
        eq(a0.getGold(), gold - 100, "recruit gold cost");
        eq(a0.getRice(), rice - 100, "recruit rice cost");

        // ownership validation: acting on neutral / enemy province fails
        s.province(2).setOwnerId(-1);
        check(g.cultivate(s, 2).startsWith("Cannot"), "cultivate rejects neutral province");

        // transfer along adjacency between two owned provinces succeeds
        Province a1 = s.province(1);
        a1.setGold(100);
        String tmsg = g.transfer(s, 1, 0, 50, 0, 0);
        check(tmsg.startsWith("Sent"), "transfer along adjacency succeeds: " + tmsg);
        eq(a1.getGold(), 50, "transfer debits source");
    }

    private static void testEndSeasonDeterminism() {
        // Same seed → identical logs (proves all RNG flows through GameState.rng).
        GameState a = world(42L); a.season = Season.FALL;
        GameState b = world(42L); b.season = Season.FALL;
        for (Province p : a.provinces) { p.setGold(300); p.setRice(300); p.setSoldiers(500); }
        for (Province p : b.provinces) { p.setGold(300); p.setRice(300); p.setSoldiers(500); }
        GameEngineImpl g = new GameEngineImpl();
        List<String> la = g.endSeason(a);
        List<String> lb = g.endSeason(b);
        eq(la.size(), lb.size(), "endSeason deterministic log size");
        for (int i = 0; i < la.size(); i++) {
            check(la.get(i).equals(lb.get(i)), "endSeason deterministic line " + i);
        }
    }

    private static void testBattle() {
        GameEngineImpl g = new GameEngineImpl();
        GameState s = world(7L);
        Province atk = s.province(0);
        Province def = s.province(2);
        atk.setSoldiers(3000); atk.setRice(2000);
        def.setSoldiers(300); def.setRice(50);

        BattleState b = g.startBattle(s, 0, 2, 2500, 1000);
        check(!b.units.isEmpty(), "battle has units deployed");
        int guard = 0;
        while (!g.battleResolved(b) && guard++ < 40) {
            g.battleAdvanceDay(b);
        }
        check(g.battleResolved(b), "battle reaches a decision");
        // Overwhelming attacker should win and annex the province.
        eq(b.winnerDaimyoId.intValue(), 0, "strong attacker wins");
        g.applyBattleOutcome(s, b);
        eq(def.getOwnerId(), 0, "conquered province changes owner");
        check(s.daimyo(0).getProvinceIds().contains(Integer.valueOf(2)), "winner gains province id");
        check(!s.daimyo(1).getProvinceIds().contains(Integer.valueOf(2)), "loser drops province id");
    }
}
