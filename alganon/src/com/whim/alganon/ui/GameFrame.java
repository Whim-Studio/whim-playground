package com.whim.alganon.ui;

import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.GameController;
import com.whim.alganon.api.Views;
import com.whim.alganon.ui.screens.CreationScreen;
import com.whim.alganon.ui.screens.HudScreen;
import com.whim.alganon.ui.screens.OverlayPanel;
import com.whim.alganon.ui.screens.TitleScreen;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

/**
 * The single top-level Swing window for the game. It holds the {@link GameController}, registers
 * a {@link GameController.ChangeListener} to repaint on every tick/intent, and routes keyboard
 * and mouse input to controller intents. Screens live on a {@link CardLayout} (title / creation /
 * hud); overlay states dim the HUD and float an {@link OverlayPanel} on a layered pane above it.
 */
public final class GameFrame extends JFrame implements GameController.ChangeListener {

    private static final String CARD_TITLE = "title";
    private static final String CARD_CREATION = "creation";
    private static final String CARD_HUD = "hud";

    private final GameController controller;
    private final JLayeredPane layers = new JLayeredPane();
    private final JPanel cards = new JPanel(new CardLayout());
    private final CardLayout cardLayout;

    private final TitleScreen title;
    private final CreationScreen creation;
    private final HudScreen hud;
    private final OverlayPanel overlay;

    public GameFrame(GameController controller) {
        super("Alganon — Single-Player Recreation");
        this.controller = controller;
        this.cardLayout = (CardLayout) cards.getLayout();

        this.title = new TitleScreen(controller);
        this.creation = new CreationScreen(controller);
        this.hud = new HudScreen(controller);
        this.overlay = new OverlayPanel(controller);

        cards.add(title, CARD_TITLE);
        cards.add(creation, CARD_CREATION);
        cards.add(hud, CARD_HUD);

        layers.setLayout(null);
        cards.setBounds(0, 0, 1180, 760);
        overlay.setBounds(0, 0, 1180, 760);
        overlay.setVisible(false);
        layers.add(cards, JLayeredPane.DEFAULT_LAYER);
        layers.add(overlay, JLayeredPane.PALETTE_LAYER);
        layers.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                Dimension d = layers.getSize();
                cards.setBounds(0, 0, d.width, d.height);
                overlay.setBounds(0, 0, d.width, d.height);
            }
        });

        setContentPane(layers);
        setPreferredSize(new Dimension(1180, 760));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        controller.addChangeListener(this);
        installKeyDispatcher();
        pack();
        setLocationRelativeTo(null);
        refresh();
    }

    /** Global key routing; skips typing keys when a text field owns focus. */
    private void installKeyDispatcher() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) return false;
                if (!isActive()) return false;
                Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focus instanceof JTextComponent) return false; // let the wizard name field type
                return handleKey(e);
            }
        });
    }

    private boolean handleKey(KeyEvent e) {
        GameStateType st = controller.state().state();
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_ESCAPE) {
            if (isOverlay(st)) { controller.closeOverlay(); return true; }
            return false;
        }

        if (st == GameStateType.PLAYING) {
            switch (code) {
                case KeyEvent.VK_W: case KeyEvent.VK_UP:    controller.move(Direction.NORTH); return true;
                case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  controller.move(Direction.SOUTH); return true;
                case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  controller.move(Direction.WEST);  return true;
                case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: controller.move(Direction.EAST);  return true;
                case KeyEvent.VK_E: controller.interact(); return true;
                case KeyEvent.VK_I: controller.openState(GameStateType.INVENTORY); return true;
                case KeyEvent.VK_C: controller.openState(GameStateType.CHARACTER_SHEET); return true;
                case KeyEvent.VK_K: controller.openState(GameStateType.STUDY); return true;
                case KeyEvent.VK_B: controller.openState(GameStateType.CRAFTING); return true;
                case KeyEvent.VK_U: controller.openState(GameStateType.AUCTION); return true;
                case KeyEvent.VK_F: controller.openState(GameStateType.FAMILY); return true;
                case KeyEvent.VK_L: controller.openState(GameStateType.LIBRARY); return true;
                case KeyEvent.VK_J: controller.openState(GameStateType.QUEST_LOG); return true;
                default: break;
            }
            if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                useAbilitySlot(code - KeyEvent.VK_1);
                return true;
            }
        }
        return false;
    }

    private void useAbilitySlot(int slot) {
        Views.CharacterView p = controller.state().player();
        if (p == null) return;
        java.util.List<Views.AbilityView> abils = p.abilities();
        if (slot < 0 || slot >= abils.size()) return;
        SoundHooks.get().play(SoundHooks.Cue.ABILITY_CAST);
        controller.useAbility(abils.get(slot).id(), hud.selectedTargetIndex());
    }

    private boolean isOverlay(GameStateType st) {
        switch (st) {
            case INVENTORY: case CHARACTER_SHEET: case QUEST_LOG: case STUDY:
            case CRAFTING: case AUCTION: case FAMILY: case LIBRARY: case SETTINGS:
                return true;
            default: return false;
        }
    }

    // ---- ChangeListener: called on the EDT after every tick/intent ----
    public void onStateChanged() {
        if (SwingUtilities.isEventDispatchThread()) refresh();
        else SwingUtilities.invokeLater(new Runnable() { public void run() { refresh(); } });
    }

    private void refresh() {
        GameStateType st = controller.state().state();
        String card = CARD_HUD;
        if (st == GameStateType.TITLE || st == GameStateType.GAME_OVER) card = CARD_TITLE;
        else if (st == GameStateType.CHARACTER_CREATION) card = CARD_CREATION;
        cardLayout.show(cards, card);

        if (st == GameStateType.CHARACTER_CREATION) creation.refresh();
        hud.refresh();

        boolean showOverlay = isOverlay(st);
        overlay.setVisible(showOverlay);
        if (showOverlay) overlay.repaint();
        layers.repaint();
    }

    /** Convenience launcher used by app.Main and the stub demo. */
    public static void launch(final GameController controller) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new GameFrame(controller).setVisible(true); }
        });
    }
}
