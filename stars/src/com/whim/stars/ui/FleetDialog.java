package com.whim.stars.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

import com.whim.stars.model.Cargo;
import com.whim.stars.model.Fleet;
import com.whim.stars.model.Galaxy;
import com.whim.stars.model.Mineral;
import com.whim.stars.model.Planet;
import com.whim.stars.model.Player;
import com.whim.stars.model.Waypoint;
import com.whim.stars.model.ship.ShipDesign;

/**
 * The Fleet management + cargo-transfer screen. Lists the human player's fleets,
 * shows composition/fuel/cargo, and lets the player append waypoints (with a
 * target, warp and task) and transfer colonists/minerals to or from a planet the
 * fleet is orbiting. Edits the model directly.
 */
public final class FleetDialog extends JDialog {

    private final Galaxy galaxy;
    private final Player human;

    private final DefaultListModel<Fleet> fleetModel = new DefaultListModel<Fleet>();
    private final JList<Fleet> fleetList = new JList<Fleet>(fleetModel);
    private final JTextArea info = new JTextArea(12, 30);

    private final JComboBox<Planet> targetPlanet = new JComboBox<Planet>();
    private final JSpinner warp = new JSpinner(new SpinnerNumberModel(6, 1, 10, 1));
    private final JComboBox<Waypoint.Task> task = new JComboBox<Waypoint.Task>(Waypoint.Task.values());

    private final JSpinner amount = new JSpinner(new SpinnerNumberModel(1000, 1, 1_000_000, 100));
    private final JComboBox<String> cargoKind =
            new JComboBox<String>(new String[] { "Colonists", "Ironium", "Boranium", "Germanium" });

    public FleetDialog(Frame owner, Galaxy galaxy, Player human) {
        super(owner, "Fleets — " + human.name(), true);
        this.galaxy = galaxy;
        this.human = human;
        setLayout(new BorderLayout(8, 8));

        for (Fleet f : galaxy.fleetsOf(human)) {
            fleetModel.addElement(f);
        }
        for (Planet p : galaxy.planets()) {
            targetPlanet.addItem(p);
        }
        fleetList.setPreferredSize(new Dimension(200, 360));
        fleetList.addListSelectionListener(e -> refreshInfo());
        add(new JScrollPane(fleetList), BorderLayout.WEST);

        info.setEditable(false);
        info.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        add(new JScrollPane(info), BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);

        if (!fleetModel.isEmpty()) {
            fleetList.setSelectedIndex(0);
        } else {
            refreshInfo();
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildControls() {
        JPanel root = new JPanel(new GridLayout(1, 2, 8, 8));

        JPanel wp = new JPanel(new GridLayout(0, 2, 4, 4));
        wp.setBorder(BorderFactory.createTitledBorder("Waypoint"));
        wp.add(new JLabel("Target:"));
        wp.add(targetPlanet);
        wp.add(new JLabel("Warp:"));
        wp.add(warp);
        wp.add(new JLabel("Task:"));
        wp.add(task);
        JButton addWp = new JButton("Add Waypoint");
        addWp.addActionListener(e -> addWaypoint());
        JButton clearWp = new JButton("Clear Waypoints");
        clearWp.addActionListener(e -> {
            Fleet f = fleetList.getSelectedValue();
            if (f != null) {
                f.waypoints().clear();
                refreshInfo();
            }
        });
        wp.add(addWp);
        wp.add(clearWp);
        root.add(wp);

        JPanel cargo = new JPanel(new GridLayout(0, 2, 4, 4));
        cargo.setBorder(BorderFactory.createTitledBorder("Cargo transfer (at orbited colony)"));
        cargo.add(new JLabel("Amount:"));
        cargo.add(amount);
        cargo.add(new JLabel("Type:"));
        cargo.add(cargoKind);
        JButton load = new JButton("Load ▲ (planet→fleet)");
        load.addActionListener(e -> transfer(true));
        JButton unload = new JButton("Unload ▼ (fleet→planet)");
        unload.addActionListener(e -> transfer(false));
        cargo.add(load);
        cargo.add(unload);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        cargo.add(new JLabel());
        cargo.add(close);
        root.add(cargo);
        return root;
    }

    private void addWaypoint() {
        Fleet f = fleetList.getSelectedValue();
        Planet target = (Planet) targetPlanet.getSelectedItem();
        if (f == null || target == null) {
            return;
        }
        f.waypoints().add(Waypoint.toPlanet(target, (Integer) warp.getValue(),
                (Waypoint.Task) task.getSelectedItem()));
        refreshInfo();
    }

    private Planet orbitedColony(Fleet f) {
        for (Planet p : galaxy.planetsOf(human)) {
            if (Math.abs(p.x() - f.x()) < 1.0 && Math.abs(p.y() - f.y()) < 1.0) {
                return p;
            }
        }
        return null;
    }

    private void transfer(boolean load) {
        Fleet f = fleetList.getSelectedValue();
        if (f == null) {
            return;
        }
        Planet p = orbitedColony(f);
        if (p == null) {
            info.append("\n(Not orbiting one of your colonies — cannot transfer.)");
            return;
        }
        long amt = ((Number) amount.getValue()).longValue();
        String kind = (String) cargoKind.getSelectedItem();
        Cargo c = f.cargo();
        if ("Colonists".equals(kind)) {
            if (load) {
                long moved = Math.min(amt, p.population());
                p.addPopulation(-moved);
                c.setColonists(c.colonists() + moved);
            } else {
                long moved = Math.min(amt, c.colonists());
                c.setColonists(c.colonists() - moved);
                p.addPopulation(moved);
            }
        } else {
            Mineral m = Mineral.valueOf(kind.toUpperCase());
            if (load) {
                long moved = Math.min(amt, p.surface(m));
                p.setSurface(m, p.surface(m) - moved);
                c.add(m, moved);
            } else {
                long moved = Math.min(amt, c.get(m));
                c.add(m, -moved);
                p.addSurface(m, moved);
            }
        }
        refreshInfo();
    }

    private void refreshInfo() {
        Fleet f = fleetList.getSelectedValue();
        if (f == null) {
            info.setText("Select a fleet.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(f.name()).append("  (fleet #").append(f.id()).append(")\n");
        sb.append("Position : ").append(Math.round(f.x())).append(", ").append(Math.round(f.y())).append('\n');
        sb.append("Fuel     : ").append(f.fuel()).append(" / ").append(f.fuelCapacity()).append('\n');
        sb.append("Mass     : ").append(f.totalMass()).append(" kT\n");
        sb.append("Max warp : ").append(f.maxWarp()).append('\n');
        sb.append("Ships    :\n");
        for (Map.Entry<ShipDesign, Integer> e : f.ships().entrySet()) {
            sb.append("   ").append(e.getValue()).append(" x ").append(e.getKey().name()).append('\n');
        }
        Cargo c = f.cargo();
        sb.append("Cargo    : I=").append(c.ironium()).append(" B=").append(c.boranium())
                .append(" G=").append(c.germanium()).append(" colonists=").append(c.colonists()).append('\n');
        sb.append("Waypoints: ").append(f.waypoints().size()).append('\n');
        List<Waypoint> wps = f.waypoints();
        for (int i = 0; i < wps.size(); i++) {
            Waypoint w = wps.get(i);
            sb.append("   ").append(i + 1).append(". -> (")
                    .append(Math.round(w.x())).append(",").append(Math.round(w.y()))
                    .append(") warp ").append(w.warp()).append(" [").append(w.task().label()).append("]\n");
        }
        Planet orbit = orbitedColony(f);
        sb.append("Orbiting : ").append(orbit == null ? "(deep space / not your colony)" : orbit.name());
        info.setText(sb.toString());
        info.setCaretPosition(0);
    }
}
