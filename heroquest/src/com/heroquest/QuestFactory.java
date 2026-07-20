package com.heroquest;

import com.heroquest.logic.Dice;
import com.heroquest.logic.Visibility;
import com.heroquest.model.Decks;
import com.heroquest.model.DungeonMap;
import com.heroquest.model.Furniture;
import com.heroquest.model.GameState;
import com.heroquest.model.Hero;
import com.heroquest.model.HeroType;
import com.heroquest.model.Monster;
import com.heroquest.model.MonsterType;
import com.heroquest.model.Point;
import com.heroquest.model.SpellElement;
import com.heroquest.model.Tile;
import com.heroquest.model.TileType;

/**
 * Builds the sample quest "The Trial": a six-room dungeon connected by a corridor
 * spine, populated with the four Heroes, Zargon's monsters, furniture and traps.
 */
public final class QuestFactory {

    private static final int W = 22;
    private static final int H = 15;

    private QuestFactory() {
    }

    public static GameState buildTrialQuest(Dice dice) {
        DungeonMap map = new DungeonMap(W, H);

        // Rooms: id, x0, y0, x1, y1 (inclusive).
        carveRoom(map, 0, 1, 1, 5, 4);    // start room (top-left)
        carveRoom(map, 2, 8, 1, 12, 4);   // top-middle
        carveRoom(map, 3, 15, 1, 20, 4);  // top-right
        carveRoom(map, 1, 1, 8, 5, 13);   // bottom-left
        carveRoom(map, 5, 8, 8, 12, 13);  // bottom-middle
        carveRoom(map, 4, 14, 8, 20, 13); // bottom-right (boss)

        // Corridor spine along y = 6.
        for (int x = 1; x <= 20; x++) {
            set(map, x, 6, TileType.FLOOR, -1);
        }

        // Doors connecting each room to the spine.
        door(map, 3, 5);
        door(map, 10, 5);
        door(map, 17, 5);
        door(map, 3, 7);
        door(map, 10, 7);
        door(map, 17, 7);

        // Furniture.
        map.tileAt(12, 3).setFurniture(Furniture.TABLE);
        map.tileAt(20, 4).setFurniture(Furniture.CHEST);
        map.tileAt(4, 13).setFurniture(Furniture.CHEST);
        map.tileAt(20, 13).setFurniture(Furniture.THRONE);
        map.tileAt(14, 13).setFurniture(Furniture.TOMB);

        // Hidden pit traps in the corridor.
        map.tileAt(7, 6).setTrap(true);
        map.tileAt(13, 6).setTrap(true);

        GameState state = new GameState(map);
        state.setTreasureDeck(Decks.buildTreasureDeck(new java.util.Random()));

        // Heroes in the start room.
        addHero(state, HeroType.BARBARIAN, 1, 1);
        addHero(state, HeroType.DWARF, 3, 1);
        Hero elf = addHero(state, HeroType.ELF, 1, 3);
        Hero wizard = addHero(state, HeroType.WIZARD, 3, 3);

        elf.getSpells().addAll(Decks.buildSpellSet(SpellElement.WATER));
        wizard.getSpells().addAll(Decks.buildSpellSet(SpellElement.FIRE));
        wizard.getSpells().addAll(Decks.buildSpellSet(SpellElement.AIR));
        wizard.getSpells().addAll(Decks.buildSpellSet(SpellElement.EARTH));

        // Monsters.
        addMonster(state, MonsterType.ORC, 9, 2);
        addMonster(state, MonsterType.GOBLIN, 11, 2);
        addMonster(state, MonsterType.SKELETON, 16, 2);
        addMonster(state, MonsterType.ZOMBIE, 19, 2);
        addMonster(state, MonsterType.GOBLIN, 2, 10);
        addMonster(state, MonsterType.GOBLIN, 4, 11);
        addMonster(state, MonsterType.FIMIR, 10, 10);
        addMonster(state, MonsterType.MUMMY, 18, 9);
        addMonster(state, MonsterType.CHAOS_WARRIOR, 16, 11);
        addMonster(state, MonsterType.GARGOYLE, 19, 12);

        // Reveal the start room and around each Hero.
        Visibility.revealRoom(state, 0);
        for (Hero h : state.getHeroes()) {
            Visibility.revealFrom(state, h.getPosition());
        }

        state.setActiveHeroIndex(0);
        state.log("Quest 'The Trial' begins. Four Heroes enter Zargon's dungeon.");
        return state;
    }

    private static void carveRoom(DungeonMap map, int roomId, int x0, int y0, int x1, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                set(map, x, y, TileType.FLOOR, roomId);
            }
        }
    }

    private static void door(DungeonMap map, int x, int y) {
        set(map, x, y, TileType.DOOR_CLOSED, -1);
    }

    private static void set(DungeonMap map, int x, int y, TileType type, int roomId) {
        Tile t = map.tileAt(x, y);
        t.setType(type);
        t.setRoomId(roomId);
    }

    private static Hero addHero(GameState state, HeroType type, int x, int y) {
        Hero h = new Hero(type);
        h.setPosition(new Point(x, y));
        state.getHeroes().add(h);
        return h;
    }

    private static Monster addMonster(GameState state, MonsterType type, int x, int y) {
        Monster m = new Monster(type);
        m.setPosition(new Point(x, y));
        state.getMonsters().add(m);
        return m;
    }
}
