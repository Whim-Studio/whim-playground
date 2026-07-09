package com.whim.alganon.ui.screens;

import com.whim.alganon.api.Defs;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.SoundHooks;
import com.whim.alganon.ui.UiTheme;
import com.whim.alganon.ui.render.Sprites;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Guided character-creation wizard: race → family → class → name, each step showing the def's
 * fresh description and a live preview sprite/summary. Options are a clickable list (left);
 * the selected def's detail + preview render on the right. Back/Next navigate; Confirm commits.
 */
public final class CreationScreen extends JPanel {

    private static final String[] STEP_TITLES = {"Choose your Race", "Choose your Family", "Choose your Class", "Name your Hero"};

    private final GameController controller;
    private final Canvas canvas = new Canvas();
    private final JButton back = flat("‹ Back");
    private final JButton next = flat("Next ›");
    private final JButton confirm = flat("Begin ⚔");
    private final JTextField nameField = new JTextField(18);
    private final JLabel stepLabel = new JLabel();

    private List<Rectangle> optionHits = new ArrayList<Rectangle>();
    private List<String> optionIds = new ArrayList<String>();

    public CreationScreen(GameController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BG);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        top.setBackground(UiTheme.PANEL_DARK);
        stepLabel.setFont(UiTheme.FONT_H1);
        stepLabel.setForeground(UiTheme.ACCENT);
        top.add(stepLabel);
        add(top, BorderLayout.NORTH);

        add(canvas, BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(UiTheme.PANEL_DARK);
        JLabel nl = new JLabel("Name:"); nl.setForeground(UiTheme.TEXT_DIM); nl.setFont(UiTheme.FONT_BODY);
        nameField.setBackground(UiTheme.PANEL_LIGHT);
        nameField.setForeground(UiTheme.TEXT);
        nameField.setCaretColor(UiTheme.ACCENT);
        nameField.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { sync(); }
            public void removeUpdate(DocumentEvent e) { sync(); }
            public void changedUpdate(DocumentEvent e) { sync(); }
            private void sync() { controller.setName(nameField.getText()); }
        });
        bar.add(nl); bar.add(nameField);
        bar.add(back); bar.add(next); bar.add(confirm);
        add(bar, BorderLayout.SOUTH);

        back.addActionListener(e -> { SoundHooks.get().play(SoundHooks.Cue.UI_BACK); controller.creationBack(); });
        next.addActionListener(e -> advance());
        confirm.addActionListener(e -> { SoundHooks.get().play(SoundHooks.Cue.GAME_START); controller.finishCreation(); });

        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { pickOption(e); }
        });
    }

    private static JButton flat(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(UiTheme.PANEL_LIGHT);
        b.setForeground(UiTheme.TEXT);
        b.setFont(UiTheme.FONT_H2);
        b.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return b;
    }

    /** Re-issue the current step's selection to advance (works whether or not choose auto-advances). */
    private void advance() {
        Views.CreationView c = controller.state().creation();
        if (c == null) return;
        SoundHooks.get().play(SoundHooks.Cue.WIZARD_STEP);
        switch (c.step()) {
            case 0: if (c.selectedRaceId() != null) controller.chooseRace(c.selectedRaceId()); break;
            case 1: if (c.selectedFamilyId() != null) controller.chooseFamily(c.selectedFamilyId()); break;
            case 2: if (c.selectedClassId() != null) controller.chooseClass(c.selectedClassId()); break;
            default: break;
        }
    }

    private void pickOption(MouseEvent e) {
        Views.CreationView c = controller.state().creation();
        if (c == null) return;
        for (int i = 0; i < optionHits.size(); i++) {
            if (optionHits.get(i).contains(e.getPoint())) {
                String id = optionIds.get(i);
                SoundHooks.get().play(SoundHooks.Cue.UI_CLICK);
                switch (c.step()) {
                    case 0: controller.chooseRace(id); break;
                    case 1: controller.chooseFamily(id); break;
                    case 2: controller.chooseClass(id); break;
                    default: break;
                }
                return;
            }
        }
    }

    /** Called by GameFrame when the controller state changes. */
    public void refresh() {
        Views.CreationView c = controller.state().creation();
        int step = c == null ? 0 : c.step();
        stepLabel.setText("Step " + (step + 1) + " / 4  —  " + STEP_TITLES[Math.min(step, 3)]);
        boolean nameStep = step >= 3;
        nameField.setVisible(nameStep);
        confirm.setVisible(nameStep);
        next.setVisible(!nameStep);
        back.setEnabled(step > 0);
        confirm.setEnabled(nameStep && c != null && c.enteredName() != null && !c.enteredName().trim().isEmpty()
                && c.selectedClassId() != null);
        if (nameStep && !nameField.getText().equals(c.enteredName())) nameField.setText(c.enteredName());
        canvas.repaint();
    }

    /** The list + preview drawing surface. */
    private final class Canvas extends JPanel {
        Canvas() { setBackground(UiTheme.BG); setPreferredSize(new Dimension(1000, 560)); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            UiTheme.aa(g);
            Views.CreationView c = controller.state().creation();
            if (c == null) return;
            int w = getWidth(), h = getHeight();
            int listW = 340, pad = 20;

            optionHits = new ArrayList<Rectangle>();
            optionIds = new ArrayList<String>();

            int step = c.step();
            // ----- options list -----
            UiTheme.panel(g, pad, pad, listW, h - pad * 2);
            int oy = pad + 16;
            List<Option> opts = optionsFor(c);
            String selected = selectedId(c);
            for (Option o : opts) {
                Rectangle r = new Rectangle(pad + 12, oy, listW - 24, 48);
                optionHits.add(r); optionIds.add(o.id);
                boolean sel = o.id.equals(selected);
                g.setColor(sel ? UiTheme.PANEL_LIGHT : UiTheme.PANEL_DARK);
                g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g.setColor(sel ? UiTheme.ACCENT : UiTheme.BORDER);
                g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8);
                g.setColor(sel ? UiTheme.ACCENT_HOT : UiTheme.TEXT);
                g.setFont(UiTheme.FONT_H2);
                g.drawString(o.name, r.x + 14, r.y + 22);
                if (o.sub != null) {
                    g.setColor(UiTheme.TEXT_DIM);
                    g.setFont(UiTheme.FONT_SMALL);
                    g.drawString(o.sub, r.x + 14, r.y + 39);
                }
                oy += 56;
            }

            // ----- preview / detail -----
            int px = pad * 2 + listW, pw = w - px - pad;
            UiTheme.panel(g, px, pad, pw, h - pad * 2);
            Option detail = find(opts, selected);
            if (step >= 3) {
                drawSummary(g, c, px + 24, pad + 30, pw - 48);
            } else if (detail != null) {
                drawPreviewSprite(g, detail, px + pw / 2, pad + 90);
                g.setColor(UiTheme.ACCENT);
                g.setFont(UiTheme.FONT_H1);
                g.drawString(detail.name, px + 24, pad + 180);
                if (detail.sub != null) {
                    g.setColor(UiTheme.TEXT_DIM);
                    g.setFont(UiTheme.FONT_H2);
                    g.drawString(detail.sub, px + 24, pad + 204);
                }
                g.setColor(UiTheme.TEXT);
                g.setFont(UiTheme.FONT_BODY);
                drawWrapped(g, detail.desc, px + 24, pad + 236, pw - 48, 19);
            } else {
                g.setColor(UiTheme.TEXT_DIM);
                g.setFont(UiTheme.FONT_H2);
                g.drawString("Select an option to preview.", px + 24, pad + 40);
            }
        }

        private void drawPreviewSprite(Graphics2D g, Option o, int cx, int cy) {
            g.setColor(UiTheme.PANEL_DARK);
            g.fillRoundRect(cx - 60, cy - 60, 120, 120, 12, 12);
            g.setColor(UiTheme.BORDER);
            g.drawRoundRect(cx - 60, cy - 60, 119, 119, 12, 12);
            Sprites.draw(g, o.spriteKey, cx, cy, 34);
        }

        private void drawSummary(Graphics2D g, Views.CreationView c, int x, int y, int w) {
            g.setColor(UiTheme.ACCENT);
            g.setFont(UiTheme.FONT_H1);
            g.drawString("Confirm your Hero", x, y);
            g.setFont(UiTheme.FONT_H2);
            g.setColor(UiTheme.TEXT);
            int ly = y + 44;
            ly = line(g, x, ly, "Name", c.enteredName() == null || c.enteredName().isEmpty() ? "(enter a name below)" : c.enteredName());
            ly = line(g, x, ly, "Race", nameOf(c.races(), c.selectedRaceId()));
            ly = line(g, x, ly, "Family", nameOf2(c.familiesFor(c.selectedRaceId()), c.selectedFamilyId()));
            ly = line(g, x, ly, "Class", nameOf3(c.classes(), c.selectedClassId()));
            g.setColor(UiTheme.TEXT_DIM);
            g.setFont(UiTheme.FONT_BODY);
            drawWrapped(g, "Enter a name and press Begin to enter the world. All choices shape your "
                    + "starting stats, abilities, family bonus, and home zone.", x, ly + 20, w, 19);
        }

        private int line(Graphics2D g, int x, int y, String k, String v) {
            g.setColor(UiTheme.TEXT_DIM); g.setFont(UiTheme.FONT_BODY);
            g.drawString(k, x, y);
            g.setColor(UiTheme.TEXT); g.setFont(UiTheme.FONT_H2);
            g.drawString(v == null ? "—" : v, x + 90, y);
            return y + 34;
        }
    }

    // ----- option modeling -----
    private static final class Option {
        final String id, name, sub, desc, spriteKey;
        Option(String id, String name, String sub, String desc, String spriteKey) {
            this.id = id; this.name = name; this.sub = sub; this.desc = desc; this.spriteKey = spriteKey; }
    }

    private List<Option> optionsFor(Views.CreationView c) {
        List<Option> out = new ArrayList<Option>();
        switch (c.step()) {
            case 0:
                for (Defs.RaceDef r : c.races())
                    out.add(new Option(r.id, r.name, r.faction.name(), r.description, "npc.hero"));
                break;
            case 1:
                for (Defs.FamilyDef f : c.familiesFor(c.selectedRaceId()))
                    out.add(new Option(f.id, f.name, f.archetype.name(), f.description, "npc"));
                break;
            case 2:
                for (Defs.ClassDef cl : c.classes())
                    out.add(new Option(cl.id.name().toLowerCase(), cl.name, cl.resource.name(), cl.description, "player." + cl.name));
                break;
            default:
                break;
        }
        return out;
    }

    private String selectedId(Views.CreationView c) {
        switch (c.step()) {
            case 0: return c.selectedRaceId();
            case 1: return c.selectedFamilyId();
            case 2: return c.selectedClassId();
            default: return null;
        }
    }

    private Option find(List<Option> opts, String id) {
        if (id == null) return null;
        for (Option o : opts) if (o.id.equals(id)) return o;
        return null;
    }

    private static String nameOf(List<Defs.RaceDef> l, String id) {
        if (id == null) return null; for (Defs.RaceDef d : l) if (d.id.equals(id)) return d.name; return id;
    }
    private static String nameOf2(List<Defs.FamilyDef> l, String id) {
        if (id == null) return null; for (Defs.FamilyDef d : l) if (d.id.equals(id)) return d.name; return id;
    }
    private static String nameOf3(List<Defs.ClassDef> l, String id) {
        if (id == null) return null; for (Defs.ClassDef d : l) if (d.id.name().toLowerCase().equals(id)) return d.name; return id;
    }

    static void drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int lineH) {
        if (text == null) return;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, cy);
                cy += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) g.drawString(line.toString(), x, cy);
    }
}
