package com.tiwas.mahjong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A human-readable breakdown of how a winning (or penalised) hand was scored.
 * Built up by the scoring engine and rendered by the UI at hand end.
 */
public final class ScoreSheet {

    /** One line in the breakdown: a label and its contribution. */
    public static final class Line {
        public final String label;
        public final String detail;

        public Line(String label, String detail) {
            this.label = label;
            this.detail = detail;
        }
    }

    private final List<Line> baseLines = new ArrayList<Line>();
    private final List<Line> doubleLines = new ArrayList<Line>();
    private int basePoints;
    private int totalDoubles;
    private int rawScore;       // base * 2^doubles, before the limit cap
    private int finalScore;     // after limit cap and rounding
    private boolean limited;    // capped at the points limit
    private boolean limitHand;  // a special limit hand (13 Orphans, etc.)
    private String title = "";

    public void addBase(String label, String detail) {
        baseLines.add(new Line(label, detail));
    }

    public void addDouble(String label, String detail) {
        doubleLines.add(new Line(label, detail));
    }

    public List<Line> getBaseLines() {
        return baseLines;
    }

    public List<Line> getDoubleLines() {
        return doubleLines;
    }

    public int getBasePoints() {
        return basePoints;
    }

    public void setBasePoints(int basePoints) {
        this.basePoints = basePoints;
    }

    public int getTotalDoubles() {
        return totalDoubles;
    }

    public void setTotalDoubles(int totalDoubles) {
        this.totalDoubles = totalDoubles;
    }

    public int getRawScore() {
        return rawScore;
    }

    public void setRawScore(int rawScore) {
        this.rawScore = rawScore;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public boolean isLimited() {
        return limited;
    }

    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    public boolean isLimitHand() {
        return limitHand;
    }

    public void setLimitHand(boolean limitHand) {
        this.limitHand = limitHand;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
