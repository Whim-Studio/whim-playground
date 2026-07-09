package com.whim.alganon.ui.stub;

import com.whim.alganon.api.Defs;
import com.whim.alganon.api.Enums.AbilityKind;
import com.whim.alganon.api.Enums.ClassId;
import com.whim.alganon.api.Enums.DamageType;
import com.whim.alganon.api.Enums.Faction;
import com.whim.alganon.api.Enums.FamilyArchetype;
import com.whim.alganon.api.Enums.QuestStatus;
import com.whim.alganon.api.Enums.ResourceType;
import com.whim.alganon.api.Enums.School;
import com.whim.alganon.api.Enums.SkillType;
import com.whim.alganon.api.Enums.StatType;
import com.whim.alganon.api.Enums.TargetType;
import com.whim.alganon.api.Views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny hand-authored content pack for the UI stub. This is NOT the real content layer
 * (Task 1 owns {@code com.whim.alganon.content}); it only exists so the wizard, library,
 * crafting and family panels render believable, non-empty data while running standalone.
 * All flavor text is written fresh (clean-room).
 */
final class StubContent {

    private final Map<String, Defs.RaceDef> races = new LinkedHashMap<String, Defs.RaceDef>();
    private final Map<String, Defs.FamilyDef> families = new LinkedHashMap<String, Defs.FamilyDef>();
    private final Map<String, Defs.ClassDef> classes = new LinkedHashMap<String, Defs.ClassDef>();
    private final Map<String, Defs.AbilityDef> abilities = new LinkedHashMap<String, Defs.AbilityDef>();
    private final Map<String, Defs.ItemDef> items = new LinkedHashMap<String, Defs.ItemDef>();

    StubContent() { buildRaces(); buildFamilies(); buildAbilities(); buildClasses(); buildItems(); }

    // ---------------- lookups ----------------
    List<Defs.RaceDef> races() { return new ArrayList<Defs.RaceDef>(races.values()); }
    List<Defs.FamilyDef> families() { return new ArrayList<Defs.FamilyDef>(families.values()); }
    List<Defs.ClassDef> classes() { return new ArrayList<Defs.ClassDef>(classes.values()); }
    Defs.RaceDef race(String id) { return id == null ? null : races.get(id); }
    Defs.FamilyDef family(String id) { return id == null ? null : families.get(id); }
    Defs.ClassDef clazz(String id) { return id == null ? null : classes.get(id); }
    Defs.AbilityDef ability(String id) { return id == null ? null : abilities.get(id); }
    Defs.ItemDef item(String id) { return id == null ? null : items.get(id); }

    List<Defs.FamilyDef> familiesForRace(String raceId) {
        Defs.RaceDef r = race(raceId);
        List<Defs.FamilyDef> out = new ArrayList<Defs.FamilyDef>();
        if (r == null) return out;
        for (Defs.FamilyDef f : families.values()) if (f.faction == r.faction) out.add(f);
        return out;
    }

    // ---------------- content authoring ----------------
    private void buildRaces() {
        Map<StatType, Integer> asharr = stats(2, 0, 0, 1, 2);
        races.put("asharr", new Defs.RaceDef("asharr", "Asharr (Human)", Faction.ASHARR, asharr,
                "Disciplined settlers of the Order who prize law, walls, and the long defense. "
                        + "Balanced and resilient, the Asharr adapt to any calling."));
        Map<StatType, Integer> kujix = stats(0, 2, 1, 0, 2);
        races.put("kujix", new Defs.RaceDef("kujix", "Kujix (Talrok)", Faction.KUJIX, kujix,
                "Ambitious conquerors of the expanding frontier. The Kujix favor aggression and "
                        + "reach, trading a little safety for raw momentum."));
    }

    private void buildFamilies() {
        // Asharr families (Order)
        fam("asharr_wardens", "Wardens", Faction.ASHARR, FamilyArchetype.ACHIEVER,
                "Steady advancers who chase mastery — a small standing bonus to experience and study.");
        fam("asharr_vanguard", "Vanguard", Faction.ASHARR, FamilyArchetype.COMPETITOR,
                "Front-line rivals who sharpen their edge in every fight — a slight combat bonus.");
        fam("asharr_pathfinders", "Pathfinders", Faction.ASHARR, FamilyArchetype.EXPLORER,
                "Mapmakers of the Order's reaches — biased toward discovery and travel work.");
        fam("asharr_covenant", "Covenant", Faction.ASHARR, FamilyArchetype.SOCIALIZER,
                "Diplomats and quartermasters — better vendor prices and reputation gains.");
        fam("asharr_artificers", "Artificers", Faction.ASHARR, FamilyArchetype.CRAFTER,
                "Guild smiths of the walled cities — a bonus to tradeskill progress.");
        // Kujix families (Chaos)
        fam("kujix_reavers", "Bloodreavers", Faction.KUJIX, FamilyArchetype.COMPETITOR,
                "Raiders who measure worth in conquest — a slight combat bonus.");
        fam("kujix_ascendant", "Ascendant", Faction.KUJIX, FamilyArchetype.ACHIEVER,
                "Relentless climbers of rank — a standing bonus to experience and study.");
        fam("kujix_outriders", "Outriders", Faction.KUJIX, FamilyArchetype.EXPLORER,
                "Scouts of the widening frontier — biased toward discovery and travel work.");
        fam("kujix_brokers", "Brokers", Faction.KUJIX, FamilyArchetype.SOCIALIZER,
                "Warband negotiators — better vendor prices and reputation gains.");
        fam("kujix_forgeborn", "Forgeborn", Faction.KUJIX, FamilyArchetype.CRAFTER,
                "Fire-tenders of the horde — a bonus to tradeskill progress.");
    }

    private void fam(String id, String name, Faction f, FamilyArchetype a, String desc) {
        families.put(id, new Defs.FamilyDef(id, name, f, a, desc));
    }

    private void buildAbilities() {
        // Champion (Fury + stances)
        ab("ch_strike", "Cleaving Strike", ClassId.CHAMPION, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL, 1, 10, 1.5, 0, 24, 0,
                "A heavy overhead blow that builds threat and spends Fury.");
        ab("ch_guard", "Bulwark", ClassId.CHAMPION, AbilityKind.BUFF, TargetType.SELF, DamageType.PHYSICAL, 2, 15, 12, 0, 0, 8,
                "Brace behind your shield, sharply raising armor for a time.");
        ab("ch_stance", "Shift Stance", ClassId.CHAMPION, AbilityKind.STANCE, TargetType.SELF, DamageType.PHYSICAL, 1, 0, 3, 0, 0, 0,
                "Rotate between Balance, Power, and Defense stances.");
        ab("ch_shout", "Rallying Shout", ClassId.CHAMPION, AbilityKind.BUFF, TargetType.SELF, DamageType.PHYSICAL, 4, 20, 30, 0, 0, 15,
                "A war cry that bolsters might for you and nearby allies.");
        // Reaver (Fury, bleeds/execute)
        ab("rv_rend", "Rend", ClassId.REAVER, AbilityKind.DOT, TargetType.ENEMY, DamageType.PHYSICAL, 1, 12, 4, 0, 8, 9,
                "Open a bleeding wound that ticks damage over time.");
        ab("rv_exec", "Reap", ClassId.REAVER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.SHADOW, 3, 20, 8, 0, 40, 0,
                "A finisher that strikes far harder against wounded foes.");
        ab("rv_slash", "Savage Slash", ClassId.REAVER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL, 1, 8, 1.2, 0, 18, 0,
                "A quick slash that generates Fury.");
        // Ranger (Focus + pet/trap/track)
        ab("rn_shot", "Aimed Shot", ClassId.RANGER, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.PHYSICAL, 1, 10, 2, 1.0, 26, 0,
                "A carefully aimed arrow with a short draw time.");
        ab("rn_pet", "Summon Companion", ClassId.RANGER, AbilityKind.PET_SUMMON, TargetType.NONE, DamageType.PHYSICAL, 2, 25, 20, 0, 0, 0,
                "Call a loyal beast to fight at your side.");
        ab("rn_trap", "Snare Trap", ClassId.RANGER, AbilityKind.TRAP, TargetType.GROUND, DamageType.PHYSICAL, 3, 15, 10, 0, 12, 6,
                "Set a trap that roots and wounds the first foe to trip it.");
        ab("rn_track", "Track", ClassId.RANGER, AbilityKind.UTILITY, TargetType.NONE, DamageType.PHYSICAL, 1, 5, 8, 0, 0, 20,
                "Reveal nearby creatures and quest marks on the minimap.");
        // Magus (Mana + schools)
        ab("mg_flame", "Flame Bolt", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.FLAME, 1, 12, 1.5, 1.2, 30, 0,
                "Hurl a bolt of fire — Flame school leans on burning damage.");
        ab("mg_frost", "Frost Lance", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.FROST, 2, 14, 3, 1.0, 22, 4,
                "A lance of ice that damages and slows — the Frost school's control tool.");
        ab("mg_storm", "Storm Arc", ClassId.MAGUS, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.STORM, 3, 18, 6, 1.5, 34, 0,
                "Chain lightning that bursts a single target — a Storm signature.");
        ab("mg_school", "Attune School", ClassId.MAGUS, AbilityKind.UTILITY, TargetType.SELF, DamageType.FLAME, 1, 0, 3, 0, 0, 0,
                "Attune to Flame, Frost, or Storm, weighting your spells.");
        // Mystic (Mana, Words/Touches)
        ab("my_word", "Word of Mending", ClassId.MYSTIC, AbilityKind.HEAL, TargetType.ALLY, DamageType.HOLY, 1, 14, 2.5, 1.5, 32, 0,
                "A spoken 'Word' that heals over a short cast.");
        ab("my_hot", "Blessing", ClassId.MYSTIC, AbilityKind.HOT, TargetType.ALLY, DamageType.HOLY, 2, 18, 8, 0, 10, 12,
                "A lingering blessing that restores health each tick.");
        ab("my_touch", "Touch of Warding", ClassId.MYSTIC, AbilityKind.HEAL, TargetType.ALLY, DamageType.HOLY, 1, 10, 1, 0, 18, 0,
                "An instant 'Touch' — a quick melee-range heal.");
        ab("my_smite", "Searing Touch", ClassId.MYSTIC, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.HOLY, 3, 12, 3, 0, 24, 0,
                "A punishing 'Touch' that smites a foe with holy fire.");
        // Cabalist (Mana, curses/summon/drain)
        ab("cb_curse", "Withering Curse", ClassId.CABALIST, AbilityKind.DOT, TargetType.ENEMY, DamageType.SHADOW, 1, 14, 5, 1.0, 10, 12,
                "A curse that rots a foe from within over time.");
        ab("cb_minion", "Summon Minion", ClassId.CABALIST, AbilityKind.PET_SUMMON, TargetType.NONE, DamageType.SHADOW, 2, 25, 20, 0, 0, 0,
                "Bind a shadow minion to serve you.");
        ab("cb_drain", "Life Drain", ClassId.CABALIST, AbilityKind.DAMAGE, TargetType.ENEMY, DamageType.SHADOW, 1, 12, 2, 1.5, 20, 0,
                "Siphon a foe's vitality to yourself.");
        ab("cb_hex", "Enfeebling Hex", ClassId.CABALIST, AbilityKind.DEBUFF, TargetType.ENEMY, DamageType.SHADOW, 3, 16, 15, 0, 0, 10,
                "Weaken an enemy's strikes for a time.");
    }

    private void ab(String id, String name, ClassId owner, AbilityKind kind, TargetType tgt, DamageType dt,
                    int lvl, int cost, double cd, double cast, int power, double dur, String desc) {
        School sc = School.NONE;
        if (id.startsWith("mg_flame")) sc = School.FLAME;
        else if (id.startsWith("mg_frost")) sc = School.FROST;
        else if (id.startsWith("mg_storm")) sc = School.STORM;
        abilities.put(id, new Defs.AbilityDef(id, name, owner, kind, tgt, dt, lvl, cost, cd, cast, power, dur, sc, desc));
    }

    private void buildClasses() {
        clz(ClassId.CHAMPION, "Champion", ResourceType.FURY, Arrays.asList("ch_strike", "ch_stance", "ch_guard", "ch_shout"),
                "A frontline warrior who shifts between Balance, Power, and Defense stances, building Fury with each blow.");
        clz(ClassId.REAVER, "Reaver", ResourceType.FURY, Arrays.asList("rv_slash", "rv_rend", "rv_exec"),
                "A dark melee duelist who stacks bleeds and executes wounded foes for lethal bursts.");
        clz(ClassId.RANGER, "Ranger", ResourceType.FOCUS, Arrays.asList("rn_shot", "rn_track", "rn_pet", "rn_trap"),
                "A ranged hunter with a loyal pet, ground traps, and tracking that reveals nearby quarry.");
        clz(ClassId.MAGUS, "Magus", ResourceType.MANA, Arrays.asList("mg_flame", "mg_school", "mg_frost", "mg_storm"),
                "An elemental caster who attunes to Flame, Frost, or Storm to burn, control, or blast.");
        clz(ClassId.MYSTIC, "Mystic", ResourceType.MANA, Arrays.asList("my_touch", "my_word", "my_hot", "my_smite"),
                "A healer who weaves cast-time 'Words' and instant 'Touches' of restorative power.");
        clz(ClassId.CABALIST, "Cabalist", ResourceType.MANA, Arrays.asList("cb_drain", "cb_curse", "cb_minion", "cb_hex"),
                "A shadow caster of curses, drains, and a summoned minion that grinds enemies down.");
    }

    private void clz(ClassId id, String name, ResourceType res, List<String> abils, String desc) {
        classes.put(name.toLowerCase(), new Defs.ClassDef(id, name, res, abils, desc));
    }

    private void buildItems() {
        items.put("rusted_sword", new Defs.ItemDef("rusted_sword", "Rusted Shortsword", com.whim.alganon.api.Enums.ItemType.WEAPON,
                com.whim.alganon.api.Enums.EquipSlot.WEAPON, stats(1, 0, 0, 0, 0), 6, 8, 1, "A pitted blade — better than fists."));
        items.put("padded_vest", new Defs.ItemDef("padded_vest", "Padded Vest", com.whim.alganon.api.Enums.ItemType.ARMOR,
                com.whim.alganon.api.Enums.EquipSlot.CHEST, stats(0, 0, 0, 0, 2), 4, 6, 1, "Layered cloth that turns a glancing blow."));
        items.put("healing_draught", new Defs.ItemDef("healing_draught", "Healing Draught", com.whim.alganon.api.Enums.ItemType.CONSUMABLE,
                null, null, 30, 5, 10, "Restores a measure of health when quaffed."));
        items.put("copper_ore", new Defs.ItemDef("copper_ore", "Copper Ore", com.whim.alganon.api.Enums.ItemType.MATERIAL,
                null, null, 0, 2, 50, "Raw ore, ready to be smelted."));
        items.put("copper_bar", new Defs.ItemDef("copper_bar", "Copper Bar", com.whim.alganon.api.Enums.ItemType.MATERIAL,
                null, null, 0, 5, 50, "A smelted ingot for the forge."));
    }

    private static Map<StatType, Integer> stats(int might, int fin, int intel, int spirit, int stam) {
        Map<StatType, Integer> m = new EnumMap<StatType, Integer>(StatType.class);
        m.put(StatType.MIGHT, might); m.put(StatType.FINESSE, fin); m.put(StatType.INTELLECT, intel);
        m.put(StatType.SPIRIT, spirit); m.put(StatType.STAMINA, stam);
        return m;
    }

    // ---------------- view builders for panels ----------------

    List<Views.QuestView> demoQuests() {
        List<Views.QuestView> out = new ArrayList<Views.QuestView>();
        out.add(new QuestV("q_boars", "Thin the Boars", "Aldric asks you to cull the boars menacing the outer fields.",
                QuestStatus.ACTIVE, false, Arrays.asList(new ObjV("Slay frontier boars", 3, 5, false))));
        out.add(new QuestV("q_ore", "Ore for the Forge", "Gather copper ore for the family forge.",
                QuestStatus.ACTIVE, false, Arrays.asList(new ObjV("Mine copper ore", 4, 4, true))));
        out.add(new QuestV("q_scout", "Scout the Ridge", "A generated task: travel to the eastern ridge marker.",
                QuestStatus.READY_TO_TURN_IN, true, Arrays.asList(new ObjV("Reach the ridge", 1, 1, true))));
        return out;
    }

    Views.CraftingView craftingView() {
        List<Views.CraftingView.RecipeProgressView> recipes = new ArrayList<Views.CraftingView.RecipeProgressView>();
        Map<String, Integer> smeltIn = new LinkedHashMap<String, Integer>(); smeltIn.put("copper_ore", 2);
        recipes.add(new RecipeV("r_smelt", "Smelt Copper Bar", SkillType.PROCESSING, true, smeltIn, "Copper Bar", 1));
        Map<String, Integer> bladeIn = new LinkedHashMap<String, Integer>(); bladeIn.put("copper_bar", 3);
        recipes.add(new RecipeV("r_blade", "Forge Copper Blade", SkillType.CRAFTING, false, bladeIn, "Copper Shortsword", 1));
        Map<String, Integer> mats = new LinkedHashMap<String, Integer>();
        mats.put("copper_ore", 4); mats.put("copper_bar", 1);
        return new CraftingV(recipes, mats);
    }

    Views.AuctionView auctionView(long playerGold) {
        List<Views.AuctionView.ListingView> l = new ArrayList<Views.AuctionView.ListingView>();
        l.add(new ListingV("a1", "Healing Draught", 5, 25, false));
        l.add(new ListingV("a2", "Copper Bar", 10, 60, false));
        l.add(new ListingV("a3", "Padded Vest", 1, 45, false));
        l.add(new ListingV("a4", "Copper Ore", 20, 40, true));
        return new AuctionV(l, playerGold);
    }

    Views.FamilyView familyView(String familyId) {
        Defs.FamilyDef f = family(familyId);
        String name = f != null ? f.name : "Unaffiliated";
        FamilyArchetype a = f != null ? f.archetype : FamilyArchetype.ACHIEVER;
        String bonus = f != null ? f.description : "No family bonus.";
        List<String> members = Arrays.asList("Elder " + name.substring(0, Math.min(4, name.length())) + "in",
                "Quartermaster Vos", "Scout Rell", "Apprentice Mira");
        return new FamilyV(name, a, bonus, members, "npc_family_vendor");
    }

    // ---- view record-likes (Java 8 plain classes) ----
    private static final class QuestV implements Views.QuestView {
        private final String id, name, desc; private final QuestStatus st; private final boolean proc;
        private final List<Views.QuestView.ObjectiveProgressView> objs;
        QuestV(String id, String name, String desc, QuestStatus st, boolean proc, List<Views.QuestView.ObjectiveProgressView> objs) {
            this.id = id; this.name = name; this.desc = desc; this.st = st; this.proc = proc; this.objs = objs; }
        public String id() { return id; } public String name() { return name; } public String description() { return desc; }
        public QuestStatus status() { return st; } public boolean procedural() { return proc; }
        public List<Views.QuestView.ObjectiveProgressView> objectives() { return objs; }
    }
    private static final class ObjV implements Views.QuestView.ObjectiveProgressView {
        private final String text; private final int cur, req; private final boolean done;
        ObjV(String text, int cur, int req, boolean done) { this.text = text; this.cur = cur; this.req = req; this.done = done; }
        public String text() { return text; } public int current() { return cur; } public int required() { return req; } public boolean done() { return done; }
    }
    private static final class RecipeV implements Views.CraftingView.RecipeProgressView {
        private final String id, name, out; private final SkillType skill; private final boolean craftable;
        private final Map<String, Integer> in; private final int qty;
        RecipeV(String id, String name, SkillType skill, boolean craftable, Map<String, Integer> in, String out, int qty) {
            this.id = id; this.name = name; this.skill = skill; this.craftable = craftable; this.in = in; this.out = out; this.qty = qty; }
        public String id() { return id; } public String name() { return name; } public SkillType skill() { return skill; }
        public boolean craftable() { return craftable; } public Map<String, Integer> inputs() { return in; }
        public String outputName() { return out; } public int outputQty() { return qty; }
    }
    private static final class CraftingV implements Views.CraftingView {
        private final List<Views.CraftingView.RecipeProgressView> recipes; private final Map<String, Integer> mats;
        CraftingV(List<Views.CraftingView.RecipeProgressView> r, Map<String, Integer> m) { this.recipes = r; this.mats = m; }
        public List<Views.CraftingView.RecipeProgressView> recipes() { return recipes; }
        public Map<String, Integer> materials() { return mats; }
    }
    private static final class ListingV implements Views.AuctionView.ListingView {
        private final String id, name; private final int qty; private final long price; private final boolean mine;
        ListingV(String id, String name, int qty, long price, boolean mine) { this.id = id; this.name = name; this.qty = qty; this.price = price; this.mine = mine; }
        public String listingId() { return id; } public String itemName() { return name; } public int quantity() { return qty; }
        public long price() { return price; } public boolean sellerIsPlayer() { return mine; }
    }
    private static final class AuctionV implements Views.AuctionView {
        private final List<Views.AuctionView.ListingView> l; private final long gold;
        AuctionV(List<Views.AuctionView.ListingView> l, long gold) { this.l = l; this.gold = gold; }
        public List<Views.AuctionView.ListingView> listings() { return l; } public long playerGold() { return gold; }
    }
    private static final class FamilyV implements Views.FamilyView {
        private final String name, bonus, vendor; private final FamilyArchetype a; private final List<String> members;
        FamilyV(String name, FamilyArchetype a, String bonus, List<String> members, String vendor) {
            this.name = name; this.a = a; this.bonus = bonus; this.members = members; this.vendor = vendor; }
        public String familyName() { return name; } public FamilyArchetype archetype() { return a; }
        public String bonusDescription() { return bonus; } public List<String> memberNames() { return members; }
        public String vendorNpcId() { return vendor; }
    }
}
