package com.whim.albion.engine.dev;

import com.whim.albion.api.ActionResult;
import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.GameStateType;
import com.whim.albion.api.Enums.QuestStatus;
import com.whim.albion.api.Views.CombatView;
import com.whim.albion.api.Views.CombatantView;
import com.whim.albion.api.Views.GameStateView;
import com.whim.albion.api.Views.QuestEntryView;
import com.whim.albion.engine.GameEngine;

import java.util.List;

/**
 * Headless smoke test wiring the engine to the in-package {@link FakeModelFactory},
 * exercising the full loop required by the contract:
 * <em>new game → move → trigger combat → win → dialogue → save → load</em>.
 *
 * <p>Run: {@code java -cp target/classes com.whim.albion.engine.dev.SmokeTest}.
 * Prints PASS/FAIL per step and exits non-zero on any failure.</p>
 */
public final class SmokeTest {

    private static int failures = 0;

    public static void main(String[] args) {
        GameEngine engine = new GameEngine(new FakeModelFactory());
        final int[] changes = {0};
        engine.addChangeListener(new com.whim.albion.api.GameController.ChangeListener() {
            @Override public void onStateChanged() { changes[0]++; }
        });

        // 1) New game -> OVERWORLD
        engine.newGame(42L);
        check("new game -> OVERWORLD", engine.state().current() == GameStateType.OVERWORLD);
        check("change listener fired on new game", changes[0] > 0);
        check("party has 3 members", engine.state().party().members().size() == 3);
        int startGold = engine.state().gold();

        // 2) Move south onto the ambush cell -> COMBAT
        ActionResult mv = engine.move(Direction.SOUTH);
        check("move south succeeded", mv.isSuccess());
        check("stepping on encounter -> COMBAT", engine.state().current() == GameStateType.COMBAT);

        // 3) Fight until resolved
        boolean won = runCombat(engine);
        check("combat resolved with victory", won);
        check("back to OVERWORLD after victory", engine.state().current() == GameStateType.OVERWORLD);
        check("gold increased from loot", engine.state().gold() > startGold);

        // 4) Walk to the Elder and talk -> DIALOGUE
        engine.move(Direction.NORTH);                 // (1,2) -> (1,1)
        engine.move(Direction.EAST);                  // blocked by Elder, now facing EAST
        ActionResult talk = engine.interact();
        check("interact opened dialogue", engine.state().current() == GameStateType.DIALOGUE);
        check("dialogue has options", engine.state().dialogue().options().size() >= 1);

        // 5) Accept the quest, then acknowledge the farewell node
        engine.selectDialogueOption(0);
        if (engine.state().current() == GameStateType.DIALOGUE) engine.selectDialogueOption(0);
        check("dialogue ended -> OVERWORLD", engine.state().current() == GameStateType.OVERWORLD);
        check("quest started via dialogue", hasActiveQuest(engine));
        check("received dungeon key from dialogue", partyHasItem(engine, "dungeon_key"));

        // 6) Save
        boolean saved = engine.saveGame(0);
        check("save to slot 0", saved);
        check("save slot label populated", engine.saveSlots().get(0).contains("gold"));
        int savedGold = engine.state().gold();

        // 7) Mutate, then load -> state restored
        engine.useItem(0, "potion_heal");             // harmless mutation
        boolean loaded = engine.loadGame(0);
        check("load slot 0", loaded);
        check("after load -> OVERWORLD", engine.state().current() == GameStateType.OVERWORLD);
        check("gold restored on load", engine.state().gold() == savedGold);
        check("quest restored on load", hasActiveQuest(engine));

        System.out.println();
        if (failures == 0) System.out.println("ALL SMOKE CHECKS PASSED");
        else { System.out.println(failures + " CHECK(S) FAILED"); System.exit(1); }
    }

    private static boolean runCombat(GameEngine engine) {
        int guard = 0;
        while (engine.state().current() == GameStateType.COMBAT && guard++ < 300) {
            CombatView cv = engine.state().combat();
            if (cv == null || cv.finished()) break;
            List<CombatantView> cs = cv.combatants();
            int idx = cv.currentTurnIndex();
            if (idx < 0 || idx >= cs.size() || !cs.get(idx).playerSide()) break;
            int enemy = -1;
            for (int i = 0; i < cs.size(); i++)
                if (!cs.get(i).playerSide() && cs.get(i).alive()) { enemy = i; break; }
            if (enemy < 0) break;
            // Try attack; fall back to a damage spell, a heal, then defend.
            ActionResult r = engine.combatAction(CombatActionType.ATTACK, enemy, null);
            if (!r.isSuccess()) r = engine.combatAction(CombatActionType.CAST, enemy, "bolt");
            if (!r.isSuccess()) r = engine.combatAction(CombatActionType.CAST, enemy, "mend");
            if (!r.isSuccess()) engine.combatAction(CombatActionType.DEFEND, -1, null);
        }
        // Victory is reflected by returning to exploration with the party alive.
        GameStateView s = engine.state();
        return s.current() == GameStateType.OVERWORLD && !s.party().members().isEmpty();
    }

    private static boolean partyHasItem(GameEngine engine, String itemId) {
        List<com.whim.albion.api.Views.CharacterView> ms = engine.state().party().members();
        for (int i = 0; i < ms.size(); i++) {
            List<com.whim.albion.api.Views.ItemView> inv = ms.get(i).inventory();
            for (int j = 0; j < inv.size(); j++) if (itemId.equals(inv.get(j).id())) return true;
        }
        return false;
    }

    private static boolean hasActiveQuest(GameEngine engine) {
        List<QuestEntryView> qs = engine.state().journal().quests();
        for (int i = 0; i < qs.size(); i++)
            if (qs.get(i).status() == QuestStatus.ACTIVE || qs.get(i).status() == QuestStatus.COMPLETED) return true;
        return false;
    }

    private static void check(String label, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + label);
        if (!ok) failures++;
    }
}
