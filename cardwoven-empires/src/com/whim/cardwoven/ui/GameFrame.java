package com.whim.cardwoven.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.whim.cardwoven.api.ActionResult;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.GameController;
import com.whim.cardwoven.api.Views.CardView;
import com.whim.cardwoven.api.Views.GameStateView;
import com.whim.cardwoven.api.Views.PlayerView;
import com.whim.cardwoven.api.Views.TileView;

/**
 * Top-level window. Lays out the map (CENTER), hand dashboard (BOTTOM), the
 * side readout (WEST) and event log (EAST), plus a control bar. Owns the
 * click-and-drop interaction: select a card in {@link HandPanel}, then click a
 * tile in {@link MapPanel} and the frame routes it to the right
 * {@link GameController} action based on the card's {@link CardType}.
 *
 * The frame talks to the controller ONLY through the api seam; swap
 * {@link StubController} for the real engine and nothing here changes.
 */
public class GameFrame extends JFrame {

    private final GameController controller;
    private final MapPanel mapPanel;
    private final HandPanel handPanel;
    private final SidePanel sidePanel;
    private final LogPanel logPanel;
    private final JLabel selectionHint;

    public GameFrame(GameController controller) {
        super("Cardwoven Empires");
        this.controller = controller;

        this.mapPanel = new MapPanel(controller);
        this.handPanel = new HandPanel(controller);
        this.sidePanel = new SidePanel(controller);
        this.logPanel = new LogPanel(controller);
        this.selectionHint = new JLabel("Select a card, then click a tile to play it.");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(UiColors.WINDOW_BG);
        setLayout(new BorderLayout(6, 6));

        add(buildTopBar(), BorderLayout.NORTH);
        add(wrap(mapPanel, "World Map"), BorderLayout.CENTER);
        add(wrap(sidePanel, "Empire"), BorderLayout.WEST);
        add(wrap(logPanel, "Chronicle"), BorderLayout.EAST);
        add(buildBottom(), BorderLayout.SOUTH);

        mapPanel.setTileClickListener(new MapPanel.TileClickListener() {
            @Override public void onTileClicked(int row, int col) { onTile(row, col); }
        });
        handPanel.setCardSelectListener(new HandPanel.CardSelectListener() {
            @Override public void onCardSelected(int cardId) { onCardSelected(cardId); }
        });

        setMinimumSize(new Dimension(980, 640));
        setSize(new Dimension(1180, 760));
        setLocationRelativeTo(null);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiColors.PANEL_BG_2);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel title = new JLabel("CARDWOVEN EMPIRES");
        title.setForeground(UiColors.SELECT_GLOW);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        bar.add(title, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));

        final JComboBox<String> factionBox = new JComboBox<String>();
        Faction[] fs = Faction.values();
        for (int i = 0; i < fs.length; i++) factionBox.addItem(fs[i].display());

        JButton newGame = accentButton("New Game", UiColors.DECK);
        newGame.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                Faction chosen = Faction.values()[factionBox.getSelectedIndex()];
                controller.newGame(chosen);
                handPanel.clearSelection();
                mapPanel.setSelection(-1, -1);
                logPanel.setStatus("New game as " + chosen.display(), true);
                refresh();
            }
        });

        right.add(new JLabel(" "));
        JLabel fl = new JLabel("Faction: ");
        fl.setForeground(UiColors.TEXT_MUTED);
        right.add(fl);
        right.add(factionBox);
        right.add(Box.createHorizontalStrut(10));
        right.add(newGame);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBottom() {
        JPanel south = new JPanel(new BorderLayout(6, 0));
        south.setBackground(UiColors.WINDOW_BG);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new GridLayout(3, 1, 0, 8));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton endTurn = accentButton("End Turn ⏭", UiColors.GOLD);
        endTurn.setFont(endTurn.getFont().deriveFont(Font.BOLD, 15f));
        endTurn.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                ActionResult r = controller.endTurn();
                logPanel.setStatus(r.message(), r.isSuccess());
                handPanel.clearSelection();
                refresh();
            }
        });

        JButton cancel = accentButton("Clear Selection", UiColors.PANEL_BG_2);
        cancel.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                handPanel.clearSelection();
                mapPanel.setSelection(-1, -1);
                updateHint();
            }
        });

        selectionHint.setForeground(UiColors.TEXT_MUTED);
        selectionHint.setFont(selectionHint.getFont().deriveFont(Font.PLAIN, 11f));

        controls.add(endTurn);
        controls.add(cancel);
        controls.add(selectionHint);

        south.add(wrap(handPanel, "Hand"), BorderLayout.CENTER);
        south.add(controls, BorderLayout.EAST);
        return south;
    }

    private JButton accentButton(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(accent);
        b.setForeground(UiColors.TEXT_DARK);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return b;
    }

    private JPanel wrap(JPanel inner, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(UiColors.PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiColors.GRID_LINE, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        JLabel cap = new JLabel("  " + title);
        cap.setForeground(UiColors.TEXT_MUTED);
        cap.setFont(cap.getFont().deriveFont(Font.BOLD, 11f));
        cap.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(cap, BorderLayout.NORTH);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // ---- Interaction -----------------------------------------------------

    private void onCardSelected(int cardId) {
        updateHint();
    }

    private void onTile(int row, int col) {
        int cardId = handPanel.selectedCardId();
        if (cardId < 0) {
            // No card selected — just inspect the tile.
            updateHint();
            return;
        }
        CardView card = findHandCard(cardId);
        if (card == null) {
            logPanel.setStatus("That card is no longer in hand.", false);
            handPanel.clearSelection();
            refresh();
            return;
        }

        ActionResult result;
        CardType type = card.type();
        if (type == CardType.BUILDING) {
            result = controller.playBuilding(cardId, row, col);
        } else if (type == CardType.ATTACHMENT) {
            TileView tile = controller.state().map().tile(row, col);
            if (tile.building() == null) {
                result = ActionResult.fail("Attachments need a building — click a tile with one.");
            } else {
                result = controller.attachCard(cardId, tile.building().id());
            }
        } else if (type == CardType.MILITARY) {
            result = controller.resolveCombat(cardId, row, col);
        } else if (type == CardType.SIN) {
            result = ActionResult.fail("Sin cards cannot be played — discard at end of turn.");
        } else { // ECONOMY / EXPLORE
            result = controller.playCard(cardId, row, col);
        }

        logPanel.setStatus(result.message(), result.isSuccess());
        if (result.isSuccess()) handPanel.clearSelection();
        refresh();
    }

    private CardView findHandCard(int cardId) {
        GameStateView state = controller.state();
        PlayerView me = state.currentPlayer();
        if (me == null || me.hand() == null) return null;
        for (int i = 0; i < me.hand().size(); i++) {
            CardView c = me.hand().get(i);
            if (c.id() == cardId) return c;
        }
        return null;
    }

    private void updateHint() {
        int cardId = handPanel.selectedCardId();
        CardView card = cardId >= 0 ? findHandCard(cardId) : null;
        if (card == null) {
            selectionHint.setText("Select a card, then click a tile to play it.");
            return;
        }
        String verb;
        CardType t = card.type();
        if (t == CardType.BUILDING) verb = "click an empty land tile to build.";
        else if (t == CardType.ATTACHMENT) verb = "click one of your buildings to attach.";
        else if (t == CardType.MILITARY) verb = "click a raider/enemy tile to attack.";
        else if (t == CardType.EXPLORE) verb = "click a tile to reveal it.";
        else if (t == CardType.SIN) verb = "unplayable — end turn to discard.";
        else verb = "click any tile to play.";
        selectionHint.setText("Selected: " + card.name() + " — " + verb);
    }

    /** Re-read state and repaint all panels. */
    public void refresh() {
        mapPanel.repaint();
        handPanel.repaint();
        sidePanel.repaint();
        logPanel.repaint();
        updateHint();
    }

    // ---- Dev entry point -------------------------------------------------

    /**
     * Launches the UI against the {@link StubController} so the whole interface
     * is runnable before the real engine exists.
     *
     * A {@link JFrame} cannot be constructed at all in a headless JVM, so under
     * {@code -Djava.awt.headless=true} we skip the window and instead run a
     * headless smoke test: build the stub controller and every panel (panels are
     * headless-safe), render them to an off-screen image (exercising all the
     * Graphics2D paint paths), and drive a few controller actions. This proves
     * the UI layer wires up and paints without throwing, which is all CI can
     * check without a display.
     */
    public static void main(String[] args) {
        final boolean headless = GraphicsEnvironment.isHeadless()
                || Boolean.getBoolean("java.awt.headless");
        final GameController controller = new StubController();
        if (headless) {
            smokeTest(controller);
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                GameFrame frame = new GameFrame(controller);
                frame.setVisible(true);
            }
        });
    }

    /** Headless verification: construct + paint panels and run sample actions. */
    static void smokeTest(GameController controller) {
        MapPanel map = new MapPanel(controller);
        HandPanel hand = new HandPanel(controller);
        SidePanel side = new SidePanel(controller);
        LogPanel log = new LogPanel(controller);
        map.setSize(560, 460);
        hand.setSize(900, 180);
        side.setSize(230, 460);
        log.setSize(230, 200);

        paintOffscreen(map);
        paintOffscreen(hand);
        paintOffscreen(side);
        paintOffscreen(log);

        GameStateView s = controller.state();
        CardView first = s.currentPlayer().hand().get(0);
        ActionResult play = controller.playBuilding(first.id(), 4, 4);
        ActionResult turn = controller.endTurn();
        // Repaint after mutations to exercise the updated-state paint paths.
        paintOffscreen(map);
        paintOffscreen(hand);
        paintOffscreen(side);

        System.out.println("[headless smoke] OK");
        System.out.println("  map " + s.map().rows() + "x" + s.map().cols()
                + ", players " + s.players().size());
        System.out.println("  starting hand " + first.name() + " (+" + (s.currentPlayer().handSize())
                + " in hand now)");
        System.out.println("  playBuilding -> " + play);
        System.out.println("  endTurn      -> " + turn);
        System.out.println("  gold now " + s.currentPlayer().resource(
                com.whim.cardwoven.api.Enums.ResourceType.GOLD));
    }

    private static void paintOffscreen(JPanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        panel.paint(g);
        g.dispose();
    }
}
