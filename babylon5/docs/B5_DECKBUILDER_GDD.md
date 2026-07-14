# Babylon 5: The Shadow War — Deck-Building Game
### Game Design Document (GDD)

> **Document type:** Tabletop game design specification (design + research only; no software code).
> **Status:** Complete first-pass design, ready for balance tuning and a later Java 8 / Swing implementation.
> **Source material:** *Babylon 5 Collectible Card Game* (Precedence Publishing, 1997). Clean-room adaptation that reuses existing B5 CCG cards, factions, and attribute stats. Authentic card data drawn from `babylon5/src/main/resources/cards/premiere-full.json` and `deluxe-full.json` (829 real cards).
> **Machine-readability note:** All resource, zone, phase, and card-type names are written as stable, unambiguous `UPPER_SNAKE_CASE`/canonical tokens so a later Java implementation can map each directly to a class or enum. See **Appendix A** for the glossary.

---

## 1. Overview

**Babylon 5: The Shadow War** is a 2–4 player competitive **deck-building game (DBG)** that translates the political CCG into a streamlined engine of recruiting ambassadors, building fleets, and resolving conflicts of **Diplomacy, Intrigue, Military,** and **Psi**.

**Elevator pitch:** You are one of the great powers on the eve of the Shadow War. Starting from a small hand of aides and credits, you recruit ambassadors and admirals from the shifting galactic scene (**THE_RIM**), fund your operations with a reliable home economy (**THE_CENTRAL_CORRIDOR**), win contests of diplomacy and force, and pursue your faction's secret **AGENDAS** — racing to **40 PRESTIGE** before the galactic order collapses.

| Property | Value |
|---|---|
| Players | 2–4 |
| Genre | Competitive deck-builder with a shared, seeded market |
| Framework | **Hybrid** — dynamic faction-seeded center-row over a small static supply |
| Currency (spend) | **INFLUENCE** |
| Victory metric | **PRESTIGE** (target 40, tunable) |
| Core factions | Narn Regime, Centauri Republic, Minbari Federation, Earth Alliance (plus Psi Corps / Non-aligned support) |
| Contest types | DIPLOMACY, INTRIGUE, MILITARY, PSI |

---

## 2. Design Goals & Pillars

1. **Authenticity over invention.** Every card, faction, and stat comes from the real B5 CCG catalogue. No generic sci-fi filler.
2. **Politics in flux.** The available cards *shift* turn to turn, reproducing B5's realpolitik — the ambassador you want may be recruited by a rival first.
3. **Four lanes of power.** DIPLOMACY, INTRIGUE, MILITARY, and PSI each open a distinct path to victory, so factions play differently and players build to cover their gaps.
4. **Reliable engine, volatile events.** A stable home economy guarantees no player bricks on a bad market, while the shared river keeps every turn tense.
5. **Clean, implementable rules.** Deterministic phase order, named zones, and integer resource pools — trivially portable to a Java 8 / Swing engine.

---

## 3. Chosen Framework & Justification

Two deck-building architectures were evaluated:

| Architecture | Model | Strength for B5 | Weakness for B5 |
|---|---|---|---|
| **Static market** | Dominion — fixed supply piles, full menu always visible | Stable political order; plannable engine; finite "seats"/warships model scarcity | Thematically *still* — cannot represent shifting alliances or fleeting opportunities |
| **Dynamic center-row / river** | Ascension, Star Realms — a shared face-up row refilled from a central deck | Perfect metaphor for galactic events & opportunistic diplomacy; take-it-or-lose-it ambassador prizes; Star-Realms faction-allegiance bonuses map onto B5's factions | Randomness can starve a plan; blurs faction identity unless the deck is seeded |

**Decision: a HYBRID framework.**

- A **dynamic, faction-seeded center-row — `THE_RIM` (6 slots)** — holds the flavorful, contested content: Characters/Ambassadors, Fleets/Ships, Conflicts, and Events. It is refilled from `THE_RIM_DECK`.
- A **small static supply — `THE_CENTRAL_CORRIDOR`** — provides always-available generic economy cards as a dependable backbone.

**Why hybrid wins:**
1. **Theme fidelity** — the river delivers shifting alliances and "take it or lose it" politics; the static supply guarantees a bad river never bricks a player, matching the CCG's mix of stable Influence generation and volatile political events.
2. **Faction identity** — `THE_RIM_DECK` is *seeded* so every card carries a faction; same-faction **Ally Bonuses** (Star Realms style) reward committing to a bloc — the "which side are you on?" pressure of the Shadow War.
3. **Planability without staleness** — the Central Corridor lets players build a dependable engine and pursue long-term Agendas, while the Rim keeps each turn's tactical menu fresh.
4. **Implementation clarity** — two market zones with well-defined refill rules are trivially machine-representable.

---

## 4. Components & Zones

### Per-player zones

| Zone | Definition |
|---|---|
| `DRAW_DECK` | Your face-down deck; you draw from its top. |
| `HAND` | Cards currently held (hand size 5). |
| `PLAY_AREA` | Cards played face-up this turn; cleared at cleanup except permanents. |
| `COMMAND_ROW` | Persistent permanents in play across turns: recruited Characters/Ambassadors and permanent Fleets/Groups/Locations. |
| `DISCARD_PILE` | Spent cards; reshuffled into `DRAW_DECK` when it empties. |
| `OUT_OF_GAME` (a.k.a. `THE_VOID`) | Trashed/purged cards, permanently removed (deck thinning). |
| `INFLUENCE_POOL` | Per-turn counter of spendable currency; resets to 0 each turn. |
| `PRESTIGE_TRACK` | Accumulated victory points; never resets, never spent. |

### Shared / central zones

| Zone | Definition |
|---|---|
| `THE_RIM` | Dynamic center-row: 6 face-up slots (Characters, Fleets, Conflicts, Events). |
| `THE_RIM_DECK` | Shared face-down central deck that refills `THE_RIM`. |
| `THE_CENTRAL_CORRIDOR` | Static supply of always-available fixed piles. |
| `CONFLICT_ZONE` | Where an active Conflict card is contested. |

---

## 5. Resources & Economy

**Core principle:** the currency you *spend* is separate from the score that *wins*.

| Resource | Kind | Persistence | Role |
|---|---|---|---|
| **INFLUENCE** | Spend currency | Per-turn pool, resets at `START_PHASE` | Acquire cards from the markets; optionally advance Agendas |
| **PRESTIGE** | Victory points | Accumulated, never spent | The win metric on `PRESTIGE_TRACK` |
| **DIPLOMACY** | Attribute (per-turn pool) | Reset each turn | Converts 1:1 to INFLUENCE; contest stat for treaty/alliance conflicts |
| **INTRIGUE** | Attribute (per-turn pool) | Reset each turn | Sabotage, forced discards, steal/deny Rim cards; espionage contests |
| **MILITARY** | Attribute (per-turn pool) | Reset each turn | Powers Fleets, combat contests, remove permanents |
| **PSI** | Attribute (per-turn pool) | Reset each turn | Negate/halve an opponent's committed attribute; gates Psi-only cards; advances Psi-Corps Agendas |

**Key economy rule:** **DIPLOMACY is the only attribute that converts to currency** (1:1 into INFLUENCE during `ACTION_PHASE`), preserving the CCG feel that soft power buys influence. INTRIGUE, MILITARY, and PSI are spent only on their gated effects and committed to conflicts. All attribute pools (and unspent INFLUENCE) are lost at `CLEANUP_PHASE`.

### Resource-flow summary (implementer reference)

| Trigger | Effect |
|---|---|
| Play `Credit Chit` | +1 INFLUENCE |
| Play a Character / Fleet | Add its D/I/M/P to matching pools (+ Ally Bonus if a same-faction pair is in play) |
| Convert DIPLOMACY | 1:1 → INFLUENCE (ACTION_PHASE only) |
| Spend INFLUENCE | Acquire from `THE_RIM` / `THE_CENTRAL_CORRIDOR`; advance Agendas |
| Spend INTRIGUE / MILITARY / PSI | Gated card effects; commit to conflicts |
| Win a Conflict / complete an Agenda | + PRESTIGE |
| `PRESTIGE_TRACK` reaches target | Game ends |

---

## 6. Turn Structure

Each turn is a **strict five-phase sequence**.

| # | Phase | Actions |
|---|---|---|
| 1 | `START_PHASE` (Upkeep) | Reset `INFLUENCE_POOL` to 0. Resolve start-of-turn `COMMAND_ROW` triggers and Agenda progress checks. |
| 2 | `STRATEGY_PHASE` (Draw) | Draw up to hand size **5** from `DRAW_DECK` (reshuffle `DISCARD_PILE` when empty). |
| 3 | `ACTION_PHASE` (Play / Resolve) | Play cards from `HAND` to `PLAY_AREA` in any order; add INFLUENCE/attributes to pools; resolve Character abilities, Events, and Conflicts in `CONFLICT_ZONE`; advance Agendas. |
| 4 | `ACQUISITION_PHASE` (Purchase / Recruit) | Spend `INFLUENCE_POOL` on `THE_RIM` and/or `THE_CENTRAL_CORRIDOR` (no cap except available Influence, unless a card says otherwise); refill the Rim after each acquisition; may also spend Influence to advance Agendas. |
| 5 | `CLEANUP_PHASE` (Discard) | Move `PLAY_AREA` (except `COMMAND_ROW`) and remaining `HAND` to `DISCARD_PILE`; unspent INFLUENCE and attribute pools are lost; resolve end-of-turn triggers; pass to next player. |

**Starting setup (per player):**
1. **Choose a faction.** Place that faction's cost-0 **Ambassador Hero(es)** directly in `COMMAND_ROW` (in play from turn 1).
2. **Generic starter deck (10 cards), shuffled as your `DRAW_DECK`:**
   - 7× **Credit Chit** — basic economy; play for **+1 INFLUENCE**.
   - 2× **Minor Diplomat** — Diplomacy 1; play for **+1 INFLUENCE or +1 DIPLOMACY** (choose on play).
   - 1× **Junior Officer** — Military 1; play for **+1 INFLUENCE or +1 MILITARY** (choose on play).
3. Draw an opening hand of 5.

> **Reconciliation note:** This merges the two starting-deck models — every player gets both their faction's free Ambassador Hero(es) in `COMMAND_ROW` **and** the identical generic 10-card economy starter as their `DRAW_DECK`.

---

## 7. Card Acquisition & Markets

### `THE_RIM` — dynamic center-row (6 slots)
- To acquire a Rim card, pay its INFLUENCE cost from `INFLUENCE_POOL`.
- The card goes to your `DISCARD_PILE` by default (or directly to `COMMAND_ROW` if it is a permanent that enters play on acquire).
- **Immediately refill** the emptied slot from the top of `THE_RIM_DECK`.
- **Optional `SCUTTLE` rule:** once per turn, banish one Rim card to the bottom of `THE_RIM_DECK` and refill (a diplomatic snub / missed opportunity).

### `THE_CENTRAL_CORRIDOR` — static supply (fixed piles)
Always available at a fixed INFLUENCE cost; acquired card goes to `DISCARD_PILE`. Suggested piles:

| Pile | Cost | Effect |
|---|---|---|
| Credit Chit | 1 | +1 INFLUENCE (buy more economy) |
| Generic Recruit | 2 | Vanilla attribute Character |
| Patrol Ship | 1 | +2 MILITARY filler Fleet |
| Diplomatic Purge | 2 | One-shot: trash a card from hand/discard to `OUT_OF_GAME` (deck thinning) |

---

## 8. Conflict Resolution

Conflict cards enter from `THE_RIM` (or are triggered by Events) and are resolved in the `CONFLICT_ZONE` during `ACTION_PHASE`. Each names a **CONTEST ATTRIBUTE** (Diplomacy / Intrigue / Military / Psi), a **DIFFICULTY**, a **reward**, and a **penalty**.

- **Vs the board (solo / Agenda conflicts):** total your generated value in the contest attribute this turn. If **total ≥ DIFFICULTY**, you win (reward = PRESTIGE and/or INFLUENCE, sometimes a permanent); otherwise it is lost/expires (optional penalty).
- **Vs an opponent (interactive conflicts):** both contestants commit points of the contest attribute; **higher total wins** (reward = PRESTIGE / seize a Rim card / remove an opponent permanent; loser may suffer the penalty).
- **PSI override:** PSI may be spent to **negate or halve** the opponent's committed total *before* comparison (a telepath reads the room).

**Faction Ally Bonus (Star Realms style):** cards are tagged Narn Regime / Centauri Republic / Minbari Federation / Earth Alliance / Psi Corps / Non-aligned. If you have **two or more cards of the same faction in play** this turn, each grants its **Ally Bonus** (extra attribute or Influence), making alliance-building mechanically real.

---

## 9. Faction Identity

| Faction | Archetype | Signature effects | Synergy | Weakness |
|---|---|---|---|---|
| **Centauri Republic** | Intrigue & political scheming (control/disruption) | Forced discard/trash on winning Intrigue; *Growth in Chaos* turns every player's lost Intrigue conflict into your PRESTIGE | Flood cheap Centauri characters, strip opponents' hands, grind PRESTIGE from chaos | Low Psi outside telepaths, mid Military — weak to a fast Military rush |
| **Narn Regime** | Military aggression & vengeance (aggro/tempo) | Heavy Fleet cancels Military buffs; Strike Fleet flexes to Intrigue; *Revenge* spikes Military; *Never Again* pays PRESTIGE for beating Centauri | Fleets + Military every turn for PRESTIGE and to seize Locations, with an Intrigue splash to disrupt | No Psi, thin card draw — can run out of gas late |
| **Minbari Federation** | Psi, defense & tempo (midrange/value) | Uncounterable military (Grey Council Fleet); *Religious Caste* rewards an undamaged board; Warrior Caste pumps Fleets; Delenn → Delenn Transformed level-up | Build an undamaged, uncounterable board and win on tempo | Higher average costs — slow to come online; punished by early aggression |
| **Earth Alliance (Human)** | Balanced, flexible & cooperative (toolbox/support) | Sheridan splits power across conflicts; Garibaldi scry; flexible cheap engine (Psi Corps Intelligence); *Alliance of Races* rewards supporting/initiating multiplayer conflicts | Answer the table; act as kingmaker/supporter to farm PRESTIGE | Master-of-none raw stats — loses head-to-head stat races |

**Cross-faction structure:** four conflict lanes (D/I/M/P), each faction strong in 1–2 and weak in others, drives an acquire-to-cover-your-gaps deck-building loop. The cost curve stays tight (0–4): Ambassadors are free starters; premium engines (Draal, Bester, elite Fleets) sit at 3–4.

> **Scope note:** Kosh / Vorlon content is **not present** in the authentic card JSON and is therefore **out of scope**, pending source data. No stats were invented for it.

---

## 10. The Card Dictionary

All names, factions, and original stats (Diplomacy / Intrigue / Military / Psi and Influence cost) are taken verbatim from the real B5 CCG card data. `DBG Cost` is the acquisition cost in INFLUENCE.

### 10.1 Ambassadors — cost-0 Starting Heroes (begin in `COMMAND_ROW`)

| Original Card | Faction | Original Stats (D/I/M/P, cost) | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Londo Mollari | Centauri | D5 I6 M4, cost 0 | 0 (Hero) | Character | Commit for 6 Intrigue / 5 Diplomacy / 4 Military; once per turn exhaust to +1 INFLUENCE when you resolve an Intrigue conflict. Cannot be trashed by opponents. |
| G'Kar | Narn | D4 I5 M3, cost 0 | 0 (Hero) | Character | Commit for 5 Intrigue / 4 Diplomacy / 3 Military; on winning any conflict, gain +1 PRESTIGE. |
| Delenn | Minbari | D7 I3 M5 P2, cost 0 | 0 (Hero) | Character | Commit for 7 Diplomacy / 5 Military / 3 Intrigue / 2 Psi; strongest opening diplomat. Cannot be trashed. |
| Delenn Transformed | Minbari | D8 I3 M6 P5, cost 0 | 0 (Upgrade Hero) | Character | Replaces Delenn once a set condition is met: 8 Diplomacy / 6 Military / 5 Psi / 3 Intrigue. An in-play level-up. |
| Jeffrey Sinclair | Earth (Human) | D5 I3 M4, cost 0 | 0 (Hero) | Character | Balanced 5 Diplomacy / 4 Military / 3 Intrigue; may not be removed by card effects (defensive anchor). |

### 10.2 Faction Characters (acquirable from `THE_RIM`)

| Original Card | Faction | Original Stats (D/I/M/P, cost) | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Lord Refa | Centauri | D2 I5 M3, cost 2 | 2 | Character | +5 Intrigue; on winning an Intrigue conflict, force target opponent to discard 1. |
| Vir Cotto | Centauri | D3 I1 M2, cost 2 | 2 | Character | +3 Diplomacy / +2 Military; on acquire or play, +1 INFLUENCE. |
| Emperor Turhan | Centauri | D6 I3 M4, cost 3 | 3 | Character | +6 Diplomacy / +4 Military; while in play, +1 PRESTIGE per turn if you initiated no Military conflict. |
| Na'Toth | Narn | D2 I5 M2, cost 2 | 2 | Character | +5 Intrigue; exhaust to draw 1 when you resolve an Intrigue conflict. |
| Ta'Lon | Narn | D2 I4 M3, cost 2 | 2 | Character | +4 Intrigue / +3 Military; may commit to Intrigue OR Military. |
| G'Sten | Narn | D2 I3 M6, cost 3 | 3 | Character | +6 Military heavy hitter. |
| Neroon | Minbari | D2 I3 M5, cost 2 | 2 | Character | +5 Military at cost 2 — efficient Warrior-caste aggressor. |
| Lennier | Minbari | D3 I3 M2 P1, cost 2 | 2 | Character | 3/3/2 +1 Psi; exhaust to +1 to a defensive conflict. |
| Draal | Minbari | D5 I2 M3 P4, cost 4 | 4 | Character | +5 Diplomacy / +4 Psi; on play draw 1, then may trash 1 from hand. |
| Warleader Shakiri | Minbari | D2 I2 M7, cost 3 | 3 | Character | +7 Military — top raw attack stat in the game. |
| John Sheridan | Earth (Human) | D4 I3 M6, cost 3 | 3 | Character | +6 Military / +4 Diplomacy; may split power across a Military and a Diplomacy conflict the same turn. |
| Susan Ivanova | Earth (Human) | D3 I3 M5, cost 3 | 3 | Character | +5 Military; exhaust to add committed Fleets' power to a Military conflict. |
| Michael Garibaldi | Earth (Human) | D2 I5 M3, cost 2 | 2 | Character | +5 Intrigue; exhaust to scry top 2 of your deck, keep order. |
| Bester | Earth (Psi Corps) | D2 I4 M3 P5, cost 4 | 4 | Character | +5 Psi / +4 Intrigue; on winning a Psi conflict, opponent discards 1. |
| Talia Winters | Earth (Psi Corps) | D2 I3 M2 P4, cost 3 | 3 | Character | +4 Psi; enables Psi conflicts for the psi-light Human deck. |

### 10.3 Fleets / Ships (SUPPORT permanents → `COMMAND_ROW`)

| Original Card | Faction | Original Stats (cost) | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Third Battle Fleet | Centauri | M4, cost 3 | 3 | Permanent (Fleet) | +4 Military; its Military cannot be reduced by opponent effects. |
| Fleet of the Line | Earth (Human) | M4, cost 3 | 3 | Permanent (Fleet) | Exhaust: +4 Military to the current conflict; one conflict per turn. |
| Grey Council Fleet | Minbari | M4, cost 3 | 3 | Permanent (Fleet) | +4 Military that cannot be reduced by card effects. |
| Heavy Fleet | Narn | M4, cost 3 | 3 | Permanent (Fleet) | Exhaust: cancel one Military Enhancement an opponent played this conflict. |
| Strike Fleet | Narn | M3, cost 2 | 2 | Permanent (Fleet) | +3 Military; may also commit to Intrigue conflicts. |
| Basic Battle Fleet (e.g. First Battle Fleet) | All four | M2, cost 1 | 1 | Permanent (Fleet) | +2 Military filler; each faction has a full suite (Colonial / Deep Space / Homeworld / Picket, etc.). |

### 10.4 Groups (SUPPORT permanents → `COMMAND_ROW`)

| Original Card | Faction | Role (cost) | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Thenta Makur | Narn | Intrigue engine, cost 3 | 3 | Permanent (Group) | Exhaust in an Intrigue conflict: +4 Intrigue. |
| Imperial Telepaths | Centauri | Psi engine, cost 3 | 3 | Permanent (Group) | Exhaust in a Psi conflict: +3 Psi; if you win, target opponent discards 1. |
| Wind Swords | Minbari | Military engine, cost 2 | 2 | Permanent (Group) | Exhaust in a Military conflict: +3 Military. |
| Warrior Caste | Minbari | Passive, cost 3 | 3 | Permanent (Group) | Static: all your Fleets get +1 Military. |
| Religious Caste | Minbari | Passive, cost 2 | 2 | Permanent (Group) | Start of turn, if you have no damaged characters, +1 PRESTIGE. |
| Psi Corps Intelligence | Earth (Human) | Flex, cost 1 | 1 | Permanent (Group) | Exhaust in any Intrigue OR Psi conflict: +2. |

### 10.5 Locations (permanents → `COMMAND_ROW`)

| Original Card | Faction | Role (cost) | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Homeworld (Centauri Prime / Narn Homeworld / Minbar / Earth) | Each faction | +2 INFLUENCE/turn, cost 1 | 1 | Permanent (Location) | +2 INFLUENCE per turn; cannot be seized by Military. |
| Mars Colony | Earth (Human) | +1 INFLUENCE/turn, cost 1 | 1 | Permanent (Location) | +1 INFLUENCE per turn; your Intrigue conflicts get +1. |
| Sleeping Z'ha'dum | Non-aligned | +2 INFLUENCE/turn, cost 3 | 3 | Permanent (Location) | Any faction may acquire; +2 INFLUENCE/turn AND all Intrigue conflicts get +1. |
| Transfer Point Io | Non-aligned | +1 INFLUENCE/turn, cost 2 | 2 | Permanent (Location) | Any faction; +1 INFLUENCE per turn. |

### 10.6 Agendas (objective / VP-engine cards)

| Original Card | Faction | Cost | DBG Cost | DBG Type | DBG Effect |
|---|---|---|---|---|---|
| Growth in Chaos | Centauri | 1 | 1 | Objective | +1 PRESTIGE whenever ANY player loses an Intrigue conflict. |
| Revenge | Narn | 1 | 1 | Objective | Once per turn, spend 1 INFLUENCE to add +2 to a Military conflict vs a chosen opponent. |
| Never Again | Narn | 1 | 1 | Objective | +2 PRESTIGE each time you win a Military conflict against the Centauri player. |
| Finish the War | Minbari | 1 | 1 | Objective | Your Military conflicts vs Human/Earth players get +3. |
| Alliance of Races | Earth (Human) | 1 | 1 | Objective | When you SUPPORT another player's winning Diplomacy conflict, +2 PRESTIGE. |

### 10.7 Conflict / Event cards (one-shots) — reference examples

| Original Card | Contest | DBG Effect |
|---|---|---|
| A Brighter Future | Diplomacy | Winner +3 PRESTIGE; supporters +1 PRESTIGE. |
| Affirmation of Peace | Diplomacy | Winner +2 PRESTIGE; loser −1 PRESTIGE. |
| Raid Shipping | Military | Winner +2 PRESTIGE and may steal 1 PRESTIGE from the loser. |
| Develop Relationship (Aftermath) | — | Both participants draw 1 card. |

---

## 11. Agendas & Long-Term Objectives

- **AGENDAS** are cost-1 objective cards acquired like any other card and placed in `COMMAND_ROW`.
- They are the primary engine converting board state into **PRESTIGE** (see §10.6).
- During `ACTION_PHASE` / `ACQUISITION_PHASE`, a player may **spend INFLUENCE and/or attribute points to add PROGRESS markers** toward an Agenda's completion; on completion the Agenda awards its PRESTIGE.
- Agendas are deliberately faction-flavored (Centauri profit from chaos, Narn from vengeance, Minbari from the Earth-Minbari war, Earth from coalition support), reinforcing each faction's archetype.

---

## 12. Win Conditions & End-Game

The game ends the moment **any** of these triggers first:

1. A player's `PRESTIGE_TRACK` reaches the **PRESTIGE target (recommended 40, tunable)**, **or**
2. A set number of **Agendas** have been completed (tunable), **or**
3. `THE_RIM_DECK` is exhausted (the galactic order collapses).

**Scoring & tiebreakers:**
1. Highest **PRESTIGE** wins.
2. Tie → most completed **Agendas**.
3. Still tied → most permanents in `COMMAND_ROW`.

---

## Appendix A — Machine-Readable Glossary (for the Java 8 / Swing implementation)

> Plain enumeration of canonical tokens. **Not code** — a mapping reference so an implementer can bind each token to a class, enum constant, or field.

### A.1 Resources → `enum Resource`
`INFLUENCE` (spend currency, per-turn), `PRESTIGE` (victory points, persistent), `DIPLOMACY`, `INTRIGUE`, `MILITARY`, `PSI` (attributes, per-turn pools).

### A.2 Zones → `enum Zone`
Per-player: `DRAW_DECK`, `HAND`, `PLAY_AREA`, `COMMAND_ROW`, `DISCARD_PILE`, `OUT_OF_GAME`, `INFLUENCE_POOL`, `PRESTIGE_TRACK`.
Shared: `THE_RIM`, `THE_RIM_DECK`, `THE_CENTRAL_CORRIDOR`, `CONFLICT_ZONE`.

### A.3 Turn phases → `enum Phase` (ordered)
`START_PHASE` → `STRATEGY_PHASE` → `ACTION_PHASE` → `ACQUISITION_PHASE` → `CLEANUP_PHASE`.

### A.4 Card types → `enum CardType`
`CHARACTER`, `AMBASSADOR_HERO`, `FLEET`, `GROUP`, `LOCATION`, `AGENDA`, `CONFLICT`, `EVENT`, `ECONOMY` (Credit Chit and other basic currency cards).

### A.5 Factions → `enum Faction`
`NARN_REGIME`, `CENTAURI_REPUBLIC`, `MINBARI_FEDERATION`, `EARTH_ALLIANCE`, `PSI_CORPS`, `NON_ALIGNED`.

### A.6 Contest types → `enum ContestType`
`DIPLOMACY`, `INTRIGUE`, `MILITARY`, `PSI`.

### A.7 Card record — suggested fields
`id`, `name`, `faction: Faction`, `cardType: CardType`, `acquisitionCost: int` (INFLUENCE), `attributes: {DIPLOMACY, INTRIGUE, MILITARY, PSI}: int`, `isPermanent: boolean`, `entersPlayOnAcquire: boolean`, `contestType: ContestType?` (conflicts), `difficulty: int?` (conflicts), `effects: List<Effect>`, `allyBonus: Effect?`, `prestigeValue: int?`.

### A.8 Effect primitives → `enum EffectPrimitive`
`GAIN_INFLUENCE`, `GAIN_PRESTIGE`, `GAIN_ATTRIBUTE(type, n)`, `DRAW(n)`, `TRASH(n)`, `DISCARD_OPPONENT(n)`, `ACQUIRE_CARD`, `SEIZE_RIM_CARD`, `REMOVE_PERMANENT`, `EXHAUST`, `NEGATE_ATTRIBUTE(type)`, `CONVERT_DIPLOMACY_TO_INFLUENCE`, `ADD_AGENDA_PROGRESS(n)`, `START_OF_TURN`, `END_OF_TURN`, `ON_WIN_CONFLICT(type)`.

### A.9 Tunable constants
`STARTING_HAND_SIZE = 5`; `STARTER_DECK = {CREDIT_CHIT×7, MINOR_DIPLOMAT×2, JUNIOR_OFFICER×1}`; `RIM_SLOTS = 6`; `PRESTIGE_TARGET = 40`; `SCUTTLE_PER_TURN = 1`; `ACQUISITION_COST_RANGE = 0..4`.

---

*End of Game Design Document. This document is design/research only; it defines rules and a card dictionary for a future Java 8 / Swing implementation and contains no source code.*
