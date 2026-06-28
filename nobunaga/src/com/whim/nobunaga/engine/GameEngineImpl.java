package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameEngine;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference implementation of {@link GameEngine}. Macro actions validate
 * gold/rice/ownership and mutate {@link GameState} in place, returning a single
 * human-readable line for the UI. {@link #endSeason} runs economy → events →
 * AI in order (it does NOT advance the clock — {@code GameLoopManager} does).
 */
public class GameEngineImpl implements GameEngine {

    // Action costs (per the contract's "guidance" formulas — balanced by seed).
    static final int CULTIVATE_COST = 100;
    static final int CULTIVATE_GAIN = 10;
    static final int FLOOD_COST = 80;
    static final int FLOOD_GAIN = 10;
    static final int RECRUIT_GOLD_PER = 1; // gold per soldier
    static final int RECRUIT_RICE_PER = 1; // rice per soldier (provisions)

    private final EconomyEngine economy = new EconomyEngine();
    private final EventEngine events = new EventEngine();
    private final BattleEngine battle = new BattleEngine();
    private final AiEngine ai = new AiEngine(economy, battle);

    // ---- macro actions ----------------------------------------------------

    public String setTax(GameState s, int provinceId, int rate) {
        Province p = owned(s, provinceId);
        if (p == null) {
            return "Cannot set tax: not your province.";
        }
        if (rate < 0 || rate > 100) {
            return "Tax rate must be 0..100.";
        }
        p.setTaxRate(rate);
        return p.getName() + ": tax rate set to " + rate + "%.";
    }

    public String cultivate(GameState s, int provinceId) {
        Province p = owned(s, provinceId);
        if (p == null) {
            return "Cannot cultivate: not your province.";
        }
        if (p.getCultivation() >= 100) {
            return p.getName() + ": cultivation already maxed.";
        }
        if (p.getGold() < CULTIVATE_COST) {
            return p.getName() + ": need " + CULTIVATE_COST + " gold to cultivate.";
        }
        p.setGold(p.getGold() - CULTIVATE_COST);
        int gain = Math.min(CULTIVATE_GAIN, 100 - p.getCultivation());
        p.setCultivation(p.getCultivation() + gain);
        return p.getName() + ": cultivation +" + gain + " (now " + p.getCultivation() + ").";
    }

    public String floodControl(GameState s, int provinceId) {
        Province p = owned(s, provinceId);
        if (p == null) {
            return "Cannot improve flood control: not your province.";
        }
        if (p.getFloodControl() >= 100) {
            return p.getName() + ": flood control already maxed.";
        }
        if (p.getGold() < FLOOD_COST) {
            return p.getName() + ": need " + FLOOD_COST + " gold for flood control.";
        }
        p.setGold(p.getGold() - FLOOD_COST);
        int gain = Math.min(FLOOD_GAIN, 100 - p.getFloodControl());
        p.setFloodControl(p.getFloodControl() + gain);
        return p.getName() + ": flood control +" + gain + " (now " + p.getFloodControl() + ").";
    }

    public String recruit(GameState s, int provinceId, int soldiers) {
        Province p = owned(s, provinceId);
        if (p == null) {
            return "Cannot recruit: not your province.";
        }
        if (soldiers <= 0) {
            return "Recruit a positive number of soldiers.";
        }
        int gold = soldiers * RECRUIT_GOLD_PER;
        int rice = soldiers * RECRUIT_RICE_PER;
        if (p.getGold() < gold || p.getRice() < rice) {
            return p.getName() + ": need " + gold + " gold + " + rice + " rice to recruit "
                    + soldiers + ".";
        }
        p.setGold(p.getGold() - gold);
        p.setRice(p.getRice() - rice);
        p.setSoldiers(p.getSoldiers() + soldiers);
        return p.getName() + ": recruited " + soldiers + " soldiers (now " + p.getSoldiers() + ").";
    }

    public String transfer(GameState s, int fromId, int toId, int gold, int rice, int soldiers) {
        if (fromId == toId) {
            return "Cannot transfer to the same province.";
        }
        Province from = owned(s, fromId);
        Province to = owned(s, toId);
        if (from == null || to == null) {
            return "Transfer requires two of your own provinces.";
        }
        if (from.getOwnerId() != to.getOwnerId()) {
            return "Transfer only between your own provinces.";
        }
        if (!from.getAdjacent().contains(Integer.valueOf(toId))) {
            return from.getName() + " and " + to.getName() + " are not adjacent.";
        }
        if (gold < 0 || rice < 0 || soldiers < 0) {
            return "Transfer amounts must be non-negative.";
        }
        if (from.getGold() < gold || from.getRice() < rice || from.getSoldiers() < soldiers) {
            return from.getName() + ": insufficient resources to transfer.";
        }
        from.setGold(from.getGold() - gold);
        from.setRice(from.getRice() - rice);
        from.setSoldiers(from.getSoldiers() - soldiers);
        to.setGold(to.getGold() + gold);
        to.setRice(to.getRice() + rice);
        to.setSoldiers(to.getSoldiers() + soldiers);
        return "Sent " + gold + "g/" + rice + "r/" + soldiers + "s from " + from.getName()
                + " to " + to.getName() + ".";
    }

    public List<String> endSeason(GameState s) {
        List<String> log = new ArrayList<String>();
        log.addAll(economy.process(s));
        log.addAll(events.process(s));
        log.addAll(ai.process(s));
        if (log.isEmpty()) {
            log.add("A quiet season passes.");
        }
        return log;
    }

    // ---- battle delegation -----------------------------------------------

    public BattleState startBattle(GameState s, int attackerProvId, int defenderProvId,
                                   int committedSoldiers, int committedRice) {
        return battle.startBattle(s, attackerProvId, defenderProvId, committedSoldiers, committedRice);
    }

    public void issueOrder(BattleState b, int unitId, int targetCol, int targetRow) {
        battle.issueOrder(b, unitId, targetCol, targetRow);
    }

    public void battleAdvanceDay(BattleState b) {
        battle.battleAdvanceDay(b);
    }

    public boolean battleResolved(BattleState b) {
        return battle.battleResolved(b);
    }

    public void applyBattleOutcome(GameState s, BattleState b) {
        battle.applyBattleOutcome(s, b);
    }

    // ---- helpers ----------------------------------------------------------

    /** Return the province if it exists and is owned by someone (not neutral), else null. */
    private Province owned(GameState s, int provinceId) {
        if (provinceId < 0 || provinceId >= s.provinces.size()) {
            return null;
        }
        Province p = s.province(provinceId);
        return p.isNeutral() ? null : p;
    }
}
