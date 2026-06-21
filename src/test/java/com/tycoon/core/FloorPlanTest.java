package com.tycoon.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FloorPlanTest {

    @Test
    public void placeFacilityInsideRoom() {
        FloorPlan plan = new FloorPlan(40, 30);
        Room dev = new Room(RoomType.DEVELOPMENT, 0, 0, 5, 5);
        plan.addRoom(dev);

        Facility desk = Facility.at(FacilityType.DESK, GridPos.of(1, 1));
        assertTrue(plan.placeFacility(desk));
        assertNotNull(plan.facilityAt(GridPos.of(1, 1)));
        assertNull(plan.facilityAt(GridPos.of(2, 2)));
        assertNotNull(plan.roomAt(GridPos.of(1, 1)));
        assertNull(plan.roomAt(GridPos.of(20, 20)));
    }

    @Test
    public void rejectFacilityOutOfBoundsOrNoRoomOrOccupied() {
        FloorPlan plan = new FloorPlan(40, 30);
        Room dev = new Room(RoomType.DEVELOPMENT, 0, 0, 5, 5);
        plan.addRoom(dev);

        assertFalse("out of bounds", plan.placeFacility(Facility.at(FacilityType.DESK, GridPos.of(99, 99))));
        assertFalse("no room", plan.placeFacility(Facility.at(FacilityType.DESK, GridPos.of(10, 10))));

        assertTrue(plan.placeFacility(Facility.at(FacilityType.DESK, GridPos.of(1, 1))));
        assertFalse("occupied", plan.placeFacility(Facility.at(FacilityType.PLANT, GridPos.of(1, 1))));
    }

    @Test
    public void lockPreventsEdits() {
        FloorPlan plan = new FloorPlan(40, 30);
        Room dev = new Room(RoomType.DEVELOPMENT, 0, 0, 5, 5);
        plan.addRoom(dev);

        plan.lock();
        assertTrue(plan.isLocked());

        // placeFacility returns false when locked.
        assertFalse(plan.placeFacility(Facility.at(FacilityType.DESK, GridPos.of(1, 1))));

        // addRoom throws when locked.
        try {
            plan.addRoom(new Room(RoomType.QA, 10, 10, 3, 3));
            fail("expected IllegalStateException when adding room to locked plan");
        } catch (IllegalStateException expected) {
            // ok
        }

        // Unlock re-opens edits.
        plan.unlock();
        assertFalse(plan.isLocked());
        assertTrue(plan.placeFacility(Facility.at(FacilityType.DESK, GridPos.of(1, 1))));
    }
}
