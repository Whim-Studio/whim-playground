package com.taipan.view;

import com.taipan.controller.CombatSession;
import com.taipan.controller.GameController;
import com.taipan.controller.VoyageResult;
import com.taipan.model.PortCity;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * The single application window and screen manager. Holds the active
 * {@link GameController} and swaps between screens with a {@link CardLayout}.
 * All cross-screen flow (starting a game, resolving a voyage, ending a game)
 * funnels through this class.
 */
public class GameFrame extends JFrame {

    private static final String NEW_GAME = "new_game";
    private static final String PORT = "port";
    private static final String TRAVEL = "travel";
    private static final String COMBAT = "combat";
    private static final String END = "end";

    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    private final NewGamePanel newGamePanel;
    private final PortPanel portPanel;
    private final TravelPanel travelPanel;
    private final CombatPanel combatPanel;
    private final EndPanel endPanel;

    private GameController controller;
    private PortCity pendingArrival; // where we will land once the voyage resolves

    public GameFrame() {
        super("Taipan!  —  Far East Trading, 1860");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        newGamePanel = new NewGamePanel(this);
        portPanel = new PortPanel(this);
        travelPanel = new TravelPanel(this);
        combatPanel = new CombatPanel(this);
        endPanel = new EndPanel(this);

        container.add(newGamePanel, NEW_GAME);
        container.add(portPanel, PORT);
        container.add(travelPanel, TRAVEL);
        container.add(combatPanel, COMBAT);
        container.add(endPanel, END);

        setContentPane(container);
        setSize(820, 620);
        setLocationRelativeTo(null);
        cards.show(container, NEW_GAME);
    }

    public GameController getController() {
        return controller;
    }

    // ---------------------------------------------------------- screen switches

    public void startNewGame(String taipan, String firm, Long seed) {
        controller = new GameController(taipan, firm, seed);
        showPort();
    }

    public void showPort() {
        portPanel.refresh();
        cards.show(container, PORT);
    }

    public void showTravel() {
        travelPanel.refresh();
        cards.show(container, TRAVEL);
    }

    // ------------------------------------------------------------- voyage flow

    /** Called by TravelPanel once a destination has been chosen. */
    public void startVoyage(PortCity destination) {
        VoyageResult r = controller.beginVoyage(destination);
        pendingArrival = r.actualArrival;

        if (!r.log.isEmpty()) {
            JOptionPane.showMessageDialog(this, joinLog(r.log),
                    "Setting sail for " + destination.display(),
                    JOptionPane.INFORMATION_MESSAGE);
        }

        if (r.gameOver) {
            showEnd();
            return;
        }

        if (r.liYuenDemand > 0) {
            handleLiYuen(r.liYuenDemand);
        }

        if (r.combat != null) {
            combatPanel.begin(r.combat);
            cards.show(container, COMBAT);
            return; // combat panel resumes via finishVoyage()
        }

        finishVoyage();
    }

    private void handleLiYuen(long demand) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Li Yuen's fleet blocks your path and demands tribute of $" + demand
                        + " for safe passage.\n\nPay the tribute?",
                "Li Yuen Demands Tribute", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            String err = controller.payLiYuen(demand);
            if (err != null) {
                JOptionPane.showMessageDialog(this,
                        err + "\nYou cannot pay in full; Li Yuen sneers and lets you pass this once.",
                        "Li Yuen", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Li Yuen accepts your tribute. His fleet will protect you this voyage.",
                        "Li Yuen", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "You refuse. Li Yuen scowls but lets you pass — this time.",
                    "Li Yuen", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /** Resume after combat (or immediately if there was none). */
    public void finishVoyage() {
        if (controller.getState().isGameOver()) {
            showEnd();
            return;
        }
        controller.arrive(pendingArrival);
        String flavour = controller.priceFlavour();
        if (flavour != null) {
            JOptionPane.showMessageDialog(this, flavour,
                    controller.getState().getLocation().display(),
                    JOptionPane.INFORMATION_MESSAGE);
        }
        showPort();
    }

    // -------------------------------------------------------------- end of game

    public void retire() {
        controller.retire();
        showEnd();
    }

    public void showEnd() {
        endPanel.refresh();
        cards.show(container, END);
    }

    public void backToNewGame() {
        controller = null;
        newGamePanel.reset();
        cards.show(container, NEW_GAME);
    }

    private static String joinLog(java.util.List<String> log) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < log.size(); i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(log.get(i));
        }
        return sb.toString();
    }
}
