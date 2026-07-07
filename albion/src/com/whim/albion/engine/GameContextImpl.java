package com.whim.albion.engine;

import com.whim.albion.api.GameContext;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.GameStateType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.QuestStatus;

/**
 * Engine-side implementation of the content mutation seam. Dialogue options and
 * scripted events call these to change world/party/quest state without depending
 * on the concrete engine. Flag and quest ids seen here are mirrored into the
 * engine's persistence shadow so they can be enumerated on save (the journal
 * view exposes neither raw flag keys nor quest ids).
 */
final class GameContextImpl implements GameContext {

    private final GameEngine engine;

    GameContextImpl(GameEngine engine) { this.engine = engine; }

    private GameModel model() { return engine.model(); }

    // ---- flags ----
    @Override public boolean flag(String key) { return model().journal().flag(key); }

    @Override public void setFlag(String key, boolean value) {
        model().journal().setFlag(key, value);
        engine.shadowFlags.put(key, value);
    }

    // ---- economy / inventory ----
    @Override public void addGold(int amount) { model().party().addGold(amount); }
    @Override public boolean spendGold(int amount) { return model().party().spendGold(amount); }

    @Override public void giveItem(String itemId, int quantity) {
        model().party().giveItem(model().party().activeIndex(), itemId, quantity);
    }
    @Override public boolean takeItem(String itemId, int quantity) {
        return model().party().takeItem(itemId, quantity);
    }
    @Override public boolean hasItem(String itemId) { return model().party().hasItem(itemId); }

    // ---- quests ----
    @Override public void startQuest(String questId) {
        // GameContext.startQuest carries only an id; JournalModel wants a title and
        // first objective. We synthesize a readable title from the id and a generic
        // first objective. Assumption documented in task2-notes.md.
        String title = titleFor(questId);
        String first = "Quest started.";
        model().journal().startQuest(questId, title, first);
        engine.shadowQuests.put(questId, QuestStatus.ACTIVE);
        engine.shadowQuestTitles.put(questId, title);
        java.util.List<String> objs = new java.util.ArrayList<String>();
        objs.add(first);
        engine.shadowQuestObjectives.put(questId, objs);
    }
    @Override public void addObjective(String questId, String objective) {
        model().journal().addObjective(questId, objective);
        java.util.List<String> objs = engine.shadowQuestObjectives.get(questId);
        if (objs == null) { objs = new java.util.ArrayList<String>(); engine.shadowQuestObjectives.put(questId, objs); }
        objs.add(objective);
    }
    @Override public void completeQuest(String questId) {
        model().journal().completeQuest(questId);
        engine.shadowQuests.put(questId, QuestStatus.COMPLETED);
    }

    private static String titleFor(String questId) {
        if (questId == null || questId.isEmpty()) return "Quest";
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (int i = 0; i < questId.length(); i++) {
            char c = questId.charAt(i);
            if (c == '_' || c == '-' || c == '.') { sb.append(' '); up = true; }
            else if (up) { sb.append(Character.toUpperCase(c)); up = false; }
            else sb.append(c);
        }
        return sb.toString();
    }

    // ---- flow control ----
    @Override public void startCombat(String encounterId) { engine.startCombat(encounterId); }

    @Override public void teleport(String mapId, int x, int y, Direction facing) {
        model().world().loadMap(mapId, x, y, facing);
        engine.afterWorldChanged();
        engine.setStatus("Teleported to " + model().world().mapName() + ".");
    }

    @Override public void notify(String message) { engine.setStatus(message); }
}
