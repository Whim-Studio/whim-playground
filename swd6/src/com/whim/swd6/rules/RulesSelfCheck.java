package com.whim.swd6.rules;

import com.whim.swd6.api.Attribute;
import com.whim.swd6.api.CreationRules;
import com.whim.swd6.api.DiceCode;
import com.whim.swd6.api.Equipment;
import com.whim.swd6.api.PlayerCharacter;
import com.whim.swd6.api.Scenario;
import com.whim.swd6.api.SkillDef;
import com.whim.swd6.api.Skill;
import com.whim.swd6.api.Template;

import com.whim.swd6.persistence.JsonCharacterRepository;

import java.io.File;
import java.util.List;

/**
 * Standalone smoke test for the rules + persistence layers. Instantiates all
 * content, asserts every template's attributes sum to 54 pips (18D), prints the
 * skill count and scenario scene count, and round-trips a character through the
 * JSON repository comparing a few fields.
 *
 * Run: {@code java com.whim.swd6.rules.RulesSelfCheck}
 */
public final class RulesSelfCheck {

    private RulesSelfCheck() {
    }

    public static void main(String[] args) throws Exception {
        GameContent content = new GameContent();
        int failures = 0;

        System.out.println("=== SWD6 Rules Self-Check ===");

        // --- Skill catalog ---
        List<SkillDef> skills = content.skillCatalog();
        System.out.println("Skill catalog size: " + skills.size());

        // --- Templates: attribute totals must equal 54 pips ---
        System.out.println();
        System.out.println("Template attribute totals (pips; must be "
                + CreationRules.ATTRIBUTE_PIPS_TOTAL + "):");
        List<Template> templates = content.templates();
        for (Template t : templates) {
            int pips = CreationRules.totalAttributePips(t.getAttributes());
            boolean ok = pips == CreationRules.ATTRIBUTE_PIPS_TOTAL;
            boolean ranges = true;
            for (Attribute a : Attribute.values()) {
                if (!CreationRules.attributeInRange(t.getAttributes().get(a))) {
                    ranges = false;
                }
            }
            if (!ok || !ranges) {
                failures++;
            }
            System.out.println(String.format("  %-22s %2dD (%2d pips)  %s%s",
                    t.getName(), pips / 3, pips,
                    ok ? "OK" : "FAIL(total)",
                    ranges ? "" : " FAIL(range)"));
        }

        // --- Scenario ---
        System.out.println();
        Scenario sc = content.scenario();
        System.out.println("Scenario: \"" + sc.getTitle() + "\"");
        System.out.println("  scenes: " + sc.getScenes().size()
                + ", startSceneId: " + sc.getStartSceneId());
        int skillChecks = 0;
        int combats = 0;
        int decisions = 0;
        int endings = 0;
        for (Scenario.Scene s : sc.getScenes()) {
            switch (s.getType()) {
                case SKILL_CHECK: skillChecks++; break;
                case COMBAT: combats++; break;
                case DECISION: decisions++; break;
                case ENDING: endings++; break;
                default: break;
            }
        }
        System.out.println("  skillChecks=" + skillChecks + " combats=" + combats
                + " decisions=" + decisions + " endings=" + endings);
        if (sc.sceneById(sc.getStartSceneId()) == null) {
            System.out.println("  FAIL: startSceneId points to no scene");
            failures++;
        }
        // Verify every wired next-id resolves to a real scene.
        for (Scenario.Scene s : sc.getScenes()) {
            failures += checkRef(sc, s.getSuccessNext());
            failures += checkRef(sc, s.getFailureNext());
            failures += checkRef(sc, s.getVictoryNext());
            failures += checkRef(sc, s.getDefeatNext());
            for (Scenario.Choice ch : s.getChoices()) {
                failures += checkRef(sc, ch.getNextSceneId());
            }
        }
        if (skillChecks < 1 || combats < 1 || decisions < 1 || endings < 1) {
            System.out.println("  FAIL: scenario missing a required scene type");
            failures++;
        }

        // --- instantiate ---
        System.out.println();
        Template smuggler = templates.get(0);
        PlayerCharacter pc = content.instantiate(smuggler);
        pc.setName("Renna Vos");
        pc.getGear().add(new Equipment("Lucky Chit", 1, 0, "Never spent it"));
        System.out.println("Instantiated: " + pc.getName() + " (" + pc.getTemplateName() + ")");
        System.out.println("  skills seeded: " + pc.getSkills().size()
                + ", weapons: " + pc.getWeapons().size()
                + ", gear: " + pc.getGear().size()
                + ", credits: " + pc.getCredits()
                + ", FP/CP: " + pc.getForcePoints() + "/" + pc.getCharacterPoints());
        if (pc.getForcePoints() != CreationRules.STARTING_FORCE_POINTS
                || pc.getCharacterPoints() != CreationRules.STARTING_CHARACTER_POINTS) {
            System.out.println("  FAIL: starting points not seeded from CreationRules");
            failures++;
        }

        // Give one skill some dice + a specialization so persistence exercises them.
        if (!pc.getSkills().isEmpty()) {
            Skill first = pc.getSkills().get(0);
            first.setAdded(DiceCode.parse("2D"));
        }

        // --- Repository round-trip ---
        System.out.println();
        JsonCharacterRepository repo = new JsonCharacterRepository();
        File tmp = File.createTempFile("swd6-selfcheck-", ".json");
        tmp.deleteOnExit();
        repo.save(pc, tmp);
        System.out.println("Saved to: " + tmp);
        PlayerCharacter loaded = repo.load(tmp);

        failures += expect("name", pc.getName(), loaded.getName());
        failures += expect("templateName", pc.getTemplateName(), loaded.getTemplateName());
        failures += expect("credits", String.valueOf(pc.getCredits()), String.valueOf(loaded.getCredits()));
        failures += expect("move", String.valueOf(pc.getMove()), String.valueOf(loaded.getMove()));
        failures += expect("skillCount", String.valueOf(pc.getSkills().size()),
                String.valueOf(loaded.getSkills().size()));
        failures += expect("DEX",
                pc.getAttribute(Attribute.DEXTERITY).toString(),
                loaded.getAttribute(Attribute.DEXTERITY).toString());
        if (!pc.getSkills().isEmpty() && !loaded.getSkills().isEmpty()) {
            failures += expect("skill0.added",
                    pc.getSkills().get(0).getAdded().toString(),
                    loaded.getSkills().get(0).getAdded().toString());
        }
        failures += expect("weaponCount", String.valueOf(pc.getWeapons().size()),
                String.valueOf(loaded.getWeapons().size()));

        // --- Force-sensitive template also round-trips its force skills ---
        PlayerCharacter adept = content.instantiate(content.templates().get(3));
        File tmp2 = File.createTempFile("swd6-adept-", ".json");
        tmp2.deleteOnExit();
        repo.save(adept, tmp2);
        PlayerCharacter adeptLoaded = repo.load(tmp2);
        failures += expect("forceSensitive", String.valueOf(adept.isForceSensitive()),
                String.valueOf(adeptLoaded.isForceSensitive()));

        System.out.println();
        System.out.println("listSaved() default dir: " + repo.defaultDirectory());

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL CHECKS PASSED");
        } else {
            System.out.println(failures + " CHECK(S) FAILED");
            System.exit(1);
        }
    }

    private static int checkRef(Scenario sc, String id) {
        if (id == null || id.isEmpty()) {
            return 0;
        }
        if (sc.sceneById(id) == null) {
            System.out.println("  FAIL: dangling scene reference '" + id + "'");
            return 1;
        }
        return 0;
    }

    private static int expect(String field, String expected, String actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        System.out.println(String.format("  round-trip %-14s expected=%-12s actual=%-12s %s",
                field, expected, actual, ok ? "OK" : "FAIL"));
        return ok ? 0 : 1;
    }
}
