import "./styles.css";
import {
  FACE_FRAMES,
  FACE_VIEW_BOX,
  MOTION_SEQUENCE,
} from "./whimFaceFrames.js";

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

// Palette mirrors Whim's landing/onboarding blue surface, with terracotta used
// only as an end-state accent. Canvas art and CSS chrome should stay aligned.
const COLORS = {
  background: "#172a45",
  surface: "#1e3355",
  surfaceHover: "#243d62",
  border: "#2a4a6e",
  foreground: "#e8e4de",
  muted: "#8eaac4",
  face: "#64CDFC",
  faceSoft: "rgba(100, 205, 252, 0.14)",
  faceLine: "rgba(100, 205, 252, 0.52)",
  danger: "#d4836c",
};

// Primary gameplay tuning surface. Distances are CSS pixels; speeds are pixels
// per second; interval values are seconds. Start here for balancing changes.
const GAME_CONFIG = {
  acceleration: 820,
  maxSpeed: 360,
  activeDamping: 0.945,
  idleDamping: 0.86,
  scoreRate: 14,
  spawnIntervalStart: 0.92,
  spawnIntervalEnd: 0.42,
  maxDifficultyTime: 52,
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
  shardSplitSpeedBoost: 1.4,
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
// rapidFire is the canonical sample proving the framework end-to-end.
const POWERUPS = {
  rapidFire: {
    id: "rapidFire",
    label: "Rapid fire",
    color: "#FFD700",
    duration: GAME_CONFIG.powerUpDuration,
    shootCooldownMs: GAME_CONFIG.rapidFireCooldownMs,
  },
  slowShards: {
    id: "slowShards",
    label: "Slow shards",
    color: "#87CEEB",
    duration: GAME_CONFIG.powerUpDuration,
    shardSpeedFactor: GAME_CONFIG.shardSlowFactor,
  },
  shield: {
    id: "shield",
    label: "Shield",
    color: "#FF69B4",
    duration: GAME_CONFIG.powerUpDuration,
    blocksAllHits: true,
  },
  strongShots: {
    id: "strongShots",
    label: "Power shots",
    color: "#FF8C42",
    duration: GAME_CONFIG.powerUpDuration,
    strongShots: true,
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
    "Press Enter, Space, any arrow key, the round button, or Restart.";
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
  spawnTimer = 0.45;
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
  centerPlayer();
  overlayEl.hidden = true;
  updateScorebar();
}

function endRound() {
  state = GAME_STATE.OVER;
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

function spawnShard(x, y, vx, vy, splitRadius) {
  // Hazards spawn just offscreen, then aim roughly toward the playfield center
  // with jitter. This feels intentional without requiring pathfinding.
  // When x/y/vx/vy are provided, this is a split from a destroyed shard, and
  // splitRadius (when given) is the smaller child size inherited from the parent.
  const isSpawned = x === undefined;

  let startX = x;
  let startY = y;
  let startVx = vx;
  let startVy = vy;

  if (isSpawned) {
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

    const difficulty = getDifficulty();
    const targetX = randomBetween(width * 0.15, width * 0.85);
    const targetY = randomBetween(height * 0.15, height * 0.85);
    const heading = Math.atan2(targetY - startY, targetX - startX) + randomBetween(-0.42, 0.42);
    const speed = randomBetween(54, 108) + difficulty * 82;

    startVx = Math.cos(heading) * speed;
    startVy = Math.sin(heading) * speed;
  }

  const difficulty = getDifficulty();
  // Split children inherit an explicit, smaller radius; fresh spawns size randomly.
  const radius = splitRadius !== undefined ? splitRadius : randomBetween(18, 38 + difficulty * 12);

  shards.push({
    x: startX,
    y: startY,
    vx: startVx,
    vy: startVy,
    radius,
    points: createShardPoints(radius),
    angle: Math.random() * Math.PI * 2,
    spin: randomBetween(-1.3, 1.3),
    pulse: Math.random() * Math.PI * 2,
  });
}

function maxShardCount() {
  const areaBonus = clamp((width * height) / 150000, 0, 5);
  return Math.floor(6 + areaBonus + getDifficulty() * 8);
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
  bullets.push({
    x: muzzleX,
    y: muzzleY,
    vx: Math.cos(angle) * bulletSpeed + player.vx * 0.18,
    vy: Math.sin(angle) * bulletSpeed + player.vy * 0.18,
    age: 0,
    life: GAME_CONFIG.bulletLife,
    radius: strong ? GAME_CONFIG.strongShotRadius : GAME_CONFIG.bulletRadius,
    pierce: strong ? GAME_CONFIG.strongShotPierce : 0,
    strong,
  });

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
  if (now < shotFrameUntil) return "smile";
  if (moving) {
    return MOTION_SEQUENCE[Math.floor(now / 96) % MOTION_SEQUENCE.length];
  }

  if (now >= nextIdleFrameAt && now >= idleFrameUntil) {
    idleFrame = Math.random() < 0.68 ? "blink" : "smile";
    idleFrameUntil = now + (idleFrame === "blink" ? 260 : 560);
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
    shard.x = wrap(shard.x + shard.vx * dt * slowFactor, width);
    shard.y = wrap(shard.y + shard.vy * dt * slowFactor, height);
    shard.angle += shard.spin * dt;
    shard.pulse += dt * 2;
  }

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

function takeDamage() {
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

function spawnPowerUp(x, y) {
  // Pick a registered power-up at random so new POWERUPS entries enter the
  // drop pool automatically.
  const type = POWERUP_IDS[Math.floor(Math.random() * POWERUP_IDS.length)];

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

function activatePowerUp(type, now) {
  const def = POWERUPS[type];
  if (!def) return;
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
    const angle = (i / GAME_CONFIG.shardSplitCount) * Math.PI * 2 + randomBetween(-0.3, 0.3);
    const vx = Math.cos(angle) * boostedSpeed;
    const vy = Math.sin(angle) * boostedSpeed;

    spawnShard(shard.x, shard.y, vx, vy, newRadius);
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
      clearScore += GAME_CONFIG.shardClearScore;
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
  }

  updateActivePowerUps(now);
  updateEffects(dt);
  updateScorebar();
}

function drawBackground(now) {
  // The game scene is painted entirely on canvas; CSS owns only the header and
  // overlay chrome. Keep decorative canvas work cheap and low contrast.
  ctx.fillStyle = COLORS.background;
  ctx.fillRect(0, 0, width, height);

  const grid = 40;
  const offset = (now * 0.012) % grid;
  ctx.lineWidth = 1;
  ctx.strokeStyle = "rgba(232, 228, 222, 0.028)";
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
    ctx.fillStyle = COLORS.foreground;
    ctx.beginPath();
    ctx.arc(star.x, star.y, star.r, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.globalAlpha = 1;
}

function drawShard(shard) {
  const alpha = 0.7 + Math.sin(shard.pulse) * 0.12;

  ctx.save();
  ctx.translate(shard.x, shard.y);
  ctx.rotate(shard.angle);
  ctx.lineWidth = 1.25;
  ctx.strokeStyle = `rgba(0, 255, 0, ${0.32 + alpha * 0.12})`;
  ctx.fillStyle = "rgba(0, 255, 0, 0.045)";
  ctx.beginPath();
  shard.points.forEach((point, index) => {
    if (index === 0) ctx.moveTo(point.x, point.y);
    else ctx.lineTo(point.x, point.y);
  });
  ctx.closePath();
  ctx.fill();
  ctx.stroke();

  ctx.strokeStyle = "rgba(232, 228, 222, 0.12)";
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
      ctx.strokeStyle = "rgba(255, 64, 200, 0.72)";
      ctx.fillStyle = COLORS.face;
      ctx.lineWidth = 1.4;
      ctx.shadowColor = "rgba(255, 64, 200, 0.38)";
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
  ctx.fillStyle = "rgba(100, 205, 252, 0.055)";
  ctx.strokeStyle = "rgba(100, 205, 252, 0.18)";
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
    ctx.strokeStyle = `rgba(100, 205, 252, ${shotPulse * 0.5})`;
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

  ctx.save();
  ctx.shadowColor = "rgba(100, 205, 252, 0.34)";
  ctx.shadowBlur = 22;
  drawFace(
    frame,
    player.x,
    player.y,
    player.size * (1 + shotPulse * 0.04),
    player.rotation,
    1,
  );
  ctx.restore();
  ctx.globalAlpha = 1;
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

    ctx.save();
    if (power.active) {
      ctx.shadowColor = def.color;
      ctx.shadowBlur = 12;
      ctx.fillStyle = def.color;
      ctx.globalAlpha = 0.8;
    } else {
      ctx.fillStyle = COLORS.muted;
      ctx.globalAlpha = 0.25;
    }

    ctx.beginPath();
    ctx.arc(x, y, indicatorSize / 2, 0, Math.PI * 2);
    ctx.fill();

    if (power.active) {
      const remaining = Math.max(0, power.expiresAt - now);
      const progress = clamp(remaining / (def.duration * 1000), 0, 1);
      ctx.strokeStyle = def.color;
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(x, y, indicatorSize / 2 + 1, -Math.PI / 2, -Math.PI / 2 + progress * Math.PI * 2);
      ctx.stroke();
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
  ctx.strokeStyle = `rgba(100, 205, 252, ${alpha})`;
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
