package com.whim.alganon.engine;

import com.whim.alganon.api.CharacterModel;
import com.whim.alganon.api.Content;
import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ChatChannel;
import com.whim.alganon.api.Enums.ControlState;
import com.whim.alganon.api.Enums.EquipSlot;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.ItemType;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.Stance;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TileType;
import com.whim.alganon.api.GridPos;
import com.whim.alganon.api.Views;
import com.whim.alganon.api.WorldModel;
import com.whim.alganon.combat.CombatSystem;
import com.whim.alganon.combat.Progression;
import com.whim.alganon.craft.CraftSystem;
import com.whim.alganon.craft.Listing;
import com.whim.alganon.quest.QuestRun;
import com.whim.alganon.study.StudySystem;
import com.whim.alganon.worldsim.WarObjective;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable read-only projections of live engine/model state, implementing the
 * {@code api.Views} interfaces the UI renders. Built fresh from {@link GameEngine} after
 * every tick/intent. The UI treats them as snapshots and never downcasts.
 */
final class EngineViews {
    private EngineViews() {}

    // ---------------- character ----------------

    static final class CharacterViewImpl implements Views.CharacterView {
        private final CharacterModel p;
        private final Content content;
        private final CombatSystem combat;
        CharacterViewImpl(CharacterModel p, Content content, CombatSystem combat) {
            this.p = p; this.content = content; this.combat = combat;
        }
        public String name() { return p.getName(); }
        public Faction faction() { return p.faction(); }
        public String raceName() {
            Defs.RaceDef r = content.race(p.raceId());
            return r != null ? r.name : p.raceId();
        }
        public String familyName() {
            Defs.FamilyDef f = content.family(p.familyId());
            return f != null ? f.name : p.familyId();
        }
        public FamilyArchetype archetype() {
            Defs.FamilyDef f = content.family(p.familyId());
            return f != null ? f.archetype : FamilyArchetype.ACHIEVER;
        }
        public String className() {
            Defs.ClassDef c = content.clazz(p.classId());
            return c != null ? c.name : p.classId();
        }
        public int level() { return p.level(); }
        public long xp() { return p.xp(); }
        public long xpToNext() { return Progression.xpToNext(p.level()); }
        public int hp() { return p.hp(); }
        public int maxHp() { return p.maxHp(); }
        public ResourceType resourceType() { return p.resourceType(); }
        public int resource() { return p.resource(); }
        public int maxResource() { return p.maxResource(); }
        public Stance stance() { return p.stance(); }
        public School school() { return p.school(); }
        public Map<StatType, Integer> stats() {
            return new LinkedHashMap<StatType, Integer>(p.stats());
        }
        public Map<SkillType, Integer> skills() {
            Map<SkillType, Integer> m = new LinkedHashMap<SkillType, Integer>();
            for (SkillType s : SkillType.values()) m.put(s, p.skill(s));
            return m;
        }
        public long gold() { return p.gold(); }
        public List<Views.AbilityView> abilities() {
            List<Views.AbilityView> out = new ArrayList<Views.AbilityView>();
            for (String id : p.knownAbilityIds()) {
                Defs.AbilityDef a = content.ability(id);
                if (a != null) out.add(new AbilityViewImpl(a, p, combat));
            }
            return out;
        }
        public List<Views.ItemView> inventory() {
            List<Views.ItemView> out = new ArrayList<Views.ItemView>();
            for (Map.Entry<String, Integer> e : p.inventory().entrySet()) {
                Defs.ItemDef d = content.item(e.getKey());
                if (d != null) out.add(new ItemViewImpl(d, e.getValue()));
            }
            return out;
        }
        public Map<EquipSlot, Views.ItemView> equipped() {
            Map<EquipSlot, Views.ItemView> m = new LinkedHashMap<EquipSlot, Views.ItemView>();
            for (Map.Entry<EquipSlot, String> e : p.equipped().entrySet()) {
                Defs.ItemDef d = content.item(e.getValue());
                if (d != null) m.put(e.getKey(), new ItemViewImpl(d, 1));
            }
            return m;
        }
        public GridPos pos() { return p.pos(); }
        public String zoneId() { return p.zoneId(); }
    }

    static final class AbilityViewImpl implements Views.AbilityView {
        private final Defs.AbilityDef a;
        private final CharacterModel p;
        private final CombatSystem combat;
        AbilityViewImpl(Defs.AbilityDef a, CharacterModel p, CombatSystem combat) {
            this.a = a; this.p = p; this.combat = combat;
        }
        public String id() { return a.id; }
        public String name() { return a.name; }
        public String description() { return a.description; }
        public int resourceCost() { return a.resourceCost; }
        public double cooldownSec() { return a.cooldownSec; }
        public double cooldownRemaining() { return combat.cooldownRemaining(a.id); }
        public boolean usable() {
            return p.level() >= a.levelReq
                    && p.resource() >= a.resourceCost
                    && combat.cooldownRemaining(a.id) <= 0
                    && !combat.isCasting();
        }
        public AbilityKind kind() { return a.kind; }
    }

    static final class ItemViewImpl implements Views.ItemView {
        private final Defs.ItemDef d;
        private final int qty;
        ItemViewImpl(Defs.ItemDef d, int qty) { this.d = d; this.qty = qty; }
        public String id() { return d.id; }
        public String name() { return d.name; }
        public String description() { return d.description; }
        public ItemType type() { return d.type; }
        public EquipSlot slot() { return d.slot; }
        public int quantity() { return qty; }
        public int value() { return d.value; }
        public int power() { return d.power; }
    }

    // ---------------- world ----------------

    static final class WorldViewImpl implements Views.WorldView {
        private final WorldModel w;
        private final Views.FactionWarView war;
        WorldViewImpl(WorldModel w, Views.FactionWarView war) { this.w = w; this.war = war; }
        public String zoneId() { return w.zoneId(); }
        public String zoneName() { return w.zoneName(); }
        public int width() { return w.width(); }
        public int height() { return w.height(); }
        public TileType tileAt(int x, int y) { return w.tileAt(x, y); }
        public List<Views.NpcView> npcs() {
            List<Views.NpcView> out = new ArrayList<Views.NpcView>();
            for (WorldModel.NpcEntity n : w.npcs()) out.add(new NpcViewImpl(n));
            return out;
        }
        public List<Views.MobView> mobs() {
            List<Views.MobView> out = new ArrayList<Views.MobView>();
            for (WorldModel.MobEntity m : w.mobs()) out.add(new MobViewImpl(m));
            return out;
        }
        public List<Views.GatherView> gatherNodes() {
            List<Views.GatherView> out = new ArrayList<Views.GatherView>();
            for (WorldModel.NodeEntity n : w.nodes()) out.add(new GatherViewImpl(n));
            return out;
        }
        public List<Views.PortalView> portals() {
            List<Views.PortalView> out = new ArrayList<Views.PortalView>();
            for (WorldModel.Portal pt : w.portals()) out.add(new PortalViewImpl(pt));
            return out;
        }
        public Views.FactionWarView factionWar() { return war; }
    }

    static final class NpcViewImpl implements Views.NpcView {
        private final WorldModel.NpcEntity n;
        NpcViewImpl(WorldModel.NpcEntity n) { this.n = n; }
        public String id() { return n.id(); }
        public String name() { return n.name(); }
        public GridPos pos() { return n.pos(); }
        public boolean questGiver() { return n.questGiver(); }
        public boolean vendor() { return n.vendor(); }
        public String spriteKey() { return n.spriteKey(); }
    }

    static final class MobViewImpl implements Views.MobView {
        private final WorldModel.MobEntity m;
        MobViewImpl(WorldModel.MobEntity m) { this.m = m; }
        public String id() { return m.id(); }
        public String name() { return m.name(); }
        public GridPos pos() { return m.pos(); }
        public int hp() { return m.hp(); }
        public int maxHp() { return m.maxHp(); }
        public int level() { return m.level(); }
        public boolean inCombat() { return m.inCombat(); }
        public String spriteKey() { return m.spriteKey(); }
    }

    static final class GatherViewImpl implements Views.GatherView {
        private final WorldModel.NodeEntity n;
        GatherViewImpl(WorldModel.NodeEntity n) { this.n = n; }
        public String id() { return n.id(); }
        public String name() { return n.name(); }
        public GridPos pos() { return n.pos(); }
        public boolean depleted() { return n.depleted(); }
    }

    static final class PortalViewImpl implements Views.PortalView {
        private final WorldModel.Portal p;
        PortalViewImpl(WorldModel.Portal p) { this.p = p; }
        public GridPos pos() { return p.pos(); }
        public String targetZoneId() { return p.targetZoneId(); }
        public String label() { return p.label(); }
    }

    static final class FactionWarViewImpl implements Views.FactionWarView {
        private final List<WarObjective> objs;
        private final int asharr, kujix;
        FactionWarViewImpl(List<WarObjective> objs, int asharr, int kujix) {
            this.objs = objs; this.asharr = asharr; this.kujix = kujix;
        }
        public List<ObjectiveView> objectives() {
            List<ObjectiveView> out = new ArrayList<ObjectiveView>();
            for (final WarObjective o : objs) {
                out.add(new ObjectiveView() {
                    public String name() { return o.name; }
                    public ControlState control() { return o.control; }
                });
            }
            return out;
        }
        public int asharrScore() { return asharr; }
        public int kujixScore() { return kujix; }
    }

    // ---------------- combat ----------------

    static final class CombatViewImpl implements Views.CombatView {
        private final CombatSystem combat;
        private final CharacterModel player;
        CombatViewImpl(CombatSystem combat, CharacterModel player) {
            this.combat = combat; this.player = player;
        }
        public boolean active() { return combat.isActive(); }
        public List<CombatantView> combatants() {
            List<CombatantView> out = new ArrayList<CombatantView>();
            out.add(wrap(player));
            for (WorldModel.MobEntity m : combat.engaged()) out.add(wrap(m));
            return out;
        }
        public int activeIndex() { return 0; }
        public String log() { return combat.combatLogText(); }
        private static CombatantView wrap(final com.whim.alganon.api.Combatant c) {
            return new CombatantView() {
                public String name() { return c.name(); }
                public boolean isPlayer() { return c.isPlayer(); }
                public int hp() { return c.hp(); }
                public int maxHp() { return c.maxHp(); }
                public boolean alive() { return c.alive(); }
            };
        }
    }

    // ---------------- quests ----------------

    static final class QuestViewImpl implements Views.QuestView {
        private final QuestRun run;
        QuestViewImpl(QuestRun run) { this.run = run; }
        public String id() { return run.def.id; }
        public String name() { return run.def.name; }
        public String description() { return run.def.description; }
        public QuestStatus status() { return run.status; }
        public boolean procedural() { return run.def.procedural; }
        public List<ObjectiveProgressView> objectives() {
            List<ObjectiveProgressView> out = new ArrayList<ObjectiveProgressView>();
            for (int i = 0; i < run.def.objectives.size(); i++) {
                final Defs.ObjectiveDef o = run.def.objectives.get(i);
                final int cur = run.progress[i];
                out.add(new ObjectiveProgressView() {
                    public String text() { return o.text; }
                    public int current() { return cur; }
                    public int required() { return o.count; }
                    public boolean done() { return cur >= o.count; }
                });
            }
            return out;
        }
    }

    // ---------------- study ----------------

    static final class StudyViewImpl implements Views.StudyView {
        private final StudySystem study;
        private final com.whim.alganon.api.GameModel model;
        StudyViewImpl(StudySystem study, com.whim.alganon.api.GameModel model) {
            this.study = study; this.model = model;
        }
        public List<SkillType> studyableSkills() {
            List<SkillType> out = new ArrayList<SkillType>();
            for (SkillType s : SkillType.values()) out.add(s);
            return out;
        }
        public SkillType assignedSkill() { return model.player().studyAssignment(); }
        public int studySlots() { return StudySystem.SLOTS; }
        public double bankedHours() { return study.bankedHours(model); }
        public double capHours() { return StudySystem.CAP_HOURS; }
        public double progressToNextPoint() { return study.progressToNextPoint(model); }
        public Map<SkillType, Integer> skillLevels() {
            Map<SkillType, Integer> m = new LinkedHashMap<SkillType, Integer>();
            for (SkillType s : SkillType.values()) m.put(s, model.player().skill(s));
            return m;
        }
    }

    // ---------------- crafting / auction ----------------

    static final class CraftingViewImpl implements Views.CraftingView {
        private final CraftSystem craft;
        private final com.whim.alganon.api.GameModel model;
        private final Content content;
        CraftingViewImpl(CraftSystem craft, com.whim.alganon.api.GameModel model, Content content) {
            this.craft = craft; this.model = model; this.content = content;
        }
        public List<RecipeProgressView> recipes() {
            List<RecipeProgressView> out = new ArrayList<RecipeProgressView>();
            for (final Defs.RecipeDef r : content.recipes()) {
                final boolean craftable = craft.isCraftable(model, r);
                final Defs.ItemDef outItem = content.item(r.outputItemId);
                out.add(new RecipeProgressView() {
                    public String id() { return r.id; }
                    public String name() { return r.name; }
                    public SkillType skill() { return r.skill; }
                    public boolean craftable() { return craftable; }
                    public Map<String, Integer> inputs() {
                        return new LinkedHashMap<String, Integer>(r.inputs);
                    }
                    public String outputName() { return outItem != null ? outItem.name : r.outputItemId; }
                    public int outputQty() { return r.outputQty; }
                });
            }
            return out;
        }
        public Map<String, Integer> materials() {
            return new LinkedHashMap<String, Integer>(model.player().inventory());
        }
    }

    static final class AuctionViewImpl implements Views.AuctionView {
        private final CraftSystem craft;
        private final com.whim.alganon.api.GameModel model;
        private final Content content;
        AuctionViewImpl(CraftSystem craft, com.whim.alganon.api.GameModel model, Content content) {
            this.craft = craft; this.model = model; this.content = content;
        }
        public List<ListingView> listings() {
            List<ListingView> out = new ArrayList<ListingView>();
            for (final Listing l : craft.listings()) {
                final Defs.ItemDef d = content.item(l.itemId);
                out.add(new ListingView() {
                    public String listingId() { return l.listingId; }
                    public String itemName() { return d != null ? d.name : l.itemId; }
                    public int quantity() { return l.quantity; }
                    public long price() { return l.price; }
                    public boolean sellerIsPlayer() { return l.sellerIsPlayer; }
                });
            }
            return out;
        }
        public long playerGold() { return model.player().gold(); }
    }

    // ---------------- family ----------------

    static final class FamilyViewImpl implements Views.FamilyView {
        private final Defs.FamilyDef fam;
        private final List<String> members;
        private final String bonus;
        private final String vendorNpcId;
        FamilyViewImpl(Defs.FamilyDef fam, List<String> members, String bonus, String vendorNpcId) {
            this.fam = fam; this.members = members; this.bonus = bonus; this.vendorNpcId = vendorNpcId;
        }
        public String familyName() { return fam != null ? fam.name : "—"; }
        public FamilyArchetype archetype() { return fam != null ? fam.archetype : FamilyArchetype.ACHIEVER; }
        public String bonusDescription() { return bonus; }
        public List<String> memberNames() { return members; }
        public String vendorNpcId() { return vendorNpcId; }
    }

    // ---------------- chat ----------------

    static final class ChatLineViewImpl implements Views.ChatLineView {
        private final ChatChannel ch;
        private final String text;
        ChatLineViewImpl(ChatChannel ch, String text) { this.ch = ch; this.text = text; }
        public ChatChannel channel() { return ch; }
        public String text() { return text; }
    }
}
