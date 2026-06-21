package com.whim.civ.engine;

import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.TechType;

import java.util.ArrayList;
import java.util.List;

/**
 * Research progression over the cascading tech tree.
 */
public final class ResearchEngine {

    public ResearchEngine() {
    }

    /**
     * Add this turn's science to the civ. If accumulated beakers reach the current tech's
     * base cost, learn it, clear the beaker pool (overflow is discarded, Civ1-style), and
     * auto-pick the next researchable tech.
     */
    public void advance(GameState s, Civilization civ, int scienceThisTurn) {
        TechType current = civ.getResearching();
        if (current == null) {
            current = pickNext(civ);
            civ.setResearching(current);
            if (current == null) {
                return; // nothing left to research
            }
        }

        int beakers = civ.getResearchBeakers() + Math.max(0, scienceThisTurn);
        if (beakers >= current.getBaseCost()) {
            civ.getKnownTechs().add(current);
            civ.setResearchBeakers(0);
            civ.setResearching(pickNext(civ));
        } else {
            civ.setResearchBeakers(beakers);
        }
    }

    /** A tech is researchable iff it is not yet known and all of its prereqs are known. */
    public List<TechType> researchable(Civilization civ) {
        List<TechType> out = new ArrayList<TechType>();
        for (TechType t : TechType.values()) {
            if (civ.knows(t)) {
                continue;
            }
            boolean prereqsMet = true;
            for (TechType p : t.getPrereqs()) {
                if (!civ.knows(p)) {
                    prereqsMet = false;
                    break;
                }
            }
            if (prereqsMet) {
                out.add(t);
            }
        }
        return out;
    }

    /** Auto-pick the cheapest researchable tech (deterministic); null if none remain. */
    private TechType pickNext(Civilization civ) {
        TechType best = null;
        for (TechType t : researchable(civ)) {
            if (best == null || t.getBaseCost() < best.getBaseCost()) {
                best = t;
            }
        }
        return best;
    }
}
