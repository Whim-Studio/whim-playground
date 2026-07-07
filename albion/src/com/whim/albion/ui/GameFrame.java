package com.whim.albion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.whim.albion.api.GameController;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.GameStateType;

/**
 * Top-level Swing shell. Fixed logical size ~960×640, {@link BorderLayout}: a CENTER viewport
 * that swaps panels by {@link GameStateType}, an EAST party/minimap strip, and a SOUTH action
 * bar (Look/Use/Talk + overlay toggles + status/gold). Registers as a
 * {@link GameController.ChangeListener} and repaints on the EDT after every mutation.
 *
 * <p>The real entry point is the orchestrator's {@code app.Main}; this frame only depends on
 * the {@code api} seam so it works equally with {@link StubController} or the real engine.
 */
public final class GameFrame extends JFrame implements GameController.ChangeListener {

    private final GameController controller;

    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel centerHolder = new JPanel(new BorderLayout());
    private final PartyPanel partyPanel;
    private final JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel goldLabel = new JLabel("Gold: 0");

    private final TitleScreen titleScreen;
    private final TopDownRenderer topDown;
    private final FirstPersonRenderer firstPerson;
    private final CombatPanel combatPanel;
    private final DialoguePanel dialoguePanel;
    private final InventoryPanel inventoryPanel;
    private final CharacterSheetPanel characterSheet;
    private final JournalPanel journalPanel;
    private final MenuPanel menuPanel;

    private GameStateType shownState = null;

    public GameFrame(GameController controller) {
        super("Albion — Swing Recreation (dev shell)");
        this.controller = controller;
        this.partyPanel = new PartyPanel(controller);
        this.titleScreen = new TitleScreen(controller);
        this.topDown = new TopDownRenderer(controller);
        this.firstPerson = new FirstPersonRenderer(controller);
        this.combatPanel = new CombatPanel(controller);
        this.dialoguePanel = new DialoguePanel(controller);
        this.inventoryPanel = new InventoryPanel(controller);
        this.characterSheet = new CharacterSheetPanel(controller);
        this.journalPanel = new JournalPanel(controller);
        this.menuPanel = new MenuPanel(controller);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildActionBar();
        centerHolder.setBackground(Color.BLACK);
        root.add(centerHolder, BorderLayout.CENTER);
        setContentPane(root);
        installKeyBindings();

        controller.addChangeListener(this);
        rebuild();

        setPreferredSize(new Dimension(960, 640));
        pack();
        setLocationRelativeTo(null);
    }

    private void buildActionBar() {
        actionBar.setBackground(new Color(20, 20, 26));
        actionBar.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        addBtn("Look", new Runnable() { public void run() { controller.interact(); } });
        addBtn("Use", new Runnable() { public void run() { controller.interact(); } });
        addBtn("Talk", new Runnable() { public void run() { controller.interact(); } });
        actionBar.add(sep());
        addBtn("Inventory (I)", new Runnable() { public void run() { controller.openState(GameStateType.INVENTORY); } });
        addBtn("Character (C)", new Runnable() { public void run() { controller.openState(GameStateType.CHARACTER_SHEET); } });
        addBtn("Journal (J)", new Runnable() { public void run() { controller.openState(GameStateType.JOURNAL); } });
        addBtn("Menu (M)", new Runnable() { public void run() { controller.openState(GameStateType.MENU); } });
        actionBar.add(sep());
        statusLabel.setForeground(UiUtil.INK);
        goldLabel.setForeground(new Color(235, 205, 110));
        actionBar.add(goldLabel);
        actionBar.add(sep());
        actionBar.add(statusLabel);
    }

    private JLabel sep() { JLabel l = new JLabel("  |  "); l.setForeground(new Color(90, 88, 96)); return l; }

    private void addBtn(String text, final Runnable r) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { r.run(); }
        });
        actionBar.add(b);
    }

    private void installKeyBindings() {
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        bindMove(im, am, "UP", "W", Direction.NORTH);
        bindMove(im, am, "DOWN", "S", Direction.SOUTH);
        bindMove(im, am, "LEFT", "A", Direction.WEST);
        bindMove(im, am, "RIGHT", "D", Direction.EAST);
        bind(im, am, "E", "interact", new Runnable() { public void run() { if (isWorld()) controller.interact(); } });
        bind(im, am, "SPACE", "interact2", new Runnable() { public void run() { if (isWorld()) controller.interact(); } });
        bind(im, am, "I", "inv", new Runnable() { public void run() { toggle(GameStateType.INVENTORY); } });
        bind(im, am, "C", "char", new Runnable() { public void run() { toggle(GameStateType.CHARACTER_SHEET); } });
        bind(im, am, "J", "jour", new Runnable() { public void run() { toggle(GameStateType.JOURNAL); } });
        bind(im, am, "M", "menu", new Runnable() { public void run() { toggle(GameStateType.MENU); } });
        bind(im, am, "ESCAPE", "esc", new Runnable() { public void run() { controller.closeOverlay(); } });
    }

    private void bindMove(InputMap im, ActionMap am, String key1, String key2, final Direction dir) {
        Runnable r = new Runnable() { public void run() { if (isWorld()) controller.move(dir); } };
        bind(im, am, key1, "mv_" + dir, r);
        bind(im, am, key2, "mv2_" + dir, r);
    }

    private void bind(InputMap im, ActionMap am, String key, String name, final Runnable r) {
        im.put(KeyStroke.getKeyStroke(key), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { r.run(); }
        });
    }

    private boolean isWorld() {
        GameStateType s = controller.state().current();
        return s == GameStateType.OVERWORLD || s == GameStateType.DUNGEON;
    }

    private void toggle(GameStateType s) {
        if (controller.state().current() == s) controller.closeOverlay();
        else controller.openState(s);
    }

    // ------------------------------------------------------------- listener

    @Override
    public void onStateChanged() {
        if (SwingUtilities.isEventDispatchThread()) rebuild();
        else SwingUtilities.invokeLater(new Runnable() { public void run() { rebuild(); } });
    }

    private void rebuild() {
        GameStateType s = controller.state().current();
        JPanel center = centerFor(s);
        boolean worldChrome = isChromeState(s);

        // swap center content
        centerHolder.removeAll();
        centerHolder.add(center, BorderLayout.CENTER);

        // east/south chrome
        root.remove(partyPanel);
        root.remove(actionBar);
        if (worldChrome) {
            root.add(partyPanel, BorderLayout.EAST);
            root.add(actionBar, BorderLayout.SOUTH);
        }

        // per-panel refresh of dynamic buttons
        if (s == GameStateType.COMBAT) combatPanel.refresh();
        if (s == GameStateType.DIALOGUE) dialoguePanel.refresh();

        statusLabel.setText(controller.state().statusMessage());
        goldLabel.setText("Gold: " + controller.state().gold());

        shownState = s;
        root.revalidate();
        root.repaint();
        partyPanel.repaint();
    }

    private boolean isChromeState(GameStateType s) {
        return s == GameStateType.OVERWORLD || s == GameStateType.DUNGEON
                || s == GameStateType.INVENTORY || s == GameStateType.CHARACTER_SHEET
                || s == GameStateType.JOURNAL;
    }

    private JPanel centerFor(GameStateType s) {
        switch (s) {
            case OVERWORLD:       return topDown;
            case DUNGEON:         return firstPerson;
            case COMBAT:          return combatPanel;
            case DIALOGUE:        return dialoguePanel;
            case INVENTORY:       return inventoryPanel;
            case CHARACTER_SHEET: return characterSheet;
            case JOURNAL:         return journalPanel;
            case MENU:            return menuPanel;
            case TITLE:
            case GAME_OVER:
            default:              return titleScreen;
        }
    }
}
