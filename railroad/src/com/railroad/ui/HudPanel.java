package com.railroad.ui;

import com.railroad.logic.GameClock;
import com.railroad.model.Cargo;
import com.railroad.model.CargoType;
import com.railroad.model.Economy;
import com.railroad.model.GameState;
import com.railroad.model.Train;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * The heads-up display: a control toolbar plus a status bar. Shows the treasury,
 * in-game date, selected tool and trip count, and provides the tool toggle,
 * buy-train, pause/resume and speed controls. All actions go through the
 * {@link GameController}; {@link #refresh()} re-reads model state into the labels.
 */
public final class HudPanel extends JPanel {

    private final GameController controller;
    private final Runnable repaintMap;

    private final JButton selectBtn = new JButton("Select");
    private final JButton buildBtn = new JButton("Build Track");
    private final JButton stationBtn = new JButton("Build Station ($" + Economy.STATION_COST + ")");
    private final JButton buyTrainBtn = new JButton("Buy Train ($" + GameState.TRAIN_COST + ")");
    private final JButton pauseBtn = new JButton("Start");
    private final JButton speedBtn = new JButton("Speed: 1x");

    private final JLabel cashLabel = new JLabel();
    private final JLabel dateLabel = new JLabel();
    private final JLabel toolLabel = new JLabel();
    private final JLabel tripsLabel = new JLabel();
    private final JLabel loadLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();

    public HudPanel(GameController controller, Runnable repaintMap) {
        this.controller = controller;
        this.repaintMap = repaintMap;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(buildToolbar());
        add(buildStatusBar());
        wireActions();
        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.add(new JLabel("Tools:"));
        bar.add(selectBtn);
        bar.add(buildBtn);
        bar.add(stationBtn);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(buyTrainBtn);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(new JLabel("Clock:"));
        bar.add(pauseBtn);
        bar.add(speedBtn);
        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));
        Font mono = new Font(Font.MONOSPACED, Font.BOLD, 13);
        cashLabel.setFont(mono);
        dateLabel.setFont(mono);
        toolLabel.setFont(mono);
        tripsLabel.setFont(mono);
        loadLabel.setFont(mono);
        bar.add(cashLabel);
        bar.add(dateLabel);
        bar.add(toolLabel);
        bar.add(tripsLabel);
        bar.add(loadLabel);

        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.add(bar);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        wrap.add(statusLabel);
        return wrap;
    }

    private void wireActions() {
        selectBtn.addActionListener(e -> {
            controller.setCurrentTool(Tool.SELECT);
            refresh();
        });
        buildBtn.addActionListener(e -> {
            controller.setCurrentTool(Tool.BUILD_TRACK);
            refresh();
        });
        stationBtn.addActionListener(e -> {
            controller.setCurrentTool(Tool.BUILD_STATION);
            refresh();
        });
        buyTrainBtn.addActionListener(e -> {
            controller.buyTrainOnFirstConnectedRoute();
            refresh();
            repaintMap.run();
        });
        pauseBtn.addActionListener(e -> {
            controller.toggleRunning();
            refresh();
        });
        speedBtn.addActionListener(e -> {
            GameClock clock = controller.getClock();
            int next = clock.getSpeedMultiplier() * 2;
            if (next > 4) {
                next = 1;
            }
            clock.setSpeedMultiplier(next);
            refresh();
        });
    }

    /** Re-reads model state into the labels and button text. */
    public void refresh() {
        GameState s = controller.getState();
        cashLabel.setText("Treasury: $" + s.getCompany().getCash());
        dateLabel.setText("Date: " + s.getDate().format());
        toolLabel.setText("Tool: " + controller.getCurrentTool().getLabel());
        String trips = "Trips: " + s.getCompletedTrips();
        if (s.getLastDeliveryRevenue() > 0) {
            trips += " (last +$" + s.getLastDeliveryRevenue() + ")";
        }
        tripsLabel.setText(trips);
        loadLabel.setText(describeLoad());
        pauseBtn.setText(controller.getClock().isRunning() ? "Pause" : "Start");
        speedBtn.setText("Speed: " + controller.getClock().getSpeedMultiplier() + "x");
        statusLabel.setText(controller.getStatusMessage());
    }

    /** Summarises the selected train's current cargo load for the HUD. */
    private String describeLoad() {
        Train t = controller.getSelectedTrain();
        if (t == null) {
            return "Load: (no train)";
        }
        Map<CargoType, Integer> counts = new EnumMap<CargoType, Integer>(CargoType.class);
        for (Cargo c : t.getHold()) {
            Integer prev = counts.get(c.getType());
            counts.put(c.getType(), prev == null ? 1 : prev + 1);
        }
        StringBuilder sb = new StringBuilder("Load ");
        sb.append(t.loadCount()).append("/").append(t.getCapacity());
        if (!counts.isEmpty()) {
            sb.append(" [");
            boolean first = true;
            for (Map.Entry<CargoType, Integer> e : counts.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(e.getValue()).append(" ").append(e.getKey().getLabel());
                first = false;
            }
            sb.append("]");
        }
        return sb.toString();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
