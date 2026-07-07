package com.whim.albion.data;

import com.whim.albion.api.Content;
import com.whim.albion.api.GameModel;
import com.whim.albion.api.PartyModel;
import com.whim.albion.api.WorldModel;

/** Simple immutable bundle of the four model pieces created by the factory. */
public final class AlbionGameModel implements GameModel {

    private final WorldModel world;
    private final PartyModel party;
    private final Content content;
    private final JournalModel journal;

    public AlbionGameModel(WorldModel world, PartyModel party, Content content, JournalModel journal) {
        this.world = world;
        this.party = party;
        this.content = content;
        this.journal = journal;
    }

    @Override public WorldModel world() { return world; }
    @Override public PartyModel party() { return party; }
    @Override public Content content() { return content; }
    @Override public JournalModel journal() { return journal; }
}
