# Alganon (single-player reimagining) — Design Document (Phase 1)

> **Status:** Phase 1 research/design spec. No code yet. This document is the authoritative
> build spec for Phase 2+.
>
> **Research-environment caveat:** Live web search/fetch is disabled in this build
> environment, so I could not pull fresh primary citations (wikis, forum posts, site
> archives, reviews) for this pass. Every claim below is therefore tagged by confidence:
>
> - **[Anchor]** — supplied by you as ground truth; treated as authoritative.
> - **[Likely]** — consistent with Alganon / late-2000s themepark-MMO design and general
>   recollection, but not freshly re-verified against a source in this session.
> - **[Gap — my design]** — no source; an original, playable decision I am filling in.
>   Labeled as such here and will be labeled in code comments where it drives rules.
>
> When web access is available, the **[Likely]** items are the ones to re-verify first.

---

## 1. Races & Factions

| Faction | Race (playable) | Theme | Confidence |
|---|---|---|---|
| Order / Protection | **Human (Asharr)** | Civilization, defense, law | [Anchor] |
| Chaos / Conquest | **Talrok (Kujix)** | Domination, expansion, war | [Anchor] |

- Two-faction structure with mirrored class rosters. **[Likely]**
- Each faction has its own starting continent & capital city (see §5). **[Likely]**
- **Single-player substitution:** faction identity affects starting zone, NPC dialog tone,
  faction-standing quests, and the background "faction war" flavor — there is no live
  opposing-faction PvP. **[Gap — my design]**

## 2. Classes

Six classes, faction-mirrored. **[Anchor]** for the class list and the specifics called out below.

| Class | Role | Resource | Signature mechanic | Confidence |
|---|---|---|---|---|
| **Champion** | Melee tank/DPS | Rage-equivalent ("Fury") | **Stances: Balance / Power / Defense** | [Anchor] stances; [Gap] resource name |
| **Reaver** | Melee DPS / dark warrior | Fury | Bleed/execute pressure | [Anchor] class; [Gap] kit |
| **Ranger** | Ranged physical | Energy ("Focus") | **Pets, traps, tracking** | [Anchor] pets/traps/tracking; [Gap] resource name |
| **Magus** | Elemental caster | Mana | **Schools: Flame / Frost / Storm** | [Anchor] |
| **Mystic** | Healer | Mana | **"Words and Touches of power"** | [Anchor] |
| **Cabalist** | Caster (dark/DoT/pet) | Mana | Curses / summons | [Anchor] class; [Gap] kit |

**Per-class design (kit is [Gap — my design] except where anchored):**

- **Champion** — pick a stance that modifies output/mitigation. Balance = neutral; Power =
  +damage, −armor; Defense = +armor/threat, −damage. Builds Fury by attacking, spends on strikes.
- **Reaver** — Fury user; applies bleeds, gains burst vs low-health targets (execute).
- **Ranger** — Focus user; summons a persistent pet (tank/DPS), drops ground traps (root/snare/damage),
  and has a tracking ability that reveals nearby mobs/quest targets on the minimap.
- **Magus** — Mana caster; Flame = DoT/burst, Frost = slow/control, Storm = burst/chain. School choice
  is a soft spec that weights available spells. **[Anchor]** for the three schools.
- **Mystic** — Mana healer; "Words" = cast-time heals/HoTs, "Touches" = instant/melee-range heals or
  smites. **[Anchor]** for the Words/Touches framing.
- **Cabalist** — Mana caster; curses (debuff DoTs) plus a summoned minion; life-drain flavor.

## 3. Families

- Each race has **5 families**, each tied to a playstyle archetype:
  **Achiever, Competitor, Explorer, Socializer, Crafter**. **[Anchor]**
- Families have their **own chat channel, merchants, and stat tracking**. **[Anchor]**
- **Single-player substitutions [Gap — my design]:**
  - *Family chat channel* → a scripted "Family" tab in the chat/log panel that surfaces
    NPC family-member messages, tips, and family-quest hooks.
  - *Family merchants* → a family vendor offering archetype-flavored goods & a small
    standing discount.
  - *Family stat tracking* → the family archetype grants a **passive bonus** and biases the
    dynamic quest generator (e.g., Explorer → more discovery/travel quests; Crafter →
    tradeskill bonuses; Achiever → XP/study bonus; Competitor → combat bonus; Socializer →
    vendor/reputation bonus).

## 4. Progression — active use + offline "Study"

- **Hybrid progression [Anchor]:** active use-based skill gain **plus** an offline **Study**
  system — assign skills to passively train while logged out; offline gains are **capped by a
  real-time metric** so idle progress can't outpace active play. **[Anchor]**
- **Model [Gap — my design]:**
  - Character has **Level/XP** (from quests & combat) and a set of **Skills** (per-ability or
    per-tradeskill lines) that advance via use.
  - **Study slot(s):** assign one (or a small number of) skills to "study." While the game is
    closed, each study skill accrues progress at a fixed rate per real hour.
  - **Cap:** offline accrual is capped at **N hours** of banked study (e.g., 8h) so leaving it
    closed for a week ≠ infinite progress. On load, compute `elapsedRealSeconds` since last
    save, clamp to the cap, and grant.
  - This is the single most important mechanic to get "feeling right," and the one most
    dependent on real elapsed wall-clock time in save/load (see Phase 2 persistence).

## 5. World Structure

- **[Anchor]:** multiple continents/capital cities; static + dynamic/instanced quests;
  non-lethal open-world dueling + faction PvP via **Towers and Keeps**.
- **[Likely]:** each faction has a home continent and a capital; contested middle/frontier
  zones host the Tower/Keep objectives.
- **Single-player build target [Gap — my design]:** ship **2–3 explorable zones** at first —
  a faction starting zone (safe, tutorializing), a wilderness/frontier zone (mobs, dynamic
  quests), and one **instanced dungeon**. Zones are selectable/loadable maps.
- **Faction PvP substitution [Gap — my design]:** Towers/Keeps become a **background simulated
  faction war** — a lightweight state machine that shifts control of contested objectives over
  time, surfaced through quest text, world-map markers, and NPC chatter. Open-world duel →
  **AI-controlled duel opponent** (non-lethal).

## 6. Grouping (multiplayer → single-player)

- **[Anchor]:** Group (6) → Legion/Battalion (36) → raid-tier (216), with tiered chat.
- **Single-player analog [Gap — my design]:** no real grouping. Replace with:
  - A small **NPC party** the player can recruit (companions), capped low (e.g., 1–2).
  - Legion/Battalion/raid tiers become **flavor/organizational** structures shown in the
    Family/Faction panels and referenced by quests, not live group mechanics.
  - Tiered group chat → scripted channels in the chat panel (Say / Family / Faction / System).

## 7. Core Systems

- **Combat [Anchor+Gap]:** target a mob, spend class resource on abilities with cooldowns,
  basic mob AI (aggro, attack, flee-at-low-hp optional). Real-time-with-cooldowns, tick-driven.
- **Quests [Anchor]:** static quest log **plus** a dynamic/procedural quest generator
  (kill-X / gather-Y / travel-Z templates, biased by family archetype).
- **Crafting/Tradeskills [Anchor]:** gather → process → craft chain, with an
  **auction/requisition house**-style vendor UI (single-player = NPC-populated listings you can
  buy from and post to). **[Gap]** for the exact recipe/economy tuning.
- **Library/Codex [Anchor]:** an in-game reference panel documenting races, classes, families,
  and systems — doubles nicely as our "scope honesty" surface (it can note single-player
  substitutions in-world).
- **UI [Anchor]:** action bar(s), minimap, quest tracker, chat/log, character panel, inventory,
  family panel; customizable panels. Character creation is a **guided wizard: race → family →
  class**, with an explanation at each step. **[Anchor]**

## 8. Explicit multiplayer → single-player substitutions (summary)

| Alganon (MMO) | Single-player replacement | Label |
|---|---|---|
| Opposing-faction live PvP | Faction flavor + AI duel opponent | [Gap] |
| Towers & Keeps faction war | Background simulated war state machine | [Gap] |
| Legion/Battalion/raid (36/216) | Flavor structures + tiny NPC companion party | [Gap] |
| Tiered group chat | Scripted chat channels (NPC-driven) | [Gap] |
| Family chat channel | Scripted "Family" chat tab | [Gap] |
| Auction house (player economy) | NPC-populated auction/requisition UI | [Gap] |

---

## 9. Biggest open design gaps (need your steer before Phase 2)

1. **Per-class ability kits.** Only Champion (stances), Ranger (pets/traps/tracking), Magus
   (Flame/Frost/Storm), and Mystic (Words/Touches) are anchored. Reaver and Cabalist kits, and
   all resource names ("Fury"/"Focus"), are **my design** — OK to proceed with the proposals in
   §2, or do you want specific kits?
2. **View & movement.** I recommend **top-down, panel/tile-based** movement (simplest robust
   Swing target, matches an MMO overhead feel). Confirm top-down vs side-view.
3. **Study cap value & granularity.** Proposing an **8-hour offline cap**, one study slot to
   start. Adjust the cap / number of slots?
4. **Scope of v1 world.** Proposing **3 zones (start / frontier / one dungeon)** for the first
   playable. More or fewer?
5. **Persistence format.** I'll recommend a concrete format in Phase 2 (leaning hand-rolled
   flat-file/JSON-ish, **zero external deps** per your constraint). Any preference toward Java
   serialization vs human-readable text?
