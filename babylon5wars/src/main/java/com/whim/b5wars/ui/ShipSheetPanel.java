package com.whim.b5wars.ui;

import com.whim.b5wars.engine.GameEvent;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Section;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponTrait;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * The Ship Control Sheet for the selected ship: a stat header, a per-facing armor and per-section
 * structure <em>damage-box diagram</em> (filled box = intact, dark box = destroyed), and a weapons
 * table (name / arc / range / to-hit / damage / reload / traits). Selecting a weapon row updates
 * the map's arc + range overlay.
 */
public final class ShipSheetPanel extends JPanel implements GameListener {

    private final GameController controller;
    private final JLabel header = new JLabel();
    private final DamageDiagram diagram;
    private final DefaultTableModel weaponModel;
    private final JTable weaponTable;

    private Ship shown;
    private boolean syncingSelection;

    public ShipSheetPanel(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 6));
        setBackground(UiTheme.PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        header.setForeground(UiTheme.TEXT);
        header.setFont(UiTheme.FONT_SMALL);
        header.setVerticalAlignment(JLabel.TOP);
        add(header, BorderLayout.NORTH);

        diagram = new DamageDiagram();
        add(diagram, BorderLayout.CENTER);

        weaponModel = new DefaultTableModel(
                new Object[] {"Weapon", "Arc", "Range", "To-Hit", "Damage", "Rel", "Traits"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        weaponTable = new JTable(weaponModel);
        weaponTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponTable.setBackground(UiTheme.PANEL_BG_ALT);
        weaponTable.setForeground(UiTheme.TEXT);
        weaponTable.setGridColor(UiTheme.PANEL_LINE);
        weaponTable.setFont(UiTheme.FONT_SMALL);
        weaponTable.getTableHeader().setReorderingAllowed(false);
        weaponTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || syncingSelection) {
                return;
            }
            int row = weaponTable.getSelectedRow();
            if (row >= 0) {
                controller.selectWeapon(row);
            }
        });
        JScrollPane weaponScroll = new JScrollPane(weaponTable);
        weaponScroll.setPreferredSize(new Dimension(360, 150));
        weaponScroll.getViewport().setBackground(UiTheme.PANEL_BG_ALT);
        add(weaponScroll, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(380, 520));
        controller.addListener(this);
        rebuild();
    }

    @Override
    public void gameChanged() {
        rebuild();
    }

    @Override
    public void logEvents(List<GameEvent> events) {
        // n/a
    }

    private void rebuild() {
        Ship s = controller.selectedShip();
        if (s != shown) {
            shown = s;
            rebuildWeaponTable(s);
        }
        header.setText(headerHtml(s));
        syncSelectedWeaponRow();
        diagram.setShip(s);
        diagram.repaint();
    }

    private void syncSelectedWeaponRow() {
        int idx = controller.selectedWeaponIndex();
        if (idx >= 0 && idx < weaponTable.getRowCount()
                && weaponTable.getSelectedRow() != idx) {
            syncingSelection = true;
            weaponTable.getSelectionModel().setSelectionInterval(idx, idx);
            syncingSelection = false;
        }
    }

    private void rebuildWeaponTable(Ship s) {
        weaponModel.setRowCount(0);
        if (s == null) {
            return;
        }
        int turn = controller.state().getTurn();
        List<Weapon> weapons = s.getType().getWeapons();
        for (int i = 0; i < weapons.size(); i++) {
            Weapon w = weapons.get(i);
            String ready = s.getReloadReadyTurn(i) > turn
                    ? ("R@" + s.getReloadReadyTurn(i)) : String.valueOf(w.getReloadTurns());
            weaponModel.addRow(new Object[] {
                    w.getName(),
                    arcText(w),
                    rangeText(w),
                    String.valueOf(w.getBaseToHit()),
                    damageText(w),
                    ready,
                    traitText(w)
            });
        }
    }

    private static String arcText(Weapon w) {
        StringBuilder sb = new StringBuilder();
        for (Facing f : Facing.values()) {
            if (w.getArc().contains(f)) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(f.name());
            }
        }
        return sb.length() == 0 ? "—" : sb.toString();
    }

    private static String rangeText(Weapon w) {
        int[] b = w.getRangeBrackets();
        if (b.length == 0) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) {
                sb.append('/');
            }
            sb.append(b[i]);
        }
        return sb.toString();
    }

    private static String damageText(Weapon w) {
        return w.getDamage().getCount() + "d" + w.getDamage().getSides()
                + (w.getDamage().getPlus() != 0 ? "+" + w.getDamage().getPlus() : "");
    }

    private static String traitText(Weapon w) {
        StringBuilder sb = new StringBuilder();
        for (WeaponTrait t : w.getTraits()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(abbrev(t));
        }
        return sb.toString();
    }

    private static String abbrev(WeaponTrait t) {
        switch (t) {
            case ARMOR_PIERCING: return "AP";
            case RAKING: return "Rk";
            case INTERCEPTOR: return "Int";
            case GUIDED: return "Gd";
            case BALLISTIC: return "Bl";
            default: return t.name();
        }
    }

    private String headerHtml(Ship s) {
        if (s == null) {
            return "<html><b>No ship selected</b><br>Click a ship on the map.</html>";
        }
        ShipClass c = s.getType();
        String status = s.isDestroyed() ? " — <font color='#ff6060'>DESTROYED</font>"
                : (s.isCrippled() ? " — <font color='#ffb040'>CRIPPLED</font>" : "");
        return "<html><b style='font-size:13px'>" + esc(c.getName()) + "</b>"
                + " <font color='#96a0b4'>[" + s.getSide() + " · " + c.getRace() + "]</font>" + status
                + "<br><font color='#96a0b4'>"
                + "Pts " + c.getPoints() + " &nbsp; Spd " + s.getSpeed() + "/" + c.getMaxSpeed()
                + " &nbsp; TurnMode " + c.getTurnMode()
                + " &nbsp; Thrust " + s.getThrustAvailable() + "/" + c.getThrust()
                + "<br>Pwr " + c.getPower() + " &nbsp; Init +" + c.getInitiativeBonus()
                + " &nbsp; Crew " + c.getCrewQuality()
                + " &nbsp; Sensor " + c.getSensorRating() + " &nbsp; EW " + c.getEwRating()
                + " (off " + s.getEwOffensive() + "/def " + s.getEwDefensive() + ")"
                + " &nbsp; Facing " + s.getFacing()
                + "</font></html>";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;");
    }

    /** Custom-painted per-facing armor + per-section structure box rows. */
    private static final class DamageDiagram extends JPanel {
        private Ship ship;

        DamageDiagram() {
            setBackground(UiTheme.PANEL_BG);
            setPreferredSize(new Dimension(360, 230));
        }

        void setShip(Ship s) {
            this.ship = s;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(UiTheme.FONT_SMALL);
            int y = 4;
            if (ship == null) {
                g.setColor(UiTheme.TEXT_DIM);
                g.drawString("No ship selected.", 6, 18);
                g.dispose();
                return;
            }
            g.setColor(UiTheme.TEXT);
            g.drawString("ARMOR  (per facing)", 6, y + 12);
            y += 20;
            Map<Facing, Integer> armor = ship.getArmor();
            Map<Facing, Integer> maxArmor = ship.getType().getArmor();
            for (Facing f : Facing.values()) {
                int cur = val(armor, f);
                int max = val(maxArmor, f);
                y = drawRow(g, f.name(), cur, max, y);
            }
            y += 8;
            g.setColor(UiTheme.TEXT);
            g.drawString("STRUCTURE  (per section)", 6, y + 12);
            y += 20;
            Map<Section, Integer> str = ship.getStructure();
            Map<Section, Integer> maxStr = ship.getType().getStructure();
            for (Section sec : Section.values()) {
                int cur = valS(str, sec);
                int max = valS(maxStr, sec);
                y = drawRow(g, sec.name(), cur, max, y);
            }
            setPreferredSize(new Dimension(360, y + 8));
            g.dispose();
        }

        private int drawRow(Graphics2D g, String label, int cur, int max, int y) {
            g.setColor(UiTheme.TEXT_DIM);
            g.drawString(label, 6, y + 11);
            int x = 92;
            int box = 11;
            int gap = 2;
            for (int i = 0; i < max; i++) {
                boolean intact = i < cur;
                g.setColor(intact ? UiTheme.BOX_INTACT : UiTheme.BOX_GONE);
                g.fillRect(x, y, box, box);
                g.setColor(UiTheme.BOX_LINE);
                g.drawRect(x, y, box, box);
                x += box + gap;
                if (x > getWidth() - box - 34) {
                    // wrap wide rows
                    x = 92;
                    y += box + gap;
                }
            }
            g.setColor(UiTheme.TEXT_DIM);
            g.drawString(cur + "/" + max, Math.min(getWidth() - 30, x + 4), y + 11);
            return y + box + 4;
        }

        private static int val(Map<Facing, Integer> m, Facing f) {
            Integer v = m.get(f);
            return v == null ? 0 : v.intValue();
        }

        private static int valS(Map<Section, Integer> m, Section s) {
            Integer v = m.get(s);
            return v == null ? 0 : v.intValue();
        }
    }
}
