package com.whim.cardwoven.domain;

import com.whim.cardwoven.api.Enums.AttachmentType;
import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Enums.ResourceType;
import com.whim.cardwoven.api.Views.AttachmentView;
import com.whim.cardwoven.api.Views.CardView;

/**
 * An attachment bound to a building. The yield it contributes is derived from
 * the attachment card's type:
 * <ul>
 *   <li>WORKER &rarr; +Gold (paid off on Cities)</li>
 *   <li>IDOL &rarr; +card draw (paid off on Temples)</li>
 *   <li>WITCH &rarr; +Command Points (paid off on Temples/Cities)</li>
 * </ul>
 * Yield amounts depend on the attachment type alone; which building types the
 * attachment may legally bind to is a separate rule the engine enforces via
 * {@link #isLegal(AttachmentType, BuildingType)} before attaching.
 */
public final class Attachment implements AttachmentView {

    private static final int WORKER_GOLD = 2;
    private static final int WITCH_COMMAND = 1;
    private static final int IDOL_DRAW = 1;

    private final Card card;
    private final AttachmentType type;
    private final ResourceType yieldResource; // may be null (Idol draws instead)
    private final int yieldAmount;
    private final int bonusDraw;

    /** Derive yield from the attachment card's type. */
    public Attachment(Card card) {
        this.card = card;
        this.type = card.attachmentType();
        ResourceType res = null;
        int amount = 0;
        int draw = 0;
        if (type == AttachmentType.WORKER) {
            res = ResourceType.GOLD;
            amount = WORKER_GOLD;
        } else if (type == AttachmentType.IDOL) {
            draw = IDOL_DRAW;
        } else if (type == AttachmentType.WITCH) {
            res = ResourceType.COMMAND_POINTS;
            amount = WITCH_COMMAND;
        }
        this.yieldResource = res;
        this.yieldAmount = amount;
        this.bonusDraw = draw;
    }

    /** Whether an attachment type may legally bind to a building type. */
    public static boolean isLegal(AttachmentType at, BuildingType host) {
        if (at == null || host == null) {
            return false;
        }
        if (at == AttachmentType.WORKER) {
            return host == BuildingType.CITY;
        }
        if (at == AttachmentType.IDOL) {
            return host == BuildingType.TEMPLE;
        }
        // WITCH
        return host == BuildingType.TEMPLE || host == BuildingType.CITY;
    }

    // --- AttachmentView ---
    public CardView card() { return card; }
    public AttachmentType type() { return type; }
    public ResourceType yieldResource() { return yieldResource; }
    public int yieldAmount() { return yieldAmount; }
    public int bonusDraw() { return bonusDraw; }

    @Override
    public String toString() {
        return "Attachment(" + type + ",res=" + yieldResource
                + ",amt=" + yieldAmount + ",draw=" + bonusDraw + ")";
    }
}
