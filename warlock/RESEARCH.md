# RESEARCH — The Warlock of Firetop Mountain (Fighting Fantasy Boardgame, 1986)

This document records the game systems this project replicates and, crucially, an
audit of **what could be verified vs. what was reconstructed**. No copyrighted
rules text or artwork is reproduced; everything below is paraphrase and mechanical
summary written in my own words.

> **Verification note:** External web search / page fetching was **unavailable** in
> the build environment used for this recreation (the demo proxy blocks outbound
> web tools). The Fighting Fantasy *core* mechanics below are well-established and
> stated from long-standing general knowledge of the system. The *boardgame-specific*
> structure (exact card lists, exact board topology, exact multiplayer edge rules)
> could **not** be checked against a primary source in this session. Where a
> board-specific detail is uncertain, I state the reasonable ruling I adopted and
> flag it **[ADOPTED RULING]** so the gap is auditable. None of these adopted rulings
> changes the faithful FF core; they only fill boardgame-shaped holes.

---

## 1. Fighting Fantasy core mechanics (high confidence)

### 1.1 Attribute generation
An adventurer has three attributes, each with an **Initial** (maximum) value and a
**Current** value:

| Attribute | Generation formula | Range |
|-----------|--------------------|-------|
| SKILL     | 1d6 + 6            | 7–12  |
| STAMINA   | 2d6 + 12           | 14–24 |
| LUCK      | 1d6 + 6            | 7–12  |

Current values fluctuate during play but may never exceed the Initial value
(except where a rule explicitly allows it). SKILL measures combat prowess,
STAMINA is health/endurance, LUCK is fortune.

### 1.2 Combat (2d6 opposed)
Combat is resolved in **Attack Rounds**. Each combatant computes an
**Attack Strength**:

```
Attack Strength = 2d6 + current SKILL
```

- Higher Attack Strength wounds the opponent for **2 STAMINA**.
- Equal Attack Strengths = the blows are parried, **no damage** that round.
- Rounds repeat until one combatant reaches **0 STAMINA** (defeated/dead).

### 1.3 Testing Your Luck
To *Test your Luck*: roll **2d6**.
- If the roll is **less than or equal to** current LUCK → **Lucky**.
- If the roll is **greater than** current LUCK → **Unlucky**.
- **Every** test, lucky or not, reduces current LUCK by **1** (luck is a
  depleting resource).

Luck applied to combat (the standard modifiers):

- **When you have just wounded a monster**, you may Test your Luck to press the
  attack:
  - **Lucky** → you inflict **2 extra** STAMINA damage (4 total that round).
  - **Unlucky** → the wound is only a graze; the monster **recovers 1** (net 1
    damage that round).
- **When a monster has just wounded you**, you may Test your Luck to soften it:
  - **Lucky** → you take **1 less** damage (recover 1; net 1 lost that round).
  - **Unlucky** → the blow is worse; you lose **1 extra** (net 3 lost that round).

Plain (non-combat) Luck tests simply resolve Lucky/Unlucky per the room/event and
still cost 1 LUCK.

### 1.4 Stamina, death, resting, provisions
- STAMINA at **0** means the character is dead (in this boardgame adaptation, out
  of the game).
- **Provisions** may be eaten (outside combat) to restore STAMINA — commonly
  **+4 STAMINA** per meal, never above Initial STAMINA. **[ADOPTED RULING: +4 per
  provision, capped at Initial]**
- Resting/potions can restore SKILL/STAMINA/LUCK up to Initial depending on the
  item.

---

## 2. The 1986 boardgame adaptation

The 1986 Games Workshop product turns the 1982 gamebook dungeon into a physical
board: a dungeon of connected rooms/tiles beneath Firetop Mountain, tokens for
1–4 adventurers, dice, and decks of cards for encounters, treasure and hazards.
Players race/crawl through the dungeon toward the Warlock **Zagor** and his
treasure.

### 2.1 Structure adopted here
- **Board = graph of rooms.** Each room has a type, a position on a grid for
  drawing, and exits to adjacent rooms. The dungeon runs from an **Entrance** to
  Zagor's **Lair** (the deepest room). **[ADOPTED RULING: hand-authored 16-room
  dungeon graph; the physical board's exact tile layout could not be verified.]**
- **Room types:** Entrance, Empty (corridor), Monster, Treasure, Trap, Event,
  Special (fountain/shrine), Lair (Zagor).

### 2.2 Sequence of play (per player turn)
1. **Move** — roll 1d6 and move up to that many rooms along corridors (step by
   step; the player picks the path). **[ADOPTED RULING: die-based movement, 1d6
   steps. The boardgame used dice/card movement; dice movement chosen as the
   faithful, verifiable default.]**
2. **Resolve the room entered** — draw the appropriate card / fight the monster /
   spring the trap / collect treasure.
3. **End turn** — pass to the next player (hot-seat).

### 2.3 Card decks
Three decks, each shuffled at game start and reshuffled from the discard pile when
exhausted:
- **Encounter deck** — monsters that ambush the party.
- **Treasure deck** — gold, potions, weapons, keys, artefacts.
- **Event deck** — traps, boons, and dungeon happenings (test Luck/Skill, gain or
  lose resources).

All specific card names and flavor text in `model/Cards.java` are **original
writing** evoking the tone of a Firetop Mountain crawl — they are **not**
transcriptions of the printed cards (which could not be sourced here).
**[ADOPTED RULING: original card set, mechanically representative of the three
deck roles.]**

### 2.4 Win / lose conditions
- **Win:** reach Zagor's Lair and **defeat Zagor** in combat (claiming the
  treasure and the way out). The gamebook's climax is the confrontation with the
  Warlock; the boardgame's goal is likewise to beat him and take the treasure.
- **Lose:** the entire party is wiped out (all adventurers at 0 STAMINA).

### 2.5 Zagor (the Warlock)
A formidable final foe. **[ADOPTED RULING: Zagor SKILL 12, STAMINA 18]** — high
end of the scale, consistent with the Warlock being the toughest encounter.
His exact printed boardgame stats could not be verified here.

### 2.6 Multiplayer (1–4 hot-seat)
Players take turns in seat order. Each controls one adventurer with independent
attributes/inventory. Cooperative-competitive: any surviving adventurer defeating
Zagor ends the game as a win; the party loses only if everyone dies.
**[ADOPTED RULING: shared board, independent characters, first to slay Zagor wins
for the table.]**

---

## 3. Explicitly unverifiable items (audit list)

The following could **not** be confirmed against a primary source in this build and
are reconstructions/adopted rulings, not claims of fidelity:

1. Exact board tile layout & room count (adopted: 16-room hand-authored graph).
2. Exact movement subsystem — dice vs. movement cards vs. free move (adopted: 1d6
   dice movement).
3. Exact card names, counts, and printed effects (adopted: original 3-deck set).
4. Zagor's printed boardgame stat line (adopted: SKILL 12 / STAMINA 18).
5. Provision healing amount (adopted: +4, capped at Initial).
6. Fine multiplayer edge rules — turn order after a death, blocking, PvP (adopted:
   simple seat order, no PvP, dead players skipped).

Everything in **Section 1 (FF core)** is standard and implemented faithfully and is
covered by unit tests (`test/com/whim/firetop/engine`).
