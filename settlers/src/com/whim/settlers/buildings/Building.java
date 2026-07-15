package com.whim.settlers.buildings;

/**
 * A placed building instance. Anchored at its top-left footprint tile
 * ({@code x,y}); occupies {@code type.footprintW() × type.footprintH()} tiles.
 *
 * <p>Lifecycle: a building is created {@link BuildingState#UNDER_CONSTRUCTION}
 * with {@code progress} in [0,1]; once progress reaches 1 it becomes
 * {@link BuildingState#FINISHED}. Phase 2 advances construction from a stubbed
 * unlimited resource pool; Phase 3 gates it on delivered planks/stone.
 */
public final class Building {

    private final BuildingType type;
    private final int x, y;
    private int ownerId; // mutable: military capture flips ownership

    private BuildingState state;
    private float progress; // 0..1 while under construction

    public Building(BuildingType type, int x, int y, int ownerId) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.ownerId = ownerId;
        // The Castle is founded ready-to-use; everything else is built up.
        if (type == BuildingType.CASTLE) {
            this.state = BuildingState.FINISHED;
            this.progress = 1f;
        } else {
            this.state = BuildingState.UNDER_CONSTRUCTION;
            this.progress = 0f;
        }
    }

    /** Advance construction. No-op once finished. */
    public void update(float dtSeconds) {
        if (state != BuildingState.UNDER_CONSTRUCTION) return;
        float t = type.buildTimeSeconds();
        progress += t <= 0 ? 1f : dtSeconds / t;
        if (progress >= 1f) {
            progress = 1f;
            state = BuildingState.FINISHED;
        }
    }

    public boolean covers(int tx, int ty) {
        return tx >= x && ty >= y
            && tx < x + type.footprintW()
            && ty < y + type.footprintH();
    }

    public BuildingType type()   { return type; }
    public int x()               { return x; }
    public int y()               { return y; }
    public int ownerId()         { return ownerId; }
    public void setOwner(int id) { this.ownerId = id; }
    public BuildingState state() { return state; }
    public float progress()      { return progress; }
    public boolean isFinished()  { return state == BuildingState.FINISHED; }
}
