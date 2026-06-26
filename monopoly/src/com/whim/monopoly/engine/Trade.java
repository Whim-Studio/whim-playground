package com.whim.monopoly.engine;

import com.whim.monopoly.domain.Player;

// Optional trade payload (engine validates conservation + ownership).
public interface Trade {
    Player getProposer();
    Player getRecipient();
    int getProposerCash();               // cash proposer gives recipient
    int getRecipientCash();              // cash recipient gives proposer
    java.util.Set<Integer> getProposerDeeds();   // ownable indices proposer gives
    java.util.Set<Integer> getRecipientDeeds();  // ownable indices recipient gives
    int getProposerJailCards();
    int getRecipientJailCards();
}
