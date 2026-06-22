# Babylon 5 CCG — Research Dossier (Task 1: domain / data / cards)

Single-player desktop adaptation of the **Babylon 5 Collectible Card Game**
(Premiere / Base Set + Deluxe Edition). This dossier documents the starter card
set shipped under `src/main/resources/cards/`, the JSON schema those files use,
and the rules ambiguities resolved while modelling the domain.

**Rule authority:** `babylon5/docs/rulebook-source.txt` — the user's uploaded
*Babylon 5 CCG — Psi Corps v1.3b* rulebook (includes Severed Dreams and Wheel of
Fire updates, March 10 2000). Section names below refer to that document.

**Legal note on art:** No copyrighted card art or full card text is embedded in
this repository. JSON records carry *minimal* flavor text only and a public
`imageUrl`. `ImageLoader` fetches art lazily from those URLs into the user's own
cache (`~/.b5ccg/cache`) and shows a generated placeholder until/unless a fetch
succeeds. Image URLs use the community archive host `vintageccg.com` (the source
named in the rulebook header) on a best-effort basis; the app never depends on a
URL resolving.

---

## 1. Card list — Premiere + Deluxe starter set

Abilities are listed as **Dip / Int / Psi / Mil** (Diplomacy, Intrigue, Psi,
Military). Per the rulebook ("Anatomy of a Card", note 4) characters use
Diplomacy/Intrigue/Psi/Leadership; **fleets and locations** use Military.
Characters therefore have Military 0; fleets/locations carry the Military rating.
`cost` is the printed Influence Cost. Ambassadors are *Starting Ambassadors*
(cost 0, cannot be sponsored — rulebook "Your Starting Ambassador").

### Premiere — Ambassadors (`premiere-ambassadors.json`)

| id | Name | Faction | Cost | Dip | Int | Psi | Mil |
|----|------|---------|------|-----|-----|-----|-----|
| prem-amb-human-sinclair | Jeffrey Sinclair | HUMAN | 0 | 3 | 2 | 0 | 0 |
| prem-amb-minbari-delenn | Delenn | MINBARI | 0 | 3 | 1 | 2 | 0 |
| prem-amb-narn-gkar | G'Kar | NARN | 0 | 2 | 3 | 0 | 0 |
| prem-amb-centauri-londo | Londo Mollari | CENTAURI | 0 | 2 | 3 | 0 | 0 |

### Premiere — Characters (`premiere-characters.json`)

| id | Name | Faction | Cost | Dip | Int | Psi | Mil |
|----|------|---------|------|-----|-----|-----|-----|
| prem-chr-human-garibaldi | Michael Garibaldi | HUMAN | 2 | 1 | 3 | 0 | 0 |
| prem-chr-human-ivanova | Susan Ivanova | HUMAN | 3 | 2 | 2 | 0 | 0 |
| prem-chr-minbari-lennier | Lennier | MINBARI | 2 | 2 | 1 | 1 | 0 |
| prem-chr-minbari-neroon | Neroon | MINBARI | 3 | 1 | 2 | 1 | 0 |
| prem-chr-narn-natoth | Na'Toth | NARN | 2 | 1 | 3 | 0 | 0 |
| prem-chr-narn-narn-bureaucrat | Narn Bureaucrat | NARN | 1 | 2 | 1 | 0 | 0 |
| prem-chr-centauri-vir | Vir Cotto | CENTAURI | 1 | 2 | 1 | 0 | 0 |
| prem-chr-centauri-refa | Lord Refa | CENTAURI | 3 | 1 | 3 | 0 | 0 |
| prem-chr-neutral-talia | Talia Winters | NONALIGNED | 3 | 1 | 1 | 3 | 0 |
| prem-chr-neutral-franklin | Stephen Franklin | NONALIGNED | 2 | 2 | 1 | 0 | 0 |

### Premiere — Conflicts (`premiere-conflicts.json`)

| id | Name | Resolved by | Cost |
|----|------|-------------|------|
| prem-cnf-diplomacy-summit | Diplomatic Summit | Diplomacy | 0 |
| prem-cnf-intrigue-blackmail | Blackmail | Intrigue | 0 |
| prem-cnf-psi-mindprobe | Mind Probe | Psi | 0 |
| prem-cnf-military-skirmish | Border Skirmish | Military | 0 |
| prem-cnf-military-limited-strike | Limited Strike | Military | 0 |
| prem-cnf-diplomacy-test-mettle | Test Their Mettle | Diplomacy | 0 |

### Premiere — Aftermath (`premiere-aftermath.json`)

| id | Name | Play condition |
|----|------|----------------|
| prem-aft-war-hero | War Hero | Won Military |
| prem-aft-disgrace | Disgrace | Lost (any) |
| prem-aft-diplomatic-coup | Diplomatic Coup | Won Diplomacy |

### Deluxe Edition (`deluxe-cards.json`)

| id | Name | Type | Faction | Cost | Dip | Int | Psi | Mil |
|----|------|------|---------|------|-----|-----|-----|-----|
| dlx-chr-human-sheridan | John Sheridan | CHARACTER | HUMAN | 4 | 3 | 2 | 0 | 0 |
| dlx-chr-neutral-marcus | Marcus Cole | CHARACTER | NONALIGNED | 3 | 2 | 2 | 0 | 0 |
| dlx-chr-neutral-lyta | Lyta Alexander | CHARACTER | NONALIGNED | 3 | 1 | 1 | 4 | 0 |
| dlx-amb-vorlon-kosh | Kosh Naranek | AMBASSADOR | VORLON | 0 | 2 | 2 | 4 | 0 |
| dlx-amb-psicorps-bester | Alfred Bester | AMBASSADOR | PSI_CORPS | 0 | 1 | 3 | 4 | 0 |
| dlx-flt-human-fleet | Earth Alliance Fleet | SUPPORT (fleet) | HUMAN | 3 | 0 | 0 | 0 | 4 |
| dlx-flt-narn-cruiser | Narn Heavy Cruiser | SUPPORT (fleet) | NARN | 3 | 0 | 0 | 0 | 4 |
| dlx-flt-minbari-warcruiser | Minbari War Cruiser | SUPPORT (fleet) | MINBARI | 4 | 0 | 0 | 0 | 5 |
| dlx-flt-centauri-fleet | Centauri Battle Fleet | SUPPORT (fleet) | CENTAURI | 3 | 0 | 0 | 0 | 4 |
| dlx-loc-babylon5 | Babylon 5 | LOCATION | NONALIGNED | 4 | 0 | 0 | 0 | 3 |
| dlx-loc-narn-homeworld | Narn Homeworld | LOCATION | NARN | 3 | 0 | 0 | 0 | 3 |
| dlx-agn-narn-revenge | Revenge | AGENDA | NARN | 0 | 0 | 0 | 0 | 0 |
| dlx-agn-knowledge-is-power | Knowledge is Power | AGENDA | NONALIGNED | 0 | 0 | 0 | 0 | 0 |
| dlx-cnf-military-war | War | CONFLICT | NONALIGNED | 0 | 0 | 0 | 0 | 0 |
| dlx-aft-promotion | Promotion | AFTERMATH | NONALIGNED | 0 | 0 | 0 | 0 | 0 |

**Total: 38 cards** (4 + 10 + 6 + 3 Premiere; 15 Deluxe). `CardType.SUPPORT` is
this adaptation's generic bucket for groups/fleets/enhancements; fleets are
tagged in their `text`. The embedded fallback in `CardDatabase` is a separate,
smaller 12-card set so the prototype always runs even with zero JSON files.

---

## 2. JSON schema

Each file under `cards/` is a UTF-8 JSON object with a single `cards` array.
Each element is one card. Unknown keys (`set`, `rarity`, `_comment`) are ignored
by the loader, so they are safe to use for documentation. A top-level bare array
(`[ {…}, {…} ]`) is also accepted.

| Field | Type | Required | Maps to `Card` ctor arg | Notes |
|-------|------|----------|-------------------------|-------|
| `id` | string | **yes** | `id` | Unique; later files override an earlier same-id card. |
| `name` | string | no (defaults to `id`) | `name` | |
| `type` | string enum | no (default `CHARACTER`) | `CardType` | One of `AMBASSADOR, CHARACTER, CONFLICT, AFTERMATH, AGENDA, LOCATION, SUPPORT` (case-insensitive). |
| `faction` | string enum | no (default `NONALIGNED`) | `FactionId` | One of `HUMAN, MINBARI, NARN, CENTAURI, VORLON, SHADOW, PSI_CORPS, NONALIGNED`. |
| `cost` | number | no (default 0) | `cost` | Printed Influence Cost. |
| `influence` | number | no (default 0) | `influence` | Influence Rating bonus the card grants in play. |
| `diplomacy` | number | no (default 0) | `diplomacy` | |
| `intrigue` | number | no (default 0) | `intrigue` | |
| `psi` | number | no (default 0) | `psi` | |
| `military` | number | no (default 0) | `military` | Fleets/locations only. |
| `text` | string | no (default `""`) | `text` | Minimal flavor / rules summary only. |
| `imageUrl` | string | no (default `""`) | `imageUrl` | Public URL; never image bytes. |

### Worked example (one complete card)

```json
{
  "id": "prem-amb-minbari-delenn",
  "name": "Delenn",
  "set": "Premiere",
  "rarity": "Starter",
  "type": "AMBASSADOR",
  "faction": "MINBARI",
  "cost": 0,
  "influence": 0,
  "diplomacy": 3,
  "intrigue": 1,
  "psi": 2,
  "military": 0,
  "text": "Starting Ambassador — Minbari Federation. Grey Council Member.",
  "imageUrl": "https://www.vintageccg.com/b5/images/premiere/delenn.jpg"
}
```

This parses to:
`new Card("prem-amb-minbari-delenn", "Delenn", CardType.AMBASSADOR, FactionId.MINBARI, 0, 0, 3, 1, 2, 0, "Starting Ambassador …", "https://…/delenn.jpg")`.

---

## 3. Ambiguous rules — resolutions for the single-player engine

The engine is Task 2; this table records the **deterministic ruling** Task 1's
domain model assumes so the two tasks agree. "Citation" gives the rulebook topic,
or a community source when the rulebook is silent for the single-player case.

| # | Question | Citation | Deterministic ruling baked into the domain |
|---|----------|----------|--------------------------------------------|
| 1 | Turn/phase order — rulebook lists READY→CONFLICT→ACTION→AFTERMATH→DRAW, contract lists READY→CONFLICT→ACTION→RESOLUTION→DRAW. | Rulebook "Overview" (rounds) + "The Resolution Round". | The rulebook's *Resolution* and *Aftermath* steps are folded into one `Phase.RESOLUTION` (the rulebook itself plays aftermaths during resolution). `Phase` enum = READY, CONFLICT, ACTION, RESOLUTION, DRAW. |
| 2 | Starting Influence Rating and pool. | "Influence"; "Set Influence Tokens". | Rating = 4, spendable pool = 4. `PlayerState` initializes both to 4 and base Power = rating. |
| 3 | Opening hand size — rulebook uses a *chosen* 4-card hand (1 ambassador + 3 non-duplicate types); contract mandates a random 6-card draw. | "Your Starting Hand". | Contract wins for the prototype: Ambassador is pre-placed in the Inner Circle (not counted in hand) and `OPENING_HAND_SIZE = 6` cards are drawn at random. Documented deviation. |
| 4 | Conflict success threshold (ties). | "The Resolution Round" Step 1: "Total modified support **must exceed** total modified opposition." | Initiator wins **iff** modified support is *strictly greater* than opposition; a tie is a loss for the initiator. Encoded as `Card.support(type)` summation; the engine compares with strict `>`. |
| 5 | Which ability resolves which conflict. | "Conflict Cards". | `Card.support(ConflictType)` maps DIPLOMACY→diplomacy, INTRIGUE→intrigue, PSI→psi, MILITARY→military. Characters can never feed a Military conflict directly (Mil = 0); only fleets/locations carry Military. |
| 6 | Can the Ambassador ever leave the Inner Circle / be discarded? | "Your Starting Ambassador". | Never demoted or discarded. `PlayerState.getAmbassador()` always reads the first AMBASSADOR in INNER_CIRCLE; `GameFactory` places it there and the deck build excludes it from the draw pile. |
| 7 | Influence Rating floor. | "Set Influence Tokens": "may never be reduced below three." | The domain does not hard-clamp (it stores any int), but the documented invariant for the engine is a floor of 3. Recorded here so Task 2 enforces it. |
| 8 | Sponsoring an off-race (loyal to a different race) character. | "Bringing Characters into Play": double the listed cost; Neutral = no surcharge. | `Card.getCost()` is the *printed* cost; the engine applies the ×2 surcharge for a non-matching loyal faction and no surcharge for `NONALIGNED` (Neutral). `forFaction()` already offers each faction its own loyal cards **plus** NONALIGNED. |
| 9 | Victory threshold. | "Victory": 20 Power and more than any other player. | `VICTORY_POWER = 20` lives in the engine; the domain exposes `getPower()`/`setPower()` with base Power = Influence Rating so the engine can add bonuses. |
| 10 | Standard game player count / races (rulebook supports 2–5). | "Game Overview"; "Choosing Your Race". | Single-player fixes exactly 4 factions: HUMAN (human, index 0), MINBARI, NARN, CENTAURI — the four Premiere player races. Deterministic seating, human always first. |
| 11 | "Marked" / Shadow–Vorlon opposing marks, tension/unrest tracks. | "Marks"; "Set Tension Tokens". | **Out of scope** for the prototype domain — not modelled as fields. Noted so neither downstream task assumes they exist. Community consensus (BoardGameGeek B5 CCG forums) is that marks/tension are non-essential to a minimal 1-v-AI power race. |
| 12 | Card uniqueness ("Limited" — one copy in play). | "Character Cards". | Not enforced at the domain level (the starter set has no duplicates and decks are built once per game). Recorded as an engine-side concern if duplicates are ever added. |

### Community-source note

Where the rulebook is silent on *single-player* specifics (it is written for 2–5
human players), the project follows the BoardGameGeek *Babylon 5 CCG* community's
common solitaire convention: replace human opponents with deterministic AI
factions, keep the standard turn structure, and resolve all hidden-information /
"fast and loose" timing by fixed initiative order (lowest Influence Rating acts
first — rulebook "Determine Initiative"). These conventions are reflected in
`GameFactory` (fixed 4-faction seating) and `Phase` (linear, no simultaneous
rounds).

---

## 4. Files delivered by Task 1

```
domain/  ConflictType, Phase, CardType, FactionId, ZoneType,
         Card, Zone, Deck, PlayerState, GameState, GameFactory,
         GameListener, NoOpGameListener
data/    CardDatabase, ImageLoader, MiniJson (package-private JSON parser)
resources/cards/  manifest.txt + premiere-ambassadors/characters/conflicts/aftermath.json + deluxe-cards.json (38 cards)
docs/    research-dossier.md (this file)
```

`GameListener.onConflictResolved(engine.ConflictResult)` is the single permitted
domain→engine forward reference (per contract); it resolves once Task 2 merges.
