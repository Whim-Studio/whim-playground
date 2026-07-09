package com.whim.alganon.worldsim;

import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Background simulated faction war — the single-player substitute for Alganon's live
 * Towers/Keeps PvP ([Gap — my design]). Contested objectives drift control over time via
 * a lightweight influence random-walk; flips update the persistent war scores on the
 * model and surface as Faction chat chatter. Surfaced to the UI via FactionWarView.
 */
public final class FactionWarSim {

    private static final String[] OBJECTIVE_NAMES = {
            "Ironhold Tower", "Rimewatch Keep", "Ashen Bastion", "Verdant Spire"
    };
    private static final double DRIFT_INTERVAL = 12.0;   // seconds between drift steps [Gap]
    private static final double DRIFT_STEP = 0.18;       // influence moved per step [Gap]
    private static final double CONTEST_BAND = 0.25;     // |influence| below this = contested

    private final GameContext ctx;
    private final List<WarObjective> objectives = new ArrayList<WarObjective>();

    public FactionWarSim(GameContext ctx) {
        this.ctx = ctx;
    }

    public List<WarObjective> objectives() { return objectives; }

    /** Seed fresh objectives (called on new game if none were loaded). */
    public void seed() {
        objectives.clear();
        double[] start = {0.6, -0.5, -0.1, 0.2};
        for (int i = 0; i < OBJECTIVE_NAMES.length; i++) {
            double inf = start[i];
            objectives.add(new WarObjective(OBJECTIVE_NAMES[i], controlFor(inf), inf,
                    DRIFT_INTERVAL * (0.4 + ctx.rng().nextDouble())));
        }
    }

    public void reset() { objectives.clear(); }

    public void tick(GameModel model, double dt) {
        if (objectives.isEmpty()) return;
        boolean flipped = false;
        for (WarObjective o : objectives) {
            o.nextTick -= dt;
            if (o.nextTick <= 0) {
                o.nextTick = DRIFT_INTERVAL * (0.6 + ctx.rng().nextDouble() * 0.8);
                double delta = (ctx.rng().nextDouble() * 2 - 1) * DRIFT_STEP;
                o.influence = clamp(o.influence + delta, -1.0, 1.0);
                ControlState next = controlFor(o.influence);
                if (next != o.control) {
                    ControlState prev = o.control;
                    o.control = next;
                    flipped = true;
                    announce(o, prev);
                }
            }
        }
        if (flipped) recomputeScores(model);
    }

    private void announce(WarObjective o, ControlState prev) {
        String holder;
        if (o.control == ControlState.ASHARR) holder = "Asharr forces";
        else if (o.control == ControlState.KUJIX) holder = "Kujix forces";
        else holder = "no clear victor";
        ctx.log(ChatChannel.FACTION, o.name + " is now held by " + holder + ".");
    }

    private void recomputeScores(GameModel model) {
        int asharr = 0, kujix = 0;
        for (WarObjective o : objectives) {
            if (o.control == ControlState.ASHARR) asharr += 10;
            else if (o.control == ControlState.KUJIX) kujix += 10;
            else if (o.control == ControlState.CONTESTED) { asharr += 3; kujix += 3; }
        }
        model.setWarScores(asharr, kujix);
    }

    private static ControlState controlFor(double influence) {
        if (influence > CONTEST_BAND) return ControlState.ASHARR;
        if (influence < -CONTEST_BAND) return ControlState.KUJIX;
        return ControlState.CONTESTED;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
