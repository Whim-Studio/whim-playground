package com.whim.xcom.geo;

import com.whim.xcom.battle.BattleFactory;
import com.whim.xcom.battle.BattleGame;
import com.whim.xcom.battle.BattleOutcome;
import com.whim.xcom.battle.BattleSetup;
import com.whim.xcom.rules.Ruleset;

/**
 * The seam by which the Geoscape starts a ground assault. The interactive app
 * plugs in an implementation that switches to the Battlescape screen and returns
 * the {@link BattleOutcome} once the mission ends; headless callers use
 * {@link #autoResolve(Ruleset)}, which simulates the fight with no UI. This keeps
 * the Geoscape runnable and testable on its own.
 */
public interface MissionLauncher {

    BattleOutcome launch(BattleSetup setup);

    /** A headless launcher that auto-resolves the tactical mission. */
    static MissionLauncher autoResolve(final Ruleset ruleset) {
        return new MissionLauncher() {
            @Override public BattleOutcome launch(BattleSetup setup) {
                BattleGame game = BattleFactory.build(ruleset, setup);
                return game.autoResolve(80);
            }
        };
    }
}
