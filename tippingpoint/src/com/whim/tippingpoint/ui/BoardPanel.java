package com.whim.tippingpoint.ui;

import com.whim.tippingpoint.domain.CitizenCard;
import com.whim.tippingpoint.domain.CitizenType;
import com.whim.tippingpoint.domain.DevelopmentCard;
import com.whim.tippingpoint.domain.DevelopmentType;
import com.whim.tippingpoint.domain.GameMode;
import com.whim.tippingpoint.domain.GameState;
import com.whim.tippingpoint.domain.Player;
import com.whim.tippingpoint.domain.RiskFactor;
import com.whim.tippingpoint.domain.Rules;
import com.whim.tippingpoint.domain.StatusBoard;
import com.whim.tippingpoint.domain.WeatherCard;
import com.whim.tippingpoint.domain.WeatherSeverity;
import com.whim.tippingpoint.engine.GameEngine;
import com.whim.tippingpoint.engine.WeatherReport;
import com.whim.tippingpoint.engine.WinStatus;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The whole playing surface, drawn procedurally with {@link Graphics2D}.
 *
 * <p>Layout: Timeline across the top, 3×4 Central Market in the middle, the active
 * player's City Tableau + Status Board along the bottom, and a compact opponents
 * strip on the right. All mutation flows through the {@link GameEngine}; the panel
 * only reads {@code domain} objects and repaints after every engine call.
 */
final class BoardPanel extends JPanel {

    // ----- palette -----
    private static final Color BG          = new Color(0x141b24);
    private static final Color PANEL       = new Color(0x1e2733);
    private static final Color PANEL_LIGHT = new Color(0x27333f);
    private static final Color INK         = new Color(0xe6edf3);
    private static final Color INK_DIM     = new Color(0x9aa7b4);
    private static final Color ACCENT      = new Color(0x8fe388);
    private static final Color CASH_COL    = new Color(0x59c17a);
    private static final Color FOOD_COL    = new Color(0xe0b64a);
    private static final Color CO2_COL     = new Color(0x8a94a0);
    private static final Color WOOD        = new Color(0x9c6b3f);
    private static final Color WOOD_DARK   = new Color(0x6f4a2a);

    private final GameEngine engine;

    /** Clickable regions, rebuilt every paint. */
    private final List<Hotspot> hotspots = new ArrayList<Hotspot>();

    /** Rolling on-screen log (most recent last). */
    private final List<String> log = new ArrayList<String>();

    /** When non-empty, an overlay is shown (weather results or game over). */
    private List<String> overlay = new ArrayList<String>();
    private boolean overlayGameOver = false;

    private Timer aiTimer;

    BoardPanel(GameEngine engine) {
        this.engine = engine;
        setBackground(BG);
        setPreferredSize(new Dimension(1280, 840));
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    // ================================================================= lifecycle

    /** Called once after the frame is shown: grant first sub-turn and kick off AI. */
    void startGame() {
        engine.beginDevelopmentPhase();
        pushLog("Round " + state().getRound() + " — Development Phase begins.");
        repaint();
        maybeScheduleAi();
    }

    private GameState state() {
        return engine.state();
    }

    // ================================================================= interaction

    private void handleClick(int x, int y) {
        // Iterate in reverse so overlay hotspots (added last) win.
        for (int i = hotspots.size() - 1; i >= 0; i--) {
            Hotspot h = hotspots.get(i);
            if (h.bounds.contains(x, y)) {
                h.action.run();
                return;
            }
        }
    }

    private void endTurn() {
        Player before = state().getCurrentPlayer();
        engine.endDevelopmentTurn();
        pushLog(before.getName() + " ended their turn.");
        afterSubTurnAdvanced();
    }

    /**
     * Shared post-advance step: the current sub-turn is over and the engine has
     * advanced the active player (either via {@link GameEngine#endDevelopmentTurn}
     * for humans, or internally inside {@link GameEngine#runAiDevelopmentTurn} for
     * AI). Start the next player's sub-turn or flip to the Weather control.
     */
    private void afterSubTurnAdvanced() {
        if (engine.developmentPhaseComplete()) {
            pushLog("Development Phase complete — run the Weather Phase.");
        } else {
            engine.beginDevelopmentPhase();
        }
        repaint();
        maybeScheduleAi();
    }

    private void buy(int r, int c) {
        Player p = state().getCurrentPlayer();
        if (engine.canBuyDevelopment(p, r, c)) {
            DevelopmentCard card = state().getMarket().get(r, c);
            engine.buyDevelopment(p, r, c);
            if (card != null) {
                pushLog(p.getName() + " bought " + card.getName() + " ($" + card.getCost() + ").");
            }
            repaint();
        }
    }

    private void recruit(CitizenType type) {
        Player p = state().getCurrentPlayer();
        if (engine.canRecruit(p, type)) {
            engine.recruit(p, type);
            pushLog(p.getName() + " recruited a " + type + ".");
            repaint();
        }
    }

    private void runWeather() {
        WeatherReport report = engine.resolveWeatherPhase();
        WinStatus status = engine.checkStatus();

        overlay = new ArrayList<String>();
        overlay.add("WEATHER PHASE — Global CO₂: " + report.getGlobalCo2());
        List<WeatherCard> revealed = report.getRevealed();
        if (revealed == null || revealed.isEmpty()) {
            overlay.add("No extreme weather this round.");
        } else {
            StringBuilder sb = new StringBuilder("Revealed: ");
            for (int i = 0; i < revealed.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(revealed.get(i).getName());
            }
            overlay.add(sb.toString());
        }
        overlay.add("");
        List<String> lines = report.getLines();
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                overlay.add(lines.get(i));
                pushLog(lines.get(i));
            }
        }
        overlay.add("");
        overlay.add(status.getMessage());
        overlayGameOver = status.isOver();
        pushLog(status.getMessage());
        repaint();
    }

    private void dismissOverlay() {
        boolean over = overlayGameOver;
        overlay = new ArrayList<String>();
        if (!over) {
            // Start the next round's Development Phase through the engine.
            engine.beginDevelopmentPhase();
            pushLog("Round " + state().getRound() + " — Development Phase begins.");
            repaint();
            maybeScheduleAi();
        } else {
            repaint();
        }
    }

    /** If it's an AI player's development sub-turn, play it after a short beat. */
    private void maybeScheduleAi() {
        if (!overlay.isEmpty() || engine.developmentPhaseComplete()) {
            return;
        }
        Player cur = state().getCurrentPlayer();
        if (cur == null || !cur.isAi()) {
            return;
        }
        if (aiTimer != null && aiTimer.isRunning()) {
            return;
        }
        aiTimer = new Timer(650, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                aiTimer.stop();
                runAiTurn();
            }
        });
        aiTimer.setRepeats(false);
        aiTimer.start();
    }

    private void runAiTurn() {
        if (!overlay.isEmpty() || engine.developmentPhaseComplete()) {
            return;
        }
        Player p = state().getCurrentPlayer();
        if (p == null || !p.isAi()) {
            return;
        }
        // runAiDevelopmentTurn plays the FULL sub-turn AND ends it internally
        // (per WD-548). Do NOT call endDevelopmentTurn again — just advance.
        engine.runAiDevelopmentTurn(p);
        pushLog(p.getName() + " (AI) took its turn.");
        afterSubTurnAdvanced();
    }

    private void pushLog(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        log.add(line);
        while (log.size() > 40) {
            log.remove(0);
        }
    }

    // ================================================================= painting

    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        hotspots.clear();
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int rightW = 250;
        int rightX = w - rightW - 12;
        int marginX = 16;
        int topH = 78;

        drawTimeline(g, marginX, 10, w - 2 * marginX, topH);

        int contentTop = topH + 22;
        int marketRight = rightX - 16;
        int marketBottom = drawMarket(g, marginX, contentTop, marketRight - marginX);

        drawOpponents(g, rightX, contentTop, rightW, h - contentTop - 12);

        int controlsH = 56;
        int bottomTop = marketBottom + 14;
        int bottomBottom = h - controlsH - 12;
        drawActivePlayer(g, marginX, bottomTop, marketRight - marginX, bottomBottom - bottomTop);

        drawControls(g, marginX, h - controlsH - 6, w - 2 * marginX, controlsH);

        if (!overlay.isEmpty()) {
            drawOverlay(g, w, h);
        }
    }

    // ------------------------------------------------------------- timeline

    private void drawTimeline(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
        GameState st = state();

        g.setFont(font(Font.BOLD, 15));
        g.setColor(ACCENT);
        g.drawString("TIPPING POINT", x + 14, y + 24);

        g.setFont(font(Font.PLAIN, 12));
        g.setColor(INK_DIM);
        String meta = "Round " + st.getRound() + "   ·   " + st.getMode()
                + "   ·   Phase: " + st.getPhase();
        g.drawString(meta, x + 14, y + 44);

        // Global CO2 gauge with tipping-point reference.
        int gco2 = st.getGlobalCo2();
        String co2s = "GLOBAL CO₂  " + gco2 + " / " + Rules.TIPPING_POINT_CO2;
        g.setFont(font(Font.BOLD, 13));
        int co2w = g.getFontMetrics().stringWidth(co2s);
        g.setColor(gco2 >= Rules.TIPPING_POINT_CO2 - 5 ? new Color(0xe06c5a) : INK);
        g.drawString(co2s, x + w - co2w - 14, y + 24);

        // year track
        int trackY = y + h - 22;
        int trackX0 = x + 14;
        int trackX1 = x + w - 14;
        g.setStroke(new BasicStroke(3f));
        g.setColor(PANEL_LIGHT);
        g.drawLine(trackX0, trackY, trackX1, trackY);

        int startYear = Rules.START_YEAR;
        int endYear = Rules.END_YEAR;
        int span = Math.max(1, endYear - startYear);
        int curYear = st.getTimeline().getYear();
        g.setFont(font(Font.PLAIN, 10));
        for (int yr = startYear; yr <= endYear; yr += Rules.YEARS_PER_ROUND) {
            int px = trackX0 + (int) ((long) (yr - startYear) * (trackX1 - trackX0) / span);
            boolean passed = yr <= curYear;
            g.setColor(passed ? ACCENT : INK_DIM);
            g.fillOval(px - 4, trackY - 4, 8, 8);
            g.setColor(INK_DIM);
            g.drawString(String.valueOf(yr), px - 12, trackY - 8);
        }
        // current-year marker
        int cx = trackX0 + (int) ((long) (Math.min(curYear, endYear) - startYear) * (trackX1 - trackX0) / span);
        g.setColor(ACCENT);
        g.fillOval(cx - 6, trackY - 6, 12, 12);
    }

    // ------------------------------------------------------------- market

    /** Returns the bottom Y of the drawn market. */
    private int drawMarket(Graphics2D g, int x, int y, int w) {
        GameState st = state();
        int rows = st.getMarket().rows();
        int cols = st.getMarket().cols();

        g.setFont(font(Font.BOLD, 14));
        g.setColor(INK);
        g.drawString("CENTRAL MARKET", x, y - 6);

        int gap = 12;
        int cardW = (w - (cols - 1) * gap) / cols;
        int cardH = Math.min(120, cardW * 3 / 4);
        Player p = st.getCurrentPlayer();
        boolean humanTurn = p != null && !p.isAi() && overlay.isEmpty() && !engine.developmentPhaseComplete();

        int yy = y;
        for (int r = 0; r < rows; r++) {
            int xx = x;
            for (int c = 0; c < cols; c++) {
                DevelopmentCard card = st.getMarket().get(r, c);
                boolean affordable = card != null && p != null && engine.canBuyDevelopment(p, r, c);
                drawDevCard(g, xx, yy, cardW, cardH, card, affordable && humanTurn);
                if (humanTurn && affordable) {
                    final int fr = r;
                    final int fc = c;
                    addHotspot(new Rectangle(xx, yy, cardW, cardH), new Runnable() {
                        public void run() {
                            buy(fr, fc);
                        }
                    });
                }
                xx += cardW + gap;
            }
            yy += cardH + gap;
        }
        return yy - gap;
    }

    private void drawDevCard(Graphics2D g, int x, int y, int w, int h,
                             DevelopmentCard card, boolean buyable) {
        if (card == null) {
            g.setColor(PANEL);
            g.fillRoundRect(x, y, w, h, 12, 12);
            g.setColor(PANEL_LIGHT);
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x, y, w, h, 12, 12);
            g.setColor(INK_DIM);
            g.setFont(font(Font.PLAIN, 11));
            centerString(g, "empty", x, y + h / 2, w);
            return;
        }

        Color type = devColor(card.getType());
        g.setColor(new Color(0x22, 0x2b, 0x36));
        g.fillRoundRect(x, y, w, h, 12, 12);
        // colored header band
        g.setColor(type);
        g.fillRoundRect(x, y, w, 22, 12, 12);
        g.fillRect(x, y + 11, w, 11);
        g.setColor(buyable ? ACCENT : new Color(0x3a4650));
        g.setStroke(new BasicStroke(buyable ? 2.5f : 1f));
        g.drawRoundRect(x, y, w, h, 12, 12);

        g.setColor(new Color(0x10, 0x16, 0x1d));
        g.setFont(font(Font.BOLD, 11));
        String tname = card.getType().toString();
        g.drawString(tname, x + 8, y + 15);

        // cost badge
        String cost = "$" + card.getCost();
        g.setFont(font(Font.BOLD, 12));
        int cw = g.getFontMetrics().stringWidth(cost) + 12;
        g.setColor(new Color(0x10, 0x16, 0x1d));
        g.fillRoundRect(x + w - cw - 6, y + 4, cw, 16, 8, 8);
        g.setColor(FOOD_COL);
        g.drawString(cost, x + w - cw - 1, y + 16);

        // name
        g.setColor(INK);
        g.setFont(font(Font.BOLD, 12));
        drawWrapped(g, card.getName(), x + 8, y + 40, w - 16, 2);

        // deltas
        g.setFont(font(Font.PLAIN, 11));
        int ly = y + h - 26;
        drawDelta(g, x + 8, ly, "$", card.getCashFlowDelta(), CASH_COL);
        drawDelta(g, x + 8, ly + 14, "F", card.getFoodDelta(), FOOD_COL);
        drawDelta(g, x + w / 2, ly + 14, "CO₂", card.getCo2Delta(), CO2_COL);
    }

    private void drawDelta(Graphics2D g, int x, int y, String label, int v, Color c) {
        String s = label + " " + (v > 0 ? "+" + v : String.valueOf(v));
        g.setColor(v == 0 ? INK_DIM : c);
        g.drawString(s, x, y);
    }

    private Color devColor(DevelopmentType t) {
        if (t == DevelopmentType.GREEN) {
            return new Color(0x6cc24a);
        }
        if (t == DevelopmentType.INDUSTRIAL) {
            return new Color(0xd08a3e);
        }
        return new Color(0x5aa0d6); // INFRASTRUCTURE
    }

    // ------------------------------------------------------------- active player

    private void drawActivePlayer(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
        Player p = state().getCurrentPlayer();
        if (p == null) {
            return;
        }
        StatusBoard b = p.getBoard();

        g.setColor(INK);
        g.setFont(font(Font.BOLD, 15));
        String who = p.getName() + (p.isAi() ? "  (AI)" : "");
        g.drawString(who, x + 14, y + 24);

        g.setFont(font(Font.PLAIN, 12));
        g.setColor(INK_DIM);
        g.drawString("Cash $" + p.getCash()
                + "    ·    Population " + p.getPopulation() + " / " + Rules.TARGET_POPULATION
                + "    ·    Workers " + p.getCity().workerCount()
                + "    ·    Farmers " + p.getCity().farmerCount(), x + 14, y + 44);

        // Status board: three wooden cylinders + risk.
        int cylTop = y + 58;
        int cylH = Math.max(70, h - 150);
        int cylW = 54;
        int cx = x + 24;
        drawCylinder(g, cx, cylTop, cylW, cylH, b.getCashFlow(), 12, "CASH FLOW", CASH_COL);
        drawCylinder(g, cx + 88, cylTop, cylW, cylH, b.getFoodProduction(), 12, "FOOD", FOOD_COL);
        drawCylinder(g, cx + 176, cylTop, cylW, cylH, b.getCo2(), Rules.TIPPING_POINT_CO2, "CO₂", CO2_COL);

        // risk factor badge
        RiskFactor rf = b.getRiskFactor();
        int rbx = cx + 250;
        int rby = cylTop + 6;
        Color rc = rf == RiskFactor.X1 ? CASH_COL : (rf == RiskFactor.X2 ? FOOD_COL : new Color(0xe06c5a));
        g.setColor(rc);
        g.fillRoundRect(rbx, rby, 84, 54, 12, 12);
        g.setColor(new Color(0x10, 0x16, 0x1d));
        g.setFont(font(Font.BOLD, 22));
        centerString(g, rf.toString(), rbx, rby + 34, 84);
        g.setFont(font(Font.PLAIN, 10));
        centerString(g, "RISK ×" + rf.multiplier(), rbx, rby + 48, 84);

        // City tableau: citizen chips + development chips
        int tabX = cx + 350;
        int tabW = x + w - tabX - 14;
        drawTableau(g, tabX, cylTop, tabW, cylH + 20, p);
    }

    private void drawTableau(Graphics2D g, int x, int y, int w, int h, Player p) {
        g.setColor(PANEL_LIGHT);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(INK_DIM);
        g.setFont(font(Font.BOLD, 11));
        g.drawString("CITY TABLEAU", x + 10, y + 16);

        List<CitizenCard> citizens = p.getCity().getCitizens();
        int chip = 16;
        int gap = 4;
        int cols = Math.max(1, (w - 20) / (chip + gap));
        int cxx = x + 10;
        int cyy = y + 26;
        int col = 0;
        for (int i = 0; i < citizens.size(); i++) {
            CitizenCard cc = citizens.get(i);
            boolean farmer = cc.isFarmer();
            g.setColor(farmer ? FOOD_COL : new Color(0x7fb3e0));
            if (farmer) {
                g.fillRoundRect(cxx, cyy, chip, chip, 4, 4);
            } else {
                g.fillOval(cxx, cyy, chip, chip);
            }
            col++;
            cxx += chip + gap;
            if (col >= cols) {
                col = 0;
                cxx = x + 10;
                cyy += chip + gap;
            }
        }

        // developments row (small colored ticks)
        List<DevelopmentCard> devs = p.getCity().getDevelopments();
        int dy = y + h - 40;
        g.setColor(INK_DIM);
        g.setFont(font(Font.PLAIN, 10));
        g.drawString("Developments (" + devs.size() + "):", x + 10, dy - 4);
        int dxx = x + 10;
        for (int i = 0; i < devs.size() && dxx < x + w - 14; i++) {
            g.setColor(devColor(devs.get(i).getType()));
            g.fillRoundRect(dxx, dy, 14, 20, 4, 4);
            dxx += 18;
        }

        // legend
        g.setFont(font(Font.PLAIN, 9));
        g.setColor(new Color(0x7fb3e0));
        g.fillOval(x + 10, y + h - 14, 9, 9);
        g.setColor(INK_DIM);
        g.drawString("worker", x + 22, y + h - 6);
        g.setColor(FOOD_COL);
        g.fillRoundRect(x + 74, y + h - 14, 9, 9, 3, 3);
        g.setColor(INK_DIM);
        g.drawString("farmer", x + 86, y + h - 6);
    }

    private void drawCylinder(Graphics2D g, int x, int y, int w, int h,
                              int value, int scaleMax, String label, Color accent) {
        // track
        g.setColor(PANEL_LIGHT);
        g.fillRoundRect(x - 4, y, w + 8, h, 8, 8);

        int max = Math.max(1, scaleMax);
        double frac = Math.min(1.0, (double) value / max);
        int cylH = 26;
        int travel = h - cylH - 24;
        int cyY = y + 4 + (int) ((1.0 - frac) * travel);

        // fill bar below cylinder to hint level
        g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        g.fillRoundRect(x + 6, cyY + cylH / 2, w - 12, (y + h - 24) - (cyY + cylH / 2), 6, 6);

        // wooden cylinder (top ellipse + body + bottom ellipse)
        int cxw = w;
        g.setColor(WOOD);
        g.fillRect(x, cyY + 6, cxw, cylH - 12);
        g.fillOval(x, cyY, cxw, 12);
        g.setColor(WOOD_DARK);
        g.fillOval(x, cyY + cylH - 12, cxw, 12);
        g.setColor(new Color(0xffffff));
        g.setFont(font(Font.BOLD, 13));
        centerString(g, String.valueOf(value), x, cyY + cylH / 2 + 5, cxw);

        // label
        g.setColor(INK_DIM);
        g.setFont(font(Font.BOLD, 10));
        centerString(g, label, x - 4, y + h - 8, w + 8);
    }

    // ------------------------------------------------------------- opponents

    private void drawOpponents(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
        g.setColor(INK);
        g.setFont(font(Font.BOLD, 13));
        g.drawString("OPPONENTS", x + 12, y + 22);

        List<Player> players = state().getPlayers();
        Player cur = state().getCurrentPlayer();
        int cardY = y + 34;
        int cardH = 84;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean active = p == cur;
            g.setColor(active ? new Color(0x2c3a2c) : PANEL_LIGHT);
            g.fillRoundRect(x + 8, cardY, w - 16, cardH - 8, 8, 8);
            if (active) {
                g.setColor(ACCENT);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(x + 8, cardY, w - 16, cardH - 8, 8, 8);
            }
            g.setColor(active ? ACCENT : INK);
            g.setFont(font(Font.BOLD, 12));
            g.drawString(p.getName() + (p.isAi() ? " (AI)" : ""), x + 16, cardY + 18);

            StatusBoard b = p.getBoard();
            g.setColor(INK_DIM);
            g.setFont(font(Font.PLAIN, 11));
            g.drawString("Pop " + p.getPopulation() + "/" + Rules.TARGET_POPULATION
                    + "   Cash $" + p.getCash(), x + 16, cardY + 36);
            g.drawString("Food " + b.getFoodProduction()
                    + "   CO₂ " + b.getCo2()
                    + "   " + b.getRiskFactor(), x + 16, cardY + 52);

            // mini bars
            drawMiniBar(g, x + 16, cardY + 60, w - 40, p.getPopulation(), Rules.TARGET_POPULATION, ACCENT);

            cardY += cardH;
            if (cardY + cardH > y + h) {
                break;
            }
        }
    }

    private void drawMiniBar(Graphics2D g, int x, int y, int w, int v, int max, Color c) {
        g.setColor(new Color(0x10, 0x16, 0x1d));
        g.fillRoundRect(x, y, w, 6, 3, 3);
        int fw = (int) (Math.min(1.0, (double) v / Math.max(1, max)) * w);
        g.setColor(c);
        g.fillRoundRect(x, y, fw, 6, 3, 3);
    }

    // ------------------------------------------------------------- controls + log

    private void drawControls(Graphics2D g, int x, int y, int w, int h) {
        panel(g, x, y, w, h);
        Player p = state().getCurrentPlayer();
        boolean humanTurn = p != null && !p.isAi() && overlay.isEmpty();
        boolean devComplete = engine.developmentPhaseComplete();

        int bx = x + 12;
        int by = y + 10;
        int bh = h - 20;

        if (overlay.isEmpty()) {
            if (devComplete) {
                bx = button(g, bx, by, 190, bh, "▶  Run Weather Phase", new Color(0x5aa0d6), true,
                        new Runnable() {
                            public void run() {
                                runWeather();
                            }
                        });
            } else if (humanTurn) {
                boolean canW = engine.canRecruit(p, CitizenType.WORKER);
                boolean canF = engine.canRecruit(p, CitizenType.FARMER);
                bx = button(g, bx, by, 150, bh, "Recruit Worker ($" + Rules.CITIZEN_COST + ")",
                        new Color(0x7fb3e0), canW, new Runnable() {
                            public void run() {
                                recruit(CitizenType.WORKER);
                            }
                        });
                bx = button(g, bx, by, 150, bh, "Recruit Farmer ($" + Rules.CITIZEN_COST + ")",
                        FOOD_COL, canF, new Runnable() {
                            public void run() {
                                recruit(CitizenType.FARMER);
                            }
                        });
                bx = button(g, bx, by, 120, bh, "End Turn", ACCENT, true, new Runnable() {
                    public void run() {
                        endTurn();
                    }
                });
            } else {
                // AI thinking indicator
                g.setColor(INK_DIM);
                g.setFont(font(Font.ITALIC, 13));
                g.drawString((p == null ? "" : p.getName() + " (AI) is taking its turn…"), bx, by + bh / 2 + 5);
            }
        }

        // rolling log on the right side of the control bar
        int logX = x + w - 460;
        if (logX < bx + 20) {
            logX = bx + 20;
        }
        g.setColor(INK_DIM);
        g.setFont(font(Font.PLAIN, 11));
        int ly = y + 16;
        int shown = 0;
        for (int i = log.size() - 1; i >= 0 && shown < 3; i--, shown++) {
            String s = log.get(i);
            g.setColor(shown == 0 ? INK : INK_DIM);
            g.drawString(trim(g, s, w - (logX - x) - 12), logX, ly);
            ly += 14;
        }
    }

    /** Draws a rounded button and (when enabled) registers a hotspot. Returns next x. */
    private int button(Graphics2D g, int x, int y, int w, int h, String label,
                       Color color, boolean enabled, Runnable action) {
        Color base = enabled ? color : new Color(0x39424c);
        g.setColor(base);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(enabled ? new Color(0x10, 0x16, 0x1d) : INK_DIM);
        g.setFont(font(Font.BOLD, 12));
        centerString(g, label, x, y + h / 2 + 5, w);
        if (enabled) {
            addHotspot(new Rectangle(x, y, w, h), action);
        }
        return x + w + 10;
    }

    // ------------------------------------------------------------- overlay

    private void drawOverlay(Graphics2D g, int w, int h) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, w, h);

        int ow = Math.min(720, w - 80);
        int oh = Math.min(480, h - 120);
        int ox = (w - ow) / 2;
        int oy = (h - oh) / 2;
        g.setColor(PANEL);
        g.fillRoundRect(ox, oy, ow, oh, 16, 16);
        g.setColor(overlayGameOver ? new Color(0xe0b64a) : new Color(0x5aa0d6));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(ox, oy, ow, oh, 16, 16);

        g.setColor(overlayGameOver ? new Color(0xe0b64a) : ACCENT);
        g.setFont(font(Font.BOLD, 20));
        centerString(g, overlayGameOver ? "GAME OVER" : "WEATHER REPORT", ox, oy + 34, ow);

        g.setFont(font(Font.PLAIN, 13));
        int ty = oy + 66;
        int lineH = 18;
        int maxLines = (oh - 120) / lineH;
        int start = Math.max(0, overlay.size() - maxLines);
        for (int i = start; i < overlay.size(); i++) {
            String s = overlay.get(i);
            g.setColor(INK);
            g.drawString(trim(g, s, ow - 48), ox + 24, ty);
            ty += lineH;
        }

        // dismiss / continue control
        int bw = 200;
        int bh = 40;
        int bx = ox + (ow - bw) / 2;
        int by = oy + oh - bh - 18;
        String label = overlayGameOver ? "Close" : "Continue to next round";
        g.setColor(overlayGameOver ? new Color(0xe06c5a) : ACCENT);
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(new Color(0x10, 0x16, 0x1d));
        g.setFont(font(Font.BOLD, 14));
        centerString(g, label, bx, by + bh / 2 + 5, bw);
        addHotspot(new Rectangle(bx, by, bw, bh), new Runnable() {
            public void run() {
                dismissOverlay();
            }
        });
    }

    // ================================================================= helpers

    private void addHotspot(Rectangle r, Runnable action) {
        hotspots.add(new Hotspot(r, action));
    }

    private void panel(Graphics2D g, int x, int y, int w, int h) {
        g.setPaint(new GradientPaint(x, y, PANEL, x, y + h, new Color(0x19212b)));
        g.fillRoundRect(x, y, w, h, 12, 12);
        g.setPaint(PANEL_LIGHT);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, w, h, 12, 12);
    }

    private Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    private void centerString(Graphics2D g, String s, int x, int baselineY, int w) {
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(s);
        g.drawString(s, x + (w - sw) / 2, baselineY);
    }

    private String trim(Graphics2D g, String s, int maxW) {
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) {
            return s;
        }
        String ell = "…";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(s.charAt(i));
            if (fm.stringWidth(sb.toString() + ell) > maxW) {
                sb.deleteCharAt(sb.length() - 1);
                sb.append(ell);
                return sb.toString();
            }
        }
        return s;
    }

    private void drawWrapped(Graphics2D g, String s, int x, int y, int maxW, int maxLines) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int lines = 0;
        int yy = y;
        for (int i = 0; i < words.length; i++) {
            String test = line.length() == 0 ? words[i] : line + " " + words[i];
            if (fm.stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, yy);
                lines++;
                yy += fm.getHeight() - 2;
                line = new StringBuilder(words[i]);
                if (lines >= maxLines - 1) {
                    // put remaining on last line, trimmed
                    StringBuilder rest = new StringBuilder(words[i]);
                    for (int j = i + 1; j < words.length; j++) {
                        rest.append(" ").append(words[j]);
                    }
                    g.drawString(trim(g, rest.toString(), maxW), x, yy);
                    return;
                }
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            g.drawString(trim(g, line.toString(), maxW), x, yy);
        }
    }

    // ================================================================= types

    private static final class Hotspot {
        final Rectangle bounds;
        final Runnable action;

        Hotspot(Rectangle bounds, Runnable action) {
            this.bounds = bounds;
            this.action = action;
        }
    }
}
