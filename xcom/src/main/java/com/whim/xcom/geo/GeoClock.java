package com.whim.xcom.geo;

/**
 * The Geoscape clock: real-time-with-pause game time measured in seconds, with
 * the six 1994 time-compression speeds (plus pause). Each engine tick advances
 * game time by the current speed's step.
 */
public final class GeoClock {

    /** The 1994 time controls; {@code stepSeconds} is game-time advanced per tick. */
    public enum Speed {
        PAUSE("Pause", 0),
        SEC5("5 Secs", 5),
        MIN1("1 Min", 60),
        MIN5("5 Mins", 300),
        MIN30("30 Mins", 1800),
        HOUR1("1 Hour", 3600),
        DAY1("1 Day", 86400);

        private final String label;
        private final int stepSeconds;

        Speed(String label, int stepSeconds) {
            this.label = label;
            this.stepSeconds = stepSeconds;
        }

        public String label() { return label; }
        public int stepSeconds() { return stepSeconds; }
    }

    private long seconds; // total elapsed game seconds since campaign start
    private Speed speed = Speed.PAUSE;

    public long seconds() { return seconds; }
    public Speed speed() { return speed; }
    public void setSpeed(Speed speed) { this.speed = speed; }

    /** Advance by the current speed; returns the seconds actually added. */
    public int tick() {
        int step = speed.stepSeconds();
        seconds += step;
        return step;
    }

    public int day() { return (int) (seconds / 86400) + 1; }
    public int month() { return (int) (seconds / (30L * 86400)); } // 0-based, 30-day months
    public int hourOfDay() { return (int) ((seconds % 86400) / 3600); }
    public int minuteOfHour() { return (int) ((seconds % 3600) / 60); }

    /** e.g. "Day 3  08:30". */
    public String display() {
        return String.format("Day %d   %02d:%02d", day(), hourOfDay(), minuteOfHour());
    }
}
