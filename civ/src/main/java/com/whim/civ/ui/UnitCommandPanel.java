package com.whim.civ.ui;

import com.whim.civ.domain.City;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.Improvement;
import com.whim.civ.domain.Terrain;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.Unit;
import com.whim.civ.domain.UnitType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * Left-hand command panel for the currently selected unit. Shows stats and exposes the
 * Civ-1 unit orders. Per the fidelity checklist only Settlers may terraform (road /
 * irrigation / mine) or found a city — those buttons are hidden/disabled for other units.
 *
 * <p>Orders mutate the domain directly (the contract's {@code EngineServices} bridge covers
 * upkeep/production/research/AI, not per-unit movement or terraforming) and then fire the
 * {@link #setOnChanged(Runnable)} callback so the host can refresh the map and status bar.
 */
public final class UnitCommandPanel extends JPanel {

    private final GameState state;
    private Unit unit;
    private Runnable onChanged = new Runnable() {
        public void run() { }
    };

    private final JLabel title = new JLabel("No unit selected");
    private final JLabel stats = new JLabel(" ");
    private final JLabel terrainLabel = new JLabel(" ");
    private final JLabel hint = new JLabel(" ");
    private final JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));

    public UnitCommandPanel(GameState state) {
        this.state = state;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiTheme.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setPreferredSize(new Dimension(190, 400));

        title.setFont(UiTheme.H1);
        title.setForeground(UiTheme.PANEL_FG);
        stats.setFont(UiTheme.BODY);
        stats.setForeground(UiTheme.PANEL_FG);
        terrainLabel.setFont(UiTheme.BODY);
        terrainLabel.setForeground(new Color(180, 200, 220));
        hint.setFont(UiTheme.BODY);
        hint.setForeground(UiTheme.ACCENT);
        buttons.setOpaque(false);

        add(left(title));
        add(Box.createVerticalStrut(6));
        add(left(stats));
        add(left(terrainLabel));
        add(Box.createVerticalStrut(10));
        add(left(buttons));
        add(Box.createVerticalStrut(8));
        add(left(hint));
        add(Box.createVerticalGlue());

        rebuild();
    }

    public void setOnChanged(Runnable r) {
        this.onChanged = r != null ? r : this.onChanged;
    }

    private static Component left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public void setActiveUnit(Unit u) {
        this.unit = u;
        rebuild();
    }

    private void rebuild() {
        buttons.removeAll();
        if (unit == null || !unit.isAlive()) {
            title.setText("No unit selected");
            stats.setText("Click a unit on the map.");
            terrainLabel.setText(" ");
            hint.setText(" ");
            buttons.revalidate();
            buttons.repaint();
            return;
        }

        UnitType t = unit.getType();
        title.setText(t.name());
        stats.setText("<html>A " + t.getAttack() + "  D " + t.getDefense()
                + "  M " + unit.getMovesLeft() + "/" + t.getMovement()
                + "<br>HP " + unit.getHitPoints() + "/" + unit.maxHitPoints()
                + (unit.isVeteran() ? "  <b>vet</b>" : "")
                + (unit.isFortified() ? "  <b>fortified</b>" : "") + "</html>");

        Tile tile = state.getMap().getTile(unit.getX(), unit.getY());
        terrainLabel.setText("Terrain: " + tile.getTerrain().name()
                + (tile.getImprovement() != Improvement.NONE ? " / " + tile.getImprovement().name() : ""));

        addButton("Move", "Click an adjacent tile on the map to move.", new Runnable() {
            public void run() {
                hint.setText("Click an adjacent tile to move " + unit.getType().name() + ".");
            }
        });
        addButton("Fortify", null, new Runnable() {
            public void run() {
                unit.setFortified(true);
                unit.setMovesLeft(0);
                fired();
            }
        });

        if (t.isSettler()) {
            addTerraform(tile);
        }
        if (t.canFound()) {
            JButton found = makeButton("Found City");
            found.setEnabled(state.cityAt(unit.getX(), unit.getY()) == null);
            found.addActionListener(e -> foundCity());
            buttons.add(found);
        }

        addButton("Sentry / Skip", null, new Runnable() {
            public void run() {
                unit.setMovesLeft(0);
                fired();
            }
        });

        buttons.revalidate();
        buttons.repaint();
    }

    private void addTerraform(Tile tile) {
        Terrain terr = tile.getTerrain();
        JButton road = makeButton(tile.hasRoad() ? "Build Railroad" : "Build Road");
        road.setEnabled(terr.canBuildRoad());
        road.addActionListener(e -> {
            tile.setImprovement(tile.hasRoad() ? Improvement.RAILROAD : Improvement.ROAD);
            unit.setMovesLeft(0);
            fired();
        });
        buttons.add(road);

        JButton irr = makeButton("Irrigate");
        irr.setEnabled(terr.canIrrigate());
        irr.addActionListener(e -> {
            tile.setImprovement(Improvement.IRRIGATION);
            unit.setMovesLeft(0);
            fired();
        });
        buttons.add(irr);

        JButton mine = makeButton("Build Mine");
        mine.setEnabled(terr.canMine());
        mine.addActionListener(e -> {
            tile.setImprovement(Improvement.MINE);
            unit.setMovesLeft(0);
            fired();
        });
        buttons.add(mine);
    }

    private void foundCity() {
        if (state.cityAt(unit.getX(), unit.getY()) != null) {
            return;
        }
        int civId = unit.getOwnerCivId();
        int existing = state.citiesOf(civId).size();
        String defaultName = cityName(civId, existing);
        String name = JOptionPane.showInputDialog(this, "Name the new city:", defaultName);
        if (name == null) {
            return;
        }
        if (name.trim().isEmpty()) {
            name = defaultName;
        }
        City c = new City(civId, name.trim(), unit.getX(), unit.getY());
        state.getCities().add(c);
        state.getMap().getTile(unit.getX(), unit.getY()).setOwnerCivId(civId);
        // The Settler is consumed founding the city.
        state.getUnits().remove(unit);
        unit = null;
        rebuild();
        fired();
    }

    private String cityName(int civId, int index) {
        String civ = state.civById(civId) != null ? state.civById(civId).getName() : "City";
        return civ + " " + (index + 1);
    }

    private void addButton(String text, final String hintText, final Runnable action) {
        JButton b = makeButton(text);
        b.addActionListener(e -> {
            if (hintText != null) {
                hint.setText(hintText);
            }
            action.run();
        });
        buttons.add(b);
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setFont(UiTheme.BODY);
        return b;
    }

    private void fired() {
        rebuild();
        onChanged.run();
    }
}
