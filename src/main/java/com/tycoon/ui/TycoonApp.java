package com.tycoon.ui;

import com.tycoon.core.AutoTurnEngine;
import com.tycoon.core.FacilityType;
import com.tycoon.core.GameState;
import com.tycoon.core.Interrupt;
import com.tycoon.core.RoomType;
import com.tycoon.core.TurnProcessor;
import com.tycoon.sim.SimTurnProcessor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.util.List;

/**
 * Final consolidated entry point for the turn-based Mad Games Tycoon adaptation.
 *
 * <p>Boots a {@link GameState}, wires an {@link AutoTurnEngine}, and shows the Swing frame
 * with the freeform building grid plus the turn/interrupt control bar.</p>
 */
public class TycoonApp extends JFrame {

    private final GameState state;
    private final AutoTurnEngine engine;
    private final FloorPlanPanel floor;

    private final JLabel statusLabel = new JLabel();
    private final JTextArea log = new JTextArea(12, 28);

    public TycoonApp(GameState state, AutoTurnEngine engine) {
        super("Mad Games Tycoon — Turn-Based (1 turn = 1 hour)");
        this.state = state;
        this.engine = engine;
        this.floor = new FloorPlanPanel(state);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        floor.setOnChange(this::refreshStatus);
        floor.setOnReject(this::logLine);

        add(new JScrollPane(floor), BorderLayout.CENTER);
        add(buildPalette(), BorderLayout.WEST);
        add(buildSidePanel(), BorderLayout.EAST);
        add(buildControlBar(), BorderLayout.SOUTH);

        refreshStatus();
        logLine("New game started (seed-based). Build your studio, then Advance.");
        pack();
        setLocationRelativeTo(null);
    }

    // ---- palette (building phase tools) ------------------------------------

    private JPanel buildPalette() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Build mode"));

        JToggleButton roomBtn = new JToggleButton("Draw Room", true);
        JToggleButton facBtn = new JToggleButton("Place Facility");
        ButtonGroup grp = new ButtonGroup();
        grp.add(roomBtn);
        grp.add(facBtn);
        roomBtn.addActionListener(e -> floor.setMode(FloorPlanPanel.Mode.DRAW_ROOM));
        facBtn.addActionListener(e -> floor.setMode(FloorPlanPanel.Mode.PLACE_FACILITY));
        p.add(roomBtn);
        p.add(facBtn);

        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("Room type"));
        final JComboBox<RoomType> rooms = new JComboBox<RoomType>(RoomType.values());
        rooms.addActionListener(e -> floor.setSelectedRoom((RoomType) rooms.getSelectedItem()));
        rooms.setMaximumSize(new Dimension(180, 26));
        p.add(rooms);

        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("Facility"));
        final JComboBox<FacilityType> facs = new JComboBox<FacilityType>(FacilityType.values());
        facs.addActionListener(e -> floor.setSelectedFacility((FacilityType) facs.getSelectedItem()));
        facs.setMaximumSize(new Dimension(180, 26));
        p.add(facs);

        p.add(Box.createVerticalStrut(10));
        JTextArea legend = new JTextArea(
                "Glyphs:\nD desk  C coffee\nH heater  P plant\nA arcade\n\n"
                        + "Drag = draw room\nClick = place facility");
        legend.setEditable(false);
        legend.setOpaque(false);
        legend.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        p.add(legend);

        return p;
    }

    // ---- right side: interrupt log -----------------------------------------

    private JPanel buildSidePanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(new JLabel("Interrupt / Event log"), BorderLayout.NORTH);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        p.add(new JScrollPane(log), BorderLayout.CENTER);
        return p;
    }

    // ---- bottom: control bar -----------------------------------------------

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        statusLabel.setForeground(new Color(20, 20, 20));
        bar.add(statusLabel);

        bar.add(new JLabel("  max hours:"));
        final JSpinner maxHours = new JSpinner(new SpinnerNumberModel(720, 1, 100000, 1));
        ((JSpinner.DefaultEditor) maxHours.getEditor()).getTextField().setColumns(5);
        bar.add(maxHours);

        JButton advance = new JButton("Advance");
        advance.addActionListener(e -> {
            int max = (Integer) maxHours.getValue();
            List<Interrupt> ints = engine.run(max);
            reportInterrupts("Advance up to " + max + "h", ints);
        });
        bar.add(advance);

        JButton step = new JButton("Step (1h)");
        step.addActionListener(e -> {
            List<Interrupt> ints = engine.step();
            reportInterrupts("Step", ints);
        });
        bar.add(step);

        return bar;
    }

    private void reportInterrupts(String label, List<Interrupt> ints) {
        // Engine unlocks the floor plan on return so the paused building phase resumes.
        if (ints == null || ints.isEmpty()) {
            logLine(label + ": no interrupts (hour now " + state.hour() + ").");
        } else {
            logLine("== " + label + " produced " + ints.size() + " interrupt(s) ==");
            for (int i = 0; i < ints.size(); i++) {
                Interrupt it = ints.get(i);
                logLine("  [" + it.type() + " @h" + it.hour() + "] " + it.message());
            }
        }
        refreshStatus();
        floor.repaint();
    }

    private void refreshStatus() {
        statusLabel.setText(String.format(
                "Cash $%d  |  hour %d  (day %d, week %d)  |  employees %d  |  %s",
                state.player().cash(), state.hour(), state.day(), state.week(),
                state.player().employees().size(),
                state.player().floorPlan().isLocked() ? "LOCKED" : "building"));
    }

    private void logLine(String s) {
        log.append(s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // ---- boot ---------------------------------------------------------------

    public static void main(String[] args) {
        long seed = args.length > 0 ? parseSeedOr(args[0], 42L) : 42L;
        final GameState state = GameState.newGame(seed);
        final TurnProcessor processor = new SimTurnProcessor();
        final AutoTurnEngine engine = new AutoTurnEngine(state, processor);

        if (GraphicsEnvironment.isHeadless()) {
            // Headless smoke path: models + engine are exercised without a display.
            System.out.println("[TycoonApp] Headless mode — skipping UI.");
            System.out.println("[TycoonApp] New game seed=" + seed
                    + " hour=" + state.hour()
                    + " cash=$" + state.player().cash()
                    + " employees=" + state.player().employees().size()
                    + " competitors=" + state.competitors().size());
            List<Interrupt> ints = engine.run(24);
            System.out.println("[TycoonApp] Ran up to 24h -> hour=" + state.hour()
                    + ", interrupts=" + (ints == null ? 0 : ints.size()));
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new TycoonApp(state, engine).setVisible(true);
            }
        });
    }

    private static long parseSeedOr(String s, long fallback) {
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}
