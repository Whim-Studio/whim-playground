package com.whim.alganon.quest;

import com.whim.alganon.api.ActionResult;
import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs.ObjectiveDef;
import com.whim.alganon.api.Defs.QuestDef;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ObjectiveType;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.GameContext;
import com.whim.alganon.api.GameModel;
import com.whim.alganon.combat.Progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static + procedural quest log. Owns per-quest progress (the model does not), wires
 * kill/gather/talk/travel progress hooks, and defers procedural generation to
 * {@link Content#generateQuest}. Quest state is persisted by the engine's save system.
 */
public final class QuestSystem {

    private final Content content;
    private final GameContext ctx;
    private final Map<String, QuestRun> runs = new LinkedHashMap<String, QuestRun>();

    public QuestSystem(Content content, GameContext ctx) {
        this.content = content;
        this.ctx = ctx;
    }

    public List<QuestRun> runs() {
        return new ArrayList<QuestRun>(runs.values());
    }

    public QuestRun get(String id) { return runs.get(id); }

    public void reset() { runs.clear(); }

    // ---------- accept / turn-in ----------

    public ActionResult accept(GameModel model, String questId) {
        if (runs.containsKey(questId)) return ActionResult.fail("You already have that quest.");
        QuestDef def = content.quest(questId);
        if (def == null) return ActionResult.fail("No such quest.");
        if (model.player().level() < def.levelReq) {
            return ActionResult.fail(def.name + " requires level " + def.levelReq + ".");
        }
        runs.put(def.id, new QuestRun(def, QuestStatus.ACTIVE));
        ctx.log(ChatChannel.SYSTEM, "Quest accepted: " + def.name + ".");
        return ActionResult.ok("Accepted: " + def.name);
    }

    /** Accept a fully-formed (possibly procedural) quest def. */
    public ActionResult acceptDef(QuestDef def) {
        if (runs.containsKey(def.id)) return ActionResult.fail("You already have that quest.");
        runs.put(def.id, new QuestRun(def, QuestStatus.ACTIVE));
        ctx.log(ChatChannel.SYSTEM, "Quest accepted: " + def.name + ".");
        return ActionResult.ok("Accepted: " + def.name);
    }

    public ActionResult turnIn(GameModel model, String questId) {
        QuestRun run = runs.get(questId);
        if (run == null) return ActionResult.fail("You are not on that quest.");
        if (run.status != QuestStatus.READY_TO_TURN_IN) {
            return ActionResult.fail("Objectives are not yet complete.");
        }
        CharacterModel p = model.player();
        QuestDef def = run.def;
        Progression.grantXp(p, def.xpReward, ctx, content);
        if (def.goldReward > 0) {
            p.addGold(def.goldReward);
            ctx.log(ChatChannel.LOOT, "Received " + def.goldReward + " gold.");
        }
        for (String itemId : def.rewardItemIds) {
            p.addItem(itemId, 1);
            ctx.log(ChatChannel.LOOT, "Received reward item.");
        }
        run.status = QuestStatus.COMPLETED;
        runs.remove(questId);
        ctx.log(ChatChannel.SYSTEM, "Quest complete: " + def.name + "!");
        return ActionResult.ok("Completed: " + def.name);
    }

    // ---------- progress hooks ----------

    public void onKill(String mobDefId) { progress(ObjectiveType.KILL, mobDefId, 1); }
    public void onGather(String itemId, int qty) { progress(ObjectiveType.GATHER, itemId, qty); }
    public void onTalk(String npcId) { progress(ObjectiveType.TALK, npcId, 1); }
    public void onTravel(String zoneId) { progress(ObjectiveType.TRAVEL, zoneId, 1); }

    private void progress(ObjectiveType type, String targetId, int amount) {
        for (QuestRun run : runs.values()) {
            boolean changed = run.advance(type, targetId, amount);
            if (changed) {
                if (run.status == QuestStatus.READY_TO_TURN_IN) {
                    ctx.log(ChatChannel.SYSTEM, "Quest ready to turn in: " + run.def.name + ".");
                } else {
                    ctx.log(ChatChannel.SYSTEM, progressLine(run));
                }
            }
        }
    }

    private String progressLine(QuestRun run) {
        StringBuilder sb = new StringBuilder(run.def.name).append(": ");
        for (int i = 0; i < run.def.objectives.size(); i++) {
            ObjectiveDef o = run.def.objectives.get(i);
            if (i > 0) sb.append(", ");
            sb.append(run.progress[i]).append('/').append(o.count);
        }
        return sb.toString();
    }

    // ---------- procedural generation ----------

    public ActionResult generate(GameModel model, FamilyArchetype archetype) {
        CharacterModel p = model.player();
        QuestDef def = content.generateQuest(p.level(), archetype, ctx.rng());
        if (def == null) return ActionResult.fail("No procedural quest available.");
        return acceptDef(def);
    }
}
