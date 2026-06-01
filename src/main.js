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
const ctx = canvas.getContext("2d");
const scoreEl = document.querySelector("#score");
const timeEl = document.querySelector("#time");
const multiplierEl = document.querySelector("#multiplier");
const weaponEl = document.querySelector("#weapon");
const overlayEl = document.querySelector("#gameOverlay");
const overlayEyebrowEl = document.querySelector("#overlayEyebrow");
const overlayTitleEl = document.querySelector("#overlayTitle");
const overlayCopyEl = document.querySelector("#overlayCopy");
const restartButton = document.querySelector("#restartButton");
const shootButton = document.querySelector("#shootButton");
const weaponButton = document.querySelector("#weaponButton");
const weaponNameEl = document.querySelector("#weaponName");

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
  // Lance bolts read hotter than the default face-blue bullet to telegraph the
  // piercing weapon while staying inside the Whim blue family.
  lance: "#bfeaff",
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
  // Per-weapon bullet tuning (speed, life, radius, cooldown, recoil) lives in
  // the WEAPONS registry below.
  shardClearScore: 35,
  // Combo multiplier: each shard cleared within comboWindow seconds of the last
  // bumps the multiplier by multiplierStep (capped at maxMultiplier). The combo
  // lapses back to x1 once the window elapses without another hit.
  comboWindow: 2.2,
  multiplierStep: 0.5,
  maxMultiplier: 5,
};

// Weapon arsenal. Each entry is self-describing so shoot() and updateBullets()
// stay weapon-agnostic: `pattern` lists per-bolt angle offsets (radians) around
// the aim, `pierce` is how many shards a bolt passes through before it despawns
// (0 = the original despawn-on-first-hit bolt), and `color` is optional (falls
// back to COLORS.face). Tune weapon feel here, not in the loop.
const WEAPONS = [
  {
    name: "Pulse",
    cooldownMs: 190,
    bulletSpeed: 620,
    bulletLife: 0.82,
    bulletRadius: 4,
    recoil: 34,
    muzzleCount: 6,
    pattern: [0],
    pierce: 0,
  },
  {
    name: "Spread",
    cooldownMs: 340,
    bulletSpeed: 560,
    bulletLife: 0.5,
    bulletRadius: 4,
    recoil: 70,
    muzzleCount: 5,
    pattern: [-0.28, 0, 0.28], // ~±16° fan: shorter range, heavier recoil
    pierce: 0,
  },
  {
    name: "Lance",
    cooldownMs: 300,
    bulletSpeed: 900,
    bulletLife: 1.05,
    bulletRadius: 4,
    recoil: 14,
    muzzleCount: 7,
    pattern: [0],
    pierce: 3, // passes through up to 3 shards, despawning on the next hit
    color: COLORS.lance,
  },
];

// Desktop number-row keys select weapon slots directly. Handled at the top of
// handleKeyDown so they never fall through to movement, fire, or round start.
const WEAPON_KEYS = {
  Digit1: 0,
  Digit2: 1,
  Digit3: 2,
};

const ARROW_KEYS = new Set([
  "ArrowLeft",
  "ArrowRight",
  "ArrowUp",
  "ArrowDown",
]);
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
};

let width = 1;
let height = 1;
let dpr = 1;
let state = GAME_STATE.INTRO;
let elapsed = 0;
let score = 0;
let clearScore = 0;
let multiplier = 1;
let comboTimer = 0;
let best = readBestScore();
let lastTime = performance.now();
let hasPlacedPlayer = false;
let spawnTimer = 0.4;
let trailTimer = 0;
let lastShotAt = -Infinity;
let lastShootButtonPointerAt = -Infinity;
let lastWeaponButtonPointerAt = -Infinity;
let activeWeapon = 0; // persists across rounds; intentionally not reset by resetRound
let shotFrameUntil = 0;
let aimAngle = -Math.PI / 2;
let idleFrame = "default";
let idleFrameUntil = 0;
let nextIdleFrameAt = 1600;
let shards = [];
let bullets = [];
let trails = [];
let burstParticles = [];
let starField = [];

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
  const seconds = String(totalSeconds % 60).padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function readBestScore() {
  try {
    return Number(localStorage.getItem("whim-asteroids-best") ?? 0) || 0;
  } catch {
    return 0;
  }
}

function writeBestScore(value) {
  try {
    localStorage.setItem("whim-asteroids-best", String(Math.floor(value)));
  } catch {
    // Score persistence is optional; the game should keep running without it.
  }
}

function updateHud() {
  scoreEl.textContent = formatScore(score);
  timeEl.textContent = formatTime(elapsed);
  multiplierEl.textContent = `×${multiplier.toFixed(1)}`;
  multiplierEl.classList.toggle("is-combo", multiplier > 1);
  const weaponName = WEAPONS[activeWeapon].name;
  weaponEl.textContent = weaponName;
  weaponNameEl.textContent = weaponName;
}

function showIntroOverlay() {
  state = GAME_STATE.INTRO;
  overlayEyebrowEl.textContent = "Ready";
  overlayTitleEl.textContent = "Whim Asteroids";
  overlayCopyEl.textContent =
    "Arrow keys or drag to move. Space shoots. Switch weapons with 1/2/3 or the mobile weapon button. On mobile, tap the glowing round button to fire. Clear shards and keep the face moving.";
  restartButton.textContent = "Start";
  overlayEl.hidden = false;
  updateHud();
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
  const measuredWidth = shellRect.width || rect.width;
  const measuredHeight = shellRect.height || rect.height;
  const fallbackHeight = Math.max(1, window.innerHeight);
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
  // Reset only round-local state. Best score persists across rounds and should
  // remain outside this function unless a future UI explicitly clears it.
  state = GAME_STATE.PLAYING;
  elapsed = 0;
  score = 0;
  clearScore = 0;
  multiplier = 1;
  comboTimer = 0;
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
  centerPlayer();
  overlayEl.hidden = true;
  updateHud();
}

function endRound() {
  state = GAME_STATE.OVER;
  best = Math.max(best, score);
  writeBestScore(best);
  showGameOverOverlay();
  createBurst(player.x, player.y, 26, COLORS.danger);
  updateHud();
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

function spawnShard() {
  // Hazards spawn just offscreen, then aim roughly toward the playfield center
  // with jitter. This feels intentional without requiring pathfinding.
  const margin = 70;
  const edge = Math.floor(Math.random() * 4);
  let x;
  let y;

  if (edge === 0) {
    x = randomBetween(0, width);
    y = -margin;
  } else if (edge === 1) {
    x = width + margin;
    y = randomBetween(0, height);
  } else if (edge === 2) {
    x = randomBetween(0, width);
    y = height + margin;
  } else {
    x = -margin;
    y = randomBetween(0, height);
  }

  const distanceToPlayer = Math.hypot(x - player.x, y - player.y);
  if (distanceToPlayer < GAME_CONFIG.safeSpawnDistance) {
    x += Math.sign(x - player.x || 1) * GAME_CONFIG.safeSpawnDistance;
    y += Math.sign(y - player.y || 1) * GAME_CONFIG.safeSpawnDistance;
  }

  const difficulty = getDifficulty();
  const radius = randomBetween(18, 38 + difficulty * 12);
  const targetX = randomBetween(width * 0.15, width * 0.85);
  const targetY = randomBetween(height * 0.15, height * 0.85);
  const heading = Math.atan2(targetY - y, targetX - x) + randomBetween(-0.42, 0.42);
  const speed = randomBetween(54, 108) + difficulty * 82;

  shards.push({
    x,
    y,
    vx: Math.cos(heading) * speed,
    vy: Math.sin(heading) * speed,
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
  const weapon = WEAPONS[activeWeapon];
  if (now - lastShotAt < weapon.cooldownMs) return;

  const angle = getFireAngle();
  const muzzleDistance = player.radius * 1.25;
  const muzzleX = player.x + Math.cos(angle) * muzzleDistance;
  const muzzleY = player.y + Math.sin(angle) * muzzleDistance;

  aimAngle = angle;
  lastShotAt = now;
  shotFrameUntil = now + 180;

  // Emit the active weapon's pattern: one bolt per angle offset around the aim.
  // Each bolt carries its own motion, pierce budget, and color so updateBullets
  // stays weapon-agnostic and reuses one shared shard-clear path.
  for (const offset of weapon.pattern) {
    const shotAngle = angle + offset;
    bullets.push({
      x: muzzleX,
      y: muzzleY,
      vx: Math.cos(shotAngle) * weapon.bulletSpeed + player.vx * 0.18,
      vy: Math.sin(shotAngle) * weapon.bulletSpeed + player.vy * 0.18,
      age: 0,
      life: weapon.bulletLife,
      radius: weapon.bulletRadius,
      pierce: weapon.pierce,
      color: weapon.color,
    });
  }

  player.vx -= Math.cos(angle) * weapon.recoil;
  player.vy -= Math.sin(angle) * weapon.recoil;
  createBurst(muzzleX, muzzleY, weapon.muzzleCount, weapon.color ?? COLORS.face);
}

function selectWeapon(index) {
  // Direct selection (number keys / future bindings). Ignore out-of-range or
  // no-op selections so the active slot can't desync.
  if (index < 0 || index >= WEAPONS.length || index === activeWeapon) return;
  activeWeapon = index;
  updateHud();
}

function cycleWeapon() {
  // Tap-to-cycle for the mobile weapon button.
  activeWeapon = (activeWeapon + 1) % WEAPONS.length;
  updateHud();
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

  for (const shard of shards) {
    shard.x = wrap(shard.x + shard.vx * dt, width);
    shard.y = wrap(shard.y + shard.vy * dt, height);
    shard.angle += shard.spin * dt;
    shard.pulse += dt * 2;
  }

  if (state !== GAME_STATE.PLAYING || elapsed < 0.65) return;

  for (const shard of shards) {
    const hitRadius = player.radius + shard.radius * 0.72;
    if (Math.hypot(player.x - shard.x, player.y - shard.y) <= hitRadius) {
      endRound();
      break;
    }
  }
}

function splitShard(shard) {
  // Split a shard into smaller chunks. Smaller shards don't split further to
  // prevent infinite fragmentation.
  const minSplitRadius = 12;
  if (shard.radius < minSplitRadius) return;

  const childRadius = shard.radius * randomBetween(0.55, 0.68);
  const childCount = Math.random() < 0.5 ? 2 : 3;
  const baseAngle = Math.random() * Math.PI * 2;

  for (let i = 0; i < childCount; i += 1) {
    const spawnAngle = baseAngle + (i / childCount) * Math.PI * 2 + randomBetween(-0.3, 0.3);
    const spawnDistance = shard.radius * 0.6;
    const childX = shard.x + Math.cos(spawnAngle) * spawnDistance;
    const childY = shard.y + Math.sin(spawnAngle) * spawnDistance;
    const childSpeed = Math.hypot(shard.vx, shard.vy) + randomBetween(40, 90);
    const childVx = Math.cos(spawnAngle) * childSpeed;
    const childVy = Math.sin(spawnAngle) * childSpeed;

    shards.push({
      x: childX,
      y: childY,
      vx: childVx,
      vy: childVy,
      radius: childRadius,
      points: createShardPoints(childRadius),
      angle: Math.random() * Math.PI * 2,
      spin: randomBetween(-1.3, 1.3),
      pulse: Math.random() * Math.PI * 2,
    });
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

      // Shared shard-clear path for every weapon: split, build the combo (bump
      // the multiplier one capped step and refresh the combo window), award the
      // shard at the boosted multiplier, then spark a burst.
      splitShard(shard);
      shards.splice(shardIndex, 1);
      multiplier = Math.min(
        GAME_CONFIG.maxMultiplier,
        multiplier + GAME_CONFIG.multiplierStep,
      );
      comboTimer = GAME_CONFIG.comboWindow;
      clearScore += Math.round(GAME_CONFIG.shardClearScore * multiplier);
      createBurst(shard.x, shard.y, 12, COLORS.face);

      // Piercing bolts spend one pierce per shard and keep flying; plain bolts
      // (pierce 0) despawn on first contact, exactly like the original bullet.
      if (bullet.pierce > 0) {
        bullet.pierce -= 1;
      } else {
        bullets.splice(bulletIndex, 1);
        break;
      }
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
    // Combo multiplier decays in real time; once the window lapses without a
    // shard cleared, it falls back to x1.
    if (comboTimer > 0) {
      comboTimer = Math.max(0, comboTimer - dt);
      if (comboTimer === 0) multiplier = 1;
    }
    score = elapsed * GAME_CONFIG.scoreRate + clearScore;
    updatePlayer(dt, now);
    updateShards(dt);
    updateBullets(dt);
  }

  updateEffects(dt);
  updateHud();
}

function drawBackground(now) {
  // The game scene is painted entirely on canvas; CSS owns only the HUD and
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
  ctx.strokeStyle = `rgba(100, 205, 252, ${0.32 + alpha * 0.12})`;
  ctx.fillStyle = "rgba(100, 205, 252, 0.045)";
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
    ctx.strokeStyle = "rgba(100, 205, 252, 0.72)";
    ctx.fillStyle = bullet.color ?? COLORS.face;
    ctx.lineWidth = 1.4;
    ctx.shadowColor = "rgba(100, 205, 252, 0.38)";
    ctx.shadowBlur = 12;
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

  drawBullets();
  drawPlayer(now);
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

function handleKeyDown(event) {
  const weaponIndex = WEAPON_KEYS[event.code];
  if (weaponIndex !== undefined) {
    // Weapon select works in any state (including the intro/over overlays) and
    // must not fall through to movement, fire, or round start.
    event.preventDefault();
    selectWeapon(weaponIndex);
    return;
  }

  const isArrow = ARROW_KEYS.has(event.key);
  const isFire = FIRE_KEYS.has(event.code);
  if (!isArrow && !isFire && event.key !== "Enter") return;
  event.preventDefault();

  if (state !== GAME_STATE.PLAYING) {
    resetRound();
  }

  if (isArrow) {
    keys.add(event.key);
  }
  if (isFire) {
    shoot(event.timeStamp);
  }
}

function handleKeyUp(event) {
  if (ARROW_KEYS.has(event.key)) {
    event.preventDefault();
    keys.delete(event.key);
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

function handleWeaponButtonPointerDown(event) {
  event.preventDefault();
  lastWeaponButtonPointerAt = performance.now();
  cycleWeapon();
}

function handleWeaponButtonClick(event) {
  event.preventDefault();
  // Mirror the shoot-button dedupe: a pointer tap also emits a click, so skip
  // the click that immediately follows it while keeping keyboard/SR clicks.
  if (performance.now() - lastWeaponButtonPointerAt < 450) return;
  cycleWeapon();
}

restartButton.addEventListener("click", resetRound);
shootButton.addEventListener("pointerdown", handleShootButtonPointerDown, {
  passive: false,
});
shootButton.addEventListener("click", handleShootButtonClick);
weaponButton.addEventListener("pointerdown", handleWeaponButtonPointerDown, {
  passive: false,
});
weaponButton.addEventListener("click", handleWeaponButtonClick);
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
updateHud();
requestAnimationFrame((now) => {
  resizeCanvas();
  lastTime = now;
  requestAnimationFrame(loop);
});
