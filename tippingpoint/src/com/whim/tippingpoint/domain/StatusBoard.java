package com.whim.tippingpoint.domain;

/** Per-player status board: cashFlow (income), foodProduction, and this city's co2. */
public final class StatusBoard {
    private int cashFlow;
    private int foodProduction;
    private int co2;

    public StatusBoard() {
        this.cashFlow = Rules.START_CASH_FLOW;
        this.foodProduction = Rules.START_FOOD;
        this.co2 = 0;
    }

    public int getCashFlow() { return cashFlow; }
    public int getFoodProduction() { return foodProduction; }
    public int getCo2() { return co2; }
    public RiskFactor getRiskFactor() { return RiskFactor.forCo2(co2); }

    public void applyDevelopment(DevelopmentCard c) {
        cashFlow += c.getCashFlowDelta();
        foodProduction += c.getFoodDelta();
        co2 = Math.max(0, co2 + c.getCo2Delta());
    }

    public void addFoodProduction(int d) {
        foodProduction += d;
        if (foodProduction < 0) foodProduction = 0;
    }

    public void loseFoodProduction(int d) {
        foodProduction -= d;
        if (foodProduction < 0) foodProduction = 0;
    }

    public void setCo2(int v) {
        co2 = Math.max(0, v);
    }
}
