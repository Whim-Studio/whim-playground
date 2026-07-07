package com.whim.albion.data;

import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.ModelFactory;
import com.whim.albion.entities.Character;
import com.whim.albion.entities.PartyModelImpl;
import com.whim.albion.world.WorldModelImpl;

import java.util.EnumSet;
import java.util.Set;

/**
 * Public no-arg factory the engine/app instantiates to build a fresh game: the
 * content pack, the two starting maps, a four-member party (four professions
 * spanning all four magic schools), and an empty journal.
 *
 * <p>The {@code seed} is accepted for API compatibility and reserved for future
 * procedural variation; the starting state is deterministic today.</p>
 */
public final class AlbionModelFactory implements ModelFactory {

    /** Required public no-arg constructor. */
    public AlbionModelFactory() {}

    @Override
    public GameModel newGame(long seed) {
        AlbionContent content = new AlbionContent();

        WorldModelImpl world = new WorldModelImpl();
        MapFactory.buildWorld(world, content);

        PartyModelImpl party = buildParty(content);

        JournalModelImpl journal = new JournalModelImpl();

        return new AlbionGameModel(world, party, content, journal);
    }

    // --------------------------------------------------------------- party

    private PartyModelImpl buildParty(AlbionContent content) {
        PartyModelImpl party = new PartyModelImpl();
        party.addGold(60);

        party.addMember(buildWarrior(content));
        party.addMember(buildRanger(content));
        party.addMember(buildHealer(content));
        party.addMember(buildMage(content));

        // A couple of shared consumables tucked into the leader's pack.
        party.giveItem(0, AlbionContent.ITM_HEAL_DRAUGHT, 2);
        party.giveItem(0, AlbionContent.ITM_MANA_TONIC, 1);
        return party;
    }

    private Character buildWarrior(AlbionContent content) {
        Character c = new Character.Builder("Bran Ironhand", content)
                .profession("Warrior").portrait("portrait.warrior")
                .stat(StatType.STRENGTH, 16).stat(StatType.STAMINA, 15)
                .stat(StatType.DEXTERITY, 12).stat(StatType.SPEED, 11)
                .stat(StatType.LUCK, 10).stat(StatType.INTELLIGENCE, 8)
                .stat(StatType.MAGIC_TALENT, 4).stat(StatType.MAGIC_RESISTANCE, 8)
                .skill(SkillType.MELEE, 45).skill(SkillType.CRITICAL, 20)
                .skill(SkillType.RANGED, 10).skill(SkillType.LOCKPICKING, 10)
                .build();
        equipStarting(c, AlbionContent.ITM_SHORT_SWORD, AlbionContent.ITM_LEATHER_VEST,
                AlbionContent.ITM_ROUND_SHIELD, AlbionContent.ITM_IRON_CAP);
        return c;
    }

    private Character buildRanger(AlbionContent content) {
        Set<SpellSchool> schools = EnumSet.of(SpellSchool.NATURE);
        Character c = new Character.Builder("Sela Quickbow", content)
                .profession("Ranger").portrait("portrait.ranger").schools(schools)
                .stat(StatType.DEXTERITY, 15).stat(StatType.SPEED, 14)
                .stat(StatType.STRENGTH, 12).stat(StatType.STAMINA, 12)
                .stat(StatType.LUCK, 11).stat(StatType.MAGIC_TALENT, 9)
                .skill(SkillType.RANGED, 45).skill(SkillType.MELEE, 20)
                .skill(SkillType.CRITICAL, 18).skill(SkillType.LOCKPICKING, 25)
                .build();
        equipStarting(c, AlbionContent.ITM_HUNT_BOW, AlbionContent.ITM_LEATHER_VEST, null, null);
        c.spellBook().learn(AlbionContent.SPELL_THORN);
        c.spellBook().learn(AlbionContent.SPELL_REGROWTH);
        c.restoreAll();
        return c;
    }

    private Character buildHealer(AlbionContent content) {
        Set<SpellSchool> schools = EnumSet.of(SpellSchool.RESTORATION, SpellSchool.PSIONIC);
        Character c = new Character.Builder("Odar the Kind", content)
                .profession("Healer").portrait("portrait.healer").schools(schools)
                .stat(StatType.MAGIC_TALENT, 15).stat(StatType.INTELLIGENCE, 14)
                .stat(StatType.STAMINA, 11).stat(StatType.SPEED, 9)
                .stat(StatType.LUCK, 12).stat(StatType.MAGIC_RESISTANCE, 12)
                .skill(SkillType.MELEE, 12)
                .build();
        equipStarting(c, AlbionContent.ITM_OAK_STAFF, null, null, null);
        c.spellBook().learn(AlbionContent.SPELL_MEND);
        c.spellBook().learn(AlbionContent.SPELL_FOCUS);
        c.restoreAll();
        return c;
    }

    private Character buildMage(AlbionContent content) {
        Set<SpellSchool> schools = EnumSet.of(SpellSchool.DESTRUCTION, SpellSchool.PSIONIC);
        Character c = new Character.Builder("Ysolde Emberquill", content)
                .profession("Mage").portrait("portrait.mage").schools(schools)
                .stat(StatType.INTELLIGENCE, 16).stat(StatType.MAGIC_TALENT, 16)
                .stat(StatType.SPEED, 10).stat(StatType.STAMINA, 8)
                .stat(StatType.LUCK, 10).stat(StatType.MAGIC_RESISTANCE, 14)
                .skill(SkillType.MELEE, 8).skill(SkillType.CRITICAL, 12)
                .build();
        equipStarting(c, AlbionContent.ITM_OAK_STAFF, null, null, null);
        c.spellBook().learn(AlbionContent.SPELL_SPARK);
        c.spellBook().learn(AlbionContent.SPELL_DAZE);
        c.restoreAll();
        return c;
    }

    /** Add then equip up to four starting items (nulls skipped). */
    private void equipStarting(Character c, String weapon, String body, String shield, String head) {
        String[] ids = { weapon, body, shield, head };
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            if (id == null) continue;
            c.inventoryModel().add(id, 1);
            c.equip(id);
        }
    }
}
