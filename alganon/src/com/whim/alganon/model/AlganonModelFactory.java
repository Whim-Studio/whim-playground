package com.whim.alganon.model;

import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.AbilityDef;
import com.whim.alganon.api.Defs.ClassDef;
import com.whim.alganon.api.Defs.FamilyDef;
import com.whim.alganon.api.Defs.RaceDef;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.ModelFactory;
import com.whim.alganon.content.AlganonContent;

import java.util.Map;

/**
 * Task 1's single entry point. Owns "how a character is built" — turning a
 * race/family/class/name selection into starting stats, resources, abilities,
 * inventory, and a placed position in the starting zone.
 *
 * <p>[Gap — my design] The starting-stat, HP/resource and starting-gear rules below
 * are original tuning consistent with DESIGN.md; the anchored content (class list,
 * resources, schools/stances) comes from {@link AlganonContent}.</p>
 */
public final class AlganonModelFactory implements ModelFactory {

    private final AlganonContent content = new AlganonContent();

    /** Required public no-arg constructor. */
    public AlganonModelFactory() {}

    @Override public Content content() { return content; }

    @Override public GameModel newGame(long seed) { return new AlganonGameModel(content, seed); }

    @Override
    public void applyCreation(GameModel model, String raceId, String familyId, String classId, String name) {
        AlganonGameModel gm = (AlganonGameModel) model;
        AlganonCharacter p = gm.player();

        RaceDef race = content.race(raceId);
        FamilyDef family = content.family(familyId);
        ClassDef clazz = content.clazz(classId);
        if (race == null || clazz == null) {
            throw new IllegalArgumentException("Invalid creation choice: race=" + raceId + " class=" + classId);
        }

        // identity
        p.setRaceId(race.id);
        p.setFamilyId(family != null ? family.id : null);
        p.setClassId(clazz.id.name());
        p.setName(name);
        p.setFaction(race.faction);
        p.setLevel(1);
        p.setXp(0);

        // base stats = 10 each + race mods
        for (StatType s : StatType.values()) p.stats().put(s, 10);
        for (Map.Entry<StatType, Integer> e : race.statMods.entrySet()) {
            p.stats().put(e.getKey(), p.stats().get(e.getKey()) + e.getValue());
        }

        // HP from Stamina
        int stamina = p.stats().get(StatType.STAMINA);
        p.setMaxHp(60 + stamina * 4);
        p.setHp(p.maxHp());

        // resource by class
        ResourceType res = clazz.resource;
        p.setResourceType(res);
        if (res == ResourceType.FURY) {
            p.setMaxResource(100);
            p.setResource(0);                 // Fury builds in combat
        } else if (res == ResourceType.FOCUS) {
            p.setMaxResource(100);
            p.setResource(100);
        } else { // MANA
            int intellect = p.stats().get(StatType.INTELLECT);
            p.setMaxResource(50 + intellect * 5);
            p.setResource(p.maxResource());
        }

        // spec defaults
        p.setStance(Stance.BALANCE);
        p.setSchool(clazz.id == ClassId.MAGUS ? School.FLAME : School.NONE);

        // learn all class abilities available at level 1
        p.knownAbilityIds().clear();
        for (String abId : clazz.abilityIds) {
            AbilityDef def = content.ability(abId);
            if (def != null && def.levelReq <= p.level()) p.learnAbility(abId);
        }

        // starting gear + inventory
        p.equipped().clear();
        p.inventory().clear();
        String weapon = startingWeapon(clazz.id);
        p.equip(EquipSlot.WEAPON, weapon);
        p.equip(EquipSlot.CHEST, "itm_leathervest");
        p.addItem("itm_healpotion", 3);
        if (res == ResourceType.MANA) p.addItem("itm_manapotion", 3);
        p.addItem("itm_leathercap", 1);   // a spare to equip from the inventory UI
        p.addGold(25);

        // place in starting zone
        String startZone = content.startingZoneId(race.id);
        AlganonWorld world = (AlganonWorld) gm.loadZone(startZone);
        p.setZoneId(startZone);
        p.setPos(world.spawn());
    }

    private static String startingWeapon(ClassId id) {
        switch (id) {
            case CHAMPION:
            case REAVER:   return "itm_rustyblade";
            case RANGER:   return "itm_huntingbow";
            case MAGUS:
            case CABALIST: return "itm_novicewand";
            case MYSTIC:   return "itm_worncudgel";
            default:       return "itm_rustyblade";
        }
    }
}
