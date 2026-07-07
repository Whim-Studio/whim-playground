package com.whim.albion.world;

import com.whim.albion.api.WorldModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Something on a tile the player can Look/Use/Talk to: an NPC (has a
 * {@code dialogueId}), a chest (has {@code loot}), or a scripted fixture such as
 * a locked door (a {@code dialogueId} whose options gate on carrying the key).
 */
public final class InteractableImpl implements WorldModel.Interactable {

    private final String id;
    private final String name;
    private final String dialogueId;
    private final List<String> loot;
    private final boolean reusable;
    private boolean consumed;

    private InteractableImpl(String id, String name, String dialogueId, List<String> loot, boolean reusable) {
        this.id = id;
        this.name = name;
        this.dialogueId = dialogueId;
        this.loot = loot == null ? new ArrayList<String>() : new ArrayList<String>(loot);
        this.reusable = reusable;
    }

    /** A talk target (NPC / sign / locked door) — never consumed. */
    public static InteractableImpl talk(String id, String name, String dialogueId) {
        return new InteractableImpl(id, name, dialogueId, null, true);
    }

    /** A one-shot lootable (chest); consumed after its loot is taken. */
    public static InteractableImpl chest(String id, String name, List<String> loot) {
        return new InteractableImpl(id, name, null, loot, false);
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public String dialogueId() { return dialogueId; }

    @Override public List<String> loot() {
        return Collections.unmodifiableList(new ArrayList<String>(loot));
    }

    @Override public boolean consumed() { return consumed; }
    @Override public void consume() { if (!reusable) consumed = true; }
}
