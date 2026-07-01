package com.whim.warroom.ui;

import com.whim.warroom.domain.SimEngine;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * SOUTH transport bar. Play (→ Battle Mode + engine.play()), Pause,
 * Fast-Forward (cycles speed), a scrub timeline that seeks across
 * {@code 0..getMaxSimTick()}, Back-to-Editor (reset), and a Cinema Mode toggle
 * that hides the editor + this bar for a clean view. All state is read from the
 * engine via {@link #syncFromEngine()}; the bar never computes game rules.
 */
public final class PlaybackBar extends JPanel {

    private final SandboxController ctl;
    private WarRoomFrame frame;

    private final JButton playBtn = new JButton("▶ Play");
    private final JButton pauseBtn = new JButton("❙❙ Pause");
    private final JButton ffBtn = new JButton("⏩ x1");
    private final JButton editBtn = new JButton("◀ Editor");
    private final JButton cinemaBtn = new JButton("🎬 Cinema");
    private final JSlider timeline = new JSlider(0, 1, 0);
    private final JLabel tickLbl = new JLabel("tick 0 / 0");

    private boolean syncing; // guards slider feedback loop

    public PlaybackBar(SandboxController ctl) {
        this.ctl = ctl;
        setBackground(ThemeUI.BG_PANEL);
        setBorder(new EmptyBorder(6, 8, 6, 8));
        setLayout(new BorderLayout(8, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        style(playBtn); style(pauseBtn); style(ffBtn); style(editBtn); style(cinemaBtn);
        left.add(playBtn); left.add(pauseBtn); left.add(ffBtn); left.add(editBtn);
        add(left, BorderLayout.WEST);

        timeline.setOpaque(false);
        timeline.setEnabled(false);
        add(timeline, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        tickLbl.setForeground(ThemeUI.TEXT_DIM);
        tickLbl.setFont(ThemeUI.MONO);
        right.add(tickLbl);
        right.add(cinemaBtn);
        add(right, BorderLayout.EAST);

        wire();
    }

    public void setFrame(WarRoomFrame f) { this.frame = f; }

    private void wire() {
        playBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ctl.play(); }
        });
        pauseBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ctl.pause(); }
        });
        ffBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ctl.cycleSpeed(); }
        });
        editBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { ctl.backToEditor(); }
        });
        cinemaBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { if (frame != null) frame.toggleCinema(); }
        });
        timeline.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (syncing) return;
                if (timeline.getValueIsAdjusting() || !ctl.getEngine().isPlaying()) {
                    ctl.seek(timeline.getValue());
                }
            }
        });
    }

    /** Pull authoritative state from the engine and repaint controls. */
    public void syncFromEngine() {
        SimEngine eng = ctl.getEngine();
        boolean battle = ctl.getMode() == SandboxController.Mode.BATTLE;
        syncing = true;
        int max = Math.max(1, eng.getMaxSimTick());
        timeline.setMaximum(max);
        timeline.setEnabled(battle);
        int cur = Math.min(max, eng.getCurrentTick());
        timeline.setValue(cur);
        tickLbl.setText("tick " + eng.getCurrentTick() + " / " + eng.getMaxSimTick());
        ffBtn.setText("⏩ x" + trim(eng.getSpeed()));
        playBtn.setEnabled(!eng.isPlaying());
        pauseBtn.setEnabled(battle && eng.isPlaying());
        editBtn.setEnabled(battle);
        syncing = false;
    }

    public void onSimFinished() {
        tickLbl.setText("tick " + ctl.getEngine().getCurrentTick() + " — resolved");
    }

    private void style(JButton b) {
        b.setFont(ThemeUI.UI_BOLD);
        b.setForeground(ThemeUI.TEXT);
        b.setBackground(ThemeUI.BG_PANEL_2);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(5, 10, 5, 10));
        b.setPreferredSize(new Dimension(96, 28));
    }

    private static String trim(double d) {
        if (d == Math.floor(d)) return String.valueOf((int) d);
        return String.valueOf(d);
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(ThemeUI.BORDER);
        g.fillRect(0, 0, getWidth(), 1);
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, 44);
    }
}
