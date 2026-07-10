package com.whim.scg.engine;

import com.whim.scg.api.Enums;
import com.whim.scg.model.CrewModel;
import com.whim.scg.model.RoomModel;
import com.whim.scg.model.ShipModel;

/** Shared gameplay math: manning bonuses, system effectiveness, derived stats. */
final class Rules {
    private Rules() {}

    /** The stat that matters for manning a given room type. */
    static Enums.StatType stationStat(Enums.RoomType t) {
        switch (t) {
            case WEAPONS: return Enums.StatType.GUNNERY;
            case SHIELDS: return Enums.StatType.SHIELDS;
            case ENGINES: return Enums.StatType.PILOTING;
            case MEDBAY: return Enums.StatType.MEDICAL;
            case SENSORS: return Enums.StatType.SCIENCE;
            case TELEPORTER: return Enums.StatType.ENGINEERING;
            case OXYGEN: return Enums.StatType.ENGINEERING;
            case BRIDGE: return Enums.StatType.COMMAND;
            default: return Enums.StatType.COMBAT;
        }
    }

    /** Best relevant skill among living crew stationed in the room (0 if unmanned). */
    static int mannedSkill(ShipModel ship, RoomModel room) {
        if (room == null) return 0;
        Enums.StatType stat = stationStat(room.type);
        int best = 0;
        for (Integer id : room.crewIds) {
            CrewModel c = ship.crewById(id);
            if (c != null && c.alive()) {
                int v = c.skill(stat);
                if (v > best) best = v;
            }
        }
        return best;
    }

    /**
     * Effectiveness multiplier of a powered, manned system. 0 if not operational.
     * Manning a system with a skilled crew boosts its effect (contract §1/§6).
     */
    static double effectiveness(ShipModel ship, RoomModel room) {
        if (room == null || !room.operational()) return 0;
        double powerFrac = room.maxPower > 0 ? (double) room.power / room.maxPower : 1.0;
        double manning = 1.0 + mannedSkill(ship, room) / 100.0; // up to +100%
        return powerFrac * manning;
    }

    static double effectiveness(ShipModel ship, Enums.RoomType type) {
        return effectiveness(ship, ship.firstRoom(type));
    }

    /** Evasion chance (0..0.45) from engines being powered + a skilled pilot. */
    static double evasion(ShipModel ship) {
        double eff = effectiveness(ship, Enums.RoomType.ENGINES);
        return Math.min(0.45, eff * 0.18);
    }
}
