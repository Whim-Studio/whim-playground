package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Cost;
import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Ids;
import com.whim.oggalaxy.api.Result;
import com.whim.oggalaxy.api.FleetOrder;
import com.whim.oggalaxy.api.Views;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.EnumMap;
import java.util.Map;

/**
 * Modal fleet dispatch dialog: choose a mission, pick how many of each on-planet ship
 * to send, set cargo and speed, then hand a {@link FleetOrder} to the controller.
 */
public final class FleetDispatchDialog extends JDialog {

    private final GameController controller;
    private final StatusSink sink;
    private final Views.PlanetView origin;
    private final int tg, ts, tp;

    private final JComboBox<Ids.MissionType> missionBox = new JComboBox<Ids.MissionType>(Ids.MissionType.values());
    private final JCheckBox moonBox = new JCheckBox("Target moon");
    private final Map<Ids.ShipType, JSpinner> shipSpinners = new EnumMap<Ids.ShipType, JSpinner>(Ids.ShipType.class);
    private final JSpinner metalField = amountSpinner();
    private final JSpinner crystalField = amountSpinner();
    private final JSpinner deutField = amountSpinner();
    private final JSlider speed = new JSlider(10, 100, 100);
    private final JSpinner holdField = new JSpinner(new SpinnerNumberModel(1, 0, 99, 1));
    private final JLabel speedLabel = UiUtil.label("100%", Palette.ACCENT, Palette.FONT_BOLD);

    public FleetDispatchDialog(Window owner, GameController controller, StatusSink sink,
                               Views.PlanetView origin, int tg, int ts, int tp, boolean targetMoon) {
        super(owner, "Dispatch Fleet", ModalityType.APPLICATION_MODAL);
        this.controller = controller;
        this.sink = sink;
        this.origin = origin;
        this.tg = tg; this.ts = ts; this.tp = tp;

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Palette.BG_PANEL);
        root.setBorder(UiUtil.padded(10, 12, 10, 12));

        root.add(buildHeader(targetMoon), BorderLayout.NORTH);
        root.add(buildShipPicker(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        setSize(440, 560);
        setLocationRelativeTo(owner);
    }

    private static JSpinner amountSpinner() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0L, 0L, 9_999_999_999L, 1000L));
        sp.setPreferredSize(new Dimension(120, 24));
        return sp;
    }

    private JPanel buildHeader(boolean targetMoon) {
        JPanel h = new JPanel();
        h.setOpaque(false);
        h.setLayout(new BoxLayout(h, BoxLayout.Y_AXIS));

        String originText = origin == null ? "no origin planet"
                : origin.name() + " " + UiUtil.coords(origin.galaxy(), origin.system(), origin.position());
        JLabel from = UiUtil.label("From: " + originText, Palette.TEXT_DIM, Palette.FONT);
        JLabel to = UiUtil.label("To:   " + UiUtil.coords(tg, ts, tp), Palette.ACCENT, Palette.FONT_BOLD);
        from.setAlignmentX(LEFT_ALIGNMENT);
        to.setAlignmentX(LEFT_ALIGNMENT);
        h.add(from);
        h.add(to);
        h.add(Box.createVerticalStrut(6));

        JPanel line = new JPanel();
        line.setOpaque(false);
        line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
        line.setAlignmentX(LEFT_ALIGNMENT);
        line.add(UiUtil.label("Mission ", Palette.TEXT, Palette.FONT_BOLD));
        UiUtil.themeDark(missionBox);
        missionBox.setMaximumSize(new Dimension(180, 26));
        line.add(missionBox);
        line.add(Box.createHorizontalStrut(12));
        moonBox.setOpaque(false);
        moonBox.setForeground(Palette.TEXT);
        moonBox.setSelected(targetMoon);
        line.add(moonBox);
        line.add(Box.createHorizontalGlue());
        h.add(line);
        return h;
    }

    private JScrollPane buildShipPicker() {
        JPanel grid = new JPanel();
        grid.setOpaque(false);
        grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));

        boolean any = false;
        for (Ids.ShipType t : Ids.ShipType.values()) {
            int have = origin == null ? 0 : origin.shipCount(t);
            if (have <= 0) continue;
            any = true;
            JPanel row = new JPanel(new BorderLayout(6, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            GlyphIcon icon = new GlyphIcon(20).ship(t, Palette.ACCENT);
            icon.setPreferredSize(new Dimension(20, 20));
            row.add(icon, BorderLayout.WEST);
            row.add(UiUtil.label(controller.catalog().ship(t).name + "  (" + have + ")",
                    Palette.TEXT, Palette.FONT), BorderLayout.CENTER);
            JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, have, 1));
            sp.setPreferredSize(new Dimension(90, 24));
            JPanel spWrap = new JPanel(new BorderLayout());
            spWrap.setOpaque(false);
            JButton all = UiUtil.button("all", Palette.BORDER_HI);
            all.setFont(Palette.FONT_SMALL);
            final int cap = have;
            all.addActionListener(e -> sp.setValue(cap));
            spWrap.add(sp, BorderLayout.CENTER);
            spWrap.add(all, BorderLayout.EAST);
            row.add(spWrap, BorderLayout.EAST);
            shipSpinners.put(t, sp);
            grid.add(row);
            grid.add(Box.createVerticalStrut(3));
        }
        if (!any) {
            grid.add(UiUtil.label("No ships on this planet.", Palette.WARN, Palette.FONT));
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(grid, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(UiUtil.panelBorder("Ships"));
        sp.getViewport().setBackground(Palette.BG_PANEL);
        return sp;
    }

    private JPanel buildFooter() {
        JPanel f = new JPanel();
        f.setOpaque(false);
        f.setLayout(new BoxLayout(f, BoxLayout.Y_AXIS));

        JPanel cargo = new JPanel(new GridLayout(0, 2, 6, 4));
        cargo.setOpaque(false);
        cargo.setBorder(UiUtil.panelBorder("Cargo"));
        cargo.add(UiUtil.label("Metal", Palette.METAL, Palette.FONT));
        cargo.add(metalField);
        cargo.add(UiUtil.label("Crystal", Palette.CRYSTAL, Palette.FONT));
        cargo.add(crystalField);
        cargo.add(UiUtil.label("Deuterium", Palette.DEUTERIUM, Palette.FONT));
        cargo.add(deutField);
        cargo.setAlignmentX(LEFT_ALIGNMENT);
        f.add(cargo);
        f.add(Box.createVerticalStrut(6));

        JPanel opts = new JPanel(new BorderLayout(8, 0));
        opts.setOpaque(false);
        opts.setAlignmentX(LEFT_ALIGNMENT);
        JPanel speedWrap = new JPanel(new BorderLayout(6, 0));
        speedWrap.setOpaque(false);
        speedWrap.add(UiUtil.label("Speed", Palette.TEXT, Palette.FONT_BOLD), BorderLayout.WEST);
        speed.setOpaque(false);
        speed.setMajorTickSpacing(10);
        speed.setSnapToTicks(true);
        speed.addChangeListener(e -> speedLabel.setText(speed.getValue() + "%"));
        speedWrap.add(speed, BorderLayout.CENTER);
        speedWrap.add(speedLabel, BorderLayout.EAST);
        opts.add(speedWrap, BorderLayout.CENTER);

        JPanel holdWrap = new JPanel(new BorderLayout(4, 0));
        holdWrap.setOpaque(false);
        holdWrap.add(UiUtil.label("Hold (h)", Palette.TEXT_DIM, Palette.FONT_SMALL), BorderLayout.WEST);
        holdField.setPreferredSize(new Dimension(56, 24));
        holdWrap.add(holdField, BorderLayout.EAST);
        opts.add(holdWrap, BorderLayout.EAST);
        f.add(opts);
        f.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        JButton cancel = UiUtil.button("Cancel", Palette.BORDER_HI);
        JButton dispatch = UiUtil.button("Dispatch", Palette.OK);
        cancel.addActionListener(e -> dispose());
        dispatch.addActionListener(e -> doDispatch());
        buttons.add(cancel, BorderLayout.WEST);
        buttons.add(dispatch, BorderLayout.EAST);
        f.add(buttons);
        return f;
    }

    private long spinnerLong(JSpinner sp) {
        Object v = sp.getValue();
        return v instanceof Number ? ((Number) v).longValue() : 0L;
    }

    private void doDispatch() {
        if (origin == null) {
            sink.status("No origin planet selected", false);
            dispose();
            return;
        }
        Map<Ids.ShipType, Integer> ships = new EnumMap<Ids.ShipType, Integer>(Ids.ShipType.class);
        int total = 0;
        for (Map.Entry<Ids.ShipType, JSpinner> e : shipSpinners.entrySet()) {
            int n = ((Number) e.getValue().getValue()).intValue();
            if (n > 0) {
                ships.put(e.getKey(), n);
                total += n;
            }
        }
        if (total <= 0) {
            sink.status("Select at least one ship to send", false);
            return;
        }
        Cost cargo = new Cost(spinnerLong(metalField), spinnerLong(crystalField), spinnerLong(deutField));
        Ids.MissionType mission = (Ids.MissionType) missionBox.getSelectedItem();
        int hold = ((Number) holdField.getValue()).intValue();
        FleetOrder order = new FleetOrder(origin.id(), tg, ts, tp, moonBox.isSelected(),
                mission, ships, cargo, speed.getValue(), hold);
        Result r = controller.dispatchFleet(order);
        sink.status(mission + " → " + UiUtil.coords(tg, ts, tp) + ": "
                + (r.message.isEmpty() ? (r.ok ? "dispatched" : "failed") : r.message), r.ok);
        sink.requestRefresh();
        if (r.ok) dispose();
    }
}
