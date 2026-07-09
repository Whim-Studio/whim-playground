package com.whim.alganon.quest;

import com.whim.alganon.api.Defs.ObjectiveDef;
import com.whim.alganon.api.Defs.QuestDef;
import com.whim.alganon.api.Enums.QuestStatus;

/**
 * Mutable engine-side progress for one accepted quest. The model does not store quest
 * state, so the {@link QuestSystem} owns these and persistence serializes them.
 */
public final class QuestRun {
    public final QuestDef def;
    public final int[] progress;    // parallel to def.objectives
    public QuestStatus status;

    public QuestRun(QuestDef def, QuestStatus status) {
        this.def = def;
        this.status = status;
        this.progress = new int[def.objectives.size()];
    }

    public boolean allObjectivesDone() {
        for (int i = 0; i < def.objectives.size(); i++) {
            if (progress[i] < def.objectives.get(i).count) return false;
        }
        return true;
    }

    /** Advance objectives whose type/target match; returns true if anything changed. */
    public boolean advance(com.whim.alganon.api.Enums.ObjectiveType type, String targetId, int amount) {
        if (status != QuestStatus.ACTIVE) return false;
        boolean changed = false;
        for (int i = 0; i < def.objectives.size(); i++) {
            ObjectiveDef o = def.objectives.get(i);
            if (o.type == type && matches(o.targetId, targetId) && progress[i] < o.count) {
                progress[i] = Math.min(o.count, progress[i] + amount);
                changed = true;
            }
        }
        if (changed && allObjectivesDone()) status = QuestStatus.READY_TO_TURN_IN;
        return changed;
    }

    private static boolean matches(String objTarget, String eventTarget) {
        // A blank/null objective target matches any event of that type (e.g. "kill anything").
        return objTarget == null || objTarget.isEmpty() || objTarget.equals(eventTarget);
    }
}
