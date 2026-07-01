package com.whim.colony.ui;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Job;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;
import com.whim.colony.domain.Resources;
import com.whim.colony.domain.SkillType;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

/**
 * READ-ONLY side panel showing colony {@link Resources}, the SELECTED colonist's
 * {@link Needs} (as bars) plus current job and skills, and a scrolling message
 * log read from {@link ColonyState#getMessageLog()}.
 *
 * <p>All values are pulled fresh in {@link #refresh()} — the panel holds no
 * simulation state and computes nothing. {@link GameFrame} calls {@code refresh}
 * from its view timer.
 */
public class HudPanel extends JPanel {

    private static final Color BG = new Color(32, 34, 40);
    private static final Color FG = new Color(225, 227, 232);
    private static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    private ColonyState state;
    private MapPanel mapPanel; // source of the current selection (view-only)

    private final JLabel tickLabel = makeLabel("Tick: 0");
    private final JLabel pausedLabel = makeLabel("");

    private final JLabel foodLabel = makeLabel("Food: 0");
    private final JLabel steelLabel = makeLabel("Steel: 0");
    private final JLabel woodLabel = makeLabel("Wood: 0");

    private final JLabel colonistNameLabel = makeLabel("No colonist selected");
    private final JLabel jobLabel = makeLabel("Job: —");
    private final NeedBar hungerBar = new NeedBar("Hunger");
    private final NeedBar restBar = new NeedBar("Rest");
    private final NeedBar moodBar = new NeedBar("Mood");
    private final JLabel skillsLabel = makeLabel(" ");

    private final JTextArea logArea = new JTextArea();

    public HudPanel(ColonyState state, MapPanel mapPanel) {
        this.state = state;
        this.mapPanel = mapPanel;
        setBackground(BG);
        setPreferredSize(new Dimension(240, 640));
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildTopSection(), BorderLayout.NORTH);
        add(buildLogSection(), BorderLayout.CENTER);

        refresh();
    }

    public void setState(ColonyState state) {
        this.state = state;
        refresh();
    }

    private JComponent buildTopSection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(header("COLONY"));
        panel.add(tickLabel);
        panel.add(pausedLabel);
        panel.add(Box.createVerticalStrut(6));

        panel.add(header("RESOURCES"));
        panel.add(foodLabel);
        panel.add(steelLabel);
        panel.add(woodLabel);
        panel.add(Box.createVerticalStrut(6));

        panel.add(header("SELECTED COLONIST"));
        panel.add(colonistNameLabel);
        panel.add(jobLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(hungerBar);
        panel.add(restBar);
        panel.add(moodBar);
        panel.add(Box.createVerticalStrut(4));
        panel.add(skillsLabel);

        return panel;
    }

    private JComponent buildLogSection() {
        JPanel wrap = new JPanel(new BorderLayout(0, 2));
        wrap.setOpaque(false);
        wrap.add(header("MESSAGE LOG"), BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(new Color(22, 23, 28));
        logArea.setForeground(FG);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        logArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(60, 62, 70)));
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    // ------------------------------------------------------------------
    // Refresh — reads current state, mutates nothing
    // ------------------------------------------------------------------

    public void refresh() {
        if (state == null) {
            return;
        }
        tickLabel.setText("Tick: " + state.getTick());
        pausedLabel.setText(state.isPaused() ? "[PAUSED]" : "[running]");
        pausedLabel.setForeground(state.isPaused() ? new Color(240, 200, 90) : new Color(140, 200, 140));

        Resources r = state.getResources();
        if (r != null) {
            foodLabel.setText("Food:  " + r.getFood());
            steelLabel.setText("Steel: " + r.getSteel());
            woodLabel.setText("Wood:  " + r.getWood());
        }

        Colonist c = mapPanel != null ? mapPanel.getSelectedColonist() : null;
        if (c == null) {
            colonistNameLabel.setText("No colonist selected");
            jobLabel.setText("Job: —");
            hungerBar.setValue(-1);
            restBar.setValue(-1);
            moodBar.setValue(-1);
            skillsLabel.setText(" ");
        } else {
            colonistNameLabel.setText(c.getName() + "  (" + c.getX() + "," + c.getY() + ")");
            Job job = c.getCurrentJob();
            jobLabel.setText("Job: " + (job != null ? job.getName() : "idle"));
            Needs n = c.getNeeds();
            if (n != null) {
                hungerBar.setValue(n.getHunger());
                restBar.setValue(n.getRest());
                moodBar.setValue(n.getMood());
            }
            skillsLabel.setText(formatSkills(c));
        }

        updateLog();
    }

    private String formatSkills(Colonist c) {
        if (c.getSkills() == null) {
            return " ";
        }
        Map<SkillType, Integer> map = c.getSkills().asMap();
        StringBuilder sb = new StringBuilder("<html>Skills:<br>");
        boolean any = false;
        for (Map.Entry<SkillType, Integer> e : map.entrySet()) {
            if (e.getValue() != null && e.getValue().intValue() > 0) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("<br>");
                any = true;
            }
        }
        if (!any) {
            sb.append("(untrained)");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private void updateLog() {
        List<String> messages = state.getMessageLog();
        StringBuilder sb = new StringBuilder();
        // show the most recent messages last (natural log order)
        int start = Math.max(0, messages.size() - 100);
        for (int i = start; i < messages.size(); i++) {
            sb.append(messages.get(i)).append('\n');
        }
        String text = sb.toString();
        if (!text.equals(logArea.getText())) {
            logArea.setText(text);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(FG);
        l.setFont(LABEL_FONT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel header(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(150, 190, 240));
        l.setFont(HEADER_FONT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        return l;
    }

    /**
     * A labeled horizontal bar for one need in [0..100]. A value &lt; 0 renders
     * as empty ("no selection"). Color goes red below the critical threshold,
     * orange below the low threshold, otherwise green.
     */
    private static final class NeedBar extends JComponent {
        private final String name;
        private double value = -1;

        NeedBar(String name) {
            this.name = name;
            setPreferredSize(new Dimension(210, 20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            setFont(LABEL_FONT);
        }

        void setValue(double value) {
            this.value = value;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int barX = 54;
                int barW = w - barX - 4;
                int barH = h - 6;
                int barY = 3;

                g2.setColor(FG);
                g2.setFont(LABEL_FONT);
                g2.drawString(name, 0, h - 6);

                // track
                g2.setColor(new Color(18, 19, 24));
                g2.fillRect(barX, barY, barW, barH);

                if (value >= 0) {
                    double t = value / Needs.MAX;
                    if (t < 0) {
                        t = 0;
                    }
                    if (t > 1) {
                        t = 1;
                    }
                    int fillW = (int) (barW * t);
                    g2.setColor(needColor(value));
                    g2.fillRect(barX, barY, fillW, barH);
                    g2.setColor(FG);
                    g2.drawString(Integer.toString((int) Math.round(value)), barX + barW - 24, h - 6);
                }

                g2.setColor(new Color(70, 72, 80));
                g2.drawRect(barX, barY, barW, barH);
            } finally {
                g2.dispose();
            }
        }

        private static Color needColor(double value) {
            if (value <= Needs.CRITICAL_THRESHOLD) {
                return new Color(200, 70, 70);
            }
            if (value <= Needs.LOW_THRESHOLD) {
                return new Color(220, 160, 70);
            }
            return new Color(90, 180, 100);
        }
    }
}
