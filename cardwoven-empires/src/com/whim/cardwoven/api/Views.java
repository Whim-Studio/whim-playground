package com.whim.cardwoven.api;

import java.util.List;
import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.Faction;
import com.whim.cardwoven.api.Enums.GamePhase;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Enums.TerrainType;
import com.whim.cardwoven.api.Enums.VictoryType;

/**
 * READ-ONLY view interfaces exposed to the UI (Task 3) and consumed by the
 * engine (Task 2). The domain layer (Task 1) provides concrete implementations.
 *
 * Contract rule: the UI reads ONLY these interfaces and never casts to a
 * concrete domain class. The engine mutates concrete domain objects but returns
 * these views. This keeps the three layers decoupled.
 */
public final class Views {
    private Views() {}

    /** A single card, whether in deck, hand, discard, or attached. */
    public interface CardView {
        int id();
        String name();
        CardType type();
        /** Gold cost to play from hand. 0 for free/sin cards. */
        int cost();
        /** BUILDING cards: what they place. Otherwise null. */
        BuildingType buildingType();
        /** ATTACHMENT cards: what they attach as. Otherwise null. */
        AttachmentType attachmentType();
        /** MILITARY cards: attack strength. Otherwise 0. */
        int attack();
        /** Short flavour / rules text for tooltips. */
        String description();
    }

    /** An attachment card bound to a building, contributing a yield buff. */
    public interface AttachmentView {
        CardView card();
        AttachmentType type();
        /** Resource this attachment yields per turn (may be null for draw). */
        ResourceType yieldResource();
        /** Amount of the resource yielded per turn. */
        int yieldAmount();
        /** Extra cards drawn per turn (Idols). */
        int bonusDraw();
    }

    /** A building placed on a tile, holding nested attachments. */
    public interface BuildingView {
        int id();
        BuildingType type();
        int row();
        int col();
        int ownerPlayerIndex();
        int defense();
        List<AttachmentView> attachments();
        /** Max attachments this building can hold. */
        int attachmentCapacity();
    }

    /** A single map tile. */
    public interface TileView {
        int row();
        int col();
        TerrainType terrain();
        boolean explored();
        /** Building on this tile, or null. */
        BuildingView building();
        /** Neutral raider strength menacing this tile, 0 if none. */
        int raiderStrength();
    }

    /** The grid map. */
    public interface MapView {
        int rows();
        int cols();
        TileView tile(int row, int col);
        List<BuildingView> buildingsOf(int playerIndex);
    }

    /** A player's public + owning state. */
    public interface PlayerView {
        int index();
        Faction faction();
        String name();
        boolean isHuman();
        int resource(ResourceType type);
        int deckCount();
        int discardCount();
        int handSize();
        List<CardView> hand();
        /** Progress 0..1 toward each victory type this faction pursues. */
        double victoryProgress(VictoryType type);
        List<VictoryType> pursuableVictories();
    }

    /** Whole-game snapshot the UI renders from. */
    public interface GameStateView {
        MapView map();
        List<PlayerView> players();
        int currentPlayerIndex();
        PlayerView currentPlayer();
        int turnNumber();
        GamePhase phase();
        boolean isGameOver();
        int winnerPlayerIndex();          // -1 if none
        VictoryType winningVictory();     // null if none
        /** Rolling log of recent events for the UI feed. */
        List<String> recentLog();
    }
}
