# Babylon 5 CCG — Engine & AI Design (Task 2)

Scope: the rule engine and the single-player opponents for the standalone Babylon 5 CCG
desktop app. This document covers the engine architecture, the turn/zone state machine, and
the AI design (heuristics, decision trees, pseudocode). It is the companion to the binding
`BABYLON5_CONTRACT.md` and the rule authority `babylon5/docs/rulebook-source.txt`
(*Psi Corps v1.3b*). Where the paper rules are silent or per-card, the deterministic prototype
ruling is stated and justified.

Java 8, no external libraries. Base package `com.whim.babylon5`. This task owns
`engine/**`, the engine tests, and this document; it **imports** Task 1's `domain` / `data`
types and never redeclares them.

---

## 1. Architecture: MVC + a game-engine loop

```
        +-------------------+        events (GameListener)        +------------------+
        |   View (Task 3)   | <----------------------------------- |  Engine (Task 2) |
        |  Swing MainWindow |                                      |   GameEngine     |
        |  panels, input    | -- user intents (sponsor, declare -> |   AIPlayer       |
        +-------------------+    conflict, commit, advance) ------ +------------------+
                                                                          |
                                                                   mutates / reads
                                                                          v
                                                  +---------------------------------------+
                                                  |  Model (Task 1) com.whim.babylon5.*    |
                                                  |  GameState, PlayerState, Zone, Card    |
                                                  +---------------------------------------+
```

- **Model** (Task 1 `domain`/`data`): pure data — `GameState`, `PlayerState`, `Zone`, `Card`,
  the enums, `GameFactory`, `CardDatabase`. No rules live here.
- **Controller / engine** (this task): `GameEngine` is the single authority that mutates the
  model. Every rule (phase progression, sponsoring cost & legality, conflict math, victory)
  is validated here so the UI and the AI cannot diverge. `AIPlayer` is a *pure advisor*: it
  reads `GameState` and returns recommended moves; it never mutates anything.
- **View** (Task 3): Swing. It implements `GameListener` and reacts to
  `onPhaseChanged / onConflictResolved / onStateChanged / onLog`. It translates clicks into
  engine calls. It owns **no** rules.

### The game-engine loop & threading

The loop is *phase-stepped*, not a tight render loop. One "tick" is `advancePhase()`, which
performs the mechanical effects of leaving the current round and fires listener events. A
human turn is driven by UI intents between ticks; an AI turn is driven entirely by
`runAiTurn(playerIndex)`, which sequences its own decisions and `advancePhase()` calls.

Contract-critical: **`runAiTurn` is pure logic with no Swing dependency**, so the UI can run
it on a background worker thread and marshal the resulting `GameListener` callbacks back onto
the EDT with `SwingUtilities.invokeLater`. The engine never touches `javax.swing`.

```
loop (until checkVictory() != null):
    p = state.activePlayer
    if p.isHuman:  wait for UI intents, each calling engine methods; UI calls advancePhase()
    else:          engine.runAiTurn(p.index)   // off the EDT
    winner = engine.checkVictory()             // evaluated at end of each turn (Draw step 5)
```

---

## 2. Turn / zone state machine

Strict round order from the rulebook ("Playing the Game"):

```
   READY  ->  CONFLICT  ->  ACTION  ->  RESOLUTION  ->  DRAW  ──(pass play)──┐
     ^                                  (Aftermath here)                     │
     └─────────────────────  next player's turn  ────────────────────────────┘
```

`advancePhase()` transition effects:

| Leaving      | Effect applied                                                                   | Rulebook |
|--------------|----------------------------------------------------------------------------------|----------|
| `READY`      | Un-rotate (ready) all the active player's non-neutralized cards; restore the spendable influence pool to the full Influence Rating | Ready round, steps 1–2 |
| `CONFLICT`   | (conflicts were declared during the round) → ACTION                              | Conflict round |
| `ACTION`     | (sponsors / commits happened during the round) → RESOLUTION                       | Action round |
| `RESOLUTION` | conflict math + fallout + aftermath window happen *within* RESOLUTION → DRAW      | Resolution round |
| `DRAW`       | discard neutralized **supporting** cards; draw one free card; then pass to next player (wrap → `incrementTurn`) and begin their `READY` | Draw round, steps 1 & 3 |

### Zone transitions

Zones (Task 1 `ZoneType`): `DRAW_DECK, HAND, INNER_CIRCLE, SUPPORTING, DISCARD, REMOVED`.

```
 DRAW_DECK --draw(free card, Draw step 3)--> HAND
 HAND --sponsor CHARACTER (pay influence, rotate an Inner Circle sponsor)--> SUPPORTING
 HAND --sponsor/replace AMBASSADOR--> INNER_CIRCLE
 SUPPORTING --promote (not in MVP scope; cost = sponsor + 1/Inner-Circle char)--> INNER_CIRCLE
 SUPPORTING --neutralized at end of turn (Draw step 1)--> DISCARD
 any --card effect "remove from game"--> REMOVED
```

Per the rulebook, neutralized **Inner Circle** cards are *not* discarded (they stay, flipped),
only neutralized **supporting** cards are swept in Draw step 1. The engine enforces exactly that.

### "Ready" vs "rotated" vs "neutralized"

- **Ready** = face-up, not rotated (`Card.isReady() == true`, damage below threshold).
- **Rotated** = acted this turn (`setReady(false)`); re-readied next Ready round.
- **Neutralized** (rule "The Effects of Damage"): total damage ≥ the card's **highest printed
  ability** (and ≥ 1 damage). A neutralized card applies **no** ability and is not re-readied.
  Damage also reduces the applied ability point-for-point, never below 0. This is implemented in
  `GameEngine.isNeutralized` / `effectiveAbility`.

---

## 3. Core conflict math (the headline rule)

Resolution Step 1: *"Total modified support must **exceed** total modified opposition for the
conflict to succeed; otherwise it fails."* The comparison is **strict** — a tie is a loss for
the initiator. `GameEngine.resolveConflict`:

```
support    = Σ effectiveAbility(card, type)  over committed supporters
opposition = Σ effectiveAbility(card, type)  over committed opposers
initiatorWon = support > opposition                         // STRICT exceed; tie => loss
effectiveAbility(c,t) = isNeutralized(c) ? 0 : max(0, c.support(t) - c.getDamage())
```

Only the matching discipline counts: a Psi-5 / Diplomacy-0 character contributes 0 to a
Diplomacy conflict (rulebook: "Only cards with a non-zero ability of the appropriate type may
rotate to support or oppose").

### Deterministic ruling — conflict fallout damage

In the paper game, damage is dealt by explicit **Attack** actions and per-card text, not by the
bare support/opposition tally. For a self-contained single-player engine we model the strain of
a decided conflict as: **the winning margin is dealt as 1-point damage tokens spread round-robin
across the losing side's committed cards**; a 0-margin tie deals no damage. Cards pushed to ≥
their highest ability are neutralized and returned in `ConflictResult.neutralized()`. This keeps
committed characters meaningfully at risk without re-implementing the full Attack sub-system, and
it is fully deterministic. (Community reference: BoardGameGeek B5 CCG rules forum — conflicts
themselves do not deal damage; only attacks do. We deviate intentionally and minimally for a
playable prototype.)

### Power & victory

- `computePower(p) = p.getInfluenceRating() + powerBonus(p)` — base Power equals the current
  Influence Rating (rulebook "Victory"); `powerBonus` is a hook for card-text Power riders (0 in
  the base prototype set). The result is cached on `PlayerState`.
- `checkVictory()` returns the player with **≥ 20 Power AND strictly more than every other
  player**; a tie at the top yields no Standard Victory. Minimum Influence Rating floor (3) is a
  constant for engine use.

---

## 4. AI design

`AIPlayer` has three tiers (`AiDifficulty.EASY/MEDIUM/HARD`). It is stateless beyond its
difficulty (reusable, thread-safe) and never mutates the model. `GameEngine` wires one brain to
each AI seat (default ladder EASY→MEDIUM→HARD for seats 1–3, overridable via `setAiDifficulty`)
and applies whatever the brain recommends after validation.

### 4.1 Tier philosophy

| Tier   | Sponsor                         | Declare conflict                              | Commit |
|--------|---------------------------------|-----------------------------------------------|--------|
| EASY   | random affordable character     | ~50% decline, else random target              | commit anything that can help |
| MEDIUM | best ability-per-influence      | best discipline vs the weakest opponent; won't pick a losing matchup | commit if ability ≥ 1 |
| HARD   | one-ply look-ahead board eval   | only when it projects a **strict** win        | spend strong cards, hold weak reserves |

### 4.2 Sponsoring — the required, fully-implemented decision

`chooseCharacterToSponsor(state, playerIndex)`:

```
me = state.players[playerIndex]
affordable = [ c in me.HAND : c.type == CHARACTER and cost(me,c) <= me.influencePool ]
cost(me,c) = c.cost * (c.faction == me.faction ? 1 : 2)     // loyalty doubling
if affordable empty: return null

switch difficulty:
  EASY:   return affordable[ rng.nextInt(affordable.size) ]          // greedy/random
  MEDIUM: return argmax_c  totalAbility(c) / max(1, cost(me,c))       // value per influence
  HARD:   return argmax_c  evaluateSponsor(state, me, c)              // look-ahead eval
          // decline if even the best eval < 0 and we can still profitably bank influence

evaluateSponsor(state, me, c):
    return totalAbility(c)                  // raw board strength gained
         + 0.5 * nonZeroAbilityCount(c)     // versatility: covers more conflict types
         - 0.6 * cost(me, c)                // influence is a scarce, victory-relevant resource
```

The engine then validates the recommendation (`sponsorCharacter`): Action round only, card in
HAND, a ready/un-neutralized Inner Circle character available to rotate, pool ≥ cost. On success
it pays, rotates the sponsor, and the new character enters `SUPPORTING` ready to act.

### 4.3 Declaring conflicts

`chooseConflict(state, playerIndex)` → a `Conflict` or `null` (decline — a legitimate, often wise
early-game choice per the rulebook tip).

```
bestType = the discipline in which my ready cards are collectively strongest
myStrength = readyStrength(me, bestType)
if myStrength <= 0: return null

EASY:   coin flip -> decline; else target a random opponent with bestType
MEDIUM/HARD:
    target  = argmax_i (myStrength - readyStrength(opponent_i, bestType))    // biggest gap
    gap     = that max
    HARD:   require gap > 0   (must project a strict win) else decline
    MEDIUM: require gap >= 0  (willing to contest an even matchup) else decline
    return Conflict(playerIndex, bestType, target)
```

`readyStrength(p,t) = Σ over p's ready Inner-Circle+Supporting cards of max(0, c.support(t) - damage)`.

### 4.4 Assigning support / opposition

`willCommit(card, pending, asSupport)` decides whether to rotate a given card into the conflict.
The engine calls it for each ready, type-capable card of the initiator (support) and, in an
all-AI matchup, for the target's defenders (opposition):

```
ability = max(0, card.support(pending.type) - card.damage)
if not card.isReady() or ability == 0: return false
EASY:   return true                          // throw everyone in
MEDIUM: return ability >= 1
HARD:   return ability >= 2 or asSupport      // spend strong cards; keep weak ones in reserve
```

Decision tree the engine drives during an AI turn (`runAiTurn`), all off the EDT:

```
READY:      advancePhase()              // ready cards, restore influence
CONFLICT:   pending = chooseConflict(); advancePhase()
ACTION:     toSponsor = chooseCharacterToSponsor(); if toSponsor: sponsorCharacter()
            if pending: for each ready type-capable card: if willCommit(...) rotate into pending
            advancePhase()
RESOLUTION: if pending: resolveConflict(pending)   // includes aftermath window
            advancePhase()
DRAW:       advancePhase()              // discard neutralized supporting, draw, pass play
```

---

## 5. Testing

`src/test/java/com/whim/babylon5/engine/EngineSelfTest.java` is a dependency-free `main`
(mirrors `startrek`'s `EngineSelfTest`) covering: the strict support>opposition boundary
(tie loses, +1 wins, wrong-discipline contributes 0), neutralization fallout, full phase
progression + turn increment + influence restore, influence spend & legality on sponsoring
(affordability, wrong phase, loyalty doubling), the Standard Victory condition (≥20 and strictly
highest; tie => none), and the MEDIUM/EASY AI sponsor decision. Run with plain `javac`/`java` —
no JUnit.

---

## 6. Contract conformance & deviations

- Every public engine signature matches `BABYLON5_CONTRACT.md` verbatim
  (`AiDifficulty`, `Conflict`, `ConflictResult`, `GameEngine`, `AIPlayer`, `VICTORY_POWER`).
- Added (allowed by the contract) helpers: `GameEngine.setAiDifficulty/aiFor`,
  `sponsorCost/effectiveAbility/isNeutralized`, `MIN_INFLUENCE_RATING`, and a public
  `ConflictResult` constructor. No contract type is changed or removed.
- Deterministic rulings (documented above): conflict-fallout damage model and same-race/neutral
  vs different-race loyalty cost (single-`FactionId` model → same faction = printed cost, else
  doubled).
