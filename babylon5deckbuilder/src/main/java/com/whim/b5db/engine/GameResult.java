package com.whim.b5db.engine;

import com.whim.b5db.model.Faction;

import java.util.List;

/** Immutable outcome of a finished game, consumed by the balance report. */
public final class GameResult {

    private final int winnerIndex;
    private final Faction winnerFaction;
    private final int turns;
    private final int[] prestige;
    private final Faction[] factions;
    private final long seed;

    public GameResult(int winnerIndex, Faction winnerFaction, int turns,
                      int[] prestige, Faction[] factions, long seed) {
        this.winnerIndex = winnerIndex;
        this.winnerFaction = winnerFaction;
        this.turns = turns;
        this.prestige = prestige;
        this.factions = factions;
        this.seed = seed;
    }

    public int winnerIndex() { return winnerIndex; }
    public Faction winnerFaction() { return winnerFaction; }
    public int turns() { return turns; }
    public int[] prestige() { return prestige; }
    public Faction[] factions() { return factions; }
    public long seed() { return seed; }

    public static GameResult from(GameState state, int turns) {
        List<PlayerState> ps = state.players();
        int bestIdx = 0;
        for (int i = 1; i < ps.size(); i++) {
            if (ps.get(i).prestige() > ps.get(bestIdx).prestige()) {
                bestIdx = i;
            }
        }
        int[] prestige = new int[ps.size()];
        Faction[] factions = new Faction[ps.size()];
        for (int i = 0; i < ps.size(); i++) {
            prestige[i] = ps.get(i).prestige();
            factions[i] = ps.get(i).faction();
        }
        return new GameResult(bestIdx, ps.get(bestIdx).faction(), turns,
                prestige, factions, state.rng().seed());
    }
}
