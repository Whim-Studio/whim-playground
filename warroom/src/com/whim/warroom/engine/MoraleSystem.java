package com.whim.warroom.engine;

import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Unit;

import java.util.List;
import java.util.Map;

/**
 * Morale dynamics: decay under fire, on nearby friendly deaths, and while
 * outnumbered; slow recovery when safe. Crossing the panic threshold routs the
 * unit (sticky) so it flees and can no longer fight — which feeds the engine's
 * side-defeated ("all dead or routed") check.
 */
final class MoraleSystem {

    static final double DAMAGE_MORALE_K = 0.6;    // morale lost per HP of damage taken
    static final double FRIENDLY_DEATH_RADIUS = 120.0;
    static final double FRIENDLY_DEATH_PENALTY = 10.0;
    static final double THREAT_RADIUS = 200.0;
    static final double OUTNUMBER_RATE = 0.4;     // per surplus enemy per tick
    static final double RECOVERY_RATE = 0.08;     // per tick when safe
    static final double PANIC_THRESHOLD = 25.0;

    /**
     * @param damageThisTick id -> HP damage the unit took this tick
     * @param deadThisTick   units that transitioned to dead this tick (pos/faction valid)
     */
    void update(List<Unit> units, MapState map, int tick,
                Map<Integer, Double> damageThisTick,
                List<Unit> deadThisTick) {

        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.isAlive()) {
                continue;
            }
            double morale = u.getMorale();
            boolean threatened = false;

            // 1. Damage taken this tick.
            Double dmg = damageThisTick.get(u.getId());
            double dmgTaken = (dmg == null) ? 0.0 : dmg.doubleValue();
            if (dmgTaken > 0) {
                morale -= dmgTaken * DAMAGE_MORALE_K;
                threatened = true;
            }

            // 2. Nearby friendly deaths this tick.
            for (int d = 0; d < deadThisTick.size(); d++) {
                Unit dead = deadThisTick.get(d);
                if (dead.getFaction() == u.getFaction()
                        && u.getPos().dist(dead.getPos()) <= FRIENDLY_DEATH_RADIUS) {
                    morale -= FRIENDLY_DEATH_PENALTY;
                }
            }

            // 3. Outnumbered within threat radius.
            int enemies = 0;
            int friendlies = 0; // includes self
            for (int j = 0; j < units.size(); j++) {
                Unit o = units.get(j);
                if (!o.isAlive()) {
                    continue;
                }
                if (u.getPos().dist(o.getPos()) > THREAT_RADIUS) {
                    continue;
                }
                if (o.getFaction() == u.getFaction()) {
                    friendlies++;
                } else if (o.getFaction() == u.getFaction().enemyOf()) {
                    enemies++;
                }
            }
            if (enemies > 0) {
                threatened = true;
            }
            if (enemies > friendlies) {
                morale -= (enemies - friendlies) * OUTNUMBER_RATE;
            }

            // 4. Slow recovery when safe.
            if (!threatened) {
                morale += RECOVERY_RATE;
            }

            double max = u.getType().getMaxMorale();
            if (morale < 0) morale = 0;
            if (morale > max) morale = max;
            u.setMorale(morale);

            // 5. Panic -> rout (sticky).
            if (morale < PANIC_THRESHOLD) {
                u.setRouted(true);
            }
        }
    }
}
