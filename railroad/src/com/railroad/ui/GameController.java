package com.railroad.ui;

import com.railroad.logic.GameClock;
import com.railroad.model.GameState;
import com.railroad.model.GridPoint;
import com.railroad.model.Town;

import java.util.List;

/**
 * Mediator between the Swing views and the model/logic. Views call these methods
 * for user actions; they never mutate the model directly. Holds transient UI
 * state (the selected tool) that is not part of the persistent game model.
 */
public final class GameController {

    private final GameState state;
    private final GameClock clock;
    private Tool currentTool = Tool.SELECT;

    private String statusMessage = "Select the Build Track tool, then drag between two towns.";

    public GameController(GameState state, GameClock clock) {
        this.state = state;
        this.clock = clock;
    }

    public GameState getState() {
        return state;
    }

    public GameClock getClock() {
        return clock;
    }

    public Tool getCurrentTool() {
        return currentTool;
    }

    public void setCurrentTool(Tool tool) {
        this.currentTool = tool;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /** Attempts to lay one segment; updates the status message with the result. */
    public boolean tryLayTrack(GridPoint a, GridPoint b) {
        if (currentTool != Tool.BUILD_TRACK) {
            return false;
        }
        int cost = state.segmentCost(a, b);
        boolean built = state.layTrack(a, b);
        if (built) {
            statusMessage = "Laid track " + a + "->" + b + " for $" + cost + ".";
        } else if (!state.getCompany().canAfford(cost)) {
            statusMessage = "Not enough cash: segment costs $" + cost + ".";
        }
        return built;
    }

    /**
     * Buys the single train, auto-selecting the first pair of connected towns.
     * Returns a human-readable outcome for the HUD.
     */
    public String buyTrainOnFirstConnectedRoute() {
        if (state.hasTrain()) {
            return "A train is already running.";
        }
        if (!state.getCompany().canAfford(GameState.TRAIN_COST)) {
            return "Not enough cash for a train ($" + GameState.TRAIN_COST + ").";
        }
        List<Town> towns = state.getWorld().getTowns();
        for (int i = 0; i < towns.size(); i++) {
            for (int j = i + 1; j < towns.size(); j++) {
                Town a = towns.get(i);
                Town b = towns.get(j);
                if (state.getNetwork().areTownsConnected(a, b)) {
                    if (state.buyTrain(a, b) != null) {
                        clock.start();
                        statusMessage = "Train running " + a.getName() + " <-> " + b.getName() + ".";
                        return statusMessage;
                    }
                }
            }
        }
        statusMessage = "No two towns are connected by track yet.";
        return statusMessage;
    }

    public void toggleRunning() {
        if (clock.isRunning()) {
            clock.pause();
        } else {
            clock.start();
        }
    }
}
