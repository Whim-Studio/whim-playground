import "./styles.css";
import {
  FACE_FRAMES,
  FACE_VIEW_BOX,
  MOTION_SEQUENCE,
} from "./whimFaceFrames.js";
import {
  resumeAudio,
  shoot as playShoot,
  explode,
  powerup,
  hit,
  gameOver,
  startMusic,
  stopMusic,
  toggleMute,
} from "./audio.js";

/*
 * Whim Asteroids is intentionally a small, dependency-free canvas loop.
 *
 * Future agents: keep simulation units in CSS pixels, let resizeCanvas handle
 * device-pixel-ratio scaling, and prefer adding new tuning knobs to
 * GAME_CONFIG/COLORS before scattering magic numbers through the loop.
 */

const canvas = document.querySelector("#gameCanvas");
const gameShell = document.querySelector(".game-shell");
const scorebar = document.querySelector(".scorebar");
const ctx = canvas.getContext("2d");
const scoreEl = document.querySelector("#score");
const roundTimeEl = document.querySelector("#roundTime");
const livesEl = document.querySelector("#lives");
const healthBarFillEl = document.querySelector("#healthBarFill");
const overlayEl = document.querySelector("#gameOverlay");
const overlayEyebrowEl = document.querySelector("#overlayEyebrow");
const overlayTitleEl = document.querySelector("#overlayTitle");
const overlayCopyEl = document.querySelector("#overlayCopy");
const restartButton = document.querySelector("#restartButton");
const shootButton = document.querySelector("#shootButton");

// "Deep-space ember": a single Whim-red world. A vertical plum-to-crimson void,
// warm-tinted stars, ember hazards, and red-forward chrome — with a few accent
// hues reserved exclusively for power-ups so they stay legible against the red.
// Canvas art (this object) and CSS chrome (:root tokens) must stay in sync.
const COLORS = {
  // Background gradient stops (top is the darkest void, bottom is warm crimson).
  backgroundTop: "#1a060c",
  background: "#2a0a12",
  backgroundBottom: "#3d0d18",
  surface: "#3a141d",
  surfaceHover: "#4a1a25",
  border: "#5a2530",
  foreground: "#f3ece6",
  muted: "#c79aa2",
  // The hot Whim red and its translucent derivatives, used for the player glow,
  // base bullets, pointer guide, grid, and most particle bursts.
  face: "#ff4d4d",
  faceSoft: "rgba(255, 77, 77, 0.14)",
  faceLine: "rgba(255, 77, 77, 0.52)",
  faceGlow: "rgba(255, 42, 36, 0.85)",
  // Star tint — a faint warm rose so the field reads as part of the red world.
  star: "#ffd9c2",
  // Hazards: every enemy reads as ember matter in the same warm-red void.
  // Identity is carried by silhouette + motion, not hue — color stays inside the
  // ember family (cooler/crimson = bigger & slower, whiter-hot = faster).
  hazard: "#ff7a59",
  hazardSoft: "rgba(255, 122, 89, 0.07)",
  hazardLine: "rgba(255, 122, 89, 0.5)",
  emberCore: "#ffb070", // hottest center for any molten body
  cinderEdge: "#7a1f12", // dark cooled crust / outline for rock
  cinderGlow: "rgba(255, 138, 74, 0.18)", // soft heat halo for new kinds
  moteHot: "#ff8a4a", // small fast embers / sparks
  veilCore: "#ff5e6e", // cool plasma/gas body (leans crimson, not orange)
  veilGlow: "rgba(255, 94, 110, 0.22)",
  // Friendly helpers, tinted to belong to the same warm world.
  squirrel: "#c2461b",
  danger: "#e08a6f",
};

// Primary gameplay tuning surface. Distances are CSS pixels; speeds are pixels
// per second; interval values are seconds. Start here for balancing changes.
const GAME_CONFIG = {
  acceleration: 820,
  maxSpeed: 360,
  activeDamping: 0.945,
  idleDamping: 0.86,
  scoreRate: 14,
  spawnIntervalStart: 1.55,
  spawnIntervalEnd: 0.48,
  maxDifficultyTime: 48,
  minPlayerSize: 48,
  maxPlayerSize: 72,
  safeSpawnDistance: 180,
  bulletSpeed: 900,
  bulletLife: 0.82,
  bulletRadius: 6,
  shootCooldownMs: 190,
  shardClearScore: 35,
  shardSplitThreshold: 12,
  shardSplitCount: 2,
  shardSplitSpeedBoost: 1.25,
  startingLives: 3,
  hitInvulnerabilityTime: 2.2,
  powerUpSpawnChance: 0.3,
  powerUpDuration: 10,
  rapidFireCooldownMs: 70,
  shardSlowFactor: 0.55,
  // Power-shot tuning: bigger, faster bullets that pierce through extra hazards
  // before being consumed. strongShotPierce is the number of additional shards a
  // single bullet can punch through (0 = stops on first hit, like a base shot).
  strongShotSpeed: 1180,
  strongShotRadius: 11,
  strongShotPierce: 2,
  maxHealth: 100,
  healthDamage: 25,
  // Power-up expansion tuning.
  spreadShotAngle: 0.22, // radians between Triple Tap pellets
  scoreMultiplierValue: 2, // Bounty: multiplier applied to clear-score only
  repairAmount: 50, // Repair: instant partial heal (kept partial to avoid snowballing)
  // Per-kind movement tuning for the ember enemy roster (see spawnShard /
  // updateShards). Multipliers scale a freshly-rolled asteroid speed.
  lanceSpeedMult: 1.85, // streaker: fastest, dead-straight, no spin
  cinderSpeedMult: 1.25, // split debris: fast, gently arcing
  cinderCurve: 0.6, // rad/s heading rotation that makes cinders arc
  drifterSpeedMult: 0.5, // plasma blob: slow looming body
  drifterTurn: 0.5, // rad/s CAPPED homing turn toward the player (dodgeable)
};

// Extensible power-up framework. To add a new power-up, append one entry here
// and nothing else needs to change: spawning, collection, expiry, collectible
// drawing, and the HUD all iterate this registry. Each entry declares an id,
// HUD label, color, and duration (seconds). Effects are expressed as optional
// declarative hooks the loop reads:
//   - shootCooldownMs: overrides the fire cooldown while active (smallest wins)
//   - shardSpeedFactor: multiplies hazard speed while active
//   - absorbsHit:       consumes the power-up to negate one incoming hit
//   - blocksAllHits:    negates every incoming hit for the full duration
//                       (timed invulnerability; not consumed per hit)
//   - strongShots:      upgrades fired bullets (bigger, faster, piercing) using
//                       the strongShot* tuning in GAME_CONFIG
//   - bulletCount/spreadAngle: fire N pellets in a fan (read in shoot())
//   - scoreMultiplier: multiplies clear-score while active (read by
//                      getScoreMultiplier at the two shard-clear sites)
//   - instant + instantEffect: applied immediately on pickup instead of being
//                      stamped with a timed expiry. activatePowerUp branches on
//                      `instant` and dispatches via applyInstantEffect(); instant
//                      defs never enter activePowerUps, so they have no timer.
// Each def also carries a `weight` for the weighted drop pool (higher = more
// common); strong/swingy effects are rarest so the calm early game stays calm.
// rapidFire is the canonical sample proving the framework end-to-end.
const POWERUPS = {
  rapidFire: {
    id: "rapidFire",
    label: "Rapid fire",
    color: "#FFD700",
    duration: GAME_CONFIG.powerUpDuration,
    shootCooldownMs: GAME_CONFIG.rapidFireCooldownMs,
    weight: 10,
  },
  slowShards: {
    id: "slowShards",
    label: "Slow shards",
    color: "#87CEEB",
    duration: GAME_CONFIG.powerUpDuration,
    shardSpeedFactor: GAME_CONFIG.shardSlowFactor,
    weight: 8,
  },
  shield: {
    id: "shield",
    label: "Shield",
    color: "#FF69B4",
    duration: GAME_CONFIG.powerUpDuration,
    blocksAllHits: true,
    weight: 6,
  },
  strongShots: {
    id: "strongShots",
    label: "Power shots",
    color: "#FFB020",
    duration: GAME_CONFIG.powerUpDuration,
    strongShots: true,
    weight: 5,
  },
  // Triple Tap: short offensive tempo buff — fires a 3-pellet fan. Composes with
  // strongShots/rapidFire and never raises the field cap, so it just clears the
  // already-capped field faster.
  spreadShot: {
    id: "spreadShot",
    label: "Triple tap",
    color: "#FFB020",
    duration: 6,
    bulletCount: 3,
    spreadAngle: GAME_CONFIG.spreadShotAngle,
    weight: 5,
  },
  // Bounty: economy buff — doubles score earned from CLEARING shards (never the
  // idle time term), so it has zero effect on hazards. Anti-runaway by design.
  bounty: {
    id: "bounty",
    label: "Bounty",
    color: "#FFD700",
    duration: GAME_CONFIG.powerUpDuration,
    scoreMultiplier: GAME_CONFIG.scoreMultiplierValue,
    weight: 4,
  },
  // Nova: INSTANT one-shot — clears the current field (awarding clear-score per
  // shard). Empties the array, so it works WITH the cap; the field respawns from
  // the calm baseline. Strongest button, so it is the rarest drop.
  bomb: {
    id: "bomb",
    label: "Nova",
    color: "#FF69B4",
    instant: true,
    instantEffect: "clearField",
    weight: 1,
  },
  // Repair: INSTANT partial heal. Wasted if already full-health (a natural
  // rarity throttle). Pure survivability economy, no hazard interaction.
  repair: {
    id: "repair",
    label: "Repair",
    color: "#87CEEB",
    instant: true,
    instantEffect: "repair",
    weight: 3,
  },
};
const POWERUP_IDS = Object.keys(POWERUPS);

function createActivePowerUpState() {
  return Object.fromEntries(
    POWERUP_IDS.map((id) => [id, { active: false, expiresAt: 0 }]),
  );
}

const ARROW_KEYS = new Set([
  "ArrowLeft",
  "ArrowRight",
  "ArrowUp",
  "ArrowDown",
]);
// WASD aliases keyed by event.code so they work regardless of keyboard layout.
const WASD_TO_ARROW = {
  KeyW: "ArrowUp",
  KeyA: "ArrowLeft",
  KeyS: "ArrowDown",
  KeyD: "ArrowRight",
};
const FIRE_KEYS = new Set(["Space"]);

const GAME_STATE = Object.freeze({
  INTRO: "intro",
  PLAYING: "playing",
  OVER: "over",
});

// Convert SVG path data once up front. Recreating Path2D instances per frame
// is unnecessary work and makes animation changes harder to reason about.
const facePathCache = Object.fromEntries(
  Object.entries(FACE_FRAMES).map(([frame, paths]) => [
    frame,
    paths.map((d) => new Path2D(d)),
  ]),
);

// Mutable simulation state lives in this module. For future features, prefer
// adding small state arrays/objects near these declarations over hidden globals.
const keys = new Set();
const pointerTarget = {
  active: false,
  x: 0,
  y: 0,
  pointerId: null,
};
const player = {
  x: 0,
  y: 0,
  vx: 0,
  vy: 0,
  size: 62,
  radius: 24,
  rotation: 0,
  health: GAME_CONFIG.maxHealth,
};

let width = 1;
let height = 1;
let dpr = 1;
let state = GAME_STATE.INTRO;
let elapsed = 0;
let score = 0;
let clearScore = 0;
let lastTime = performance.now();
let hasPlacedPlayer = false;
let spawnTimer = 0.4;
let trailTimer = 0;
let lastShotAt = -Infinity;
let lastShootButtonPointerAt = -Infinity;
let shotFrameUntil = 0;
let aimAngle = -Math.PI / 2;
let idleFrame = "default";
let idleFrameUntil = 0;
let nextIdleFrameAt = 1600;
let lives = GAME_CONFIG.startingLives;
let invulnerableTimer = 0;
let shards = [];
let bullets = [];
let trails = [];
let burstParticles = [];
let starField = [];
let powerUps = [];
let activePowerUps = createActivePowerUpState();
// Friendly helpers: a squirrel that hunts and smashes hazards, and a dolphin in
// a dress that occasionally drops by to shield the player and then flies off.
let squirrel = null;
let dolphin = null;
let dolphinTimer = 0;

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function lerp(start, end, amount) {
  return start + (end - start) * amount;
}

function randomBetween(min, max) {
  return min + Math.random() * (max - min);
}

function formatScore(value) {
  return String(Math.max(0, Math.floor(value))).padStart(6, "0");
}

const HIGH_SCORE_KEY = "whim-highscore";

function readHighScore() {
  try {
    return Math.max(0, Math.floor(Number(localStorage.getItem(HIGH_SCORE_KEY)) || 0));
  } catch (_e) {
    return 0;
  }
}

function writeHighScore(value) {
  try {
    localStorage.setItem(HIGH_SCORE_KEY, String(value));
  } catch (_e) {
    /* no-op */
  }
}

function formatTime(value) {
  const totalSeconds = Math.max(0, Math.floor(value));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function updateScorebar() {
  scoreEl.textContent = formatScore(score);
  roundTimeEl.textContent = formatTime(elapsed);
  livesEl.textContent = "♥".repeat(Math.max(0, lives)) || "—";
  const healthPercent = Math.max(0, Math.min(100, (player.health / GAME_CONFIG.maxHealth) * 100));
  healthBarFillEl.style.width = `${healthPercent}%`;
}

function showIntroOverlay() {
  state = GAME_STATE.INTRO;
  overlayEyebrowEl.textContent = "Ready";
  overlayTitleEl.textContent = "Whim Asteroids";
  overlayCopyEl.textContent =
    "Arrow keys or WASD or drag to move. Space shoots. On mobile, tap the glowing round button. Clear shards and keep the face moving.";
  restartButton.textContent = "Start";
  overlayEl.hidden = false;
  updateScorebar();
}

function showGameOverOverlay() {
  overlayEyebrowEl.textContent = "Round complete";
  overlayTitleEl.textContent = formatScore(score);
  overlayCopyEl.textContent =
    `Best: ${formatScore(readHighScore())}. Press Enter, Space, any arrow key, the round button, or Restart.`;
  restartButton.textContent = "Restart";
  overlayEl.hidden = false;
}

function resizeCanvas() {
  // Canvas dimensions are tracked in CSS pixels, then scaled into the backing
  // store with dpr. Prefer the measured game pane so embedded Whim workspaces
  // do not simulate outside the visible canvas; use viewport values only while
  // early layout is still reporting zero-sized elements.
  const rect = canvas.getBoundingClientRect();
  const shellRect = gameShell.getBoundingClientRect();
  const scorebarHeight = scorebar.getBoundingClientRect().height;
  const measuredWidth = shellRect.width || rect.width;
  const measuredHeight = shellRect.height || rect.height;
  const fallbackHeight = Math.max(1, window.innerHeight - scorebarHeight);
  const nextWidth = Math.max(1, measuredWidth || window.innerWidth);
  const nextHeight = Math.max(1, measuredHeight || fallbackHeight);
  dpr = Math.min(window.devicePixelRatio || 1, 2);
  width = nextWidth;
  height = nextHeight;
  canvas.width = Math.round(width * dpr);
  canvas.height = Math.round(height * dpr);
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

  player.size = clamp(
    width * 0.075,
    GAME_CONFIG.minPlayerSize,
    GAME_CONFIG.maxPlayerSize,
  );
  player.radius = player.size * 0.38;
  if (!hasPlacedPlayer || !Number.isFinite(player.x)) {
    centerPlayer();
  } else {
    player.x = wrap(player.x, width);
    player.y = wrap(player.y, height);
  }

  createStarField();
}

function centerPlayer() {
  player.x = width / 2;
  player.y = height / 2;
  player.vx = 0;
  player.vy = 0;
  player.rotation = 0;
  hasPlacedPlayer = true;
}

function createStarField() {
  const count = clamp(Math.floor((width * height) / 9000), 36, 112);
  starField = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    r: randomBetween(0.6, 1.7),
    alpha: randomBetween(0.12, 0.48),
    drift: randomBetween(0.05, 0.18),
  }));
}

function wrap(value, max) {
  // Keep wrapping slightly outside the visible area so large shards and the
  // face enter naturally instead of snapping at the exact edge.
  if (value < -80) return max + 80;
  if (value > max + 80) return -80;
  return value;
}

function resetRound() {
  state = GAME_STATE.PLAYING;
  elapsed = 0;
  score = 0;
  clearScore = 0;
  lives = GAME_CONFIG.startingLives;
  player.health = GAME_CONFIG.maxHealth;
  invulnerableTimer = 0;
  spawnTimer = 1.2;
  trailTimer = 0;
  lastShotAt = -Infinity;
  shotFrameUntil = 0;
  aimAngle = -Math.PI / 2;
  idleFrame = "default";
  idleFrameUntil = 0;
  nextIdleFrameAt = performance.now() + 1600;
  shards = [];
  bullets = [];
  trails = [];
  burstParticles = [];
  powerUps = [];
  activePowerUps = createActivePowerUpState();
  createSquirrel();
  dolphin = null;
  dolphinTimer = randomBetween(7, 11);
  centerPlayer();
  overlayEl.hidden = true;
  startMusic();
  updateScorebar();
}

function endRound() {
  state = GAME_STATE.OVER;
  stopMusic();
  gameOver();
  if (Math.floor(score) > readHighScore()) {
    writeHighScore(Math.floor(score));
  }
  showGameOverOverlay();
  createBurst(player.x, player.y, 26, COLORS.danger);
  updateScorebar();
}

function getDifficulty() {
  // One normalized scalar drives spawn pressure and shard speed. This keeps the
  // v1 difficulty curve easy to replace with levels or scripted waves later.
  return clamp(elapsed / GAME_CONFIG.maxDifficultyTime, 0, 1);
}

function createShardPoints(radius) {
  const sides = Math.floor(randomBetween(5, 8.999));
  const offset = Math.random() * Math.PI * 2;
  return Array.from({ length: sides }, (_, index) => {
    const angle = offset + (index / sides) * Math.PI * 2;
    const pointRadius = radius * randomBetween(0.58, 1.08);
    return {
      x: Math.cos(angle) * pointRadius,
      y: Math.sin(angle) * pointRadius,
    };
  });
}

// Difficulty-gated kind roll for a FRESH hazard. Early game (d~0) is almost
// pure asteroids — the calm opening the rebalance added is preserved. Faster
// streakers (lance) phase in first, then slow looming drifters as d climbs.
// `cinder` is intentionally absent here: it only ever exists as split debris,
// keeping its "broke off something" meaning intact.
function pickFreshShardKind() {
  const d = getDifficulty();
  // Weights ramp with difficulty. asteroid is always the floor.
  const weights = {
    asteroid: 1.0,
    lance: 0.15 + d * 0.6, // a little even early (reactive dodge spike), grows
    drifter: clamp((d - 0.35) / 0.65, 0, 1) * 0.5, // loomers only enter mid-game
  };
  const total = weights.asteroid + weights.lance + weights.drifter;
  let r = Math.random() * total;
  for (const kind of ["lance", "drifter"]) {
    r -= weights[kind];
    if (r < 0) return kind;
  }
  return "asteroid";
}

function spawnShard(x, y, vx, vy, kind = "asteroid") {
  // Hazards spawn just offscreen, then aim roughly toward the playfield center
  // with jitter. This feels intentional without requiring pathfinding.
  // When x/y/vx/vy are provided, this is a split from a destroyed shard (its
  // `kind` is passed by the caller). A fresh spawn (no coords) rolls its own
  // kind via difficulty-gated weighting so the early game stays asteroid-heavy.
  const isSpawned = x === undefined;

  let startX = x;
  let startY = y;
  let startVx = vx;
  let startVy = vy;
  let spawnKind = kind;

  const difficulty = getDifficulty();

  if (isSpawned) {
    spawnKind = pickFreshShardKind();
    const margin = 70;
    const edge = Math.floor(Math.random() * 4);

    if (edge === 0) {
      startX = randomBetween(0, width);
      startY = -margin;
    } else if (edge === 1) {
      startX = width + margin;
      startY = randomBetween(0, height);
    } else if (edge === 2) {
      startX = randomBetween(0, width);
      startY = height + margin;
    } else {
      startX = -margin;
      startY = randomBetween(0, height);
    }

    const distanceToPlayer = Math.hypot(startX - player.x, startY - player.y);
    if (distanceToPlayer < GAME_CONFIG.safeSpawnDistance) {
      startX += Math.sign(startX - player.x || 1) * GAME_CONFIG.safeSpawnDistance;
      startY += Math.sign(startY - player.y || 1) * GAME_CONFIG.safeSpawnDistance;
    }

    const targetX = randomBetween(width * 0.15, width * 0.85);
    const targetY = randomBetween(height * 0.15, height * 0.85);
    const heading = Math.atan2(targetY - startY, targetX - startX) + randomBetween(-0.42, 0.42);
    let speed = randomBetween(54, 108) + difficulty * 82;
    // Per-kind speed shaping: lances streak, drifters loom.
    if (spawnKind === "lance") speed *= GAME_CONFIG.lanceSpeedMult;
    else if (spawnKind === "drifter") speed *= GAME_CONFIG.drifterSpeedMult;

    startVx = Math.cos(heading) * speed;
    startVy = Math.sin(heading) * speed;
  }

  // Radius by kind: drifters are very big soft orbs, lances are small slivers,
  // cinders are small chips; asteroids keep the original range.
  let radius;
  if (spawnKind === "drifter") radius = randomBetween(40, 52 + difficulty * 10);
  else if (spawnKind === "lance") radius = randomBetween(9, 13);
  else if (spawnKind === "cinder") radius = randomBetween(7, 11);
  else radius = randomBetween(18, 38 + difficulty * 12);

  // Lances are thrown spears: no tumble. Cinders tumble fast. Others drift-spin.
  let spin;
  if (spawnKind === "lance") spin = 0;
  else if (spawnKind === "cinder") spin = randomBetween(-3.2, 3.2);
  else spin = randomBetween(-1.3, 1.3);

  shards.push({
    x: startX,
    y: startY,
    vx: startVx,
    vy: startVy,
    radius,
    points: createShardPoints(radius),
    angle: spawnKind === "lance" ? Math.atan2(startVy, startVx) : Math.random() * Math.PI * 2,
    spin,
    pulse: Math.random() * Math.PI * 2,
    kind: spawnKind,
    // cinder steering: +1/-1 arc direction so debris curves instead of going straight.
    curveDir: spawnKind === "cinder" ? (Math.random() < 0.5 ? -1 : 1) : 0,
  });
}

function maxShardCount() {
  // Start with a calm field (just a few hazards) and grow with difficulty.
  // Screen-area headroom is folded into the LATE-game cap, not the opening, so
  // large screens aren't slammed at t=0.
  const areaBonus = clamp((width * height) / 150000, 0, 5);
  return Math.floor(lerp(3, 9 + areaBonus, getDifficulty()));
}

function createBurst(x, y, count, color) {
  for (let i = 0; i < count; i += 1) {
    const angle = Math.random() * Math.PI * 2;
    const speed = randomBetween(70, 250);
    burstParticles.push({
      x,
      y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      size: randomBetween(3, 8),
      age: 0,
      life: randomBetween(0.35, 0.85),
      color,
    });
  }
}

function getFireAngle() {
  if (pointerTarget.active) {
    const dx = pointerTarget.x - player.x;
    const dy = pointerTarget.y - player.y;
    if (Math.hypot(dx, dy) > player.radius) {
      return Math.atan2(dy, dx);
    }
  }

  const speed = Math.hypot(player.vx, player.vy);
  if (speed > 24) return Math.atan2(player.vy, player.vx);
  return aimAngle;
}

function shoot(now = performance.now()) {
  if (state !== GAME_STATE.PLAYING) return;

  if (now - lastShotAt < getShootCooldownMs()) return;

  const angle = getFireAngle();
  const muzzleDistance = player.radius * 1.25;
  const muzzleX = player.x + Math.cos(angle) * muzzleDistance;
  const muzzleY = player.y + Math.sin(angle) * muzzleDistance;

  aimAngle = angle;
  lastShotAt = now;
  shotFrameUntil = now + 180;
  const strong = getStrongShotsActive();
  const bulletSpeed = strong ? GAME_CONFIG.strongShotSpeed : GAME_CONFIG.bulletSpeed;
  // Fire a fan of `n` pellets centered on `angle`; each pellet keeps the full
  // strong/speed/radius logic so spreadShot composes with strongShots.
  const n = getBulletCount();
  const spread = getBulletSpread();
  for (let i = 0; i < n; i += 1) {
    const pelletAngle = angle + (i - (n - 1) / 2) * spread;
    bullets.push({
      x: muzzleX,
      y: muzzleY,
      vx: Math.cos(pelletAngle) * bulletSpeed + player.vx * 0.18,
      vy: Math.sin(pelletAngle) * bulletSpeed + player.vy * 0.18,
      age: 0,
      life: GAME_CONFIG.bulletLife,
      radius: strong ? GAME_CONFIG.strongShotRadius : GAME_CONFIG.bulletRadius,
      pierce: strong ? GAME_CONFIG.strongShotPierce : 0,
      strong,
    });
  }
  playShoot();

  player.vx -= Math.cos(angle) * 34;
  player.vy -= Math.sin(angle) * 34;
  createBurst(muzzleX, muzzleY, 6, COLORS.face);
}

function getInputVector() {
  let x = 0;
  let y = 0;
  if (keys.has("ArrowLeft")) x -= 1;
  if (keys.has("ArrowRight")) x += 1;
  if (keys.has("ArrowUp")) y -= 1;
  if (keys.has("ArrowDown")) y += 1;

  if (pointerTarget.active) {
    const dx = pointerTarget.x - player.x;
    const dy = pointerTarget.y - player.y;
    const distance = Math.hypot(dx, dy);
    if (distance > player.radius * 0.35) {
      const pull = clamp(distance / 140, 0, 1);
      x += (dx / distance) * pull;
      y += (dy / distance) * pull;
    }
  }

  const magnitude = Math.hypot(x, y);
  if (magnitude > 1) {
    x /= magnitude;
    y /= magnitude;
    return { x, y, magnitude: 1 };
  }

  return { x, y, magnitude };
}

function updatePlayer(dt, now) {
  // Movement is acceleration-based rather than direct position control. That
  // gives the arrow keys a soft Asteroids feel while staying approachable.
  const input = getInputVector();
  if (pointerTarget.active) {
    aimAngle = getFireAngle();
  } else if (input.magnitude > 0.05) {
    aimAngle = Math.atan2(input.y, input.x);
  }
  const damping = input.magnitude > 0
    ? GAME_CONFIG.activeDamping
    : GAME_CONFIG.idleDamping;

  player.vx += input.x * GAME_CONFIG.acceleration * dt;
  player.vy += input.y * GAME_CONFIG.acceleration * dt;

  const speed = Math.hypot(player.vx, player.vy);
  if (speed > GAME_CONFIG.maxSpeed) {
    const scale = GAME_CONFIG.maxSpeed / speed;
    player.vx *= scale;
    player.vy *= scale;
  }

  const frameDamping = damping ** (dt * 60);
  player.vx *= frameDamping;
  player.vy *= frameDamping;

  player.x = wrap(player.x + player.vx * dt, width);
  player.y = wrap(player.y + player.vy * dt, height);
  player.rotation = lerp(
    player.rotation,
    clamp(player.vx / GAME_CONFIG.maxSpeed, -1, 1) * 0.22,
    1 - 0.001 ** dt,
  );

  trailTimer -= dt;
  if (speed > 70 && trailTimer <= 0) {
    trails.push({
      x: player.x,
      y: player.y,
      size: player.size * randomBetween(0.74, 0.9),
      rotation: player.rotation,
      frame: getFaceFrame(now, true),
      age: 0,
      life: 0.42,
    });
    trailTimer = 0.055;
  }
}

function getFaceFrame(now, moving) {
  // Animation policy for the Whim face: motion frames while moving, occasional
  // blink/smile while idle, smile during shots, blink on game over.
  if (state === GAME_STATE.OVER) return "blink";
  if (state === GAME_STATE.INTRO) return "default";
  // He's always mad — never smile. Shots and movement just agitate the glyph.
  if (now < shotFrameUntil) return "motion2";
  if (moving) {
    return MOTION_SEQUENCE[Math.floor(now / 96) % MOTION_SEQUENCE.length];
  }

  if (now >= nextIdleFrameAt && now >= idleFrameUntil) {
    idleFrame = "blink";
    idleFrameUntil = now + 260;
    nextIdleFrameAt = now + randomBetween(2600, 7600);
  }

  if (now < idleFrameUntil) return idleFrame;
  return "default";
}

function updateShards(dt) {
  // Shard lifecycle, spawn pacing, and collision detection are intentionally
  // coupled for v1. Split this only when hazards gain distinct behaviors.
  const difficulty = getDifficulty();
  spawnTimer -= dt;
  const interval = lerp(
    GAME_CONFIG.spawnIntervalStart,
    GAME_CONFIG.spawnIntervalEnd,
    difficulty,
  );

  if (spawnTimer <= 0 && shards.length < maxShardCount()) {
    spawnShard();
    spawnTimer = interval * randomBetween(0.72, 1.2);
  }

  const slowFactor = getShardSpeedFactor();

  for (const shard of shards) {
    // Per-kind steering BEFORE the generic integrate. Anti-runaway: nothing here
    // spawns shards, and homing is capped-turn + fixed-speed so it stays dodgeable.
    if (shard.kind === "cinder") {
      // Debris arcs: rotate velocity a little each frame so it visibly curves
      // instead of flying dead-straight like an asteroid.
      const c = GAME_CONFIG.cinderCurve * shard.curveDir * dt;
      const cos = Math.cos(c);
      const sin = Math.sin(c);
      const nvx = shard.vx * cos - shard.vy * sin;
      const nvy = shard.vx * sin + shard.vy * cos;
      shard.vx = nvx;
      shard.vy = nvy;
    } else if (shard.kind === "drifter") {
      // Mild player-homing with a CAPPED turn rate: the blob looms toward you but
      // a player at speed can always escape. Speed is preserved (slow body).
      const desired = Math.atan2(player.y - shard.y, player.x - shard.x);
      const cur = Math.atan2(shard.vy, shard.vx);
      let da = desired - cur;
      while (da > Math.PI) da -= Math.PI * 2;
      while (da < -Math.PI) da += Math.PI * 2;
      const maxTurn = GAME_CONFIG.drifterTurn * dt;
      const heading = cur + clamp(da, -maxTurn, maxTurn);
      const spd = Math.hypot(shard.vx, shard.vy);
      shard.vx = Math.cos(heading) * spd;
      shard.vy = Math.sin(heading) * spd;
    }

    shard.x = wrap(shard.x + shard.vx * dt * slowFactor, width);
    shard.y = wrap(shard.y + shard.vy * dt * slowFactor, height);
    // Lances are streakers: lock facing to travel direction, no tumble.
    if (shard.kind === "lance") shard.angle = Math.atan2(shard.vy, shard.vx);
    else shard.angle += shard.spin * dt;
    shard.pulse += dt * 2;
  }

  resolveShardCollisions();

  if (state !== GAME_STATE.PLAYING || elapsed < 0.65 || invulnerableTimer > 0) {
    return;
  }

  for (const shard of shards) {
    const hitRadius = player.radius + shard.radius * 0.72;
    if (Math.hypot(player.x - shard.x, player.y - shard.y) <= hitRadius) {
      if (absorbHitWithPowerUp()) {
        createBurst(player.x, player.y, 16, COLORS.face);
      } else {
        takeDamage();
      }
      break;
    }
  }
}

// Shard-on-shard elastic collisions (from main, PR #44): larger rocks shove
// smaller ones along the contact normal so the field jostles instead of
// overlapping silently.
function resolveShardCollisions() {
  // Treat each shard's mass as proportional to its area so larger rocks shove
  // smaller ones convincingly. This is a simple equal-restitution elastic
  // bounce along the contact normal — good enough for the arcade feel.
  for (let i = 0; i < shards.length; i++) {
    const a = shards[i];
    for (let j = i + 1; j < shards.length; j++) {
      const b = shards[j];
      const dx = b.x - a.x;
      const dy = b.y - a.y;
      const minDist = a.radius + b.radius;
      const distSq = dx * dx + dy * dy;
      if (distSq >= minDist * minDist || distSq === 0) {
        continue;
      }

      const dist = Math.sqrt(distSq);
      const nx = dx / dist;
      const ny = dy / dist;

      // Push the pair apart so they stop overlapping.
      const overlap = minDist - dist;
      const massA = a.radius * a.radius;
      const massB = b.radius * b.radius;
      const totalMass = massA + massB;
      a.x -= nx * overlap * (massB / totalMass);
      a.y -= ny * overlap * (massB / totalMass);
      b.x += nx * overlap * (massA / totalMass);
      b.y += ny * overlap * (massA / totalMass);

      // Only exchange momentum if they are approaching each other.
      const relVel = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny;
      if (relVel >= 0) {
        continue;
      }

      const impulse = (2 * relVel) / totalMass;
      a.vx += impulse * massB * nx;
      a.vy += impulse * massB * ny;
      b.vx -= impulse * massA * nx;
      b.vy -= impulse * massA * ny;
    }
  }
}

function takeDamage() {
  hit();
  player.health = Math.max(0, player.health - GAME_CONFIG.healthDamage);
  createBurst(player.x, player.y, 14, COLORS.danger);
  invulnerableTimer = GAME_CONFIG.hitInvulnerabilityTime;

  if (player.health <= 0) {
    loseLife();
  } else {
    updateScorebar();
  }
}

function loseLife() {
  lives -= 1;
  if (lives <= 0) {
    endRound();
    return;
  }

  createBurst(player.x, player.y, 22, COLORS.danger);
  centerPlayer();
  player.health = GAME_CONFIG.maxHealth;
  invulnerableTimer = GAME_CONFIG.hitInvulnerabilityTime;
  updateScorebar();
}

// Weighted drop selection: each def's `weight` (default 1) shapes WHICH power-up
// drops, not HOW OFTEN — drop frequency stays powerUpSpawnChance. New POWERUPS
// entries auto-enroll in the pool.
function pickPowerUpId() {
  const total = POWERUP_IDS.reduce((sum, id) => sum + (POWERUPS[id].weight ?? 1), 0);
  let r = Math.random() * total;
  for (const id of POWERUP_IDS) {
    r -= POWERUPS[id].weight ?? 1;
    if (r < 0) return id;
  }
  return POWERUP_IDS[POWERUP_IDS.length - 1];
}

function spawnPowerUp(x, y) {
  const type = pickPowerUpId();

  powerUps.push({
    x,
    y,
    type,
    vx: randomBetween(-60, 60),
    vy: randomBetween(-60, 60),
    radius: 10,
    age: 0,
    collected: false,
  });
}

// Apply an INSTANT power-up's effect immediately on pickup. Instant defs carry
// `instant: true` and an `instantEffect` key dispatched here; they never enter
// activePowerUps, so they have no timer and no HUD ring.
function applyInstantEffect(def) {
  switch (def.instantEffect) {
    case "clearField": {
      // Award clear-score per shard (honoring Bounty), burst each, then empty
      // the field. Emptying the array works WITH the cap — the field simply
      // respawns from the calm baseline rather than spawning anything new.
      for (const shard of shards) {
        clearScore += GAME_CONFIG.shardClearScore * getScoreMultiplier();
        createBurst(shard.x, shard.y, 10, def.color);
      }
      shards = [];
      break;
    }
    case "repair": {
      player.health = Math.min(
        GAME_CONFIG.maxHealth,
        player.health + GAME_CONFIG.repairAmount,
      );
      break;
    }
    default:
      break;
  }
}

function activatePowerUp(type, now) {
  const def = POWERUPS[type];
  if (!def) return;
  powerup();
  // Instant power-ups apply once and never stamp a timed expiry.
  if (def.instant) {
    applyInstantEffect(def);
    return;
  }
  activePowerUps[type].active = true;
  // Durations live in seconds (GAME_CONFIG convention); the loop clock is in
  // milliseconds, so convert here when stamping the expiry.
  activePowerUps[type].expiresAt = now + def.duration * 1000;
}

function isPowerUpActive(id) {
  return activePowerUps[id]?.active === true;
}

// Fire cooldown is the smallest override contributed by an active power-up, or
// the base cooldown when none apply.
function getShootCooldownMs() {
  let cooldown = GAME_CONFIG.shootCooldownMs;
  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    if (def.shootCooldownMs !== undefined && isPowerUpActive(id)) {
      cooldown = Math.min(cooldown, def.shootCooldownMs);
    }
  }
  return cooldown;
}

// Hazard speed multiplier, compounding every active power-up that slows shards.
function getShardSpeedFactor() {
  let factor = 1;
  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    if (def.shardSpeedFactor !== undefined && isPowerUpActive(id)) {
      factor *= def.shardSpeedFactor;
    }
  }
  return factor;
}

// True while any active power-up upgrades the player's missiles. Read by shoot()
// when stamping a new bullet, so a bullet keeps its strength for its whole flight
// even if the power-up expires mid-air.
function getStrongShotsActive() {
  for (const id of POWERUP_IDS) {
    if (POWERUPS[id].strongShots && isPowerUpActive(id)) {
      return true;
    }
  }
  return false;
}

// Number of pellets to fire (max bulletCount among active defs; default 1) and
// the matching spread angle. Read by shoot() to fan out the volley.
function getBulletCount() {
  let count = 1;
  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    if (def.bulletCount !== undefined && isPowerUpActive(id)) {
      count = Math.max(count, def.bulletCount);
    }
  }
  return count;
}

function getBulletSpread() {
  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    if (def.bulletCount !== undefined && isPowerUpActive(id)) {
      return def.spreadAngle ?? 0;
    }
  }
  return 0;
}

// Clear-score multiplier, compounding every active scoreMultiplier power-up.
// Returns >= 1 so it is safe to apply unconditionally at the clear-score sites.
function getScoreMultiplier() {
  let multiplier = 1;
  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    if (def.scoreMultiplier !== undefined && isPowerUpActive(id)) {
      multiplier *= def.scoreMultiplier;
    }
  }
  return multiplier;
}

// Negate an incoming hit using an active defensive power-up; returns true if the
// hit was absorbed so the caller can skip damage. Timed blockers (blocksAllHits)
// stay active for their full duration; one-shot absorbers (absorbsHit) are
// consumed. Blockers win first so they aren't needlessly spent.
function absorbHitWithPowerUp() {
  for (const id of POWERUP_IDS) {
    if (POWERUPS[id].blocksAllHits && isPowerUpActive(id)) {
      return true;
    }
  }
  for (const id of POWERUP_IDS) {
    if (POWERUPS[id].absorbsHit && isPowerUpActive(id)) {
      activePowerUps[id].active = false;
      return true;
    }
  }
  return false;
}

function splitShard(shard) {
  // Split a shard into smaller pieces when destroyed
  if (shard.radius < GAME_CONFIG.shardSplitThreshold) {
    return;
  }

  const newRadius = shard.radius * 0.62;
  const speedBoost = GAME_CONFIG.shardSplitSpeedBoost;
  const baseSpeed = Math.hypot(shard.vx, shard.vy);
  const boostedSpeed = baseSpeed * speedBoost;

  for (let i = 0; i < GAME_CONFIG.shardSplitCount; i += 1) {
    // Splits bypass the spawn pacing, so honor the field cap here too. The
    // parent shard is still counted at this point, so at the cap a destroyed
    // shard yields zero pieces (net clear) instead of multiplying forever.
    if (shards.length >= maxShardCount()) break;

    const angle = (i / GAME_CONFIG.shardSplitCount) * Math.PI * 2 + randomBetween(-0.3, 0.3);
    const vx = Math.cos(angle) * boostedSpeed;
    const vy = Math.sin(angle) * boostedSpeed;

    // Broken-off pieces are glowing ember cinders: small, fast, gently arcing,
    // and below the split threshold so they can never re-split (hard runaway floor).
    spawnShard(shard.x, shard.y, vx, vy, "cinder");
  }

  if (Math.random() < GAME_CONFIG.powerUpSpawnChance) {
    spawnPowerUp(shard.x, shard.y);
  }
}

function updateBullets(dt) {
  for (const bullet of bullets) {
    bullet.age += dt;
    bullet.x = wrap(bullet.x + bullet.vx * dt, width);
    bullet.y = wrap(bullet.y + bullet.vy * dt, height);
  }

  bullets = bullets.filter((bullet) => bullet.age < bullet.life);

  for (let bulletIndex = bullets.length - 1; bulletIndex >= 0; bulletIndex -= 1) {
    const bullet = bullets[bulletIndex];
    for (let shardIndex = shards.length - 1; shardIndex >= 0; shardIndex -= 1) {
      const shard = shards[shardIndex];
      const hitRadius = bullet.radius + shard.radius * 0.78;
      if (Math.hypot(bullet.x - shard.x, bullet.y - shard.y) > hitRadius) {
        continue;
      }

      splitShard(shard);
      shards.splice(shardIndex, 1);
      clearScore += GAME_CONFIG.shardClearScore * getScoreMultiplier();
      explode();
      createBurst(shard.x, shard.y, 12, bullet.strong ? POWERUPS.strongShots.color : COLORS.face);

      // Power shots punch through up to `pierce` extra hazards before being
      // consumed; base shots stop on the first hit.
      if (bullet.pierce > 0) {
        bullet.pierce -= 1;
        continue;
      }
      bullets.splice(bulletIndex, 1);
      break;
    }
  }
}

function updatePowerUps(dt, now) {
  for (const powerUp of powerUps) {
    powerUp.x = wrap(powerUp.x + powerUp.vx * dt, width);
    powerUp.y = wrap(powerUp.y + powerUp.vy * dt, height);
    powerUp.vy += 80 * dt;
  }

  if (state !== GAME_STATE.PLAYING) return;

  for (let i = powerUps.length - 1; i >= 0; i -= 1) {
    const powerUp = powerUps[i];
    const hitRadius = player.radius + powerUp.radius;

    if (Math.hypot(player.x - powerUp.x, player.y - powerUp.y) <= hitRadius) {
      activatePowerUp(powerUp.type, now);
      createBurst(powerUp.x, powerUp.y, 8, COLORS.face);
      powerUps.splice(i, 1);
    }
  }
}

function updateActivePowerUps(now) {
  for (const [type, power] of Object.entries(activePowerUps)) {
    if (power.active && now >= power.expiresAt) {
      power.active = false;
    }
  }
}

function createSquirrel() {
  squirrel = {
    x: width * 0.5,
    y: height * 0.65,
    vx: 0,
    vy: 0,
    radius: 16,
    facing: 1,
    runPhase: 0,
    wander: Math.random() * Math.PI * 2,
  };
}

// The squirrel sprints toward the nearest hazard and smashes it on contact,
// breaking it exactly as a bullet would (so asteroids still shed ember cinders).
function updateSquirrel(dt) {
  if (!squirrel || state !== GAME_STATE.PLAYING) return;

  let target = null;
  let best = Infinity;
  for (const shard of shards) {
    const d = Math.hypot(shard.x - squirrel.x, shard.y - squirrel.y);
    if (d < best) {
      best = d;
      target = shard;
    }
  }

  let ax;
  let ay;
  if (target) {
    const ang = Math.atan2(target.y - squirrel.y, target.x - squirrel.x);
    ax = Math.cos(ang);
    ay = Math.sin(ang);
  } else {
    // No hazards: scamper around on a slowly drifting heading.
    squirrel.wander += randomBetween(-0.12, 0.12);
    ax = Math.cos(squirrel.wander);
    ay = Math.sin(squirrel.wander);
  }

  const accel = 1100;
  squirrel.vx += ax * accel * dt;
  squirrel.vy += ay * accel * dt;
  const maxSpeed = target ? 460 : 240;
  const sp = Math.hypot(squirrel.vx, squirrel.vy);
  if (sp > maxSpeed) {
    squirrel.vx *= maxSpeed / sp;
    squirrel.vy *= maxSpeed / sp;
  }
  squirrel.vx *= 0.9 ** (dt * 60);
  squirrel.vy *= 0.9 ** (dt * 60);

  squirrel.x = wrap(squirrel.x + squirrel.vx * dt, width);
  squirrel.y = wrap(squirrel.y + squirrel.vy * dt, height);
  if (Math.abs(squirrel.vx) > 10) squirrel.facing = Math.sign(squirrel.vx);
  squirrel.runPhase += dt * (6 + sp * 0.03);

  for (let i = shards.length - 1; i >= 0; i -= 1) {
    const shard = shards[i];
    const hit = squirrel.radius + shard.radius * 0.7;
    if (Math.hypot(shard.x - squirrel.x, shard.y - squirrel.y) <= hit) {
      splitShard(shard);
      shards.splice(i, 1);
      clearScore += GAME_CONFIG.shardClearScore * getScoreMultiplier();
      explode();
      createBurst(shard.x, shard.y, 12, COLORS.squirrel);
    }
  }
}

function drawSquirrel() {
  if (!squirrel) return;
  const r = squirrel.radius;
  const bob = Math.sin(squirrel.runPhase * 2) * 2;
  const legSwing = Math.sin(squirrel.runPhase * 2) * r * 0.4;

  ctx.save();
  ctx.translate(squirrel.x, squirrel.y + bob);
  ctx.scale(squirrel.facing, 1);

  // Bushy tail behind the body.
  ctx.fillStyle = "#7a4a22";
  ctx.beginPath();
  ctx.moveTo(-r * 0.55, r * 0.25);
  ctx.quadraticCurveTo(-r * 1.7, -r * 0.1, -r * 1.05, -r * 1.25);
  ctx.quadraticCurveTo(-r * 0.65, -r * 0.5, -r * 0.15, -r * 0.2);
  ctx.closePath();
  ctx.fill();

  // Running legs.
  ctx.strokeStyle = "#7a4a22";
  ctx.lineWidth = r * 0.16;
  ctx.lineCap = "round";
  ctx.beginPath();
  ctx.moveTo(r * 0.22, r * 0.35);
  ctx.lineTo(r * 0.22 + legSwing, r * 0.75);
  ctx.moveTo(-r * 0.18, r * 0.35);
  ctx.lineTo(-r * 0.18 - legSwing, r * 0.75);
  ctx.stroke();

  // Body + belly.
  ctx.fillStyle = "#a8703a";
  ctx.beginPath();
  ctx.ellipse(0, 0, r * 0.72, r * 0.56, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = "#e7c79a";
  ctx.beginPath();
  ctx.ellipse(r * 0.16, r * 0.14, r * 0.4, r * 0.32, 0, 0, Math.PI * 2);
  ctx.fill();

  // Head, ear, eye.
  ctx.fillStyle = "#a8703a";
  ctx.beginPath();
  ctx.arc(r * 0.66, -r * 0.34, r * 0.42, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.moveTo(r * 0.58, -r * 0.7);
  ctx.lineTo(r * 0.78, -r * 1.08);
  ctx.lineTo(r * 0.98, -r * 0.62);
  ctx.closePath();
  ctx.fill();
  ctx.fillStyle = "#1a1008";
  ctx.beginPath();
  ctx.arc(r * 0.82, -r * 0.4, r * 0.09, 0, Math.PI * 2);
  ctx.fill();

  ctx.restore();
}

function maybeSpawnDolphin(dt) {
  if (state !== GAME_STATE.PLAYING || dolphin) return;
  dolphinTimer -= dt;
  if (dolphinTimer <= 0) {
    spawnDolphin();
  }
}

function spawnDolphin() {
  const edge = Math.floor(Math.random() * 4);
  let x;
  let y;
  if (edge === 0) {
    x = randomBetween(0, width);
    y = -90;
  } else if (edge === 1) {
    x = width + 90;
    y = randomBetween(0, height);
  } else if (edge === 2) {
    x = randomBetween(0, width);
    y = height + 90;
  } else {
    x = -90;
    y = randomBetween(0, height);
  }
  dolphin = {
    x,
    y,
    radius: 26,
    phase: "arrive",
    heading: 0,
    giftTimer: 0,
    gifted: false,
    bob: Math.random() * Math.PI * 2,
  };
}

// Dolphin lifecycle: swim in toward the player, hover briefly while granting a
// shield, then bolt off the nearest edge and despawn until the next visit.
function updateDolphin(dt, now) {
  if (!dolphin) return;
  dolphin.bob += dt * 4;
  const speed = 280;

  if (dolphin.phase === "arrive") {
    dolphin.heading = Math.atan2(player.y - dolphin.y, player.x - dolphin.x);
    dolphin.x += Math.cos(dolphin.heading) * speed * dt;
    dolphin.y += Math.sin(dolphin.heading) * speed * dt;
    if (Math.hypot(player.x - dolphin.x, player.y - dolphin.y) < player.radius + 64) {
      dolphin.phase = "gift";
      dolphin.giftTimer = 0.9;
    }
  } else if (dolphin.phase === "gift") {
    if (!dolphin.gifted) {
      activatePowerUp("shield", now);
      createBurst(player.x, player.y, 18, POWERUPS.shield.color);
      dolphin.gifted = true;
    }
    dolphin.giftTimer -= dt;
    if (dolphin.giftTimer <= 0) {
      dolphin.phase = "leave";
      const toLeft = dolphin.x;
      const toRight = width - dolphin.x;
      const toTop = dolphin.y;
      const toBot = height - dolphin.y;
      const min = Math.min(toLeft, toRight, toTop, toBot);
      if (min === toLeft) dolphin.heading = Math.PI;
      else if (min === toRight) dolphin.heading = 0;
      else if (min === toTop) dolphin.heading = -Math.PI / 2;
      else dolphin.heading = Math.PI / 2;
    }
  } else {
    const leaveSpeed = speed * 1.6;
    dolphin.x += Math.cos(dolphin.heading) * leaveSpeed * dt;
    dolphin.y += Math.sin(dolphin.heading) * leaveSpeed * dt;
    if (
      dolphin.x < -140 || dolphin.x > width + 140 ||
      dolphin.y < -140 || dolphin.y > height + 140
    ) {
      dolphin = null;
      dolphinTimer = randomBetween(12, 20);
    }
  }
}

function drawDolphin() {
  if (!dolphin) return;
  const r = dolphin.radius;
  const faceDir = Math.cos(dolphin.heading) >= 0 ? 1 : -1;
  const bobY = Math.sin(dolphin.bob) * 4;

  ctx.save();
  ctx.translate(dolphin.x, dolphin.y + bobY);
  ctx.scale(faceDir, 1);
  ctx.rotate(Math.sin(dolphin.bob) * 0.06);

  // Body.
  ctx.fillStyle = "#5c7a99";
  ctx.beginPath();
  ctx.moveTo(-r * 1.3, 0);
  ctx.quadraticCurveTo(-r * 0.3, -r * 0.95, r * 1.1, -r * 0.18);
  ctx.quadraticCurveTo(r * 1.55, 0, r * 1.1, r * 0.12);
  ctx.quadraticCurveTo(-r * 0.3, r * 0.62, -r * 1.3, 0);
  ctx.closePath();
  ctx.fill();

  // Lighter belly.
  ctx.fillStyle = "#9fb8cf";
  ctx.beginPath();
  ctx.moveTo(r * 1.0, r * 0.06);
  ctx.quadraticCurveTo(-r * 0.2, r * 0.52, -r * 1.0, r * 0.05);
  ctx.quadraticCurveTo(-r * 0.2, r * 0.22, r * 1.0, r * 0.06);
  ctx.fill();

  // Tail fluke.
  ctx.fillStyle = "#5c7a99";
  ctx.beginPath();
  ctx.moveTo(-r * 1.1, 0);
  ctx.lineTo(-r * 1.75, -r * 0.5);
  ctx.lineTo(-r * 1.5, 0);
  ctx.lineTo(-r * 1.75, r * 0.5);
  ctx.closePath();
  ctx.fill();

  // Dorsal fin.
  ctx.beginPath();
  ctx.moveTo(-r * 0.1, -r * 0.58);
  ctx.lineTo(r * 0.22, -r * 1.18);
  ctx.lineTo(r * 0.46, -r * 0.5);
  ctx.closePath();
  ctx.fill();

  // Eye.
  ctx.fillStyle = "#10202e";
  ctx.beginPath();
  ctx.arc(r * 0.8, -r * 0.22, r * 0.08, 0, Math.PI * 2);
  ctx.fill();

  // The dress: a flared pink frock around the midsection.
  ctx.fillStyle = "#ff5db1";
  ctx.beginPath();
  ctx.moveTo(-r * 0.18, -r * 0.52);
  ctx.lineTo(r * 0.36, -r * 0.42);
  ctx.lineTo(r * 0.78, r * 0.72);
  ctx.lineTo(-r * 0.62, r * 0.58);
  ctx.closePath();
  ctx.fill();
  ctx.strokeStyle = "#ffd1e8";
  ctx.lineWidth = r * 0.1;
  ctx.lineJoin = "round";
  ctx.stroke();

  // Polka dots on the dress.
  ctx.fillStyle = "#ffd1e8";
  for (const [dx, dy] of [
    [r * 0.02, r * 0.0],
    [r * 0.34, r * 0.28],
    [-r * 0.18, r * 0.34],
    [r * 0.18, -r * 0.18],
  ]) {
    ctx.beginPath();
    ctx.arc(dx, dy, r * 0.07, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.restore();
}

function updateEffects(dt) {
  for (const trail of trails) {
    trail.age += dt;
  }
  trails = trails.filter((trail) => trail.age < trail.life);

  for (const particle of burstParticles) {
    particle.age += dt;
    particle.vx *= 0.985 ** (dt * 60);
    particle.vy *= 0.985 ** (dt * 60);
    particle.x += particle.vx * dt;
    particle.y += particle.vy * dt;
  }
  burstParticles = burstParticles.filter((particle) => particle.age < particle.life);
}

function update(dt, now) {
  if (state === GAME_STATE.PLAYING) {
    elapsed += dt;
    invulnerableTimer = Math.max(0, invulnerableTimer - dt);
    score = elapsed * GAME_CONFIG.scoreRate + clearScore;
    updatePlayer(dt, now);
    updateShards(dt);
    updateBullets(dt);
    updatePowerUps(dt, now);
    updateSquirrel(dt);
    maybeSpawnDolphin(dt);
    updateDolphin(dt, now);
  }

  updateActivePowerUps(now);
  updateEffects(dt);
  updateScorebar();
}

function drawBackground(now) {
  // The game scene is painted entirely on canvas; CSS owns only the header and
  // overlay chrome. Keep decorative canvas work cheap and low contrast.
  // Vertical plum-to-crimson void sets the mood for the whole red world.
  const sky = ctx.createLinearGradient(0, 0, 0, height);
  sky.addColorStop(0, COLORS.backgroundTop);
  sky.addColorStop(0.55, COLORS.background);
  sky.addColorStop(1, COLORS.backgroundBottom);
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, width, height);

  // A faint warm ember glow rising from the lower-center keeps the gradient from
  // feeling flat and draws the eye toward the action.
  const glow = ctx.createRadialGradient(
    width / 2, height * 0.92, 0,
    width / 2, height * 0.92, Math.max(width, height) * 0.85,
  );
  glow.addColorStop(0, "rgba(255, 77, 77, 0.10)");
  glow.addColorStop(1, "rgba(255, 77, 77, 0)");
  ctx.fillStyle = glow;
  ctx.fillRect(0, 0, width, height);

  const grid = 40;
  const offset = (now * 0.012) % grid;
  ctx.lineWidth = 1;
  ctx.strokeStyle = "rgba(255, 77, 77, 0.03)";
  ctx.beginPath();
  for (let x = -grid + offset; x <= width + grid; x += grid) {
    ctx.moveTo(x, 0);
    ctx.lineTo(x, height);
  }
  for (let y = -grid + offset; y <= height + grid; y += grid) {
    ctx.moveTo(0, y);
    ctx.lineTo(width, y);
  }
  ctx.stroke();

  for (const star of starField) {
    const twinkle = 0.65 + Math.sin(now * star.drift + star.x) * 0.35;
    ctx.globalAlpha = star.alpha * twinkle;
    ctx.fillStyle = COLORS.star;
    ctx.beginPath();
    ctx.arc(star.x, star.y, star.r, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.globalAlpha = 1;
}

// `cinder` — molten chip flung off a broken body. A small lumpy ember blob
// (reuses the jagged `points`) glowing hot in the center, trailing tiny sparks
// opposite its velocity so it reads as "still-burning debris."
function drawCinder(shard) {
  const r = shard.radius;
  // Trailing motes behind the cinder (in world space, opposite velocity).
  const sp = Math.hypot(shard.vx, shard.vy) || 1;
  const ux = -shard.vx / sp;
  const uy = -shard.vy / sp;
  ctx.save();
  ctx.fillStyle = COLORS.moteHot;
  for (let i = 1; i <= 3; i += 1) {
    const t = i / 3;
    ctx.globalAlpha = 0.5 * (1 - t);
    ctx.beginPath();
    ctx.arc(shard.x + ux * r * (1 + i), shard.y + uy * r * (1 + i), r * 0.3 * (1 - t * 0.4), 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();

  ctx.save();
  ctx.translate(shard.x, shard.y);
  ctx.rotate(shard.angle);
  const grad = ctx.createRadialGradient(0, 0, r * 0.1, 0, 0, r);
  grad.addColorStop(0, COLORS.emberCore);
  grad.addColorStop(1, COLORS.cinderEdge);
  ctx.fillStyle = grad;
  ctx.strokeStyle = COLORS.cinderEdge;
  ctx.lineWidth = 1.2;
  ctx.shadowColor = COLORS.cinderGlow;
  ctx.shadowBlur = 8;
  ctx.beginPath();
  shard.points.forEach((point, index) => {
    if (index === 0) ctx.moveTo(point.x, point.y);
    else ctx.lineTo(point.x, point.y);
  });
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
  ctx.restore();
}

// `drifter` — slow heavy plasma blob. The only round, soft-edged big body, so it
// never gets confused with a jagged rock. A breathing crimson halo sells the dread.
function drawDrifter(shard) {
  const r = shard.radius;
  const breath = 1 + Math.sin(shard.pulse) * 0.1;
  ctx.save();
  ctx.translate(shard.x, shard.y);

  // Wobbling outer heat halo.
  ctx.fillStyle = COLORS.veilGlow;
  ctx.beginPath();
  ctx.arc(0, 0, r * 1.5 * breath, 0, Math.PI * 2);
  ctx.fill();

  // Soft plasma body: crimson core fading to transparent.
  const grad = ctx.createRadialGradient(0, 0, r * 0.15, 0, 0, r);
  grad.addColorStop(0, COLORS.veilCore);
  grad.addColorStop(0.7, "rgba(255, 94, 110, 0.5)");
  grad.addColorStop(1, "rgba(255, 94, 110, 0)");
  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(0, 0, r, 0, Math.PI * 2);
  ctx.fill();

  // Faint crimson rim.
  ctx.strokeStyle = "rgba(255, 94, 110, 0.4)";
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.arc(0, 0, r * 0.92, 0, Math.PI * 2);
  ctx.stroke();
  ctx.restore();
}

// `lance` — fast spindly streaker. An elongated needle oriented along its
// velocity with a hot tip and a short motion streak behind: the only directional,
// non-circular shape, reading as a thrown spear.
function drawLance(shard) {
  const r = shard.radius;
  ctx.save();
  ctx.translate(shard.x, shard.y);
  ctx.rotate(shard.angle);

  // Additive heat streak behind the tip.
  ctx.fillStyle = COLORS.cinderGlow;
  ctx.beginPath();
  ctx.moveTo(-r * 3.4, 0);
  ctx.lineTo(-r * 0.8, -r * 0.5);
  ctx.lineTo(-r * 0.8, r * 0.5);
  ctx.closePath();
  ctx.fill();

  // Stretched 2:1 diamond/needle: hot core tip -> cooled tail.
  const grad = ctx.createLinearGradient(-r * 2, 0, r * 2, 0);
  grad.addColorStop(0, COLORS.cinderEdge);
  grad.addColorStop(1, COLORS.emberCore);
  ctx.fillStyle = grad;
  ctx.shadowColor = COLORS.moteHot;
  ctx.shadowBlur = 8;
  ctx.beginPath();
  ctx.moveTo(r * 2, 0); // hot leading tip
  ctx.lineTo(0, -r * 0.7);
  ctx.lineTo(-r * 2, 0);
  ctx.lineTo(0, r * 0.7);
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}

function drawShard(shard) {
  // Per-kind rendering dispatch. All paths stay inside the ember palette;
  // silhouette + motion carry identity. asteroid falls through to the default.
  if (shard.kind === "cinder") {
    drawCinder(shard);
    return;
  }
  if (shard.kind === "drifter") {
    drawDrifter(shard);
    return;
  }
  if (shard.kind === "lance") {
    drawLance(shard);
    return;
  }

  const alpha = 0.7 + Math.sin(shard.pulse) * 0.12;

  ctx.save();
  ctx.translate(shard.x, shard.y);
  ctx.rotate(shard.angle);
  ctx.lineWidth = 1.4;
  ctx.strokeStyle = `rgba(255, 122, 89, ${0.42 + alpha * 0.16})`;
  ctx.fillStyle = COLORS.hazardSoft;
  ctx.shadowColor = "rgba(255, 122, 89, 0.45)";
  ctx.shadowBlur = 8;
  ctx.beginPath();
  shard.points.forEach((point, index) => {
    if (index === 0) ctx.moveTo(point.x, point.y);
    else ctx.lineTo(point.x, point.y);
  });
  ctx.closePath();
  ctx.fill();
  ctx.stroke();
  ctx.shadowBlur = 0;

  ctx.strokeStyle = "rgba(243, 236, 230, 0.14)";
  ctx.beginPath();
  ctx.moveTo(shard.points[0].x * 0.28, shard.points[0].y * 0.28);
  const opposite = shard.points[Math.floor(shard.points.length / 2)];
  ctx.lineTo(opposite.x * 0.62, opposite.y * 0.62);
  ctx.stroke();
  ctx.restore();
}

function drawBullets() {
  for (const bullet of bullets) {
    const progress = bullet.age / bullet.life;
    ctx.save();
    ctx.globalAlpha = 1 - progress * 0.45;
    if (bullet.strong) {
      // Power shots read as hotter, heavier slugs than the base bullet.
      const strongColor = POWERUPS.strongShots.color;
      ctx.strokeStyle = strongColor;
      ctx.fillStyle = "#FFE3C2";
      ctx.lineWidth = 2.2;
      ctx.shadowColor = strongColor;
      ctx.shadowBlur = 20;
    } else {
      ctx.strokeStyle = "rgba(255, 180, 150, 0.85)";
      ctx.fillStyle = COLORS.face;
      ctx.lineWidth = 1.4;
      ctx.shadowColor = COLORS.faceGlow;
      ctx.shadowBlur = 12;
    }
    ctx.beginPath();
    ctx.arc(bullet.x, bullet.y, bullet.radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    ctx.restore();
  }
}

function drawFace(frame, x, y, size, rotation, alpha = 1, color = COLORS.face) {
  // All Whim-face rendering goes through this helper so future agents can swap
  // the art source, tint, or scaling policy in one place.
  const paths = facePathCache[frame] ?? facePathCache.default;
  const scale = size / FACE_VIEW_BOX.width;

  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(rotation);
  ctx.scale(scale, scale);
  ctx.translate(-FACE_VIEW_BOX.width / 2, -FACE_VIEW_BOX.height / 2);
  ctx.globalAlpha *= alpha;
  ctx.fillStyle = color;
  for (const path of paths) {
    ctx.fill(path);
  }
  ctx.restore();
}

function drawPlayer(now) {
  const speed = Math.hypot(player.vx, player.vy);
  const moving = state === GAME_STATE.PLAYING && speed > 42;
  const frame = getFaceFrame(now, moving);
  const shotPulse = clamp((shotFrameUntil - now) / 180, 0, 1);

  // Flicker during the post-hit grace period so the player can see they are
  // temporarily safe after losing a life.
  if (invulnerableTimer > 0) {
    ctx.globalAlpha = Math.floor(now / 110) % 2 === 0 ? 0.35 : 0.85;
  }

  ctx.save();
  ctx.translate(player.x, player.y);
  ctx.rotate(player.rotation);
  ctx.fillStyle = "rgba(255, 77, 77, 0.055)";
  ctx.strokeStyle = "rgba(255, 77, 77, 0.18)";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.ellipse(0, 0, player.size * 0.62, player.size * 0.46, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.stroke();
  ctx.restore();

  // Draw the timed shield bubble while invulnerable so it's obviously active.
  if (isPowerUpActive("shield")) {
    const shieldPulse = (Math.sin(now / 120) + 1) / 2;
    const radius = player.radius * (1.55 + shieldPulse * 0.12);
    ctx.save();
    ctx.translate(player.x, player.y);
    // Soft inner glow fill.
    ctx.fillStyle = `rgba(255, 105, 180, ${0.08 + shieldPulse * 0.07})`;
    ctx.beginPath();
    ctx.arc(0, 0, radius, 0, Math.PI * 2);
    ctx.fill();
    // Bright pulsing outer ring.
    ctx.strokeStyle = `rgba(255, 105, 180, ${0.45 + shieldPulse * 0.35})`;
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.restore();
  }

  if (shotPulse > 0) {
    ctx.save();
    ctx.translate(player.x, player.y);
    ctx.rotate(aimAngle);
    ctx.strokeStyle = `rgba(255, 77, 77, ${shotPulse * 0.5})`;
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(
      player.radius * 1.1,
      0,
      player.size * (0.22 + (1 - shotPulse) * 0.16),
      0,
      Math.PI * 2,
    );
    ctx.stroke();
    ctx.restore();
  }

  // He's permanently furious: a fast tremble, a hot red glare, and angry brows.
  const trembleX = Math.sin(now * 0.05) * 1.4 + Math.sin(now * 0.13) * 0.8;
  const trembleY = Math.cos(now * 0.047) * 1.4 + Math.cos(now * 0.11) * 0.8;
  const faceX = player.x + trembleX;
  const faceY = player.y + trembleY;

  ctx.save();
  ctx.shadowColor = "rgba(255, 32, 32, 0.85)";
  ctx.shadowBlur = 30;
  drawFace(
    frame,
    faceX,
    faceY,
    player.size * (1 + shotPulse * 0.04),
    player.rotation,
    1,
    "#FF2A1A",
  );
  ctx.restore();

  drawAngryBrows(faceX, faceY, player.size, player.rotation);
  ctx.globalAlpha = 1;
}

function drawAngryBrows(x, y, size, rotation) {
  // Two thick brows angled down toward the center — the universal "mad" signal.
  // Drawn over the face so the abstract Whim glyph reads as angry.
  ctx.save();
  ctx.translate(x, y);
  ctx.rotate(rotation);
  ctx.strokeStyle = "#7a0000";
  ctx.lineWidth = Math.max(3, size * 0.07);
  ctx.lineCap = "round";
  ctx.beginPath();
  // Left brow: outer-high to inner-low.
  ctx.moveTo(-size * 0.36, -size * 0.5);
  ctx.lineTo(-size * 0.08, -size * 0.36);
  // Right brow: inner-low to outer-high.
  ctx.moveTo(size * 0.08, -size * 0.36);
  ctx.lineTo(size * 0.36, -size * 0.5);
  ctx.stroke();
  ctx.restore();
}

function drawEffects() {
  for (const trail of trails) {
    const progress = trail.age / trail.life;
    drawFace(
      trail.frame,
      trail.x,
      trail.y,
      trail.size * (1 + progress * 0.12),
      trail.rotation,
      (1 - progress) * 0.18,
      COLORS.face,
    );
  }

  for (const particle of burstParticles) {
    const progress = particle.age / particle.life;
    ctx.globalAlpha = (1 - progress) * 0.78;
    ctx.strokeStyle = particle.color;
    ctx.lineWidth = 1.4;
    ctx.beginPath();
    ctx.moveTo(particle.x, particle.y);
    ctx.lineTo(
      particle.x - particle.vx * 0.035,
      particle.y - particle.vy * 0.035,
    );
    ctx.stroke();
  }
  ctx.globalAlpha = 1;
}

function drawPowerUp(powerUp, now) {
  const glow = 0.5 + Math.sin(now * 0.008 + powerUp.x) * 0.5;
  const color = POWERUPS[powerUp.type]?.color ?? COLORS.face;

  ctx.save();
  ctx.translate(powerUp.x, powerUp.y);
  ctx.shadowColor = color;
  ctx.shadowBlur = 16 * glow;
  ctx.strokeStyle = color;
  ctx.lineWidth = 1.8;
  ctx.beginPath();
  ctx.arc(0, 0, powerUp.radius, 0, Math.PI * 2);
  ctx.stroke();

  ctx.fillStyle = `rgba(255, 255, 255, ${glow * 0.4})`;
  ctx.beginPath();
  ctx.arc(0, 0, powerUp.radius * 0.6, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

function drawPowerUpIndicators(now) {
  const padding = 16;
  const indicatorSize = 14;
  const indicatorSpacing = 20;
  let x = padding;
  const y = padding;

  for (const id of POWERUP_IDS) {
    const def = POWERUPS[id];
    const power = activePowerUps[id];

    // Instant power-ups have no timer, so they never render a permanent dim dot.
    if (def.instant) continue;

    ctx.save();
    if (power.active) {
      const remaining = Math.max(0, power.expiresAt - now);
      ctx.shadowColor = def.color;
      ctx.shadowBlur = 12;
      ctx.fillStyle = def.color;
      // Pulse the dot in its final closing window so the player feels a
      // build-defining buff about to expire.
      ctx.globalAlpha = remaining < 1200 ? 0.5 + 0.5 * Math.sin(now / 120) : 0.8;
    } else {
      ctx.fillStyle = COLORS.muted;
      ctx.globalAlpha = 0.25;
    }

    ctx.beginPath();
    ctx.arc(x, y, indicatorSize / 2, 0, Math.PI * 2);
    ctx.fill();

    if (power.active) {
      const remaining = Math.max(0, power.expiresAt - now);
      // Guard against a missing/zero duration so the ring math never divides by
      // undefined for any future timed entry.
      const span = def.duration > 0 ? def.duration * 1000 : 1;
      const progress = clamp(remaining / span, 0, 1);
      ctx.strokeStyle = def.color;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(x, y, indicatorSize / 2 + 1, -Math.PI / 2, -Math.PI / 2 + progress * Math.PI * 2);
      ctx.stroke();

      // Surface the Bounty economy buff as a small multiplier label.
      if (def.scoreMultiplier !== undefined) {
        ctx.shadowBlur = 0;
        ctx.globalAlpha = 0.9;
        ctx.fillStyle = def.color;
        ctx.font = "10px system-ui, sans-serif";
        ctx.textAlign = "center";
        ctx.textBaseline = "top";
        ctx.fillText(`x${def.scoreMultiplier}`, x, y + indicatorSize / 2 + 3);
      }
    }

    ctx.restore();
    x += indicatorSpacing;
  }
}

function drawPointerGuide() {
  if (!pointerTarget.active || state !== GAME_STATE.PLAYING) return;

  const distance = Math.hypot(pointerTarget.x - player.x, pointerTarget.y - player.y);
  const alpha = clamp(distance / 220, 0.12, 0.42);

  ctx.save();
  ctx.strokeStyle = `rgba(255, 77, 77, ${alpha})`;
  ctx.lineWidth = 1;
  ctx.setLineDash([4, 8]);
  ctx.beginPath();
  ctx.moveTo(player.x, player.y);
  ctx.lineTo(pointerTarget.x, pointerTarget.y);
  ctx.stroke();

  ctx.setLineDash([]);
  ctx.beginPath();
  ctx.arc(pointerTarget.x, pointerTarget.y, 13, 0, Math.PI * 2);
  ctx.stroke();
  ctx.restore();
}

function draw(now) {
  drawBackground(now);
  drawEffects();
  drawPointerGuide();

  for (const shard of shards) {
    drawShard(shard);
  }

  for (const powerUp of powerUps) {
    drawPowerUp(powerUp, now);
  }

  drawSquirrel();
  drawDolphin();
  drawBullets();
  drawPlayer(now);
  drawPowerUpIndicators(now);
}

function loop(now) {
  // Cap delta time so a hidden tab or paused debugger does not jump the player
  // through hazards when animation resumes.
  const dt = Math.min((now - lastTime) / 1000, 0.033);
  lastTime = now;
  update(dt, now);
  draw(now);
  requestAnimationFrame(loop);
}

function moveKeyFor(event) {
  if (ARROW_KEYS.has(event.key)) return event.key;
  return WASD_TO_ARROW[event.code] ?? null;
}

function handleKeyDown(event) {
  resumeAudio();
  // Mute toggle on 'M' — handle before the restart-on-any-key logic so it never
  // starts or restarts a round.
  if (event.code === "KeyM") {
    event.preventDefault();
    toggleMute();
    return;
  }
  const moveKey = moveKeyFor(event);
  const isFire = FIRE_KEYS.has(event.code);
  if (!moveKey && !isFire && event.key !== "Enter") return;
  event.preventDefault();

  if (state !== GAME_STATE.PLAYING) {
    resetRound();
  }

  if (moveKey) {
    keys.add(moveKey);
  }
  if (isFire) {
    shoot(event.timeStamp);
  }
}

function handleKeyUp(event) {
  const moveKey = moveKeyFor(event);
  if (moveKey) {
    event.preventDefault();
    keys.delete(moveKey);
  }
}

function updatePointerTarget(event) {
  const rect = canvas.getBoundingClientRect();
  pointerTarget.x = clamp(event.clientX - rect.left, 0, width);
  pointerTarget.y = clamp(event.clientY - rect.top, 0, height);
}

function handlePointerDown(event) {
  if (event.button !== undefined && event.button !== 0) return;
  resumeAudio();
  event.preventDefault();

  pointerTarget.active = true;
  pointerTarget.pointerId = event.pointerId;
  updatePointerTarget(event);
  canvas.setPointerCapture?.(event.pointerId);
}

function handlePointerMove(event) {
  if (!pointerTarget.active || event.pointerId !== pointerTarget.pointerId) return;
  event.preventDefault();
  updatePointerTarget(event);
}

function clearPointerTarget(event) {
  if (event && event.pointerId !== pointerTarget.pointerId) return;
  pointerTarget.active = false;
  pointerTarget.pointerId = null;
  if (event) canvas.releasePointerCapture?.(event.pointerId);
}

function triggerShootButton() {
  if (state !== GAME_STATE.PLAYING) {
    resetRound();
    return;
  }
  shoot(performance.now());
}

function handleShootButtonPointerDown(event) {
  resumeAudio();
  event.preventDefault();
  lastShootButtonPointerAt = performance.now();
  triggerShootButton();
}

function handleShootButtonClick(event) {
  event.preventDefault();
  // Pointer activation also produces a click in most browsers. Keep the
  // pointer path immediate, while preserving keyboard/screen-reader clicks.
  if (performance.now() - lastShootButtonPointerAt < 450) return;
  triggerShootButton();
}

restartButton.addEventListener("click", resetRound);
shootButton.addEventListener("pointerdown", handleShootButtonPointerDown, {
  passive: false,
});
shootButton.addEventListener("click", handleShootButtonClick);
canvas.addEventListener("pointerdown", handlePointerDown, { passive: false });
canvas.addEventListener("pointermove", handlePointerMove, { passive: false });
canvas.addEventListener("pointerup", clearPointerTarget);
canvas.addEventListener("pointercancel", clearPointerTarget);
window.addEventListener("keydown", handleKeyDown, { passive: false });
window.addEventListener("keyup", handleKeyUp, { passive: false });
window.addEventListener("blur", () => {
  keys.clear();
  clearPointerTarget();
});
window.addEventListener("resize", resizeCanvas);
if ("ResizeObserver" in window) {
  // The game row can change height independently from the viewport on mobile
  // when browser UI appears/disappears, so observe the actual playfield.
  new ResizeObserver(resizeCanvas).observe(gameShell);
}

resizeCanvas();
showIntroOverlay();
updateScorebar();
requestAnimationFrame((now) => {
  resizeCanvas();
  lastTime = now;
  requestAnimationFrame(loop);
});
