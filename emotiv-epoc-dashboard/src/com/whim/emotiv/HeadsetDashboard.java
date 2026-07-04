package com.whim.emotiv;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Live Emotiv EPOC v1 dashboard: contact quality, battery/signal, Affectiv,
 * Expressiv, Cognitiv (live + training + persistent profiles), CSV logging,
 * hardware/EmoComposer toggle, and raw 128 Hz EEG waveform.
 *
 * Pure consumer of {@link EmoEnginePoller.Snapshot}; touches Swing on the EDT only.
 */
public class HeadsetDashboard extends JFrame implements EmoEnginePoller.Listener {

    private static final Color[] CQ_COLORS = {
        Color.DARK_GRAY, new Color(200, 40, 40), new Color(230, 120, 30),
        new Color(220, 200, 40), new Color(60, 180, 75)
    };

    private final JLabel status  = new JLabel("Disconnected");
    private final JLabel battery = new JLabel("Battery: —");
    private final JLabel signal  = new JLabel("Signal: —");

    private final JPanel grid = new JPanel(new GridLayout(0, 4, 6, 6));
    private final Map<String, JLabel> cells = new HashMap<String, JLabel>();

    // Affectiv
    private final AffectivGauge gEngage = new AffectivGauge("Engagement", new Color(60, 130, 200));
    private final AffectivGauge gExST   = new AffectivGauge("Excitement (short)", new Color(210, 90, 40));
    private final AffectivGauge gExLT   = new AffectivGauge("Excitement (long)", new Color(150, 90, 40));
    private final AffectivGauge gMed    = new AffectivGauge("Meditation", new Color(90, 170, 120));
    private final AffectivGauge gFrust  = new AffectivGauge("Frustration", new Color(180, 60, 120));

    // Expressiv
    private final AffectivGauge gUpper = new AffectivGauge("Upper: neutral", new Color(120, 90, 180));
    private final AffectivGauge gLower = new AffectivGauge("Lower: neutral", new Color(90, 120, 180));
    private final JLabel[] lights = new JLabel[5];
    private static final String[] LIGHT_NAMES = { "blink", "wink L", "wink R", "look L", "look R" };
    private final javax.swing.Timer[] lightTimers = new javax.swing.Timer[5];

    // Cognitiv
    private final AffectivGauge gCog = new AffectivGauge("Command: neutral", new Color(200, 120, 60));
    private final JLabel trainStatus = new JLabel("Cognitiv: idle");

    // Controls
    private final JCheckBox useComposer = new JCheckBox("EmoComposer (emulator)");
    private final JToggleButton logBtn  = new JToggleButton("Log CSV");
    private final JCheckBox rawBox      = new JCheckBox("Raw EEG");

    private final DetectionLogger logger =
        new DetectionLogger(new DetectionLogger.Note() {
            public void status(String msg) { status.setText(msg); }
        });

    private WaveformPanel waves;
    private EmoEnginePoller poller;

    public HeadsetDashboard() {
        super("Emotiv EPOC v1 — Live Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildTopBar(), BorderLayout.NORTH);

        grid.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(grid, BorderLayout.CENTER);

        add(new JScrollPane(buildDetectionStack(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.EAST);

        add(buildSouth(), BorderLayout.SOUTH);

        setSize(1000, 900);
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- UI build

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        top.add(status, BorderLayout.WEST);

        final JButton start = new JButton("Connect");
        final JButton stop  = new JButton("Disconnect");
        stop.setEnabled(false);

        start.addActionListener(e -> {
            start.setEnabled(false); stop.setEnabled(true); useComposer.setEnabled(false);
            poller = new EmoEnginePoller(this, useComposer.isSelected());
            poller.setRawEnabled(rawBox.isSelected());
            java.io.File def = new java.io.File("default.emu");
            if (def.isFile()) poller.setAutoLoadProfile(def);
            poller.start();
        });
        stop.addActionListener(e -> {
            if (poller != null) poller.stop();
            logger.stop(); logBtn.setSelected(false);
            start.setEnabled(true); stop.setEnabled(false); useComposer.setEnabled(true);
        });

        logBtn.addActionListener(e -> {
            if (logBtn.isSelected()) {
                JFileChooser fc = new JFileChooser();
                if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    logger.start(fc.getSelectedFile());
                } else {
                    logBtn.setSelected(false);
                }
            } else {
                logger.stop();
                status.setText("Logging stopped");
            }
        });

        JPanel btns = new JPanel();
        btns.add(useComposer);
        btns.add(rawBox);
        btns.add(logBtn);
        btns.add(start);
        btns.add(stop);
        top.add(btns, BorderLayout.EAST);
        return top;
    }

    private JPanel buildDetectionStack() {
        JPanel aff = new JPanel(new GridLayout(0, 1, 0, 4));
        aff.setBorder(BorderFactory.createTitledBorder(
                "Affectiv (normalizes over first ~30–60s)"));
        aff.add(gEngage); aff.add(gExST); aff.add(gExLT); aff.add(gMed); aff.add(gFrust);

        JPanel face = new JPanel();
        face.setLayout(new BoxLayout(face, BoxLayout.Y_AXIS));
        face.setBorder(BorderFactory.createTitledBorder("Expressiv (facial)"));
        face.add(gUpper);
        face.add(gLower);
        JPanel lightRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        for (int i = 0; i < lights.length; i++) {
            final JLabel l = new JLabel(LIGHT_NAMES[i], SwingConstants.CENTER);
            l.setOpaque(true);
            l.setPreferredSize(new Dimension(56, 24));
            l.setBackground(Color.LIGHT_GRAY);
            l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            lights[i] = l;
            lightRow.add(l);
            final int idx = i;
            javax.swing.Timer t = new javax.swing.Timer(250, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    lights[idx].setBackground(Color.LIGHT_GRAY);
                }
            });
            t.setRepeats(false);
            lightTimers[i] = t;
        }
        face.add(lightRow);

        JPanel cog = new JPanel();
        cog.setLayout(new BoxLayout(cog, BoxLayout.Y_AXIS));
        cog.setBorder(BorderFactory.createTitledBorder("Cognitiv (mental commands)"));
        cog.add(gCog);
        cog.add(trainStatus);

        JPanel cogBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton bEnable = new JButton("Enable Push");
        JButton bNeutral = new JButton("Train Neutral");
        JButton bPush = new JButton("Train Push");
        JButton bAccept = new JButton("Accept");
        JButton bReject = new JButton("Reject");
        bEnable.addActionListener(e -> { if (poller != null)
            poller.cognitivSetActive(EdkConstants.COG_NEUTRAL | EdkConstants.COG_PUSH); });
        bNeutral.addActionListener(e -> { if (poller != null)
            poller.cognitivTrain(EdkConstants.COG_NEUTRAL, EdkConstants.COG_CTRL_START); });
        bPush.addActionListener(e -> { if (poller != null)
            poller.cognitivTrain(EdkConstants.COG_PUSH, EdkConstants.COG_CTRL_START); });
        bAccept.addActionListener(e -> { if (poller != null)
            poller.cognitivTrain(0, EdkConstants.COG_CTRL_ACCEPT); });
        bReject.addActionListener(e -> { if (poller != null)
            poller.cognitivTrain(0, EdkConstants.COG_CTRL_REJECT); });
        cogBtns.add(bEnable); cogBtns.add(bNeutral); cogBtns.add(bPush);
        cogBtns.add(bAccept); cogBtns.add(bReject);
        cog.add(cogBtns);

        JPanel profBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton bSave = new JButton("Save Profile");
        JButton bLoad = new JButton("Load Profile");
        bSave.addActionListener(e -> {
            if (poller == null) return;
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                poller.cognitivSaveProfile(fc.getSelectedFile());
            }
        });
        bLoad.addActionListener(e -> {
            if (poller == null) return;
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File f = fc.getSelectedFile();
                    byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
                    poller.cognitivLoadProfile(data);
                } catch (Exception ex) {
                    trainStatus.setText("Cognitiv: load failed " + ex.getMessage());
                }
            }
        });
        profBtns.add(bSave); profBtns.add(bLoad);
        cog.add(profBtns);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(aff);
        stack.add(face);
        stack.add(cog);
        return stack;
    }

    private JPanel buildSouth() {
        JPanel telemetry = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        telemetry.add(battery);
        telemetry.add(signal);

        waves = new WaveformPanel(EdkConstants.EEG_NAMES, 5);
        JScrollPane wavePane = new JScrollPane(waves);
        wavePane.setBorder(BorderFactory.createTitledBorder("Raw EEG (µV, DC removed)"));
        wavePane.setPreferredSize(new Dimension(560, 360));

        JPanel south = new JPanel(new BorderLayout());
        south.add(telemetry, BorderLayout.NORTH);
        south.add(wavePane, BorderLayout.CENTER);
        return south;
    }

    private JLabel cellFor(String name) {
        JLabel c = cells.get(name);
        if (c == null) {
            c = new JLabel(name, SwingConstants.CENTER);
            c.setOpaque(true);
            c.setPreferredSize(new Dimension(120, 54));
            c.setForeground(Color.WHITE);
            c.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            cells.put(name, c);
            grid.add(c);
            grid.revalidate();
        }
        return c;
    }

    private void flash(int idx, boolean on, Color c) {
        if (!on) return;
        lights[idx].setBackground(c);
        lightTimers[idx].restart();
    }

    // ---------------------------------------------------------------- Listener (EDT)

    public void onStatus(String message) { status.setText(message); }

    public void onSnapshot(EmoEnginePoller.Snapshot s) {
        for (int i = 0; i < s.names.length; i++) {
            int q = (s.quality[i] < 0 || s.quality[i] >= CQ_COLORS.length) ? 0 : s.quality[i];
            JLabel c = cellFor(s.names[i]);
            c.setBackground(CQ_COLORS[q]);
            c.setText("<html><center>" + s.names[i] +
                      "<br><small>" + EdkConstants.CQ_TEXT[q] + "</small></center></html>");
        }
        battery.setText("Battery: " + s.battery + " / " + s.batteryMax);
        int sig = (s.signal < 0 || s.signal >= EdkConstants.SIGNAL_TEXT.length) ? 0 : s.signal;
        signal.setText("Signal: " + EdkConstants.SIGNAL_TEXT[sig]);

        gEngage.setValue(s.engagement);
        gExST.setValue(s.excitementST);
        gExLT.setValue(s.excitementLT);
        gMed.setValue(s.meditation);
        gFrust.setValue(s.frustration);

        gUpper.setValue(s.upperPower);
        gLower.setValue(s.lowerPower);
        gUpper.setLabel("Upper: " + s.upperFace);
        gLower.setLabel("Lower: " + s.lowerFace);
        flash(0, s.blink, new Color(70, 140, 220));
        flash(1, s.winkL, new Color(70, 180, 120));
        flash(2, s.winkR, new Color(70, 180, 120));
        flash(3, s.lookL, new Color(210, 160, 60));
        flash(4, s.lookR, new Color(210, 160, 60));

        gCog.setLabel("Command: " + s.cognitivAction);
        gCog.setValue(s.cognitivPower);

        logger.log(s);
    }

    public void onTrainingEvent(String message) {
        trainStatus.setText("Cognitiv: " + message);
    }

    public void onRawEeg(String[] names, double[][] samples) {
        if (waves != null) waves.append(samples);
    }

    public void onDisconnected() {
        status.setText("Disconnected");
        battery.setText("Battery: —");
        signal.setText("Signal: —");
        gEngage.setValue(0); gExST.setValue(0); gExLT.setValue(0);
        gMed.setValue(0); gFrust.setValue(0);
        gUpper.setValue(0); gLower.setValue(0);
        gCog.setValue(0);
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new HeadsetDashboard().setVisible(true); }
        });
    }
}
