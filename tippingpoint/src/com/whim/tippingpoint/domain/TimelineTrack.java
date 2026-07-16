package com.whim.tippingpoint.domain;

public final class TimelineTrack {
    private int year;

    public TimelineTrack() { this.year = Rules.START_YEAR; }

    public int getYear() { return year; }
    public void advance() { year += Rules.YEARS_PER_ROUND; }
    public boolean isAtEnd() { return year >= Rules.END_YEAR; }
}
