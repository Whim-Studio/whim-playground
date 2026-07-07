package com.whim.albion.api;

import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Defs.MonsterDef;
import com.whim.albion.api.Defs.SpellDef;

import java.util.List;

/**
 * Content registry owned by the model/data package (Task 1): the loaded item,
 * spell and monster templates plus encounter and dialogue lookups. Consumed by
 * the engine (Task 2) to resolve ids into concrete definitions and combatants.
 */
public interface Content {

    ItemDef item(String id);
    SpellDef spell(String id);
    MonsterDef monster(String id);

    /** Build the enemy combatants for an encounter id (fresh instances). */
    List<Combatant> spawnEncounter(String encounterId);

    /** Root dialogue node id resolver -> a navigable dialogue tree. */
    DialogueTree dialogue(String dialogueId);

    /** A conversation tree: nodes keyed by id, each with text + conditional options. */
    interface DialogueTree {
        String rootId();
        Node node(String nodeId);

        interface Node {
            String id();
            String speaker();
            String portraitKey();
            String text();
            List<Option> options();
        }

        interface Option {
            String label();
            /** Next node id, or null to end the conversation. */
            String next();
            /** Side effects applied via {@link GameContext} when chosen. */
            void apply(GameContext ctx);
            /** Whether this option is currently selectable. */
            boolean available(GameContext ctx);
        }
    }
}
