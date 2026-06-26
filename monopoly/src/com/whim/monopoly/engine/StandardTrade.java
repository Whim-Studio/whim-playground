package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable trade payload. All-args constructor in field order matching the
 * {@link Trade} accessor order in the contract.
 */
public final class StandardTrade implements Trade {
    private final Player proposer;
    private final Player recipient;
    private final int proposerCash;
    private final int recipientCash;
    private final Set<Integer> proposerDeeds;
    private final Set<Integer> recipientDeeds;
    private final int proposerJailCards;
    private final int recipientJailCards;

    public StandardTrade(Player proposer, Player recipient,
                         int proposerCash, int recipientCash,
                         Set<Integer> proposerDeeds, Set<Integer> recipientDeeds,
                         int proposerJailCards, int recipientJailCards) {
        this.proposer = proposer;
        this.recipient = recipient;
        this.proposerCash = proposerCash;
        this.recipientCash = recipientCash;
        this.proposerDeeds = proposerDeeds == null
                ? new HashSet<Integer>() : new HashSet<Integer>(proposerDeeds);
        this.recipientDeeds = recipientDeeds == null
                ? new HashSet<Integer>() : new HashSet<Integer>(recipientDeeds);
        this.proposerJailCards = proposerJailCards;
        this.recipientJailCards = recipientJailCards;
    }

    public Player getProposer() { return proposer; }
    public Player getRecipient() { return recipient; }
    public int getProposerCash() { return proposerCash; }
    public int getRecipientCash() { return recipientCash; }
    public Set<Integer> getProposerDeeds() { return Collections.unmodifiableSet(proposerDeeds); }
    public Set<Integer> getRecipientDeeds() { return Collections.unmodifiableSet(recipientDeeds); }
    public int getProposerJailCards() { return proposerJailCards; }
    public int getRecipientJailCards() { return recipientJailCards; }
}
