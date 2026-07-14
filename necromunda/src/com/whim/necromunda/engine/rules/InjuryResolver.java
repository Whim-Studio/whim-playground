package com.whim.necromunda.engine.rules;

import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterStatus;
import com.whim.necromunda.model.Stat;

/**
 * Resolves what happens when a wound "goes through" a fighter's save.
 *
 * <p>Multi-wound fighters absorb wounds until their last one is gone; only then
 * is an injury roll forced. The injury roll is classified into the three bands
 * and applied to the fighter's status / stats.
 */
public final class InjuryResolver {

    private InjuryResolver() {
    }

    /** Map a D6 to an injury band: 1-2 Flesh Wound, 3-4 Down, 5-6 Out of Action. */
    public static InjuryResult classify(int d6) {
        if (d6 <= 2) {
            return InjuryResult.FLESH_WOUND;
        }
        if (d6 <= 4) {
            return InjuryResult.DOWN;
        }
        return InjuryResult.OUT_OF_ACTION;
    }

    /**
     * Apply one wound that has already beaten the save.
     *
     * <p>If the fighter still has more than one wound remaining, the wound is
     * simply absorbed (a wound is deducted, no injury) and this returns
     * {@code null}. Otherwise the fighter is reduced to 0 wounds, the supplied
     * D6 is classified into an injury band, that band is applied, and the band is
     * returned.
     *
     * @param injuryD6 the D6 roll to use if an injury is forced
     * @return the injury band applied, or {@code null} if the wound was absorbed
     */
    public static InjuryResult applyWound(Fighter fighter, int injuryD6) {
        if (fighter.woundsRemaining() > 1) {
            fighter.setWoundsRemaining(fighter.woundsRemaining() - 1);
            return null;
        }
        fighter.setWoundsRemaining(0);
        InjuryResult result = classify(injuryD6);
        applyResult(fighter, result);
        return result;
    }

    /** Apply an already-decided injury band's consequences to the fighter. */
    public static void applyResult(Fighter fighter, InjuryResult result) {
        switch (result) {
            case FLESH_WOUND:
                // Stays up but weakened. Modelled as a WS knock in-battle; the
                // campaign "choose which stat" detail belongs to advancement.
                fighter.stats().modify(Stat.WS, -1);
                if (fighter.status() == FighterStatus.OUT_OF_ACTION
                        || fighter.status() == FighterStatus.DOWN) {
                    // don't upgrade a worse status
                    break;
                }
                fighter.setStatus(FighterStatus.ACTIVE);
                break;
            case DOWN:
                fighter.setStatus(FighterStatus.DOWN);
                break;
            case OUT_OF_ACTION:
                fighter.setStatus(FighterStatus.OUT_OF_ACTION);
                break;
            default:
                break;
        }
    }
}
