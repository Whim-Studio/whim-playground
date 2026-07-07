package com.whim.albion.data;

import com.whim.albion.api.Enums.QuestStatus;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.Views.QuestEntryView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The party's flags + quest log. Flags are arbitrary boolean world state (shop
 * opened, door unlocked). Quests track a title, a {@link QuestStatus} and an
 * ordered objective list. Doubles as the read-only {@link com.whim.albion.api.Views.JournalView}.
 */
public final class JournalModelImpl implements GameModel.JournalModel {

    private final Map<String, Boolean> flags = new LinkedHashMap<String, Boolean>();
    private final Map<String, Quest> quests = new LinkedHashMap<String, Quest>();

    // ------------------------------------------------------------------ flags

    @Override public boolean flag(String key) {
        Boolean b = flags.get(key);
        return b != null && b;
    }

    @Override public void setFlag(String key, boolean value) { flags.put(key, value); }

    // ----------------------------------------------------------------- quests

    @Override public void startQuest(String questId, String title, String firstObjective) {
        Quest q = quests.get(questId);
        if (q == null) {
            q = new Quest(title);
            quests.put(questId, q);
        }
        if (firstObjective != null && !firstObjective.isEmpty()) {
            q.objectives.add(firstObjective);
        }
    }

    @Override public void addObjective(String questId, String objective) {
        Quest q = quests.get(questId);
        if (q != null && objective != null && !objective.isEmpty()) {
            q.objectives.add(objective);
        }
    }

    @Override public void completeQuest(String questId) {
        Quest q = quests.get(questId);
        if (q != null) q.status = QuestStatus.COMPLETED;
    }

    public boolean questActive(String questId) {
        Quest q = quests.get(questId);
        return q != null && q.status == QuestStatus.ACTIVE;
    }

    public boolean questCompleted(String questId) {
        Quest q = quests.get(questId);
        return q != null && q.status == QuestStatus.COMPLETED;
    }

    public boolean hasQuest(String questId) { return quests.containsKey(questId); }

    // ------------------------------------------------------------ JournalView

    @Override public List<QuestEntryView> quests() {
        List<QuestEntryView> out = new ArrayList<QuestEntryView>();
        for (Quest q : quests.values()) out.add(q);
        return Collections.unmodifiableList(out);
    }

    /** One quest log entry, also its own read-only view. */
    private static final class Quest implements QuestEntryView {
        private final String title;
        private QuestStatus status = QuestStatus.ACTIVE;
        private final List<String> objectives = new ArrayList<String>();

        private Quest(String title) { this.title = title; }

        @Override public String title() { return title; }
        @Override public QuestStatus status() { return status; }
        @Override public List<String> objectives() {
            return Collections.unmodifiableList(new ArrayList<String>(objectives));
        }
    }
}
