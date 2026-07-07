package com.whim.b5wars.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;

import org.junit.Before;
import org.junit.Test;

public class MovementEngineTest {

    private MovementEngine engine;

    @Before
    public void setUp() {
        engine = new MovementEngine();
    }

    private Ship shipWithTurnMode(int turnMode, int maxSpeed, int speed) {
        ShipClass c = Fixtures.shipClass("mover", 200, maxSpeed, turnMode, 10, 10, 4, 10,
                DefenseType.ARMOR, Fixtures.weapons());
        return Fixtures.ship(c, Side.A, new Hex(0, 0), Facing.F, speed);
    }

    @Test
    public void moveForwardBoundedBySpeed() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(0, 8, 2);
        assertTrue(engine.moveForward(s));
        assertTrue(engine.moveForward(s));
        assertFalse("third forward move exceeds Speed 2", engine.moveForward(s));
        assertEquals(2, s.getStraightHexes());
    }

    @Test
    public void cannotTurnBeforeTravellingTurnMode() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(2, 8, 5);
        s.setThrustAvailable(5);
        // Straight-hexes = 0 < turnMode 2 -> illegal.
        assertFalse(engine.turn(s, 1));
        engine.moveForward(s);
        assertFalse("still only 1 straight hex", engine.turn(s, 1));
        engine.moveForward(s);
        // Now 2 straight hexes >= turnMode 2 -> legal.
        Facing before = s.getFacing();
        assertTrue(engine.turn(s, 1));
        assertEquals(before.rotate(1), s.getFacing());
        assertEquals("straight-hex counter resets after a turn", 0, s.getStraightHexes());
    }

    @Test
    public void turnRequiresThrust() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(0, 8, 5);
        s.setThrustAvailable(0);
        assertFalse("no thrust -> cannot turn", engine.turn(s, 1));
    }

    @Test
    public void accelerateBoundedByMaxSpeed() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(0, 8, 6);
        s.setThrustAvailable(10);
        engine.accelerate(s, 5); // 6 + 5 = 11 -> clamp to maxSpeed 8
        assertEquals(8, s.getSpeed());
    }

    @Test
    public void accelerateBoundedByThrust() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(0, 8, 4);
        s.setThrustAvailable(1); // ACCEL_THRUST_PER_SPEED = 1 -> only +1 affordable
        engine.accelerate(s, 5);
        assertEquals(5, s.getSpeed());
        assertEquals(0, s.getThrustAvailable());
    }

    @Test
    public void decelerateFlooredAtZero() {
        engine.resetTurn();
        Ship s = shipWithTurnMode(0, 8, 2);
        s.setThrustAvailable(10);
        engine.accelerate(s, -5);
        assertEquals(0, s.getSpeed());
    }

    @Test
    public void driftAdvancesAlongVectorWithoutTurning() {
        Ship s = shipWithTurnMode(0, 8, 3);
        Facing facing = s.getFacing();
        Hex expected = new Hex(0, 0).neighbor(facing).neighbor(facing).neighbor(facing);
        engine.drift(s);
        assertEquals("drifts Speed hexes forward", expected, s.getPos());
        assertEquals("facing unchanged by drift", facing, s.getFacing());
    }
}
