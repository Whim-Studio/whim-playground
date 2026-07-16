package com.whim.tippingpoint.domain;

public final class WeatherCard extends Card {
    private final WeatherSeverity severity;
    private final int citizensLost, cashLost, foodProductionLost;
    public WeatherCard(String id, String name, WeatherSeverity severity,
                       int citizensLost, int cashLost, int foodProductionLost) {
        super(id, name, severity + " weather");
        this.severity=severity; this.citizensLost=citizensLost; this.cashLost=cashLost; this.foodProductionLost=foodProductionLost;
    }
    public WeatherSeverity getSeverity() { return severity; }
    public int getCitizensLost() { return citizensLost; }
    public int getCashLost() { return cashLost; }
    public int getFoodProductionLost() { return foodProductionLost; }
}
