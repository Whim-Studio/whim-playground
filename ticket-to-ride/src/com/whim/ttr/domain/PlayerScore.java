package com.whim.ttr.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable final-scoring breakdown for one player, produced by the engine at
 * game end and rendered by the UI's results screen.
 */
public final class PlayerScore {

    private final int playerId;
    private final int routePoints;
    private final int ticketPoints;
    private final int stationBonus;
    private final boolean hasLongestPath;
    private final List<String> completedTickets;
    private final List<String> failedTickets;

    public PlayerScore(int playerId, int routePoints, int ticketPoints,
                       int stationBonus, boolean hasLongestPath,
                       List<String> completedTickets, List<String> failedTickets) {
        this.playerId = playerId;
        this.routePoints = routePoints;
        this.ticketPoints = ticketPoints;
        this.stationBonus = stationBonus;
        this.hasLongestPath = hasLongestPath;
        this.completedTickets = completedTickets == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(completedTickets));
        this.failedTickets = failedTickets == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(failedTickets));
    }

    public int playerId() { return playerId; }
    public int routePoints() { return routePoints; }
    public int ticketPoints() { return ticketPoints; }
    public int stationBonus() { return stationBonus; }
    public boolean hasLongestPath() { return hasLongestPath; }

    public int total() {
        int longest = hasLongestPath ? com.whim.ttr.api.GameConstants.LONGEST_PATH_BONUS : 0;
        return routePoints + ticketPoints + stationBonus + longest;
    }

    public List<String> completedTickets() { return completedTickets; }
    public List<String> failedTickets() { return failedTickets; }
}
