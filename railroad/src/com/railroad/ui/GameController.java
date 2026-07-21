package com.railroad.ui;

import com.railroad.logic.GameClock;
import com.railroad.model.Economy;
import com.railroad.model.GameState;
import com.railroad.model.GridPoint;
import com.railroad.model.Station;
import com.railroad.model.Town;
import com.railroad.model.Train;

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
    private Train selectedTrain; // train whose load the HUD shows

    private String statusMessage = "Build Track between two towns, add Stations, then Buy Train.";

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
     * Attempts to build a station at {@code p} (town tile or adjacent). Updates
     * the status message with the outcome.
     */
    public boolean tryBuildStation(GridPoint p) {
        if (currentTool != Tool.BUILD_STATION) {
            return false;
        }
        if (state.stationAt(p) != null) {
            statusMessage = "A station already stands here.";
            return false;
        }
        if (!state.isTownOrAdjacent(p)) {
            statusMessage = "Stations must sit on or next to a town.";
            return false;
        }
        if (!state.getCompany().canAfford(Economy.STATION_COST)) {
            statusMessage = "Not enough cash for a station ($" + Economy.STATION_COST + ").";
            return false;
        }
        Station s = state.buildStation(p);
        if (s != null) {
            statusMessage = s.getName() + " built at " + p + " serving "
                    + s.getServedTowns().size() + " town(s), "
                    + s.getServedIndustries().size() + " industry(ies).";
            return true;
        }
        statusMessage = "Could not build a station there.";
        return false;
    }

    /** The train whose load the HUD shows: the explicit selection, else the first train. */
    public Train getSelectedTrain() {
        if (selectedTrain != null) {
            return selectedTrain;
        }
        List<Train> trains = state.getTrains();
        return trains.isEmpty() ? null : trains.get(0);
    }

    /**
     * With the Select tool, picks the train nearest {@code p} (within one tile)
     * so the HUD shows its load.
     */
    public void selectTrainNear(GridPoint p) {
        Train nearest = null;
        double best = 1.5; // within ~one tile
        for (Train t : state.getTrains()) {
            List<GridPoint> path = t.getRoute().getPath();
            int i = (int) Math.floor(t.getPosition());
            if (i < 0) {
                i = 0;
            }
            if (i >= path.size()) {
                i = path.size() - 1;
            }
            GridPoint at = path.get(i);
            double d = Math.hypot(at.x - p.x, at.y - p.y);
            if (d <= best) {
                best = d;
                nearest = t;
            }
        }
        if (nearest != null) {
            selectedTrain = nearest;
            statusMessage = "Selected " + nearest.getName() + ".";
        }
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
