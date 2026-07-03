package com.whim.cardwoven.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.cardwoven.api.ActionResult;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.TileView;
import com.whim.cardwoven.domain.Card;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.GridMap;
import com.whim.cardwoven.domain.PlayerState;

/**
 * Simple heuristic opponent. During its MAIN phase an AI player tries, in order,
 * to EXPAND (place buildings), ATTACH (buff its buildings), and spend ECONOMY
 * cards; during COMBAT it uses MILITARY cards to DEFEND/ATTACK the nearest
 * raiders or rival buildings. Every action is routed through the same validated
 * engine entry points a human would use, so the AI can never make an illegal
 * move — it just tries options and keeps whatever the engine accepts.
 */
final class AiPlayer {

    private final GameState state;
    private final GameEngine engine;

    AiPlayer(GameState state, GameEngine engine) {
        this.state = state;
        this.engine = engine;
    }

    /** EXPAND / ATTACH / ECONOMY / EXPLORE decisions. */
    void playMainPhase(PlayerState ai) {
        // Snapshot the hand: actions mutate it as cards are consumed.
        List<Card> hand = new ArrayList<Card>(ai.handCards());
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            CardType type = card.type();
            if (type == CardType.BUILDING) {
                tryExpand(ai, card);
            } else if (type == CardType.ATTACHMENT) {
                tryAttach(ai, card);
            } else if (type == CardType.ECONOMY) {
                engine.doPlayCard(ai, card.id(), 0, 0);
            } else if (type == CardType.EXPLORE) {
                tryExplore(ai, card);
            }
            // MILITARY handled in combat phase; SIN is dead weight, skipped.
        }
    }

    /** DEFEND / ATTACK decisions with MILITARY cards. */
    void playCombatPhase(PlayerState ai) {
        List<Card> hand = new ArrayList<Card>(ai.handCards());
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.type() != CardType.MILITARY) {
                continue;
            }
            int[] target = findCombatTarget(ai);
            if (target != null) {
                engine.doResolveCombat(ai, card.id(), target[0], target[1]);
            }
        }
    }

    private void tryExpand(PlayerState ai, Card card) {
        GridMap map = state.gridMap();
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                TileView tile = map.tile(r, c);
                if (tile.building() != null) {
                    continue;
                }
                ActionResult res = engine.doPlayBuilding(ai, card.id(), r, c);
                if (res.isSuccess()) {
                    return; // placed; done with this card
                }
            }
        }
    }

    private void tryAttach(PlayerState ai, Card card) {
        GridMap map = state.gridMap();
        List<BuildingView> mine = map.buildingsOf(ai.index());
        for (int i = 0; i < mine.size(); i++) {
            BuildingView b = mine.get(i);
            ActionResult res = engine.doAttach(ai, card.id(), b.id());
            if (res.isSuccess()) {
                return;
            }
        }
    }

    private void tryExplore(PlayerState ai, Card card) {
        GridMap map = state.gridMap();
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                if (!map.tile(r, c).explored()) {
                    ActionResult res = engine.doPlayCard(ai, card.id(), r, c);
                    if (res.isSuccess()) {
                        return;
                    }
                }
            }
        }
    }

    /** Nearest raider tile, else a rival building tile, else null. */
    private int[] findCombatTarget(PlayerState ai) {
        GridMap map = state.gridMap();
        int[] enemyBuilding = null;
        for (int r = 0; r < map.rows(); r++) {
            for (int c = 0; c < map.cols(); c++) {
                TileView tile = map.tile(r, c);
                if (tile.raiderStrength() > 0) {
                    return new int[] { r, c }; // raiders first — defend the realm
                }
                BuildingView b = tile.building();
                if (enemyBuilding == null && b != null
                        && b.ownerPlayerIndex() != ai.index()) {
                    enemyBuilding = new int[] { r, c };
                }
            }
        }
        return enemyBuilding;
    }
}
