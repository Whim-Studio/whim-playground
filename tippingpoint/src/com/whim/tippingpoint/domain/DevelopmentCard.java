package com.whim.tippingpoint.domain;

public final class DevelopmentCard extends Card {
    private final DevelopmentType type;
    private final int cost, cashFlowDelta, foodDelta, co2Delta;
    public DevelopmentCard(String id, String name, DevelopmentType type,
                           int cost, int cashFlowDelta, int foodDelta, int co2Delta) {
        super(id, name, type + " development");
        this.type=type; this.cost=cost; this.cashFlowDelta=cashFlowDelta; this.foodDelta=foodDelta; this.co2Delta=co2Delta;
    }
    public DevelopmentType getType() { return type; }
    public int getCost() { return cost; }
    public int getCashFlowDelta() { return cashFlowDelta; }
    public int getFoodDelta() { return foodDelta; }
    public int getCo2Delta() { return co2Delta; }
}
