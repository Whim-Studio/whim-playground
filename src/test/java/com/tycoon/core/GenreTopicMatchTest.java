package com.tycoon.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Verifies the authentic genre/topic fit table and the chronological tech tree. */
public class GenreTopicMatchTest {

    @Test
    public void classicPairingsAreStrong() {
        assertEquals(GenreTopicMatch.Rating.PERFECT,
                GenreTopicMatch.rate(Genre.RPG, Topic.FANTASY));
        assertEquals(GenreTopicMatch.Rating.PERFECT,
                GenreTopicMatch.rate(Genre.SPORT, Topic.SOCCER));
        assertEquals(GenreTopicMatch.Rating.PERFECT,
                GenreTopicMatch.rate(Genre.RACING, Topic.CARS));
    }

    @Test
    public void mismatchesAreWeak() {
        assertTrue(GenreTopicMatch.multiplier(Genre.SPORT, Topic.FANTASY) < 1.0);
        assertTrue(GenreTopicMatch.multiplier(Genre.RPG, Topic.SOCCER) < 1.0);
    }

    @Test
    public void unknownAndNullPairingsAreNeutral() {
        assertEquals(GenreTopicMatch.Rating.OK, GenreTopicMatch.rate(null, Topic.FANTASY));
        assertEquals(GenreTopicMatch.Rating.OK, GenreTopicMatch.rate(Genre.RPG, null));
        // A pairing not in the table defaults to neutral 1.0x.
        assertEquals(1.0, GenreTopicMatch.multiplier(Genre.PUZZLE, Topic.PIRATES), 0.0001);
    }

    @Test
    public void techTreeUnlocksChronologically() {
        // At week 0 only the most primitive engine exists.
        assertEquals(Technology.TEXT_PARSER, Technology.bestAtWeek(0));
        assertEquals(1, Technology.unlockedAtWeek(0).size());

        // Engines accumulate over time and never regress.
        int earlyCount = Technology.unlockedAtWeek(60).size();
        int lateCount = Technology.unlockedAtWeek(400).size();
        assertTrue(lateCount > earlyCount);
        assertEquals(Technology.values().length, lateCount);

        // More advanced engines carry a higher quality bonus.
        assertTrue(Technology.RAY_TRACING.qualityBonus() > Technology.TEXT_PARSER.qualityBonus());
    }
}
