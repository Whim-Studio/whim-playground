package com.whim.ttr.ui;

import com.whim.ttr.api.ActionOutcome;
import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.GameConstants;
import com.whim.ttr.api.GameEngine;
import com.whim.ttr.api.GamePhase;
import com.whim.ttr.domain.DestinationTicket;
import com.whim.ttr.domain.GameState;
import com.whim.ttr.domain.PlayerScore;
import com.whim.ttr.domain.Route;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The main window: the procedural {@link BoardPanel} on top and the
 * {@link DashboardPanel} beneath it. Owns the single worker thread that runs all
 * {@link GameEngine} calls off the EDT; every result is applied back to Swing
 * via {@link SwingUtilities#invokeLater}.
 */
public class GameFrame extends JFrame {

    private final GameEngine engine;
    private final GameState state;
    private final BoardPanel board;
    private final DashboardPanel dashboard;

    /** All engine calls are serialized onto this single background thread. */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ttr-engine");
        t.setDaemon(true);
        return t;
    });

    private boolean gameOverShown = false;

    public GameFrame(GameEngine engine) {
        super("Ticket to Ride — Europe");
        this.engine = engine;
        this.state = engine.state();
        this.board = new BoardPanel(state);
        this.dashboard = new DashboardPanel(state);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(new JScrollPane(board), BorderLayout.CENTER);
        add(dashboard, BorderLayout.SOUTH);

        wireBoard();
        wireDashboard();

        setSize(1040, 940);
        setLocationRelativeTo(null);
    }

    /** Show the window and run any initial (SETUP-phase) ticket selection. */
    public void start() {
        setVisible(true);
        refresh();
        if (engine.phase() == GamePhase.SETUP) {
            stepSetup(0);
        }
    }

    // ---- wiring -------------------------------------------------------------

    private void wireBoard() {
        board.setClickListener(new BoardPanel.ClickListener() {
            @Override public void onRouteClicked(String routeId) { attemptClaim(routeId); }
            @Override public void onCityClicked(String cityId) { attemptStation(cityId); }
        });
    }

    private void wireDashboard() {
        dashboard.setListener(new DashboardPanel.Listener() {
            @Override public void onDrawFaceUp(int index) {
                run(() -> engine.drawTrainCard(index), GameFrame.this::showOutcome);
            }
            @Override public void onDrawBlind() {
                run(() -> engine.drawTrainCard(-1), GameFrame.this::showOutcome);
            }
            @Override public void onDrawTickets() { drawTickets(); }
            @Override public void onEndTurn() {
                run(engine::endTurn, GameFrame.this::showOutcome);
            }
        });
    }

    // ---- turn actions -------------------------------------------------------

    private void attemptClaim(String routeId) {
        Route r = state.board().route(routeId);
        if (r == null) {
            return;
        }
        if (r.ownerId() != null) {
            status("That route is already claimed.");
            return;
        }
        List<CardColor> hand = handOfCurrent();
        String info = describeRoute(r);
        List<CardColor> pay = CardPaymentDialog.prompt(this,
                "Claim route", "Claim " + r.cityA() + " – " + r.cityB() + "<br>" + info
                        + "<br>Choose the cards to pay.", hand);
        if (pay == null || pay.isEmpty()) {
            return;
        }
        run(() -> engine.beginClaimRoute(routeId, pay), outcome -> {
            if (outcome.isAwaitingTunnel()) {
                resolveTunnel(outcome);
            } else {
                showOutcome(outcome);
            }
        });
    }

    private void resolveTunnel(ActionOutcome pending) {
        StringBuilder flipped = new StringBuilder();
        for (CardColor c : pending.getTunnelDraw()) {
            if (flipped.length() > 0) {
                flipped.append(", ");
            }
            flipped.append(UiColors.label(c));
        }
        int extra = pending.getTunnelExtra();
        String prompt = "Tunnel! Flipped: " + flipped + "<br>"
                + "You must supply <b>" + extra + "</b> more matching card(s) to finish the claim,"
                + "<br>or cancel to abort.";
        List<CardColor> extraCards = CardPaymentDialog.prompt(this, "Tunnel", prompt, handOfCurrent());
        if (extraCards == null) {
            run(engine::cancelTunnel, this::showOutcome);
        } else {
            run(() -> engine.confirmTunnel(extraCards), this::showOutcome);
        }
    }

    private void attemptStation(String cityId) {
        List<CardColor> pay = CardPaymentDialog.prompt(this, "Build station",
                "Build a train station in <b>" + cityId + "</b>.<br>Choose the cards to pay.",
                handOfCurrent());
        if (pay == null || pay.isEmpty()) {
            return;
        }
        run(() -> engine.buildStation(cityId, pay), this::showOutcome);
    }

    private void drawTickets() {
        call(engine::offerTickets, offered -> {
            if (offered == null || offered.isEmpty()) {
                status("No tickets available.");
                return;
            }
            List<DestinationTicket> kept = TicketDialog.prompt(this, "Draw tickets",
                    offered, GameConstants.TICKETS_MIN_KEEP);
            run(() -> engine.keepTickets(kept), this::showOutcome);
        });
    }

    // ---- SETUP initial ticket selection (best-effort) -----------------------

    private void stepSetup(final int guard) {
        if (guard > GameConstants.MAX_PLAYERS + 1 || engine.phase() != GamePhase.SETUP) {
            refresh();
            return;
        }
        call(engine::offerTickets, offered -> {
            if (offered == null || offered.isEmpty()) {
                refresh();
                return;
            }
            List<DestinationTicket> kept = TicketDialog.prompt(this,
                    "Initial tickets — " + currentName(), offered,
                    GameConstants.START_TICKETS_MIN_KEEP);
            run(() -> {
                engine.keepTickets(kept);
                return engine.endTurn();
            }, outcome -> stepSetup(guard + 1));
        });
    }

    // ---- worker plumbing ----------------------------------------------------

    private void run(Callable<ActionOutcome> action, Consumer<ActionOutcome> onDone) {
        worker.submit(() -> {
            ActionOutcome outcome;
            try {
                outcome = action.call();
            } catch (Throwable t) {
                outcome = ActionOutcome.of(false, "Error: " + t.getMessage());
            }
            final ActionOutcome result = outcome;
            SwingUtilities.invokeLater(() -> {
                if (onDone != null) {
                    onDone.accept(result);
                }
                refresh();
            });
        });
    }

    private <T> void call(Callable<T> action, Consumer<T> onDone) {
        worker.submit(() -> {
            T value = null;
            Throwable err = null;
            try {
                value = action.call();
            } catch (Throwable t) {
                err = t;
            }
            final T result = value;
            final Throwable failure = err;
            SwingUtilities.invokeLater(() -> {
                if (failure != null) {
                    status("Error: " + failure.getMessage());
                } else if (onDone != null) {
                    onDone.accept(result);
                }
                refresh();
            });
        });
    }

    // ---- view refresh -------------------------------------------------------

    private void showOutcome(ActionOutcome o) {
        if (o != null && o.getMessage() != null && !o.getMessage().isEmpty()) {
            status(o.getMessage());
        }
    }

    private void status(String msg) {
        state.setLastMessage(msg);
        dashboard.refresh();
    }

    private void refresh() {
        board.repaint();
        dashboard.refresh();
        if (!gameOverShown && engine.isGameOver()) {
            gameOverShown = true;
            showFinalScores();
        }
    }

    private void showFinalScores() {
        call(engine::finalScores, scores -> {
            StringBuilder sb = new StringBuilder("<html><body style='width:360px'>");
            sb.append("<h2>Final Scores</h2>");
            if (scores != null) {
                for (PlayerScore s : scores) {
                    sb.append("<b>").append(nameOf(s.playerId())).append("</b>: ")
                      .append(s.total()).append(" pts<br>")
                      .append("&nbsp;routes ").append(s.routePoints())
                      .append(", tickets ").append(s.ticketPoints())
                      .append(", stations ").append(s.stationBonus());
                    if (s.hasLongestPath()) {
                        sb.append(", <i>European Express +")
                          .append(GameConstants.LONGEST_PATH_BONUS).append("</i>");
                    }
                    sb.append("<br><br>");
                }
            }
            sb.append("</body></html>");
            JOptionPane.showMessageDialog(this, sb.toString(), "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // ---- small helpers ------------------------------------------------------

    private List<CardColor> handOfCurrent() {
        return state.player(state.currentPlayerId()).hand();
    }

    private String currentName() {
        return nameOf(state.currentPlayerId());
    }

    private String nameOf(int id) {
        return state.player(id) != null ? state.player(id).name() : ("Player " + id);
    }

    private static String describeRoute(Route r) {
        String color = r.color() == null ? "GRAY" : r.color().name();
        String s = "length " + r.length() + ", " + color + ", " + r.kind();
        if (r.locomotivesRequired() > 0) {
            s += " (needs " + r.locomotivesRequired() + " loco)";
        }
        return s;
    }
}
