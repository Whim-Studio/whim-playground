package com.whim.b5wars.data;

import com.google.gson.Gson;
import com.whim.b5wars.model.DamageProfile;
import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Placement;
import com.whim.b5wars.model.Race;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.Section;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;
import com.whim.b5wars.model.Special;
import com.whim.b5wars.model.VictoryCondition;
import com.whim.b5wars.model.Weapon;
import com.whim.b5wars.model.WeaponArc;
import com.whim.b5wars.model.WeaponTrait;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Gson-backed loaders for factions, scenarios, and rules tables from classpath resources. */
public final class DataLoader {

    private static final Gson GSON = new Gson();
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** Faction resources bundled on the classpath under {@code /factions}. */
    private static final String[] FACTION_RESOURCES = {
            "/factions/earth-alliance.json",
            "/factions/narn-regime.json"
    };

    private DataLoader() {
    }

    /** Loads all bundled faction JSON on the classpath under {@code /factions/*.json}. */
    public static List<Faction> loadFactions() {
        List<Faction> factions = new ArrayList<Faction>();
        for (int i = 0; i < FACTION_RESOURCES.length; i++) {
            factions.add(loadFaction(FACTION_RESOURCES[i]));
        }
        return factions;
    }

    /** Loads one faction, e.g. {@code "/factions/earth-alliance.json"}. */
    public static Faction loadFaction(String resourcePath) {
        Dtos.FactionDto dto = read(resourcePath, Dtos.FactionDto.class);
        Race race = Race.valueOf(dto.race);
        List<ShipClass> classes = new ArrayList<ShipClass>();
        if (dto.shipClasses != null) {
            for (int i = 0; i < dto.shipClasses.size(); i++) {
                classes.add(toShipClass(dto.shipClasses.get(i)));
            }
        }
        return new Faction(race, classes);
    }

    /** Loads a scenario, e.g. {@code "/scenarios/border-skirmish.json"}. */
    public static Scenario loadScenario(String resourcePath) {
        Dtos.ScenarioDto dto = read(resourcePath, Dtos.ScenarioDto.class);
        List<Placement> placements = new ArrayList<Placement>();
        if (dto.placements != null) {
            for (int i = 0; i < dto.placements.size(); i++) {
                Dtos.PlacementDto p = dto.placements.get(i);
                placements.add(new Placement(
                        p.shipClassId,
                        Side.valueOf(p.side),
                        new Hex(p.q, p.r),
                        Facing.valueOf(p.facing),
                        p.speed));
            }
        }
        VictoryCondition victory = VictoryCondition.valueOf(dto.victory);
        return new Scenario(dto.name, dto.mapWidth, dto.mapHeight, placements, victory, dto.turnLimit);
    }

    /** Impulse cadence: map speed -&gt; boolean[impulseCount] (true = enters a hex that impulse). */
    public static Map<Integer, boolean[]> loadImpulseCadence() {
        Dtos.ImpulseCadenceDto dto = read("/tables/impulse-cadence.json", Dtos.ImpulseCadenceDto.class);
        Map<Integer, boolean[]> result = new LinkedHashMap<Integer, boolean[]>();
        if (dto.cadence != null) {
            for (Map.Entry<String, boolean[]> e : dto.cadence.entrySet()) {
                result.put(Integer.valueOf(Integer.parseInt(e.getKey())), e.getValue());
            }
        }
        return result;
    }

    /** The d20 critical-hit table, ascending by roll range. */
    public static List<CriticalEntry> loadCriticalTable() {
        Dtos.CriticalTableDto dto = read("/tables/critical-hits.json", Dtos.CriticalTableDto.class);
        List<CriticalEntry> entries = new ArrayList<CriticalEntry>();
        if (dto.entries != null) {
            for (int i = 0; i < dto.entries.size(); i++) {
                Dtos.CriticalEntryDto c = dto.entries.get(i);
                entries.add(new CriticalEntry(c.rollMin, c.rollMax, c.effect));
            }
        }
        return entries;
    }

    // ---- mapping helpers -------------------------------------------------

    private static ShipClass toShipClass(Dtos.ShipClassDto dto) {
        Map<Facing, Integer> armor = new EnumMap<Facing, Integer>(Facing.class);
        if (dto.armor != null) {
            for (Map.Entry<String, Integer> e : dto.armor.entrySet()) {
                armor.put(Facing.valueOf(e.getKey()), e.getValue());
            }
        }
        Map<Section, Integer> structure = new EnumMap<Section, Integer>(Section.class);
        if (dto.structure != null) {
            for (Map.Entry<String, Integer> e : dto.structure.entrySet()) {
                structure.put(Section.valueOf(e.getKey()), e.getValue());
            }
        }
        List<Weapon> weapons = new ArrayList<Weapon>();
        if (dto.weapons != null) {
            for (int i = 0; i < dto.weapons.size(); i++) {
                weapons.add(toWeapon(dto.weapons.get(i)));
            }
        }
        List<Special> specials = new ArrayList<Special>();
        if (dto.specials != null) {
            for (int i = 0; i < dto.specials.size(); i++) {
                Dtos.SpecialDto s = dto.specials.get(i);
                specials.add(new Special(s.id, s.name, s.value));
            }
        }
        DefenseType defense = dto.defenseType == null
                ? DefenseType.ARMOR : DefenseType.valueOf(dto.defenseType);
        Race race = dto.race == null ? null : Race.valueOf(dto.race);
        return new ShipClass(dto.id, dto.name, race, dto.points,
                dto.maxSpeed, dto.turnMode, dto.thrust, dto.power,
                dto.initiativeBonus, dto.crewQuality,
                dto.sensorRating, dto.ewRating,
                armor, structure, defense, weapons, specials);
    }

    private static Weapon toWeapon(Dtos.WeaponDto dto) {
        Set<Facing> arcFacings = EnumSet.noneOf(Facing.class);
        if (dto.arc != null) {
            for (int i = 0; i < dto.arc.size(); i++) {
                arcFacings.add(Facing.valueOf(dto.arc.get(i)));
            }
        }
        WeaponArc arc = new WeaponArc(arcFacings);
        DamageProfile damage = dto.damage == null
                ? new DamageProfile(0, 0, 0)
                : new DamageProfile(dto.damage.count, dto.damage.sides, dto.damage.plus);
        Set<WeaponTrait> traits = EnumSet.noneOf(WeaponTrait.class);
        if (dto.traits != null) {
            for (int i = 0; i < dto.traits.size(); i++) {
                traits.add(WeaponTrait.valueOf(dto.traits.get(i)));
            }
        }
        return new Weapon(dto.name, dto.type, arc, dto.rangeBrackets,
                dto.baseToHit, damage, dto.reloadTurns, traits);
    }

    private static <T> T read(String resourcePath, Class<T> type) {
        InputStream in = DataLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(in, UTF8);
            T value = GSON.fromJson(reader, type);
            if (value == null) {
                throw new IllegalStateException("Empty or malformed JSON: " + resourcePath);
            }
            return value;
        } finally {
            close(reader);
            close(in);
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
                // best-effort close
            }
        }
    }
}
