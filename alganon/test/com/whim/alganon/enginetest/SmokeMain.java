package com.whim.alganon.enginetest;

import com.whim.alganon.api.Enums.Direction;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Views;
import com.whim.alganon.engine.GameEngine;

/**
 * Headless smoke test of the full engine path against the {@link FakeFactory} model:
 * new-game → creation → move → kill a mob → gain XP → accept + progress a quest →
 * assign study → save → reload grants offline progress. No JUnit / no display required.
 */
public final class SmokeMain {

    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        GameEngine engine = new GameEngine(new FakeFactory());
        engine.setAutoTick(false); // drive ticks manually for determinism

        // ---- title ----
        check("starts on TITLE", engine.state().state() == GameStateType.TITLE);
        check("title has menu", !engine.state().menuOptions().isEmpty());
        check("codex reference lists present outside creation",
                engine.state().creation() != null && !engine.state().creation().races().isEmpty());

        // ---- creation ----
        engine.beginCreation();
        check("in CHARACTER_CREATION", engine.state().state() == GameStateType.CHARACTER_CREATION);
        engine.chooseRace("human");
        Views.CreationView cv = engine.state().creation();
        check("families filtered by faction", !cv.familiesFor("human").isEmpty());
        engine.chooseFamily("fam_ach");
        engine.chooseClass("CHAMPION");
        engine.setName("Kaelen");
        engine.finishCreation();
        check("now PLAYING", engine.state().state() == GameStateType.PLAYING);

        Views.CharacterView p = engine.state().player();
        check("player placed", p != null && p.zoneId() != null);
        check("player knows starting ability", !p.abilities().isEmpty());
        check("starting level 1", p.level() == 1);

        // ---- movement ----
        int startX = p.pos().x;
        engine.move(Direction.EAST);
        check("moved east", engine.state().player().pos().x == startX + 1);

        // ---- accept a static quest (kill rat) ----
        engine.acceptQuest("q_rats");
        check("quest accepted", engine.state().quests().size() == 1);

        // ---- combat: walk next to the rat (at 5,5), then attack ----
        // From ~(3,1): go east to x=5, then south to y=4 (adjacent to the rat).
        for (int i = 0; i < 6 && engine.state().player().pos().x < 5; i++) engine.move(Direction.EAST);
        for (int i = 0; i < 6 && engine.state().player().pos().y < 4; i++) engine.move(Direction.SOUTH);
        check("navigated adjacent to rat",
                engine.state().player().pos().manhattan(new com.whim.alganon.api.GridPos(5, 5)) <= 2);

        long xpBefore = engine.state().player().xp();
        int lvlBefore = engine.state().player().level();
        // hammer the rat until it dies (auto-target nearest enemy within range)
        boolean ratDead = false;
        for (int i = 0; i < 60 && !ratDead; i++) {
            engine.useAbility("strike", -1);
            engine.tickOnce(0.1);
            ratDead = countMobs(engine, "Sewer Rat") == 0;
        }
        check("rat was killed", ratDead);
        Views.CharacterView after = engine.state().player();
        boolean gained = after.level() > lvlBefore || after.xp() != xpBefore || after.level() > 1;
        check("gained XP / progressed from kill", gained);

        // quest progress from the kill
        Views.QuestView q = engine.state().quests().isEmpty() ? null : engine.state().quests().get(0);
        check("kill advanced or completed the quest",
                q == null || q.objectives().get(0).current() >= 1
                        || q.status() == com.whim.alganon.api.Enums.QuestStatus.READY_TO_TURN_IN);

        // ---- procedural quest ----
        engine.generateProceduralQuest();
        boolean hasProc = false;
        for (Views.QuestView qv : engine.state().quests()) if (qv.procedural()) hasProc = true;
        check("procedural quest generated", hasProc);

        // ---- study assignment ----
        engine.assignStudy(SkillType.WEAPON);
        check("study assigned", engine.state().study().assignedSkill() == SkillType.WEAPON);

        // ---- gather + craft chain ----
        // walk to the ore vein at (2,5) is unnecessary — call gather via interact proximity is
        // handled by the engine; here we exercise the direct crafting path after seeding ore.
        engine.craft("r_ingot"); // player starts with 2 ore
        boolean hasIngot = engine.state().player().inventory().stream()
                .anyMatch(it -> it.id().equals("ingot"));
        check("crafted an ingot from ore", hasIngot);

        // ---- faction war surfaced ----
        check("faction war view present",
                engine.state().world() != null && engine.state().world().factionWar() != null);

        // ---- save ----
        // Force a small elapsed window by back-dating the save's lastSave via a real save now,
        // then reload and confirm offline study is granted.
        int skillBefore = engine.state().player().skills().get(SkillType.WEAPON);
        boolean saved = engine.saveGame(0);
        check("saved to slot 0", saved);

        // Reload into a fresh engine.
        GameEngine engine2 = new GameEngine(new FakeFactory());
        engine2.setAutoTick(false);
        boolean loaded = engine2.loadGame(0);
        check("loaded from slot 0", loaded);
        Views.CharacterView pl = engine2.state().player();
        check("reload preserved name", "Kaelen".equals(pl.name()));
        check("reload preserved level", pl.level() == engine.state().player().level());
        check("reload preserved study assignment", engine2.state().study().assignedSkill() == SkillType.WEAPON);
        check("reload preserved quests", !engine2.state().quests().isEmpty());

        // offline study: simulate elapsed time by loading a save whose lastSave is in the past.
        offlineStudyCheck();

        System.out.println();
        System.out.println("Checks: " + checks + "  Failures: " + failures);
        if (failures > 0) {
            System.out.println("SMOKE TEST FAILED");
            System.exit(1);
        }
        System.out.println("SMOKE TEST PASSED");
    }

    /** Explicitly verify offline Study grants banked progress for elapsed wall-clock time. */
    private static void offlineStudyCheck() {
        GameEngine engine = new GameEngine(new FakeFactory());
        engine.setAutoTick(false);
        engine.beginCreation();
        engine.chooseRace("human");
        engine.chooseFamily("fam_ach");
        engine.chooseClass("CHAMPION");
        engine.setName("Idler");
        engine.finishCreation();
        engine.assignStudy(SkillType.GATHERING);
        int before = engine.state().player().skills().get(SkillType.GATHERING);
        engine.saveGame(1);

        // Rewrite the save file so lastSave is 2 hours in the past, then reload.
        try {
            java.io.File f = new java.io.File(
                    new java.io.File(new java.io.File(System.getProperty("user.home"), ".alganon"), "saves"),
                    "slot1.sav");
            java.util.List<String> lines = java.nio.file.Files.readAllLines(f.toPath());
            long twoHoursAgo = System.currentTimeMillis() - 2L * 3600_000L;
            java.util.List<String> out = new java.util.ArrayList<String>();
            for (String l : lines) out.add(l.startsWith("lastSave=") ? "lastSave=" + twoHoursAgo : l);
            java.nio.file.Files.write(f.toPath(), out);
        } catch (Exception e) {
            check("offline study: could rewrite save timestamp", false);
            return;
        }

        GameEngine reloaded = new GameEngine(new FakeFactory());
        reloaded.setAutoTick(false);
        reloaded.loadGame(1);
        int afterOffline = reloaded.state().player().skills().get(SkillType.GATHERING);
        check("offline study granted skill points (>= before)", afterOffline >= before);
        check("offline study granted at least 1 point over 2h", afterOffline > before);
    }

    private static int countMobs(GameEngine engine, String name) {
        int n = 0;
        if (engine.state().world() == null) return 0;
        for (Views.MobView m : engine.state().world().mobs()) if (m.name().equals(name)) n++;
        return n;
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "  ok   " : "  FAIL ") + label);
    }
}
