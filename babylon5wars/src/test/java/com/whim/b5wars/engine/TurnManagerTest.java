package com.whim.b5wars.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Placement;
import com.whim.b5wars.model.Race;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;
import com.whim.b5wars.model.VictoryCondition;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponArc;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TurnManagerTest {

    private GameState state;
    private TurnManager tm;

    @Before
    public void setUp() {
        Weapon fwd = Fixtures.weapon("beam", WeaponArc.forward(), new int[] {2, 6, 12}, 8, 4);
        ShipClass ea = new ShipClass("ea", "Hyperion", Race.EARTH_ALLIANCE, 300, 8, 2, 8, 8,
                2, 4, 5, 6, Fixtures.uniformArmor(4), Fixtures.uniformStructure(10),
                DefenseType.ARMOR, Fixtures.weapons(fwd), new ArrayList<com.whim.b5wars.model.Special>());

        List<ShipClass> classes = new ArrayList<ShipClass>();
        classes.add(ea);
        List<Faction> factions = new ArrayList<Faction>();
        factions.add(new Faction(Race.EARTH_ALLIANCE, classes));

        List<Placement> placements = new ArrayList<Placement>();
        placements.add(new Placement("ea", Side.A, new Hex(0, 0), Facing.F, 6));
        placements.add(new Placement("ea", Side.B, new Hex(0, 10), Facing.B, 6));
        Scenario scenario = new Scenario("duel", 20, 20, placements,
                VictoryCondition.DESTROY_OR_CRIPPLE_ENEMY, 12);

        state = new GameState(scenario, factions, 42L);
        tm = new TurnManager(state);
    }

    @Test
    public void buildsShipsFromPlacements() {
        assertEquals(2, state.getShips().size());
        assertEquals(TurnPhase.INITIATIVE, state.getPhase());
    }

    @Test
    public void initiativePhaseSetsWinnerAndAdvances() {
        tm.advancePhase();
        assertNotNull(state.getInitiativeWinner());
        assertEquals(TurnPhase.POWER, state.getPhase());
    }

    @Test
    public void powerPhaseSetsThrustAvailable() {
        tm.advancePhase(); // INITIATIVE -> POWER
        tm.advancePhase(); // POWER -> EW
        for (Ship s : state.getShips()) {
            assertTrue("thrust set from class thrust/power", s.getThrustAvailable() > 0);
        }
        assertEquals(TurnPhase.EW, state.getPhase());
    }

    @Test
    public void fullPhaseCycleReachesEndOfTurn() {
        tm.advancePhase(); // INITIATIVE -> POWER
        tm.advancePhase(); // POWER -> EW
        tm.advancePhase(); // EW -> IMPULSE
        tm.advancePhase(); // IMPULSE (runs loop) -> END_OF_TURN
        assertEquals(TurnPhase.END_OF_TURN, state.getPhase());
        tm.advancePhase(); // END_OF_TURN -> next turn or over
        assertTrue(state.getTurn() >= 2 || state.isOver());
    }
}
