package com.whim.cardwoven.domain;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.CardType;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Views.CardView;

/**
 * Concrete card. Immutable data object implementing the read-only
 * {@link CardView} the UI sees.
 *
 * <p>Engine-facing extras (not on {@link CardView}, only visible to code that
 * imports {@code domain}): {@link #value()} carries the generic magnitude for
 * non-military cards — ECONOMY gain amount, EXPLORE reveal radius — and
 * {@link #economyResource()} names which resource an ECONOMY card grants. The
 * UI never needs these; only the engine does.</p>
 */
public final class Card implements CardView {

    private final int id;
    private final String name;
    private final CardType type;
    private final int cost;
    private final BuildingType buildingType;   // BUILDING cards only, else null
    private final AttachmentType attachmentType; // ATTACHMENT cards only, else null
    private final int attack;                  // MILITARY cards only, else 0
    private final int value;                   // ECONOMY gain / EXPLORE radius, else 0
    private final ResourceType economyResource; // ECONOMY cards: what they grant, else null
    private final boolean powerful;            // triggers Sin for The Unfaithful
    private final String description;

    public Card(int id, String name, CardType type, int cost,
                BuildingType buildingType, AttachmentType attachmentType,
                int attack, int value, ResourceType economyResource,
                boolean powerful, String description) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.cost = cost;
        this.buildingType = buildingType;
        this.attachmentType = attachmentType;
        this.attack = attack;
        this.value = value;
        this.economyResource = economyResource;
        this.powerful = powerful;
        this.description = description;
    }

    // --- CardView (read-only, UI-visible) ---
    public int id() { return id; }
    public String name() { return name; }
    public CardType type() { return type; }
    public int cost() { return cost; }
    public BuildingType buildingType() { return buildingType; }
    public AttachmentType attachmentType() { return attachmentType; }
    public int attack() { return attack; }
    public String description() { return description; }

    // --- domain/engine-only extras ---

    /** Generic magnitude: ECONOMY gain amount, EXPLORE reveal radius, else 0. */
    public int value() { return value; }

    /** ECONOMY cards: the resource granted (GOLD / COMMAND_POINTS). Else null. */
    public ResourceType economyResource() { return economyResource; }

    /**
     * Whether playing this card is "powerful" — the trigger The Unfaithful's
     * {@code SinLogic} uses to inject SIN cards. Never true for SIN cards.
     */
    public boolean isPowerful() { return powerful; }

    @Override
    public String toString() {
        return "Card#" + id + "(" + name + "," + type + ",cost=" + cost + ")";
    }
}
