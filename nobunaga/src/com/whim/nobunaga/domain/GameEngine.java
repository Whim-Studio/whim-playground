package com.whim.nobunaga.domain;

import java.util.List;

/**
 * All game rules live behind this interface (implemented by Task 2's
 * {@code GameEngineImpl}). Macro actions mutate {@link GameState} in place and
 * return a one-line, human-readable result (success or the reason for failure)
 * that the UI shows. Implementations validate gold/rice/ownership/adjacency.
 */
public interface GameEngine {

    /** Set a province's tax rate (0..100). */
    String setTax(GameState s, int provinceId, int rate);

    /** Spend gold to raise the province's cultivation (wealth/yield base). */
    String cultivate(GameState s, int provinceId);

    /** Spend gold to raise the province's flood control (disaster mitigation). */
    String floodControl(GameState s, int provinceId);

    /** Spend gold + rice to add soldiers to the province garrison. */
    String recruit(GameState s, int provinceId, int soldiers);

    /** Move gold/rice/soldiers between two adjacent owned provinces. */
    String transfer(GameState s, int fromId, int toId, int gold, int rice, int soldiers);

    /**
     * Resolve the current season for everyone: economy, events, rebellions, and
     * rival AI. Returns a log of what happened. Does NOT advance the clock
     * (the GameLoopManager does that).
     */
    List<String> endSeason(GameState s);

    // ---- Tactical battle ----

    /** Begin a battle: the attacker commits soldiers + rice against the defender province. */
    BattleState startBattle(GameState s, int attackerProvId, int defenderProvId,
                            int committedSoldiers, int committedRice);

    /** Queue a player move/attack order for one unit, resolved on the next day. */
    void issueOrder(BattleState b, int unitId, int targetCol, int targetRow);

    /**
     * Resolve one DAY: AI moves for enemy and un-ordered units, melee, daily
     * supply burn, and victory check (sets winnerDaimyoId if decided).
     */
    void battleAdvanceDay(BattleState b);

    /** True once the battle has a winner. */
    boolean battleResolved(BattleState b);

    /** Apply troop losses and, if the attacker won, transfer the province. */
    void applyBattleOutcome(GameState s, BattleState b);
}
