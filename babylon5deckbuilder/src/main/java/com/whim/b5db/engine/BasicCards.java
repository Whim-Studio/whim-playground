package com.whim.b5db.engine;

import com.whim.b5db.model.Card;
import com.whim.b5db.model.CardType;
import com.whim.b5db.model.ContestType;
import com.whim.b5db.model.Effect;
import com.whim.b5db.model.Faction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hardcoded cards that must always exist regardless of the JSON catalogue: the
 * generic 10-card starter deck, the always-available CENTRAL_CORRIDOR piles, and
 * the cost-0 faction Ambassador Heroes that begin in each player's COMMAND_ROW.
 * Keeping these in code guarantees the game is playable even with an empty
 * {@code assets/cards} directory.
 */
public final class BasicCards {

    private BasicCards() {
    }

    public static final Card CREDIT_CHIT = economy("credit_chit", "Credit Chit", 1);
    public static final Card PATROL_SHIP = fleet("patrol_ship", "Patrol Ship", Faction.NON_ALIGNED, 1, 2);
    public static final Card MINOR_DIPLOMAT = character("minor_diplomat", "Minor Diplomat",
            Faction.NON_ALIGNED, 0, attr(ContestType.DIPLOMACY, 1));
    public static final Card JUNIOR_OFFICER = character("junior_officer", "Junior Officer",
            Faction.NON_ALIGNED, 0, attr(ContestType.MILITARY, 1));

    public static final Card LONDO = hero("londo", "Londo Mollari", Faction.CENTAURI_REPUBLIC,
            attrs(5, 6, 4, 0), effect(Effect.Type.GAIN_INFLUENCE, 1));
    public static final Card GKAR = hero("gkar", "G'Kar", Faction.NARN_REGIME,
            attrs(4, 5, 3, 0), effect(Effect.Type.GAIN_PRESTIGE, 1));
    public static final Card DELENN = hero("delenn", "Delenn", Faction.MINBARI_FEDERATION,
            attrs(7, 3, 5, 2), effect(Effect.Type.GAIN_INFLUENCE, 1));
    public static final Card SINCLAIR = hero("sinclair", "Jeffrey Sinclair", Faction.EARTH_ALLIANCE,
            attrs(5, 3, 4, 0), effect(Effect.Type.GAIN_INFLUENCE, 1));

    /** The identical 10-card starter deck each player begins with. */
    public static List<Card> starterDeck() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            deck.add(CREDIT_CHIT);
        }
        deck.add(MINOR_DIPLOMAT);
        deck.add(MINOR_DIPLOMAT);
        deck.add(JUNIOR_OFFICER);
        return deck;
    }

    /** The static supply piles reachable every turn for a fixed INFLUENCE cost. */
    public static List<Card> corridorPiles() {
        return new ArrayList<>(Arrays.asList(CREDIT_CHIT, PATROL_SHIP));
    }

    /** Faction Ambassador Hero(es) placed directly in COMMAND_ROW at setup. */
    public static List<Card> ambassadorsFor(Faction faction) {
        switch (faction) {
            case CENTAURI_REPUBLIC: return one(LONDO);
            case NARN_REGIME: return one(GKAR);
            case MINBARI_FEDERATION: return one(DELENN);
            case EARTH_ALLIANCE: return one(SINCLAIR);
            default: return new ArrayList<>();
        }
    }

    /** Every hardcoded card, for building a global id-&gt;card index. */
    public static List<Card> allBasics() {
        return new ArrayList<>(Arrays.asList(
                CREDIT_CHIT, PATROL_SHIP, MINOR_DIPLOMAT, JUNIOR_OFFICER,
                LONDO, GKAR, DELENN, SINCLAIR));
    }

    // --- small builders ---

    private static List<Card> one(Card c) {
        List<Card> list = new ArrayList<>();
        list.add(c);
        return list;
    }

    private static List<Effect> effect(Effect.Type type, int amount) {
        List<Effect> fx = new ArrayList<>();
        fx.add(new Effect(type, amount, null));
        return fx;
    }

    private static Map<ContestType, Integer> attr(ContestType t, int v) {
        Map<ContestType, Integer> m = new EnumMap<>(ContestType.class);
        m.put(t, v);
        return m;
    }

    private static Map<ContestType, Integer> attrs(int dip, int intr, int mil, int psi) {
        Map<ContestType, Integer> m = new EnumMap<>(ContestType.class);
        if (dip != 0) m.put(ContestType.DIPLOMACY, dip);
        if (intr != 0) m.put(ContestType.INTRIGUE, intr);
        if (mil != 0) m.put(ContestType.MILITARY, mil);
        if (psi != 0) m.put(ContestType.PSI, psi);
        return m;
    }

    private static Card economy(String id, String name, int cost) {
        List<Effect> fx = new ArrayList<>();
        fx.add(new Effect(Effect.Type.GAIN_INFLUENCE, 1, null));
        return new Card(id, name, Faction.NON_ALIGNED, CardType.ECONOMY, cost, 0,
                null, null, 0, fx, "Basic economy: +1 Influence.");
    }

    private static Card fleet(String id, String name, Faction f, int cost, int mil) {
        return new Card(id, name, f, CardType.FLEET, cost, 0,
                attr(ContestType.MILITARY, mil), null, 0,
                Collections.<Effect>emptyList(), "Permanent fleet: +" + mil + " Military each turn.");
    }

    private static Card character(String id, String name, Faction f, int cost,
                                  Map<ContestType, Integer> a) {
        return new Card(id, name, f, CardType.CHARACTER, cost, 0, a, null, 0,
                Collections.<Effect>emptyList(), "");
    }

    private static Card hero(String id, String name, Faction f,
                             Map<ContestType, Integer> a, List<Effect> fx) {
        return new Card(id, name, f, CardType.AMBASSADOR_HERO, 0, 0, a, null, 0, fx,
                "Ambassador Hero: begins in play.");
    }
}
