package com.whim.starcraft8.engine;

import java.util.ArrayList;
import java.util.List;

import com.whim.starcraft8.domain.BuildingType;
import com.whim.starcraft8.domain.UnitType;

/**
 * The only way the UI (and the orchestrator) construct {@link Command}s. Concrete
 * command types are package-private and dispatch through {@link Base#exec}, so the UI
 * never depends on engine internals.
 */
public final class Commands {
    private Commands() {}

    /** Package-private base: the engine executes a command via {@link #exec}. */
    static abstract class Base implements Command {
        abstract void exec(SimulationImpl s);
    }

    private static List<Long> copy(List<Long> ids) {
        return ids == null ? new ArrayList<Long>() : new ArrayList<Long>(ids);
    }

    public static Command move(List<Long> unitIds, double tx, double ty) {
        return new MoveCmd(copy(unitIds), tx, ty, false);
    }

    public static Command attackMove(List<Long> unitIds, double tx, double ty) {
        return new MoveCmd(copy(unitIds), tx, ty, true);
    }

    public static Command attackTarget(List<Long> unitIds, long targetId) {
        return new AttackTargetCmd(copy(unitIds), targetId);
    }

    public static Command gather(List<Long> workerIds, long resourceBuildingOrFieldId) {
        return new GatherCmd(copy(workerIds), resourceBuildingOrFieldId);
    }

    public static Command build(long workerId, BuildingType type, int tileX, int tileY) {
        return new BuildCmd(workerId, type, tileX, tileY);
    }

    public static Command train(long buildingId, UnitType type) {
        return new TrainCmd(buildingId, type);
    }

    public static Command setRally(long buildingId, double tx, double ty) {
        return new RallyCmd(buildingId, tx, ty);
    }

    public static Command stop(List<Long> unitIds) {
        return new StopCmd(copy(unitIds));
    }

    // ---- concrete commands -------------------------------------------------

    private static final class MoveCmd extends Base {
        final List<Long> ids; final double tx, ty; final boolean attack;
        MoveCmd(List<Long> ids, double tx, double ty, boolean attack) {
            this.ids = ids; this.tx = tx; this.ty = ty; this.attack = attack;
        }
        void exec(SimulationImpl s) { s.cmdMove(ids, tx, ty, attack); }
    }

    private static final class AttackTargetCmd extends Base {
        final List<Long> ids; final long targetId;
        AttackTargetCmd(List<Long> ids, long targetId) { this.ids = ids; this.targetId = targetId; }
        void exec(SimulationImpl s) { s.cmdAttackTarget(ids, targetId); }
    }

    private static final class GatherCmd extends Base {
        final List<Long> ids; final long resId;
        GatherCmd(List<Long> ids, long resId) { this.ids = ids; this.resId = resId; }
        void exec(SimulationImpl s) { s.cmdGather(ids, resId); }
    }

    private static final class BuildCmd extends Base {
        final long workerId; final BuildingType type; final int tx, ty;
        BuildCmd(long workerId, BuildingType type, int tx, int ty) {
            this.workerId = workerId; this.type = type; this.tx = tx; this.ty = ty;
        }
        void exec(SimulationImpl s) { s.cmdBuild(workerId, type, tx, ty); }
    }

    private static final class TrainCmd extends Base {
        final long buildingId; final UnitType type;
        TrainCmd(long buildingId, UnitType type) { this.buildingId = buildingId; this.type = type; }
        void exec(SimulationImpl s) { s.cmdTrain(buildingId, type); }
    }

    private static final class RallyCmd extends Base {
        final long buildingId; final double tx, ty;
        RallyCmd(long buildingId, double tx, double ty) { this.buildingId = buildingId; this.tx = tx; this.ty = ty; }
        void exec(SimulationImpl s) { s.cmdRally(buildingId, tx, ty); }
    }

    private static final class StopCmd extends Base {
        final List<Long> ids;
        StopCmd(List<Long> ids) { this.ids = ids; }
        void exec(SimulationImpl s) { s.cmdStop(ids); }
    }
}
