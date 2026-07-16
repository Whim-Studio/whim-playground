package com.whim.tippingpoint.domain;

public final class CitizenCard extends Card {
    private final CitizenType type;
    public CitizenCard(String id, String name, CitizenType type) { super(id, name, type + " citizen"); this.type = type; }
    public CitizenType getType() { return type; }
    public boolean isFarmer() { return type == CitizenType.FARMER; }
}
