package com.whim.albion.data;

import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.TileType;
import com.whim.albion.world.InteractableImpl;
import com.whim.albion.world.NpcImpl;
import com.whim.albion.world.Tile;
import com.whim.albion.world.TileMap;
import com.whim.albion.world.TransitionImpl;
import com.whim.albion.world.WorldModelImpl;

import java.util.Arrays;

/**
 * Authors the two starting maps and registers them into a {@link WorldModelImpl}:
 * the OUTDOOR_2D town of Duskhollow (NPCs, a peddler/shop, a chest, a crypt
 * entrance) and the INDOOR_3D crypt (corridors, an encounter cell, a locked door
 * gated on the crypt key, and the relic vault).
 */
public final class MapFactory {

    public static final String MAP_TOWN  = "map_duskhollow";
    public static final String MAP_CRYPT = "map_crypt";

    // Town entry (new-game start).
    public static final int TOWN_START_X = 8;
    public static final int TOWN_START_Y = 8;

    // Crypt entry (from the town stairs).
    public static final int CRYPT_ENTRY_X = 6;
    public static final int CRYPT_ENTRY_Y = 9;

    // Vault cell the player is placed on after unlocking the iron door.
    public static final int CRYPT_VAULT_X = 9;
    public static final int CRYPT_VAULT_Y = 6;

    private MapFactory() {}

    /** Build + register both maps, then place the player at the town start. */
    public static void buildWorld(WorldModelImpl world, AlbionContent content) {
        world.registerMap(buildTown());
        world.registerMap(buildCrypt());
        world.loadMap(MAP_TOWN, TOWN_START_X, TOWN_START_Y, Direction.NORTH);
    }

    // --------------------------------------------------------------- town map

    private static TileMap buildTown() {
        TileMap m = new TileMap(MAP_TOWN, "Duskhollow", MapType.OUTDOOR_2D, 16, 12, TileType.GRASS);
        m.border(TileType.OBSTACLE); // a ring of hedges/trees

        // Cobbled paths (a cross through town).
        for (int y = 1; y <= 10; y++) m.set(8, y, TileType.PATH);
        for (int x = 1; x <= 14; x++) m.set(x, 6, TileType.PATH);

        // A little decorative greenery (blocks movement + sight).
        m.set(2, 2, TileType.OBSTACLE);
        m.set(3, 2, TileType.OBSTACLE);
        m.set(13, 3, TileType.OBSTACLE);
        m.set(12, 9, TileType.OBSTACLE);
        m.set(3, 10, TileType.OBSTACLE);

        // Crypt entrance: stairs at the north end of the path.
        m.set(8, 1, Tile.of(TileType.STAIRS).withDecor("decor.crypt_mouth"));
        m.addTransition(8, 1, new TransitionImpl(MAP_CRYPT, CRYPT_ENTRY_X, CRYPT_ENTRY_Y, Direction.NORTH));

        // Steward Maelen — quest giver.
        m.addNpc(new NpcImpl(7, 4, "Steward Maelen", "portrait.steward", false));
        m.addInteractable(7, 4, InteractableImpl.talk("npc_steward", "Steward Maelen", AlbionContent.DLG_ELDER));

        // WEND the Peddler — shopkeeper.
        m.addNpc(new NpcImpl(4, 5, "WEND, the Peddler", "portrait.peddler", false));
        m.addInteractable(4, 5, InteractableImpl.talk("npc_peddler", "WEND, the Peddler", AlbionContent.DLG_SHOP));

        // A fretful villager.
        m.addNpc(new NpcImpl(11, 7, "Fretful Villager", "portrait.villager", false));
        m.addInteractable(11, 7, InteractableImpl.talk("npc_villager", "Fretful Villager", AlbionContent.DLG_VILLAGER));

        // A town chest with a spare potion.
        m.addInteractable(10, 8, InteractableImpl.chest("chest_town", "Weathered Crate",
                Arrays.asList(AlbionContent.ITM_HEAL_DRAUGHT)));

        return m;
    }

    // -------------------------------------------------------------- crypt map

    private static TileMap buildCrypt() {
        TileMap m = new TileMap(MAP_CRYPT, "Crypt of Duskhollow", MapType.INDOOR_3D, 12, 12, TileType.WALL);

        // Main vertical corridor.
        for (int y = 2; y <= 9; y++) m.set(6, y, TileType.FLOOR);
        m.set(CRYPT_ENTRY_X, CRYPT_ENTRY_Y, Tile.of(TileType.FLOOR));

        // Exit stairs back to town.
        m.set(6, 10, Tile.of(TileType.STAIRS).withDecor("decor.stairs_up"));
        m.addTransition(6, 10, new TransitionImpl(MAP_TOWN, TOWN_START_X, TOWN_START_Y - 6, Direction.SOUTH));

        // West passage + a treasure crate.
        for (int x = 2; x <= 6; x++) m.set(x, 2, TileType.FLOOR);
        m.addInteractable(2, 2, InteractableImpl.chest("chest_crypt_west", "Mouldering Chest",
                Arrays.asList(AlbionContent.ITM_MANA_TONIC)));

        // A wandering pack lurks on the west passage.
        m.addEncounter(4, 2, AlbionContent.ENC_DUNGEON_2);

        // The main ambush cell on the corridor.
        m.addEncounter(6, 4, AlbionContent.ENC_DUNGEON_1);

        // East branch to the locked vault.
        m.set(7, 6, TileType.FLOOR);
        m.set(8, 6, new Tile(TileType.DOOR, false, true, "decor.iron_door")); // locked (blocked)
        m.addInteractable(8, 6, InteractableImpl.talk("door_crypt", "Iron Crypt Door", AlbionContent.DLG_DOOR));

        // The relic vault (reached by teleport after unlocking).
        m.set(9, 6, TileType.FLOOR);
        m.set(10, 6, TileType.FLOOR);
        m.set(9, 7, TileType.FLOOR);
        m.set(10, 7, TileType.FLOOR);
        m.addInteractable(10, 6, InteractableImpl.chest("chest_relic", "Reliquary",
                Arrays.asList(AlbionContent.ITM_RELIC_SHARD)));

        return m;
    }
}
