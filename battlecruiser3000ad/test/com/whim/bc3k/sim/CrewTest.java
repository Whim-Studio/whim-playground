package com.whim.bc3k.sim;

import com.whim.bc3k.sim.crew.CrewMember;
import com.whim.bc3k.sim.crew.CrewRoster;
import com.whim.bc3k.sim.crew.ShipLocation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CrewTest {

    @Test public void hireAddsLivingCrew() {
        CrewRoster r = new CrewRoster();
        r.hire("Reyes");
        r.hire("Okonkwo");
        assertEquals(2, r.aliveCount());
    }

    @Test public void crewWalksToDestinationOverTime() {
        CrewMember m = new CrewMember(1, "Reyes", ShipLocation.BRIDGE);
        m.orderTo(ShipLocation.ENGINEERING);
        assertEquals(ShipLocation.BRIDGE, m.location());
        for (int i = 0; i < 30; i++) m.tick(0.1);   // > 2s of travel
        assertEquals(ShipLocation.ENGINEERING, m.location());
        assertNull(m.destination());
    }

    @Test public void crewStarvesToDeathWhenHungerMaxed() {
        CrewMember m = new CrewMember(1, "Reyes", ShipLocation.BRIDGE);
        // Run a long time away from the galley: hunger maxes, then health drains to 0.
        for (int i = 0; i < 1000; i++) m.tick(0.5);
        assertFalse(m.alive());
    }

    @Test public void eatingInGalleyReducesHunger() {
        CrewMember m = new CrewMember(1, "Reyes", ShipLocation.BRIDGE);
        for (int i = 0; i < 40; i++) m.tick(0.5);   // build hunger
        int hungry = m.hunger();
        m.orderTo(ShipLocation.GALLEY);
        for (int i = 0; i < 40; i++) m.tick(0.5);   // walk there + eat
        assertTrue(m.hunger() < hungry);
    }

    @Test public void deathBanksDnaAndCloningRestoresHeadcount() {
        CrewRoster r = new CrewRoster();
        r.hire("Reyes");
        for (int i = 0; i < 1000; i++) r.tick(0.5);   // starve the lone crew member
        assertEquals(0, r.aliveCount());
        assertTrue(r.dnaStored() >= 1);
        CrewMember clone = r.cloneFromDna();
        assertNotNull(clone);
        assertEquals(1, r.aliveCount());
    }

    @Test public void assignBestFitPicksHighestSkillAndOrdersMovement() {
        CrewRoster r = new CrewRoster();
        CrewMember a = r.hire("Gunner A");
        CrewMember b = r.hire("Gunner B");
        a.setSkill(CrewMember.Skill.GUNNERY, 30);
        b.setSkill(CrewMember.Skill.GUNNERY, 90);
        CrewMember chosen = r.assignBestFit(CrewMember.Skill.GUNNERY, ShipLocation.TACTICAL);
        assertEquals(b.id(), chosen.id());
        assertEquals(ShipLocation.TACTICAL, chosen.destination());
    }
}
