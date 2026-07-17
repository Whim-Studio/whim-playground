package com.whim.xcom.meta;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;

/**
 * Phase 8 — the UFOpaedia catalog: basic gear is always readable and alien/tech
 * dossiers unlock progressively as research completes.
 */
public class UfopaediaTest {

    private final Ruleset rs = Ruleset1994.load();

    private Campaign freshCampaign() {
        return new Campaign(10, 10, new SoldierRoster());
    }

    private Ufopaedia.Entry entry(Campaign c, String id) {
        for (Ufopaedia.Entry e : Ufopaedia.entries(rs, c)) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    @Test
    public void basicGearAndSurveyIsAlwaysUnlocked() {
        Campaign c = freshCampaign();
        assertTrue("rifle readable from the start", entry(c, "rifle").unlocked());
        assertTrue("stun rod readable from the start", entry(c, "stun_rod").unlocked());
        assertTrue("UFO survey readable", entry(c, "small_scout").unlocked());
        assertTrue("own facilities readable", entry(c, "laboratory").unlocked());
    }

    @Test
    public void advancedTechIsLockedUntilResearched() {
        Campaign c = freshCampaign();
        assertFalse("laser locked before research", entry(c, "laser_rifle").unlocked());
        c.completedResearch().add("laser_weapons");
        assertTrue("laser unlocked after research", entry(c, "laser_rifle").unlocked());
    }

    @Test
    public void alienDossiersUnlockWithInterrogationAndOrigins() {
        Campaign c = freshCampaign();
        assertFalse("sectoid locked initially", entry(c, "sectoid_soldier").unlocked());
        c.completedResearch().add("interrogate_sectoid_soldier");
        assertTrue("sectoid dossier after interrogation", entry(c, "sectoid_soldier").unlocked());
        // General races open once alien intelligence is understood.
        assertFalse("muton locked before origins", entry(c, "muton_soldier").unlocked());
        c.completedResearch().add("alien_origins");
        assertTrue("muton dossier after Alien Origins", entry(c, "muton_soldier").unlocked());
    }

    @Test
    public void unlockedListGrowsWithProgress() {
        Campaign c = freshCampaign();
        List<Ufopaedia.Entry> before = Ufopaedia.unlockedEntries(rs, c);
        assertNotNull(before);
        int baseCount = before.size();
        assertTrue("some entries always readable", baseCount > 0);
        c.completedResearch().add("laser_weapons");
        c.completedResearch().add("alien_origins");
        int afterCount = Ufopaedia.unlockedEntries(rs, c).size();
        assertTrue("catalog grows as research completes", afterCount > baseCount);
    }
}
