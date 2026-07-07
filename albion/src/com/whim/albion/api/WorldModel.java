package com.whim.albion.api;

import com.whim.albion.api.Enums.Direction;

import java.util.List;

/**
 * Navigable, mutable world state owned by the model (Task 1) and driven by the
 * engine (Task 2). Extends the read-only {@link Views.WorldView} the UI renders.
 */
public interface WorldModel extends Views.WorldView {

    /** Current map id. */
    String mapId();

    /** Attempt a single step in {@code dir}; false if blocked. Updates facing. */
    boolean stepPlayer(Direction dir);

    /** Rotate the player's facing without moving (used by first-person turning). */
    void turnPlayer(Direction newFacing);

    /** Load a map and place the player at the entry cell/facing. */
    void loadMap(String mapId, int x, int y, Direction facing);

    /**
     * A map transition anchored at (x,y), or null. Triggered when the player steps
     * onto (or interacts with) the tile.
     */
    Transition transitionAt(int x, int y);

    /** The interactable at (x,y), or null (NPC, sign, chest, etc.). */
    Interactable interactableAt(int x, int y);

    /** Encounter id triggered by entering (x,y), or null. Consumed once fired. */
    String encounterAt(int x, int y);

    /** Mark an encounter cleared so it will not re-trigger. */
    void clearEncounter(int x, int y);

    /** A door/edge link to another map. */
    interface Transition {
        String targetMapId();
        int targetX();
        int targetY();
        Direction targetFacing();
    }

    /** Something the player can Look/Use/Talk to on a tile. */
    interface Interactable {
        String id();
        String name();
        /** Dialogue tree id to open, or null. */
        String dialogueId();
        /** Item ids granted on first use (chest), or empty. */
        List<String> loot();
        boolean consumed();
        void consume();
    }
}
