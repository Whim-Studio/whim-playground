package com.whim.capes.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import com.whim.capes.content.ExtendedData;
import com.whim.capes.content.NonPersonTemplate;
import com.whim.capes.engine.GameEngine;
import com.whim.capes.engine.IllegalMoveException;
import com.whim.capes.model.Ability;
import com.whim.capes.model.Character;
import com.whim.capes.model.Conflict;
import com.whim.capes.model.ConflictSide;
import com.whim.capes.model.ConflictType;
import com.whim.capes.model.Die;
import com.whim.capes.model.Drive;
import com.whim.capes.model.DriveType;
import com.whim.capes.model.GameState;
import com.whim.capes.model.Inspiration;
import com.whim.capes.model.Player;
import com.whim.capes.model.Scene;

/**
 * The interactive Table View (Phase 3). Renders the live Scene as index-card
 * Conflicts with pip dice and Control highlight, lets the player click to select
 * a side or die, and drives the {@link GameEngine} through an action toolbar:
 * Start Page, Add Conflict, Claim, Stake, Split, Use Ability (roll + Reactions),
 * Spend Inspiration and Resolve (with Gloat fallback). Illegal moves surface as
 * dialogs so play stays legal.
 */
public final class TableView extends JPanel {
    private final GameState state;
    private final GameEngine engine;
    private final Canvas canvas = new Canvas();

    private Conflict selConflict;
    private int selSide = -1;
    private int selDie = -1;

    public TableView(GameState state, GameEngine engine) {
        this.state = state;
        this.engine = engine;
        setLayout(new BorderLayout());
        setBackground(Palette.PAPER);
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(buildStoryBar(), BorderLayout.SOUTH);
    }

    private final JTextField storyField = new JTextField();

    /** Free-narration bar: type story text tied to the current moment; logged as Narration (p.24). */
    private JPanel buildStoryBar() {
        JPanel bar = new JPanel(new BorderLayout(6, 0));
        bar.setBackground(Palette.PAPER);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        javax.swing.JLabel l = new javax.swing.JLabel("Story ✎ ");
        l.setFont(Palette.HEADING);
        bar.add(l, BorderLayout.WEST);
        bar.add(storyField, BorderLayout.CENTER);
        ActionListener narrate = new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                engine.logNarration(storyField.getText());
                storyField.setText("");
            }
        };
        storyField.addActionListener(narrate);
        JButton add = new JButton("Add to Story");
        add.addActionListener(narrate);
        bar.add(add, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBackground(Palette.PAPER);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Palette.PANEL_EDGE));
        bar.add(btn("Start Page", new Runnable() { public void run() { startPage(); } }));
        bar.add(btn("Next Turn", new Runnable() { public void run() { guard(new Runnable() { public void run() { engine.advanceTurn(); repaintAll(); } }); } }));
        bar.add(btn("Next Phase", new Runnable() { public void run() { nextPhase(); } }));
        bar.add(sep());
        bar.add(btn("Add Conflict", new Runnable() { public void run() { addConflict(); } }));
        bar.add(btn("Add Participant (Ch.5)", new Runnable() { public void run() { addParticipant(); } }));
        bar.add(btn("Exemplar Conflict", new Runnable() { public void run() { exemplarConflict(); } }));
        bar.add(sep());
        bar.add(btn("Claim", new Runnable() { public void run() { claim(); } }));
        bar.add(btn("Stake", new Runnable() { public void run() { stake(); } }));
        bar.add(btn("Split", new Runnable() { public void run() { split(); } }));
        bar.add(btn("Split→New Side", new Runnable() { public void run() { splitNewSide(); } }));
        bar.add(btn("Use Ability", new Runnable() { public void run() { useAbility(); } }));
        bar.add(btn("Spend Inspiration", new Runnable() { public void run() { spendInspiration(); } }));
        bar.add(sep());
        bar.add(btn("Resolve", new Runnable() { public void run() { resolve(); } }));
        return bar;
    }

    // ---------------------------------------------------------------- actions
    private void startPage() {
        Scene sc = scene();
        if (sc == null) { warn("Declare a Scene first."); return; }
        guard(new Runnable() { public void run() { engine.startPage(sc); repaintAll(); } });
    }

    private void addConflict() {
        Scene sc = scene();
        if (sc == null) { warn("Declare a Scene first."); return; }
        final Player p = choosePlayer("Which player adds the Conflict?");
        if (p == null) return;
        final String title = JOptionPane.showInputDialog(this, "Conflict statement:", "New Conflict", JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.trim().isEmpty()) return;
        final ConflictType type = (ConflictType) choose("Type?", ConflictType.values());
        if (type == null) return;
        guard(new Runnable() { public void run() { engine.addConflict(sc, p.id(), title.trim(), type, true); repaintAll(); } });
    }

    private void nextPhase() {
        Scene sc = scene();
        if (sc == null || sc.pages().isEmpty()) { warn("Start a Page first."); return; }
        engine.advancePhase(sc.pages().get(sc.pages().size() - 1));
        repaintAll();
    }

    /** Introduce the Free Conflict of an Exemplar relationship present in the game (pp.75-76). */
    private void exemplarConflict() {
        final Scene sc = scene();
        if (sc == null) { warn("Declare a Scene first."); return; }
        List<Object[]> options = new ArrayList<Object[]>(); // {Character owner, Exemplar}
        List<String> labels = new ArrayList<String>();
        for (Character owner : state.roster()) {
            for (com.whim.capes.model.Exemplar ex : owner.exemplars()) {
                options.add(new Object[]{ owner, ex });
                Character exch = state.characterById(ex.exemplarCharacterId());
                labels.add(owner.name() + " ↔ " + (exch == null ? ex.exemplarCharacterId() : exch.name())
                        + " (" + ex.drive().displayName() + ")");
            }
        }
        if (options.isEmpty()) { warn("No Exemplars defined. Add one from a Character Sheet."); return; }
        Object picked = choose("Introduce which Exemplar's Free Conflict?", labels.toArray());
        if (picked == null) return;
        final Object[] sel = options.get(labels.indexOf(picked));
        final Player p = choosePlayer("Which player introduces it?");
        if (p == null) return;
        guard(new Runnable() { public void run() {
            engine.addExemplarConflict(sc, (Character) sel[0], (com.whim.capes.model.Exemplar) sel[1], p.id());
            repaintAll();
        } });
    }

    private void addParticipant() {
        final Scene sc = scene();
        if (sc == null) { warn("Declare a Scene first."); return; }
        final NonPersonTemplate t = (NonPersonTemplate) choose("Bring in which Ch.5 participant?", ExtendedData.all().toArray());
        if (t == null) return;
        final Player p = choosePlayer("Which player controls it?");
        if (p == null) return;
        guard(new Runnable() { public void run() {
            Character np = engine.addNonPerson(t, p.id());
            if (t.hasFreeConflict()) {
                int add = JOptionPane.showConfirmDialog(TableView.this,
                        "Add its Free " + t.freeConflictType().label() + " \"" + t.freeConflictStatement()
                        + "\" now?\n(Resolving it removes " + np.name() + " from the Scene.)",
                        "Character Conflict", JOptionPane.YES_NO_OPTION);
                if (add == JOptionPane.YES_OPTION)
                    engine.addCharacterConflict(sc, np, t.freeConflictType(), t.freeConflictStatement(), p.id());
            }
            repaintAll();
        } });
    }

    private void splitNewSide() {
        if (!haveDie()) { warn("Click the die to split off from."); return; }
        final Scene sc = scene();
        final Character c = chooseCharacter("Which character splits off (Stakes 1 Debt)?");
        if (c == null) return;
        DriveType drive = null;
        if (!c.isUndifferentiated()) {
            drive = (DriveType) choose("Stake which Drive (1 point)?", driveTypes(c));
            if (drive == null) return;
        }
        final String stmt = JOptionPane.showInputDialog(this, "New side's Resolution statement:");
        if (stmt == null) return;
        final Player p = choosePlayer("Which player Claims the new side?");
        if (p == null) return;
        final Conflict conf = selConflict; final int side = selSide; final int die = selDie; final DriveType d = drive;
        guard(new Runnable() { public void run() {
            engine.foundNewSide(sc, conf, side, die, c, d, stmt.trim(), p.id());
            selDie = -1; repaintAll();
        } });
    }

    private void claim() {
        if (!haveSide()) { warn("Click a Conflict side first."); return; }
        final Player p = choosePlayer("Which player Claims side " + (selSide + 1) + "?");
        if (p == null) return;
        final Conflict c = selConflict; final int side = selSide;
        guard(new Runnable() { public void run() { engine.claim(scene(), c, side, p.id(), true); repaintAll(); } });
    }

    private void stake() {
        if (!haveSide()) { warn("Click a Conflict side first."); return; }
        final Character c = chooseCharacter("Which character Stakes?");
        if (c == null) return;
        DriveType drive = null;
        if (!c.isUndifferentiated()) {
            drive = (DriveType) choose("Stake which Drive?", driveTypes(c));
            if (drive == null) return;
        }
        Integer amt = chooseInt("How much Debt to Stake?", 1, 5);
        if (amt == null) return;
        final Conflict conf = selConflict; final int side = selSide; final DriveType d = drive; final int a = amt;
        guard(new Runnable() { public void run() { engine.stake(conf, side, c, d, a); repaintAll(); } });
    }

    private void split() {
        if (!haveDie()) { warn("Click a specific die first."); return; }
        Integer parts = chooseInt("Split into how many dice?", 2, 6);
        if (parts == null) return;
        final Conflict c = selConflict; final int side = selSide; final int die = selDie; final int p = parts;
        guard(new Runnable() { public void run() { engine.split(c, side, die, p); selDie = -1; repaintAll(); } });
    }

    private void useAbility() {
        if (!haveDie()) { warn("Click a specific die to roll."); return; }
        Scene sc = scene();
        if (sc == null || sc.pages().isEmpty()) { warn("Start a Page first."); return; }
        final com.whim.capes.model.Page page = sc.pages().get(sc.pages().size() - 1);
        final Character c = chooseCharacter("Which character acts?");
        if (c == null) return;
        final Ability ab = chooseAbility(c);
        if (ab == null) return;
        DriveType debtDrive = pickDebtDrive(c, ab);
        final DriveType dd = debtDrive;
        final Conflict conf = selConflict; final int side = selSide; final int die = selDie;
        guard(new Runnable() { public void run() {
            int rolled = engine.useAbilityRoll(sc, page, c, ab, conf, side, die, dd);
            repaintAll();
            int keep = JOptionPane.showConfirmDialog(TableView.this,
                    "Rolled " + rolled + ". Accept this roll? (No = turn the die back)",
                    "Accept roll?", JOptionPane.YES_NO_OPTION);
            if (keep == JOptionPane.YES_OPTION) {
                engine.acceptRoll(conf, side, die);
                repaintAll();
                offerReactions(sc, page, conf, side, die);
            } else {
                engine.revertRoll(conf, side, die);
                repaintAll();
            }
        } });
    }

    /** After an accepted roll, walk the table offering each player one Reaction (p.40). */
    private void offerReactions(Scene sc, com.whim.capes.model.Page page, Conflict conf, int side, int die) {
        while (true) {
            Player reactor = choosePlayer("Any Reactions? Pick a reacting player, or Cancel to stop.");
            if (reactor == null) return;
            Character rc = chooseCharacter("React with which character?");
            if (rc == null) continue;
            Ability rab = chooseAbility(rc);
            if (rab == null) continue;
            DriveType dd = pickDebtDrive(rc, rab);
            try {
                int rolled = engine.react(sc, page, reactor.id(), rc, rab, conf, side, die, dd);
                repaintAll();
                int keep = JOptionPane.showConfirmDialog(this, "Reaction rolled " + rolled + ". Accept?",
                        "Accept Reaction?", JOptionPane.YES_NO_OPTION);
                if (keep == JOptionPane.YES_OPTION) engine.acceptRoll(conf, side, die);
                else engine.revertRoll(conf, side, die);
                repaintAll();
            } catch (IllegalMoveException ex) {
                warn(ex.getMessage());
            }
        }
    }

    private void spendInspiration() {
        if (!haveDie()) { warn("Click a die to raise."); return; }
        final Player p = choosePlayer("Whose Inspiration?");
        if (p == null) return;
        if (p.inspirations().isEmpty()) { warn(p.name() + " holds no Inspirations."); return; }
        final Inspiration ins = (Inspiration) choose("Spend which Inspiration?", p.inspirations().toArray());
        if (ins == null) return;
        final Conflict c = selConflict; final int side = selSide; final int die = selDie;
        guard(new Runnable() { public void run() { engine.spendInspiration(p, ins, c, side, die); repaintAll(); } });
    }

    private void resolve() {
        if (!haveSide()) { warn("Click the side you Claimed."); return; }
        final Player p = choosePlayer("Which player Resolves (Claimant)?");
        if (p == null) return;
        final Conflict c = selConflict; final int side = selSide;
        try {
            engine.resolve(scene(), c, side, p.id());
            String narration = JOptionPane.showInputDialog(this,
                    "Narrate the outcome (optional):", "Resolve \"" + c.title() + "\"", JOptionPane.PLAIN_MESSAGE);
            engine.logNarration(narration);
            selConflict = null; selSide = -1; selDie = -1;
            repaintAll();
        } catch (IllegalMoveException ex) {
            // Offer a Gloat if the outcome would violate the Comics Code.
            int gl = JOptionPane.showConfirmDialog(this,
                    ex.getMessage() + "\n\nGloat instead (would resolving violate the Comics Code)?",
                    "Resolve / Gloat", JOptionPane.YES_NO_OPTION);
            if (gl == JOptionPane.YES_OPTION) {
                Integer n = chooseInt("Turn how many of your dice down to 1?", 1, c.sides().get(side).dice().size());
                if (n != null) guard(new Runnable() { public void run() { engine.gloat(c, side, p.id(), n); repaintAll(); } });
            }
        }
    }

    // ------------------------------------------------------------- choosers
    private DriveType pickDebtDrive(Character c, Ability ab) {
        if (!ab.isSuperPowered() || c.isUndifferentiated() || c.drives().isEmpty()) return null;
        return (DriveType) choose("Place the earned Debt on which Drive?", driveTypes(c));
    }

    private Player choosePlayer(String prompt) {
        return (Player) choose(prompt, state.players().toArray());
    }
    private Character chooseCharacter(String prompt) {
        if (state.roster().isEmpty()) { warn("No characters. Build one in Character Creation."); return null; }
        return (Character) choose(prompt, state.roster().toArray());
    }
    private Ability chooseAbility(Character c) {
        if (c.abilities().isEmpty()) { warn(c.name() + " has no Abilities."); return null; }
        return (Ability) choose("Use which Ability of " + c.name() + "?", c.abilities().toArray());
    }
    private DriveType[] driveTypes(Character c) {
        List<DriveType> ds = new ArrayList<DriveType>();
        for (Drive d : c.drives()) ds.add(d.type());
        return ds.toArray(new DriveType[0]);
    }

    private Object choose(String prompt, Object[] options) {
        if (options.length == 0) return null;
        JComboBox<Object> combo = new JComboBox<Object>(options);
        int ok = JOptionPane.showConfirmDialog(this, combo, prompt, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return ok == JOptionPane.OK_OPTION ? combo.getSelectedItem() : null;
    }
    private Integer chooseInt(String prompt, int min, int max) {
        String s = JOptionPane.showInputDialog(this, prompt + " (" + min + "-" + max + ")", min);
        if (s == null) return null;
        try {
            int v = Integer.parseInt(s.trim());
            if (v < min || v > max) { warn("Enter a number in " + min + "-" + max + "."); return null; }
            return v;
        } catch (NumberFormatException e) { warn("Not a number."); return null; }
    }

    // --------------------------------------------------------------- helpers
    private Scene scene() { return state.currentScene(); }
    private boolean haveSide() { return selConflict != null && selSide >= 0; }
    private boolean haveDie() { return haveSide() && selDie >= 0 && selDie < selConflict.sides().get(selSide).dice().size(); }
    private void guard(Runnable r) { try { r.run(); } catch (IllegalMoveException e) { warn(e.getMessage()); } }
    private void warn(String msg) { JOptionPane.showMessageDialog(this, msg, "Not allowed", JOptionPane.WARNING_MESSAGE); }
    private void repaintAll() { canvas.revalidate(); canvas.repaint(); }

    private JButton btn(String label, final Runnable action) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { action.run(); } });
        return b;
    }
    private static Box.Filler sep() { return (Box.Filler) Box.createRigidArea(new Dimension(10, 1)); }

    // ---------------------------------------------------------------- canvas
    private final class Canvas extends JPanel {
        private final List<Object[]> sideHits = new ArrayList<Object[]>(); // {Conflict, Integer side, Rectangle}
        private final List<Object[]> dieHits = new ArrayList<Object[]>();  // {Conflict, Integer side, Integer die, Rectangle}

        Canvas() {
            setBackground(Palette.PAPER);
            setPreferredSize(new Dimension(900, 560));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { onClick(e.getX(), e.getY()); }
            });
        }

        private void onClick(int x, int y) {
            for (Object[] h : dieHits) {
                if (((Rectangle) h[3]).contains(x, y)) {
                    selConflict = (Conflict) h[0]; selSide = (Integer) h[1]; selDie = (Integer) h[2];
                    repaint(); return;
                }
            }
            for (Object[] h : sideHits) {
                if (((Rectangle) h[2]).contains(x, y)) {
                    selConflict = (Conflict) h[0]; selSide = (Integer) h[1]; selDie = -1;
                    repaint(); return;
                }
            }
            selConflict = null; selSide = -1; selDie = -1;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            sideHits.clear(); dieHits.clear();
            Graphics2D g2 = (Graphics2D) g;
            UiKit.antialias(g2);

            int y = 16;
            g2.setColor(Palette.INK); g2.setFont(Palette.TITLE);
            g2.drawString("The Table", 20, y + 20);
            y += 40;

            g2.setFont(Palette.BODY);
            int px = 20;
            for (Player p : state.players()) {
                g2.setColor(Palette.INK);
                g2.drawString(p.name(), px, y + 4);
                UiKit.token(g2, px + 88, y - 12, 18, Palette.GOLD, "S");
                g2.drawString("x" + p.storyTokens(), px + 110, y + 4);
                UiKit.token(g2, px + 146, y - 12, 18, Palette.GOLD, "I");
                g2.drawString("x" + p.inspirations().size(), px + 168, y + 4);
                px += 230;
            }
            y += 26;

            Scene sc = scene();
            if (sc == null) {
                g2.setColor(Palette.MUTED);
                g2.drawString("No Scene yet.", 20, y + 20);
                return;
            }
            g2.setColor(Palette.INK); g2.setFont(Palette.HEADING);
            String pageInfo = sc.pages().isEmpty() ? "(no Page yet)"
                    : "Page " + sc.pages().get(sc.pages().size() - 1).number();
            g2.drawString("Scene " + sc.number() + ": " + safe(sc.title()) + "   " + pageInfo, 20, y + 12);
            y += 22;

            if (!sc.pages().isEmpty()) {
                com.whim.capes.model.Page pg = sc.pages().get(sc.pages().size() - 1);
                Player actor = engine.currentActor();
                StringBuilder order = new StringBuilder();
                for (int i = 0; i < state.players().size(); i++) {
                    Player pl = state.players().get(i);
                    boolean cur = actor != null && pl.id().equals(actor.id());
                    order.append(cur ? "▶ " : "").append(pl.name()).append(i < state.players().size() - 1 ? "  " : "");
                }
                g2.setColor(Palette.HERO_BLUE); g2.setFont(Palette.BODY);
                g2.drawString("Phase: " + pg.phase() + "    Turn: " + order, 20, y + 12);
                y += 18;
            }

            List<Character> overdrawn = engine.overdrawnCharacters();
            if (!overdrawn.isEmpty()) {
                StringBuilder od = new StringBuilder("Overdrawn (penalty roll each Page): ");
                for (int i = 0; i < overdrawn.size(); i++) od.append(i > 0 ? ", " : "").append(overdrawn.get(i).name());
                g2.setColor(Palette.VILLAIN_RED); g2.setFont(Palette.BODY);
                g2.drawString(od.toString(), 20, y + 12);
            }
            y += 18;

            int cardW = 320, cardH = 168, gap = 20;
            int perRow = Math.max(1, (getWidth() - 40) / (cardW + gap));
            int col = 0, rowY = y;
            for (Conflict c : sc.conflicts()) {
                int cx = 20 + col * (cardW + gap);
                drawConflict(g2, c, cx, rowY, cardW, cardH);
                if (++col >= perRow) { col = 0; rowY += cardH + gap; }
            }
            if (sc.conflicts().isEmpty()) {
                g2.setColor(Palette.MUTED); g2.setFont(Palette.BODY);
                g2.drawString("No Conflicts. Use Add Conflict.", 20, y + 20);
            }
            setPreferredSize(new Dimension(getWidth(), rowY + cardH + 40));
        }

        private void drawConflict(Graphics2D g2, Conflict c, int x, int y, int w, int h) {
            ConflictSide controlling = c.controllingSide();
            UiKit.indexCard(g2, x, y, w, h, Palette.PANEL, Palette.PANEL_EDGE);
            g2.setColor(Palette.INK); g2.setFont(Palette.HEADING);
            g2.drawString(c.type().label() + ": " + safe(c.title()), x + 12, y + 22);

            List<ConflictSide> sides = c.sides();
            int sideH = (h - 34) / Math.max(2, sides.size());
            int sy = y + 32;
            for (int i = 0; i < sides.size(); i++) {
                ConflictSide s = sides.get(i);
                Rectangle sideRect = new Rectangle(x + 6, sy - 2, w - 12, sideH);
                sideHits.add(new Object[]{ c, i, sideRect });
                boolean selectedSide = (c == selConflict && i == selSide);
                if (s == controlling) {
                    g2.setColor(new Color(46, 125, 79, 38));
                    g2.fillRect(sideRect.x, sideRect.y, sideRect.width, sideRect.height);
                }
                if (selectedSide) {
                    g2.setColor(Palette.GOLD);
                    g2.drawRect(sideRect.x, sideRect.y, sideRect.width, sideRect.height);
                }
                Color accent = (i == 0) ? Palette.HERO_BLUE : Palette.VILLAIN_RED;
                int dx = x + 12;
                List<Die> dice = s.dice();
                for (int di = 0; di < dice.size(); di++) {
                    Rectangle dieRect = new Rectangle(dx, sy, 26, 26);
                    dieHits.add(new Object[]{ c, i, di, dieRect });
                    UiKit.die(g2, dx, sy, 26, dice.get(di).value(), accent);
                    if (c == selConflict && i == selSide && di == selDie) {
                        g2.setColor(Palette.GOLD);
                        g2.drawRect(dx - 2, sy - 2, 30, 30);
                    }
                    dx += 32;
                }
                g2.setColor(Palette.INK); g2.setFont(Palette.BODY);
                String tag = "total " + s.total() + (s == controlling ? "  ◀ Controls" : "")
                        + (s.stakedDebt() > 0 ? "  · Staked " + s.stakedDebt() : "");
                g2.drawString(tag, dx + 6, sy + 17);
                sy += sideH;
            }
        }
    }

    private static String safe(String s) { return s == null ? "(untitled)" : s; }
}
