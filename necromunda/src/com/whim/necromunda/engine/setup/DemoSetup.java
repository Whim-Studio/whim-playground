package com.whim.necromunda.engine.setup;

import com.whim.necromunda.engine.data.WeaponCatalogue;
import com.whim.necromunda.model.Armour;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.House;
import com.whim.necromunda.model.StatLine;
import com.whim.necromunda.model.board.Board;
import com.whim.necromunda.model.board.Position;
import com.whim.necromunda.model.board.TerrainType;
import com.whim.necromunda.model.board.Tile;

/**
 * Scaffold data for the first playable: a fixed 24x24 demo board with a couple of
 * multi-level structures, and two placed gangs. Everything here is plain model
 * data — no rendering, no rules — so tests and the UI share the same setup.
 */
public final class DemoSetup {

    public static final int BOARD_W = 24;
    public static final int BOARD_H = 24;

    private DemoSetup() {
    }

    /** Build the demo battlefield: walls with a doorway, a pit, rubble, a barricade,
     *  and a ladder + gantry leading up to a raised platform. */
    public static Board demoBoard() {
        Board board = new Board(BOARD_W, BOARD_H);

        // A central wall running horizontally with a doorway gap.
        for (int x = 4; x < 20; x++) {
            if (x == 11 || x == 12) {
                continue; // doorway
            }
            board.tile(x, 12).setTerrain(TerrainType.WALL);
            board.tile(x, 12).setHeight(2);
        }

        // A hazard pit near the lower-left.
        for (int x = 5; x <= 7; x++) {
            for (int y = 4; y <= 6; y++) {
                board.tile(x, y).setTerrain(TerrainType.PIT);
            }
        }

        // Rubble fields.
        setTerrain(board, TerrainType.RUBBLE, 14, 4, 17, 6);
        setTerrain(board, TerrainType.RUBBLE, 2, 18, 4, 20);

        // A barricade line the attackers can shelter behind.
        for (int x = 9; x <= 14; x++) {
            board.tile(x, 17).setTerrain(TerrainType.BARRICADE);
        }

        // A raised platform (upper-right) reached by a ladder + gantry.
        setTerrainWithHeight(board, TerrainType.PLATFORM, 18, 18, 21, 21, 2);
        board.tile(17, 19).setTerrain(TerrainType.LADDER);
        board.tile(17, 20).setTerrain(TerrainType.GANTRY);
        board.tile(17, 20).setHeight(2);

        return board;
    }

    private static void setTerrain(Board b, TerrainType t, int x0, int y0, int x1, int y1) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                b.tile(x, y).setTerrain(t);
            }
        }
    }

    private static void setTerrainWithHeight(Board b, TerrainType t, int x0, int y0, int x1, int y1, int h) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                Tile tile = b.tile(x, y);
                tile.setTerrain(t);
                tile.setHeight(h);
            }
        }
    }

    /** Blue tech/marksman gang. */
    public static Gang gangA() {
        Gang gang = new Gang("Voltaic Syndicate", House.VAN_SAAR);
        gang.setCredits(120);
        gang.add(fighter("A-1", "Vex", FighterType.LEADER,
                StatLine.of(4, 4, 4, 3, 3, 1, 4, 1, 8), Armour.MESH, "LAS_PISTOL", "POWER_SWORD"));
        gang.add(fighter("A-2", "Dace", FighterType.CHAMPION,
                StatLine.of(4, 3, 4, 3, 3, 1, 3, 1, 7), Armour.MESH, "LASGUN"));
        gang.add(fighter("A-3", "Kordo", FighterType.GANGER,
                StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7), Armour.FLAK, "AUTOGUN"));
        gang.add(fighter("A-4", "Sprint", FighterType.JUVE,
                StatLine.of(4, 2, 2, 3, 3, 1, 3, 1, 6), Armour.NONE, "STUB_GUN", "KNIFE"));
        return gang;
    }

    /** Red brute gang. */
    public static Gang gangB() {
        Gang gang = new Gang("Iron Maw", House.GOLIATH);
        gang.setCredits(95);
        gang.add(fighter("B-1", "Brakk", FighterType.LEADER,
                StatLine.of(4, 4, 3, 4, 4, 1, 3, 1, 8), Armour.MESH, "AUTO_PISTOL", "CLUB"));
        gang.add(fighter("B-2", "Hoss", FighterType.CHAMPION,
                StatLine.of(4, 4, 3, 4, 4, 1, 2, 1, 7), Armour.FLAK, "HEAVY_STUBBER"));
        gang.add(fighter("B-3", "Grist", FighterType.GANGER,
                StatLine.of(4, 3, 3, 4, 4, 1, 2, 1, 7), Armour.FLAK, "SHOTGUN"));
        gang.add(fighter("B-4", "Nub", FighterType.JUVE,
                StatLine.of(4, 2, 2, 3, 3, 1, 3, 1, 6), Armour.NONE, "STUB_GUN", "KNIFE"));
        return gang;
    }

    private static Fighter fighter(String id, String name, FighterType type,
                                   StatLine stats, Armour armour, String... weaponIds) {
        Fighter f = new Fighter(id, name, type, stats);
        f.setArmour(armour);
        for (String wid : weaponIds) {
            if (WeaponCatalogue.contains(wid)) {
                f.addWeapon(WeaponCatalogue.byId(wid));
            }
        }
        return f;
    }

    /** Place both gangs at opposite edges of the board (deployment zones). */
    public static void placeGangs(Board board, Gang a, Gang b) {
        int y = 2;
        int x = 3;
        for (Fighter f : a.roster()) {
            board.place(f, new Position(x, y, 0));
            x += 2;
        }
        // Gang B: put the leader up on the platform to show elevation.
        int bx = 5;
        int by = 22;
        boolean first = true;
        for (Fighter f : b.roster()) {
            if (first) {
                board.place(f, new Position(19, 19, 2));
                first = false;
            } else {
                board.place(f, new Position(bx, by, 0));
                bx += 2;
            }
        }
    }
}
