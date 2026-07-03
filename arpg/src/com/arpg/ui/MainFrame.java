package com.arpg.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.arpg.engine.GameEngine;
import com.arpg.model.CharacterClass;
import com.arpg.model.GameStateSnapshot;
import com.arpg.model.PlayerAction;

/**
 * Main application window. Owns the {@link GameEngine} reference, registers the
 * UI event listener, and hosts every panel. A {@link CardLayout} switches the
 * center region between character creation, the realm map, the inventory and the
 * combat arena; the character sheet, combat log and ability bar frame the game
 * cards once play begins.
 *
 * <p>Implements the small UI seams — {@link ActionSink} (submit a PlayerAction),
 * {@link GameControls} (tick/save/load) and {@link SnapshotConsumer} (refresh
 * from a snapshot) — so panels never touch the engine directly.</p>
 */
public class MainFrame extends JFrame
        implements ActionSink, GameControls, SnapshotConsumer, CharacterCreationPanel.CreationListener {

    private static final String CARD_CREATE = "create";
    private static final String CARD_MAP = "map";
    private static final String CARD_INVENTORY = "inventory";
    private static final String CARD_COMBAT = "combat";

    private final GameEngine engine;

    private final CardLayout cards = new CardLayout();
    private final JPanel centerCards = new JPanel(cards);

    private final CharacterCreationPanel creationPanel;
    private final CharacterSheetPanel sheetPanel;
    private final CombatLogPanel combatLog;
    private final AbilityBarPanel abilityBar;
    private final RealmMapPanel realmMap;
    private final InventoryPanel inventoryPanel;
    private final CombatViewPanel combatView;

    private final JPanel navBar;

    public MainFrame(GameEngine engine) {
        super("ARPG — Shattered Realms");
        this.engine = engine;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 680));

        creationPanel = new CharacterCreationPanel(this);
        sheetPanel = new CharacterSheetPanel(this);
        combatLog = new CombatLogPanel();
        abilityBar = new AbilityBarPanel(this);
        realmMap = new RealmMapPanel(this, this);
        inventoryPanel = new InventoryPanel(this);
        combatView = new CombatViewPanel(abilityBar);

        centerCards.add(wrapCreation(), CARD_CREATE);
        centerCards.add(realmMap, CARD_MAP);
        centerCards.add(inventoryPanel, CARD_INVENTORY);
        centerCards.add(combatView, CARD_COMBAT);

        navBar = buildNavBar();

        sheetPanel.setPreferredSize(new Dimension(260, 0));
        combatLog.setPreferredSize(new Dimension(300, 0));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(navBar, BorderLayout.NORTH);
        getContentPane().add(sheetPanel, BorderLayout.WEST);
        getContentPane().add(centerCards, BorderLayout.CENTER);
        getContentPane().add(combatLog, BorderLayout.EAST);
        getContentPane().add(abilityBar, BorderLayout.SOUTH);

        // Register the reactive listener.
        engine.addEventListener(new UIGameEventListener(combatLog, this));

        // Feed the creation panel with the available classes.
        creationPanel.setClasses(engine.getAvailableClasses());

        showCreationMode();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel wrapCreation() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(creationPanel, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildNavBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        bar.setBackground(UiTheme.BG_DARK);
        bar.setBorder(new EmptyBorder(2, 6, 2, 6));
        bar.add(navButton("Map", CARD_MAP));
        bar.add(navButton("Inventory", CARD_INVENTORY));
        bar.add(navButton("Combat", CARD_COMBAT));
        return bar;
    }

    private JButton navButton(String text, final String card) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cards.show(centerCards, card);
            }
        });
        return b;
    }

    private void showCreationMode() {
        navBar.setVisible(false);
        sheetPanel.setVisible(false);
        combatLog.setVisible(false);
        abilityBar.setVisible(false);
        cards.show(centerCards, CARD_CREATE);
    }

    private void showGameMode() {
        navBar.setVisible(true);
        sheetPanel.setVisible(true);
        combatLog.setVisible(true);
        abilityBar.setVisible(true);
        cards.show(centerCards, CARD_MAP);
        revalidate();
        repaint();
    }

    // ---- CharacterCreationPanel.CreationListener ----

    @Override
    public void onStartGame(String playerName, CharacterClass clazz) {
        GameStateSnapshot snapshot = engine.startNewGame(playerName, clazz);
        realmMap.setRealms(engine.getAvailableRealms());
        combatLog.setLines(snapshot.getRecentLog());
        combatLog.log("A new adventure begins for " + playerName + ".");
        showGameMode();
        refresh(snapshot);
    }

    // ---- ActionSink ----

    @Override
    public void submit(PlayerAction action) {
        if (action == null) {
            return;
        }
        try {
            engine.processPlayerAction(action);
        } catch (RuntimeException ex) {
            combatLog.log("Action failed: " + ex.getMessage());
        }
        // Pull the authoritative snapshot so the UI reflects the result even if
        // the engine did not emit an onGameStateChanged for this action.
        refresh(engine.getSnapshot());
    }

    // ---- GameControls ----

    @Override
    public void tick() {
        engine.tick();
        refresh(engine.getSnapshot());
    }

    @Override
    public void saveGame() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Game");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            boolean ok = engine.saveGame(file);
            if (ok) {
                combatLog.logLoot("Game saved to " + file.getName() + ".");
            } else {
                JOptionPane.showMessageDialog(this, "Save failed.", "Save", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void loadGame() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Game");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                GameStateSnapshot snapshot = engine.loadGame(file);
                realmMap.setRealms(engine.getAvailableRealms());
                showGameMode();
                refresh(snapshot);
                combatLog.setLines(snapshot.getRecentLog());
                combatLog.logLoot("Game loaded from " + file.getName() + ".");
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage(),
                        "Load", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ---- SnapshotConsumer ----

    @Override
    public void refresh(final GameStateSnapshot snapshot) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    refresh(snapshot);
                }
            });
            return;
        }
        if (snapshot == null) {
            return;
        }
        sheetPanel.update(snapshot.getPlayer());
        inventoryPanel.update(snapshot.getPlayer());
        abilityBar.update(snapshot.getPlayer());
        realmMap.setCurrentRealm(snapshot.getCurrentRealm());
        combatView.update(snapshot);
        // NOTE: the running combat log is fed incrementally by the event
        // listener (color-coded). We deliberately do NOT rewrite it from
        // snapshot.getRecentLog() here — that would erase the colored history on
        // every refresh. It is seeded from the snapshot only on start/load.
    }
}
