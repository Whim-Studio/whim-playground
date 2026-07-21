package com.railroad.model;

import java.awt.Color;

/**
 * The cargo classes hauled in Phase 2. Kept deliberately small but real: the two
 * "people" cargoes towns produce ({@link #PASSENGERS}, {@link #MAIL}) plus a
 * single working freight production chain — {@link #COAL} (a raw resource mined
 * near hills/mountains) is delivered to a steel mill, which turns it into
 * {@link #STEEL} (a manufactured good hauled onward to towns).
 *
 * <p>{@code baseValue} is the revenue earned per carload per tile of track the
 * cargo is carried before delivery; see {@link Economy#deliveryRevenue}. Higher
 * value cargoes (mail, steel) pay more than bulk coal, mirroring the 1990 game's
 * "premium vs bulk" split without copying its exact numbers.
 */
public enum CargoType {

    PASSENGERS("Passengers", 55, new Color(240, 220, 90)),
    MAIL("Mail", 65, new Color(230, 130, 200)),
    COAL("Coal", 30, new Color(40, 40, 40)),
    STEEL("Steel", 80, new Color(180, 190, 210));

    private final String label;
    private final int baseValue;
    private final Color color;

    CargoType(String label, int baseValue, Color color) {
        this.label = label;
        this.baseValue = baseValue;
        this.color = color;
    }

    /** Human-readable name for HUD/legend. */
    public String getLabel() {
        return label;
    }

    /** Revenue per carload per tile carried; feeds {@link Economy#deliveryRevenue}. */
    public int getBaseValue() {
        return baseValue;
    }

    /** Display colour for load/legend markers. */
    public Color getColor() {
        return color;
    }
}
