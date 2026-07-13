package com.whim.babylon5.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import com.whim.babylon5.data.CardDatabase;
import com.whim.babylon5.data.DeckStore;
import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.FactionId;

/**
 * A per-faction deck builder. Pick a faction, move cards between the faction's
 * available pool (left) and the deck (right, with per-card counts), and save.
 *
 * <p>Saved decks persist to {@code ~/.babylon5/decks.json} and are used to build
 * that faction's draw deck in the next game. An empty deck means "use every
 * eligible card" (the default). The Ambassador is always placed automatically, so
 * it is not part of the deck list.</p>
 */
public final class DeckEditorDialog extends JDialog {

    /** The four playable factions (match GameFactory's standard game). */
    private static final FactionId[] FACTIONS = {
            FactionId.HUMAN, FactionId.MINBARI, FactionId.NARN, FactionId.CENTAURI
    };

    private final JComboBox<FactionId> factionBox = new JComboBox<FactionId>(FACTIONS);
    private final JTextField filterField = new JTextField();
    private final DefaultListModel<Card> availModel = new DefaultListModel<Card>();
    private final JList<Card> availList = new JList<Card>(availModel);
    private final DefaultListModel<String> deckModel = new DefaultListModel<String>();
    private final JList<String> deckList = new JList<String>(deckModel);
    private final JLabel status = new JLabel(" ");

    /** Working deck for the selected faction: card id -> count. */
    private LinkedHashMap<String, Integer> deck = new LinkedHashMap<String, Integer>();

    public DeckEditorDialog(java.awt.Window owner) {
        super(owner, "Deck Builder", ModalityType.APPLICATION_MODAL);
        buildUi();
        loadFaction();
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        // top: faction picker + filter
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        JPanel fac = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fac.add(new JLabel("Faction:"));
        fac.add(factionBox);
        factionBox.addActionListener(e -> loadFaction());
        top.add(fac, BorderLayout.WEST);
        JPanel filt = new JPanel(new BorderLayout(4, 0));
        filt.add(new JLabel("Filter available: "), BorderLayout.WEST);
        filt.add(filterField, BorderLayout.CENTER);
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshAvailable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshAvailable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshAvailable(); }
        });
        top.add(filt, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // center: available | buttons | deck
        availList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availList.setCellRenderer(new CardCellRenderer());
        JScrollPane availScroll = titled(availList, "Available cards");
        availScroll.setPreferredSize(new Dimension(330, 0));

        deckList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane deckScroll = titled(deckList, "Deck");
        deckScroll.setPreferredSize(new Dimension(330, 0));

        JButton add = new JButton("Add ▶");
        JButton addFour = new JButton("Add ×3 ▶");
        JButton remove = new JButton("◀ Remove");
        add.addActionListener(e -> addSelected(1));
        addFour.addActionListener(e -> addSelected(3));
        remove.addActionListener(e -> removeSelected());
        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setBorder(BorderFactory.createEmptyBorder(40, 6, 0, 6));
        for (JButton b : new JButton[] { add, addFour, remove }) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(120, 28));
            mid.add(b);
            mid.add(Box.createVerticalStrut(8));
        }

        JPanel center = new JPanel(new BorderLayout(4, 0));
        center.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
        center.add(availScroll, BorderLayout.WEST);
        center.add(mid, BorderLayout.CENTER);
        center.add(deckScroll, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        // bottom: actions
        JButton fillAll = new JButton("Fill with all");
        JButton clear = new JButton("Clear (use default)");
        JButton save = new JButton("Save Deck");
        JButton close = new JButton("Close");
        fillAll.addActionListener(e -> fillAll());
        clear.addActionListener(e -> { deck.clear(); refreshDeck(); status.setText("Cleared — empty deck uses all cards."); });
        save.addActionListener(e -> save());
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        actions.add(fillAll);
        actions.add(clear);
        actions.add(save);
        actions.add(close);
        status.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 8));
        south.add(status, BorderLayout.WEST);
        south.add(actions, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);
    }

    // ---- data ----

    private FactionId faction() {
        return (FactionId) factionBox.getSelectedItem();
    }

    /** All non-Ambassador cards a faction may run (its loyal cards + neutrals). */
    private List<Card> pool() {
        List<Card> out = new ArrayList<Card>();
        for (Card c : CardDatabase.forFaction(faction())) {
            if (c.getType() != CardType.AMBASSADOR) {
                out.add(c);
            }
        }
        return out;
    }

    private void loadFaction() {
        deck = DeckStore.deckFor(faction()); // copy; empty if none
        refreshAvailable();
        refreshDeck();
        status.setForeground(UiTheme.INK_DIM);
        status.setText(DeckStore.hasDeck(faction())
                ? "Loaded saved deck for " + faction() + "."
                : faction() + " has no custom deck — currently uses all cards.");
    }

    private void refreshAvailable() {
        String q = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        Card keep = availList.getSelectedValue();
        availModel.clear();
        for (Card c : pool()) {
            if (q.isEmpty()
                    || c.getName().toLowerCase().contains(q)
                    || c.getType().name().toLowerCase().contains(q)
                    || c.getId().toLowerCase().contains(q)) {
                availModel.addElement(c);
            }
        }
        if (keep != null) {
            int i = availModel.indexOf(keep);
            if (i >= 0) availList.setSelectedIndex(i);
        }
    }

    private void refreshDeck() {
        String keep = deckList.getSelectedValue();
        deckModel.clear();
        int total = 0;
        for (Map.Entry<String, Integer> e : deck.entrySet()) {
            Card c = CardDatabase.byId(e.getKey());
            String name = c == null ? e.getKey() : c.getName();
            String type = c == null ? "?" : c.getType().toString();
            deckModel.addElement(e.getValue() + "×  " + name + "  [" + type + "]  {" + e.getKey() + "}");
            total += e.getValue();
        }
        if (keep != null) {
            int i = deckModel.indexOf(keep);
            if (i >= 0) deckList.setSelectedIndex(i);
        }
        setTitle("Deck Builder — " + faction() + " (" + total + " cards)");
    }

    private void addSelected(int n) {
        Card c = availList.getSelectedValue();
        if (c == null) {
            status.setText("Select a card on the left to add.");
            return;
        }
        int cur = deck.containsKey(c.getId()) ? deck.get(c.getId()) : 0;
        deck.put(c.getId(), Math.min(20, cur + n));
        refreshDeck();
        status.setForeground(UiTheme.INK_DIM);
        status.setText("Added " + n + "× " + c.getName() + ".");
    }

    private void removeSelected() {
        String id = selectedDeckId();
        if (id == null) {
            status.setText("Select a deck entry on the right to remove.");
            return;
        }
        int cur = deck.containsKey(id) ? deck.get(id) : 0;
        if (cur <= 1) {
            deck.remove(id);
        } else {
            deck.put(id, cur - 1);
        }
        refreshDeck();
    }

    private String selectedDeckId() {
        String row = deckList.getSelectedValue();
        if (row == null) return null;
        int lo = row.lastIndexOf('{'), hi = row.lastIndexOf('}');
        return (lo >= 0 && hi > lo) ? row.substring(lo + 1, hi) : null;
    }

    private void fillAll() {
        deck.clear();
        for (Card c : pool()) {
            deck.put(c.getId(), 1);
        }
        refreshDeck();
        status.setForeground(UiTheme.INK_DIM);
        status.setText("Filled with all " + deck.size() + " eligible cards — trim from here.");
    }

    private void save() {
        boolean ok = DeckStore.saveDeck(faction(), deck);
        if (ok) {
            status.setForeground(UiTheme.OK);
            status.setText("Saved " + faction() + " deck (" + totalCount() + " cards). "
                    + "Start a new game to play it.");
        } else {
            status.setForeground(UiTheme.DANGER);
            status.setText("Could NOT write " + DeckStore.decksPath());
        }
    }

    private int totalCount() {
        int t = 0;
        for (int v : deck.values()) t += v;
        return t;
    }

    private static JScrollPane titled(java.awt.Component c, String title) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        return sp;
    }

    private static final class CardCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Card) {
                Card c = (Card) value;
                setText(c.getName() + "  [" + c.getType() + "]  cost " + c.getCost());
            }
            return this;
        }
    }
}
