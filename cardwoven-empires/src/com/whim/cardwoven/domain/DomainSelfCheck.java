package com.whim.cardwoven.domain;

import java.util.List;
import java.util.Random;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.ResourceType;

/**
 * Headless self-check for the domain layer. No test framework required: run
 * {@code java com.whim.cardwoven.domain.DomainSelfCheck} and it exits non-zero
 * on the first failed assertion, printing what it verified.
 *
 * Proves: seeded shuffle determinism, draw + discard reshuffle, attachment-buff
 * yield math, and a coherent {@link GameState#create} factory.
 */
public final class DomainSelfCheck {

    private static int checks = 0;

    public static void main(String[] args) {
        testShuffleDeterminism();
        testDrawAndReshuffle();
        testAttachmentBuffMath();
        testBuildingCapacity();
        testResources();
        testGameStateFactory();
        System.out.println("DomainSelfCheck: ALL " + checks + " CHECKS PASSED");
    }

    private static void testShuffleDeterminism() {
        List<Card> a = CardLibrary.startingDeck(Faction.LANDS_OF_THE_KING);
        List<Card> b = CardLibrary.startingDeck(Faction.LANDS_OF_THE_KING);
        Deck d1 = new Deck(new Random(42L), a);
        Deck d2 = new Deck(new Random(42L), b);
        d1.shuffle();
        d2.shuffle();
        // same seed => same order of card *names* (ids differ by mint sequence)
        List<Card> c1 = d1.cards();
        List<Card> c2 = d2.cards();
        check(c1.size() == c2.size() && c1.size() == 14,
                "deck size after shuffle == 14");
        for (int i = 0; i < c1.size(); i++) {
            check(c1.get(i).name().equals(c2.get(i).name()),
                    "same-seed shuffle order matches at " + i);
        }
        // a different seed should (very likely) differ somewhere
        Deck d3 = new Deck(new Random(7L),
                CardLibrary.startingDeck(Faction.LANDS_OF_THE_KING));
        d3.shuffle();
        boolean anyDiff = false;
        List<Card> c3 = d3.cards();
        for (int i = 0; i < c1.size(); i++) {
            if (!c1.get(i).name().equals(c3.get(i).name())) {
                anyDiff = true;
                break;
            }
        }
        check(anyDiff, "different seed yields a different order");
    }

    private static void testDrawAndReshuffle() {
        Random rng = new Random(1L);
        Deck deck = new Deck(rng, CardLibrary.startingDeck(Faction.BABYLON));
        deck.shuffle();
        DiscardPile discard = new DiscardPile();
        PlayerState p = new PlayerState(0, Faction.BABYLON, "P", true,
                deck, discard);
        int total = deck.size();
        check(total == 13, "Babylon starting deck size == 13");

        // draw the whole deck into hand, discard each -> deck empties
        for (int i = 0; i < total; i++) {
            Card c = p.drawOne();
            check(c != null, "draw " + i + " non-null");
            p.discardFromHand(c);
        }
        check(p.deck().isEmpty(), "deck empty after drawing all");
        check(p.discard().size() == total, "discard holds all " + total);
        check(p.handSize() == 0, "hand empty after discarding all");

        // next draw must auto-reshuffle discard back into the deck
        Card again = p.drawOne();
        check(again != null, "draw after empty deck auto-reshuffles (non-null)");
        check(p.discard().isEmpty(), "discard emptied by reshuffle");
        check(p.deck().size() == total - 1,
                "deck refilled minus the one just drawn");
    }

    private static void testAttachmentBuffMath() {
        // WORKER on CITY -> +2 gold
        Building city = new Building(1, BuildingType.CITY, 0, 0, 0);
        int cityBaseGold = Building.baseGoldYield(BuildingType.CITY); // 1
        Card worker = new Card(100, "Worker", CardType.ATTACHMENT, 1, null,
                AttachmentType.WORKER, 0, 0, null, false, "");
        check(Attachment.isLegal(AttachmentType.WORKER, BuildingType.CITY),
                "worker legal on city");
        city.addAttachment(new Attachment(worker));
        check(city.goldYield() == cityBaseGold + 2,
                "city gold yield = base(" + cityBaseGold + ") + worker(2)");
        check(city.commandYield() == 0, "worker adds no command");
        check(city.bonusDraw() == 0, "worker adds no draw");

        // WITCH on CITY -> +1 command
        Card witch = new Card(101, "Witch", CardType.ATTACHMENT, 2, null,
                AttachmentType.WITCH, 0, 0, null, false, "");
        check(Attachment.isLegal(AttachmentType.WITCH, BuildingType.CITY),
                "witch legal on city");
        city.addAttachment(new Attachment(witch));
        check(city.commandYield() == 1, "witch adds +1 command");
        check(city.goldYield() == cityBaseGold + 2, "witch adds no gold");

        // IDOL on TEMPLE -> +1 draw
        Building temple = new Building(2, BuildingType.TEMPLE, 1, 1, 0);
        Card idol = new Card(102, "Idol", CardType.ATTACHMENT, 1, null,
                AttachmentType.IDOL, 0, 0, null, false, "");
        check(Attachment.isLegal(AttachmentType.IDOL, BuildingType.TEMPLE),
                "idol legal on temple");
        check(!Attachment.isLegal(AttachmentType.IDOL, BuildingType.CITY),
                "idol illegal on city");
        temple.addAttachment(new Attachment(idol));
        check(temple.bonusDraw() == 1, "idol adds +1 draw on temple");
        check(temple.goldYield() == Building.baseGoldYield(BuildingType.TEMPLE),
                "idol adds no gold");
    }

    private static void testBuildingCapacity() {
        Building farm = new Building(3, BuildingType.FARM, 0, 0, 0);
        check(farm.attachmentCapacity() == 1, "farm capacity == 1");
        Card w1 = new Card(200, "Worker", CardType.ATTACHMENT, 1, null,
                AttachmentType.WORKER, 0, 0, null, false, "");
        Card w2 = new Card(201, "Worker", CardType.ATTACHMENT, 1, null,
                AttachmentType.WORKER, 0, 0, null, false, "");
        check(farm.addAttachment(new Attachment(w1)), "first attachment fits");
        check(!farm.addAttachment(new Attachment(w2)),
                "second attachment rejected at capacity");
        check(farm.attachmentCount() == 1, "farm holds exactly 1");
    }

    private static void testResources() {
        Resources r = new Resources(5, 1);
        check(r.get(ResourceType.GOLD) == 5, "gold init 5");
        check(r.canAfford(ResourceType.GOLD, 5), "can afford 5");
        check(!r.canAfford(ResourceType.GOLD, 6), "cannot afford 6");
        check(r.spend(ResourceType.GOLD, 3), "spend 3 ok");
        check(r.get(ResourceType.GOLD) == 2, "gold now 2");
        check(!r.spend(ResourceType.GOLD, 5), "overspend rejected");
        check(r.get(ResourceType.GOLD) == 2, "gold unchanged after reject");
        r.add(ResourceType.COMMAND_POINTS, 4);
        check(r.get(ResourceType.COMMAND_POINTS) == 5, "command now 5");
    }

    private static void testGameStateFactory() {
        GameState gs = GameState.create(Faction.THE_UNFAITHFUL, 99L);
        check(gs.playerStates().size() == 3, "3 players created");
        check(gs.playerAt(0).faction() == Faction.THE_UNFAITHFUL,
                "player 0 is chosen human faction");
        check(gs.playerAt(0).isHuman(), "player 0 is human");
        check(!gs.playerAt(1).isHuman(), "player 1 is AI");
        check(gs.gridMap().rows() == GameState.ROWS
                        && gs.gridMap().cols() == GameState.COLS,
                "map is " + GameState.ROWS + "x" + GameState.COLS);
        // opening hands dealt to base hand size
        for (int i = 0; i < 3; i++) {
            PlayerState p = gs.playerAt(i);
            check(p.handSize() == p.profile().baseHandSize(),
                    "player " + i + " dealt base hand of "
                            + p.profile().baseHandSize());
        }
        // raiders seeded on at least a few land tiles
        int raiderTiles = 0;
        for (int r = 0; r < gs.gridMap().rows(); r++) {
            for (int c = 0; c < gs.gridMap().cols(); c++) {
                if (gs.gridMap().tileAt(r, c).raiderStrength() > 0) {
                    raiderTiles++;
                }
            }
        }
        check(raiderTiles >= 4, "at least 4 raider tiles seeded (got "
                + raiderTiles + ")");
        // determinism: same seed => same first draw names for player 0
        GameState gs2 = GameState.create(Faction.THE_UNFAITHFUL, 99L);
        List<com.whim.cardwoven.api.Views.CardView> h1 = gs.playerAt(0).hand();
        List<com.whim.cardwoven.api.Views.CardView> h2 = gs2.playerAt(0).hand();
        check(h1.size() == h2.size(), "same-seed hands same size");
        for (int i = 0; i < h1.size(); i++) {
            check(h1.get(i).name().equals(h2.get(i).name()),
                    "same-seed hand card " + i + " matches");
        }
    }

    private static void check(boolean cond, String label) {
        checks++;
        if (!cond) {
            System.out.println("FAIL: " + label);
            throw new AssertionError("Self-check failed: " + label);
        }
        System.out.println("  ok: " + label);
    }

    private DomainSelfCheck() {}
}
