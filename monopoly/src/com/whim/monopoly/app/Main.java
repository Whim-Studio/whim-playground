package com.whim.monopoly.app;

import com.whim.monopoly.data.Cards;
import com.whim.monopoly.domain.Board;
import com.whim.monopoly.domain.DefaultPlayer;
import com.whim.monopoly.domain.Player;
import com.whim.monopoly.domain.StandardBoard;
import com.whim.monopoly.engine.GameEngine;
import com.whim.monopoly.engine.StandardGameEngine;
import com.whim.monopoly.ui.MonopolyFrame;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Entry point for the Monopoly Swing app. Wires the domain (board + players +
 * card decks) into the rules engine and launches the UI on the EDT.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        final Board board = new StandardBoard();

        final List<Player> players = new ArrayList<Player>();
        players.add(new DefaultPlayer(0, "Red", new Color(0xD32F2F)));
        players.add(new DefaultPlayer(1, "Blue", new Color(0x1976D2)));
        players.add(new DefaultPlayer(2, "Green", new Color(0x388E3C)));
        players.add(new DefaultPlayer(3, "Gold", new Color(0xF9A825)));

        final GameEngine engine = new StandardGameEngine(
                players, board, Cards.chance(), Cards.communityChest(), new Random());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MonopolyFrame(engine).setVisible(true);
            }
        });
    }
}
