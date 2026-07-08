package klahklok;

import javax.swing.*;
import java.util.*;

/**
 * Entry point for the standalone Klah Klok dice game. Prompts for game mode,
 * builds the domain/engine/AI objects, and shows the Swing frame on the EDT.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                start();
            }
        });
    }

    private static void start() {
        String[] modes = { "2 Player (Hotseat)", "1 Player vs Computer" };
        int choice = JOptionPane.showOptionDialog(null,
                "Choose game mode:",
                "Klah Klok",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                modes,
                modes[0]);

        if (choice < 0) {
            return; // dialog dismissed
        }

        boolean vsComputer = (choice == 1);
        int startBankroll = 1000;

        List<Player> players = new ArrayList<Player>();
        if (vsComputer) {
            players.add(new Player("Player 1", startBankroll, false));
            players.add(new Player("Computer", startBankroll, true));
        } else {
            players.add(new Player("Player 1", startBankroll, false));
            players.add(new Player("Player 2", startBankroll, false));
        }

        GameState state = new GameState(players);
        ResolutionEngine engine = new ResolutionEngine();
        AIController ai = new AIController(new Random());
        Die[] dice = { new Die(), new Die(), new Die() };

        KlahKlokFrame frame = new KlahKlokFrame(state, engine, ai, dice, vsComputer);
        frame.setVisible(true);
    }
}
