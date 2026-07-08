package com.whim.necromunda.test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.whim.necromunda.engine.GameState;
import com.whim.necromunda.engine.setup.DemoSetup;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.board.Board;
import com.whim.necromunda.ui.BoardPanel;

/**
 * Milestone 2 — headless render smoke. Paints the {@link BoardPanel} off-screen
 * to a PNG (no display needed) and asserts the board actually drew visible
 * content, so the {@code paintComponent} path is verified in CI-like conditions.
 */
public final class BoardRenderSmoke {

    private static final int SIZE = 650;
    private static final java.awt.Color SENTINEL = new java.awt.Color(0xFF, 0x00, 0xFF);

    public static void main(String[] args) throws IOException {
        Assert a = new Assert();

        Board board = DemoSetup.demoBoard();
        Gang gangA = DemoSetup.gangA();
        Gang gangB = DemoSetup.gangB();
        DemoSetup.placeGangs(board, gangA, gangB);
        GameState state = new GameState(board, gangA, gangB, 1L);

        BoardPanel panel = new BoardPanel(state);
        panel.setSize(SIZE, SIZE);
        panel.doLayout();
        panel.setSelected(gangA.roster().get(0));

        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // Pre-fill with a sentinel colour so "painted area" = pixels the panel
        // actually drew over.
        g.setColor(SENTINEL);
        g.fillRect(0, 0, SIZE, SIZE);
        panel.paint(g);
        g.dispose();

        File out = new File("out-render/board.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);

        double coverage = nonBackgroundCoverage(img);
        System.out.println("Rendered " + SIZE + "x" + SIZE + " board to " + out.getPath());
        System.out.printf("Non-background coverage: %.1f%%%n", coverage * 100);

        a.section("Board render");
        a.that("board paints visible content", coverage > 0.30);
        a.finish();
    }

    /** Fraction of pixels the panel actually painted over the sentinel fill. */
    private static double nonBackgroundCoverage(BufferedImage img) {
        int bg = SENTINEL.getRGB();
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
