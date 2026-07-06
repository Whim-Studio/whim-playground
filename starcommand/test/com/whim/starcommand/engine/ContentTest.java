package com.whim.starcommand.engine;

import com.whim.starcommand.model.Ship;
import com.whim.starcommand.model.Weapon;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Verifies content loads from the JSON data files with the expected values. */
public class ContentTest {

    @Test
    public void rolesLoadFromJson() {
        String[] roles = Content.roles();
        assertEquals(6, roles.length);
        assertEquals("Pilot", roles[0]);
    }

    @Test
    public void weaponsLoadWithTypedFields() {
        List<Weapon> weapons = Content.weaponShop();
        assertEquals(5, weapons.size());
        Weapon first = weapons.get(0);
        assertEquals("Pulse Laser", first.name);
        assertEquals(Weapon.Type.BEAM, first.type);
        assertEquals(600, first.cost);
    }

    @Test
    public void startingShipIsScoutWithTwoLasers() {
        Ship s = Content.startingShip();
        assertEquals("Scout", s.className);
        assertEquals(2, s.weapons.size());
        assertEquals(40, s.maxHull);
    }

    @Test
    public void shipCostsResolveByClass() {
        assertEquals(0, Content.shipCost("Scout"));
        assertEquals(4000, Content.shipCost("Corvette"));
        assertEquals(18000, Content.shipCost("Cruiser"));
    }
}
