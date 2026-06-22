package com.whim.civ.ui;

import com.whim.civ.domain.Building;
import com.whim.civ.domain.City;
import com.whim.civ.domain.Civilization;
import com.whim.civ.domain.EconomyView;
import com.whim.civ.domain.GameMap;
import com.whim.civ.domain.GameState;
import com.whim.civ.domain.TechType;
import com.whim.civ.domain.Tile;
import com.whim.civ.domain.UnitType;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

/**
 * City management screen. Shows the 21-tile fat-cross work radius with each tile's yields,
 * the city's food/shields/trade (read from the {@link EconomyView} so the UI never imports
 * the engine), the tax/science/luxury split of that trade, a production picker, and the list
 * of completed buildings. Selecting an item from the production list updates the city's
 * order via the domain setters.
 */
public final class CityScreen extends JDialog {

    private final GameState state;
    private final City city;
    private final EconomyView econ;

    private final JLabel yieldsLabel = new JLabel();
    private final JLabel prodLabel = new JLabel();
    private final DefaultListModel<ProdOption> prodModel = new DefaultListModel<ProdOption>();
    private final JList<ProdOption> prodList = new JList<ProdOption>(prodModel);
    private final DefaultListModel<String> buildingModel = new DefaultListModel<String>();

    public CityScreen(Frame owner, GameState state, City city, EconomyView econ) {
        super(owner, "City of " + city.getName(), true);
        this.state = state;
        this.city = city;
        this.econ = econ;

        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(buildHeader(), BorderLayout.NORTH);
        add(new WorkRadiusPanel(), BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);

        refresh();
        setMinimumSize(new Dimension(720, 460));
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new GridLayout(0, 1));
        JLabel name = new JLabel(city.getName() + "  —  size " + city.getPopulation());
        name.setFont(UiTheme.H1);
        p.add(name);
        yieldsLabel.setFont(UiTheme.BODY);
        p.add(yieldsLabel);
        return p;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout(6, 6));
        side.setPreferredSize(new Dimension(260, 400));

        JPanel prodBox = new JPanel(new BorderLayout(4, 4));
        prodBox.setBorder(BorderFactory.createTitledBorder("Production"));
        prodLabel.setFont(UiTheme.BODY);
        prodBox.add(prodLabel, BorderLayout.NORTH);
        prodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        prodBox.add(new JScrollPane(prodList), BorderLayout.CENTER);
        JButton setBtn = new JButton("Produce Selected");
        setBtn.setFocusable(false);
        setBtn.addActionListener(e -> {
            ProdOption opt = prodList.getSelectedValue();
            if (opt != null) {
                opt.apply(city);
                refresh();
            }
        });
        prodBox.add(setBtn, BorderLayout.SOUTH);
        side.add(prodBox, BorderLayout.CENTER);

        JPanel bldBox = new JPanel(new BorderLayout());
        bldBox.setBorder(BorderFactory.createTitledBorder("Completed Buildings"));
        JList<String> bldList = new JList<String>(buildingModel);
        bldList.setVisibleRowCount(5);
        bldBox.add(new JScrollPane(bldList), BorderLayout.CENTER);
        side.add(bldBox, BorderLayout.SOUTH);

        JButton close = new JButton("Close");
        close.setFocusable(false);
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.add(close, BorderLayout.EAST);
        side.add(south, BorderLayout.NORTH);
        return side;
    }

    private Civilization owner() {
        return state.civById(city.getOwnerCivId());
    }

    private void refresh() {
        int food = econ.food(state, city);
        int shields = econ.shields(state, city);
        int trade = econ.trade(state, city);
        int[] split = econ.tradeSplit(owner(), trade);
        int growth = food - 2 * city.getPopulation();
        yieldsLabel.setText("<html>Food " + food + " (net " + (growth >= 0 ? "+" : "") + growth
                + ", box " + city.getFoodStore() + "/" + city.getFoodBoxSize() + ")"
                + " &nbsp; Shields " + shields
                + " &nbsp; Trade " + trade
                + " &nbsp;[ Tax " + split[0] + " / Sci " + split[1] + " / Lux " + split[2] + " ]</html>");

        prodLabel.setText("<html>Now building: <b>" + currentProduction()
                + "</b><br>" + city.getShieldStore() + " / " + currentCost() + " shields</html>");

        rebuildProdList();

        buildingModel.clear();
        List<Building> built = city.getBuildings();
        if (built.isEmpty()) {
            buildingModel.addElement("(none yet)");
        } else {
            for (int i = 0; i < built.size(); i++) {
                buildingModel.addElement(pretty(built.get(i).name()));
            }
        }
        repaint();
    }

    private String currentProduction() {
        if (city.getProducingUnit() != null) {
            return pretty(city.getProducingUnit().name());
        }
        if (city.getProducingBuilding() != null) {
            return pretty(city.getProducingBuilding().name());
        }
        return "nothing";
    }

    private int currentCost() {
        if (city.getProducingUnit() != null) {
            return city.getProducingUnit().getCost();
        }
        if (city.getProducingBuilding() != null) {
            return city.getProducingBuilding().getCost();
        }
        return 0;
    }

    private void rebuildProdList() {
        prodModel.clear();
        Civilization civ = owner();
        for (UnitType u : UnitType.values()) {
            if (u.getPrereq() == null || (civ != null && civ.knows(u.getPrereq()))) {
                prodModel.addElement(ProdOption.unit(u));
            }
        }
        for (Building b : Building.values()) {
            if (city.getBuildings().contains(b)) {
                continue;
            }
            if (b.getPrereq() != null && (civ == null || !civ.knows(b.getPrereq()))) {
                continue;
            }
            if (b.isWonder() && wonderBuiltSomewhere(b)) {
                continue;
            }
            prodModel.addElement(ProdOption.building(b));
        }
    }

    private boolean wonderBuiltSomewhere(Building b) {
        for (City c : state.getCities()) {
            if (c.getBuildings().contains(b)) {
                return true;
            }
        }
        return false;
    }

    private static String pretty(String enumName) {
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /** One selectable production order (a unit or a building). */
    private static final class ProdOption {
        private final UnitType unit;
        private final Building building;

        private ProdOption(UnitType u, Building b) {
            this.unit = u;
            this.building = b;
        }

        static ProdOption unit(UnitType u) {
            return new ProdOption(u, null);
        }

        static ProdOption building(Building b) {
            return new ProdOption(null, b);
        }

        void apply(City c) {
            if (unit != null) {
                c.setProducingUnit(unit);
            } else {
                c.setProducingBuilding(building);
            }
        }

        @Override
        public String toString() {
            if (unit != null) {
                return "Unit: " + pretty(unit.name()) + "  (" + unit.getCost() + "s)";
            }
            return (building.isWonder() ? "Wonder: " : "Bldg: ")
                    + pretty(building.name()) + "  (" + building.getCost() + "s)";
        }
    }

    /** Paints the 21-tile fat cross centred on the city, with per-tile yields. */
    private final class WorkRadiusPanel extends JPanel {
        private static final int CELL = 78;

        WorkRadiusPanel() {
            setPreferredSize(new Dimension(CELL * 5 + 20, CELL * 5 + 20));
            setBackground(new Color(24, 28, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GameMap map = state.getMap();
            int ox = (getWidth() - CELL * 5) / 2;
            int oy = (getHeight() - CELL * 5) / 2;

            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    if (Math.abs(dx) == 2 && Math.abs(dy) == 2) {
                        continue; // fat-cross corner cut
                    }
                    int tx = city.getX() + dx;
                    int ty = city.getY() + dy;
                    int px = ox + (dx + 2) * CELL;
                    int py = oy + (dy + 2) * CELL;
                    if (!map.inBounds(tx, ty)) {
                        g2.setColor(new Color(40, 40, 48));
                        g2.fillRect(px, py, CELL - 2, CELL - 2);
                        continue;
                    }
                    Tile t = map.getTile(tx, ty);
                    Color bg = UiTheme.terrainColor(t.getTerrain());
                    g2.setColor(bg);
                    g2.fillRect(px, py, CELL - 2, CELL - 2);
                    g2.setColor(UiTheme.GRID);
                    g2.drawRect(px, py, CELL - 2, CELL - 2);

                    boolean center = dx == 0 && dy == 0;
                    if (center) {
                        g2.setColor(UiTheme.civColor(city.getOwnerCivId()));
                        g2.fillOval(px + CELL / 2 - 14, py + CELL / 2 - 14, 28, 28);
                        g2.setColor(Color.WHITE);
                        g2.drawString(Integer.toString(city.getPopulation()),
                                px + CELL / 2 - 4, py + CELL / 2 + 4);
                    }

                    g2.setColor(UiTheme.inkFor(bg));
                    g2.setFont(UiTheme.MONO);
                    g2.drawString("F" + t.yieldFood() + " S" + t.yieldShields() + " T" + t.yieldTrade(),
                            px + 4, py + CELL - 8);
                    g2.setFont(UiTheme.BODY);
                    g2.drawString(shortName(t.getTerrain().name()), px + 4, py + 14);
                }
            }
            g2.dispose();
        }

        private String shortName(String n) {
            return n.length() > 8 ? n.substring(0, 8) : n;
        }
    }
}
