package com.whim.xcom.geo;

/**
 * A Council funding nation. Its monthly funding rises or falls with X-COM's
 * performance (activity score) in its region; sustained failure can cause it to
 * withdraw funding entirely.
 */
public final class FundingNation {

    private final String name;
    private int monthlyFunding;   // dollars
    private int scoreThisMonth;   // net X-COM vs alien activity points
    private boolean withdrawn;

    public FundingNation(String name, int monthlyFunding) {
        this.name = name;
        this.monthlyFunding = monthlyFunding;
    }

    public String name() { return name; }
    public int monthlyFunding() { return monthlyFunding; }
    public int scoreThisMonth() { return scoreThisMonth; }
    public boolean withdrawn() { return withdrawn; }

    public void addScore(int points) { scoreThisMonth += points; }
    public void resetMonthlyScore() { scoreThisMonth = 0; }

    /**
     * Apply the end-of-month review: good scores raise funding (up to +40%), poor
     * scores cut it; a very bad month can trigger withdrawal.
     */
    public int applyMonthlyReview() {
        if (withdrawn) {
            return 0;
        }
        int delta;
        if (scoreThisMonth <= -200) {
            withdrawn = true;
            monthlyFunding = 0;
            return 0;
        } else if (scoreThisMonth > 0) {
            delta = (int) Math.round(monthlyFunding * Math.min(0.40, scoreThisMonth / 500.0));
        } else {
            delta = (int) Math.round(monthlyFunding * Math.max(-0.30, scoreThisMonth / 500.0));
        }
        monthlyFunding = Math.max(0, monthlyFunding + delta);
        return monthlyFunding;
    }
}
