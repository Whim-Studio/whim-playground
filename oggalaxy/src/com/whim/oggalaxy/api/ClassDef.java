package com.whim.oggalaxy.api;

import java.io.Serializable;

/**
 * Passive bonuses granted by a {@link Ids.PlayerClass}. Multipliers are expressed
 * as factors (1.0 == no change). These approximate the OGame "Discoverer / Collector
 * / General" class families adapted to OG Galaxy's Explorer / Miner / General.
 *
 * The exact OG Galaxy numbers are not publicly documented; these are clearly-labelled
 * reasonable approximations and are the single source of truth for the whole game.
 */
public final class ClassDef implements Serializable {

    private static final long serialVersionUID = 1L;

    public final Ids.PlayerClass type;
    public final String name;
    public final String description;

    public final double productionBonus;     // multiplier on metal/crystal/deut production
    public final double energyBonus;          // multiplier on energy production
    public final double fleetSpeedBonus;      // multiplier on ship speed (fewer flight ticks)
    public final double fuelDiscount;         // multiplier on fuel consumption (<1 == cheaper)
    public final double combatBonus;          // multiplier on ship attack in combat
    public final double expeditionBonus;      // multiplier on expedition find sizes
    public final double researchSpeedBonus;   // multiplier reducing research time (<1 == faster)

    public ClassDef(Ids.PlayerClass type, String name, String description,
                    double productionBonus, double energyBonus, double fleetSpeedBonus,
                    double fuelDiscount, double combatBonus, double expeditionBonus,
                    double researchSpeedBonus) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.productionBonus = productionBonus;
        this.energyBonus = energyBonus;
        this.fleetSpeedBonus = fleetSpeedBonus;
        this.fuelDiscount = fuelDiscount;
        this.combatBonus = combatBonus;
        this.expeditionBonus = expeditionBonus;
        this.researchSpeedBonus = researchSpeedBonus;
    }
}
