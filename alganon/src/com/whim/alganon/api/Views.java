package com.whim.alganon.api;

import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.GameStateType;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TileType;

import java.util.List;
import java.util.Map;

/**
 * Read-only projections the UI (Task 3) renders. The engine (Task 2) returns a
 * {@link GameStateView} snapshot from {@link GameController#state()} after every
 * mutation. The UI must treat every view as immutable and must never downcast to
 * a concrete engine/model type. All fields are exposed via zero-arg accessor
 * methods so the engine can back them with live objects or copies as it prefers.
 */
public final class Views {
    private Views() {}

    /** Root snapshot. Sub-views are null when not relevant to the current state. */
    public interface GameStateView {
        GameStateType state();
        CharacterView player();          // null on TITLE / before creation
        WorldView world();               // null unless PLAYING-family state
        CombatView combat();             // null unless in combat
        CreationView creation();         // null unless CHARACTER_CREATION
        List<QuestView> quests();        // active + ready quests (never null)
        StudyView study();               // null before creation
        CraftingView crafting();         // null before creation
        AuctionView auction();           // null unless AUCTION open
        FamilyView family();             // null before creation
        List<ChatLineView> chat();       // recent chat/log lines (never null)
        List<String> menuOptions();      // for TITLE / GAME_OVER / SETTINGS
        String toastMessage();           // transient status line, may be empty
    }

    /** Character-creation wizard data + current partial selection. */
    public interface CreationView {
        List<Defs.RaceDef> races();
        List<Defs.FamilyDef> familiesFor(String raceId);
        List<Defs.ClassDef> classes();
        String selectedRaceId();         // may be null
        String selectedFamilyId();       // may be null
        String selectedClassId();        // may be null
        String enteredName();            // may be empty
        int step();                      // 0=race,1=family,2=class,3=name/confirm
    }

    public interface CharacterView {
        String name();
        Faction faction();
        String raceName();
        String familyName();
        FamilyArchetype archetype();
        String className();
        int level();
        long xp();
        long xpToNext();
        int hp(); int maxHp();
        ResourceType resourceType();
        int resource(); int maxResource();
        Stance stance();                 // Champion only; else BALANCE/ignored
        School school();                 // Magus only; else NONE
        Map<StatType, Integer> stats();
        Map<SkillType, Integer> skills();
        long gold();
        List<AbilityView> abilities();
        List<ItemView> inventory();
        Map<Enums.EquipSlot, ItemView> equipped();
        GridPos pos();
        String zoneId();
    }

    public interface AbilityView {
        String id(); String name(); String description();
        int resourceCost(); double cooldownSec();
        double cooldownRemaining();      // 0 == ready
        boolean usable();                // level + resource + cooldown satisfied
        Enums.AbilityKind kind();
    }

    public interface ItemView {
        String id(); String name(); String description();
        Enums.ItemType type(); Enums.EquipSlot slot();
        int quantity(); int value(); int power();
    }

    /** The loaded zone the player is standing in. */
    public interface WorldView {
        String zoneId(); String zoneName();
        int width(); int height();
        TileType tileAt(int x, int y);
        List<NpcView> npcs();
        List<MobView> mobs();
        List<GatherView> gatherNodes();
        List<PortalView> portals();      // zone transitions
        FactionWarView factionWar();     // background Towers/Keeps sim, may be null
    }

    public interface NpcView { String id(); String name(); GridPos pos(); boolean questGiver(); boolean vendor(); String spriteKey(); }
    public interface MobView { String id(); String name(); GridPos pos(); int hp(); int maxHp(); int level(); boolean inCombat(); String spriteKey(); }
    public interface GatherView { String id(); String name(); GridPos pos(); boolean depleted(); }
    public interface PortalView { GridPos pos(); String targetZoneId(); String label(); }

    /** Background simulated faction war (single-player substitute for Towers/Keeps PvP). */
    public interface FactionWarView {
        List<ObjectiveView> objectives();
        int asharrScore(); int kujixScore();
        interface ObjectiveView { String name(); ControlState control(); }
    }

    public interface CombatView {
        boolean active();
        List<CombatantView> combatants();   // index 0 is always the player
        int activeIndex();
        String log();
        interface CombatantView {
            String name(); boolean isPlayer(); int hp(); int maxHp(); boolean alive();
        }
    }

    public interface QuestView {
        String id(); String name(); String description();
        QuestStatus status(); boolean procedural();
        List<ObjectiveProgressView> objectives();
        interface ObjectiveProgressView { String text(); int current(); int required(); boolean done(); }
    }

    /** Offline "Study" progression panel. */
    public interface StudyView {
        List<SkillType> studyableSkills();
        SkillType assignedSkill();       // null if none assigned
        int studySlots();                // number of concurrent study assignments (v1: 1)
        double bankedHours();            // offline hours currently banked toward progress
        double capHours();               // offline accrual cap (v1: 8h)
        double progressToNextPoint();    // 0..1 toward the next skill point of assignedSkill
        Map<SkillType, Integer> skillLevels();
    }

    public interface CraftingView {
        List<RecipeProgressView> recipes();
        Map<String, Integer> materials();   // itemId -> qty in inventory
        interface RecipeProgressView {
            String id(); String name(); Enums.SkillType skill(); boolean craftable();
            Map<String, Integer> inputs(); String outputName(); int outputQty();
        }
    }

    /** NPC-populated auction/requisition house (single-player economy substitute). */
    public interface AuctionView {
        List<ListingView> listings();
        long playerGold();
        interface ListingView { String listingId(); String itemName(); int quantity(); long price(); boolean sellerIsPlayer(); }
    }

    public interface FamilyView {
        String familyName(); FamilyArchetype archetype(); String bonusDescription();
        List<String> memberNames();      // NPC "family" members
        String vendorNpcId();            // family merchant, may be null
    }

    public interface ChatLineView { ChatChannel channel(); String text(); }
}
