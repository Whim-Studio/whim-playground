package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.List;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.ResourceType;

/**
 * Static factories for the starting decks of each faction plus the SIN card.
 *
 * <p>Card <em>content and ordering</em> are deterministic (a given faction
 * always produces the same list of cards in the same order). Card <em>ids</em>
 * come from a process-wide monotonic sequence so every card ever minted is
 * unique; the contract only requires shuffle/draw determinism (driven by the
 * game's seeded {@link java.util.Random}), not id stability across games.</p>
 */
public final class CardLibrary {

    private CardLibrary() {}

    /** Process-wide id sequence guaranteeing globally-unique card ids. */
    private static final IdGenerator SEQ = new IdGenerator(1);

    /** The starting deck for a faction (unshuffled — the caller shuffles). */
    public static List<Card> startingDeck(Faction faction) {
        if (faction == Faction.BABYLON) {
            return babylonDeck();
        }
        if (faction == Faction.THE_UNFAITHFUL) {
            return unfaithfulDeck();
        }
        return landsOfTheKingDeck();
    }

    /** A single SIN card: un-playable dead weight that clogs a deck. */
    public static Card sinCard() {
        return new Card(SEQ.next(), "Sin", CardType.SIN, 0,
                null, null, 0, 0, null, false,
                "Dead weight. Cannot be played; discard it to move on.");
    }

    // ---- faction decks ----

    private static List<Card> landsOfTheKingDeck() {
        List<Card> d = new ArrayList<Card>();
        // balanced spread, leans on card draw
        d.add(building("City", BuildingType.CITY, 4,
                "A seat of population. Hosts Workers and Witches."));
        d.add(building("Farm", BuildingType.FARM, 2,
                "Steady gold from the land."));
        d.add(building("Temple", BuildingType.TEMPLE, 3,
                "Faith made stone. Hosts Idols and Witches."));
        d.add(building("Port", BuildingType.PORT, 3,
                "Trade by water. Must sit beside the sea."));
        d.add(attachment("Worker", AttachmentType.WORKER, 1,
                "Labours in a City for extra gold."));
        d.add(attachment("Worker", AttachmentType.WORKER, 1,
                "Labours in a City for extra gold."));
        d.add(attachment("Idol", AttachmentType.IDOL, 1,
                "Draws an extra card each turn from a Temple."));
        d.add(attachment("Witch", AttachmentType.WITCH, 2,
                "Channels command points from a Temple or City."));
        d.add(military("Levy", 2, 3, false,
                "Conscript militia. Modest attack."));
        d.add(military("Knight", 3, 4, false,
                "Armoured cavalry. Strong attack."));
        d.add(economy("Tax", 2, ResourceType.GOLD, 3, false,
                "Collect gold from your lands."));
        d.add(economy("Muster", 2, ResourceType.COMMAND_POINTS, 2, false,
                "Rally command points."));
        d.add(explore("Scout", 1, 1,
                "Reveal nearby tiles."));
        d.add(explore("Scout", 1, 1,
                "Reveal nearby tiles."));
        return d;
    }

    private static List<Card> babylonDeck() {
        List<Card> d = new ArrayList<Card>();
        // building-focused: extra Cities, plenty of attachments
        d.add(building("City", BuildingType.CITY, 4,
                "A seat of population. Hosts Workers and Witches."));
        d.add(building("City", BuildingType.CITY, 4,
                "A seat of population. Hosts Workers and Witches."));
        d.add(building("Farm", BuildingType.FARM, 2,
                "Steady gold from the land."));
        d.add(building("Temple", BuildingType.TEMPLE, 3,
                "Faith made stone. Hosts Idols and Witches."));
        d.add(building("Port", BuildingType.PORT, 3,
                "Trade by water. Must sit beside the sea."));
        d.add(attachment("Worker", AttachmentType.WORKER, 1,
                "Labours in a City for extra gold."));
        d.add(attachment("Worker", AttachmentType.WORKER, 1,
                "Labours in a City for extra gold."));
        d.add(attachment("Idol", AttachmentType.IDOL, 1,
                "Draws an extra card each turn from a Temple."));
        d.add(attachment("Witch", AttachmentType.WITCH, 2,
                "Channels command points from a Temple or City."));
        d.add(military("City Guard", 2, 2, false,
                "Defensive militia."));
        d.add(economy("Tribute", 2, ResourceType.GOLD, 3, false,
                "Vassals pay in gold."));
        d.add(economy("Decree", 2, ResourceType.COMMAND_POINTS, 2, false,
                "Issue a royal decree for command."));
        d.add(explore("Scout", 1, 1,
                "Reveal nearby tiles."));
        return d;
    }

    private static List<Card> unfaithfulDeck() {
        List<Card> d = new ArrayList<Card>();
        // power now: strong military/economy flagged "powerful" (triggers Sin)
        d.add(building("City", BuildingType.CITY, 4,
                "A seat of population. Hosts Workers and Witches."));
        d.add(building("Farm", BuildingType.FARM, 2,
                "Steady gold from the land."));
        d.add(building("Temple", BuildingType.TEMPLE, 3,
                "Unholy faith made stone. Hosts Witches."));
        d.add(attachment("Worker", AttachmentType.WORKER, 1,
                "Labours in a City for extra gold."));
        d.add(attachment("Witch", AttachmentType.WITCH, 2,
                "Channels command points from a Temple or City."));
        d.add(military("Skirmish", 2, 3, false,
                "A quick raid. No lasting cost."));
        d.add(military("Raid", 3, 4, true,
                "A brutal raid. Adds a Sin to your deck."));
        d.add(military("Warband", 4, 5, true,
                "Overwhelming force. Adds a Sin to your deck."));
        d.add(economy("Toll", 2, ResourceType.GOLD, 2, false,
                "Extort a modest toll."));
        d.add(economy("Plunder", 3, ResourceType.GOLD, 5, true,
                "Seize great wealth. Adds a Sin to your deck."));
        d.add(economy("Dark Pact", 3, ResourceType.COMMAND_POINTS, 3, true,
                "Bargain for power. Adds a Sin to your deck."));
        d.add(explore("Scout", 1, 1,
                "Reveal nearby tiles."));
        return d;
    }

    // ---- private per-card factories ----

    private static Card building(String name, BuildingType bt, int cost,
                                 String desc) {
        return new Card(SEQ.next(), name, CardType.BUILDING, cost,
                bt, null, 0, 0, null, false, desc);
    }

    private static Card attachment(String name, AttachmentType at, int cost,
                                   String desc) {
        return new Card(SEQ.next(), name, CardType.ATTACHMENT, cost,
                null, at, 0, 0, null, false, desc);
    }

    private static Card military(String name, int cost, int attack,
                                 boolean powerful, String desc) {
        return new Card(SEQ.next(), name, CardType.MILITARY, cost,
                null, null, attack, 0, null, powerful, desc);
    }

    private static Card economy(String name, int cost, ResourceType res,
                                int value, boolean powerful, String desc) {
        return new Card(SEQ.next(), name, CardType.ECONOMY, cost,
                null, null, 0, value, res, powerful, desc);
    }

    private static Card explore(String name, int cost, int radius, String desc) {
        return new Card(SEQ.next(), name, CardType.EXPLORE, cost,
                null, null, 0, radius, null, false, desc);
    }
}
