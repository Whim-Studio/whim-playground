package com.whim.ttr.domain;

import com.whim.ttr.api.CardColor;
import com.whim.ttr.api.GameConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The train-car draw pile plus the 5-card face-up market and the destination
 * ticket deck. Deterministic given a seed so tests are reproducible.
 *
 * <ul>
 *   <li>110 train cards: 12 of each of the 8 colors + 14 LOCOMOTIVEs.</li>
 *   <li>Drawing from an empty pile reshuffles the discards back in.</li>
 *   <li>The face-up market auto-redeals whenever 3 LOCOMOTIVEs show at once.</li>
 * </ul>
 */
public final class Deck {

    private final Random rng;
    private final List<CardColor> drawPile = new ArrayList<CardColor>();
    private final List<CardColor> discardPile = new ArrayList<CardColor>();
    private final List<CardColor> faceUp = new ArrayList<CardColor>();
    private final List<DestinationTicket> ticketPile = new ArrayList<DestinationTicket>();

    public Deck(long seed) {
        this.rng = new Random(seed);
        buildTrainDeck();
        Collections.shuffle(drawPile, rng);
        buildTicketDeck();
        Collections.shuffle(ticketPile, rng);
        refillFaceUp();
    }

    private void buildTrainDeck() {
        for (CardColor c : CardColor.trainColors()) {
            for (int i = 0; i < GameConstants.CARDS_PER_COLOR; i++) {
                drawPile.add(c);
            }
        }
        for (int i = 0; i < GameConstants.LOCOMOTIVE_CARDS; i++) {
            drawPile.add(CardColor.LOCOMOTIVE);
        }
    }

    private void buildTicketDeck() {
        ticketPile.addAll(Tickets.official());
    }

    // ---- train cards -------------------------------------------------------

    /** Draw a card blind from the top of the pile, reshuffling discards if empty. */
    public CardColor draw() {
        if (drawPile.isEmpty()) {
            reshuffleDiscards();
        }
        if (drawPile.isEmpty()) {
            return null; // no cards anywhere
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    public void discard(CardColor c) {
        if (c != null) {
            discardPile.add(c);
        }
    }

    private void reshuffleDiscards() {
        if (discardPile.isEmpty()) return;
        drawPile.addAll(discardPile);
        discardPile.clear();
        Collections.shuffle(drawPile, rng);
    }

    /** Number of cards remaining across draw + discard piles (excludes face-up). */
    public int cardsRemaining() {
        return drawPile.size() + discardPile.size();
    }

    // ---- face-up market ----------------------------------------------------

    /**
     * The LIVE, mutable market list (per contract addendum). The engine takes a
     * face-up card by removing element {@code i} and then calling
     * {@link #refillFaceUp()}; returning the backing list makes that work.
     */
    public List<CardColor> faceUp() {
        return faceUp;
    }

    /** Take the face-up card at {@code index}, leaving a gap that a refill fills. */
    public CardColor takeFaceUp(int index) {
        if (index < 0 || index >= faceUp.size()) return null;
        CardColor c = faceUp.remove(index);
        refillFaceUp();
        return c;
    }

    /**
     * Top the market back up to {@link GameConstants#FACE_UP_SLOTS}. If, once
     * full, {@link GameConstants#FACE_UP_LOCO_RESHUFFLE} or more LOCOMOTIVEs are
     * showing, discard all five and redeal — repeating while the deck can still
     * provide a legal spread.
     */
    public void refillFaceUp() {
        fillSlots();
        // Auto-redeal on too many locomotives, but only while a redeal is possible.
        int guard = 0;
        while (countLocomotives() >= GameConstants.FACE_UP_LOCO_RESHUFFLE
                && (cardsRemaining() + faceUp.size()) >= GameConstants.FACE_UP_SLOTS
                && guard < 20) {
            discardPile.addAll(faceUp);
            faceUp.clear();
            fillSlots();
            guard++;
        }
    }

    private void fillSlots() {
        while (faceUp.size() < GameConstants.FACE_UP_SLOTS) {
            CardColor c = draw();
            if (c == null) break;
            faceUp.add(c);
        }
    }

    private int countLocomotives() {
        int n = 0;
        for (CardColor c : faceUp) {
            if (c == CardColor.LOCOMOTIVE) n++;
        }
        return n;
    }

    // ---- destination tickets -----------------------------------------------

    /** The remaining (shuffled) destination-ticket pile. */
    public List<DestinationTicket> ticketDeck() {
        return Collections.unmodifiableList(new ArrayList<DestinationTicket>(ticketPile));
    }

    /** Draw the top destination ticket, or null if the ticket pile is empty. */
    public DestinationTicket drawTicket() {
        if (ticketPile.isEmpty()) return null;
        return ticketPile.remove(ticketPile.size() - 1);
    }

    public int ticketsRemaining() {
        return ticketPile.size();
    }
}
