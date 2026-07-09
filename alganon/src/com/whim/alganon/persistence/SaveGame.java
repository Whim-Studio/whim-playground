package com.whim.alganon.persistence;

import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.StatType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A flat, serializable snapshot of everything the engine must restore on load: the full
 * model progression (identity, level/xp, stats, skills, inventory, equipment, study,
 * position) plus engine-owned state the model doesn't hold (quests, faction-war
 * objectives, player auction listings) and the {@code lastSaveEpochMillis} that drives
 * offline-Study accrual. Populated by the engine, encoded by {@link SaveCodec}.
 */
public final class SaveGame {
    public static final int VERSION = 1;

    public long seed;
    public String raceId, familyId, classId, name;
    public int level;
    public long xp;
    public int hp, maxHp, resource, maxResource;
    public String stance, school;
    public Map<StatType, Integer> stats = new LinkedHashMap<StatType, Integer>();
    public Map<SkillType, Integer> skills = new LinkedHashMap<SkillType, Integer>();
    public long gold;
    public Map<String, Integer> inventory = new LinkedHashMap<String, Integer>();
    public Map<EquipSlot, String> equipped = new LinkedHashMap<EquipSlot, String>();
    public String studyAssignment; // null if none
    public double bankedStudy;
    public int posX, posY;
    public String zoneId;
    public long lastSaveEpochMillis;
    public int asharrWarScore, kujixWarScore;

    public List<QuestSave> quests = new ArrayList<QuestSave>();
    public List<WarObjSave> warObjectives = new ArrayList<WarObjSave>();
    public List<ListingSave> listings = new ArrayList<ListingSave>();

    /** Full quest def + live progress (procedural quests are not in Content, so stored whole). */
    public static final class QuestSave {
        public String id, name, description, giverNpcId, turnInNpcId;
        public int levelReq, xpReward, goldReward;
        public boolean procedural;
        public String status;
        public List<String> rewardItemIds = new ArrayList<String>();
        public List<ObjSave> objectives = new ArrayList<ObjSave>();
    }

    public static final class ObjSave {
        public String type, targetId, text;
        public int count, progress;
    }

    public static final class WarObjSave {
        public String name, control;
        public double influence, nextTick;
    }

    public static final class ListingSave {
        public String listingId, itemId;
        public int quantity;
        public long price;
    }
}
