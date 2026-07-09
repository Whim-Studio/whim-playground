package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.FamilyEngine;
import com.whim.samurai.engine.HonorEngine;
import com.whim.samurai.render.Palette;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * End-of-game screen (design ref §8) — shown on death, defeat or victory.
 *
 * <p>If the head of the family died leaving an eligible heir, the dynasty can go on:
 * "Continue as your heir" promotes the son via {@link FamilyEngine#succeed} and returns
 * to the map. Otherwise (died heir-less, or a victorious run named Shogun) the final
 * "Verdict of History" dynasty score is shown via {@link HonorEngine#dynastyScore}, with
 * options to start again or quit.</p>
 */
public class GameOverScreen extends Screen {

    private final JButton continueBtn;
    private final JButton newGameBtn;
    private final JButton quitBtn;

    private boolean canContinue;   // died with an heir → dynasty may continue
    private long finalScore;

    public GameOverScreen(Game game) {
        super(game);
        setLayout(null);

        continueBtn = UiKit.button("Continue as your Heir");
        continueBtn.setBounds(60, 430, 280, 46);
        continueBtn.addActionListener(e -> {
            if (FamilyEngine.succeed(game)) game.screens.show(Game.MAP);
            else onShow();   // heir vanished — fall back to final score
        });
        add(continueBtn);

        newGameBtn = UiKit.button("New Game");
        newGameBtn.setBounds(60, 430, 200, 46);
        newGameBtn.addActionListener(e -> game.screens.show(Game.MENU));
        add(newGameBtn);

        quitBtn = UiKit.button("Quit");
        quitBtn.setBounds(280, 430, 160, 46);
        quitBtn.addActionListener(e -> System.exit(0));
        add(quitBtn);
    }

    public String name() { return Game.GAMEOVER; }

    @Override public void onShow() {
        boolean victory = game.state != null && game.state.victory;
        // A death (not a victory) with a living eligible heir lets the line continue
        // (design ref §4.4 / §5.4). Victory always goes to the final Verdict of History.
        canContinue = !victory && FamilyEngine.hasHeir(game);
        finalScore = HonorEngine.dynastyScore(game.state);

        continueBtn.setVisible(canContinue);
        newGameBtn.setVisible(!canContinue);
        quitBtn.setVisible(!canContinue);
        // When continuing, also offer a way out beneath the heir button.
        if (canContinue) {
            newGameBtn.setVisible(true);
            newGameBtn.setBounds(360, 430, 180, 46);
            quitBtn.setVisible(true);
            quitBtn.setBounds(360, 486, 180, 46);
            continueBtn.setBounds(60, 430, 280, 46);
        } else {
            newGameBtn.setBounds(60, 430, 200, 46);
            quitBtn.setBounds(280, 430, 160, 46);
        }
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        boolean victory = game.state != null && game.state.victory;
        String reason = (game.state != null) ? game.state.gameOverReason : "";

        // Title
        g.setFont(UiKit.TITLE);
        if (victory) {
            g.setColor(Palette.GOLD);
            g.drawString("Shogun of All Japan", 60, 90);
        } else if (canContinue) {
            g.setColor(Palette.INK);
            g.drawString("The Lord has Fallen", 60, 90);
        } else {
            g.setColor(Palette.CINNABAR_DK);
            g.drawString("The Dynasty Ends", 60, 90);
        }

        int x = 62, y = 130;
        g.setFont(UiKit.BODY);
        g.setColor(Palette.INK_SOFT);
        if (reason != null && !reason.isEmpty()) {
            for (String line : wrap(g, reason, getWidth() - 130)) {
                g.drawString(line, x, y);
                y += 24;
            }
        }
        y += 14;

        if (canContinue) {
            g.setFont(UiKit.HEAD);
            g.setColor(Palette.JADE);
            String heirName = game.state.player.heir().name;
            g.drawString("Your heir, " + heirName + ", stands ready to take up the sword.", x, y);
            y += 30;
            g.setFont(UiKit.BODY);
            g.setColor(Palette.INK);
            g.drawString("He inherits your name and lands, though his own strength must still be proven.", x, y);
        } else {
            // Final "Verdict of History" score (design ref §8.3).
            g.setFont(UiKit.HEAD);
            g.setColor(Palette.CINNABAR_DK);
            g.drawString("The Verdict of History", x, y);
            y += 34;
            g.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 30));
            g.setColor(Palette.GOLD);
            g.drawString("Dynasty Score: " + finalScore, x, y);
            y += 36;
            g.setFont(UiKit.BODY);
            g.setColor(Palette.INK);
            for (String line : wrap(g, HonorEngine.dynastyVerdict(finalScore), getWidth() - 130)) {
                g.drawString(line, x, y);
                y += 24;
            }
            g.setFont(UiKit.SMALL);
            g.setColor(Palette.DIM);
            g.drawString("(scoring is an approximation of the manual's seven-factor Verdict — design ref §8.3)",
                    x, y + 8);
        }
    }

    /** Naive word-wrap to a pixel width for painted paragraphs. */
    private java.util.List<String> wrap(Graphics2D g, String text, int maxW) {
        java.util.List<String> lines = new java.util.ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String trial = cur.length() == 0 ? word : cur + " " + word;
            if (g.getFontMetrics().stringWidth(trial) > maxW && cur.length() > 0) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(trial);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }
}
