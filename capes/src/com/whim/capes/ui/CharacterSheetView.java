package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.EventLogEntry;
import com.whim.capes.model.Exemplar;
import com.whim.capes.model.GameState;

/**
 * Read-only character sheet, laid out to echo the printed sheet on p.18: three
 * ability columns (Powers/Skills, Attitudes, Styles) numbered 1-up, a Drives
 * block showing Strength and current Debt boxes, and Exemplar slots. A roster
 * combo at the top selects which Character to display. Super abilities are
 * marked so the "costs Debt" distinction is visible.
 */
public final class CharacterSheetView extends JPanel {
    private final GameState state;
    private final JComboBox<Character> rosterCombo = new JComboBox<Character>();
    private final SheetCanvas canvas = new SheetCanvas();

    public CharacterSheetView(GameState state) {
        this.state = state;
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);

        JPanel top = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        top.setBackground(Palette.PAPER);
        JLabel l = new JLabel("Character:");
        l.setFont(Palette.HEADING);
        top.add(l);
        rosterCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> lst, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(lst, v, i, s, f);
                if (v instanceof Character) setText(((Character) v).name());
                return this;
            }
        });
        rosterCombo.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { canvas.repaint(); }
        });
        top.add(rosterCombo);
        JButton addExemplar = new JButton("Add Exemplar…");
        addExemplar.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { addExemplar(); }
        });
        top.add(addExemplar);
        add(top, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        refresh();
    }

    /** Authoring dialog: attach an Exemplar (another character + Drive + root conflict) to the selected character. */
    private void addExemplar() {
        Character owner = selected();
        if (owner == null) { warn("Select a character first."); return; }
        java.util.List<Character> others = new java.util.ArrayList<Character>();
        for (Character c : state.roster()) if (c != owner) others.add(c);
        if (others.isEmpty()) { warn("Need another character in the roster to be the Exemplar."); return; }

        Character exemplar = (Character) choose("Who is " + owner.name() + "'s Exemplar?", others.toArray());
        if (exemplar == null) return;
        DriveType drive = (DriveType) choose("Which Drive does the relationship embody?", DriveType.values());
        if (drive == null) return;
        String root = JOptionPane.showInputDialog(this,
                "Root conflict (\"This good thing, but that bad thing\"):", "Exemplar", JOptionPane.PLAIN_MESSAGE);
        if (root == null) return;
        ConflictType type = (ConflictType) choose("Free Conflict type?", ConflictType.values());
        if (type == null) return;

        owner.exemplars().add(new Exemplar(exemplar.id(), drive, root.trim(), type));
        state.eventLog().log(EventLogEntry.Category.SYSTEM,
                owner.name() + " gains " + exemplar.name() + " as " + drive.displayName() + " Exemplar.");
        canvas.repaint();
    }

    private Object choose(String prompt, Object[] options) {
        JComboBox<Object> combo = new JComboBox<Object>(options);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Character) setText(((Character) v).name());
                return this;
            }
        });
        int ok = JOptionPane.showConfirmDialog(this, combo, prompt, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return ok == JOptionPane.OK_OPTION ? combo.getSelectedItem() : null;
    }

    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Exemplar", JOptionPane.WARNING_MESSAGE); }

    /** Repopulate the roster combo (call when a character is added). */
    public void refresh() {
        Object sel = rosterCombo.getSelectedItem();
        rosterCombo.removeAllItems();
        for (Character c : state.roster()) rosterCombo.addItem(c);
        if (sel instanceof Character && state.roster().contains(sel)) rosterCombo.setSelectedItem(sel);
        canvas.repaint();
    }

    public void selectCharacter(Character c) {
        refresh();
        rosterCombo.setSelectedItem(c);
        canvas.repaint();
    }

    private Character selected() {
        Object o = rosterCombo.getSelectedItem();
        return (o instanceof Character) ? (Character) o : null;
    }

    private final class SheetCanvas extends JPanel {
        SheetCanvas() { setBackground(Palette.PAPER); setPreferredSize(new Dimension(900, 560)); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            UiKit.antialias(g2);
            Character c = selected();
            if (c == null) {
                g2.setColor(Palette.MUTED);
                g2.setFont(Palette.HEADING);
                g2.drawString("No characters yet. Build one in Character Creation.", 24, 40);
                return;
            }

            int m = 20;
            UiKit.indexCard(g2, m, m, getWidth() - 2 * m, getHeight() - 2 * m, Palette.PANEL, Palette.PANEL_EDGE);

            g2.setColor(c.isSuperPowered() ? Palette.HERO_BLUE : Palette.INK);
            g2.setFont(Palette.TITLE);
            g2.drawString(c.name(), m + 18, m + 34);
            g2.setColor(Palette.MUTED);
            g2.setFont(Palette.BODY);
            String sub = (c.isSuperPowered() ? "Super-powered" : "Mundane")
                    + (c.isUndifferentiated() ? " · Undifferentiated" : "")
                    + (c.concept() != null && !c.concept().isEmpty() ? " · " + c.concept() : "");
            g2.drawString(sub, m + 20, m + 54);

            int colY = m + 78;
            int colW = (getWidth() - 2 * m - 40) / 3;
            AbilityKind primary = c.isSuperPowered() ? AbilityKind.POWER : AbilityKind.SKILL;
            drawColumn(g2, c, primary, primary.displayName() + "s", m + 18, colY, colW);
            drawColumn(g2, c, AbilityKind.ATTITUDE, "Attitudes", m + 18 + colW + 10, colY, colW);
            drawColumn(g2, c, AbilityKind.STYLE, "Styles", m + 18 + 2 * (colW + 10), colY, colW);

            // Drives block along the bottom
            int drivesY = getHeight() - m - 118;
            drawDrives(g2, c, m + 18, drivesY, getWidth() - 2 * m - 36);
        }

        private void drawColumn(Graphics2D g2, Character c, AbilityKind kind, String title, int x, int y, int w) {
            g2.setColor(Palette.INK);
            g2.setFont(Palette.HEADING);
            g2.drawString(title, x, y);
            g2.setColor(Palette.PANEL_EDGE);
            g2.drawLine(x, y + 5, x + w - 8, y + 5);
            g2.setFont(Palette.BODY);
            int ly = y + 24;
            List<Ability> col = c.abilitiesOfKind(kind);
            // sort by score for display
            col.sort(new java.util.Comparator<Ability>() {
                @Override public int compare(Ability a, Ability b) { return a.score() - b.score(); }
            });
            for (Ability a : col) {
                g2.setColor(Palette.INK);
                g2.drawString(a.score() + ".  " + a.name(), x, ly);
                if (a.isSuperPowered()) {
                    g2.setColor(Palette.DEBT);
                    g2.drawString("◆", x + w - 20, ly); // marks a super (costs Debt) ability
                }
                ly += 20;
            }
        }

        private void drawDrives(Graphics2D g2, Character c, int x, int y, int w) {
            g2.setColor(Palette.INK);
            g2.setFont(Palette.HEADING);
            if (!c.isSuperPowered()) {
                g2.setColor(Palette.MUTED);
                g2.setFont(Palette.BODY);
                g2.drawString("Mundane character — no Drives.", x, y + 16);
                return;
            }
            if (c.isUndifferentiated()) {
                g2.drawString("Drives: Undifferentiated", x, y);
                g2.setColor(Palette.MUTED);
                g2.setFont(Palette.BODY);
                g2.drawString("Single Debt stack · Overdrawn above 5 · Stake up to 3 per Conflict.", x, y + 20);
                return;
            }
            g2.drawString("Drives", x, y);
            g2.setFont(Palette.BODY);
            int dx = x;
            for (Drive d : c.drives()) {
                g2.setColor(Palette.INK);
                g2.drawString(d.type().displayName(), dx, y + 22);
                g2.setColor(Palette.MUTED);
                g2.drawString("Str " + d.strength(), dx, y + 40);
                // Debt boxes: one box per point of Strength, filled = current resting Debt
                int bx = dx;
                for (int i = 0; i < d.strength(); i++) {
                    boolean filled = i < d.debt();
                    g2.setColor(filled ? Palette.DEBT : Palette.PANEL);
                    g2.fillRect(bx, y + 50, 14, 14);
                    g2.setColor(Palette.PANEL_EDGE);
                    g2.drawRect(bx, y + 50, 14, 14);
                    bx += 18;
                }
                if (d.isOverdrawn()) {
                    g2.setColor(Palette.VILLAIN_RED);
                    g2.drawString("Overdrawn", dx, y + 82);
                }
                dx += Math.max(120, d.strength() * 18 + 40);
            }

            // Exemplars
            if (!c.exemplars().isEmpty()) {
                g2.setColor(Palette.INK);
                g2.setFont(Palette.BODY);
                int ex = x;
                StringBuilder sb = new StringBuilder("Exemplars: ");
                for (Exemplar e : c.exemplars()) sb.append(e.exemplarCharacterId())
                        .append(" (").append(e.drive().displayName()).append(")  ");
                g2.drawString(sb.toString(), ex, y + 100);
            }
        }
    }

    static final Color UNUSED = null;
}
