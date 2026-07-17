package com.whim.xcom.geo.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.whim.xcom.app.GameContext;
import com.whim.xcom.geo.GeoClock;
import com.whim.xcom.geo.GeoFactory;
import com.whim.xcom.geo.GeoGame;
import com.whim.xcom.geo.Interceptor;
import com.whim.xcom.geo.Ufo;

/**
 * The Geoscape screen: a drawn world map with the base, radar coverage, UFOs and
 * interceptors, plus the six 1994 time-compression controls, a funds/score
 * readout and an event log. A {@link Timer} advances the {@link GeoGame} on the
 * EDT; clicking a detected UFO scrambles an interceptor. When a UFO is shot down
 * the screen delegates the ground assault to an {@link AssaultHandler}.
 */
public final class GeoScreen extends JPanel {

    /** How a crash/landing site becomes a tactical mission (app-supplied). */
    public interface AssaultHandler {
        void assault(GeoGame game, Ufo ufo);
        /** Launch the two-stage Cydonia final assault (surface then alien base). */
        void cydonia(GeoGame game);
    }

    private final transient GameContext ctx;
    private final transient GeoGame game;
    private final transient AssaultHandler assault;

    private final WorldCanvas canvas = new WorldCanvas();
    private final JTextArea log = new JTextArea();
    private final JLabel clockLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel(" ");
    private final JButton cydoniaBtn = pixelButton("Cydonia or Bust!");
    private final Timer timer;
    private boolean endShown;

    public GeoScreen(GameContext ctx, long seed, AssaultHandler assault, Runnable onExit) {
        this.ctx = ctx;
        this.assault = assault;
        this.game = GeoFactory.defaultCampaign(ctx.ruleset(), seed);
        setLayout(new BorderLayout());
        setBackground(new Color(4, 8, 14));

        add(buildTopBar(onExit), BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        game.setListener(new GeoGame.Listener() {
            @Override public void onEvent(String message) {
                log.append(message + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
            @Override public void onChanged() {
                refresh();
            }
            @Override public void onCrashSite(final Ufo ufo) {
                game.clock().setSpeed(GeoClock.Speed.PAUSE);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        if (GeoScreen.this.assault != null) {
                            GeoScreen.this.assault.assault(game, ufo);
                        }
                    }
                });
            }
            @Override public void onVictory(String message) { endScreen("VICTORY", message); }
            @Override public void onDefeat(String message) { endScreen("DEFEAT", message); }
        });

        timer = new Timer(80, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                game.tick();
            }
        });
        timer.start();
        refresh();
    }

    /** Open the base management screen (research/manufacturing/roster/save-load). */
    private void openBase() {
        boolean wasRunning = timer.isRunning();
        timer.stop();
        java.awt.Window owner = SwingUtilities.getWindowAncestor(this);
        final javax.swing.JDialog dialog = new javax.swing.JDialog(owner, "Base",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(new BaseScreen(ctx, game));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true); // blocks until closed
        if (wasRunning) {
            timer.start();
        }
        refresh();
    }

    /** Launch the Cydonia final assault via the app-supplied handler. */
    private void launchCydonia() {
        game.clock().setSpeed(GeoClock.Speed.PAUSE);
        if (assault != null) {
            assault.cydonia(game);
        }
    }

    /** Show the terminal win/lose screen once, and stop the clock. */
    private void endScreen(final String title, final String message) {
        if (endShown) {
            return;
        }
        endShown = true;
        timer.stop();
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                JOptionPane.showMessageDialog(GeoScreen.this,
                        message, "X-COM — " + title,
                        "VICTORY".equals(title)
                                ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /** Stop the clock (used when leaving the screen or entering a battle). */
    public void suspend() {
        timer.stop();
    }

    /** Resume the clock after returning from a battle. */
    public void resume() {
        timer.start();
    }

    public GeoGame game() {
        return game;
    }

    // ---- UI -----------------------------------------------------------------

    private JComponent buildTopBar(final Runnable onExit) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(14, 22, 30));
        clockLabel.setForeground(new Color(150, 210, 240));
        clockLabel.setFont(new Font("Monospaced", Font.BOLD, 15));
        clockLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        bar.add(clockLabel, BorderLayout.WEST);

        JPanel speeds = new JPanel();
        speeds.setOpaque(false);
        for (final GeoClock.Speed s : GeoClock.Speed.values()) {
            JButton b = pixelButton(s.label());
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    game.clock().setSpeed(s);
                    refresh();
                }
            });
            speeds.add(b);
        }
        bar.add(speeds, BorderLayout.CENTER);

        JButton baseBtn = pixelButton("Base");
        baseBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { openBase(); }
        });
        cydoniaBtn.setBackground(new Color(70, 20, 30));
        cydoniaBtn.setForeground(new Color(255, 210, 120));
        cydoniaBtn.setVisible(false);
        cydoniaBtn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { launchCydonia(); }
        });
        JButton exit = pixelButton("Menu");
        exit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                suspend();
                if (onExit != null) {
                    onExit.run();
                }
            }
        });
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(cydoniaBtn);
        right.add(baseBtn);
        right.add(exit);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(new Color(10, 16, 22));
        side.setPreferredSize(new Dimension(250, 100));

        statsLabel.setForeground(new Color(180, 220, 200));
        statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statsLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        side.add(statsLabel, BorderLayout.NORTH);

        log.setEditable(false);
        log.setBackground(new Color(6, 10, 14));
        log.setForeground(new Color(160, 200, 210));
        log.setFont(new Font("Monospaced", Font.PLAIN, 11));
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(log);
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 80, 90)), "Geoscape Log"));
        side.add(sp, BorderLayout.CENTER);

        JLabel help = new JLabel("<html>Click a red UFO to scramble an interceptor.</html>");
        help.setForeground(new Color(140, 170, 180));
        help.setFont(new Font("Monospaced", Font.PLAIN, 11));
        help.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        side.add(help, BorderLayout.SOUTH);
        return side;
    }

    private JButton pixelButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Monospaced", Font.BOLD, 11));
        b.setForeground(new Color(220, 235, 240));
        b.setBackground(new Color(20, 36, 46));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(70, 120, 140), 1));
        return b;
    }

    private void refresh() {
        clockLabel.setText(game.clock().display()
                + "    Speed: " + game.clock().speed().label());
        statsLabel.setText(String.format(
                "<html>Funds: $%,d<br>Score: %d<br>Live aliens: %d<br>UFOs tracked: %d"
                        + "<br>Interceptors: %d<br>Nations: %d</html>",
                game.funds(), game.totalScore(), game.liveAlienCount(), game.ufos().size(),
                game.interceptors().size(), game.nations().size()));
        cydoniaBtn.setVisible(game.cydoniaAvailable());
        canvas.repaint();
    }

    // ---- world rendering ----------------------------------------------------

    private final class WorldCanvas extends JComponent {

        WorldCanvas() {
            setPreferredSize(new Dimension(660, 440));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    Ufo hit = ufoAt(e.getX(), e.getY());
                    if (hit != null) {
                        game.intercept(hit);
                    }
                }
            });
        }

        private int px(double x) { return (int) Math.round(x * getWidth()); }
        private int py(double y) { return (int) Math.round(y * getHeight()); }

        private Ufo ufoAt(int mx, int my) {
            Ufo best = null;
            double bestD = 16;
            for (Ufo u : game.ufos()) {
                if (!u.detected() || !u.active()) {
                    continue;
                }
                double d = Math.hypot(px(u.x()) - mx, py(u.y()) - my);
                if (d < bestD) {
                    bestD = d;
                    best = u;
                }
            }
            return best;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Ocean.
            g2.setColor(new Color(12, 30, 58));
            g2.fillRect(0, 0, w, h);
            // Lat/long grid.
            g2.setColor(new Color(30, 55, 90));
            for (int i = 1; i < 12; i++) {
                g2.drawLine(w * i / 12, 0, w * i / 12, h);
            }
            for (int i = 1; i < 6; i++) {
                g2.drawLine(0, h * i / 6, w, h * i / 6);
            }
            // Rough drawn continents (placeholder land masses).
            g2.setColor(new Color(38, 74, 46));
            for (double[][] blob : CONTINENTS) {
                Polygon p = new Polygon();
                for (double[] pt : blob) {
                    p.addPoint(px(pt[0]), py(pt[1]));
                }
                g2.fillPolygon(p);
            }

            // Radar coverage.
            double rr = game.base().radarRangeNorm();
            int rx = px(game.base().x());
            int ry = py(game.base().y());
            int rw = (int) Math.round(rr * w);
            int rh = (int) Math.round(rr * h);
            g2.setColor(new Color(80, 200, 160, 40));
            g2.fillOval(rx - rw, ry - rh, rw * 2, rh * 2);
            g2.setColor(new Color(90, 220, 170, 120));
            g2.drawOval(rx - rw, ry - rh, rw * 2, rh * 2);

            // Base marker.
            g2.setColor(new Color(120, 240, 160));
            g2.fillRect(rx - 5, ry - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.drawString(game.base().name(), rx + 8, ry + 4);

            // Interceptors.
            for (Interceptor c : game.interceptors()) {
                int ix = px(c.x());
                int iy = py(c.y());
                g2.setColor(c.ready() ? new Color(120, 180, 240) : new Color(220, 230, 120));
                g2.fillOval(ix - 3, iy - 3, 6, 6);
                if (c.status() == Interceptor.Status.PURSUING && c.target() != null) {
                    g2.setColor(new Color(220, 230, 120, 120));
                    g2.drawLine(ix, iy, px(c.target().x()), py(c.target().y()));
                }
            }

            // UFOs (only detected ones are shown).
            for (Ufo u : game.ufos()) {
                if (!u.detected() || !u.active()) {
                    continue;
                }
                int ux = px(u.x());
                int uy = py(u.y());
                g2.setColor(new Color(240, 70, 70));
                g2.fillOval(ux - 5, uy - 5, 10, 10);
                g2.setColor(new Color(255, 160, 160));
                g2.drawOval(ux - 7, uy - 7, 14, 14);
                g2.setColor(new Color(255, 200, 200));
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2.drawString(u.def().name(), ux + 8, uy + 3);
            }
        }
    }

    /** Very rough continent silhouettes (normalised coords) — placeholder art only. */
    private static final double[][][] CONTINENTS = {
        // North America
        {{0.08, 0.20}, {0.24, 0.16}, {0.28, 0.34}, {0.18, 0.46}, {0.10, 0.40}},
        // South America
        {{0.22, 0.52}, {0.30, 0.54}, {0.28, 0.82}, {0.22, 0.86}, {0.20, 0.64}},
        // Europe/Africa
        {{0.46, 0.20}, {0.58, 0.22}, {0.60, 0.60}, {0.50, 0.82}, {0.44, 0.50}, {0.48, 0.30}},
        // Asia
        {{0.60, 0.18}, {0.86, 0.16}, {0.92, 0.40}, {0.72, 0.44}, {0.62, 0.34}},
        // Australia
        {{0.80, 0.62}, {0.92, 0.62}, {0.92, 0.76}, {0.80, 0.76}},
    };
}
