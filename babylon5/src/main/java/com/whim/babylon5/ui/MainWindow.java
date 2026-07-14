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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.whim.babylon5.domain.Card;
import com.whim.babylon5.domain.CardType;
import com.whim.babylon5.domain.ConflictType;
import com.whim.babylon5.domain.GameFactory;
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
    private final JButton deployBtn = new JButton("Deploy Selected");
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

        javax.swing.JMenu game = new javax.swing.JMenu("Game");
        javax.swing.JMenuItem newGame = new javax.swing.JMenuItem("New Game");
        newGame.addActionListener(e -> newGame());
        game.add(newGame);
        bar.add(game);

        javax.swing.JMenu cards = new javax.swing.JMenu("Cards");
        javax.swing.JMenuItem editCards = new javax.swing.JMenuItem("Edit Cards…");
        editCards.addActionListener(e -> new CardEditorDialog(this).setVisible(true));
        javax.swing.JMenuItem deckBuilder = new javax.swing.JMenuItem("Deck Builder…");
        deckBuilder.addActionListener(e -> new DeckEditorDialog(this).setVisible(true));
        cards.add(editCards);
        cards.add(deckBuilder);
        bar.add(cards);
        return bar;
    }

    /** Start a fresh standard game in a new window, applying any card/deck edits. */
    private void newGame() {
        GameState st = GameFactory.newStandardGame(System.currentTimeMillis());
        MainWindow w = new MainWindow(new GameEngine(st));
        dispose();
        worker.shutdownNow();
        w.start();
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
        styleButton(deployBtn);
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
        deployBtn.addActionListener(e -> deploySelected());
        conflictBtn.addActionListener(e -> chooseAndDeclareConflict());
        bar.add(advanceBtn);
        bar.add(sponsorBtn);
        bar.add(deployBtn);
        bar.add(conflictBtn);
        JLabel hint = new JLabel("  CONFLICT: play a conflict card (1/turn) · ACTION: sponsor/deploy to bolster it · it resolves in RESOLUTION · AI plays itself");
        hint.setForeground(UiTheme.INK_DIM);
        bar.add(hint);
        return bar;
    }

    /**
     * Play the selected hand card. Enhancements prompt (on the EDT) for an in-play
     * host to attach to; everything else routes to {@link GameEngine#deployCard}.
     */
    private void deploySelected() {
        final Card c = selected;
        if (c == null) {
            appendLog("Select a card in your hand first.");
            return;
        }
        if (c.getType() == CardType.ENHANCEMENT) {
            PlayerState me = state.getActivePlayer();
            java.util.List<Card> hosts = new java.util.ArrayList<Card>();
            for (ZoneType zt : new ZoneType[] { ZoneType.INNER_CIRCLE, ZoneType.SUPPORTING }) {
                for (Card h : me.zone(zt).getCards()) {
                    if (engine.contributesToConflict(h)) {
                        hosts.add(h);
                    }
                }
            }
            if (hosts.isEmpty()) {
                appendLog("No in-play card to attach " + c.getName() + " to (sponsor a character first).");
                return;
            }
            Card host = (Card) JOptionPane.showInputDialog(this,
                    "Attach " + c.getName() + " to which card?", "Attach Enhancement",
                    JOptionPane.PLAIN_MESSAGE, null, hosts.toArray(new Card[0]), hosts.get(0));
            if (host == null) {
                return; // cancelled
            }
            submit(() -> {
                boolean ok = engine.attachEnhancement(state.getActiveIndex(), c, host);
                appendLog(ok ? "Attached " + c.getName() + " to " + host.getName() + "."
                             : "Cannot attach " + c.getName() + " now (cost/phase).");
                onStateChanged();
            });
            return;
        }
        submit(() -> {
            boolean ok = engine.deployCard(state.getActiveIndex(), c);
            appendLog(ok ? "Played " + c.getName() + " (" + c.getType() + ")."
                         : "Cannot play " + c.getName() + " now (cost/phase/type).");
            onStateChanged();
        });
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

    /**
     * Ask the human which conflict card (or conflict-granting agenda) to initiate with and
     * against whom (EDT), then hand the declaration to the worker. Rulebook: a conflict must
     * be initiated by a conflict card or an agenda, at most one per turn, and its discipline
     * is fixed by the card. Declared conflicts resolve during the Resolution round.
     */
    private void chooseAndDeclareConflict() {
        int me = state.getActiveIndex();
        if (state.getActivePlayer().hasInitiatedConflictThisTurn()) {
            appendLog("You have already initiated a conflict this turn (one per turn).");
            return;
        }

        // Legal initiators: conflict cards in hand + conflict-granting agendas in play.
        java.util.List<Card> conflictCards = engine.conflictCardsInHand(me);
        java.util.List<Card> agendas = engine.conflictAgendasInPlay(me);
        java.util.List<Card> initiators = new java.util.ArrayList<Card>();
        initiators.addAll(conflictCards);
        initiators.addAll(agendas);
        if (initiators.isEmpty()) {
            appendLog("No conflict card in hand and no conflict-granting agenda in play — cannot declare a conflict.");
            return;
        }

        // Label each initiator with its name + discipline so the choice is clear.
        String[] labels = new String[initiators.size()];
        for (int i = 0; i < initiators.size(); i++) {
            Card c = initiators.get(i);
            ConflictType t = c.getConflictType();
            labels[i] = c.getName()
                    + (c.getType() == CardType.AGENDA ? "  (agenda)" : "  (" + (t == null ? "?" : t) + ")");
        }
        JComboBox<String> cardBox = new JComboBox<String>(labels);

        java.util.List<Integer> oppIdx = new java.util.ArrayList<Integer>();
        java.util.List<String> oppNames = new java.util.ArrayList<String>();
        for (int i = 0; i < state.getPlayers().size(); i++) {
            if (i != me) {
                oppIdx.add(i);
                oppNames.add(state.getPlayers().get(i).getName());
            }
        }
        JComboBox<String> targetBox = new JComboBox<String>(oppNames.toArray(new String[0]));

        // Agendas let the player pick the discipline; conflict cards fix it.
        JComboBox<ConflictType> typeBox = new JComboBox<ConflictType>(ConflictType.values());

        JPanel panel = new JPanel(new java.awt.GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Conflict card / agenda:"));
        panel.add(cardBox);
        panel.add(new JLabel("Discipline (agenda only):"));
        panel.add(typeBox);
        panel.add(new JLabel("Target:"));
        panel.add(targetBox);

        int ok = JOptionPane.showConfirmDialog(this, panel, "Declare Conflict",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        final Card source = initiators.get(Math.max(0, cardBox.getSelectedIndex()));
        final ConflictType chosenType = (ConflictType) typeBox.getSelectedItem();
        final int target = oppIdx.get(Math.max(0, targetBox.getSelectedIndex()));
        submit(() -> {
            boolean declared = engine.declareConflict(me, source, chosenType, target);
            if (declared) {
                appendLog("Conflict declared — it resolves in the Resolution round. "
                        + "Sponsor/deploy in ACTION to bolster your side, then advance.");
            } else {
                appendLog("Could not declare that conflict now.");
            }
            onStateChanged();
        });
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
        phaseLabel.setText(buildPhaseStepper(active));
        phaseLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 12));

        // Center banner: reflect the declared-but-unresolved conflict while it is pending.
        Conflict pending = engine.getPendingConflict();
        if (pending != null && state.getPhase() != Phase.RESOLUTION) {
            String src = pending.getSourceCard() == null ? "" : pending.getSourceCard().getName() + " — ";
            centerLabel.setText("<html><center>⚔ Pending: " + src + pending.getType()
                    + " conflict vs " + state.getPlayers().get(pending.getTarget()).getName()
                    + "<br><font size='2'>resolves in the Resolution round</font></center></html>");
            centerLabel.setForeground(UiTheme.GOLD);
        }

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
        deployBtn.setEnabled(myTurn
                && (state.getPhase() == Phase.ACTION || state.getPhase() == Phase.RESOLUTION));
        conflictBtn.setEnabled(myTurn && state.getPhase() == Phase.CONFLICT);

        revalidate();
        repaint();
    }

    /** A compact READY ▸ CONFLICT ▸ ACTION ▸ RESOLUTION ▸ DRAW stepper with the current phase lit. */
    private String buildPhaseStepper(PlayerState active) {
        Phase current = state.getPhase();
        Phase[] order = { Phase.READY, Phase.CONFLICT, Phase.ACTION, Phase.RESOLUTION, Phase.DRAW };
        StringBuilder sb = new StringBuilder("<html>Turn ").append(state.getTurn())
                .append(" &nbsp; ");
        for (int i = 0; i < order.length; i++) {
            boolean on = order[i] == current;
            String color = hex(on ? UiTheme.GOLD : UiTheme.INK_DIM);
            sb.append("<span style='color:#").append(color).append("'>")
              .append(on ? "<b>" : "").append(order[i]).append(on ? "</b>" : "")
              .append("</span>");
            if (i < order.length - 1) {
                sb.append(" <span style='color:#").append(hex(UiTheme.PANEL_HI)).append("'>▸</span> ");
            }
        }
        sb.append(" &nbsp; | &nbsp; ").append(active == null ? "-" : esc(active.getName()));
        return sb.append("</html>").toString();
    }

    private static String hex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
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
