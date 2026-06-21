package com.whim.startrek.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.Fleet;
import com.whim.startrek.domain.GameFactory;
import com.whim.startrek.domain.GameServices;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.Race;
import com.whim.startrek.domain.ResourceType;
import com.whim.startrek.domain.Ship;
import com.whim.startrek.domain.TurnManager;
import com.whim.startrek.engine.BattleSimulator;
import com.whim.startrek.engine.BorgEngine;
import com.whim.startrek.engine.EconomyEngine;
import com.whim.startrek.engine.EngineServices;
import com.whim.startrek.engine.FleetAI;

/**
 * Top-level window. A {@link CardLayout} toggles between the macro {@link GalaxyPanel}
 * (TBS) and the real-time {@link BattlePanel} (RTS). A shared toolbar exposes the
 * "End Turn" control wired to {@link TurnManager#advanceTurn()}.
 *
 * <p>Two construction paths: a public no-arg constructor that builds a self-contained
 * demo game (so the app runs before integration) and a fully parameterized constructor
 * the orchestrator's {@code Main} can call with finalized wiring.
 */
public class MainFrame extends JFrame {

    private static final String CARD_GALAXY = "galaxy";
    private static final String CARD_BATTLE = "battle";

    private static final int DEMO_ROWS = 12;
    private static final int DEMO_COLS = 16;
    private static final int DEMO_SHIPS_PER_SIDE = 4;

    private final GameState state;
    private final TurnManager turnManager;
    private final BattleFactory battleFactory;

    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final GalaxyPanel galaxyPanel;
    private final BattlePanel battlePanel;
    private final JLabel statusLabel = new JLabel();
    private final JButton returnButton = new JButton("◄ Galaxy");

    /** Standalone demo: full galaxy via {@link GameFactory} + live engine services. */
    public MainFrame() {
        this(GameFactory.newGame(Race.FEDERATION, DEMO_ROWS, DEMO_COLS),
                new EconomyEngine(), new FleetAI(), new EngineServices(), defaultBattleFactory());
    }

    /**
     * Convenience overload matching the dependency set named in the contract. The
     * {@link BorgEngine} is stepped inside {@link GameServices} during the turn loop,
     * so it is accepted here only to give the orchestrator the documented call shape.
     */
    public MainFrame(GameState state, EconomyEngine economy, FleetAI fleetAI,
                     BorgEngine borg, GameServices services, BattleFactory battleFactory) {
        this(state, economy, fleetAI, services, battleFactory);
    }

    /**
     * Parameterized constructor. The orchestrator supplies a ready {@link GameState},
     * the {@link EconomyEngine} the trade board drives, the {@link FleetAI} used for
     * sensor/cloak-aware rendering, the {@link GameServices} the {@link TurnManager}
     * runs, and a {@link BattleFactory}.
     */
    public MainFrame(GameState state, EconomyEngine economy, FleetAI fleetAI,
                     GameServices services, BattleFactory battleFactory) {
        super("StarTrek: A New Beginning");
        this.state = state;
        this.turnManager = new TurnManager(state, services);
        this.battleFactory = battleFactory;
        this.galaxyPanel = new GalaxyPanel(state, economy, fleetAI);
        this.battlePanel = new BattlePanel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);

        deck.add(galaxyPanel, CARD_GALAXY);
        deck.add(battlePanel, CARD_BATTLE);
        add(deck, BorderLayout.CENTER);

        refreshStatus();
        showGalaxyView();
        pack();
        setLocationRelativeTo(null);
    }

    // ---- toolbar --------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.PANEL_BG);

        statusLabel.setForeground(UiTheme.TEXT);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        left.setOpaque(false);
        left.add(statusLabel);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        right.setOpaque(false);

        JButton endTurn = new JButton("End Turn ▶");
        endTurn.addActionListener(e -> onEndTurn());

        JButton battleDemo = new JButton("Battle ⚔");
        battleDemo.addActionListener(e -> openBattle());

        returnButton.addActionListener(e -> showGalaxyView());
        returnButton.setEnabled(false);

        right.add(returnButton);
        right.add(battleDemo);
        right.add(endTurn);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void onEndTurn() {
        turnManager.advanceTurn();
        refreshStatus();
        galaxyPanel.clearSelection();
        galaxyPanel.repaint();
    }

    private void openBattle() {
        if (battleFactory == null) {
            return;
        }
        BattleSimulator sim = battleFactory.create(state);
        if (sim != null) {
            showBattleView(sim);
        }
    }

    private void refreshStatus() {
        Empire p = state.getPlayerEmpire();
        StringBuilder sb = new StringBuilder();
        sb.append("Turn ").append(state.getTurnNumber())
          .append("  |  Phase: ").append(state.getPhase());
        if (p != null) {
            sb.append("  |  ").append(p.getRace().name())
              .append("  Credits: ").append(p.getTreasury(ResourceType.CREDITS))
              .append("  Dilithium: ").append(p.getTreasury(ResourceType.DILITHIUM));
        }
        statusLabel.setText(sb.toString());
    }

    // ---- view toggles (contract API) ------------------------------------

    public void showGalaxyView() {
        battlePanel.stop();
        returnButton.setEnabled(false);
        cards.show(deck, CARD_GALAXY);
        refreshStatus();
        galaxyPanel.repaint();
        galaxyPanel.requestFocusInWindow();
    }

    public void showBattleView(BattleSimulator battle) {
        battlePanel.setBattle(battle);
        battlePanel.start();
        returnButton.setEnabled(true);
        cards.show(deck, CARD_BATTLE);
        battlePanel.requestFocusInWindow();
    }

    // ---- demo helpers ---------------------------------------------------

    /** Default factory: pit the first two empires' ships against each other. */
    static BattleFactory defaultBattleFactory() {
        return new BattleFactory() {
            @Override
            public BattleSimulator create(GameState st) {
                List<Ship> a = new ArrayList<Ship>();
                List<Ship> b = new ArrayList<Ship>();
                List<Empire> empires = st.getEmpires();
                if (empires != null && empires.size() >= 2) {
                    collectShips(empires.get(0), a, DEMO_SHIPS_PER_SIDE);
                    collectShips(empires.get(1), b, DEMO_SHIPS_PER_SIDE);
                }
                if (a.isEmpty()) {
                    a.add(new Ship("USS Enterprise", "Heavy Cruiser", Race.FEDERATION));
                    a.add(new Ship("USS Defiant", "Escort", Race.FEDERATION));
                }
                if (b.isEmpty()) {
                    b.add(new Ship("IKS Negh'Var", "Battleship", Race.KLINGON));
                    b.add(new Ship("IKS Rotarran", "Bird of Prey", Race.KLINGON));
                }
                return new BattleSimulator(a, b, 800, 600);
            }
        };
    }

    private static void collectShips(Empire e, List<Ship> out, int max) {
        if (e == null || e.getFleets() == null) {
            return;
        }
        for (Fleet f : e.getFleets()) {
            if (f.getShips() == null) {
                continue;
            }
            for (Ship s : f.getShips()) {
                if (out.size() >= max) {
                    return;
                }
                out.add(s);
            }
        }
    }

    // ---- standalone entry (orchestrator's Main supersedes this) ----------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainFrame frame = new MainFrame();
                frame.getContentPane().setBackground(Color.BLACK);
                frame.setVisible(true);
            }
        });
    }
}
