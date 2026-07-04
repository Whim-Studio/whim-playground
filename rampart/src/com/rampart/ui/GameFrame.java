package com.rampart.ui;

import com.rampart.engine.GameApi;
import com.rampart.model.GameStateView;
import com.rampart.model.Phase;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Top-level window for the Rampart UI. Constructed with any {@link GameApi} (the
 * {@link StubGameApi} for standalone dev, the real engine from {@code app.Main}),
 * it hosts a {@link CardLayout} that swaps four screens chosen by the snapshot's
 * {@link Phase}:
 * <ul>
 *   <li>{@code TITLE} &rarr; {@link Phase#TITLE}</li>
 *   <li>{@code GAME} (the {@link GamePanel}) &rarr; {@link Phase#BUILD},
 *       {@link Phase#BATTLE}, {@link Phase#REPAIR}</li>
 *   <li>{@code ROUND} &rarr; {@link Phase#ROUND_TRANSITION}</li>
 *   <li>{@code GAMEOVER} &rarr; {@link Phase#GAME_OVER}</li>
 * </ul>
 *
 * <p>The single game loop is a {@link Timer}: each tick calls {@code api.tick(dt)},
 * swaps the card if the phase changed, and repaints. All work happens on the EDT.
 * This class holds no game logic — it renders {@code state()} and forwards input.
 */
public final class GameFrame extends JFrame {

    /** Card names. */
    private static final String CARD_TITLE = "TITLE";
    private static final String CARD_GAME = "GAME";
    private static final String CARD_ROUND = "ROUND";
    private static final String CARD_GAMEOVER = "GAMEOVER";

    /** Frame period in ms (~30 fps). Passed to {@link GameApi#tick(long)}. */
    private static final int FRAME_MS = 33;

    private final GameApi api;
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);

    private final GamePanel gamePanel;
    private final TitleScreen titleScreen;
    private final RoundScreen roundScreen;
    private final GameOverScreen gameOverScreen;

    private Timer loop;
    private String currentCard = CARD_TITLE;

    public GameFrame(GameApi api) {
        super("RAMPART (1990, Atari Games) — Java 8 Swing");
        this.api = api;
        this.gamePanel = new GamePanel(api);
        this.titleScreen = new TitleScreen();
        this.roundScreen = new RoundScreen();
        this.gameOverScreen = new GameOverScreen();

        InputHandler input = new InputHandler(api, gamePanel);
        input.attach();

        Dimension size = new Dimension(GamePanel.PANEL_W, GamePanel.PANEL_H);
        deck.setPreferredSize(size);
        deck.add(titleScreen, CARD_TITLE);
        deck.add(gamePanel, CARD_GAME);
        deck.add(roundScreen, CARD_ROUND);
        deck.add(gameOverScreen, CARD_GAMEOVER);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(deck);
        pack();
        setLocationRelativeTo(null);
    }

    /** Reset to a fresh game, show the frame, and start the tick loop. */
    public void launch() {
        api.newGame();
        showCardFor(api.state().phase());
        setVisible(true);
        startLoop();
    }

    private void startLoop() {
        loop = new Timer(FRAME_MS, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                api.tick(FRAME_MS);
                Phase phase = api.state().phase();
                showCardFor(phase);
                deck.repaint();
            }
        });
        loop.start();
    }

    /** Stop the loop (used by app.Main teardown if desired). */
    public void shutdown() {
        if (loop != null) loop.stop();
    }

    /** Map a phase to its card and switch if it changed, moving focus with it. */
    private void showCardFor(Phase phase) {
        String card = cardFor(phase);
        if (!card.equals(currentCard)) {
            currentCard = card;
            cards.show(deck, card);
            focusActive(card);
        }
    }

    private void focusActive(String card) {
        if (CARD_GAME.equals(card)) {
            gamePanel.requestFocusInWindow();
        } else if (CARD_TITLE.equals(card)) {
            titleScreen.requestFocusInWindow();
        } else if (CARD_GAMEOVER.equals(card)) {
            gameOverScreen.requestFocusInWindow();
        } else {
            roundScreen.requestFocusInWindow();
        }
    }

    private static String cardFor(Phase phase) {
        if (phase == null) return CARD_TITLE;
        switch (phase) {
            case TITLE:            return CARD_TITLE;
            case BUILD:
            case BATTLE:
            case REPAIR:           return CARD_GAME;
            case ROUND_TRANSITION: return CARD_ROUND;
            case GAME_OVER:        return CARD_GAMEOVER;
            default:               return CARD_TITLE;
        }
    }

    /** Content dimensions the frame targets (sanity helper for headless checks). */
    public static int contentWidth()  { return GamePanel.PANEL_W; }
    public static int contentHeight() { return GamePanel.PANEL_H; }

    // ---- Screens (procedurally painted JPanels swapped by CardLayout) --------

    private static Graphics2D prep(Graphics g0, JPanel p) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Palette.SCREEN_BG);
        g.fillRect(0, 0, p.getWidth(), p.getHeight());
        return g;
    }

    private static void centered(Graphics2D g, String s, int y, int w) {
        int x = (w - g.getFontMetrics().stringWidth(s)) / 2;
        g.drawString(s, x, y);
    }

    /** TITLE screen: press ENTER to begin the current round. */
    private final class TitleScreen extends JPanel {
        TitleScreen() {
            setPreferredSize(new Dimension(GamePanel.PANEL_W, GamePanel.PANEL_H));
            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER
                            || e.getKeyCode() == KeyEvent.VK_SPACE) {
                        api.startRound();
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = prep(g0, this);
            int w = getWidth();
            g.setColor(Palette.SCREEN_TITLE);
            g.setFont(new Font("SansSerif", Font.BOLD, 64));
            centered(g, "RAMPART", getHeight() / 2 - 40, w);
            g.setColor(Palette.HUD_TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            centered(g, "1990 Atari Games — Java 8 Swing recreation", getHeight() / 2 + 6, w);
            g.setColor(Palette.HUD_LABEL);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            centered(g, "Press ENTER to build", getHeight() / 2 + 70, w);
            g.setColor(Palette.HUD_TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            centered(g, "BUILD: place cannons   BATTLE: click to fire   "
                    + "REPAIR: drop wall pieces (R rotate)", getHeight() - 60, w);
        }
    }

    /** ROUND_TRANSITION splash showing the round the player just cleared. */
    private final class RoundScreen extends JPanel {
        RoundScreen() {
            setPreferredSize(new Dimension(GamePanel.PANEL_W, GamePanel.PANEL_H));
            setFocusable(true);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = prep(g0, this);
            GameStateView s = api.state();
            int w = getWidth();
            g.setColor(Palette.SCREEN_TITLE);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            centered(g, "ROUND " + s.round(), getHeight() / 2 - 20, w);
            g.setColor(Palette.HUD_TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            centered(g, "Territory secured — prepare to defend", getHeight() / 2 + 24, w);
            g.setColor(Palette.HUD_LABEL);
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            centered(g, "Score " + s.score(), getHeight() / 2 + 60, w);
        }
    }

    /** GAME_OVER screen: press ENTER to return to the title / new game. */
    private final class GameOverScreen extends JPanel {
        GameOverScreen() {
            setPreferredSize(new Dimension(GamePanel.PANEL_W, GamePanel.PANEL_H));
            setFocusable(true);
            addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER
                            || e.getKeyCode() == KeyEvent.VK_SPACE) {
                        api.newGame();
                    }
                }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = prep(g0, this);
            GameStateView s = api.state();
            int w = getWidth();
            g.setColor(Palette.HUD_WARN);
            g.setFont(new Font("SansSerif", Font.BOLD, 56));
            centered(g, "GAME OVER", getHeight() / 2 - 30, w);
            g.setColor(Palette.HUD_TEXT);
            g.setFont(new Font("SansSerif", Font.PLAIN, 22));
            centered(g, "Reached round " + s.round() + "   —   Score " + s.score(),
                    getHeight() / 2 + 20, w);
            g.setColor(Palette.HUD_LABEL);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            centered(g, "Press ENTER for a new game", getHeight() / 2 + 70, w);
        }
    }
}
