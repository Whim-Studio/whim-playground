package com.whim.xcom.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.whim.xcom.rules.Ruleset;

/**
 * The title / main-menu screen. All art is drawn procedurally with Java2D — no
 * original assets. Buttons are placeholders that Phase 1+ will wire to the
 * Geoscape/new-game flow.
 */
public final class MainMenuPanel extends JPanel {

    private final transient Ruleset ruleset;

    public MainMenuPanel(Ruleset ruleset,
                         ActionListener onNewGame,
                         ActionListener onOptions,
                         ActionListener onQuit) {
        this.ruleset = ruleset;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(900, 640));

        add(buildTitle(), BorderLayout.NORTH);
        add(buildButtons(onNewGame, onOptions, onQuit), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildTitle() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(48, 24, 8, 24));

        JLabel title = new JLabel("UFO: ENEMY UNKNOWN", SwingConstants.CENTER);
        title.setFont(new Font("Monospaced", Font.BOLD, 44));
        title.setForeground(new Color(120, 230, 140));

        JLabel sub = new JLabel("clean-room recreation — Phase 0 foundation", SwingConstants.CENTER);
        sub.setFont(new Font("Monospaced", Font.PLAIN, 16));
        sub.setForeground(new Color(150, 180, 160));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        p.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(8, 0, 0, 0);
        p.add(sub, c);
        return p;
    }

    private JPanel buildButtons(ActionListener onNewGame, ActionListener onOptions, ActionListener onQuit) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new javax.swing.BoxLayout(col, javax.swing.BoxLayout.Y_AXIS));

        col.add(menuButton("New Game", onNewGame));
        col.add(Box.createVerticalStrut(14));
        col.add(menuButton("Options", onOptions));
        col.add(Box.createVerticalStrut(14));
        col.add(menuButton("Quit", onQuit));

        p.add(col);
        return p;
    }

    private JButton menuButton(String text, ActionListener listener) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 20));
        b.setForeground(new Color(220, 240, 220));
        b.setBackground(new Color(24, 44, 32));
        b.setFocusPainted(false);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(280, 48));
        b.setPreferredSize(new Dimension(280, 48));
        b.setBorder(BorderFactory.createLineBorder(new Color(90, 160, 110), 2));
        if (listener != null) {
            b.addActionListener(listener);
        }
        return b;
    }

    private JLabel buildFooter() {
        JLabel footer = new JLabel(
                "Ruleset: " + ruleset.displayName()
                        + "   •   weapons=" + ruleset.weapons().size()
                        + "  aliens=" + ruleset.aliens().size()
                        + "  facilities=" + ruleset.facilities().size(),
                SwingConstants.CENTER);
        footer.setFont(new Font("Monospaced", Font.PLAIN, 13));
        footer.setForeground(new Color(120, 150, 130));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 18, 12));
        return footer;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        // Night-sky vertical gradient.
        g2.setPaint(new GradientPaint(0, 0, new Color(6, 10, 16), 0, h, new Color(2, 6, 10)));
        g2.fillRect(0, 0, w, h);

        // Procedural starfield (deterministic so it doesn't shimmer on repaint).
        g2.setColor(new Color(180, 200, 220, 160));
        long s = 0x1234_5678L;
        for (int i = 0; i < 140; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int x = (int) ((s >>> 33) % Math.max(1, w));
            s = s * 6364136223846793005L + 1442695040888963407L;
            int y = (int) ((s >>> 33) % Math.max(1, h));
            int size = ((int) (s >>> 20) & 1) + 1;
            g2.fillRect(x, y, size, size);
        }

        // A drawn "UFO" glow low on the horizon — placeholder art.
        int ux = w / 2;
        int uy = (int) (h * 0.78);
        g2.setPaint(new RadialGradientPaint(ux, uy, 120,
                new float[] {0f, 1f},
                new Color[] {new Color(40, 120, 70, 120), new Color(0, 0, 0, 0)}));
        g2.fillOval(ux - 120, uy - 120, 240, 240);
        g2.setColor(new Color(70, 180, 110));
        g2.fillOval(ux - 70, uy - 14, 140, 28);
        g2.setColor(new Color(120, 220, 150));
        g2.fillOval(ux - 26, uy - 30, 52, 30);

        g2.dispose();
    }
}
