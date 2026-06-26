package com.whim.starcraft8.data;

import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.Race;
import com.whim.starcraft8.domain.UnitType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static production / prerequisite graph. All cross-type relationships live here so
 * the {@link UnitType} / {@link BuildingType} enum constructors stay free of
 * cross-enum references (avoids static-init ordering hazards).
 */
public final class TechTree {
    private TechTree() {}

    /** Units trainable at the given production building (train menu). */
    public static List<UnitType> producedBy(BuildingType b) {
        List<UnitType> out = new ArrayList<UnitType>();
        switch (b) {
            case COMMAND_CENTER:
                out.add(UnitType.SCV);
                break;
            case BARRACKS:
                out.add(UnitType.MARINE);
                out.add(UnitType.FIREBAT);
                break;
            case FACTORY:
                out.add(UnitType.SIEGE_TANK);
                break;
            case HATCHERY:
                out.add(UnitType.DRONE);
                out.add(UnitType.OVERLORD);
                break;
            case SPAWNING_POOL:
                out.add(UnitType.ZERGLING);
                break;
            case HYDRALISK_DEN:
                out.add(UnitType.HYDRALISK);
                break;
            case NEXUS:
                out.add(UnitType.PROBE);
                break;
            case GATEWAY:
                out.add(UnitType.ZEALOT);
                out.add(UnitType.DRAGOON);
                break;
            default:
                break;
        }
        return out;
    }

    /** Buildings a worker of this race can construct (build menu). */
    public static List<BuildingType> buildableBy(Race r) {
        List<BuildingType> out = new ArrayList<BuildingType>();
        switch (r) {
            case TERRAN:
                out.add(BuildingType.COMMAND_CENTER);
                out.add(BuildingType.SUPPLY_DEPOT);
                out.add(BuildingType.REFINERY);
                out.add(BuildingType.BARRACKS);
                out.add(BuildingType.FACTORY);
                break;
            case ZERG:
                out.add(BuildingType.HATCHERY);
                out.add(BuildingType.EXTRACTOR);
                out.add(BuildingType.SPAWNING_POOL);
                out.add(BuildingType.HYDRALISK_DEN);
                break;
            case PROTOSS:
                out.add(BuildingType.NEXUS);
                out.add(BuildingType.PYLON);
                out.add(BuildingType.ASSIMILATOR);
                out.add(BuildingType.GATEWAY);
                break;
            default:
                break;
        }
        return out;
    }

    /** Completed buildings required before this one (or its units) can be built. */
    public static List<BuildingType> prerequisites(BuildingType b) {
        List<BuildingType> out = new ArrayList<BuildingType>();
        switch (b) {
            case FACTORY:
                out.add(BuildingType.BARRACKS);
                break;
            case HYDRALISK_DEN:
                out.add(BuildingType.SPAWNING_POOL);
                break;
            default:
                break;
        }
        return out;
    }

    public static BuildingType townHall(Race r) {
        switch (r) {
            case TERRAN: return BuildingType.COMMAND_CENTER;
            case ZERG: return BuildingType.HATCHERY;
            case PROTOSS: return BuildingType.NEXUS;
            default: return null;
        }
    }

    public static UnitType worker(Race r) {
        switch (r) {
            case TERRAN: return UnitType.SCV;
            case ZERG: return UnitType.DRONE;
            case PROTOSS: return UnitType.PROBE;
            default: return null;
        }
    }

    /** Overlord for Zerg, else null (Terran/Protoss use supply buildings). */
    public static UnitType supplyUnit(Race r) {
        if (r == Race.ZERG) return UnitType.OVERLORD;
        return null;
    }

    /** Depot/Pylon, null for Zerg (Zerg uses the Overlord supply unit). */
    public static BuildingType supplyBuilding(Race r) {
        switch (r) {
            case TERRAN: return BuildingType.SUPPLY_DEPOT;
            case PROTOSS: return BuildingType.PYLON;
            default: return null;
        }
    }

    /** Gas-extraction building for the race. */
    public static BuildingType gasBuilding(Race r) {
        switch (r) {
            case TERRAN: return BuildingType.REFINERY;
            case ZERG: return BuildingType.EXTRACTOR;
            case PROTOSS: return BuildingType.ASSIMILATOR;
            default: return null;
        }
    }

    /** Primary first-tier combat production building for the race. */
    public static BuildingType primaryProduction(Race r) {
        switch (r) {
            case TERRAN: return BuildingType.BARRACKS;
            case ZERG: return BuildingType.SPAWNING_POOL;
            case PROTOSS: return BuildingType.GATEWAY;
            default: return null;
        }
    }

    /** Immutable empty list helper (unused placeholder kept for clarity). */
    static List<UnitType> none() { return Collections.emptyList(); }
}
