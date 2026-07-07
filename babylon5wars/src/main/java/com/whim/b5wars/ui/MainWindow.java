package com.whim.b5wars.ui;

import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Section;
import com.whim.b5wars.model.Ship;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

/**
 * The main application window: menu bar plus the tactical map, turn bar, ship sheet, power/EW
 * panel and combat log, all wired to a single {@link GameController}.
 *
 * <p>Construction is headless-safe (it only builds Swing components); callers must guard the
 * actual {@link #setVisible} in a headless environment.
 */
public final class MainWindow extends JFrame {

    private final GameController controller;
    private final PlayAreaPanel playArea;
    private final ShipSheetPanel shipSheet;
    private final TurnBarPanel turnBar;
    private final PowerEwPanel powerEw;
    private final LogPanel log;

    public MainWindow(GameController controller) {
        super("Babylon 5 Wars — " + controller.state().getScenario().getName());
        this.controller = controller;

        this.playArea = new PlayAreaPanel(controller);
        this.shipSheet = new ShipSheetPanel(controller);
        this.turnBar = new TurnBarPanel(controller);
        this.powerEw = new PowerEwPanel(controller);
        this.log = new LogPanel();
        controller.addListener(log);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(buildMenu());

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(UiTheme.PANEL_BG);
        right.add(shipSheet);
        right.add(powerEw);
        right.add(log);
        JScrollPane rightScroll = new JScrollPane(right,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.setPreferredSize(new Dimension(400, 800));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playArea, rightScroll);
        split.setResizeWeight(0.72);
        split.setContinuousLayout(true);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(turnBar, BorderLayout.NORTH);
        getContentPane().add(split, BorderLayout.CENTER);

        setPreferredSize(new Dimension(1180, 820));
        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();

        JMenu game = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game (reset seed)");
        newGame.addActionListener(e -> controller.newGame());
        JMenuItem save = new JMenuItem("Save Snapshot…");
        save.addActionListener(e -> saveSnapshot());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        game.add(newGame);
        game.add(save);
        game.addSeparator();
        game.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem controls = new JMenuItem("Controls & About");
        controls.addActionListener(e -> showHelp());
        help.add(controls);

        bar.add(game);
        bar.add(help);
        return bar;
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(this,
                "Babylon 5 Wars — Java 8 / Swing hot-seat recreation (unofficial).\n\n"
                        + "Flow: click Advance Phase to walk INITIATIVE → POWER → EW → IMPULSE.\n"
                        + "  • POWER/EW: adjust the selected ship's thrust / EW in the right panel.\n"
                        + "  • IMPULSE: select a ship, then Forward / Turn / Slip / Accel to maneuver,\n"
                        + "    and Fire… to shoot. Then Advance Phase (\"End Turn\") to resolve the turn.\n\n"
                        + "Click a ship on the map to select it; click a weapon row to show its arc\n"
                        + "and range rings. All stats are APPROXIMATED and unverified vs the rulebook.",
                "Controls & About", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Simple, optional "save": a human-readable text snapshot of the current state. Full binary
     * save/load is intentionally omitted — the engine/model types are not {@code Serializable} and
     * are owned by other tasks, so a round-trip cannot be provided without changing them.
     */
    private void saveSnapshot() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("b5wars-snapshot.txt"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            PrintWriter out = new PrintWriter(chooser.getSelectedFile(), "UTF-8");
            try {
                writeSnapshot(out);
            } finally {
                out.close();
            }
            JOptionPane.showMessageDialog(this, "Snapshot saved to\n"
                    + chooser.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeSnapshot(PrintWriter out) {
        out.println("Babylon 5 Wars snapshot");
        out.println("Scenario: " + controller.state().getScenario().getName());
        out.println("Turn " + controller.state().getTurn()
                + "  Phase " + controller.state().getPhase()
                + "  Impulse " + controller.state().getImpulse()
                + "  Initiative " + controller.initiativeText());
        out.println("Over: " + controller.state().isOver()
                + (controller.state().getWinner() != null
                        ? "  Winner Side " + controller.state().getWinner() : ""));
        out.println();
        for (Ship s : controller.state().getShips()) {
            out.println(s.getType().getName() + "  [Side " + s.getSide() + "]");
            out.println("  pos " + s.getPos() + "  facing " + s.getFacing()
                    + "  speed " + s.getSpeed()
                    + "  thrust " + s.getThrustAvailable()
                    + (s.isDestroyed() ? "  DESTROYED" : "")
                    + (s.isCrippled() ? "  CRIPPLED" : ""));
            StringBuilder armor = new StringBuilder("  armor ");
            for (Facing f : Facing.values()) {
                Map<Facing, Integer> a = s.getArmor();
                armor.append(f).append('=').append(a.get(f)).append(' ');
            }
            out.println(armor.toString());
            StringBuilder str = new StringBuilder("  struct ");
            for (Section sec : Section.values()) {
                str.append(sec).append('=').append(s.getStructure().get(sec)).append(' ');
            }
            out.println(str.toString());
        }
    }
}
