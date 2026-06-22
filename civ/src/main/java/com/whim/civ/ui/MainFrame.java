package com.whim.civ.ui;

import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.EngineServices;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.TurnManager;
import com.whim.civ.domain.Unit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

/**
 * Root window for the Civilization clone. Wires the square-tiled {@link MapPanel} (centre,
 * scrollable), the {@link UnitCommandPanel} (west), the {@link TechTreePanel} (east), and a
 * status / End-Turn bar (south) showing year, active civ, treasury, and the tax/science/
 * luxury rates. Clicking a tile selects a unit, opens a city, or moves the selected unit to
 * an adjacent tile. The End Turn button drives {@link TurnManager} / {@link EngineServices}.
 *
 * <p>Per the contract this talks to the engine only through the {@code EngineServices}
 * handed in (via {@code TurnManager}); the city economy display uses a UI-side
 * {@link SimpleEconomyView} so the screen has numbers even with a no-op engine.
 */
public final class MainFrame extends JFrame {

    private final GameState state;
    private final EngineServices engine;
    private final TurnManager turnManager;
    private EconomyView econ = new SimpleEconomyView();

    private final MapPanel mapPanel;
    private final UnitCommandPanel commandPanel;
    private final TechTreePanel techPanel;

    private final JLabel yearLabel = new JLabel();
    private final JLabel civLabel = new JLabel();
    private final JLabel treasuryLabel = new JLabel();
    private final JLabel ratesLabel = new JLabel();

    private Unit selectedUnit;

    /**
     * Inject the economy view used by city screens. Lets {@code Main} wire the real
     * engine-backed {@code EconomyView} (e.g. {@code EngineEconomyView}) so city numbers
     * reflect the live engine instead of the UI-side {@link SimpleEconomyView} fallback.
     */
    public void setEconomyView(EconomyView view) {
        if (view != null) {
            this.econ = view;
        }
    }

    public MainFrame(GameState state, EngineServices engine) {
        super("Civilization (1991) — Java 8 Swing");
        this.state = state;
        this.engine = engine;
        this.turnManager = new TurnManager(state, engine);
        this.mapPanel = new MapPanel(state);
        this.commandPanel = new UnitCommandPanel(state);
        this.techPanel = new TechTreePanel(state);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(buildToolBar(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(mapPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(mapPanel.getTileSize());
        scroll.getHorizontalScrollBar().setUnitIncrement(mapPanel.getTileSize());
        add(scroll, BorderLayout.CENTER);

        add(commandPanel, BorderLayout.WEST);
        add(techPanel, BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);

        wireCallbacks();
    }

    private void wireCallbacks() {
        mapPanel.setTileListener(new MapPanel.TileListener() {
            @Override
            public void tileClicked(int tileX, int tileY) {
                onTileClicked(tileX, tileY);
            }
        });
        commandPanel.setOnChanged(new Runnable() {
            public void run() {
                refreshAll();
            }
        });
        techPanel.setOnChanged(new Runnable() {
            public void run() {
                refreshStatus();
            }
        });
    }

    private JToolBar buildToolBar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton center = new JButton("Center on Capital");
        center.setFocusable(false);
        center.addActionListener(e -> centerOnCapital());
        bar.add(center);

        JButton zoomIn = new JButton("Zoom +");
        zoomIn.setFocusable(false);
        zoomIn.addActionListener(e -> mapPanel.setTileSize(mapPanel.getTileSize() + 4));
        bar.add(zoomIn);

        JButton zoomOut = new JButton("Zoom -");
        zoomOut.setFocusable(false);
        zoomOut.addActionListener(e -> mapPanel.setTileSize(mapPanel.getTileSize() - 4));
        bar.add(zoomOut);

        JButton rates = new JButton("Tax Rates...");
        rates.setFocusable(false);
        rates.addActionListener(e -> editRates());
        bar.add(rates);

        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.PANEL_BG);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel labels = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        labels.setOpaque(false);
        Font f = UiTheme.H2;
        for (JLabel l : new JLabel[] { yearLabel, civLabel, treasuryLabel, ratesLabel }) {
            l.setForeground(UiTheme.PANEL_FG);
            l.setFont(f);
            labels.add(l);
        }
        bar.add(labels, BorderLayout.CENTER);

        JButton endTurn = new JButton("End Turn  ▶");
        endTurn.setFocusable(false);
        endTurn.setFont(UiTheme.H2);
        endTurn.addActionListener(e -> endTurn());
        bar.add(endTurn, BorderLayout.EAST);

        return bar;
    }

    /** Build the UI, select an initial unit, and show the window. */
    public void showGame() {
        Runnable r = new Runnable() {
            public void run() {
                setPreferredSize(new Dimension(1180, 760));
                pack();
                setLocationRelativeTo(null);
                techPanel.setCivilization(activeCiv());
                selectInitialUnit();
                centerOnCapital();
                refreshAll();
                setVisible(true);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private Civilization activeCiv() {
        List<Civilization> civs = state.getCivilizations();
        if (civs.isEmpty()) {
            return null;
        }
        int i = Math.max(0, Math.min(state.getActiveCivIndex(), civs.size() - 1));
        return civs.get(i);
    }

    private void onTileClicked(int x, int y) {
        City city = state.cityAt(x, y);
        if (city != null && city.getOwnerCivId() == state.getActiveCivIndex()) {
            new CityScreen(this, state, city, econ).setVisible(true);
            refreshAll();
            return;
        }

        // Move the selected unit onto an adjacent tile.
        if (selectedUnit != null && selectedUnit.isAlive()
                && selectedUnit.getOwnerCivId() == state.getActiveCivIndex()) {
            int dx = Math.abs(x - selectedUnit.getX());
            int dy = Math.abs(y - selectedUnit.getY());
            boolean adjacent = dx <= 1 && dy <= 1 && (dx + dy) > 0;
            if (adjacent && selectedUnit.getMovesLeft() > 0
                    && state.cityAt(x, y) == null && state.unitsAt(x, y).isEmpty()) {
                moveSelected(x, y);
                return;
            }
        }

        // Otherwise select a unit belonging to the active civ on this tile.
        List<Unit> here = state.unitsAt(x, y);
        Unit pick = null;
        for (int i = 0; i < here.size(); i++) {
            if (here.get(i).getOwnerCivId() == state.getActiveCivIndex()) {
                pick = here.get(i);
                break;
            }
        }
        select(pick);
    }

    private void moveSelected(int x, int y) {
        Tile dest = state.getMap().getTile(x, y);
        selectedUnit.setPosition(x, y);
        selectedUnit.setMovesLeft(selectedUnit.getMovesLeft() - 1);
        selectedUnit.setFortified(false);
        if (dest.hasGoodyHut()) {
            dest.setGoodyHut(false);
            JOptionPane.showMessageDialog(this,
                    "Your " + selectedUnit.getType().name() + " explored a tribal hut!",
                    "Goody Hut", JOptionPane.INFORMATION_MESSAGE);
        }
        refreshAll();
    }

    private void select(Unit u) {
        this.selectedUnit = u;
        mapPanel.setSelectedUnit(u);
        commandPanel.setActiveUnit(u);
        if (u != null) {
            mapPanel.centerOn(u.getX(), u.getY());
        }
    }

    private void selectInitialUnit() {
        List<Unit> mine = state.unitsOf(state.getActiveCivIndex());
        select(mine.isEmpty() ? null : mine.get(0));
    }

    private void centerOnCapital() {
        List<City> cities = state.citiesOf(state.getActiveCivIndex());
        if (!cities.isEmpty()) {
            mapPanel.centerOn(cities.get(0).getX(), cities.get(0).getY());
            return;
        }
        List<Unit> mine = state.unitsOf(state.getActiveCivIndex());
        if (!mine.isEmpty()) {
            mapPanel.centerOn(mine.get(0).getX(), mine.get(0).getY());
        }
    }

    private void endTurn() {
        // Advance through the active human's turn and any following AI civs until it is a
        // human's turn again (TurnManager.endTurn runs the EngineServices phases + AI).
        turnManager.endTurn();
        int guard = 0;
        while (!turnManager.isHumanTurn() && guard < state.getCivilizations().size() + 1) {
            turnManager.endTurn();
            guard++;
        }
        // Refresh movement points for the civ now on the clock so units are usable even with
        // a no-op engine (the real engine resets these during its UPKEEP phase).
        for (Unit u : state.unitsOf(state.getActiveCivIndex())) {
            u.setMovesLeft(u.getType().getMovement());
        }
        techPanel.setCivilization(activeCiv());
        selectInitialUnit();
        centerOnCapital();
        refreshAll();
    }

    private void editRates() {
        Civilization civ = activeCiv();
        if (civ == null) {
            return;
        }
        Integer[] steps = new Integer[11];
        for (int i = 0; i <= 10; i++) {
            steps[i] = i * 10;
        }
        JComboBox<Integer> tax = new JComboBox<Integer>(steps);
        JComboBox<Integer> sci = new JComboBox<Integer>(steps);
        tax.setSelectedItem(civ.getTaxRate());
        sci.setSelectedItem(civ.getScienceRate());
        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Tax %"));
        p.add(tax);
        p.add(new JLabel("Science %"));
        p.add(sci);
        p.add(new JLabel("Luxury %"));
        JLabel luxLabel = new JLabel();
        p.add(luxLabel);
        Runnable updateLux = new Runnable() {
            public void run() {
                int lux = 100 - (Integer) tax.getSelectedItem() - (Integer) sci.getSelectedItem();
                luxLabel.setText(lux + "  " + (lux < 0 ? "(too high!)" : ""));
            }
        };
        tax.addActionListener(e -> updateLux.run());
        sci.addActionListener(e -> updateLux.run());
        updateLux.run();

        int ok = JOptionPane.showConfirmDialog(this, p, "Set Tax / Science / Luxury rates",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            int t = (Integer) tax.getSelectedItem();
            int s = (Integer) sci.getSelectedItem();
            int lux = 100 - t - s;
            if (lux < 0) {
                JOptionPane.showMessageDialog(this, "Tax + Science may not exceed 100%.",
                        "Invalid rates", JOptionPane.WARNING_MESSAGE);
                return;
            }
            civ.setRates(t, s, lux);
            refreshStatus();
        }
    }

    private void refreshAll() {
        mapPanel.repaint();
        commandPanel.setActiveUnit(selectedUnit != null && selectedUnit.isAlive() ? selectedUnit : null);
        if (selectedUnit != null && !selectedUnit.isAlive()) {
            selectedUnit = null;
            mapPanel.setSelectedUnit(null);
        }
        techPanel.refresh();
        refreshStatus();
    }

    private void refreshStatus() {
        yearLabel.setText("Year: " + formatYear(state.getYear()) + "  (turn " + state.getTurnNumber() + ")");
        Civilization civ = activeCiv();
        if (civ != null) {
            civLabel.setText("Civ: " + civ.getName() + "  [" + civ.getGovernment().name() + "]");
            treasuryLabel.setText("Treasury: " + civ.getTreasury() + "g");
            ratesLabel.setText("Tax " + civ.getTaxRate() + "%  Sci " + civ.getScienceRate()
                    + "%  Lux " + civ.getLuxuryRate() + "%");
            treasuryLabel.setForeground(civ.getTreasury() < 0 ? new Color(240, 120, 120) : UiTheme.PANEL_FG);
        }
    }

    private static String formatYear(int year) {
        if (year < 0) {
            return (-year) + " BC";
        }
        return year + " AD";
    }
}
