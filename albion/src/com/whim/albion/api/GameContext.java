package com.whim.albion.api;

import com.whim.albion.api.Enums.Direction;

/**
 * Mutation seam passed to dialogue options and scripted events. It lets content
 * (Task 1) drive world/party/quest changes without depending on the concrete
 * engine (Task 2). The engine provides the implementation.
 */
public interface GameContext {

    // world flags / quest state
    boolean flag(String key);
    void setFlag(String key, boolean value);

    // party economy / inventory
    void addGold(int amount);
    boolean spendGold(int amount);
    void giveItem(String itemId, int quantity);
    boolean takeItem(String itemId, int quantity);
    boolean hasItem(String itemId);

    // quests
    void startQuest(String questId);
    void addObjective(String questId, String objective);
    void completeQuest(String questId);

    // flow control
    void startCombat(String encounterId);
    void teleport(String mapId, int x, int y, Direction facing);

    /** Queue a transient status banner for the UI. */
    void notify(String message);
}
