package com.whim.alganon.ui.stub;

import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Views;

/** Mutable ability projection for the stub (tracks a live cooldown so the sweep animates). */
final class StubAbility implements Views.AbilityView {
    final String id, name, description;
    final int cost;
    final double cooldownSec;
    final AbilityKind kind;
    double cooldownRemaining = 0;

    StubAbility(Defs.AbilityDef d) {
        this.id = d.id; this.name = d.name; this.description = d.description;
        this.cost = d.resourceCost; this.cooldownSec = d.cooldownSec; this.kind = d.kind;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String description() { return description; }
    public int resourceCost() { return cost; }
    public double cooldownSec() { return cooldownSec; }
    public double cooldownRemaining() { return cooldownRemaining; }
    public boolean usable() { return cooldownRemaining <= 0; }
    public AbilityKind kind() { return kind; }
}
