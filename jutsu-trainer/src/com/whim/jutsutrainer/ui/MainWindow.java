package com.whim.jutsutrainer.ui;

import com.whim.jutsutrainer.domain.*;
import com.whim.jutsutrainer.engine.JutsuService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing UI for the Jutsu Database & Seal Trainer.
 *
 * <p>Two tabs: a searchable/filterable database view and an interactive
 * "Training Dojo" where the user clicks hand-seal buttons to build a sequence
 * and the app live-filters every jutsu whose seal list begins with (or exactly
 * matches) the input.
 */
public final class MainWindow extends JFrame {

    private final JutsuService service;

    // ----- Database tab widgets -----
    private JTextField searchField;
    private JComboBox<NatureItem> natureFilter;
    private DefaultListModel<Jutsu> dbListModel;
    private JList<Jutsu> dbList;
    private JTextArea detailArea;

    // ----- Training Dojo tab widgets -----
    private final List<HandSeal> currentSequence = new ArrayList<HandSeal>();
    private JLabel sequenceLabel;
    private DefaultListModel<Jutsu> dojoListModel;
    private JList<Jutsu> dojoList;

    public MainWindow(JutsuService service) {
        super("Jutsu Database & Seal Trainer");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 640);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Database", buildDatabaseTab());
        tabs.addTab("Training Dojo", buildDojoTab());
        add(tabs, BorderLayout.CENTER);

        // Initial population.
        refreshDatabase();
        refreshDojo();
    }

    // ================= Database tab =================

    private JComponent buildDatabaseTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Top: search + filter ---
        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        top.add(new JLabel("Name:"), gc);

        searchField = new JTextField();
        gc.gridx = 1; gc.weightx = 1.0;
        top.add(searchField, gc);

        gc.gridx = 2; gc.weightx = 0;
        top.add(new JLabel("Nature:"), gc);

        natureFilter = new JComboBox<NatureItem>();
        natureFilter.addItem(new NatureItem(null)); // "All"
        ChakraNature[] natures = ChakraNature.values();
        for (int i = 0; i < natures.length; i++) {
            natureFilter.addItem(new NatureItem(natures[i]));
        }
        gc.gridx = 3; gc.weightx = 0.4;
        top.add(natureFilter, gc);

        root.add(top, BorderLayout.NORTH);

        // --- Center: results list | detail ---
        dbListModel = new DefaultListModel<Jutsu>();
        dbList = new JList<Jutsu>(dbListModel);
        dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dbList.setCellRenderer(new JutsuCellRenderer());

        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane listScroll = new JScrollPane(dbList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Results"));
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, detailScroll);
        split.setResizeWeight(0.4);
        root.add(split, BorderLayout.CENTER);

        // --- Listeners (live update) ---
        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshDatabase(); }
            public void removeUpdate(DocumentEvent e) { refreshDatabase(); }
            public void changedUpdate(DocumentEvent e) { refreshDatabase(); }
        };
        searchField.getDocument().addDocumentListener(docListener);

        natureFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) { refreshDatabase(); }
        });

        dbList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) { showDetail(dbList.getSelectedValue()); }
            }
        });

        return root;
    }

    private void refreshDatabase() {
        String name = searchField.getText();
        NatureItem item = (NatureItem) natureFilter.getSelectedItem();
        ChakraNature nature = (item == null) ? null : item.nature;

        List<Jutsu> results = service.search(name, nature);
        dbListModel.clear();
        for (int i = 0; i < results.size(); i++) {
            dbListModel.addElement(results.get(i));
        }
        if (!dbListModel.isEmpty()) {
            dbList.setSelectedIndex(0);
        } else {
            showDetail(null);
        }
    }

    private void showDetail(Jutsu j) {
        if (j == null) {
            detailArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Name:   ").append(j.getName()).append('\n');
        sb.append("Nature: ").append(j.getNature().getDisplayName()).append('\n');
        sb.append("Rank:   ").append(j.getRank()).append('\n');
        String user = j.getUser();
        sb.append("User:   ").append((user == null || user.isEmpty()) ? "—" : user).append('\n');
        sb.append('\n');
        sb.append(j.getDescription()).append('\n');
        sb.append('\n');
        sb.append("Seals:  ").append(formatSeals(j.getSeals())).append('\n');
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    // ================= Training Dojo tab =================

    private JComponent buildDojoTab() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Left: seal button grid ---
        HandSeal[] seals = HandSeal.values();
        JPanel grid = new JPanel(new GridLayout(0, 3, 4, 4));
        for (int i = 0; i < seals.length; i++) {
            final HandSeal seal = seals[i];
            JButton b = new JButton(seal.getDisplayName());
            b.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    currentSequence.add(seal);
                    refreshDojo();
                }
            });
            grid.add(b);
        }
        JPanel gridWrap = new JPanel(new BorderLayout());
        gridWrap.setBorder(BorderFactory.createTitledBorder("Hand Seals"));
        gridWrap.add(grid, BorderLayout.NORTH);
        root.add(new JScrollPane(gridWrap), BorderLayout.WEST);

        // --- Center: current sequence + results ---
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel seqPanel = new JPanel(new BorderLayout(8, 8));
        seqPanel.setBorder(BorderFactory.createTitledBorder("Current Sequence"));
        sequenceLabel = new JLabel(" ");
        sequenceLabel.setFont(sequenceLabel.getFont().deriveFont(Font.BOLD, 16f));
        seqPanel.add(sequenceLabel, BorderLayout.CENTER);

        JButton clear = new JButton("Clear Sequence");
        clear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                currentSequence.clear();
                refreshDojo();
            }
        });
        seqPanel.add(clear, BorderLayout.EAST);
        center.add(seqPanel, BorderLayout.NORTH);

        dojoListModel = new DefaultListModel<Jutsu>();
        dojoList = new JList<Jutsu>(dojoListModel);
        dojoList.setCellRenderer(new JutsuCellRenderer());
        JScrollPane resultsScroll = new JScrollPane(dojoList);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Matching Jutsu"));
        center.add(resultsScroll, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private void refreshDojo() {
        // Update the visible sequence text.
        if (currentSequence.isEmpty()) {
            sequenceLabel.setText("(click hand seals below)");
        } else {
            sequenceLabel.setText(formatSeals(currentSequence));
        }

        // Compute matches and mark which are exact full matches.
        List<Jutsu> prefixMatches = service.matchPrefix(currentSequence);
        List<Jutsu> exactMatches = service.matchExact(currentSequence);

        dojoListModel.clear();
        for (int i = 0; i < prefixMatches.size(); i++) {
            dojoListModel.addElement(prefixMatches.get(i));
        }
        // Tell the renderer which jutsu are exact matches.
        ((JutsuCellRenderer) dojoList.getCellRenderer()).setExactMatches(exactMatches);
        dojoList.repaint();
    }

    // ================= Helpers =================

    private static String formatSeals(List<HandSeal> seals) {
        if (seals == null || seals.isEmpty()) {
            return "(no seals)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seals.size(); i++) {
            if (i > 0) { sb.append(" → "); } // → arrow
            sb.append(seals.get(i).getDisplayName());
        }
        return sb.toString();
    }

    /** Wrapper so the "All" option (null nature) renders nicely in the combo box. */
    private static final class NatureItem {
        final ChakraNature nature;
        NatureItem(ChakraNature nature) { this.nature = nature; }
        public String toString() {
            return (nature == null) ? "All" : nature.getDisplayName();
        }
    }

    /** Renders a Jutsu by name, bolding/marking exact matches in the dojo list. */
    private static final class JutsuCellRenderer extends DefaultListCellRenderer {
        private List<Jutsu> exactMatches = new ArrayList<Jutsu>();

        void setExactMatches(List<Jutsu> exact) {
            this.exactMatches = (exact == null) ? new ArrayList<Jutsu>() : exact;
        }

        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Jutsu) {
                Jutsu j = (Jutsu) value;
                boolean exact = contains(exactMatches, j);
                if (exact) {
                    setText("✓ READY  —  " + j.getName());
                    setFont(getFont().deriveFont(Font.BOLD));
                    if (!isSelected) { setForeground(new Color(0, 128, 0)); }
                } else {
                    setText(j.getName());
                }
            }
            return this;
        }

        private static boolean contains(List<Jutsu> list, Jutsu target) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == target) { return true; }
            }
            return false;
        }
    }
}
