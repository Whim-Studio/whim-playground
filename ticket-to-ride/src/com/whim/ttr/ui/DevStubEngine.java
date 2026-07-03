package com.whim.ttr.ui;

import com.whim.ttr.api.ActionOutcome;
import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.GameConstants;
import com.whim.ttr.api.GameEngine;
import com.whim.ttr.api.GamePhase;
import com.whim.ttr.domain.DestinationTicket;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.Player;
import com.whim.ttr.domain.PlayerScore;

import java.util.ArrayList;
import java.util.List;

/**
 * DEV-ONLY throwaway engine so the UI can be exercised standalone before the
 * real {@code com.whim.ttr.engine.RulesEngine} (Task 2) lands. It implements the
 * {@link GameEngine} contract with deliberately shallow behaviour: it lets you
 * draw cards, cycle turns and open every dialog so the board and dashboard can
 * be visually verified.
 *
 * <p>It is <b>not</b> a rules implementation — for example it cannot recolor a
 * claimed route because {@code domain.Route} exposes no owner mutator in the
 * contract (that is the engine's job). {@code app.Main} defaults to the real
 * engine; pass {@code stub} on the command line to select this one.</p>
 */
public final class DevStubEngine implements GameEngine {

    private final GameState state;

    public DevStubEngine(GameState state) {
        this.state = state;
        if (state.phase() == null) {
            state.setPhase(GamePhase.PLAYING);
        }
        state.setLastMessage("DEV STUB engine — UI preview only, rules are not enforced.");
    }

    @Override public GameState state() { return state; }
    @Override public int currentPlayerId() { return state.currentPlayerId(); }
    @Override public GamePhase phase() { return state.phase(); }
    @Override public boolean isGameOver() { return state.phase() == GamePhase.GAME_OVER; }

    @Override
    public ActionOutcome drawTrainCard(int faceUpIndex) {
        Player p = current();
        CardColor c;
        if (faceUpIndex >= 0 && state.deck().faceUp() != null
                && faceUpIndex < state.deck().faceUp().size()) {
            c = state.deck().faceUp().get(faceUpIndex);
            state.deck().refillFaceUp();
        } else {
            c = state.deck().draw();
        }
        if (c != null) {
            p.addCard(c);
        }
        state.setLastMessage(p.name() + " drew a " + UiColors.label(c) + " card.");
        return ActionOutcome.of(true, state.lastMessage());
    }

    @Override
    public ActionOutcome beginClaimRoute(String routeId, List<CardColor> cards) {
        // Stub cannot mutate route ownership; just acknowledge for UI flow.
        state.setLastMessage("(stub) claim of " + routeId + " noted; real engine assigns ownership.");
        return ActionOutcome.of(false, state.lastMessage());
    }

    @Override
    public ActionOutcome confirmTunnel(List<CardColor> extraCards) {
        return ActionOutcome.of(false, "(stub) tunnels are resolved by the real engine.");
    }

    @Override
    public ActionOutcome cancelTunnel() {
        return ActionOutcome.of(true, "(stub) tunnel cancelled.");
    }

    @Override
    public List<DestinationTicket> offerTickets() {
        List<DestinationTicket> out = new ArrayList<DestinationTicket>();
        for (int i = 0; i < GameConstants.START_TICKETS_DEALT; i++) {
            DestinationTicket t = state.deck().drawTicket();
            if (t != null) {
                out.add(t);
            }
        }
        return out;
    }

    @Override
    public ActionOutcome keepTickets(List<DestinationTicket> kept) {
        Player p = current();
        if (kept != null) {
            for (DestinationTicket t : kept) {
                p.addTicket(t);
            }
        }
        state.setLastMessage(p.name() + " kept " + (kept == null ? 0 : kept.size()) + " ticket(s).");
        return ActionOutcome.of(true, state.lastMessage());
    }

    @Override
    public ActionOutcome buildStation(String cityId, List<CardColor> cards) {
        return ActionOutcome.of(false, "(stub) stations are validated by the real engine.");
    }

    @Override
    public ActionOutcome endTurn() {
        int n = state.players().size();
        int next = (state.currentPlayerId() + 1) % n;
        state.setCurrentPlayerId(next);
        if (state.phase() == GamePhase.SETUP) {
            state.setPhase(GamePhase.PLAYING);
        }
        state.setLastMessage("It is now " + current().name() + "'s turn.");
        return ActionOutcome.of(true, state.lastMessage());
    }

    @Override
    public List<PlayerScore> finalScores() {
        return new ArrayList<PlayerScore>();
    }

    private Player current() {
        return state.player(state.currentPlayerId());
    }
}
