package com.whim.necromunda.test;

/**
 * A zero-dependency assertion helper for the {@code main()} test harnesses, so
 * the whole project runs under plain {@code javac}/{@code java} with no JUnit jar.
 */
public final class Assert {

    private int checks;
    private int failures;

    public void that(String label, boolean condition) {
        checks++;
        if (condition) {
            System.out.println("  [PASS] " + label);
        } else {
            failures++;
            System.out.println("  [FAIL] " + label);
        }
    }

    public void equals(String label, Object expected, Object actual) {
        boolean ok = (expected == null) ? actual == null : expected.equals(actual);
        that(label + " (expected " + expected + ", got " + actual + ")", ok);
    }

    public void equalsInt(String label, int expected, int actual) {
        that(label + " (expected " + expected + ", got " + actual + ")", expected == actual);
    }

    public void section(String title) {
        System.out.println();
        System.out.println("== " + title + " ==");
    }

    public int checks() { return checks; }
    public int failures() { return failures; }

    /** Print the tally and exit non-zero if anything failed. */
    public void finish() {
        System.out.println();
        System.out.println("Ran " + checks + " checks, " + failures + " failure(s).");
        if (failures == 0) {
            System.out.println("ALL PASSED");
        } else {
            System.out.println("FAILURES PRESENT");
            System.exit(1);
        }
    }
}
