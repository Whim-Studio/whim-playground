# DESIGN — Warlock of Firetop Mountain (Java 8 / Swing)

Package root: `com.whim.firetop`. Package-per-layer.

```
com.whim.firetop
├── model         immutable-ish data: game entities, no game logic
├── engine        rules logic: dice, combat, luck, decks, turn state machine
├── persistence   save/load (Java serialization)
└── ui            Swing views: frame, board panel, dialogs, status
```

## Data model (`model`)

- **Attribute-bearing state** lives directly on entities as `initial`/`current`
  int pairs (no separate `Attributes` class needed for Java 8 simplicity).
- `Character` (adventurer) — `name`, skill/stamina/luck (initial+current),
  `gold`, `provisions`, `inventory : List<Item>`, `roomId`, `alive`. `Serializable`.
- `Monster` — `name`, `skill`, `stamina` (current, mutated in combat),
  `description`. `Serializable`.
- `Item` — `name`, `ItemType`, `description`, `magnitude` (e.g. potion heal amount).
- `ItemType` — WEAPON, POTION, TREASURE, KEY, PROVISION.
- `Card` — `name`, `CardType`, `description`, `CardEffect effect`, `magnitude`.
- `CardType` — ENCOUNTER, TREASURE, EVENT.
- `CardEffect` — enum of resolvable effects (GAIN_GOLD, LOSE_STAMINA,
  TEST_LUCK_TRAP, GAIN_POTION, GAIN_PROVISION, RESTORE_STAMINA, GAIN_SKILL, ...).
- `Room` — `id`, `name`, `RoomType`, grid `x`,`y`, `List<Integer> exits`,
  `Monster monster` (nullable), `int gold`, `boolean visited`, `description`.
- `RoomType` — ENTRANCE, EMPTY, MONSTER, TREASURE, TRAP, EVENT, SPECIAL, LAIR.
- `Board` — `Map<Integer,Room>`, `entranceId`, `lairId`; adjacency via room exits;
  helpers `neighbors`, `roomsWithin(start, steps)`.
- `Cards` (factory) — builds the three decks + the canonical dungeon board +
  Zagor.

## Engine (`engine`)

- `Dice` — wraps `java.util.Random`. Two constructors: `new Dice()` (real random)
  and `new Dice(long seed)` (seedable/deterministic for tests + reproducible games).
  `roll(n,sides)`, `d6()`, `roll2d6()`, `rollSkill()`, `rollStamina()`,
  `rollLuck()`.
- `LuckTest` — `TestResult test(Character c, Dice d)`: rolls 2d6, compares to
  current luck, decrements luck by 1, returns lucky flag + roll.
- `Combat` — pure round resolution:
  - `RoundResult resolveRound(Character c, Monster m, Dice d)` computes both
    Attack Strengths, determines winner, applies base 2 damage.
  - `applyLuckToAttack(RoundResult, Character, Dice)` — press-attack luck (+2 / -1).
  - `applyLuckToDefense(RoundResult, Character, Dice)` — soften-blow luck (+1 / -1).
  - Damage helpers are also exposed as pure static functions so unit tests can
    assert exact numbers without RNG.
- `Deck` — `Deck(List<Card>, Dice)`; `draw()` reshuffles discards when empty;
  `discard(card)`.
- `GameEngine` — the turn state machine. Holds `GameState`. Phases:
  `AWAIT_MOVE → RESOLVE_ROOM → (combat/card) → END_TURN → next player`. Exposes
  intent methods the UI calls: `reachableRooms()`, `moveTo(roomId)`,
  `currentCharacter()`, `startCombat()`, combat step methods, `eatProvision()`,
  `useItem()`, `endTurn()`, win/lose checks. Emits log lines via a listener.

## Persistence (`persistence`)

- `SaveGame` — `save(GameState, File)` / `GameState load(File)` using
  `ObjectOutputStream`/`ObjectInputStream`. All model + `GameState` implement
  `Serializable` with `serialVersionUID`.

## UI (`ui`)

- `Main` — entry point; sets system L&F; shows `GameFrame` on the EDT.
- `GameFrame` — `JFrame` + menu bar (New / Save / Load / How to Play / Exit).
  Hosts a `CardLayout`: setup screen, play screen, end screen.
- `SetupPanel` — choose number of players (1–4) and names; roll attributes.
- `BoardPanel` — custom `paintComponent` drawing the room graph, corridors,
  tokens, highlighting reachable rooms; mouse + keyboard movement.
- `StatusPanel` — per-player SKILL/STAMINA/LUCK meters, gold, provisions,
  inventory list, "Eat Provision" / "Use Item" buttons; current-player highlight.
- `CombatDialog` — modal; shows both Attack Strengths, exchange log, damage, and a
  "Test Luck" button enabled only at valid moments.
- `CardDialog` — shows a drawn card and its resolved effect.
- `HowToPlayDialog` — rules in original wording + the fan-made disclaimer.
- `EndScreen` — win/lose summary.

## Turn/state machine

```
SETUP ──newGame──► PLAY
PLAY loop per active character:
   AWAIT_MOVE ──moveTo(room)──► RESOLVE_ROOM
   RESOLVE_ROOM: dispatch on RoomType
        MONSTER/LAIR ──► CombatDialog ──win──► (LAIR ⇒ VICTORY) / (else continue)
                                      └─lose─► character dies ⇒ maybe party wipe
        TREASURE/EVENT ──► draw deck ──► CardDialog resolves effect
        TRAP/SPECIAL   ──► immediate effect (may test luck)
        EMPTY/ENTRANCE ──► nothing
   ──► END_TURN ──► advance to next living character ──► AWAIT_MOVE
Terminal: VICTORY (Zagor slain) or DEFEAT (all dead).
```

Testing strategy: all rules logic (Dice, Combat, LuckTest) is pure and
seed-driven, covered by JUnit before any UI is written (Phase 1). The UI only
orchestrates engine calls.
