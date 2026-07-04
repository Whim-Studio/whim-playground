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
        if (w.phase != Enums.Phase.PLAYING) return false;
        if (!w.player.alive || w.player.ninjutsu <= 0) return false;
        w.player.ninjutsu--;
        w.phase = Enums.Phase.NINJUTSU;
        w.ninjutsuFlash = 0.0;
        return true;
    }

    /** Advance the flash animation and clear on-screen enemies while active. */
    static void update(WorldState w) {
        if (w.phase != Enums.Phase.NINJUTSU) return;

        clearOnScreenEnemies(w);

        w.ninjutsuFlash += INC;
        if (w.ninjutsuFlash >= 1.0) {
            w.ninjutsuFlash = -1.0;
            w.phase = Enums.Phase.PLAYING;
        }
    }

    private static void clearOnScreenEnemies(WorldState w) {
        double viewL = w.cameraX;
        double viewR = w.cameraX + Config.VIEW_W;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (!e.alive) continue;
            if (e.box.right() >= viewL && e.box.x <= viewR) {
                e.alive = false;
                e.state = Enums.EntityState.DEAD;
                e.blocking = false;
                w.player.score += (e.type == Enums.EnemyType.NINJA) ? 200 : 100;
            }
        }
    }
}
