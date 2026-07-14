package com.whim.necromunda.test;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.whim.necromunda.engine.setup.DemoSetup;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.ui.RosterEditorPanel;

/**
 * Milestone 3 — headless render smoke for the roster editor. Lays the panel out
 * without a window (recursive doLayout, since a JFrame can't instantiate under
 * true headless) and paints it to a PNG, asserting it drew visible content.
 */
public final class RosterUiSmoke {

    private static final int W = 760;
    private static final int H = 480;

    public static void main(String[] args) throws IOException {
        Assert a = new Assert();

        Gang gang = DemoSetup.gangA();
        RosterEditorPanel panel = new RosterEditorPanel(gang);
        layoutRecursively(panel, W, H);

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, W, H);
        panel.paint(g);
        g.dispose();

        File out = new File("out-render/roster-editor.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);

        double coverage = nonBackgroundCoverage(img);
        System.out.println("Rendered " + W + "x" + H + " roster editor to " + out.getPath());
        System.out.printf("Non-background coverage: %.1f%%%n", coverage * 100);

        a.section("Roster editor render");
        a.that("roster editor paints visible content", coverage > 0.05);
        a.finish();
    }

    /** Size a container and lay out its whole subtree without needing a peer. */
    private static void layoutRecursively(Component c, int w, int h) {
        c.setSize(w, h);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.doLayout();
            for (Component child : cont.getComponents()) {
                layoutRecursively(child, child.getWidth(), child.getHeight());
            }
        }
    }

    private static double nonBackgroundCoverage(BufferedImage img) {
        int bg = img.getRGB(1, 1);
        long differing = 0;
        long total = (long) img.getWidth() * img.getHeight();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (img.getRGB(x, y) != bg) {
                    differing++;
                }
            }
        }
        return (double) differing / (double) total;
    }
}
