package com.whim.shinobi.engine;

import com.whim.shinobi.api.Config;
import com.whim.shinobi.api.Enums;
import com.whim.shinobi.domain.Enemy;
import com.whim.shinobi.domain.WorldState;

/**
 * Screen-clearing Ninjutsu magic. Triggering consumes one charge, enters
 * {@link Enums.Phase#NINJUTSU}, and drives {@code ninjutsuFlash} from 0→1 over a
 * short window while destroying every on-screen enemy, then returns to
 * {@link Enums.Phase#PLAYING}. Purely tick-driven; the UI reads {@code ninjutsuFlash}
 * for the full-screen flash.
 */
final class NinjutsuSystem {
    static final int FLASH_TICKS = 42;
    private static final double INC = 1.0 / FLASH_TICKS;

    private NinjutsuSystem() {}

    /** Begin the effect if a charge is available and play is normal. Returns true. */
    static boolean trigger(WorldState w) {
        if (w.phase() != Enums.Phase.PLAYING) return false;
        if (!w.playerEntity().alive() || w.playerEntity().ninjutsu() <= 0) return false;
        w.playerEntity().useNinjutsu();
        w.setPhase(Enums.Phase.NINJUTSU);
        w.setNinjutsuFlash(0.0);
        return true;
    }

    /** Advance the flash animation and clear on-screen enemies while active. */
    static void update(WorldState w) {
        if (w.phase() != Enums.Phase.NINJUTSU) return;

        clearOnScreenEnemies(w);

        w.setNinjutsuFlash(w.ninjutsuFlash() + INC);
        if (w.ninjutsuFlash() >= 1.0) {
            w.setNinjutsuFlash(-1.0);
            w.setPhase(Enums.Phase.PLAYING);
        }
    }

    private static void clearOnScreenEnemies(WorldState w) {
        double viewL = w.cameraX();
        double viewR = w.cameraX() + Config.VIEW_W;
        for (int i = 0; i < w.enemyList().size(); i++) {
            Enemy e = w.enemyList().get(i);
            if (!e.alive()) continue;
            if (e.box().right() >= viewL && e.box().x() <= viewR) {
                e.setAlive(false);
                e.setState(Enums.EntityState.DEAD);
                e.setBlocking(false);
                w.playerEntity().addScore((e.type() == Enums.EnemyType.NINJA) ? 200 : 100);
            }
        }
    }
}
