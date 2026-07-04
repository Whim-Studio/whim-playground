package com.whim.swd6.ui;

import com.whim.swd6.api.Armor;
import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.ForceSkill;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.Weapon;
import com.whim.swd6.api.WoundLevel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * A custom-painted character sheet in the style of a classic RPG sheet LAYOUT
 * (original design — no copyrighted sheet art): boxed attribute blocks, skill
 * columns, a wound track, point pools, and a gear list. Everything is drawn with
 * {@link Graphics2D}. The canvas implements {@link Printable} and a Print button
 * sends it to a {@link PrinterJob}.
 *
 * Owned by Task 3 (ui).
 */
public final class CharacterSheetPanel extends HubPanel {

    private final SheetCanvas canvas = new SheetCanvas();

    public CharacterSheetPanel(AppContext ctx) {
        super(ctx);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setOpaque(false);
        bar.add(Ui.title("Character Sheet"));
        JButton print = Ui.button("Print");
        print.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { doPrint(); }
        });
        JButton refresh = Ui.ghost("Refresh");
        refresh.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { canvas.repaint(); }
        });
        bar.add(print);
        bar.add(refresh);
        add(bar, BorderLayout.NORTH);
        add(Ui.scroll(canvas), BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        canvas.repaint();
    }

    private void doPrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(canvas);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Print failed: " + ex.getMessage(), "Print", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** The drawing surface; also the Printable. */
    private final class SheetCanvas extends JComponent implements Printable {

        private static final int W = 720;
        private static final int H = 940;

        SheetCanvas() {
            setPreferredSize(new Dimension(W, H));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintSheet(g2, false);
            g2.dispose();
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            double scale = Math.min(pf.getImageableWidth() / W, pf.getImageableHeight() / H);
            g2.scale(scale, scale);
            paintSheet(g2, true);
            return PAGE_EXISTS;
        }

        private void paintSheet(Graphics2D g2, boolean forPrint) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = forPrint ? Color.WHITE : Palette.SPACE_PANEL;
            Color ink = forPrint ? new Color(0x20, 0x20, 0x20) : Palette.TEXT;
            Color accent = forPrint ? new Color(0x8a, 0x5a, 0x00) : Palette.AMBER;
            Color line = forPrint ? new Color(0x99, 0x99, 0x99) : Palette.GRID_LINE;
            Color faint = forPrint ? new Color(0x66, 0x66, 0x66) : Palette.TEXT_DIM;

            g2.setColor(bg);
            g2.fillRect(0, 0, W, H);

            PlayerCharacter pc = ctx.character();
            if (pc == null) {
                g2.setColor(faint);
                g2.setFont(Palette.HEAD);
                g2.drawString("No active character. Create or load one on the Create tab.", 40, 60);
                return;
            }

            int m = 24;
            // ---- header ----
            g2.setColor(accent);
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            String name = pc.getName().isEmpty() ? "(unnamed)" : pc.getName();
            g2.drawString(name, m, 42);
            g2.setColor(faint);
            g2.setFont(Palette.SMALL);
            String sub = "Species: " + pc.getSpecies()
                    + (pc.getTemplateName().isEmpty() ? "   ·   Point-buy" : "   ·   " + pc.getTemplateName())
                    + (pc.isForceSensitive() ? "   ·   Force-Sensitive" : "");
            g2.drawString(sub, m, 60);
            g2.setColor(line);
            g2.setStroke(Palette.HAIRLINE);
            g2.drawLine(m, 70, W - m, 70);

            // ---- attribute boxes ----
            int ax = m, ay = 82;
            int bw = (W - 2 * m - 5 * 8) / 6;
            int bh = 66;
            Attribute[] attrs = Attribute.values();
            for (int i = 0; i < attrs.length; i++) {
                int x = ax + i * (bw + 8);
                box(g2, x, ay, bw, bh, line, forPrint);
                g2.setColor(accent);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString(attrs[i].abbrev(), x + 8, ay + 18);
                g2.setColor(ink);
                g2.setFont(new Font("Monospaced", Font.BOLD, 20));
                String code = pc.getAttribute(attrs[i]).toString();
                g2.drawString(code, x + 8, ay + 46);
            }

            // ---- skills columns ----
            int sy = ay + bh + 20;
            g2.setColor(accent);
            g2.setFont(Palette.HEAD);
            g2.drawString("SKILLS", m, sy);
            sy += 8;
            List<Skill> skills = pc.getSkills();
            int colW = (W - 2 * m) / 2;
            int rowH = 20;
            g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
            int half = (skills.size() + 1) / 2;
            for (int i = 0; i < skills.size(); i++) {
                Skill s = skills.get(i);
                int col = i < half ? 0 : 1;
                int rowIndex = i < half ? i : i - half;
                int x = m + col * colW;
                int y = sy + 18 + rowIndex * rowH;
                g2.setColor(rowIndex % 2 == 0 ? Palette.alpha(line, forPrint ? 40 : 60) : new Color(0, 0, 0, 0));
                g2.fillRect(x, y - 13, colW - 10, rowH - 2);
                g2.setColor(ink);
                DiceCode eff = pc.skillCode(s);
                String added = s.getAdded().pipValue() > 0 ? "  (+" + s.getAdded() + ")" : "";
                g2.drawString(pad(s.getName(), 22) + " " + eff + added, x + 4, y);
            }
            int skillRows = half;
            int afterSkills = sy + 24 + skillRows * rowH + 10;

            // ---- force skills ----
            if (pc.isForceSensitive()) {
                g2.setColor(accent);
                g2.setFont(Palette.HEAD);
                g2.drawString("FORCE", m, afterSkills);
                g2.setColor(ink);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
                int fx = m;
                for (ForceSkill fs : ForceSkill.values()) {
                    g2.drawString(fs.display() + ": " + pc.getForceSkill(fs), fx, afterSkills + 20);
                    fx += 150;
                }
                afterSkills += 40;
            }

            // ---- wound track ----
            int wy = afterSkills + 6;
            g2.setColor(accent);
            g2.setFont(Palette.HEAD);
            g2.drawString("WOUND STATUS", m, wy);
            WoundLevel[] levels = WoundLevel.values();
            int cellW = (W - 2 * m) / levels.length;
            int cy = wy + 10;
            for (int i = 0; i < levels.length; i++) {
                int x = m + i * cellW;
                boolean active = pc.getWoundLevel() == levels[i];
                Color fill = active ? (forPrint ? new Color(0xdd, 0xaa, 0x55) : Palette.AMBER)
                        : (forPrint ? new Color(0xf0, 0xf0, 0xf0) : Palette.SPACE_RAISED);
                g2.setColor(fill);
                g2.fillRect(x, cy, cellW - 4, 30);
                g2.setColor(line);
                g2.drawRect(x, cy, cellW - 4, 30);
                g2.setColor(active ? (forPrint ? Color.BLACK : Palette.SPACE_DEEP) : faint);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                drawCentered(g2, levels[i].display(), x, cy, cellW - 4, 30);
            }

            // ---- points row ----
            int py = cy + 52;
            g2.setColor(accent);
            g2.setFont(Palette.HEAD);
            g2.drawString("RESOURCES", m, py);
            g2.setColor(ink);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
            String pts = "Force Points: " + pc.getForcePoints()
                    + "     Character Points: " + pc.getCharacterPoints()
                    + "     Dark Side: " + pc.getDarkSidePoints()
                    + "     Move: " + pc.getMove() + "m"
                    + "     Credits: " + pc.getCredits();
            g2.drawString(pts, m, py + 20);

            // ---- gear ----
            int gy = py + 48;
            g2.setColor(accent);
            g2.setFont(Palette.HEAD);
            g2.drawString("GEAR & ARMAMENTS", m, gy);
            g2.setColor(ink);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            int line1 = gy + 20;
            for (Weapon w : pc.getWeapons()) {
                g2.drawString("• " + w.getName() + "  (" + w.getSkill() + ", dmg " + w.getDamage() + ")", m, line1);
                line1 += 18;
            }
            for (Armor a : pc.getArmor()) {
                g2.drawString("• " + a.getName() + "  (phys " + a.getPhysicalBonus()
                        + ", energy " + a.getEnergyBonus() + ")", m, line1);
                line1 += 18;
            }
            for (Equipment eq : pc.getGear()) {
                g2.drawString("• " + eq.getName() + (eq.getQuantity() > 1 ? " x" + eq.getQuantity() : ""), m, line1);
                line1 += 18;
            }

            // footer
            g2.setColor(faint);
            g2.setFont(Palette.SMALL);
            g2.drawString("D6 System character sheet · original layout", m, H - 16);
        }

        private void box(Graphics2D g2, int x, int y, int w, int h, Color line, boolean forPrint) {
            g2.setColor(forPrint ? new Color(0xf7, 0xf7, 0xf7) : Palette.SPACE_RAISED);
            g2.fillRoundRect(x, y, w, h, 8, 8);
            g2.setColor(line);
            g2.setStroke(Palette.HAIRLINE);
            g2.drawRoundRect(x, y, w, h, 8, 8);
        }

        private void drawCentered(Graphics2D g2, String s, int x, int y, int w, int h) {
            int sw = g2.getFontMetrics().stringWidth(s);
            int sh = g2.getFontMetrics().getAscent();
            g2.drawString(s, x + (w - sw) / 2, y + (h + sh) / 2 - 2);
        }

        private String pad(String s, int n) {
            if (s.length() >= n) {
                return s.substring(0, n);
            }
            StringBuilder b = new StringBuilder(s);
            while (b.length() < n) {
                b.append('.');
            }
            return b.toString();
        }
    }
}
