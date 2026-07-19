-- =============================================================================
-- Tiwa's Mah Jong — MySQL schema (DDL)
-- Engine: MySQL 8.0+ / InnoDB / utf8mb4. Run:  mysql tiwas_mahjong < db/schema.sql
--
-- Design notes baked into this schema:
--  * Identity is passwordless. A profile is keyed by (name, email_key) where
--    email_key = COALESCE(email,'').  So a name-only "Alice" (email_key='') and a
--    "Alice"+email row are DISTINCT rows and never collide. Guests get a generated
--    unique display name (email NULL). This is the confirmed collision policy.
--  * `games` is a continuous seating session (NO fixed 16-round length here).
--    `game_seats` maps seats E/S/W/N -> a player row. Any seat can be AI or human;
--    nothing in the schema assumes 3-of-4 are AI — this is the multiplayer seam.
--  * `hand_events` is the authoritative chronological log (draws/discards/claims/
--    melds/replacements). §8 verification runs against it server-side; the client
--    never needs it. Discards are derivable here even though hidden in opponents' UI.
--  * Money is stored as integer minor units? NO — the rulebook rounds DOWN to whole
--    currency units, so money columns are whole-unit signed integers. Points are
--    whole integers too (final scores round down to nearest point).
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- players : one row per identity (human guest / named / named+email / AI).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS players (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name           VARCHAR(64)     NOT NULL,
  email          VARCHAR(255)        NULL,             -- NULL for guest / name-only
  -- Generated key so (name, email_key) is a single clean UNIQUE index.
  email_key      VARCHAR(255)    AS (COALESCE(email, '')) STORED,
  is_ai          TINYINT(1)      NOT NULL DEFAULT 0,
  is_guest       TINYINT(1)      NOT NULL DEFAULT 0,

  -- Persisted lifetime + current stats (updated after every hand).
  games_played   INT UNSIGNED    NOT NULL DEFAULT 0,
  games_won      INT UNSIGNED    NOT NULL DEFAULT 0,
  current_money  BIGINT          NOT NULL DEFAULT 1000, -- new profiles start at $1000
  money_won      BIGINT          NOT NULL DEFAULT 0,     -- lifetime cumulative winnings
  current_points BIGINT          NOT NULL DEFAULT 0,
  owed_debt      BIGINT          NOT NULL DEFAULT 0,     -- unpaid remainder (partial pay)

  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_played_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (id),
  UNIQUE KEY uq_player_identity (name, email_key),
  KEY idx_players_is_ai (is_ai),
  KEY idx_players_last_played (last_played_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- games : a continuous table/seating session. No end-of-match condition other
-- than a player quitting or going bankrupt. round_wind rotates for flavor.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS games (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  status        ENUM('active','ended') NOT NULL DEFAULT 'active',
  round_wind    ENUM('E','S','W','N')  NOT NULL DEFAULT 'E',
  hands_played  INT UNSIGNED    NOT NULL DEFAULT 0,
  ended_reason  ENUM('bankruptcy','quit') NULL,
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ended_at      DATETIME            NULL,
  PRIMARY KEY (id),
  KEY idx_games_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- game_seats : which player fills each seat. Multiplayer-ready seam.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS game_seats (
  game_id   BIGINT UNSIGNED NOT NULL,
  seat      ENUM('E','S','W','N') NOT NULL,
  player_id BIGINT UNSIGNED NOT NULL,
  is_ai     TINYINT(1)      NOT NULL DEFAULT 1,
  PRIMARY KEY (game_id, seat),
  KEY idx_game_seats_player (player_id),
  CONSTRAINT fk_seat_game   FOREIGN KEY (game_id)   REFERENCES games(id)   ON DELETE CASCADE,
  CONSTRAINT fk_seat_player FOREIGN KEY (player_id) REFERENCES players(id)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- hands : one row per hand played. Captures the §6 scoring breakdown + §5 flags.
-- points_limit / money_limit are NULL when the hand's mode is 'unlimited'.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hands (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  game_id           BIGINT UNSIGNED NOT NULL,
  hand_number       INT UNSIGNED    NOT NULL,          -- 1-based within the game
  round_wind        ENUM('E','S','W','N') NOT NULL,
  dealer_seat       ENUM('E','S','W','N') NOT NULL,

  limit_mode        ENUM('limited','unlimited') NOT NULL DEFAULT 'limited',
  points_limit      INT UNSIGNED    NULL,              -- NULL iff unlimited
  money_limit       INT UNSIGNED    NULL,              -- NULL iff unlimited ($1/pt)

  result_type       ENUM('win','draw','false_mahjong') NOT NULL,
  winner_seat       ENUM('E','S','W','N') NULL,        -- NULL on draw / offender path

  -- §6 scoring audit (all whole numbers; final rounds down):
  base_points       INT UNSIGNED    NULL,
  flower_points     INT UNSIGNED    NULL,
  mahjong_bonus     INT UNSIGNED    NULL,
  doubles_count     INT UNSIGNED    NULL,              -- total doubles applied
  final_points      INT UNSIGNED    NULL,              -- after doubles + cap
  is_limit_hand     TINYINT(1)      NOT NULL DEFAULT 0,
  fully_concealed   TINYINT(1)      NOT NULL DEFAULT 0,
  special_hand      VARCHAR(48)     NULL,              -- '13_orphans','all_flowers_seasons',...
  win_timing        ENUM('normal','first_tile','last_wall_tile','final_discard') NULL,

  wall_remaining    INT UNSIGNED    NULL,
  started_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ended_at          DATETIME            NULL,

  PRIMARY KEY (id),
  UNIQUE KEY uq_hand_seq (game_id, hand_number),
  KEY idx_hands_game (game_id),
  CONSTRAINT fk_hand_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- hand_results : per-player-per-hand settlement delta (§7). One row per seat.
-- Sum of money_delta across the 4 rows of a hand is 0 (zero-sum, after rounding
-- once at settlement). money_after/points_after snapshot the profile post-hand.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hand_results (
  hand_id            BIGINT UNSIGNED NOT NULL,
  player_id          BIGINT UNSIGNED NOT NULL,
  seat               ENUM('E','S','W','N') NOT NULL,
  is_winner          TINYINT(1)      NOT NULL DEFAULT 0,

  points_delta       BIGINT          NOT NULL DEFAULT 0,
  money_delta        BIGINT          NOT NULL DEFAULT 0,  -- +winnings / -losses, whole units
  flower_pay_delta   BIGINT          NOT NULL DEFAULT 0,  -- net of immediate $2 flower payments
  penalty_delta      BIGINT          NOT NULL DEFAULT 0,  -- false-mahjong etc.
  debt_incurred      BIGINT          NOT NULL DEFAULT 0,  -- unpaid remainder this hand

  money_after        BIGINT          NOT NULL,
  points_after       BIGINT          NOT NULL,

  PRIMARY KEY (hand_id, seat),
  KEY idx_results_player (player_id),
  CONSTRAINT fk_result_hand   FOREIGN KEY (hand_id)   REFERENCES hands(id)   ON DELETE CASCADE,
  CONSTRAINT fk_result_player FOREIGN KEY (player_id) REFERENCES players(id)  ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------------
-- hand_events : authoritative chronological log for §8 verification.
-- seq is monotonic per hand. tile_code uses the canonical codes in domain/tiles.js
-- (e.g. 'D5','B9','C1','WE','DR','F2','S3'). meta holds structured extras (meld
-- tiles, claimed-from seat, replacement source, penalty target, etc.).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hand_events (
  id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  hand_id    BIGINT UNSIGNED NOT NULL,
  seq        INT UNSIGNED    NOT NULL,
  seat       ENUM('E','S','W','N') NOT NULL,
  event_type ENUM('deal','draw','discard','claim_pung','conceal_kong',
                  'kong_upgrade','flower','replacement_draw','mahjong',
                  'penalty','warning') NOT NULL,
  tile_code  VARCHAR(4)      NULL,
  meta       JSON            NULL,
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_event_seq (hand_id, seq),
  KEY idx_events_hand (hand_id),
  CONSTRAINT fk_event_hand FOREIGN KEY (hand_id) REFERENCES hands(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
