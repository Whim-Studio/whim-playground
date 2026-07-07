package com.whim.albion.api;

import com.whim.albion.api.Enums.CombatActionType;
import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.GameStateType;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Enums.MapType;
import com.whim.albion.api.Enums.QuestStatus;
import com.whim.albion.api.Enums.SkillType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TileType;

import java.util.List;

/**
 * Read-only projections consumed by the UI (Task 3) for rendering. The engine
 * (Task 2) exposes the whole game as {@link GameStateView}. Implementations must
 * be safe to read from the Swing EDT and must not tear across a single read.
 */
public final class Views {

    private Views() {}

    /** Root snapshot the UI renders each frame. Never null. */
    public interface GameStateView {
        GameStateType current();
        WorldView world();          // null outside OVERWORLD/DUNGEON
        PartyView party();
        CombatView combat();        // null unless current()==COMBAT
        DialogueView dialogue();    // null unless current()==DIALOGUE
        JournalView journal();
        int gold();
        String statusMessage();     // transient one-line banner ("" if none)
        List<String> menuOptions(); // for TITLE / MENU / GAME_OVER screens
    }

    // ------------------------------------------------------------- world/maps

    public interface WorldView {
        String mapName();
        MapType mapType();
        int width();
        int height();
        TileView tileAt(int x, int y);
        PlayerView player();
        List<NpcView> npcs();
    }

    public interface TileView {
        TileType type();
        boolean walkable();
        boolean blocksSight();
        String decorKey();          // procedural-art key, "" if none
    }

    public interface PlayerView {
        int x();
        int y();
        Direction facing();
    }

    public interface NpcView {
        int x();
        int y();
        String name();
        String spriteKey();
        boolean hostile();
    }

    // -------------------------------------------------------------- party/char

    public interface PartyView {
        List<CharacterView> members();
        int activeIndex();
    }

    public interface CharacterView {
        String name();
        String portraitKey();
        String profession();
        int level();
        int xp();
        int xpToNext();
        int lp();
        int maxLp();
        int sp();
        int maxSp();
        boolean alive();
        int stat(StatType type);
        int skill(SkillType type);
        List<ItemView> inventory();
        ItemView equipped(EquipSlot slot);   // null if empty
        List<SpellView> spells();
        boolean canCast(SpellSchool school);
    }

    public interface ItemView {
        String id();
        String name();
        ItemType type();
        EquipSlot slot();           // null if not equippable
        int quantity();
        int value();
        String description();
        String spriteKey();
    }

    public interface SpellView {
        String id();
        String name();
        SpellSchool school();
        int spCost();
        boolean castable();         // active caster has SP + meets reqs
        String description();
    }

    // ------------------------------------------------------------------ combat

    public interface CombatView {
        int cols();
        int rows();
        List<CombatantView> combatants();
        int currentTurnIndex();     // index into combatants()
        List<CombatActionType> availableActions();
        List<String> log();         // recent combat messages, oldest first
        boolean finished();
        boolean victory();
    }

    public interface CombatantView {
        String name();
        boolean playerSide();
        int gridX();
        int gridY();
        int lp();
        int maxLp();
        int sp();
        int maxSp();
        boolean alive();
        boolean current();          // is it this combatant's turn
        String spriteKey();
    }

    // --------------------------------------------------------------- dialogue

    public interface DialogueView {
        String speaker();
        String portraitKey();
        String text();
        List<String> options();     // selectable replies, may be empty (end)
    }

    // ---------------------------------------------------------------- journal

    public interface JournalView {
        List<QuestEntryView> quests();
    }

    public interface QuestEntryView {
        String title();
        QuestStatus status();
        List<String> objectives();
    }
}
