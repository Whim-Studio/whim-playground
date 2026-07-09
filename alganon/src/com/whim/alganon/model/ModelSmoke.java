package com.whim.alganon.model;

import com.whim.alganon.api.Defs.QuestDef;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.api.WorldModel;

import java.util.Random;

/**
 * Standalone smoke check for Task 1's model + content. Run with:
 * {@code java -cp target/classes com.whim.alganon.model.ModelSmoke}
 *
 * <p>Proves: {@code newGame(0)} + {@code applyCreation(...)} yields a placed character
 * with abilities; the starting zone loads with mobs and gather nodes; a mob is
 * spawnable and killable; and {@code generateQuest} produces a procedural quest.</p>
 */
public final class ModelSmoke {
    private ModelSmoke() {}

    public static void main(String[] args) {
        AlganonModelFactory factory = new AlganonModelFactory();

        // Content sanity.
        check(factory.content().races().size() == 2, "expected 2 races");
        check(factory.content().families().size() == 10, "expected 10 families");
        check(factory.content().classes().size() == 6, "expected 6 classes");
        check(factory.content().zones().size() == 3, "expected 3 zones");
        check(factory.content().staticQuests().size() >= 3, "expected >=3 static quests");
        check(factory.content().recipes().size() >= 6, "expected >=6 recipes");

        // New game + creation.
        GameModel model = factory.newGame(0L);
        factory.applyCreation(model, "race_asharr", "fam_ashheart", "CHAMPION", "Testwyn");
        AlganonCharacter p = ((AlganonGameModel) model).player();

        check("Testwyn".equals(p.getName()), "name not applied");
        check(p.zoneId() != null, "character not placed in a zone");
        check(p.pos() != null, "character has no position");
        check(!p.knownAbilityIds().isEmpty(), "character learned no abilities");
        check(p.attackPower() > 0, "attackPower should be positive");
        check(p.hp() == p.maxHp() && p.hp() > 0, "character should start at full HP");

        // Loaded world with mobs + nodes.
        WorldModel world = model.world();
        check(world != null, "no world loaded");
        check(!world.mobs().isEmpty(), "starting zone has no mobs");
        check(!world.nodes().isEmpty(), "starting zone has no gather nodes");

        // Kill a mob.
        WorldModel.MobEntity mob = world.mobs().get(0);
        check(mob.alive(), "mob should start alive");
        int guard = 0;
        while (mob.alive() && guard++ < 1000) {
            mob.takeDamage(p.attackPower(), DamageType.PHYSICAL);
        }
        check(!mob.alive(), "mob should be killable");

        // Zone traversal: load the dungeon and confirm its boss exists.
        WorldModel dungeon = model.loadZone("zone_dungeon");
        boolean hasWarden = false;
        for (WorldModel.MobEntity m : dungeon.mobs()) {
            if ("mob_vaultwarden".equals(m.defId())) hasWarden = true;
        }
        check(hasWarden, "dungeon should contain the warden boss");

        // Procedural quest generation.
        QuestDef q = factory.content().generateQuest(3, FamilyArchetype.EXPLORER, new Random(1));
        check(q != null && q.procedural, "generateQuest should return a procedural quest");
        check(!q.objectives.isEmpty(), "generated quest should have an objective");

        System.out.println("ModelSmoke OK — character '" + p.getName() + "' placed in "
                + world.zoneName() + " with " + p.knownAbilityIds().size() + " abilities; "
                + "killed " + mob.name() + "; generated quest '" + q.name + "'.");
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new AssertionError("SMOKE FAIL: " + msg);
    }
}
