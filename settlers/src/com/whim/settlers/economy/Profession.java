package com.whim.settlers.economy;

/**
 * The job a {@link Settler} performs. Each production building is staffed by
 * exactly one professional; {@link #CARRIER} and {@link #BUILDER} are the
 * transport/construction roles (carriers become the flag-relay couriers in
 * Phase 4). Modelled as a role enum per settler, as the design calls for, so
 * later phases can attach job-specific movement and appearance.
 */
public enum Profession {
    IDLE, BUILDER, CARRIER,
    WOODCUTTER, SAWYER, FORESTER, STONEMASON,
    FARMER, MILLER, WELL_DIGGER, BAKER, FISHERMAN, PIG_FARMER, BUTCHER,
    MINER, SMELTER, GOLDSMITH, TOOLMAKER, BLACKSMITH,
    KNIGHT
}
