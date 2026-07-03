package com.whim.cardwoven.engine;

import java.util.List;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.VictoryType;
import com.whim.cardwoven.api.Views.AttachmentView;
import com.whim.cardwoven.api.Views.BuildingView;
import com.whim.cardwoven.domain.GameState;
import com.whim.cardwoven.domain.PlayerState;

/**
 * After every turn, recomputes each player's 0..1 progress toward all five
 * victory types and pushes it back into the domain via
 * {@link PlayerState#setVictoryProgress}. A player wins when a victory type they
 * actually pursue (their faction's list) reaches full progress. Pursued
 * victories are faction-weighted: they need only a fraction of the base metric,
 * so each faction races down its own lane.
 */
final class VictoryMonitor {

    // Base metric targets for 100% progress on a NON-pursued lane.
    private static final double GOLD_TARGET = 80.0;
    private static final double KILL_TARGET = 6.0;
    private static final double BUILDING_TARGET = 8.0;
    private static final double FAITH_TARGET = 5.0;
    private static final double COMMAND_TARGET = 40.0;

    /** Pursued lanes need only this fraction of the base metric (faction weight). */
    private static final double PURSUED_DISCOUNT = 0.75;

    private final GameState state;
    private final EngineStats stats;

    VictoryMonitor(GameState state, EngineStats stats) {
        this.state = state;
        this.stats = stats;
    }

    /**
     * Recompute progress for every player and award victory if a pursued lane is
     * complete. Returns true if the game is now over.
     */
    boolean check() {
        List<PlayerState> players = state.playerStates();

        for (int i = 0; i < players.size(); i++) {
            PlayerState p = players.get(i);
            List<VictoryType> pursued = p.profile().pursuedVictories();

            VictoryType[] all = VictoryType.values();
            for (int v = 0; v < all.length; v++) {
                VictoryType type = all[v];
                boolean isPursued = pursued.contains(type);
                double metric = rawMetric(p, type);
                double target = baseTarget(type) * (isPursued ? PURSUED_DISCOUNT : 1.0);
                double progress = target <= 0 ? 0.0 : metric / target;
                if (progress > 1.0) {
                    progress = 1.0;
                }
                p.setVictoryProgress(type, progress);

                if (!state.isGameOver() && isPursued && progress >= 1.0) {
                    state.setWinner(p.index(), type);
                    state.log(p.name() + " achieves a " + type.display()
                            + " victory!");
                }
            }
        }
        return state.isGameOver();
    }

    private double rawMetric(PlayerState p, VictoryType type) {
        switch (type) {
            case ECONOMIC:
                return p.resource(ResourceType.GOLD);
            case MILITARY:
                return stats.militaryScore(p.index());
            case EXPANSION:
                return buildingCount(p);
            case FAITH:
                return faithScore(p);
            case DOMINANCE:
                return p.resource(ResourceType.COMMAND_POINTS);
            default:
                return 0.0;
        }
    }

    private static double baseTarget(VictoryType type) {
        switch (type) {
            case ECONOMIC:
                return GOLD_TARGET;
            case MILITARY:
                return KILL_TARGET;
            case EXPANSION:
                return BUILDING_TARGET;
            case FAITH:
                return FAITH_TARGET;
            case DOMINANCE:
                return COMMAND_TARGET;
            default:
                return Double.MAX_VALUE;
        }
    }

    private int buildingCount(PlayerState p) {
        return state.gridMap().buildingsOf(p.index()).size();
    }

    /** Temples plus Idol attachments — the material of a Faith victory. */
    private int faithScore(PlayerState p) {
        int score = 0;
        List<BuildingView> buildings = state.gridMap().buildingsOf(p.index());
        for (int i = 0; i < buildings.size(); i++) {
            BuildingView b = buildings.get(i);
            if (b.type() == BuildingType.TEMPLE) {
                score += 1;
            }
            List<AttachmentView> atts = b.attachments();
            for (int j = 0; j < atts.size(); j++) {
                if (atts.get(j).type() == AttachmentType.IDOL) {
                    score += 1;
                }
            }
        }
        return score;
    }
}
