package com.whim.b5wars.data;

import java.util.List;
import java.util.Map;

/**
 * Gson deserialization DTOs. These mirror the JSON layout so the {@code model}
 * package can stay free of Gson concerns; {@link DataLoader} maps DTOs into model types.
 */
final class Dtos {

    private Dtos() {
    }

    static final class FactionDto {
        String _note;
        String race;
        List<ShipClassDto> shipClasses;
    }

    static final class ShipClassDto {
        String id;
        String name;
        String race;
        int points;
        int maxSpeed;
        int turnMode;
        int thrust;
        int power;
        int initiativeBonus;
        int crewQuality;
        int sensorRating;
        int ewRating;
        String defenseType;
        Map<String, Integer> armor;
        Map<String, Integer> structure;
        List<WeaponDto> weapons;
        List<SpecialDto> specials;
    }

    static final class WeaponDto {
        String name;
        String type;
        List<String> arc;
        int[] rangeBrackets;
        int baseToHit;
        DamageDto damage;
        int reloadTurns;
        List<String> traits;
    }

    static final class DamageDto {
        int count;
        int sides;
        int plus;
    }

    static final class SpecialDto {
        String id;
        String name;
        int value;
    }

    static final class ScenarioDto {
        String _note;
        String name;
        int mapWidth;
        int mapHeight;
        List<PlacementDto> placements;
        String victory;
        int turnLimit;
    }

    static final class PlacementDto {
        String shipClassId;
        String side;
        int q;
        int r;
        String facing;
        int speed;
    }

    static final class ImpulseCadenceDto {
        String _note;
        int impulseCount;
        Map<String, boolean[]> cadence;
    }

    static final class CriticalTableDto {
        String _note;
        List<CriticalEntryDto> entries;
    }

    static final class CriticalEntryDto {
        int rollMin;
        int rollMax;
        String effect;
    }
}
