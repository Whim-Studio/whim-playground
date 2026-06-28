package com.whim.nobunaga.ui;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.GameEngine;
import com.whim.nobunaga.domain.GameLoopManager;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.util.List;

/**
 * Thin wiring layer between the Swing UI and the domain/engine packages.
 *
 * <p>This controller owns <b>no game rules</b>. It holds the live
 * {@link GameState}, the {@link GameEngine} (Task 2) and the
 * {@link GameLoopManager} (Task 1), tracks the currently selected province, and
 * forwards every macro action straight to the engine, returning the engine's
 * one-line result string for the UI to display. All mutation of game state
 * happens inside the engine.
 */
public final class GameController {

    private final GameState state;
    private final GameEngine engine;
    private final GameLoopManager loop;

    /** Currently selected province id, or {@code -1} when nothing is selected. */
    private int selected = -1;

    public GameController(GameState state, GameEngine engine, GameLoopManager loop) {
        this.state = state;
        this.engine = engine;
        this.loop = loop;
    }

    public GameState state() {
        return state;
    }

    public GameEngine engine() {
        return engine;
    }

    public int selected() {
        return selected;
    }

    public void setSelected(int provinceId) {
        this.selected = provinceId;
    }

    /** @return the selected {@link Province}, or {@code null} if none. */
    public Province selectedProvince() {
        return selected < 0 ? null : state.province(selected);
    }

    /** @return {@code true} if the selected province is owned by the player. */
    public boolean selectionOwnedByPlayer() {
        Province p = selectedProvince();
        return p != null && p.getOwnerId() == state.playerDaimyoId;
    }

    /** Header text, e.g. {@code "1560 Spring"}. */
    public String header() {
        return loop.seasonHeader(state);
    }

    // --- Macro actions: pure delegation to the engine ----------------------

    public String setTax(int provinceId, int rate) {
        return engine.setTax(state, provinceId, rate);
    }

    public String cultivate(int provinceId) {
        return engine.cultivate(state, provinceId);
    }

    public String floodControl(int provinceId) {
        return engine.floodControl(state, provinceId);
    }

    public String recruit(int provinceId, int soldiers) {
        return engine.recruit(state, provinceId, soldiers);
    }

    public String transfer(int fromId, int toId, int gold, int rice, int soldiers) {
        return engine.transfer(state, fromId, toId, gold, rice, soldiers);
    }

    /** Runs the season for every daimyo, then advances the clock. */
    public List<String> endSeason() {
        return loop.endSeason(state);
    }

    // --- Battle: pure delegation to the engine -----------------------------

    public BattleState startBattle(int attackerProvId, int defenderProvId,
                                   int committedSoldiers, int committedRice) {
        return engine.startBattle(state, attackerProvId, defenderProvId,
                committedSoldiers, committedRice);
    }

    public void issueOrder(BattleState b, int unitId, int targetCol, int targetRow) {
        engine.issueOrder(b, unitId, targetCol, targetRow);
    }

    public void battleAdvanceDay(BattleState b) {
        engine.battleAdvanceDay(b);
    }

    public boolean battleResolved(BattleState b) {
        return engine.battleResolved(b);
    }

    public void applyBattleOutcome(BattleState b) {
        engine.applyBattleOutcome(state, b);
    }
}
