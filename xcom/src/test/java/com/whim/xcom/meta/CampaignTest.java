package com.whim.xcom.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.Ruleset1994;
import com.whim.xcom.rules.def.ManufactureNode;
import com.whim.xcom.rules.def.ResearchNode;

/**
 * Tests the meta layer headlessly: research accrues scientist-days and completes,
 * manufacturing accrues engineer-hours and yields items, soldier progression on a
 * survived mission, and a full save → JSON → load round-trip.
 */
public class CampaignTest {

    private final Ruleset rs = Ruleset1994.load();

    private Campaign fresh() {
        SoldierRoster roster = new SoldierRoster();
        roster.add(new Soldier("A", 50, 30, 55, 45, 30));
        roster.add(new Soldier("B", 52, 32, 60, 50, 28));
        return new Campaign(10, 10, roster);
    }

    @Test
    public void researchCompletesAfterEnoughScientistDays() {
        Campaign c = fresh();
        ResearchNode node = rs.research("sectoid_autopsy"); // 60 scientist-days
        assertTrue(node != null);
        c.startResearch(node, 10); // 10 scientists → 6 days
        List<String> ev = c.advance(6 * 86400); // 6 game-days
        assertTrue(c.completedResearch().contains("sectoid_autopsy"));
        assertTrue(ev.toString().contains("Research complete"));
    }

    @Test
    public void manufacturingProducesItems() {
        Campaign c = fresh();
        c.completedResearch().add("laser_weapons"); // unlock
        ManufactureNode node = rs.manufacture("make_laser_rifle"); // 350 engineer-hours
        assertTrue(c.researchUnlocksManufacture(node));
        c.startManufacture(node, 10, 1); // 10 engineers → 35 hours
        c.advance(36 * 3600); // 36 game-hours
        Integer built = c.stores().get(node.outputItemId());
        assertTrue("one laser rifle should be built", built != null && built >= 1);
    }

    @Test
    public void soldierGainsExperienceAndRank() {
        Soldier s = new Soldier("Rookie", 50, 30, 50, 40, 30);
        int accBefore = s.firingAccuracy();
        assertEquals("Rookie", s.rankName());
        s.onMissionSurvived(true);
        s.onMissionSurvived(true);
        assertTrue(s.firingAccuracy() > accBefore);
        assertTrue(s.missions() >= 2);
        assertFalse("Rookie".equals(s.rankName())); // promoted
    }

    @Test
    public void saveRoundTripPreservesState() {
        Campaign c = fresh();
        c.completedResearch().add("alien_alloys");
        c.startResearch(rs.research("laser_weapons"), 8);
        c.advance(2 * 86400); // partial progress
        c.startManufacture(rs.manufacture("make_laser_rifle"), 5, 3);
        c.roster().byName("A").onMissionSurvived(true);

        SaveGame.Snapshot snap = SaveGame.capture(c, 750_000L, 120, 999_999L);
        String json = SaveGame.toJson(snap);
        SaveGame.Snapshot back = SaveGame.fromJson(json);
        Campaign restored = SaveGame.restoreCampaign(back, rs);

        assertEquals(750_000L, back.funds);
        assertEquals(120, back.score);
        assertTrue(restored.completedResearch().contains("alien_alloys"));
        assertEquals(c.activeResearch().size(), restored.activeResearch().size());
        assertEquals(c.manufacturing().size(), restored.manufacturing().size());
        assertEquals(c.roster().size(), restored.roster().size());
        assertEquals(1, restored.roster().byName("A").missions());
        // progress within the same tolerance (double round-trip)
        assertEquals(c.activeResearch().get(0).percent(),
                restored.activeResearch().get(0).percent());
    }

    @Test
    public void manufacturedGearBecomesEquipableAndLoadoutPersists() {
        Campaign c = fresh();
        // Basic issue is always available; laser rifle / personal armour are not yet.
        assertTrue(c.equipableWeapons(rs).contains("rifle"));
        assertFalse(c.equipableWeapons(rs).contains("laser_rifle"));
        assertFalse(c.equipableArmors(rs).contains("personal_armor"));

        // Build them into stores → they become equipable.
        c.addToStores("laser_rifle", 1);
        c.addToStores("personal_armor", 1);
        assertTrue(c.equipableWeapons(rs).contains("laser_rifle"));
        assertTrue(c.equipableArmors(rs).contains("personal_armor"));

        // Equip a soldier and round-trip through a save.
        c.roster().byName("A").equip("laser_rifle", "personal_armor");
        SaveGame.Snapshot snap = SaveGame.capture(c, 0L, 0, 0L);
        Campaign restored = SaveGame.restoreCampaign(SaveGame.fromJson(SaveGame.toJson(snap)), rs);
        Soldier a = restored.roster().byName("A");
        assertEquals("laser_rifle", a.weaponId());
        assertEquals("personal_armor", a.armorId());
    }
}
