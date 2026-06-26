package com.whim.monopoly.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.whim.monopoly.domain.OwnableSpace;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.Space;
import com.whim.monopoly.domain.SpaceType;
import com.whim.monopoly.domain.StreetSpace;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.GameState;
import com.whim.monopoly.engine.Holding;
import com.whim.monopoly.engine.TurnPhase;

/**
 * Holdings dashboard for the current player. Lists owned ownables and exposes
 * build/sell/mortgage/unmortgage and trade actions. Illegal actions are greyed
 * out via the engine's {@code canBuildHouse} / {@code canMortgage} predicates.
 */
public class PropertyPanel extends JPanel {

    private final MonopolyFrame frame;
    private final GameEngine engine;

    private final JLabel header = new JLabel();
    private final DefaultListModel<String> model = new DefaultListModel<String>();
    private final JList<String> list = new JList<String>(model);
    private final List<Integer> deedIndices = new ArrayList<Integer>();

    private final JButton buildBtn = new JButton("Build House");
    private final JButton sellBtn = new JButton("Sell House");
    private final JButton mortgageBtn = new JButton("Mortgage");
    private final JButton unmortgageBtn = new JButton("Unmortgage");
    private final JButton tradeBtn = new JButton("Trade…");

    public PropertyPanel(MonopolyFrame frame, GameEngine engine) {
        this.frame = frame;
        this.engine = engine;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Holdings"));
        setPreferredSize(new Dimension(280, 360));

        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        add(header, BorderLayout.NORTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DeedRenderer());
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateButtons();
                }
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 2, 5, 5));
        buttons.add(buildBtn);
        buttons.add(sellBtn);
        buttons.add(mortgageBtn);
        buttons.add(unmortgageBtn);
        JPanel south = new JPanel(new BorderLayout(0, 5));
        south.add(buttons, BorderLayout.CENTER);
        tradeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        south.add(tradeBtn, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        buildBtn.addActionListener(action(new Op() {
            public void run(int idx) {
                engine.buildHouse(idx);
            }
        }));
        sellBtn.addActionListener(action(new Op() {
            public void run(int idx) {
                engine.sellHouse(idx);
            }
        }));
        mortgageBtn.addActionListener(action(new Op() {
            public void run(int idx) {
                engine.mortgage(idx);
            }
        }));
        unmortgageBtn.addActionListener(action(new Op() {
            public void run(int idx) {
                engine.unmortgage(idx);
            }
        }));
        tradeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.openTradeDialog();
            }
        });
    }

    private interface Op {
        void run(int spaceIndex);
    }

    private ActionListener action(final Op op) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int idx = selectedIndex();
                if (idx < 0) {
                    return;
                }
                frame.submitEngineAction(new Runnable() {
                    public void run() {
                        op.run(idx);
                    }
                });
            }
        };
    }

    private int selectedIndex() {
        int sel = list.getSelectedIndex();
        if (sel < 0 || sel >= deedIndices.size()) {
            return -1;
        }
        return deedIndices.get(sel).intValue();
    }

    /** Rebuild from current player's deeds. Must run on the EDT. */
    public void refresh() {
        GameState s = engine.getState();
        Player cur = s.getCurrentPlayer();
        int keep = list.getSelectedIndex();

        model.clear();
        deedIndices.clear();
        if (cur == null) {
            header.setText("—");
            updateButtons();
            return;
        }
        header.setText(cur.getName() + " — $" + cur.getCash()
                + (cur.getJailCards() > 0 ? "  | Jail cards: " + cur.getJailCards() : ""));

        List<Integer> deeds = new ArrayList<Integer>(cur.getDeeds());
        Collections.sort(deeds);
        for (int i = 0; i < deeds.size(); i++) {
            int idx = deeds.get(i).intValue();
            Space space = s.getBoard().spaceAt(idx);
            Holding h = s.holdingAt(idx);
            deedIndices.add(Integer.valueOf(idx));
            model.addElement(describe(space, h));
        }
        if (keep >= 0 && keep < model.size()) {
            list.setSelectedIndex(keep);
        }
        updateButtons();
    }

    private String describe(Space space, Holding h) {
        StringBuilder sb = new StringBuilder(space.getName());
        if (space.getType() == SpaceType.STREET && h != null) {
            if (h.hasHotel()) {
                sb.append("  [HOTEL]");
            } else if (h.getHouseCount() > 0) {
                sb.append("  [").append(h.getHouseCount()).append("h]");
            }
        }
        if (h != null && h.isMortgaged()) {
            sb.append("  (mortgaged)");
        }
        return sb.toString();
    }

    private void updateButtons() {
        GameState s = engine.getState();
        boolean myTurn = s.getPhase() == TurnPhase.AWAITING_END_TURN;
        int idx = selectedIndex();

        boolean canBuild = false;
        boolean canSell = false;
        boolean canMort = false;
        boolean canUnmort = false;
        if (idx >= 0) {
            Space space = s.getBoard().spaceAt(idx);
            Holding h = s.holdingAt(idx);
            canBuild = myTurn && engine.canBuildHouse(idx);
            canMort = myTurn && engine.canMortgage(idx);
            if (h != null) {
                boolean isStreet = space.getType() == SpaceType.STREET;
                canSell = myTurn && isStreet && (h.hasHotel() || h.getHouseCount() > 0);
                canUnmort = myTurn && h.isMortgaged();
            }
        }
        buildBtn.setEnabled(canBuild);
        sellBtn.setEnabled(canSell);
        mortgageBtn.setEnabled(canMort);
        unmortgageBtn.setEnabled(canUnmort);
        tradeBtn.setEnabled(s.getPhase() == TurnPhase.AWAITING_END_TURN
                || s.getPhase() == TurnPhase.AWAITING_ROLL);
    }

    /** Paints a small color swatch beside street deeds. */
    private class DeedRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> jlist, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    jlist, value, index, isSelected, cellHasFocus);
            if (index >= 0 && index < deedIndices.size()) {
                Space space = engine.getState().getBoard().spaceAt(deedIndices.get(index).intValue());
                if (space instanceof StreetSpace) {
                    Color c = BoardColors.of(((StreetSpace) space).getColorGroup());
                    label.setIcon(new SwatchIcon(c));
                } else {
                    label.setIcon(new SwatchIcon(new Color(210, 210, 210)));
                }
            }
            return label;
        }
    }

    private static class SwatchIcon implements javax.swing.Icon {
        private final Color color;

        SwatchIcon(Color color) {
            this.color = color;
        }

        public int getIconWidth() {
            return 14;
        }

        public int getIconHeight() {
            return 14;
        }

        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y + 1, 12, 12);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y + 1, 12, 12);
        }
    }
}
