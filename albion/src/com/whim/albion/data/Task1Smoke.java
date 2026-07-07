package com.whim.albion.data;

import com.whim.albion.api.Combatant;
import com.whim.albion.api.Content;
import com.whim.albion.api.Content.DialogueTree;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.GameContext;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.ModelFactory;
import com.whim.albion.api.WorldModel;
import com.whim.albion.api.Views.CharacterView;

import java.util.List;

/**
 * Standalone smoke check for the Task 1 model + content. Run with
 * {@code java com.whim.albion.data.Task1Smoke}. Proves {@code newGame(0)} yields
 * a populated party, a loadable town map, a spawnable encounter and a navigable
 * dialogue tree. Exits non-zero on any failed assertion.
 */
public final class Task1Smoke {

    private Task1Smoke() {}

    public static void main(String[] args) {
        int failures = 0;

        ModelFactory factory = new AlbionModelFactory();
        GameModel model = factory.newGame(0L);
        failures += check("model built", model != null);

        // --- party ---
        List<CharacterView> members = model.party().members();
        failures += check("party has 3-4 members", members.size() >= 3 && members.size() <= 4);
        failures += check("party gold seeded", model.party().gold() > 0);

        boolean[] schools = new boolean[SpellSchool.values().length];
        for (CharacterView c : members) {
            failures += check("member '" + c.name() + "' has LP", c.maxLp() > 0 && c.lp() == c.maxLp());
            for (SpellSchool s : SpellSchool.values()) {
                if (c.canCast(s)) schools[s.ordinal()] = true;
            }
        }
        int schoolCount = 0;
        for (boolean b : schools) if (b) schoolCount++;
        failures += check("at least two magic schools represented", schoolCount >= 2);
        failures += check("all four magic schools represented", schoolCount == 4);

        List<Combatant> partyCombatants = model.party().asCombatants();
        failures += check("party exposes combat adapters", partyCombatants.size() == members.size());

        // --- content counts ---
        Content content = model.content();
        int items = 0;
        String[] itemIds = {
                AlbionContent.ITM_SHORT_SWORD, AlbionContent.ITM_HUNT_BOW, AlbionContent.ITM_OAK_STAFF,
                AlbionContent.ITM_LEATHER_VEST, AlbionContent.ITM_ROUND_SHIELD, AlbionContent.ITM_IRON_CAP,
                AlbionContent.ITM_SUN_AMULET, AlbionContent.ITM_HEAL_DRAUGHT, AlbionContent.ITM_MANA_TONIC,
                AlbionContent.ITM_SCROLL_SPARK, AlbionContent.ITM_RUSTY_KEY, AlbionContent.ITM_RELIC_SHARD };
        for (String id : itemIds) if (content.item(id) != null) items++;
        failures += check("~10 items authored (found " + items + ")", items >= 10);

        // --- world: town map loaded ---
        failures += check("town map loaded", MapFactory.MAP_TOWN.equals(model.world().mapId()));
        failures += check("town is OUTDOOR_2D", model.world().mapType() == MapType.OUTDOOR_2D);
        failures += check("town has tiles", model.world().tileAt(
                MapFactory.TOWN_START_X, MapFactory.TOWN_START_Y) != null);
        failures += check("town has NPCs", !model.world().npcs().isEmpty());
        String townName = model.world().mapName();

        // --- encounter spawn ---
        List<Combatant> enemies = content.spawnEncounter(AlbionContent.ENC_DUNGEON_1);
        failures += check("encounter spawns enemies", enemies != null && !enemies.isEmpty());
        boolean hostile = true;
        for (Combatant e : enemies) if (e.playerSide()) hostile = false;
        failures += check("spawned enemies are enemy-side", hostile);

        // --- dialogue tree ---
        DialogueTree tree = content.dialogue(AlbionContent.DLG_ELDER);
        failures += check("dialogue tree resolves", tree != null);
        failures += check("dialogue has a root node",
                tree != null && tree.node(tree.rootId()) != null);
        failures += check("root node has options",
                tree != null && !tree.node(tree.rootId()).options().isEmpty());

        // --- quest wired through dialogue + dungeon interactable + journal ---
        failures += checkQuestWiring(model);

        System.out.println();
        if (failures == 0) {
            System.out.println("SMOKE PASS: newGame(0) → party of " + members.size()
                    + ", town '" + townName + "', "
                    + enemies.size() + "-enemy encounter, dialogue OK.");
        } else {
            System.out.println("SMOKE FAIL: " + failures + " assertion(s) failed.");
            System.exit(1);
        }
    }

    private static int check(String label, boolean ok) {
        System.out.println((ok ? "  [ok]   " : "  [FAIL] ") + label);
        return ok ? 0 : 1;
    }

    /**
     * Drives the "Sunken Relic" quest end-to-end through dialogue options, the
     * dungeon reliquary interactable and the journal, using a minimal in-class
     * {@link GameContext} (the real one is Task 2's engine).
     */
    private static int checkQuestWiring(GameModel model) {
        int f = 0;
        JournalModelImpl journal = (JournalModelImpl) model.journal();
        Content content = model.content();
        GameContext ctx = new SmokeContext(model);

        DialogueTree elder = content.dialogue(AlbionContent.DLG_ELDER);
        apply(elder, "start", 0, ctx);       // "Tell me about the missing relic."
        apply(elder, "give_quest", 0, ctx);  // "I will bring it back."
        f += check("dialogue starts the quest", journal.questActive(AlbionContent.QUEST_RELIC));
        f += check("quest dialogue grants the crypt key", model.party().hasItem(AlbionContent.ITM_RUSTY_KEY));

        DialogueTree door = content.dialogue(AlbionContent.DLG_DOOR);
        f += check("locked door offers key option only with key",
                door.node("start").options().get(0).available(ctx));
        door.node("start").options().get(0).apply(ctx); // uses key + teleports into vault
        f += check("using the key consumes it", !model.party().hasItem(AlbionContent.ITM_RUSTY_KEY));

        WorldModel world = model.world();
        WorldModel.Interactable reliquary =
                world.interactableAt(MapFactory.CRYPT_VAULT_X + 1, MapFactory.CRYPT_VAULT_Y);
        f += check("dungeon reliquary interactable exists", reliquary != null);
        boolean holdsShard = reliquary != null && reliquary.loot().contains(AlbionContent.ITM_RELIC_SHARD);
        f += check("reliquary holds the quest relic", holdsShard);
        if (reliquary != null) {
            for (String id : reliquary.loot()) model.party().giveItem(0, id, 1);
        }

        apply(elder, "start", 1, ctx);   // "I have recovered the relic."
        apply(elder, "turn_in", 0, ctx); // "You are welcome."
        f += check("journal marks the quest completed", journal.questCompleted(AlbionContent.QUEST_RELIC));
        f += check("relic handed over on turn-in", !model.party().hasItem(AlbionContent.ITM_RELIC_SHARD));
        return f;
    }

    private static void apply(DialogueTree tree, String nodeId, int optionIndex, GameContext ctx) {
        DialogueTree.Option opt = tree.node(nodeId).options().get(optionIndex);
        opt.apply(ctx);
    }

    /** Minimal {@link GameContext} backed by the model, sufficient to run dialogue effects. */
    private static final class SmokeContext implements GameContext {
        private final GameModel model;
        SmokeContext(GameModel model) { this.model = model; }

        @Override public boolean flag(String key) { return model.journal().flag(key); }
        @Override public void setFlag(String key, boolean value) { model.journal().setFlag(key, value); }
        @Override public void addGold(int amount) { model.party().addGold(amount); }
        @Override public boolean spendGold(int amount) { return model.party().spendGold(amount); }
        @Override public void giveItem(String itemId, int quantity) { model.party().giveItem(0, itemId, quantity); }
        @Override public boolean takeItem(String itemId, int quantity) { return model.party().takeItem(itemId, quantity); }
        @Override public boolean hasItem(String itemId) { return model.party().hasItem(itemId); }
        @Override public void startQuest(String questId) {
            model.journal().startQuest(questId, "The Sunken Relic", null);
        }
        @Override public void addObjective(String questId, String objective) {
            model.journal().addObjective(questId, objective);
        }
        @Override public void completeQuest(String questId) { model.journal().completeQuest(questId); }
        @Override public void startCombat(String encounterId) { /* no-op in the smoke */ }
        @Override public void teleport(String mapId, int x, int y, Direction facing) {
            model.world().loadMap(mapId, x, y, facing);
        }
        @Override public void notify(String message) { /* no-op in the smoke */ }
    }
}
