package com.whim.babylon5.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.GameListener;
import com.whim.babylon5.domain.GameState;
import com.whim.babylon5.domain.Phase;
import com.whim.babylon5.domain.PlayerState;
import com.whim.babylon5.domain.ZoneType;
import com.whim.babylon5.engine.Conflict;
import com.whim.babylon5.engine.ConflictResult;
import com.whim.babylon5.engine.GameEngine;

/**
 * Babylon 5 CCG virtual tabletop (Task 3).
 *
 * <p>Layout: the three AI opponents occupy the top stack, a center "Conflict Resolution"
 * area shows the active player + phase + the last resolved conflict, the human player's
 * Inner Circle / Supporting rows sit below, and the hand + control bar anchor the bottom.
 * A side log mirrors {@link GameListener} events.
 *
 * <p>Threading: {@link #start()} shows the window on the EDT, then ALL engine / AI work
 * (advance phase, sponsor, resolve, AI turns) runs on a single background worker thread.
 * UI mutations are marshalled back via {@link SwingUtilities#invokeLater}. The engine is
 * never touched from the EDT and Swing is never touched from the worker.
 */
public final class MainWindow extends JFrame implements GameListener {

    private final GameEngine engine;
    private final GameState state;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "b5-engine");
        t.setDaemon(true);
        return t;
    });

    private final JPanel opponents = vstack();
    private final JPanel humanZones = vstack();
    private final JPanel handRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JTextArea log = new JTextArea();
    private final JLabel phaseLabel = new JLabel();
    private final JLabel centerLabel = new JLabel("", JLabel.CENTER);
    private final JButton advanceBtn = new JButton("Advance Phase ▶");
    private final JButton sponsorBtn = new JButton("Sponsor Selected");
    private final JButton conflictBtn = new JButton("Declare Conflict");

    private Card selected;

    public MainWindow(GameEngine engine) {
        super("Babylon 5 CCG — Standalone (1 human vs 3 AI)");
        this.engine = engine;
        this.state = engine.getState();
        engine.addListener(this);
        buildUi();
    }

    /** Build/show on the EDT, then start the loop on the worker thread. */
    public void start() {
        Runnable show = () -> {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setMinimumSize(new Dimension(1180, 800));
            setLocationRelativeTo(null);
            refresh();
            setVisible(true);
            pumpAiIfNeeded();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            show.run();
        } else {
            SwingUtilities.invokeLater(show);
        }
    }

    // ---- UI construction (EDT) ------------------------------------------------

    private void buildUi() {
        getContentPane().setBackground(UiTheme.SPACE);
        setLayout(new BorderLayout(8, 8));
        setJMenuBar(buildMenuBar());

        JLabel title = new JLabel("BABYLON 5 — Collectible Card Game");
        title.setFont(UiTheme.H1);
        title.setForeground(UiTheme.GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        phaseLabel.setForeground(UiTheme.INK);
        phaseLabel.setFont(UiTheme.H2);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(phaseLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        opponents.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane oppScroll = transparentScroll(opponents);
        oppScroll.setPreferredSize(new Dimension(820, 360));

        centerLabel.setForeground(UiTheme.INK);
        centerLabel.setFont(UiTheme.H2);
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiTheme.ACCENT), "Conflict Resolution"));
        center.add(centerLabel, BorderLayout.CENTER);
        center.setPreferredSize(new Dimension(820, 90));

        humanZones.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        handRow.setOpaque(false);
        JScrollPane handScroll = transparentScroll(handRow);
        handScroll.setPreferredSize(new Dimension(820, 190));

        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.setOpaque(false);
        left.add(oppScroll, BorderLayout.NORTH);
        left.add(center, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        JLabel handLbl = new JLabel("Your Inner Circle / Supporting  +  Hand");
        handLbl.setForeground(UiTheme.INK_DIM);
        bottom.add(humanZones, BorderLayout.NORTH);
        bottom.add(handLbl, BorderLayout.CENTER);
        bottom.add(handScroll, BorderLayout.SOUTH);
        left.add(bottom, BorderLayout.SOUTH);
        add(left, BorderLayout.CENTER);

        add(buildSidebar(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);
    }

    private javax.swing.JMenuBar buildMenuBar() {
        javax.swing.JMenuBar bar = new javax.swing.JMenuBar();
        javax.swing.JMenu cards = new javax.swing.JMenu("Cards");
        javax.swing.JMenuItem edit = new javax.swing.JMenuItem("Edit Cards…");
        edit.addActionListener(e -> new CardEditorDialog(this).setVisible(true));
        cards.add(edit);
        bar.add(cards);
        return bar;
    }

    private JPanel buildSidebar() {
        log.setEditable(false);
        log.setBackground(UiTheme.PANEL);
        log.setForeground(UiTheme.INK);
        log.setFont(UiTheme.MONO);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(log);
        sp.setPreferredSize(new Dimension(320, 0));
        sp.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UiTheme.PANEL_HI), "Game Log"));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        bar.setBackground(UiTheme.PANEL);
        styleButton(advanceBtn);
        styleButton(sponsorBtn);
        styleButton(conflictBtn);
        advanceBtn.addActionListener(e -> submit(() -> {
            engine.advancePhase();
            pumpAiIfNeeded();
        }));
        sponsorBtn.addActionListener(e -> {
            final Card c = selected;
            if (c == null) {
                appendLog("Select a character in your hand first.");
                return;
            }
            submit(() -> {
                boolean ok = engine.sponsorCharacter(state.getActiveIndex(), c);
                appendLog(ok ? "Sponsored " + c.getName() + "."
                             : "Cannot sponsor " + c.getName() + " now (cost/phase).");
                onStateChanged();
            });
        });
        conflictBtn.addActionListener(e -> submit(this::humanDeclareConflict));
        bar.add(advanceBtn);
        bar.add(sponsorBtn);
        bar.add(conflictBtn);
        JLabel hint = new JLabel("  Sponsor in ACTION · declare conflicts in CONFLICT · AI plays itself");
        hint.setForeground(UiTheme.INK_DIM);
        bar.add(hint);
        return bar;
    }

    // ---- Worker-thread game actions ------------------------------------------

    private void submit(Runnable r) {
        worker.submit(() -> {
            try {
                r.run();
            } catch (RuntimeException ex) {
                appendLog("ERROR: " + ex.getMessage());
            }
        });
    }

    /** While the active player is an AI, run its turn off the EDT, then continue. */
    private void pumpAiIfNeeded() {
        while (true) {
            PlayerState active = state.getActivePlayer();
            if (active == null || active.isHuman()) {
                break;
            }
            PlayerState winner = engine.checkVictory();
            if (winner != null) {
                announceWinner(winner);
                return;
            }
            engine.runAiTurn(state.getActiveIndex());
            try {
                Thread.sleep(350);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        PlayerState winner = engine.checkVictory();
        if (winner != null) {
            announceWinner(winner);
        }
    }

    /** Human declares a simple Diplomacy conflict from a ready Inner-Circle/Supporting card. */
    private void humanDeclareConflict() {
        int me = state.getActiveIndex();
        Conflict c = new Conflict(me, ConflictType.DIPLOMACY, nextOpponent(me));
        for (Card card : state.getActivePlayer().zone(ZoneType.SUPPORTING).getCards()) {
            if (card.isReady() && card.support(ConflictType.DIPLOMACY) > 0) {
                c.getSupport().add(card);
            }
        }
        for (Card card : state.getActivePlayer().zone(ZoneType.INNER_CIRCLE).getCards()) {
            if (card.isReady() && card.support(ConflictType.DIPLOMACY) > 0) {
                c.getSupport().add(card);
            }
        }
        if (c.getSupport().isEmpty()) {
            appendLog("No ready characters to support a Diplomacy conflict.");
            return;
        }
        ConflictResult r = engine.resolveConflict(c);
        appendLog(r.summary());
        onStateChanged();
    }

    private int nextOpponent(int me) {
        return (me + 1) % state.getPlayers().size();
    }

    // ---- GameListener (called from worker; marshal to EDT) -------------------

    @Override public void onPhaseChanged(Phase phase, int activeIndex) {
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override public void onConflictResolved(ConflictResult result) {
        SwingUtilities.invokeLater(() -> {
            centerLabel.setText("<html><center>" + result.summary() + "</center></html>");
            centerLabel.setForeground(result.initiatorWon() ? UiTheme.OK : UiTheme.DANGER);
        });
    }

    @Override public void onStateChanged() {
        SwingUtilities.invokeLater(this::refresh);
    }

    @Override public void onLog(String message) {
        appendLog(message);
    }

    // ---- Rendering (EDT) ------------------------------------------------------

    private void refresh() {
        PlayerState active = state.getActivePlayer();
        phaseLabel.setText("Turn " + state.getTurn() + "   ·   Phase: " + state.getPhase()
                + "   ·   Active: " + (active == null ? "-" : active.getName()));
        phaseLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 12));

        CardView.SelectionListener sel = (view, card) -> selectInHand(card);

        opponents.removeAll();
        for (int i = 0; i < state.getPlayers().size(); i++) {
            PlayerState p = state.getPlayers().get(i);
            if (p.isHuman()) {
                continue;
            }
            opponents.add(new PlayerPanel(p, i == state.getActiveIndex(),
                    engine.computePower(p), sel));
        }

        humanZones.removeAll();
        PlayerState human = humanPlayer();
        if (human != null) {
            humanZones.add(new PlayerPanel(human, human == active,
                    engine.computePower(human), sel));
        }

        handRow.removeAll();
        if (human != null) {
            for (Card c : human.zone(ZoneType.HAND).getCards()) {
                CardView v = new CardView(c);
                v.setSelectionListener((view, card) -> selectInHand(card));
                v.setSelected(c == selected);
                handRow.add(v);
            }
        }

        boolean myTurn = active != null && active.isHuman();
        advanceBtn.setEnabled(myTurn);
        sponsorBtn.setEnabled(myTurn && state.getPhase() == Phase.ACTION);
        conflictBtn.setEnabled(myTurn && state.getPhase() == Phase.CONFLICT);

        revalidate();
        repaint();
    }

    private void selectInHand(Card card) {
        selected = (selected == card) ? null : card;
        refresh();
    }

    private PlayerState humanPlayer() {
        for (PlayerState p : state.getPlayers()) {
            if (p.isHuman()) {
                return p;
            }
        }
        return null;
    }

    private void announceWinner(PlayerState winner) {
        SwingUtilities.invokeLater(() -> {
            centerLabel.setText("★ STANDARD VICTORY — " + winner.getName()
                    + " reaches " + engine.computePower(winner) + " Power ★");
            centerLabel.setForeground(UiTheme.GOLD);
            advanceBtn.setEnabled(false);
            sponsorBtn.setEnabled(false);
            conflictBtn.setEnabled(false);
        });
        appendLog("VICTORY: " + winner.getName());
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    // ---- small helpers --------------------------------------------------------

    private static JPanel vstack() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JScrollPane transparentScroll(JPanel view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private static void styleButton(JButton b) {
        b.setFocusPainted(false);
        b.setBackground(UiTheme.ACCENT);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
    }
}
