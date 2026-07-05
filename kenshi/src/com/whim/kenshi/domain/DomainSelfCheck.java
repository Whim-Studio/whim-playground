package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.BodyPart;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.SkillType;
import com.whim.kenshi.api.Enums.WeaponClass;

import java.util.List;

/**
 * Headless proof of the domain model. Builds a world from a fixed seed, prints a
 * population summary, then walks one character through a sequence of injuries to
 * demonstrate the anatomy → moveState/effectiveWeapon derivations (disabled
 * parts → crawl → downed → dead), plus a quick skill-XP and faction check.
 *
 * <p>Run: {@code java com.whim.kenshi.domain.DomainSelfCheck}
 */
public final class DomainSelfCheck {

    public static void main(String[] args) {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        System.out.println("=== Kenshi Domain Self-Check (seed=" + seed + ") ===");

        WorldState world = WorldBuilder.build(seed);
        printWorldSummary(world);

        System.out.println();
        System.out.println("--- Anatomy / MoveState transitions ---");
        Character c = world.character("player_0");
        report(c, "start");

        // Disable one leg — still upright.
        c.anatomy().damage(BodyPart.LEFT_LEG, 120.0);
        report(c, "left leg disabled");

        // Disable the other leg — now crawling.
        c.anatomy().damage(BodyPart.RIGHT_LEG, 120.0);
        report(c, "both legs disabled -> CRAWLING");

        // Disable an arm — two-handed degrades to unarmed.
        Character hero2 = world.character("player_1"); // wields TWO_HANDED
        System.out.println();
        System.out.println("player_1 weapon before arm loss: " + hero2.weapon()
                + " / effective: " + hero2.effectiveWeapon());
        hero2.anatomy().damage(BodyPart.RIGHT_ARM, 130.0);
        System.out.println("player_1 after right arm disabled -> effective: "
                + hero2.effectiveWeapon() + " (arm down=" + hero2.anatomy().anyArmDown() + ")");

        // Stomach disabled -> DOWNED.
        System.out.println();
        c.anatomy().damage(BodyPart.STOMACH, 105.0);
        report(c, "stomach disabled -> DOWNED");

        // Blood loss alone also downs a fresh character.
        Character bleeder = world.character("player_1");
        bleeder.setBlood(Config.BLOOD_UNCONSCIOUS_AT - 1.0);
        report(bleeder, "player_1 blood below threshold -> DOWNED");

        // Chest to the floor -> DEAD.
        c.anatomy().damage(BodyPart.CHEST, 999.0);
        report(c, "chest to floor -> DEAD");

        System.out.println();
        System.out.println("--- Skills ---");
        Character sk = world.character("player_2");
        int before = sk.skills().level(SkillType.MELEE_ATTACK);
        sk.skills().addXp(SkillType.MELEE_ATTACK, 500.0);
        System.out.println("player_2 MELEE_ATTACK " + before + " -> "
                + sk.skills().level(SkillType.MELEE_ATTACK) + " after 500 XP");

        System.out.println();
        System.out.println("--- Factions ---");
        FactionMatrix fm = world.factions();
        System.out.println("Holy Nation vs Shek hostile? "
                + fm.isHostile(FactionId.HOLY_NATION, FactionId.SHEK));
        System.out.println("Player vs Dust Bandits hostile? "
                + fm.isHostile(FactionId.PLAYER, FactionId.DUST_BANDITS));
        System.out.println("Player rep with Trade Guild: "
                + fm.reputationWithPlayer(FactionId.TRADE_GUILD));

        System.out.println();
        boolean ok = verify(world);
        System.out.println(ok ? "SELF-CHECK PASSED" : "SELF-CHECK FAILED");
        if (!ok) System.exit(1);
    }

    private static void report(Character c, String label) {
        StringBuilder parts = new StringBuilder();
        for (BodyPart p : BodyPart.values()) {
            if (c.anatomy().disabled(p)) {
                if (parts.length() > 0) parts.append(", ");
                parts.append(p.label());
            }
        }
        System.out.printf("[%-32s] move=%-8s alive=%-5s downed=%-5s blood=%5.1f disabled=[%s]%n",
                label, c.moveState(), c.alive(), c.isDowned(), c.blood(), parts.toString());
    }

    private static void printWorldSummary(WorldState world) {
        List<Character> chars = world.charactersList();
        int player = 0, bandit = 0, guard = 0, drifter = 0;
        for (Character c : chars) {
            switch (c.faction()) {
                case PLAYER: player++; break;
                case DUST_BANDITS:
                case HUNGRY_BANDITS: bandit++; break;
                case DRIFTERS: drifter++; break;
                default: guard++; break;
            }
        }
        System.out.println("Characters: " + chars.size() + " (player=" + player
                + ", guards=" + guard + ", bandits=" + bandit + ", drifters=" + drifter + ")");
        System.out.println("Squads: " + world.squadsList().size()
                + ", Nodes: " + world.nodesList().size());
        System.out.println("Map: " + world.map().tiles() + "x" + world.map().tiles()
                + " tiles, tileSize=" + world.map().tileSize());
    }

    /** Assert the key invariants the derivations promise. */
    private static boolean verify(WorldState world) {
        boolean ok = true;

        Anatomy a = new Anatomy();
        Character t = new Character("t", "T", FactionId.PLAYER, 0, 0);
        t.setWeapon(WeaponClass.TWO_HANDED);

        // fresh character is IDLE
        if (t.moveState() != MoveState.IDLE) { fail("fresh char not IDLE"); ok = false; }

        // both legs -> CRAWLING
        t.anatomy().damage(BodyPart.LEFT_LEG, 200);
        t.anatomy().damage(BodyPart.RIGHT_LEG, 200);
        if (t.moveState() != MoveState.CRAWLING) { fail("both legs not CRAWLING"); ok = false; }

        // arm -> two-handed degrades to unarmed
        t.anatomy().damage(BodyPart.LEFT_ARM, 200);
        if (t.effectiveWeapon() != WeaponClass.UNARMED) { fail("arm-down weapon not degraded"); ok = false; }

        // stomach disabled (hp<=0 but above the -max floor) -> DOWNED
        t.anatomy().damage(BodyPart.STOMACH, 100);
        if (t.moveState() != MoveState.DOWNED) { fail("stomach not DOWNED"); ok = false; }

        // head floor -> DEAD (overrides downed)
        t.anatomy().damage(BodyPart.HEAD, 500);
        if (!t.isDead() || t.moveState() != MoveState.DEAD) { fail("head floor not DEAD"); ok = false; }

        // part floor is -max
        Anatomy a2 = new Anatomy();
        a2.damage(BodyPart.HEAD, 100000);
        if (a2.hp(BodyPart.HEAD) != -a2.max(BodyPart.HEAD)) { fail("part floor != -max"); ok = false; }

        // heal clamps to max
        a2.heal(BodyPart.HEAD, 100000);
        if (a2.hp(BodyPart.HEAD) != a2.max(BodyPart.HEAD)) { fail("heal not clamped to max"); ok = false; }

        // skill grind promotes and caps
        Skills sk = new Skills();
        sk.addXp(SkillType.STRENGTH, 1_000_000);
        if (sk.level(SkillType.STRENGTH) != Config.SKILL_MAX) { fail("skill did not cap at 100"); ok = false; }

        // determinism: same seed => same population count
        long s = 7L;
        if (WorldBuilder.build(s).characterCount() != WorldBuilder.build(s).characterCount()) {
            fail("world build not deterministic"); ok = false;
        }

        return ok;
    }

    private static void fail(String msg) { System.out.println("  ASSERT FAILED: " + msg); }
}
