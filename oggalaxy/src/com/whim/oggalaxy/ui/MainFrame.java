package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.GameController;
import com.whim.oggalaxy.api.Views;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * The main game shell: top resource strip + clock controls, a left planet selector, a
 * centre {@link JTabbedPane} of the six game screens, and a bottom colour-coded event
 * log. A single Swing {@link Timer} polls {@code controller.state()} and pushes the
 * snapshot to every child panel — no game logic runs here.
 */
public final class MainFrame extends JFrame implements StatusSink {

    private final GameController controller;

    private final ResourceBar resourceBar = new ResourceBar();
    private final OverviewPanel overview;
    private final BuildingsPanel buildings;
    private final ResearchPanel research;
    private final ShipyardPanel shipyard;
    private final GalaxyPanel galaxy;
    private final ReportsPanel reports = new ReportsPanel();
    private final LogPanel logPanel = new LogPanel();
    private final List<Refreshable> refreshables = new ArrayList<Refreshable>();

    private final DefaultListModel<Views.PlanetView> planetModel = new DefaultListModel<Views.PlanetView>();
    private final JList<Views.PlanetView> planetList = new JList<Views.PlanetView>(planetModel);
    private String planetSig = "";

    private final JLabel clockLabel = UiUtil.label("T+0h", Palette.ACCENT, Palette.FONT_BOLD);
    private final JLabel slotsLabel = UiUtil.label("Fleets 0/1", Palette.TEXT_DIM, Palette.FONT_SMALL);
    private final JLabel statusLabel = UiUtil.label(" ", Palette.TEXT_DIM, Palette.FONT_SMALL);
    private final JButton pauseButton = UiUtil.button("Resume", Palette.OK);
    private final JComboBox<String> speedBox = new JComboBox<String>(
            new String[]{"1x", "2x", "5x", "10x", "30x", "60x"});
    private final int[] speedValues = {1, 2, 5, 10, 30, 60};

    private boolean suppressPlanetEvent;
    private final Timer poll;

    public MainFrame(GameController controller) {
        super("OG Galaxy");
        this.controller = controller;
        this.overview = new OverviewPanel();
        this.buildings = new BuildingsPanel(controller, this);
        this.research = new ResearchPanel(controller, this);
        this.shipyard = new ShipyardPanel(controller, this);
        this.galaxy = new GalaxyPanel(controller, this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Palette.BG_DEEP);
        root.add(buildTopBar(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UiUtil.themeDark(tabs);
        tabs.addTab("Overview", overview);
        tabs.addTab("Buildings", buildings);
        tabs.addTab("Research", research);
        tabs.addTab("Shipyard", shipyard);
        tabs.addTab("Galaxy", galaxy);
        tabs.addTab("Reports", reports);

        JSplitPane centreSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, logPanel);
        centreSplit.setResizeWeight(0.76);
        centreSplit.setDividerLocation(500);
        centreSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildPlanetSelector(), centreSplit);
        mainSplit.setDividerLocation(210);
        mainSplit.setBorder(null);
        root.add(mainSplit, BorderLayout.CENTER);

        setContentPane(root);

        refreshables.add(resourceBar);
        refreshables.add(overview);
        refreshables.add(buildings);
        refreshables.add(research);
        refreshables.add(shipyard);
        refreshables.add(galaxy);
        refreshables.add(reports);
        refreshables.add(logPanel);

        poll = new Timer(150, e -> pollAndRefresh());
        poll.setInitialDelay(0);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Palette.BG_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.BORDER));
        bar.add(resourceBar, BorderLayout.CENTER);
        bar.add(buildClockControls(), BorderLayout.EAST);

        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Palette.BG_PANEL);
        south.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.BORDER));
        statusLabel.setBorder(UiUtil.padded(1, 10, 2, 10));
        south.add(statusLabel, BorderLayout.WEST);

        JPanel stack = new JPanel(new BorderLayout());
        stack.add(bar, BorderLayout.NORTH);
        stack.add(south, BorderLayout.SOUTH);
        return stack;
    }

    private JPanel buildClockControls() {
        JPanel c = new JPanel();
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.X_AXIS));
        c.setBorder(UiUtil.padded(2, 8, 2, 8));

        JPanel clockCol = new JPanel();
        clockCol.setOpaque(false);
        clockCol.setLayout(new BoxLayout(clockCol, BoxLayout.Y_AXIS));
        clockLabel.setAlignmentX(RIGHT_ALIGNMENT);
        slotsLabel.setAlignmentX(RIGHT_ALIGNMENT);
        clockCol.add(clockLabel);
        clockCol.add(slotsLabel);
        c.add(clockCol);
        c.add(Box.createHorizontalStrut(10));

        JButton advance = UiUtil.button("Advance ▶", Palette.ACCENT);
        advance.setToolTipText("Advance the simulation by one hour");
        advance.addActionListener(e -> {
            controller.advance(1);
            pollAndRefresh();
            status("Advanced 1 hour", true);
        });
        pauseButton.addActionListener(e -> toggleClock());

        UiUtil.themeDark(speedBox);
        speedBox.setMaximumSize(new Dimension(64, 28));
        speedBox.setSelectedIndex(0);
        speedBox.addActionListener(e -> {
            int idx = speedBox.getSelectedIndex();
            if (idx >= 0) controller.setSpeed(speedValues[idx]);
        });

        c.add(pauseButton);
        c.add(Box.createHorizontalStrut(4));
        c.add(advance);
        c.add(Box.createHorizontalStrut(4));
        c.add(UiUtil.label("Speed", Palette.TEXT_DIM, Palette.FONT_SMALL));
        c.add(Box.createHorizontalStrut(2));
        c.add(speedBox);
        return c;
    }

    private void toggleClock() {
        if (controller.isClockRunning()) {
            controller.stopClock();
        } else {
            controller.startClock();
        }
        syncPauseButton();
    }

    private void syncPauseButton() {
        boolean running = controller.isClockRunning();
        pauseButton.setText(running ? "Pause" : "Resume");
    }

    private JPanel buildPlanetSelector() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Palette.BG_PANEL);
        wrap.setBorder(UiUtil.panelBorder("Planets"));

        planetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        planetList.setBackground(Palette.BG_PANEL);
        planetList.setCellRenderer(new PlanetRenderer());
        planetList.addListSelectionListener(e -> {
            if (suppressPlanetEvent || e.getValueIsAdjusting()) return;
            Views.PlanetView p = planetList.getSelectedValue();
            if (p != null) {
                controller.selectPlanet(p.id());
                pollAndRefresh();
            }
        });
        JScrollPane sp = new JScrollPane(planetList);
        sp.setBorder(null);
        sp.getViewport().setBackground(Palette.BG_PANEL);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    /** Start polling and show the window. */
    public void launch() {
        syncPauseButton();
        pollAndRefresh();
        poll.start();
        setVisible(true);
    }

    private void pollAndRefresh() {
        Views.GameStateView state = controller.state();
        if (state == null) return;
        clockLabel.setText(state.formattedTime());
        slotsLabel.setText("Fleets " + state.usedFleetSlots() + "/" + state.maxFleetSlots());
        syncPauseButton();
        updatePlanetList(state);
        for (Refreshable r : refreshables) {
            r.refresh(state);
        }
    }

    private void updatePlanetList(Views.GameStateView state) {
        Views.EmpireView player = state.player();
        List<Views.PlanetView> planets = player == null
                ? new ArrayList<Views.PlanetView>() : player.planets();
        StringBuilder sig = new StringBuilder();
        for (Views.PlanetView p : planets) sig.append(p.id()).append('|');
        if (!sig.toString().equals(planetSig)) {
            planetSig = sig.toString();
            suppressPlanetEvent = true;
            planetModel.clear();
            for (Views.PlanetView p : planets) planetModel.addElement(p);
            suppressPlanetEvent = false;
        }
        String selId = state.selectedPlanetId();
        if (selId != null) {
            for (int i = 0; i < planetModel.size(); i++) {
                if (selId.equals(planetModel.get(i).id())) {
                    if (planetList.getSelectedIndex() != i) {
                        suppressPlanetEvent = true;
                        planetList.setSelectedIndex(i);
                        suppressPlanetEvent = false;
                    }
                    break;
                }
            }
        }
    }

    // ---- StatusSink ----
    @Override
    public void status(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setForeground(ok ? Palette.OK : Palette.BAD);
    }

    @Override
    public void requestRefresh() {
        SwingUtilities.invokeLater(this::pollAndRefresh);
    }

    private final class PlanetRenderer extends JLabel implements ListCellRenderer<Views.PlanetView> {
        PlanetRenderer() {
            setOpaque(true);
            setBorder(UiUtil.padded(4, 8, 4, 8));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Views.PlanetView> list,
                Views.PlanetView value, int index, boolean selected, boolean focus) {
            setText("<html><b>" + value.name() + "</b>"
                    + (value.isMoon() ? " [M]" : "")
                    + "<br><span style='color:#8c98b2'>"
                    + UiUtil.coords(value.galaxy(), value.system(), value.position())
                    + "</span></html>");
            setForeground(Palette.TEXT);
            setBackground(selected ? Palette.mix(Palette.BG_PANEL, Palette.ACCENT, 0.28) : Palette.BG_PANEL);
            return this;
        }
    }
}
