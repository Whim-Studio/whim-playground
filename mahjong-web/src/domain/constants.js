// =============================================================================
// domain/constants.js — every point and double value from Rulebook §6.
// This file is DATA ONLY (no logic). ScoringEngine reads from here so the
// magic numbers live in exactly one auditable place.
// =============================================================================

export const DEFAULT_POINTS_LIMIT = 1000;

// Base meld points: [exposed, concealed]. Honors = winds + dragons.
export const BASE_POINTS = {
  PUNG_SIMPLE: { exposed: 2, concealed: 4 },   // tiles 1–9
  PUNG_HONOR:  { exposed: 4, concealed: 8 },
  KONG_SIMPLE: { exposed: 4, concealed: 8 },
  KONG_HONOR:  { exposed: 8, concealed: 16 },
  CHOW:        { exposed: 0, concealed: 0 },   // chows are always concealed, 0 pts
};

export const FLOWER_SEASON_POINTS = 4;         // each, never doubles, not a meld
export const FLOWER_SEASON_PAYMENT = 2;        // $2 from every other player, immediate

// Doubles. Value = number of ×2 multipliers. Doubles stack MULTIPLICATIVELY:
// total *= 2 ** (sum of applicable double values).  See ScoringEngine.
export const DOUBLES = {
  ALL_CONCEALED_HAND:   2,
  NO_CHOWS:             1,
  ALL_CHOWS:            1,
  ALL_KONGS:            4,
  ALL_CONCEALED_KONGS:  4,   // separate from ALL_KONGS; both can apply (×2^8)
  DOUBLE_PUNG:          2,   // same number, two different suits
  MIXED_DOUBLE_CHOW:    1,   // same chow, two different suits
  SHORT_STRAIGHT:       1,   // 123+456 or 456+789, same suit
  TWO_TERMINAL_CHOWS:   1,   // 123 and 789
  ALL_ONE_SUIT_HONORS:  7,   // one suit + any honors
  ALL_HONORS:          10,   // winds/dragons only
  HEAVENLY_HAND:       13,   // dealer wins on starting hand
  EARTHLY_HAND:        13,   // non-dealer wins on first discard
  HUMAN_HAND:          13,   // win before discarding any tile
  FULLY_CONCEALED:      2,   // alias of ALL_CONCEALED_HAND per §5 (see open questions)
  LAST_WALL_TILE:       2,   // mahjong on last drawable wall tile
  FINAL_DISCARD:        1,   // mahjong on another player's final discard
};

// §5 flat first-tile bonus. Resolution vs. the 13-double hands is "take the
// higher of the two" (confirmed). See ScoringEngine.resolveFirstTileWin.
export const FIRST_TILE_FLAT_POINTS = 1000;

// §8 penalties (points). Money equivalent in Limited games is derived by the
// same money formula as winnings: (POINTS / pointsLimit) * moneyLimit, floored.
export const FALSE_MAHJONG_PENALTY_POINTS = 1000;   // to EACH other player
export const UNLIMITED_FALSE_MAHJONG_MONEY = 1000;  // $ to each other player (unlimited)

// Starting bankroll for every new profile (human or AI).
export const STARTING_MONEY = 1000;

export const CLAIM_TIMEOUT_MS = 6000;               // §3 claim window
