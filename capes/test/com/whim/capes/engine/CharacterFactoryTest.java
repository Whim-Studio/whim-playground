package com.whim.capes.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.whim.capes.content.ClickLockData;
import com.whim.capes.content.ClickLockModule;
import com.whim.capes.model.Ability;
import com.whim.capes.model.AbilityKind;
import com.whim.capes.model.Character;

/** Phase 2 tests: Click-and-Lock combination, super/mundane flags, and 1-up renumbering. */
public class CharacterFactoryTest {

    private ClickLockModule powerSet(String name) {
        for (ClickLockModule m : ClickLockData.powerSets()) if (m.name().equals(name)) return m;
        throw new IllegalStateException("missing " + name);
    }
    private ClickLockModule persona(String name) {
        for (ClickLockModule m : ClickLockData.personae()) if (m.name().equals(name)) return m;
        throw new IllegalStateException("missing " + name);
    }
    private ClickLockModule skillSet(String name) {
        for (ClickLockModule m : ClickLockData.skillSets()) if (m.name().equals(name)) return m;
        throw new IllegalStateException("missing " + name);
    }

    @Test public void catalogueSizes() {
        assertEquals(15, ClickLockData.powerSets().size());
        assertEquals(17, ClickLockData.skillSets().size());
        assertEquals(17, ClickLockData.personae().size());
    }

    @Test public void combineGivesFivePowersFiveAttitudesFiveStyles() {
        List<Ability> pool = CharacterFactory.combine(powerSet("Godling"), persona("Seducer"));
        assertEquals(5, countKind(pool, AbilityKind.POWER));
        assertEquals(5, countKind(pool, AbilityKind.ATTITUDE));
        assertEquals(5, countKind(pool, AbilityKind.STYLE)); // 3 from set + 2 from persona
    }

    @Test public void powerSetItemsAreSuperPersonaItemsAreMundane() {
        List<Ability> pool = CharacterFactory.combine(powerSet("Godling"), persona("Seducer"));
        for (Ability a : pool) {
            if (a.kind() == AbilityKind.POWER) assertTrue(a.isSuperPowered());
            if (a.kind() == AbilityKind.ATTITUDE) assertTrue(!a.isSuperPowered());
        }
        // A Style from the Power-Set is super; a Style from the Persona is mundane.
        int superStyles = 0, mundaneStyles = 0;
        for (Ability a : pool) if (a.kind() == AbilityKind.STYLE) {
            if (a.isSuperPowered()) superStyles++; else mundaneStyles++;
        }
        assertEquals(3, superStyles);
        assertEquals(2, mundaneStyles);
    }

    @Test public void skillSetItemsAreMundane() {
        List<Ability> pool = CharacterFactory.combine(skillSet("Journalist"), persona("Crusader"));
        for (Ability a : pool) assertTrue(!a.isSuperPowered());
        assertEquals(5, countKind(pool, AbilityKind.SKILL));
    }

    @Test public void renumberProducesLegalOneUpShape() {
        // Take Godling + Seducer, drop three (one Power, one Attitude, one Style) -> 12, then renumber.
        List<Ability> pool = CharacterFactory.combine(powerSet("Godling"), persona("Seducer"));
        Character c = new Character("h", "Test", true);
        int p = 0, a = 0, s = 0;
        for (Ability ab : pool) {
            if (ab.kind() == AbilityKind.POWER && p++ == 0) continue;      // drop first power
            if (ab.kind() == AbilityKind.ATTITUDE && a++ == 0) continue;   // drop first attitude
            if (ab.kind() == AbilityKind.STYLE && s++ == 0) continue;      // drop first style
            c.abilities().add(ab);
        }
        CharacterFactory.renumberColumns(c);
        assertNull(c.validateAbilityShape());
        assertEquals(4, c.abilitiesOfKind(AbilityKind.POWER).size());
        assertEquals(4, c.abilitiesOfKind(AbilityKind.ATTITUDE).size());
        assertEquals(4, c.abilitiesOfKind(AbilityKind.STYLE).size());
    }

    private int countKind(List<Ability> pool, AbilityKind k) {
        int n = 0;
        for (Ability a : pool) if (a.kind() == k) n++;
        return n;
    }
}
