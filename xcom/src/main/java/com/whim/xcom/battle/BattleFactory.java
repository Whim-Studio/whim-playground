package com.whim.xcom.battle;

import com.whim.xcom.rng.Rng;
import com.whim.xcom.rng.SeededRng;
import com.whim.xcom.rules.Ruleset;
import com.whim.xcom.rules.def.AlienDef;
import com.whim.xcom.rules.def.ArmorDef;
import com.whim.xcom.rules.def.WeaponDef;

/**
 * Turns a {@link BattleSetup} into a ready-to-play {@link BattleGame}: it
 * generates a procedural map, deploys the squad along the south edge and the
 * aliens across the north, and wires each {@link BattleUnit} to its ruleset
 * weapon/armour. Also provides a canned skirmish for the "New Game" menu.
 */
public final class BattleFactory {

    private BattleFactory() {
    }

    public static BattleGame build(Ruleset ruleset, BattleSetup setup) {
        Rng rng = new SeededRng(setup.seed());
        BattleMap map = generateMap(rng, setup.mapWidth(), setup.mapHeight());
        BattleGame game = new BattleGame(ruleset, rng, map, setup.night());

        // Deploy soldiers along the bottom two rows, spread across the width.
        int sIndex = 0;
        int soldierRowTop = map.height() - 2;
        for (BattleSetup.UnitSpec spec : setup.soldiers()) {
            int[] pos = freeTile(map, game, spreadColumn(map, sIndex, setup.soldiers().size()),
                    map.height() - 1, soldierRowTop);
            WeaponDef weapon = ruleset.weapon(spec.weaponId);
            ArmorDef armor = spec.armorId != null ? ruleset.armor(spec.armorId) : ruleset.armor("none");
            BattleUnit u = new BattleUnit("S" + sIndex, spec.name, Side.XCOM,
                    spec.maxTU, spec.maxHealth, spec.firingAccuracy, spec.reactions, spec.strength,
                    weapon, armor);
            u.setGrenades(2);
            u.setPos(pos[0], pos[1]);
            u.setFacing(0); // face north, toward the aliens
            game.addUnit(u);
            sIndex++;
        }

        // Deploy aliens across the top third.
        int aIndex = 0;
        for (BattleSetup.UnitSpec spec : setup.aliens()) {
            AlienDef def = ruleset.alien(spec.alienId);
            int col = spreadColumn(map, aIndex, setup.aliens().size());
            int[] pos = freeTile(map, game, col, 0, Math.max(1, map.height() / 3));
            WeaponDef weapon = ruleset.weapon(spec.weaponId);
            ArmorDef armor = Armors.uniform(spec.alienId + "_skin",
                    def != null ? def.frontArmor() : 0);
            // Difficulty scales alien stats around the Experienced (level 2) baseline.
            double scale = 1.0 + 0.08 * (setup.difficulty().level() - 2);
            int tu = scaled(def != null ? def.timeUnits() : 50, scale);
            int hp = scaled(def != null ? def.health() : 30, scale);
            int acc = scaled(def != null ? def.firingAccuracy() : 50, scale);
            int rea = scaled(def != null ? def.reactions() : 50, scale);
            int str = def != null ? def.strength() : 30;
            String name = (def != null ? def.name() : "Alien") + " " + (aIndex + 1);
            BattleUnit u = new BattleUnit("A" + aIndex, name, Side.ALIEN,
                    tu, hp, acc, rea, str, weapon, armor);
            u.setPos(pos[0], pos[1]);
            u.setFacing(4); // face south, toward the squad
            game.addUnit(u);
            aIndex++;
        }

        game.recomputeVisibility();
        return game;
    }

    /** A default 4-soldier vs 4-Sectoid skirmish for the menu's "New Game". */
    public static BattleGame defaultSkirmish(Ruleset ruleset, long seed) {
        String rifle = ruleset.weapon("rifle") != null ? "rifle" : firstWeaponId(ruleset);
        BattleSetup setup = new BattleSetup().mapSize(16, 16).seed(seed);
        setup.addSoldier(BattleSetup.UnitSpec.soldier("Sgt. Vasquez", rifle, "none", 65, 58, 34, 55, 30));
        setup.addSoldier(BattleSetup.UnitSpec.soldier("Cpl. Tanaka", rifle, "none", 60, 54, 32, 50, 28));
        setup.addSoldier(BattleSetup.UnitSpec.soldier("Pvt. Novak", rifle, "none", 55, 56, 36, 45, 32));
        setup.addSoldier(BattleSetup.UnitSpec.soldier("Pvt. Adeyemi", rifle, "none", 58, 55, 33, 48, 30));

        String alienId = ruleset.alien("sectoid_soldier") != null ? "sectoid_soldier" : firstAlienId(ruleset);
        for (int i = 0; i < 4; i++) {
            setup.addAlien(BattleSetup.UnitSpec.alien(alienId, rifle));
        }
        return build(ruleset, setup);
    }

    // ---- helpers ------------------------------------------------------------

    private static BattleMap generateMap(Rng rng, int w, int h) {
        BattleMap map = new BattleMap(w, h);
        // Scatter rock/bush clusters, keeping the top and bottom two rows clear
        // as deployment zones.
        int clusters = 4 + rng.nextInt(4);
        for (int c = 0; c < clusters; c++) {
            int cx = 1 + rng.nextInt(w - 2);
            int cy = 3 + rng.nextInt(Math.max(1, h - 6));
            int size = 1 + rng.nextInt(3);
            Tile.Kind kind = rng.chance(0.5) ? Tile.Kind.ROCK : Tile.Kind.BUSH;
            for (int i = 0; i < size; i++) {
                int x = Math.max(0, Math.min(w - 1, cx + rng.rangeInclusive(-1, 1)));
                int y = Math.max(2, Math.min(h - 3, cy + rng.rangeInclusive(-1, 1)));
                map.tile(x, y).setKind(kind);
            }
        }
        // A small ruined building (walls) near the middle for cover/LOS interest.
        int bx = w / 2 - 1;
        int by = h / 2 - 1;
        if (map.inBounds(bx, by) && map.inBounds(bx + 2, by + 1)) {
            map.tile(bx, by).setKind(Tile.Kind.WALL);
            map.tile(bx + 2, by).setKind(Tile.Kind.WALL);
            map.tile(bx, by + 1).setKind(Tile.Kind.WALL);
            map.tile(bx + 1, by).setKind(Tile.Kind.ROAD);
        }
        return map;
    }

    private static int scaled(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
    }

    private static int spreadColumn(BattleMap map, int index, int count) {
        if (count <= 1) {
            return map.width() / 2;
        }
        int margin = 1;
        int span = map.width() - 2 * margin - 1;
        return margin + (int) Math.round((double) index * span / (count - 1));
    }

    /** Find a free walkable tile near {@code (col, rowFrom..rowTo)}. */
    private static int[] freeTile(BattleMap map, BattleGame game, int col, int rowFrom, int rowTo) {
        int step = rowTo >= rowFrom ? 1 : -1;
        for (int y = rowFrom; y != rowTo + step; y += step) {
            for (int dx = 0; dx < map.width(); dx++) {
                for (int sgn = -1; sgn <= 1; sgn += 2) {
                    int x = col + sgn * dx;
                    if (map.walkable(x, y) && game.unitAt(x, y) == null) {
                        return new int[] {x, y};
                    }
                }
            }
        }
        // fallback: any free tile
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (map.walkable(x, y) && game.unitAt(x, y) == null) {
                    return new int[] {x, y};
                }
            }
        }
        return new int[] {0, 0};
    }

    private static String firstWeaponId(Ruleset ruleset) {
        WeaponDef w = ruleset.weapons().iterator().next();
        return w.id();
    }

    private static String firstAlienId(Ruleset ruleset) {
        AlienDef a = ruleset.aliens().iterator().next();
        return a.id();
    }
}
