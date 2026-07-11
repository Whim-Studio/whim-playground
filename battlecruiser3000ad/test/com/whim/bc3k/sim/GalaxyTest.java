package com.whim.bc3k.sim;

import com.whim.bc3k.sim.galaxy.Galaxy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GalaxyTest {

    @Test public void startsAtGalcomHqVisited() {
        Galaxy g = Galaxy.defaultSector();
        assertEquals(0, g.currentId());
        assertTrue(g.current().visited());
        assertTrue(g.current().hasStation());
    }

    @Test public void canOnlyJumpToLinkedSystems() {
        Galaxy g = Galaxy.defaultSector();
        assertTrue(g.canJumpTo(1));    // Sol -> Centauri is linked
        assertFalse(g.canJumpTo(4));   // Sol -> Gammula Reach is not directly linked
        assertFalse(g.canJumpTo(0));   // cannot jump to self
    }

    @Test public void jumpMovesAndMarksVisited() {
        Galaxy g = Galaxy.defaultSector();
        assertTrue(g.jumpTo(1));
        assertEquals(1, g.currentId());
        assertTrue(g.byId(1).visited());
    }

    @Test public void illegalJumpIsRejectedAndPositionUnchanged() {
        Galaxy g = Galaxy.defaultSector();
        assertFalse(g.jumpTo(4));
        assertEquals(0, g.currentId());
    }

    @Test public void multiHopRouteToFrontierWorks() {
        Galaxy g = Galaxy.defaultSector();
        assertTrue(g.jumpTo(1));   // Sol -> Centauri
        assertTrue(g.jumpTo(3));   // Centauri -> Vega
        assertTrue(g.jumpTo(4));   // Vega -> Gammula Reach
        assertEquals(4, g.currentId());
    }

    @Test public void neighboursReflectCurrentSystem() {
        Galaxy g = Galaxy.defaultSector();
        assertEquals(2, g.neighbours().size());  // Sol links to Centauri + Sirius
    }
}
