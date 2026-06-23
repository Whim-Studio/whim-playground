package com.tiwas.mahjong.ui;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.tiwas.mahjong.engine.HandResult;
import com.tiwas.mahjong.model.GameState;
import com.tiwas.mahjong.model.ScoreSheet;

/** Builds the end-of-hand scoring breakdown and the final standings dialogs. */
public final class DialogUtils {

    private DialogUtils() {
    }

    public static String scoreBreakdown(GameState state, HandResult result) {
        StringBuilder sb = new StringBuilder();
        if (result.drawnGame) {
            sb.append("Drawn game — the wall is exhausted.\nNo points are scored.\n");
            return sb.toString();
        }
        if (result.falseMahjong) {
            sb.append(result.message).append("\n\n");
            appendDeltas(sb, state, result);
            return sb.toString();
        }
        ScoreSheet sheet = result.sheet;
        sb.append(state.getPlayer(result.winner).getName())
                .append(" wins — ").append(sheet.getTitle()).append("\n");
        sb.append("--------------------------------------\n");
        sb.append("Base points:\n");
        for (int i = 0; i < sheet.getBaseLines().size(); i++) {
            ScoreSheet.Line l = sheet.getBaseLines().get(i);
            sb.append("  ").append(l.label).append("  ").append(l.detail).append("\n");
        }
        sb.append("  = base total ").append(sheet.getBasePoints()).append("\n");
        if (!sheet.getDoubleLines().isEmpty()) {
            sb.append("Doubles:\n");
            for (int i = 0; i < sheet.getDoubleLines().size(); i++) {
                ScoreSheet.Line l = sheet.getDoubleLines().get(i);
                sb.append("  ").append(l.label).append("  ").append(l.detail).append("\n");
            }
            sb.append("  = ").append(sheet.getTotalDoubles()).append(" doublings (x")
                    .append(1 << Math.min(sheet.getTotalDoubles(), 30)).append(")\n");
        }
        sb.append("--------------------------------------\n");
        sb.append("Raw score: ").append(sheet.getRawScore());
        if (sheet.isLimited()) {
            sb.append("  (capped at the limit ").append(state.getLimit()).append(")");
        }
        sb.append("\nFinal score: ").append(sheet.getFinalScore()).append("\n\n");
        appendDeltas(sb, state, result);
        return sb.toString();
    }

    private static void appendDeltas(StringBuilder sb, GameState state, HandResult result) {
        sb.append("Score changes this hand:\n");
        for (int i = 0; i < state.getPlayers().size(); i++) {
            int d = result.deltas[i];
            sb.append("  ").append(state.getPlayer(i).getName()).append(": ")
                    .append(d >= 0 ? "+" : "").append(d)
                    .append("   (total ").append(state.getPlayer(i).getScore()).append(")\n");
        }
    }

    public static void showHandResult(Component parent, GameState state, HandResult result) {
        JOptionPane.showMessageDialog(parent, scoreBreakdown(state, result),
                "Hand " + state.getHandNumber() + " result", JOptionPane.INFORMATION_MESSAGE);
    }

    public static String finalStandings(GameState state) {
        StringBuilder sb = new StringBuilder("Game over — final standings:\n\n");
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < state.getPlayers().size(); i++) {
            if (state.getPlayer(i).getScore() > bestScore) {
                bestScore = state.getPlayer(i).getScore();
                best = i;
            }
        }
        for (int i = 0; i < state.getPlayers().size(); i++) {
            sb.append(i == best ? "🏆 " : "   ");
            sb.append(state.getPlayer(i).getName()).append(": ")
                    .append(state.getPlayer(i).getScore()).append("\n");
        }
        sb.append("\nWinner: ").append(state.getPlayer(best).getName()).append("!");
        return sb.toString();
    }
}
