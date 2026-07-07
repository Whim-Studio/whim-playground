package com.whim.albion.data;

import com.whim.albion.api.Combatant;
import com.whim.albion.api.Content;
import com.whim.albion.api.Defs.ItemDef;
import com.whim.albion.api.Defs.MonsterDef;
import com.whim.albion.api.Defs.SpellDef;
import com.whim.albion.api.Enums.DamageType;
import com.whim.albion.api.Enums.EnemyBehaviorType;
import com.whim.albion.api.Enums.EquipSlot;
import com.whim.albion.api.Enums.ItemType;
import com.whim.albion.api.Enums.SpellEffectType;
import com.whim.albion.api.Enums.SpellSchool;
import com.whim.albion.api.Enums.StatType;
import com.whim.albion.api.Enums.TargetType;
import com.whim.albion.api.GridPos;
import com.whim.albion.entities.EnemyCombatant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The invented content pack for the recreation: items, spells, monsters,
 * encounters and dialogue trees, all keyed by string ids. Clean-room flavor —
 * the town of "Duskhollow", the "Sunken Relic" quest, generic-descriptive magic
 * schools; no names or text from the original 1995 game.
 */
public final class AlbionContent implements Content {

    // --- item ids ---
    public static final String ITM_SHORT_SWORD   = "itm_short_sword";
    public static final String ITM_HUNT_BOW      = "itm_hunt_bow";
    public static final String ITM_OAK_STAFF     = "itm_oak_staff";
    public static final String ITM_LEATHER_VEST  = "itm_leather_vest";
    public static final String ITM_ROUND_SHIELD  = "itm_round_shield";
    public static final String ITM_IRON_CAP      = "itm_iron_cap";
    public static final String ITM_SUN_AMULET    = "itm_sun_amulet";
    public static final String ITM_HEAL_DRAUGHT  = "itm_healing_draught";
    public static final String ITM_MANA_TONIC    = "itm_mana_tonic";
    public static final String ITM_SCROLL_SPARK  = "itm_scroll_spark";
    public static final String ITM_RUSTY_KEY     = "itm_rusty_key";
    public static final String ITM_RELIC_SHARD   = "itm_relic_shard";

    // --- spell ids ---
    public static final String SPELL_SPARK    = "spell_spark";
    public static final String SPELL_FIREBOLT = "spell_firebolt";
    public static final String SPELL_THORN    = "spell_thorn";
    public static final String SPELL_REGROWTH = "spell_regrowth";
    public static final String SPELL_MEND     = "spell_mend";
    public static final String SPELL_RENEWAL  = "spell_renewal";
    public static final String SPELL_DAZE     = "spell_daze";
    public static final String SPELL_FOCUS    = "spell_focus";

    // --- monster ids ---
    public static final String MON_SKITTERLING = "mon_skitterling";
    public static final String MON_SLINGER     = "mon_slinger";
    public static final String MON_FENMYSTIC   = "mon_fenmystic";

    // --- encounter ids ---
    public static final String ENC_DUNGEON_1 = "enc_dungeon1";
    public static final String ENC_DUNGEON_2 = "enc_dungeon2";

    // --- dialogue ids ---
    public static final String DLG_ELDER    = "dlg_elder";
    public static final String DLG_SHOP     = "dlg_shop";
    public static final String DLG_VILLAGER = "dlg_villager";
    public static final String DLG_DOOR     = "dlg_iron_door";

    // --- quest / flags ---
    public static final String QUEST_RELIC   = "quest_relic";
    public static final String FLAG_QUEST_ON = "quest_relic_started";
    public static final String FLAG_QUEST_DONE = "quest_relic_done";
    public static final String FLAG_SHOP_OPEN = "shop_open";
    public static final String FLAG_DOOR_OPEN = "crypt_door_open";

    private final Map<String, ItemDef> items = new HashMap<String, ItemDef>();
    private final Map<String, SpellDef> spells = new HashMap<String, SpellDef>();
    private final Map<String, MonsterDef> monsters = new HashMap<String, MonsterDef>();
    private final Map<String, List<String>> encounters = new HashMap<String, List<String>>();
    private final Map<String, DialogueTreeImpl> dialogues = new HashMap<String, DialogueTreeImpl>();

    public AlbionContent() {
        buildItems();
        buildSpells();
        buildMonsters();
        buildEncounters();
        buildDialogues();
    }

    // ------------------------------------------------------------- Content API

    @Override public ItemDef item(String id) { return items.get(id); }
    @Override public SpellDef spell(String id) { return spells.get(id); }
    @Override public MonsterDef monster(String id) { return monsters.get(id); }

    @Override public List<Combatant> spawnEncounter(String encounterId) {
        List<Combatant> out = new ArrayList<Combatant>();
        List<String> ids = encounters.get(encounterId);
        if (ids == null) return out;
        int i = 0;
        for (String monId : ids) {
            MonsterDef d = monsters.get(monId);
            if (d == null) continue;
            List<SpellDef> known = new ArrayList<SpellDef>();
            if (d.behavior == EnemyBehaviorType.SUPPORT) {
                SpellDef mend = spells.get(SPELL_REGROWTH);
                if (mend != null) known.add(mend);
            }
            EnemyCombatant e = new EnemyCombatant(d, i, known);
            e.setPos(new GridPos(i, 0)); // engine repositions on the battlefield
            out.add(e);
            i++;
        }
        return out;
    }

    @Override public DialogueTree dialogue(String dialogueId) { return dialogues.get(dialogueId); }

    // ------------------------------------------------------------------ items

    private void buildItems() {
        put(ItemDef.builder(ITM_SHORT_SWORD, "Short Sword", ItemType.WEAPON)
                .slot(EquipSlot.WEAPON).attack(6).value(40).weight(3)
                .damageType(DamageType.PHYSICAL).sprite("weapon.sword")
                .description("A reliable soldier's blade.").build());
        put(ItemDef.builder(ITM_HUNT_BOW, "Hunter's Bow", ItemType.WEAPON)
                .slot(EquipSlot.WEAPON).attack(5).value(45).weight(2)
                .damageType(DamageType.PHYSICAL).sprite("weapon.bow")
                .description("A supple yew bow for striking from afar.").build());
        put(ItemDef.builder(ITM_OAK_STAFF, "Oaken Staff", ItemType.WEAPON)
                .slot(EquipSlot.WEAPON).attack(3).value(35).weight(3)
                .statBonus(StatType.MAGIC_TALENT, 2).sprite("weapon.staff")
                .description("Focuses a caster's will; light in a scholar's hand.").build());
        put(ItemDef.builder(ITM_LEATHER_VEST, "Boiled Leather Vest", ItemType.ARMOR)
                .slot(EquipSlot.BODY).defense(4).value(50).weight(5).sprite("armor.leather")
                .description("Layered hide that turns a glancing blow.").build());
        put(ItemDef.builder(ITM_ROUND_SHIELD, "Round Shield", ItemType.SHIELD)
                .slot(EquipSlot.SHIELD).defense(3).value(30).weight(4).sprite("armor.shield")
                .description("A banded wooden buckler.").build());
        put(ItemDef.builder(ITM_IRON_CAP, "Iron Cap", ItemType.HELMET)
                .slot(EquipSlot.HEAD).defense(2).value(25).weight(2).sprite("armor.helm")
                .description("A simple dented skullcap.").build());
        put(ItemDef.builder(ITM_SUN_AMULET, "Amulet of the Pale Sun", ItemType.ARMOR)
                .slot(EquipSlot.ACCESSORY).defense(1).statBonus(StatType.LUCK, 3)
                .value(80).weight(1).sprite("accessory.amulet")
                .description("Warm to the touch; the wearer feels fortunate.").build());
        put(ItemDef.builder(ITM_HEAL_DRAUGHT, "Healing Draught", ItemType.CONSUMABLE)
                .heal(25).value(20).weight(1).sprite("potion.red")
                .description("A bitter red tonic that knits wounds.").build());
        put(ItemDef.builder(ITM_MANA_TONIC, "Azure Tonic", ItemType.CONSUMABLE)
                .mana(15).value(22).weight(1).sprite("potion.blue")
                .description("Clears the mind and restores spell energy.").build());
        put(ItemDef.builder(ITM_SCROLL_SPARK, "Scroll of Sparks", ItemType.SCROLL)
                .spellId(SPELL_SPARK).value(30).weight(1).sprite("scroll.spark")
                .description("Teaches Spark to a willing student of Destruction.").build());
        put(ItemDef.builder(ITM_RUSTY_KEY, "Rusty Crypt Key", ItemType.KEY)
                .value(0).weight(1).sprite("key.rusty")
                .description("A corroded iron key. It fits an old lock.").build());
        put(ItemDef.builder(ITM_RELIC_SHARD, "Shard of the Sunken Relic", ItemType.QUEST)
                .value(0).weight(1).sprite("quest.shard")
                .description("A humming fragment of pale crystal. The Steward wants this.").build());
    }

    // ------------------------------------------------------------------ spells

    private void buildSpells() {
        put(SpellDef.builder(SPELL_SPARK, "Spark", SpellSchool.DESTRUCTION)
                .effect(SpellEffectType.DAMAGE).target(TargetType.SINGLE_ENEMY)
                .damageType(DamageType.FIRE).spCost(2).magnitude(8).levelReq(1).talentReq(0)
                .description("A snapping arc of flame.").build());
        put(SpellDef.builder(SPELL_FIREBOLT, "Firebolt", SpellSchool.DESTRUCTION)
                .effect(SpellEffectType.DAMAGE).target(TargetType.SINGLE_ENEMY)
                .damageType(DamageType.FIRE).spCost(5).magnitude(16).levelReq(3).talentReq(12)
                .description("A lance of roaring fire.").build());
        put(SpellDef.builder(SPELL_THORN, "Thorn Lash", SpellSchool.NATURE)
                .effect(SpellEffectType.DAMAGE).target(TargetType.SINGLE_ENEMY)
                .damageType(DamageType.POISON).spCost(3).magnitude(10).levelReq(2).talentReq(8)
                .description("Barbed vines rake a foe.").build());
        put(SpellDef.builder(SPELL_REGROWTH, "Regrowth", SpellSchool.NATURE)
                .effect(SpellEffectType.HEAL).target(TargetType.SINGLE_ALLY)
                .damageType(DamageType.MAGIC).spCost(3).magnitude(14).levelReq(1).talentReq(0)
                .description("Coaxes flesh to mend like green wood.").build());
        put(SpellDef.builder(SPELL_MEND, "Mend Wounds", SpellSchool.RESTORATION)
                .effect(SpellEffectType.HEAL).target(TargetType.SINGLE_ALLY)
                .damageType(DamageType.MAGIC).spCost(2).magnitude(18).levelReq(1).talentReq(0)
                .description("A soothing white light closes cuts.").build());
        put(SpellDef.builder(SPELL_RENEWAL, "Renewal", SpellSchool.RESTORATION)
                .effect(SpellEffectType.HEAL).target(TargetType.ALL_ALLIES)
                .damageType(DamageType.MAGIC).spCost(6).magnitude(12).levelReq(4).talentReq(14)
                .description("A wave of vigor washes over the party.").build());
        put(SpellDef.builder(SPELL_DAZE, "Daze", SpellSchool.PSIONIC)
                .effect(SpellEffectType.DEBUFF).target(TargetType.SINGLE_ENEMY)
                .damageType(DamageType.MAGIC).spCost(3).magnitude(4).levelReq(2).talentReq(6)
                .description("Muddles a foe's mind, blunting its defense.").build());
        put(SpellDef.builder(SPELL_FOCUS, "Inner Focus", SpellSchool.PSIONIC)
                .effect(SpellEffectType.BUFF).target(TargetType.SELF)
                .damageType(DamageType.MAGIC).spCost(2).magnitude(5).levelReq(1).talentReq(0)
                .description("Sharpens the caster's aim and reflexes.").build());
    }

    // ---------------------------------------------------------------- monsters

    private void buildMonsters() {
        put(MonsterDef.builder(MON_SKITTERLING, "Cave Skitterling")
                .behavior(EnemyBehaviorType.AGGRESSIVE)
                .stat(StatType.STRENGTH, 9).stat(StatType.SPEED, 12).stat(StatType.DEXTERITY, 10)
                .stat(StatType.STAMINA, 6)
                .maxLp(18).attack(6).defense(1).damageType(DamageType.PHYSICAL)
                .xp(14).gold(6).sprite("mon.skitterling").build());
        put(MonsterDef.builder(MON_SLINGER, "Bog Slinger")
                .behavior(EnemyBehaviorType.RANGED)
                .stat(StatType.STRENGTH, 7).stat(StatType.SPEED, 9).stat(StatType.DEXTERITY, 13)
                .stat(StatType.STAMINA, 5)
                .maxLp(14).attack(7).defense(0).damageType(DamageType.PHYSICAL)
                .xp(16).gold(9).sprite("mon.slinger").build());
        put(MonsterDef.builder(MON_FENMYSTIC, "Fen Mystic")
                .behavior(EnemyBehaviorType.SUPPORT)
                .stat(StatType.INTELLIGENCE, 13).stat(StatType.SPEED, 8).stat(StatType.MAGIC_TALENT, 12)
                .stat(StatType.STAMINA, 6)
                .maxLp(16).maxSp(20).attack(4).defense(1).damageType(DamageType.MAGIC)
                .xp(22).gold(14).sprite("mon.fenmystic").build());
    }

    private void buildEncounters() {
        encounters.put(ENC_DUNGEON_1,
                new ArrayList<String>(Arrays.asList(MON_SKITTERLING, MON_SKITTERLING, MON_SLINGER)));
        encounters.put(ENC_DUNGEON_2,
                new ArrayList<String>(Arrays.asList(MON_FENMYSTIC, MON_SKITTERLING)));
    }

    // --------------------------------------------------------------- dialogues

    private void buildDialogues() {
        dialogues.put(DLG_ELDER, buildElderDialogue());
        dialogues.put(DLG_SHOP, buildShopDialogue());
        dialogues.put(DLG_VILLAGER, buildVillagerDialogue());
        dialogues.put(DLG_DOOR, buildDoorDialogue());
    }

    private DialogueTreeImpl buildElderDialogue() {
        DialogueTreeImpl t = new DialogueTreeImpl("start");
        String who = "Steward Maelen";
        String pk = "portrait.steward";
        t.addNode("start", who, pk,
                "Welcome, wanderer. Duskhollow has known better days.")
                .option("Tell me about the missing relic.", "give_quest", null,
                        ctx -> !ctx.flag(FLAG_QUEST_ON))
                .option("I have recovered the relic.", "turn_in", null,
                        ctx -> ctx.flag(FLAG_QUEST_ON) && !ctx.flag(FLAG_QUEST_DONE) && ctx.hasItem(ITM_RELIC_SHARD))
                .option("The relic is safe with you. Farewell.", null, null,
                        ctx -> ctx.flag(FLAG_QUEST_DONE))
                .option("Good day to you.", null, null,
                        ctx -> !ctx.flag(FLAG_QUEST_ON));
        t.addNode("give_quest", who, pk,
                "A shard of the Sunken Relic lies in the old crypt beneath the hill. "
                        + "Bring it to me before the damp claims it. Take this key for the inner door.")
                .option("I will bring it back.", null, ctx -> {
                    ctx.startQuest(QUEST_RELIC);
                    ctx.addObjective(QUEST_RELIC, "Descend into the crypt beneath Duskhollow.");
                    ctx.addObjective(QUEST_RELIC, "Recover the Shard of the Sunken Relic.");
                    ctx.setFlag(FLAG_QUEST_ON, true);
                    ctx.giveItem(ITM_RUSTY_KEY, 1);
                    ctx.notify("Quest started: The Sunken Relic. Received a Rusty Crypt Key.");
                });
        t.addNode("turn_in", who, pk,
                "You found it! I feared it lost forever. Duskhollow is in your debt — "
                        + "take this gold with my thanks.")
                .option("You are welcome.", null, ctx -> {
                    ctx.takeItem(ITM_RELIC_SHARD, 1);
                    ctx.completeQuest(QUEST_RELIC);
                    ctx.addGold(150);
                    ctx.setFlag(FLAG_QUEST_DONE, true);
                    ctx.notify("Quest complete: The Sunken Relic. Received 150 gold.");
                });
        return t;
    }

    private DialogueTreeImpl buildShopDialogue() {
        DialogueTreeImpl t = new DialogueTreeImpl("start");
        String who = "WEND, the Peddler";
        String pk = "portrait.peddler";
        t.addNode("start", who, pk,
                "Potions, oddments, a fair price for a brave face. What'll it be?")
                .option("Open your wares.", null, ctx -> {
                    ctx.setFlag(FLAG_SHOP_OPEN, true);
                    ctx.notify("The shop is open for business.");
                })
                .option("Buy a Healing Draught (20 gold).", null, ctx -> {
                    if (ctx.spendGold(20)) {
                        ctx.giveItem(ITM_HEAL_DRAUGHT, 1);
                        ctx.notify("Bought a Healing Draught.");
                    } else {
                        ctx.notify("Not enough gold.");
                    }
                })
                .option("Just browsing.", null);
        return t;
    }

    private DialogueTreeImpl buildVillagerDialogue() {
        DialogueTreeImpl t = new DialogueTreeImpl("start");
        t.addNode("start", "Fretful Villager", "portrait.villager",
                "Strange lights under the hill at night. I keep my shutters barred, I do.")
                .option("Stay safe.", null);
        return t;
    }

    private DialogueTreeImpl buildDoorDialogue() {
        DialogueTreeImpl t = new DialogueTreeImpl("start");
        t.addNode("start", "Iron Crypt Door", "portrait.door",
                "A heavy iron door bars the way. Its lock is old but sound.")
                .option("Use the Rusty Crypt Key.", null, ctx -> {
                    ctx.takeItem(ITM_RUSTY_KEY, 1);
                    ctx.setFlag(FLAG_DOOR_OPEN, true);
                    ctx.notify("The lock yields with a groan.");
                    ctx.teleport(MapFactory.MAP_CRYPT, MapFactory.CRYPT_VAULT_X, MapFactory.CRYPT_VAULT_Y,
                            com.whim.albion.api.Enums.Direction.EAST);
                }, ctx -> ctx.hasItem(ITM_RUSTY_KEY))
                .option("Leave it for now.", null);
        return t;
    }

    // ------------------------------------------------------------------ helpers

    private void put(ItemDef d) { items.put(d.id, d); }
    private void put(SpellDef d) { spells.put(d.id, d); }
    private void put(MonsterDef d) { monsters.put(d.id, d); }
}
