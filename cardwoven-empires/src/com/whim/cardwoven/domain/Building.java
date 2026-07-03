package com.whim.cardwoven.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.cardwoven.api.Enums.BuildingType;
import com.whim.cardwoven.api.Views.AttachmentView;
import com.whim.cardwoven.api.Views.BuildingView;

/**
 * A building placed on a tile. Holds nested attachments up to a capacity and
 * exposes both the read-only {@link BuildingView} and domain helpers the engine
 * uses to compute per-turn yields (the sum of attachment buff modifiers plus a
 * small base yield for the building type).
 */
public final class Building implements BuildingView {

    private final int id;
    private final BuildingType type;
    private final int row;
    private final int col;
    private final int ownerPlayerIndex;
    private final int defense;
    private final int capacity;
    private final List<Attachment> attachments = new ArrayList<Attachment>();

    public Building(int id, BuildingType type, int row, int col,
                    int ownerPlayerIndex) {
        this(id, type, row, col, ownerPlayerIndex,
                defaultDefense(type), defaultCapacity(type));
    }

    public Building(int id, BuildingType type, int row, int col,
                    int ownerPlayerIndex, int defense, int capacity) {
        this.id = id;
        this.type = type;
        this.row = row;
        this.col = col;
        this.ownerPlayerIndex = ownerPlayerIndex;
        this.defense = defense;
        this.capacity = capacity;
    }

    /** Base defense per building type. */
    public static int defaultDefense(BuildingType type) {
        if (type == BuildingType.CITY) {
            return 3;
        }
        if (type == BuildingType.PORT) {
            return 2;
        }
        return 1; // FARM, TEMPLE
    }

    /** Base attachment capacity per building type. */
    public static int defaultCapacity(BuildingType type) {
        if (type == BuildingType.CITY) {
            return 3;
        }
        if (type == BuildingType.TEMPLE || type == BuildingType.PORT) {
            return 2;
        }
        return 1; // FARM
    }

    /** Base per-turn gold yield for the building itself (before attachments). */
    public static int baseGoldYield(BuildingType type) {
        if (type == BuildingType.FARM) {
            return 2;
        }
        if (type == BuildingType.CITY || type == BuildingType.PORT) {
            return 1;
        }
        return 0; // TEMPLE
    }

    // --- mutation ---

    /** True if another attachment fits. */
    public boolean hasCapacity() {
        return attachments.size() < capacity;
    }

    /** Number of attachments currently bound. */
    public int attachmentCount() {
        return attachments.size();
    }

    /**
     * Add an attachment. Returns false (no-op) if capacity is full. The engine
     * is responsible for legality/cost checks before calling.
     */
    public boolean addAttachment(Attachment a) {
        if (a == null || !hasCapacity()) {
            return false;
        }
        attachments.add(a);
        return true;
    }

    // --- domain yield helpers (engine EconomyCalculator uses these) ---

    /** Total gold yield per turn: base building yield + attachment buffs. */
    public int goldYield() {
        int total = baseGoldYield(type);
        int n = attachments.size();
        for (int i = 0; i < n; i++) {
            Attachment a = attachments.get(i);
            if (a.yieldResource() == com.whim.cardwoven.api.Enums.ResourceType.GOLD) {
                total += a.yieldAmount();
            }
        }
        return total;
    }

    /** Total command-point yield per turn from attachment buffs. */
    public int commandYield() {
        int total = 0;
        int n = attachments.size();
        for (int i = 0; i < n; i++) {
            Attachment a = attachments.get(i);
            if (a.yieldResource()
                    == com.whim.cardwoven.api.Enums.ResourceType.COMMAND_POINTS) {
                total += a.yieldAmount();
            }
        }
        return total;
    }

    /** Total bonus card draws per turn from attachment buffs (Idols). */
    public int bonusDraw() {
        int total = 0;
        int n = attachments.size();
        for (int i = 0; i < n; i++) {
            total += attachments.get(i).bonusDraw();
        }
        return total;
    }

    // --- BuildingView ---
    public int id() { return id; }
    public BuildingType type() { return type; }
    public int row() { return row; }
    public int col() { return col; }
    public int ownerPlayerIndex() { return ownerPlayerIndex; }
    public int defense() { return defense; }
    public int attachmentCapacity() { return capacity; }

    public List<AttachmentView> attachments() {
        return Collections.<AttachmentView>unmodifiableList(
                new ArrayList<AttachmentView>(attachments));
    }

    @Override
    public String toString() {
        return "Building#" + id + "(" + type + "@" + row + "," + col
                + ",att=" + attachments.size() + "/" + capacity + ")";
    }
}
