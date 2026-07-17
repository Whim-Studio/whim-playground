package com.whim.xcom.rules.def;

import java.util.Map;

/**
 * A manufacturing project. Requires an unlocking research, engineer-hours, an
 * up-front cost and (optionally) input items; produces one output item.
 */
public interface ManufactureNode extends GameDef {

    /** Research id that must be complete to unlock this project. */
    String requiredResearchId();

    /** Engineer-hours to build one unit. */
    int engineerHours();

    /** Up-front dollar cost per unit. */
    int costDollars();

    /** Workshop space consumed while the project is queued. */
    int workspace();

    /** Required input items (id → quantity) consumed per unit built. */
    Map<String, Integer> inputs();

    /** Produced item id and quantity. */
    String outputItemId();

    int outputQuantity();
}
