package com.whim.albion.api;

/**
 * Bundle of the mutable model pieces the engine (Task 2) coordinates. Created by
 * {@link ModelFactory}. The engine navigates {@link #world()}, reads/mutates
 * {@link #party()}, resolves ids via {@link #content()}, and tracks quest/flag
 * state through {@link #journal()}.
 */
public interface GameModel {

    WorldModel world();
    PartyModel party();
    Content content();
    JournalModel journal();

    /** Mutable journal/quest/flag store, exposing the read-only view for the UI. */
    interface JournalModel extends Views.JournalView {
        boolean flag(String key);
        void setFlag(String key, boolean value);
        void startQuest(String questId, String title, String firstObjective);
        void addObjective(String questId, String objective);
        void completeQuest(String questId);
    }
}
