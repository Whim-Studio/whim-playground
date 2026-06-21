package com.xiangqi.ui;

import javax.swing.JOptionPane;

import com.xiangqi.core.Side;

/**
 * Small modal flow shown at launch: pick the game mode, and (for vs-Computer)
 * which colour the human plays. Returns null if the user cancels out.
 */
final class StartupDialog {

    /** Immutable bundle of the user's startup choices. */
    static final class Choice {
        final GameMode mode;
        final Side humanSide; // ignored for TWO_PLAYER

        Choice(GameMode mode, Side humanSide) {
            this.mode = mode;
            this.humanSide = humanSide;
        }
    }

    private StartupDialog() {
    }

    static Choice prompt() {
        String[] modes = {"Two Player (local)", "Player vs Computer"};
        int modeIdx = JOptionPane.showOptionDialog(null,
                "Welcome to Xiangqi (Chinese Chess).\nChoose a game mode:",
                "Xiangqi", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, modes, modes[0]);
        if (modeIdx == JOptionPane.CLOSED_OPTION) {
            return null;
        }
        if (modeIdx == 0) {
            return new Choice(GameMode.TWO_PLAYER, Side.RED);
        }

        String[] sides = {"RED (move first)", "BLACK"};
        int sideIdx = JOptionPane.showOptionDialog(null,
                "Which side do you want to play?",
                "Player vs Computer", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, sides, sides[0]);
        if (sideIdx == JOptionPane.CLOSED_OPTION) {
            return null;
        }
        Side human = sideIdx == 1 ? Side.BLACK : Side.RED;
        return new Choice(GameMode.VS_COMPUTER, human);
    }
}
