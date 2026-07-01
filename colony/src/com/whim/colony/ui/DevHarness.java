package com.whim.colony.ui;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Job;
import com.whim.colony.domain.Building;
import com.whim.colony.domain.BuildingType;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.ColonyMap;
import com.whim.colony.domain.MapTile;
import com.whim.colony.domain.Resources;
import com.whim.colony.domain.SkillType;
import com.whim.colony.domain.TerrainType;

import javax.swing.SwingUtilities;

/**
 * DEV HARNESS ONLY — not the production entry point.
 *
 * <p>The orchestrator (app/Main) owns the real wiring: it builds a
 * {@link ColonyState} via the engine, constructs a {@link GameFrame} around it,
 * makes the frame visible and calls {@link GameFrame#startRefresh()}.
 *
 * <p>This class exists so the UI can be eyeballed in isolation without the
 * engine on the branch. It builds a small THROWAWAY {@link ColonyState} fixture
 * purely from Task-1 domain constructors (no engine, no simulation) and shows
 * the frame. Delete or ignore for production.
 */
public final class DevHarness {

    private DevHarness() {
    }

    public static void main(String[] args) {
        final ColonyState state = buildFixtureState();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                GameFrame frame = new GameFrame(state);
                frame.setVisible(true);
                frame.startRefresh();
            }
        });
    }

    /** Build a small static world by hand — data only, no engine logic. */
    public static ColonyState buildFixtureState() {
        int w = 32;
        int h = 24;
        ColonyMap map = new ColonyMap(w, h);

        // sculpt some terrain variety directly on the tiles
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                MapTile tile = map.getTile(x, y);
                if (tile == null) {
                    continue;
                }
                if (y >= h - 4) {
                    tile.setTerrain(TerrainType.WATER);
                } else if (x < 5 && y < 6) {
                    tile.setTerrain(TerrainType.ROCK);
                } else if ((x + y) % 11 == 0) {
                    tile.setTerrain(TerrainType.DIRT);
                }
            }
        }

        // a little walled room with a bed and a stockpile
        placeBuilding(map, 10, 8, BuildingType.WALL);
        placeBuilding(map, 11, 8, BuildingType.WALL);
        placeBuilding(map, 12, 8, BuildingType.WALL);
        placeBuilding(map, 10, 10, BuildingType.WALL);
        placeBuilding(map, 12, 10, BuildingType.WALL);
        placeBuilding(map, 11, 9, BuildingType.BED);
        placeBuilding(map, 14, 9, BuildingType.STOCKPILE);
        placeBuilding(map, 16, 12, BuildingType.FARM);
        placeBuilding(map, 17, 12, BuildingType.FARM);

        Resources resources = new Resources(120, 45, 80);
        ColonyState state = new ColonyState(map, resources);

        Colonist alice = new Colonist(1, "Alice", 8, 9);
        alice.getNeeds().setHunger(72);
        alice.getNeeds().setRest(40);
        alice.getNeeds().setMood(85);
        alice.getSkills().setLevel(SkillType.CONSTRUCTION, 6);
        alice.getSkills().setLevel(SkillType.MINING, 3);
        alice.setCurrentJob(namedJob("Build wall", 12, 10));
        alice.getPath().add(new int[]{9, 9});
        alice.getPath().add(new int[]{10, 9});
        alice.getPath().add(new int[]{11, 10});
        alice.getPath().add(new int[]{12, 10});
        state.getColonists().add(alice);

        Colonist bob = new Colonist(2, "Bob", 16, 12);
        bob.getNeeds().setHunger(18);   // hungry -> orange pip
        bob.getNeeds().setRest(60);
        bob.getNeeds().setMood(22);     // low mood -> reddish body
        bob.getSkills().setLevel(SkillType.FARMING, 8);
        bob.getSkills().setLevel(SkillType.COOKING, 4);
        bob.setCurrentJob(namedJob("Sow farm", 17, 12));
        state.getColonists().add(bob);

        Colonist cara = new Colonist(3, "Cara", 20, 5);
        cara.getNeeds().setHunger(90);
        cara.getNeeds().setRest(8);     // exhausted -> red pip
        cara.getNeeds().setMood(50);
        cara.getSkills().setLevel(SkillType.MEDICINE, 5);
        state.getColonists().add(cara); // idle (no job)

        state.addMessage("Colony founded at tick 0.");
        state.addMessage("Alice started: Build wall.");
        state.addMessage("Bob is getting hungry.");
        state.addMessage("Cara is exhausted and idle.");
        state.setTick(1234L);

        return state;
    }

    private static void placeBuilding(ColonyMap map, int x, int y, BuildingType type) {
        MapTile tile = map.getTile(x, y);
        if (tile == null) {
            return;
        }
        Building b = new Building(type, tile);
        tile.setBuilding(b);
    }

    /** A trivial fixture Job — just a label + target. Not real work logic. */
    private static Job namedJob(final String name, final int tx, final int ty) {
        return new Job() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isComplete(ColonyState state, Colonist c) {
                return false;
            }

            @Override
            public int getTargetX() {
                return tx;
            }

            @Override
            public int getTargetY() {
                return ty;
            }
        };
    }
}
