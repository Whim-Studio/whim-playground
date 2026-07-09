package com.whim.samurai.ui;

import com.whim.samurai.app.Game;
import com.whim.samurai.app.Screen;
import com.whim.samurai.engine.SaveManager;
import com.whim.samurai.render.Palette;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.geom.GeneralPath;

/**
 * Title screen — a woodblock-style splash over the shared washi-paper background
 * (design ref §7.1). Offers New Game, Continue (a saved game), How to Play and Quit,
 * plus a small save hook. Continue is enabled only when a save exists.
 */
public class MainMenuScreen extends Screen {

    private final JButton continueBtn;

    public MainMenuScreen(Game game) {
        super(game);
        setLayout(new GridBagLayout());

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JButton newGame  = UiKit.button("New Game");
        continueBtn      = UiKit.button("Continue");
        JButton howTo    = UiKit.button("How to Play");
        JButton quit     = UiKit.button("Quit");

        for (JButton b : new JButton[]{ newGame, continueBtn, howTo, quit }) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(280, 48));
            col.add(b);
            col.add(Box.createVerticalStrut(14));
        }

        newGame.addActionListener(e -> game.screens.show(Game.CREATE));
        continueBtn.addActionListener(e -> onContinue());
        howTo.addActionListener(e -> game.screens.show(Game.HELP));
        quit.addActionListener(e -> System.exit(0));

        // Leave room at the top for the painted title/emblem.
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.insets = new java.awt.Insets(210, 0, 0, 0);
        add(col, gc);

        Keys.bind(this, "N", () -> game.screens.show(Game.CREATE));
        Keys.bind(this, "H", () -> game.screens.show(Game.HELP));
    }

    public String name() { return Game.MENU; }

    @Override public void onShow() {
        continueBtn.setEnabled(SaveManager.saveExists());
        repaint();
    }

    /** Load the single save slot into the shared state, handling failure gracefully. */
    private void onContinue() {
        try {
            game.state = SaveManager.load();
            game.screens.show(Game.MAP);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "The saved scroll could not be read:\n" + ex.getMessage(),
                    "Save Damaged", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        UiKit.aa(g);
        UiKit.paperBackground(g, getWidth(), getHeight());

        int cx = getWidth() / 2;

        // Cinnabar "seal" emblem behind the title — a stylised sword (procedural art).
        drawSwordEmblem(g, cx, 70);

        g.setFont(UiKit.TITLE);
        String title = "Sword of the Samurai";
        int tw = g.getFontMetrics().stringWidth(title);
        // subtle ink shadow, then the title
        g.setColor(Palette.INK_SOFT);
        g.drawString(title, cx - tw / 2 + 2, 150 + 2);
        g.setColor(Palette.INK);
        g.drawString(title, cx - tw / 2, 150);

        g.setFont(UiKit.BODY);
        g.setColor(Palette.CINNABAR_DK);
        String sub = "—  Sengoku Jidai, the Age of the Country at War  —";
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, cx - sw / 2, 182);

        g.setFont(UiKit.SMALL);
        g.setColor(Palette.DIM);
        String foot = "Java 8 / Swing clean-room recreation — procedural art, no original assets";
        g.drawString(foot, cx - g.getFontMetrics().stringWidth(foot) / 2, getHeight() - 24);
    }

    /** A small procedurally-drawn katana crossing a cinnabar disc (placeholder emblem). */
    private void drawSwordEmblem(Graphics2D g, int cx, int cy) {
        g.setColor(Palette.CINNABAR);
        g.fillOval(cx - 34, cy - 34, 68, 68);
        g.setColor(Palette.PAPER);
        // blade
        GeneralPath blade = new GeneralPath();
        blade.moveTo(cx - 24, cy + 18);
        blade.lineTo(cx + 20, cy - 22);
        blade.lineTo(cx + 24, cy - 18);
        blade.lineTo(cx - 20, cy + 22);
        blade.closePath();
        g.fill(blade);
        // guard + hilt
        g.setColor(Palette.INK);
        g.fillRect(cx - 27, cy + 15, 10, 4);
        g.fillOval(cx - 30, cy + 16, 6, 6);
    }
}
