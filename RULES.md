# Finesse Chess — Rules

This repository implements a digital version of Walter Browne's **"Finesse"** chess
variant.

## Architecture

The engine is split into a rule-agnostic **core** and a variant-specific layer:

- **`com.finesse.core`** — the modular engine (this task). It knows about boards,
  positions, pieces, colors, moves, and game state, but contains **no** rules
  about how Finesse is set up or how pieces move. The board dimensions are
  configurable (default 8x8) and nothing hardcodes an 8x8 assumption.
- **`com.finesse.variant`** — Finesse-specific rules (**owned by Task 2**). This
  is where the starting position is built and where `MoveGenerator` is
  implemented. **Finesse rule details belong here, not in the core.**

## Where the rules live

> **Finesse-specific setup and movement rules are owned by Task 2** and live under
> `com.finesse.variant`. This file is a stub; the variant module is the source of
> truth for how the game is actually played. The core's `GameState` starts with an
> **empty** board — Task 2's setup class populates the Finesse starting position.

## Extending the core

- **New piece types:** append constants to `PieceType`. The core makes no
  assumption about the roster beyond the six standard types; the variant's
  `MoveGenerator` defines their behaviour.
- **Non-standard board sizes:** construct `Board(width, height)`. All core
  operations are dimension-driven.
