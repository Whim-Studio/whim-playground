# Task 1 — Model & Content: design notes

Owner packages: `com.whim.albion.world`, `.entities`, `.items`, `.magic`, `.data`.
Everything is exposed to the engine (Task 2) and UI (Task 3) purely through
`com.whim.albion.api` interfaces — no concrete class of ours leaks across the seam.
The single entry point is `data.AlbionModelFactory` (public no-arg constructor).

## Package map

| Package | Key classes | Role |
|---|---|---|
| `entities` | `Character`, `PartyCombatant`, `EnemyCombatant`, `PartyModelImpl`, `CharacterSpellView` | Party members, combat adapters, party model |
| `items` | `Inventory`, `ItemStack` | Backpack + 5 equip slots, equip/use, item views |
| `magic` | `SpellBook` | Known spells + profession school gate |
| `world` | `TileMap`, `Tile`, `WorldModelImpl`, `NpcImpl`, `TransitionImpl`, `InteractableImpl` | Grid maps + navigation |
| `data` | `AlbionModelFactory`, `AlbionGameModel`, `AlbionContent`, `MapFactory`, `DialogueTreeImpl`, `JournalModelImpl`, `Task1Smoke` | Content pack, model bundle, factory, smoke |

## Model design choices

- **Character ↔ Combatant split.** `Character` is the persistent sheet (stats,
  skills, level/xp, LP/SP, inventory, spellbook) and is its own `CharacterView`.
  Combat runs through a thin `PartyCombatant` adapter that delegates LP/SP straight
  back to the `Character`, so battle damage and spent SP persist afterwards; only the
  transient battlefield `GridPos` and the defend flag live on the adapter.
- **Effective stats.** `stat()` = base stat + equipped stat bonuses. `defense()` =
  summed equipment defense + STAMINA/4. `attackPower()` = equipped weapon attack, or
  a base 2 (fists) when unarmed. The combat engine layers STR/skill on top per its own
  formulas — the model only reports the base attack channel.
- **`ranged()`** is derived (RANGED skill > MELEE skill) rather than a weapon flag,
  since `ItemDef` carries no ranged marker.
- **Progression.** Cumulative XP curve `xpForLevel(L) = 100·(L-1)·L/2` (L1=0, L2=100,
  L3=300, L4=600…). `PartyModel.awardXp` distributes to every *living* member;
  level-up raises max LP/SP (scaled by STAMINA / MAGIC_TALENT), nudges primary stats
  and MELEE skill, and restores pools to full.
- **Magic gating.** `SpellBook` holds the profession's allowed `SpellSchool`s;
  `canCast(school)` is the profession gate. Per-spell readiness (SP + levelReq +
  talentReq) is `Character.canCastNow`, surfaced through `SpellView.castable()`.

## Content pack (invented, clean-room)

- **Party (4, all four schools):** Bran Ironhand (Warrior, no magic), Sela Quickbow
  (Ranger → NATURE), Odar the Kind (Healer → RESTORATION + PSIONIC), Ysolde Emberquill
  (Mage → DESTRUCTION + PSIONIC). Start with equipped gear, learned spells, shared
  potions and 60 gold.
- **Items (12):** short sword, hunter's bow, oaken staff, leather vest, round shield,
  iron cap, amulet (accessory), healing draught, azure (mana) tonic, scroll of sparks,
  rusty crypt key, relic shard (quest).
- **Spells (8, 4 schools):** Spark / Firebolt (DESTRUCTION), Thorn Lash / Regrowth
  (NATURE), Mend Wounds / Renewal (RESTORATION), Daze / Inner Focus (PSIONIC) —
  damage, heal, buff and debuff/utility all represented.
- **Monsters (3, one per behavior):** Cave Skitterling (AGGRESSIVE), Bog Slinger
  (RANGED), Fen Mystic (SUPPORT, carries Regrowth). Encounters `enc_dungeon1`
  (2 skitterlings + slinger) and `enc_dungeon2` (mystic + skitterling).
- **Maps:**
  - `map_duskhollow` — OUTDOOR_2D town: path cross, hedge border, three NPCs
    (Steward quest-giver, WEND the peddler/shopkeeper, a villager), a town chest, and
    a crypt-entrance `STAIRS` transition.
  - `map_crypt` — INDOOR_3D dungeon: wall-filled grid carved into corridors, an
    encounter cell, a west treasure chest, a **locked iron door**, and the relic vault.

## Quest wiring — "The Sunken Relic"

Wired through all three required hooks:
1. **Dialogue** (`dlg_elder`): accepting starts `quest_relic` via `GameContext`
   (`startQuest` + two objectives + `FLAG_QUEST_ON`) and grants the crypt key.
2. **Dungeon interactable**: the reliquary chest in the vault has the relic shard as
   loot (the engine grants loot on first use).
3. **Journal**: turn-in dialogue is available only when carrying the shard; it takes
   the shard, calls `completeQuest`, and pays 150 gold.

The **locked door** is modelled within the frozen API: the door tile is a
non-walkable `DOOR`, and its interactable opens `dlg_iron_door`. The "use the key"
option is `available(ctx)` only when `ctx.hasItem(key)`; choosing it consumes the key
and `teleport`s the party into the vault. This keeps the key requirement expressible
without any API change.

## Conventions the engine (Task 2) should know

- **Enemy reward lookup.** `EnemyCombatant.id()` is `"<monsterId>#<index>"`. To read
  xp/gold rewards after victory, strip the `#index` suffix and call
  `content.monster(baseId)`. (`Combatant` carries no reward fields, so this string
  convention is the recovery path.)
- **`spawnEncounter` positioning.** Spawned enemies get placeholder positions
  `(index, 0)`; the combat engine is expected to lay them out on the battlefield grid.
- **Loot delivery.** `Interactable.loot()` lists item ids; the engine adds them to the
  party and calls `consume()`. The model does not self-grant loot.
- **Teleport as unlock.** Dialogue options use `GameContext.teleport(mapId,x,y,facing)`
  to move the party (e.g. past the unlocked door); the engine's `teleport` should place
  the player via `WorldModel.loadMap`.

## Assumptions / notes

- **`seed`** is accepted but the start state is deterministic today (reserved for
  future procedural variation).
- **No API changes were needed.** The one area that required creativity within the
  frozen seam was the locked door (solved via the dialogue availability gate + teleport,
  above) and enemy rewards (solved via the id convention, above).
- **Testing.** `data.Task1Smoke` is a plain `main` (no JUnit) that asserts party,
  content counts, town load, encounter spawn, dialogue tree, and the full quest walk
  (28 checks). It is plain Java because the pom's `sourceDirectory=src` compiles the
  whole tree with `mvn compile`, so a JUnit file under `src/test` would (a) be pulled
  into the main compile and (b) fail offline where the JUnit artifact can't resolve.
  Run: `cd albion && mvn -q compile && java -cp target/classes com.whim.albion.data.Task1Smoke`.
