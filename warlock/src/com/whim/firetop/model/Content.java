package com.whim.firetop.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory of all game content: the dungeon board, the three card decks, room
 * monsters and Zagor. All names and descriptions here are <em>original writing</em>
 * evoking the tone of a Firetop Mountain crawl; none are transcriptions of the
 * 1986 boardgame's printed material.
 */
public final class Content {

    private Content() { }

    /** Zagor's boardgame stats (adopted ruling — see RESEARCH.md). */
    public static Monster zagor() {
        return new Monster("Zagor the Warlock", 12, 18,
                "The master of Firetop Mountain rises from his throne, eyes burning "
                        + "with cold sorcery. Only his death opens the way to the treasure.");
    }

    /**
     * Builds the canonical 16-room dungeon. Layout is a hand-authored graph
     * (adopted ruling — the physical board's exact tiling could not be verified).
     * Grid coordinates are used only for drawing.
     */
    public static Board buildDungeon() {
        Board b = new Board();

        add(b, 0, "Mountain Gate", RoomType.ENTRANCE, 0, 2,
                "A rough archway in the mountainside. Torchlight flickers on damp stone.");
        add(b, 1, "Guardroom", RoomType.MONSTER, 1, 2,
                "Bones and broken shields litter a cold guard post.");
        add(b, 2, "Weeping Passage", RoomType.EVENT, 1, 1,
                "Water seeps from the ceiling in steady, echoing drips.");
        add(b, 3, "Store Cellar", RoomType.TREASURE, 1, 3,
                "Shelves of rotted crates — and something that still glints.");
        add(b, 4, "Crossways", RoomType.EMPTY, 2, 2,
                "Four passages meet beneath a soot-black vault.");
        add(b, 5, "Snake Pit", RoomType.TRAP, 2, 1,
                "The floor gives way to a nest of hissing coils.");
        add(b, 6, "Fungus Grotto", RoomType.MONSTER, 2, 3,
                "Pale luminous fungus clings to every surface here.");
        add(b, 7, "Wishing Well", RoomType.SPECIAL, 3, 1,
                "An ancient well hums with a faint, hopeful magic.");
        add(b, 8, "Hall of Idols", RoomType.EVENT, 3, 2,
                "Row upon row of leering stone faces watch you pass.");
        add(b, 9, "Torture Chamber", RoomType.MONSTER, 3, 3,
                "Rusted implements hang from chains; the air reeks of old fear.");
        add(b, 10, "Silver Vault", RoomType.TREASURE, 4, 1,
                "A sealed strongroom the Warlock's servants missed.");
        add(b, 11, "Collapsed Bridge", RoomType.TRAP, 4, 2,
                "A narrow span crosses a chasm of unknown depth.");
        add(b, 12, "Ferryman's Landing", RoomType.EVENT, 4, 3,
                "Black water laps at a stone quay where a hooded ferryman waits.");
        add(b, 13, "Maze of Zagor", RoomType.EMPTY, 5, 2,
                "Identical corridors twist back on themselves without end.");
        add(b, 14, "Dragon's Rest", RoomType.MONSTER, 5, 1,
                "Scorched bones and a mound of coins mark a great beast's lair.");
        add(b, 15, "Throne of Firetop", RoomType.LAIR, 6, 2,
                "The final chamber. Upon a throne of black iron sits the Warlock himself.");

        // Corridors (bidirectional).
        b.link(0, 1);
        b.link(1, 2);
        b.link(1, 3);
        b.link(2, 4);
        b.link(3, 4);
        b.link(4, 5);
        b.link(4, 6);
        b.link(5, 7);
        b.link(4, 8);
        b.link(6, 9);
        b.link(8, 10);
        b.link(8, 11);
        b.link(9, 12);
        b.link(7, 10);
        b.link(10, 13);
        b.link(11, 13);
        b.link(12, 13);
        b.link(13, 14);
        b.link(13, 15);
        b.link(14, 15);

        b.setEntranceId(0);
        b.setLairId(15);

        // Seed fixed room monsters, gold and the Warlock.
        b.getRoom(1).setMonster(new Monster("Skeleton Warrior", 7, 6,
                "A rattling guard, sword still gripped in bony fingers."));
        b.getRoom(6).setMonster(new Monster("Cave Troll", 9, 11,
                "A hulking brute that smells of wet fur and rotten meat."));
        b.getRoom(9).setMonster(new Monster("Orc Torturer", 8, 8,
                "A squat, cruel creature delighted to have a new guest."));
        b.getRoom(14).setMonster(new Monster("Slumbering Dragon", 11, 14,
                "It wakes slowly, then all at once, in a rush of flame."));
        b.getRoom(15).setMonster(zagor());

        b.getRoom(3).setGold(8);
        b.getRoom(10).setGold(20);
        b.getRoom(14).setGold(15);

        return b;
    }

    private static void add(Board b, int id, String name, RoomType type, int x, int y, String desc) {
        b.addRoom(new Room(id, name, type, x, y, desc));
    }

    // ---- Decks (original content) --------------------------------------

    public static List<Card> buildTreasureDeck() {
        List<Card> d = new ArrayList<Card>();
        d.add(new Card("Pouch of Gold", CardType.TREASURE,
                "Coins spill from a hidden crevice.", CardEffect.GAIN_GOLD, 6));
        d.add(new Card("Jewelled Dagger", CardType.TREASURE,
                "A fine blade set with a single red stone.", CardEffect.GAIN_TREASURE, 12));
        d.add(new Card("Flask of Healing", CardType.TREASURE,
                "A warm draught that knits flesh and steadies nerves.", CardEffect.GAIN_POTION, 4));
        d.add(new Card("Whetstone Charm", CardType.TREASURE,
                "Your edge feels keener; your hand, surer.", CardEffect.GAIN_SKILL, 1));
        d.add(new Card("Traveller's Rations", CardType.TREASURE,
                "Dried meat and hard bread, still good.", CardEffect.GAIN_PROVISION, 2));
        d.add(new Card("Silver Idol", CardType.TREASURE,
                "A small heavy figure of forgotten worship.", CardEffect.GAIN_TREASURE, 18));
        d.add(new Card("Rabbit's Foot", CardType.TREASURE,
                "A grisly little token said to turn fortune.", CardEffect.RESTORE_LUCK, 2));
        d.add(new Card("Handful of Copper", CardType.TREASURE,
                "Not much, but it clinks pleasantly.", CardEffect.GAIN_GOLD, 3));
        d.add(new Card("Elixir of Vigour", CardType.TREASURE,
                "Strength floods back into tired limbs.", CardEffect.RESTORE_STAMINA, 6));
        d.add(new Card("Empty Chest", CardType.TREASURE,
                "Someone got here first. Nothing but dust.", CardEffect.NOTHING, 0));
        return d;
    }

    public static List<Card> buildEventDeck() {
        List<Card> d = new ArrayList<Card>();
        d.add(new Card("Falling Rocks", CardType.EVENT,
                "The ceiling groans and lets go. Dive, or be crushed!",
                CardEffect.TEST_LUCK_TRAP, 3));
        d.add(new Card("Poison Needle", CardType.EVENT,
                "A hidden spring drives a blackened point at your hand.",
                CardEffect.TEST_LUCK_TRAP, 2));
        d.add(new Card("Cool Spring", CardType.EVENT,
                "A clear pool refreshes body and spirit.",
                CardEffect.RESTORE_STAMINA, 4));
        d.add(new Card("Lucky Coin", CardType.EVENT,
                "A gleaming coin winks at you from the mud — and fortune smiles.",
                CardEffect.RESTORE_LUCK, 2));
        d.add(new Card("Sudden Chill", CardType.EVENT,
                "An unnatural cold saps the warmth from your bones.",
                CardEffect.LOSE_STAMINA, 2));
        d.add(new Card("Abandoned Pack", CardType.EVENT,
                "A dead adventurer's supplies — no use to them now.",
                CardEffect.GAIN_PROVISION, 1));
        d.add(new Card("Whispering Shadows", CardType.EVENT,
                "Voices promise secrets, then fade. Nothing more.",
                CardEffect.NOTHING, 0));
        d.add(new Card("Trapped Coffer", CardType.EVENT,
                "Gold glints inside — but the lid is rigged.",
                CardEffect.TEST_LUCK_TRAP, 2));
        d.add(new Card("Shrine Blessing", CardType.EVENT,
                "You kneel at a broken altar and rise feeling stronger.",
                CardEffect.GAIN_SKILL, 1));
        d.add(new Card("Rat Swarm", CardType.EVENT,
                "A living carpet of teeth and fur boils out of the dark.",
                CardEffect.ENCOUNTER_MONSTER, 0,
                new Monster("Rat Swarm", 6, 5, "Dozens of biting vermin.")));
        d.add(new Card("Lone Goblin", CardType.EVENT,
                "A startled goblin bares its teeth and lunges.",
                CardEffect.ENCOUNTER_MONSTER, 0,
                new Monster("Goblin Scout", 6, 6, "Small, quick and vicious.")));
        d.add(new Card("Pit Viper", CardType.EVENT,
                "Something long and dark uncoils across the passage.",
                CardEffect.ENCOUNTER_MONSTER, 0,
                new Monster("Pit Viper", 7, 4, "Its bite is swift and cold.")));
        return d;
    }

    /** The encounter deck (used when a MONSTER room's fixed foe is already beaten). */
    public static List<Card> buildEncounterDeck() {
        List<Card> d = new ArrayList<Card>();
        d.add(new Card("Wandering Ghoul", CardType.ENCOUNTER, "A grave-stench creature shambles near.",
                CardEffect.ENCOUNTER_MONSTER, 0, new Monster("Ghoul", 8, 7, "Grey flesh and hungry eyes.")));
        d.add(new Card("Giant Spider", CardType.ENCOUNTER, "Webs shiver as it drops from above.",
                CardEffect.ENCOUNTER_MONSTER, 0, new Monster("Giant Spider", 7, 8, "Fangs drip venom.")));
        d.add(new Card("Orc Patrol", CardType.ENCOUNTER, "A patrol rounds the corner, weapons ready.",
                CardEffect.ENCOUNTER_MONSTER, 0, new Monster("Orc Soldier", 8, 9, "Scarred and spoiling for a fight.")));
        d.add(new Card("Restless Wraith", CardType.ENCOUNTER, "A chill mist takes the shape of a warrior.",
                CardEffect.ENCOUNTER_MONSTER, 0, new Monster("Wraith", 9, 8, "It hungers for the living.")));
        return d;
    }
}
