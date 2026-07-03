package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import com.arpg.model.Character;
import com.arpg.model.Equipment;
import com.arpg.model.Item;
import com.arpg.model.Rarity;

/**
 * Inventory browser: a rarity-colored {@link JList} of carried items plus
 * click-to-equip / reforge controls. Equip fires an EQUIP_ITEM
 * {@link com.arpg.model.PlayerAction}; reforge fires REFORGE_ITEM.
 */
public class InventoryPanel extends JPanel {

    private final ActionSink sink;
    private final DefaultListModel<Item> model = new DefaultListModel<Item>();
    private final JList<Item> list = new JList<Item>(model);
    private final JButton equipButton = new JButton("Equip");
    private final JButton reforgeButton = new JButton("Reforge");
    private final JLabel detail = new JLabel(" ");

    public InventoryPanel(ActionSink sink) {
        super(new BorderLayout(6, 6));
        this.sink = sink;
        setBackground(UiTheme.BG_PANEL);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Inventory");
        title.setFont(UiTheme.TITLE);
        title.setForeground(UiTheme.FG_TEXT);
        add(title, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(UiTheme.BG_DARK);
        list.setForeground(UiTheme.FG_TEXT);
        list.setFont(UiTheme.BODY);
        list.setCellRenderer(new ItemRenderer());
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onSelectionChanged();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        add(buildSouth(), BorderLayout.SOUTH);
        updateButtons();
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.setOpaque(false);

        detail.setFont(UiTheme.BODY);
        detail.setForeground(UiTheme.FG_MUTED);
        south.add(detail, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        buttons.setOpaque(false);
        equipButton.setFocusable(false);
        reforgeButton.setFocusable(false);
        equipButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Item sel = list.getSelectedValue();
                if (sel != null) {
                    sink.submit(UiActions.equipItem(sel.getId()));
                }
            }
        });
        reforgeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Item sel = list.getSelectedValue();
                if (sel != null) {
                    sink.submit(UiActions.reforgeItem(sel.getId()));
                }
            }
        });
        buttons.add(equipButton);
        buttons.add(reforgeButton);
        south.add(buttons, BorderLayout.CENTER);
        return south;
    }

    /** Rebuild the list from the player's current inventory, preserving selection by id. */
    public void update(Character player) {
        String keepId = null;
        Item sel = list.getSelectedValue();
        if (sel != null) {
            keepId = sel.getId();
        }
        model.clear();
        List<Item> items = player == null ? new ArrayList<Item>() : player.getInventory();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                model.addElement(items.get(i));
            }
        }
        if (keepId != null) {
            for (int i = 0; i < model.size(); i++) {
                if (keepId.equals(model.get(i).getId())) {
                    list.setSelectedIndex(i);
                    break;
                }
            }
        }
        updateButtons();
    }

    private void onSelectionChanged() {
        Item sel = list.getSelectedValue();
        if (sel == null) {
            detail.setText(" ");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(sel.getName());
            Rarity r = sel.getRarity();
            if (r != null) {
                sb.append("  [").append(r.name()).append("]");
            }
            sb.append("  req Lv ").append(sel.getLevelRequirement());
            detail.setText(sb.toString());
        }
        updateButtons();
    }

    private void updateButtons() {
        Item sel = list.getSelectedValue();
        boolean isEquip = sel instanceof Equipment;
        equipButton.setEnabled(isEquip);
        reforgeButton.setEnabled(isEquip);
    }

    /** Renders each item with its rarity color and required level. */
    private static final class ItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> jList, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
            if (value instanceof Item) {
                Item item = (Item) value;
                label.setText(item.getName() + "   (Lv " + item.getLevelRequirement() + ")");
                Color rc = UiTheme.rarityColor(item.getRarity());
                label.setForeground(isSelected ? Color.WHITE : rc);
            }
            label.setBackground(isSelected ? UiTheme.ACCENT.darker() : UiTheme.BG_DARK);
            label.setFont(UiTheme.BODY);
            return label;
        }
    }
}
