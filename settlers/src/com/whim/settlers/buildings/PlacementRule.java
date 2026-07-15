package com.whim.settlers.buildings;

/**
 * Terrain requirement for placing a building's footprint.
 * <ul>
 *   <li>{@link #LAND} — every footprint tile must be buildable land.</li>
 *   <li>{@link #MOUNTAIN} — the tile must be mountain terrain (mines); if a
 *       specific resource is required, {@link BuildingType#requiredResource()}
 *       pins it (e.g. a Coal Mine needs a coal mountain).</li>
 *   <li>{@link #COAST} — buildable land that is adjacent to water (shipyards).</li>
 * </ul>
 */
public enum PlacementRule {
    LAND, MOUNTAIN, COAST
}
