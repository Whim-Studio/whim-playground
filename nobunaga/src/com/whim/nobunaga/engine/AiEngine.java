package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.util.ArrayList;
import java.util.List;

/**
 * Rival-daimyo decision tree, run once per season for every living non-player
 * lord. Each daimyo stabilises taxes, invests surplus gold into cultivation /
 * flood control, recruits when safe, then invades the weakest adjacent enemy or
 * neutral fief when it holds a clear troop advantage. Invasions auto-resolve
 * through {@link BattleEngine}.
 */
public class AiEngine {

    private final EconomyEngine economy;
    private final BattleEngine battle;

    public AiEngine(EconomyEngine economy, BattleEngine battle) {
        this.economy = economy;
        this.battle = battle;
    }

    public List<String> process(GameState s) {
        List<String> log = new ArrayList<String>();
        for (Daimyo d : s.daimyos) {
            if (d.getId() == s.playerDaimyoId || !d.isAlive()) {
                continue;
            }
            // Snapshot ids first: a successful invasion mutates the province list.
            List<Integer> owned = new ArrayList<Integer>(d.getProvinceIds());
            for (Integer pid : owned) {
                Province p = s.province(pid.intValue());
                if (p.getOwnerId() != d.getId()) {
                    continue; // lost mid-pass
                }
                manageDomestic(s, p);
            }
            String war = maybeInvade(s, d, log);
            if (war != null) {
                log.add(war);
            }
        }
        return log;
    }

    /** Tax/cultivate/flood/recruit choices for one province. */
    private void manageDomestic(GameState s, Province p) {
        // Keep loyalty stable: aim taxes around 45.
        if (p.getLoyalty() < 40 && p.getTaxRate() > 35) {
            p.setTaxRate(Math.max(20, p.getTaxRate() - 10));
        } else if (p.getLoyalty() > 70 && p.getTaxRate() < 50) {
            p.setTaxRate(Math.min(55, p.getTaxRate() + 5));
        }
        // Invest surplus gold.
        if (p.getGold() > 200 && p.getCultivation() < 90) {
            p.setCultivation(Math.min(100, p.getCultivation() + 8));
            p.setGold(p.getGold() - 100);
        }
        if (p.getGold() > 200 && p.getFloodControl() < 70) {
            p.setFloodControl(Math.min(100, p.getFloodControl() + 8));
            p.setGold(p.getGold() - 80);
        }
        // Recruit when treasury and granary allow it.
        if (p.getGold() > 150 && p.getRice() > 150 && p.getSoldiers() < 4000) {
            int hire = 300;
            p.setSoldiers(p.getSoldiers() + hire);
            p.setGold(p.getGold() - hire);
            p.setRice(p.getRice() - hire / 2);
        }
    }

    /** Pick the strongest base fief and invade its weakest beatable neighbour. */
    private String maybeInvade(GameState s, Daimyo d, List<String> log) {
        Province base = null;
        for (Integer pid : d.getProvinceIds()) {
            Province p = s.province(pid.intValue());
            if (base == null || p.getSoldiers() > base.getSoldiers()) {
                base = p;
            }
        }
        if (base == null || base.getSoldiers() < 600) {
            return null; // too weak to campaign
        }
        Province target = null;
        for (Integer adj : base.getAdjacent()) {
            Province cand = s.province(adj.intValue());
            if (cand.getOwnerId() == d.getId()) {
                continue;
            }
            if (target == null || cand.getSoldiers() < target.getSoldiers()) {
                target = cand;
            }
        }
        if (target == null) {
            return null;
        }
        // Need a clear advantage to commit.
        int commit = (int) (base.getSoldiers() * 0.7);
        if (commit < target.getSoldiers() * 1.3 + 100) {
            return null;
        }
        int rice = Math.min(base.getRice(), Math.max(50, commit / 4));
        BattleState b = battle.startBattle(s, base.getId(), target.getId(), commit, rice);
        int guard = 0;
        while (!battle.battleResolved(b) && guard++ < 60) {
            battle.battleAdvanceDay(b);
        }
        if (!battle.battleResolved(b)) {
            b.winnerDaimyoId = Integer.valueOf(b.defenderDaimyoId);
        }
        boolean won = b.winnerDaimyoId.intValue() == d.getId();
        battle.applyBattleOutcome(s, b);
        return d.getAbbrev() + " invades " + target.getName() + " — "
                + (won ? "VICTORY (annexed)" : "repelled");
    }
}
