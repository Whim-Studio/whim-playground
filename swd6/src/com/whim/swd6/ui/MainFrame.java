package com.whim.swd6.ui;

import com.whim.swd6.api.CharacterRepository;
import com.whim.swd6.api.CombatTracker;
import com.whim.swd6.api.ContentProvider;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.RpgEngine;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The application shell: a {@link JFrame} with a top nav (Create / Sheet / Dice /
 * Combat / Adventure) over a {@link CardLayout} hub. It implements {@link AppContext}
 * and injects the real services (Main passes them in); a dev {@link #main} wires the
 * stub fakes so the UI runs standalone.
 *
 * Owned by Task 3 (ui).
 */
public final class MainFrame extends JFrame implements AppContext {

    private final ContentProvider content;
    private final RpgEngine engine;
    private final CharacterRepository repository;
    private final Supplier<CombatTracker> trackerSupplier;

    private PlayerCharacter character;

    private final CardLayout cards = new CardLayout();
    private final JPanel hub = new JPanel(cards);
    private final Map<String, HubPanel> panels = new LinkedHashMap<String, HubPanel>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<String, JButton>();
    private final JLabel statusLabel = Ui.dim("No active character");

    public MainFrame(ContentProvider content, RpgEngine engine,
                     CharacterRepository repository, Supplier<CombatTracker> trackerSupplier) {
        super("Star Wars D6 — Digital Tabletop");
        this.content = content;
        this.engine = engine;
        this.repository = repository;
        this.trackerSupplier = trackerSupplier;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));

        panels.put("Create", new CreationWizardPanel(this));
        panels.put("Sheet", new CharacterSheetPanel(this));
        panels.put("Dice", new DiceRollerPanel(this));
        panels.put("Combat", new CombatTrackerPanel(this));
        panels.put("Adventure", new AdventurePanel(this));

        hub.setOpaque(false);
        for (Map.Entry<String, HubPanel> e : panels.entrySet()) {
            hub.add(e.getValue(), e.getKey());
        }

        getContentPane().setBackground(Palette.SPACE_DEEP);
        getContentPane().add(buildNav(), BorderLayout.NORTH);
        getContentPane().add(hub, BorderLayout.CENTER);
        getContentPane().add(buildStatus(), BorderLayout.SOUTH);

        showCard("Create");
    }

    private JPanel buildNav() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new java.awt.GradientPaint(0, 0, Palette.SPACE_RAISED, 0, getHeight(), Palette.SPACE_PANEL));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Palette.AMBER_DIM);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        bar.setOpaque(true);
        bar.setBackground(Palette.SPACE_PANEL);

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        brand.setOpaque(false);
        JLabel mark = new JLabel("◆ SW·D6");
        mark.setForeground(Palette.AMBER);
        mark.setFont(Palette.TITLE);
        brand.add(mark);
        bar.add(brand, BorderLayout.WEST);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 10));
        tabs.setOpaque(false);
        for (final String key : panels.keySet()) {
            final JButton b = new JButton(key);
            b.setFocusPainted(false);
            b.setFont(Palette.HEAD);
            b.setForeground(Palette.TEXT);
            b.setBackground(Palette.SPACE_RAISED);
            b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            b.setOpaque(true);
            b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            b.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) { showCard(key); }
            });
            navButtons.put(key, b);
            tabs.add(b);
        }
        bar.add(tabs, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildStatus() {
        JPanel s = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        s.setBackground(Palette.SPACE_PANEL);
        s.add(statusLabel);
        return s;
    }

    private void highlightNav(String active) {
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean on = e.getKey().equals(active);
            e.getValue().setBackground(on ? Palette.AMBER : Palette.SPACE_RAISED);
            e.getValue().setForeground(on ? Palette.SPACE_DEEP : Palette.TEXT);
        }
    }

    private void refreshStatus() {
        if (character == null) {
            statusLabel.setForeground(Palette.TEXT_FAINT);
            statusLabel.setText("No active character — build one on Create.");
        } else {
            statusLabel.setForeground(Palette.CYAN);
            String nm = character.getName().isEmpty() ? "(unnamed)" : character.getName();
            statusLabel.setText("Active: " + nm + "   ·   FP " + character.getForcePoints()
                    + "   CP " + character.getCharacterPoints()
                    + "   ·   " + character.getWoundLevel().display());
        }
    }

    // ---------------- AppContext ----------------
    @Override public ContentProvider content() { return content; }
    @Override public RpgEngine engine() { return engine; }
    @Override public CharacterRepository repository() { return repository; }
    @Override public CombatTracker newTracker() { return trackerSupplier.get(); }
    @Override public PlayerCharacter character() { return character; }

    @Override
    public void setCharacter(PlayerCharacter pc) {
        this.character = pc;
        refreshStatus();
        for (HubPanel p : panels.values()) {
            p.onShow();
        }
    }

    @Override
    public void showCard(String name) {
        if (!panels.containsKey(name)) {
            return;
        }
        cards.show(hub, name);
        highlightNav(name);
        refreshStatus();
        panels.get(name).onShow();
    }

    // ---------------- dev standalone entry ----------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StubContent content = new StubContent();
                StubEngine engine = new StubEngine();
                StubRepository repo = new StubRepository();
                Supplier<CombatTracker> supplier = new Supplier<CombatTracker>() {
                    @Override public CombatTracker get() { return new StubCombatTracker(); }
                };
                MainFrame f = new MainFrame(content, engine, repo, supplier);
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
    }
}
