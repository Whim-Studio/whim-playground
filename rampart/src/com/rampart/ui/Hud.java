package com.rampart.ui;

import com.rampart.model.GameStateView;
import com.rampart.model.Phase;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Paints the heads-up display strip: score, round, territory %, phase name, and a
 * countdown timer, read straight from the {@link GameStateView} snapshot. Pure
 * presentation — it computes nothing about the game, it only formats view fields.
 */
public final class Hud {

    /** Height in pixels of the HUD strip drawn below the playfield. */
    public static final int HEIGHT = 72;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.BOLD, 16);

    /**
     * Draws the HUD strip occupying {@code [0,y0]..[width,y0+HEIGHT]}.
     *
     * @param g     the target graphics
     * @param s     the current snapshot
     * @param y0    the top y of the HUD strip (below the grid)
     * @param width the strip width in pixels
     */
    public void draw(Graphics2D g, GameStateView s, int y0, int width) {
        g.setColor(Palette.HUD_BG);
        g.fillRect(0, y0, width, HEIGHT);
        g.setColor(Palette.HUD_LABEL);
        g.drawLine(0, y0, width, y0);

        int pad = 14;
        int col1 = pad;
        int col2 = pad + 150;
        int col3 = pad + 300;
        int col4 = pad + 470;

        drawStat(g, col1, y0, "SCORE", String.valueOf(s.score()));
        drawStat(g, col2, y0, "ROUND", String.valueOf(s.round()));
        int pct = (int) Math.round(s.territoryFraction() * 100.0);
        drawStat(g, col3, y0, "TERRITORY", pct + "%");
        drawStat(g, col4, y0, "LIVES", String.valueOf(s.lives()));

        // Phase name + countdown on the right.
        String phase = phaseLabel(s.phase());
        double secs = s.timerRemainingMillis() / 1000.0;
        g.setFont(TITLE_FONT);
        g.setColor(Palette.HUD_TEXT);
        String phaseStr = phase;
        int px = width - pad - g.getFontMetrics().stringWidth(phaseStr);
        g.drawString(phaseStr, px, y0 + 26);

        g.setFont(VALUE_FONT);
        boolean urgent = secs <= 5.0 && s.timerRemainingMillis() > 0
                && (s.phase() == Phase.BUILD || s.phase() == Phase.BATTLE || s.phase() == Phase.REPAIR);
        g.setColor(urgent ? Palette.HUD_WARN : Palette.HUD_LABEL);
        String tstr = String.format("%04.1fs", secs);
        int tx = width - pad - g.getFontMetrics().stringWidth(tstr);
        g.drawString(tstr, tx, y0 + 50);

        // Context line: cannons remaining / current piece.
        g.setFont(LABEL_FONT);
        g.setColor(Palette.HUD_TEXT);
        String ctx = contextLine(s);
        if (ctx != null) {
            g.drawString(ctx, pad, y0 + HEIGHT - 10);
        }
    }

    private void drawStat(Graphics2D g, int x, int y0, String label, String value) {
        g.setFont(LABEL_FONT);
        g.setColor(Palette.HUD_LABEL);
        g.drawString(label, x, y0 + 22);
        g.setFont(VALUE_FONT);
        g.setColor(Palette.HUD_TEXT);
        g.drawString(value, x, y0 + 44);
    }

    private String contextLine(GameStateView s) {
        switch (s.phase()) {
            case BUILD:
                return "BUILD — click enclosed land to place a cannon ("
                        + s.cannonsRemainingToPlace() + " left).  SPACE = ready";
            case BATTLE:
                return "BATTLE — click a target to fire a ready cannon.  SPACE = ready";
            case REPAIR:
                String shape = s.currentPiece() != null ? s.currentPiece().shape().name() : "-";
                return "REPAIR — left-click drops piece [" + shape
                        + "], right-click / R rotates.  SPACE = ready";
            default:
                return null;
        }
    }

    private static String phaseLabel(Phase p) {
        if (p == null) return "";
        switch (p) {
            case TITLE:            return "TITLE";
            case BUILD:            return "CANNON PLACEMENT";
            case BATTLE:           return "BATTLE";
            case REPAIR:           return "REPAIR";
            case ROUND_TRANSITION: return "ROUND CLEAR";
            case GAME_OVER:        return "GAME OVER";
            default:               return p.name();
        }
    }
}
