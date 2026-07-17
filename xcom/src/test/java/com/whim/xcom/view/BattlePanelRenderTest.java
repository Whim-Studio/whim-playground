package com.whim.xcom.view;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import org.junit.Test;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.app.NoopAudioManager;
import com.whim.xcom.battle.BattleFactory;
import com.whim.xcom.battle.BattleGame;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Headless smoke test: build the Battlescape screen and paint it to an offscreen
 * image. No display/Xvfb is required, so this proves the render + layout wiring
 * doesn't throw and produces non-blank output on a build machine.
 */
public class BattlePanelRenderTest {

    @Test
    public void battlePanelRendersOffscreen() {
        GameContext ctx = new GameContext(Ruleset1994.load(), new SeededRng(1L), new NoopAudioManager());
        BattleGame game = BattleFactory.defaultSkirmish(ctx.ruleset(), 1L);
        BattlePanel panel = new BattlePanel(ctx, game, null);
        panel.setSize(900, 660);
        panel.validate();

        BufferedImage img = new BufferedImage(900, 660, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        panel.printAll(g);
        g.dispose();

        boolean painted = false;
        for (int x = 0; x < 900 && !painted; x += 11) {
            for (int y = 0; y < 660; y += 11) {
                if ((img.getRGB(x, y) & 0x00FFFFFF) != 0) {
                    painted = true;
                    break;
                }
            }
        }
        assertTrue("battle screen should paint visible content", painted);
    }
}
