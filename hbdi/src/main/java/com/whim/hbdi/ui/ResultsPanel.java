package com.whim.hbdi.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.whim.hbdi.domain.Quadrant;
import com.whim.hbdi.domain.QuadrantScore;

/**
 * Final results card: a Graphics2D quadrant chart plus a numeric breakdown
 * and a "Save Report" button that exports the profile to a .txt file.
 */
public class ResultsPanel extends JPanel {

    private final QuadrantChartPanel chart = new QuadrantChartPanel();
    private final JPanel breakdown = new JPanel(new GridLayout(0, 1, 0, 6));
    private final JButton saveButton = new JButton("Save Report…");
    private final JButton restartButton = new JButton("Restart");

    private List<QuadrantScore> scores = new ArrayList<QuadrantScore>();

    public ResultsPanel(Runnable onRestart) {
        setLayout(new BorderLayout(16, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("Your HBDI Whole-Brain Profile");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        add(title, BorderLayout.NORTH);

        add(chart, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(260, 10));

        breakdown.setOpaque(false);
        breakdown.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        side.add(breakdown, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        restartButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (onRestart != null) onRestart.run();
            }
        });
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveReport();
            }
        });
        buttons.add(restartButton);
        buttons.add(saveButton);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Populate the chart and numeric breakdown from scoring output. */
    public void setScores(List<QuadrantScore> scores) {
        this.scores = (scores == null) ? new ArrayList<QuadrantScore>() : scores;
        chart.setScores(this.scores);

        breakdown.removeAll();
        JLabel heading = new JLabel("Breakdown");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        breakdown.add(heading);

        Map<Quadrant, QuadrantScore> byQuadrant = new EnumMap<Quadrant, QuadrantScore>(Quadrant.class);
        for (QuadrantScore s : this.scores) {
            if (s != null && s.getQuadrant() != null) {
                byQuadrant.put(s.getQuadrant(), s);
            }
        }
        for (Quadrant q : Quadrant.values()) {
            QuadrantScore s = byQuadrant.get(q);
            double pct = (s == null) ? 0.0 : s.getPercentage();
            double raw = (s == null) ? 0.0 : s.getRawScore();
            JLabel row = new JLabel(String.format(
                    "%s (%s): %.1f%%  ·  raw %.0f", q.name(), q.getLabel(), pct, raw));
            row.setFont(row.getFont().deriveFont(Font.PLAIN, 13f));
            breakdown.add(row);
        }
        Quadrant dominant = dominantQuadrant(byQuadrant);
        if (dominant != null) {
            JLabel dom = new JLabel("<html><br>Dominant preference: <b>" + dominant.name()
                    + " — " + dominant.getLabel() + "</b></html>");
            dom.setFont(dom.getFont().deriveFont(Font.PLAIN, 13f));
            breakdown.add(dom);
        }
        breakdown.revalidate();
        breakdown.repaint();
    }

    private static Quadrant dominantQuadrant(Map<Quadrant, QuadrantScore> byQuadrant) {
        Quadrant best = null;
        double bestPct = -1.0;
        for (Quadrant q : Quadrant.values()) {
            QuadrantScore s = byQuadrant.get(q);
            double pct = (s == null) ? 0.0 : s.getPercentage();
            if (pct > bestPct) {
                bestPct = pct;
                best = q;
            }
        }
        return best;
    }

    private void saveReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save HBDI Report");
        chooser.setSelectedFile(new File("hbdi-report.txt"));
        chooser.setFileFilter(new FileNameExtensionFilter("Text file (*.txt)", "txt"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new File(file.getParentFile(), file.getName() + ".txt");
        }
        try {
            writeReport(file);
            JOptionPane.showMessageDialog(this,
                    "Report saved to:\n" + file.getAbsolutePath(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save report:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writeReport(File file) throws IOException {
        Map<Quadrant, QuadrantScore> byQuadrant = new EnumMap<Quadrant, QuadrantScore>(Quadrant.class);
        for (QuadrantScore s : scores) {
            if (s != null && s.getQuadrant() != null) {
                byQuadrant.put(s.getQuadrant(), s);
            }
        }
        Writer w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(
                    new java.io.FileOutputStream(file), Charset.forName("UTF-8")));
            String nl = System.getProperty("line.separator");
            w.write("HBDI Whole-Brain Profile Report" + nl);
            w.write("================================" + nl);
            w.write(nl);
            w.write("Quadrant scores and percentages:" + nl);
            w.write(nl);
            for (Quadrant q : Quadrant.values()) {
                QuadrantScore s = byQuadrant.get(q);
                double pct = (s == null) ? 0.0 : s.getPercentage();
                double raw = (s == null) ? 0.0 : s.getRawScore();
                w.write(String.format("  %s  %-14s  raw=%8.2f   percentage=%6.2f%%%s",
                        q.name(), q.getLabel(), raw, pct, nl));
            }
            w.write(nl);
            Quadrant dominant = dominantQuadrant(byQuadrant);
            if (dominant != null) {
                w.write("Dominant preference: " + dominant.name() + " - " + dominant.getLabel() + nl);
            }
            w.write(nl);
            w.write("Legend: A=Analytical, B=Sequential, C=Interpersonal, D=Imaginative" + nl);
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }
}
