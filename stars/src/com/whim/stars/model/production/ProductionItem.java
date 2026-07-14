package com.whim.stars.model.production;

import java.io.Serializable;

import com.whim.stars.model.ship.ShipDesign;

/**
 * One entry in a planet's production queue. An entry has a kind, a remaining
 * quantity, and — for open-ended AUTO entries — a flag that keeps it in the
 * queue year after year. The turn engine spends resources and minerals down the
 * queue, carrying partial progress between years via {@link #partialResources}.
 */
public final class ProductionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind {
        FACTORY("Factory"),
        MINE("Mine"),
        DEFENSE("Defense"),
        PLANETARY_SCANNER("Planetary Scanner"),
        SHIP("Ship"),
        AUTO_FACTORY("Auto: Factories to Max"),
        AUTO_MINE("Auto: Mines to Max");

        private final String label;
        Kind(String label) { this.label = label; }
        public String label() { return label; }
        public boolean isAuto() { return this == AUTO_FACTORY || this == AUTO_MINE; }
    }

    private final Kind kind;
    private int quantity;              // remaining count (ignored for auto kinds)
    private final ShipDesign design;   // non-null only for SHIP
    private int partialResources;      // resources banked toward the next unit

    private ProductionItem(Kind kind, int quantity, ShipDesign design) {
        this.kind = kind;
        this.quantity = quantity;
        this.design = design;
    }

    public static ProductionItem of(Kind kind, int quantity) {
        return new ProductionItem(kind, quantity, null);
    }

    public static ProductionItem ship(ShipDesign design, int quantity) {
        return new ProductionItem(Kind.SHIP, quantity, design);
    }

    public static ProductionItem auto(Kind autoKind) {
        return new ProductionItem(autoKind, 0, null);
    }

    public Kind kind() { return kind; }
    public int quantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = Math.max(0, q); }
    public void decrement() { if (quantity > 0) quantity--; }
    public ShipDesign design() { return design; }

    public int partialResources() { return partialResources; }
    public void setPartialResources(int p) { this.partialResources = Math.max(0, p); }

    public boolean isComplete() {
        return !kind.isAuto() && quantity <= 0;
    }

    public String label() {
        if (kind == Kind.SHIP && design != null) {
            return design.name() + " x" + quantity;
        }
        if (kind.isAuto()) {
            return kind.label();
        }
        return kind.label() + " x" + quantity;
    }
}
