package com.whim.swd6.ui;

import com.whim.swd6.api.CombatTracker;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.util.function.Supplier;

/**
 * Standalone demo entry point for the UI. Wires the dev-only stub fakes
 * ({@link StubContent}, {@link StubEngine}, {@link StubCombatTracker},
 * {@link StubRepository}) into {@link MainFrame} so the whole interface can be
 * explored without Tasks 1 and 2. At runtime, Main injects the real
 * implementations instead.
 *
 * Owned by Task 3 (ui).
 */
public final class UiPreview {

    private UiPreview() {
    }

    public static void main(String[] args) {
        // Headless guard: still construct everything (a smoke check) but skip setVisible.
        final boolean headless = GraphicsEnvironment.isHeadless();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {
            // fall back to default L&F
        }

        Runnable launch = new Runnable() {
            @Override
            public void run() {
                StubContent content = new StubContent();
                StubEngine engine = new StubEngine();
                StubRepository repo = new StubRepository();
                Supplier<CombatTracker> supplier = new Supplier<CombatTracker>() {
                    @Override public CombatTracker get() { return new StubCombatTracker(); }
                };
                MainFrame frame = new MainFrame(content, engine, repo, supplier);
                frame.setLocationRelativeTo(null);
                if (!headless) {
                    frame.setVisible(true);
                }
            }
        };

        if (headless) {
            // Swing frames cannot be built without a display; run a logic smoke test
            // of the stub services instead so the standalone wiring is still verified.
            System.out.println("[UiPreview] Headless environment — running stub smoke test (no display).");
            smokeTest();
        } else {
            SwingUtilities.invokeLater(launch);
        }
    }

    /** Exercise the dev stubs end-to-end without any Swing components. */
    private static void smokeTest() {
        StubContent content = new StubContent();
        StubEngine engine = new StubEngine(42L);
        StubRepository repo = new StubRepository();

        System.out.println("  templates: " + content.templates().size()
                + ", skills: " + content.skillCatalog().size()
                + ", weapons: " + content.weapons().size());

        com.whim.swd6.api.PlayerCharacter pc =
                content.instantiate(content.templates().get(0));
        pc.setName("Preview Pilot");
        com.whim.swd6.api.RollResult r =
                engine.roll(com.whim.swd6.api.DiceCode.parse("4D+2"), true, 12);
        System.out.println("  sample roll 4D+2 vs 12: total=" + r.getTotal()
                + " success=" + r.isSuccess()
                + " complication=" + r.isComplication()
                + " wildExploded=" + r.isWildExploded());

        com.whim.swd6.api.DamageResult dr =
                engine.resolveDamage(com.whim.swd6.api.DiceCode.parse("5D"),
                        com.whim.swd6.api.DiceCode.parse("2D"));
        System.out.println("  sample damage 5D vs 2D: margin=" + dr.getMargin()
                + " → " + dr.getInflicted().display());

        StubCombatTracker tracker = new StubCombatTracker();
        com.whim.swd6.api.Combatant hero = new com.whim.swd6.api.Combatant();
        hero.setName("Hero");
        hero.setPlayerCharacter(true);
        hero.setPc(pc);
        tracker.add(hero);
        for (com.whim.swd6.api.Combatant e : content.scenario()
                .sceneById("ambush").getEnemies()) {
            com.whim.swd6.api.Combatant c = new com.whim.swd6.api.Combatant();
            c.setName(e.getName());
            c.setResistCode(e.getResistCode());
            tracker.add(c);
        }
        tracker.rollInitiative();
        System.out.println("  combat order size: " + tracker.order().size()
                + ", round " + tracker.round() + ", over=" + tracker.isOver());

        try {
            java.io.File f = new java.io.File(repo.defaultDirectory(), "preview.chr");
            repo.save(pc, f);
            com.whim.swd6.api.PlayerCharacter back = repo.load(f);
            System.out.println("  save/load round-trip name: '" + back.getName()
                    + "', skills=" + back.getSkills().size());
            f.delete();
        } catch (java.io.IOException ex) {
            System.out.println("  repository round-trip failed: " + ex.getMessage());
        }

        System.out.println("  scenario: '" + content.scenario().getTitle()
                + "' scenes=" + content.scenario().getScenes().size());
        System.out.println("[UiPreview] Stub smoke test complete — UI wiring is sound.");
    }
}
