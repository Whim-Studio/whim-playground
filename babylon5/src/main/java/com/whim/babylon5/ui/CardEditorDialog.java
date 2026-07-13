package com.whim.babylon5.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.whim.babylon5.data.CardDatabase;
import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.FactionId;

/**
 * A modal card editor. Lets a human hand-correct the printed definition of any
 * card — most importantly the sponsor {@code cost} and {@code influence}, which
 * the uploaded card lists did not ship, plus the ability ratings and text.
 *
 * <p>Edits are written straight into {@link CardDatabase} and persisted to
 * {@code ~/.babylon5/card-overrides.json}, so they survive restarts without
 * touching the packaged JSON. Because the game deals independent card copies at
 * start, an edit takes effect for cards dealt in the <em>next</em> game.</p>
 */
public final class CardEditorDialog extends JDialog {

    private final DefaultListModel<Card> listModel = new DefaultListModel<Card>();
    private final JList<Card> cardList = new JList<Card>(listModel);
    private final JTextField filterField = new JTextField();

    private final JTextField idField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JComboBox<CardType> typeBox = new JComboBox<CardType>(CardType.values());
    private final JComboBox<FactionId> factionBox = new JComboBox<FactionId>(FactionId.values());
    private final JSpinner costSpin = intSpinner();
    private final JSpinner influenceSpin = intSpinner();
    private final JSpinner diplomacySpin = intSpinner();
    private final JSpinner intrigueSpin = intSpinner();
    private final JSpinner psiSpin = intSpinner();
    private final JSpinner militarySpin = intSpinner();
    private final JTextArea textArea = new JTextArea(3, 20);
    private final JTextField imageField = new JTextField();
    private final JLabel status = new JLabel(" ");

    private Card current;

    public CardEditorDialog(java.awt.Window owner) {
        super(owner, "Card Editor — edit costs & stats", ModalityType.APPLICATION_MODAL);
        buildUi();
        reload(null);
        setMinimumSize(new Dimension(760, 560));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        // ---- left: filter + card list ----
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });
        JPanel filterRow = new JPanel(new BorderLayout(4, 0));
        filterRow.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterRow.add(filterField, BorderLayout.CENTER);
        left.add(filterRow, BorderLayout.NORTH);

        cardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cardList.setCellRenderer(new CardCellRenderer());
        cardList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showCard(cardList.getSelectedValue());
            }
        });
        JScrollPane listScroll = new JScrollPane(cardList);
        listScroll.setPreferredSize(new Dimension(280, 0));
        left.add(listScroll, BorderLayout.CENTER);
        add(left, BorderLayout.WEST);

        // ---- right: form ----
        add(buildForm(), BorderLayout.CENTER);

        // ---- bottom: buttons ----
        JButton save = new JButton("Save Card");
        save.addActionListener(e -> save());
        JButton revert = new JButton("Revert");
        revert.addActionListener(e -> showCard(current));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        status.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel south = new JPanel(new BorderLayout());
        south.add(status, BorderLayout.WEST);
        buttons.add(revert);
        buttons.add(save);
        buttons.add(close);
        south.add(buttons, BorderLayout.EAST);
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
        add(south, BorderLayout.SOUTH);
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Card definition"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        idField.setEditable(false);
        addRow(form, g, row++, "ID (read-only)", idField);
        addRow(form, g, row++, "Name", nameField);
        addRow(form, g, row++, "Type", typeBox);
        addRow(form, g, row++, "Faction", factionBox);
        addRow(form, g, row++, "Cost (influence to sponsor)", costSpin);
        addRow(form, g, row++, "Influence (stored)", influenceSpin);
        addRow(form, g, row++, "Diplomacy", diplomacySpin);
        addRow(form, g, row++, "Intrigue", intrigueSpin);
        addRow(form, g, row++, "Psi", psiSpin);
        addRow(form, g, row++, "Military", militarySpin);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        addRow(form, g, row++, "Text", new JScrollPane(textArea));
        addRow(form, g, row++, "Image URL", imageField);

        // filler
        g.gridx = 0; g.gridy = row; g.weighty = 1; g.gridwidth = 2;
        form.add(new JLabel(" "), g);
        return form;
    }

    private void addRow(JPanel form, GridBagConstraints g, int row, String label, Component field) {
        g.gridx = 0; g.gridy = row; g.weightx = 0; g.gridwidth = 1;
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN));
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        form.add(field, g);
    }

    // ---- data ----

    private void reload(String selectId) {
        List<Card> cards = new ArrayList<Card>(CardDatabase.all());
        listModel.clear();
        for (Card c : cards) {
            listModel.addElement(c);
        }
        applyFilter();
        if (selectId != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (selectId.equals(listModel.get(i).getId())) {
                    cardList.setSelectedIndex(i);
                    cardList.ensureIndexIsVisible(i);
                    return;
                }
            }
        }
        if (!listModel.isEmpty()) {
            cardList.setSelectedIndex(0);
        }
    }

    private void applyFilter() {
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        Card keep = cardList.getSelectedValue();
        listModel.clear();
        for (Card c : CardDatabase.all()) {
            if (q.isEmpty()
                    || c.getName().toLowerCase().contains(q)
                    || c.getId().toLowerCase().contains(q)
                    || c.getFaction().name().toLowerCase().contains(q)
                    || c.getType().name().toLowerCase().contains(q)) {
                listModel.addElement(c);
            }
        }
        if (keep != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getId().equals(keep.getId())) {
                    cardList.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void showCard(Card c) {
        current = c;
        if (c == null) {
            return;
        }
        idField.setText(c.getId());
        nameField.setText(c.getName());
        typeBox.setSelectedItem(c.getType());
        factionBox.setSelectedItem(c.getFaction());
        costSpin.setValue(c.getCost());
        influenceSpin.setValue(c.getInfluence());
        diplomacySpin.setValue(c.getDiplomacy());
        intrigueSpin.setValue(c.getIntrigue());
        psiSpin.setValue(c.getPsi());
        militarySpin.setValue(c.getMilitary());
        textArea.setText(c.getText());
        textArea.setCaretPosition(0);
        imageField.setText(c.getImageUrl());
        status.setText("Editing " + c.getId());
        status.setForeground(UiTheme.INK_DIM);
    }

    private void save() {
        if (current == null) {
            return;
        }
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name cannot be empty.", "Invalid",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Card edited = new Card(
                current.getId(),
                name,
                (CardType) typeBox.getSelectedItem(),
                (FactionId) factionBox.getSelectedItem(),
                intVal(costSpin), intVal(influenceSpin),
                intVal(diplomacySpin), intVal(intrigueSpin),
                intVal(psiSpin), intVal(militarySpin),
                textArea.getText(), imageField.getText().trim());

        boolean persisted = CardDatabase.updateCard(edited);
        reload(edited.getId());
        if (persisted) {
            status.setForeground(UiTheme.OK);
            status.setText("Saved \"" + edited.getName() + "\" — cost " + edited.getCost()
                    + " · influence " + edited.getInfluence() + "  (applies to your next game)");
        } else {
            status.setForeground(UiTheme.DANGER);
            status.setText("Saved in memory but could NOT write " + CardDatabase.overridesPath());
        }
    }

    // ---- helpers ----

    private static JSpinner intSpinner() {
        return new JSpinner(new SpinnerNumberModel(0, 0, 99, 1));
    }

    private static int intVal(JSpinner s) {
        Object v = s.getValue();
        return (v instanceof Number) ? ((Number) v).intValue() : 0;
    }

    private static final class CardCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Card) {
                Card c = (Card) value;
                setText(c.getName() + "  —  " + c.getFaction() + " · " + c.getType()
                        + "  (cost " + c.getCost() + ")");
            }
            return this;
        }
    }
}
