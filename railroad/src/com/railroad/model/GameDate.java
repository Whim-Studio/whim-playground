package com.railroad.model;

/**
 * A minimal in-game calendar tracking whole days elapsed since a start year.
 * Uses a flat 30-day month / 360-day year — plenty for a Phase 1 clock display
 * and deterministic without pulling in java.time date math.
 */
public final class GameDate {

    private static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final int DAYS_PER_MONTH = 30;
    private static final int DAYS_PER_YEAR = DAYS_PER_MONTH * 12;

    private final int startYear;
    private double days; // fractional days elapsed since start

    public GameDate(int startYear) {
        this.startYear = startYear;
        this.days = 0;
    }

    public void advance(double dDays) {
        days += dDays;
    }

    public int getYear() {
        return startYear + (int) (days / DAYS_PER_YEAR);
    }

    public double getElapsedDays() {
        return days;
    }

    /** e.g. "12 Mar 1834". */
    public String format() {
        int whole = (int) days;
        int year = startYear + whole / DAYS_PER_YEAR;
        int dayOfYear = whole % DAYS_PER_YEAR;
        int month = dayOfYear / DAYS_PER_MONTH;         // 0..11
        int dayOfMonth = dayOfYear % DAYS_PER_MONTH + 1; // 1..30
        return dayOfMonth + " " + MONTHS[month] + " " + year;
    }
}
