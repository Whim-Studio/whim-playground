package com.whim.tippingpoint.domain;

public enum RiskFactor {
    X1(1), X2(2), X3(3);
    private final int multiplier;
    RiskFactor(int m) { this.multiplier = m; }
    public int multiplier() { return multiplier; }
    // co2 -> risk factor, per Rules thresholds
    public static RiskFactor forCo2(int co2) {
        if (co2 >= Rules.RISK_X3_AT) return X3;
        if (co2 >= Rules.RISK_X2_AT) return X2;
        return X1;
    }
}
