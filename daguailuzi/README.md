# 大怪路子 (Da Guai Lu Zi)

A standalone Java 8 Swing desktop card game: 6 players (1 Human + 5 AI), two
alternating teams of three, rule-enforced engine, with an interactive Coach Mode.

This repo is built by three parallel tasks against `DAGUAILUZI_CONTRACT.md` (the single
source of truth). **Task 1 (this branch)** delivers the `com.dglz.domain` and
`com.dglz.engine` packages only. `ai/`, `ui/`, and `app/` are owned by Tasks 2 and 3.

## Build & run

Plain `javac`, no external libraries:

```
javac -d daguailuzi/out $(find daguailuzi/src -name '*.java')
java -cp daguailuzi/out com.dglz.app.Main   # available once Task 3 lands
```

Task 1's packages compile on their own (no `ai/ui/app` needed yet):

```
javac -d daguailuzi/out $(find daguailuzi/src/com/dglz/domain daguailuzi/src/com/dglz/engine -name '*.java')
```

> Note: the spec targets Java 8. This container only had OpenJDK 17, so it was verified
> with `javac --release 8` (which rejects post-8 language features). The source uses no
> `var`, switch expressions, text blocks, records, or `java.time`-only APIs.

## Deck

`domain.Deck` builds **162 cards** = 3 decks × (52 ranked + 2 jokers) = 3 × 54. `deal(6, 27)`
hands 27 cards to each of the 6 seats (6 × 27 = 162, the whole deck is dealt).

## Combination rules core (`engine.ComboValidator`)

`identify(cards)` returns the single **best** legal combination, preferring the highest
`ComboType`. Jokers act as wildcards to complete `PAIR`, `TRIPLE`, `STRAIGHT`, `FLUSH`,
`FULL_HOUSE`, `FOUR_PLUS_ONE`, `STRAIGHT_FLUSH`, and `FIVE_OF_A_KIND`. A bare joker played
as a `SINGLE` is its own rank (orders 16 / 17, above all natural ranks; `2` is high at 15).

Wildcard fill constraint per the contract: **a Small Joker may never represent a Big
Joker.** A Big Joker may stand in for any rank (including a Small Joker); a Small Joker may
stand in for any *natural* rank only. (See `serveAs` / `bestSameRank`.)

5-card type detection runs strongest-first
(FIVE_OF_A_KIND → STRAIGHT_FLUSH → FOUR_PLUS_ONE → FULL_HOUSE → FLUSH → STRAIGHT) and
returns the first match, so e.g. four kings + a joker resolves to FIVE_OF_A_KIND, not
FOUR_PLUS_ONE.

## 5-road enumeration bound

`enumerate(hand, leadRoad, toBeat)` for the `FIVE` road enumerates **exhaustively over all
C(n, 5) card subsets** of the hand (n ≤ 27, so at most C(27,5) = 80 730 subsets at full
hand, shrinking each trick), de-duplicated by combo type + card faces. This is a hard,
predictable bound and is fast enough for per-turn use; no sampling or truncation is applied.
Singles/pairs/triples are enumerated over C(n,1/2/3) similarly.

## Turn / trick flow (`engine.GameEngine`)

Deal 27 each; seat 0 (Human, Team A) leads first. Each trick: the leader sets the Road;
following players must play a same-Road combo that `beats` the current best, or pass. A
**pass is permanent for that trick.** When every other active player has passed, the
current best holder wins and leads the next trick (if that winner is out, the lead passes
to the next active seat). A player who is out is skipped. The first team with all three
members out wins.

## Self-check

Task 1 ships no `main`. Correctness was verified with an out-of-tree harness exercising deck
size, every combination type (including joker fills and the Small-Joker-≠-Big-Joker rule),
`beats` ordering, enumeration, and a full seeded game played to a team win (29/29 checks
pass).
