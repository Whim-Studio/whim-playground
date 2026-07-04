# Shinobi (1987) — Arcade Mechanics Research → Domain Model

Concise notes on the original Sega arcade *Shinobi* (1987), focused only on the
mechanics this domain layer must represent, and how each maps to `api.Config` /
`api.Enums`. Behavior (physics, AI, combat resolution) belongs to Task 2 — this
package only holds **data and mutable state**.

## 1. Two-plane (foreground/background) path structure
The arcade stages are side-scrolling but split vertically into two walkable
"lanes." Joe Musashi can leap between a **foreground** lane and a **background**
lane. Enemies, hostages, and the player each occupy one lane at a time; attacks
generally only connect within the same lane, and the plane-shift is a discrete
vertical hop rather than free movement.

- Maps to `Enums.Plane{LOWER, UPPER}` — LOWER = foreground path, UPPER = raised
  background path.
- `Config.GROUND_Y_LOWER = 384`, `Config.GROUND_Y_UPPER = 224` are the feet-Y
  (ground line) of each plane. A grounded entity's top-left `y = groundY -
  ENTITY_H`. `LevelBuilder` places every ground/segment platform and every
  hostage/enemy using these constants via `LevelMap.groundYFor(plane, x)`.

## 2. Joe Musashi movement & jump feel
Musashi walks at a steady pace, and his jump is a fixed-impulse arc (no variable
jump height in the arcade original) with a fast fall — snappy and committal.
Crouching lets him duck low shots and throw low.

- `Config.MOVE_SPEED = 2.6` px/tick walk, `Config.JUMP_VELOCITY = -9.5` impulse,
  `Config.GRAVITY = 0.55` px/tick² fall — a short, punchy arc.
- `Enums.EntityState{IDLE, WALK, JUMP, ATTACK, BLOCK, DEAD}` and `Enums.Facing`
  carry the coarse pose; crouch is an engine-driven flag (held input), so the
  domain only stores state + facing + velocity, not the input.

## 3. Enemy archetypes: THUG vs sword-NINJA
Two broad melee/ranged threats matter for the model:
- **THUGs** — rush and/or throw; die to a single hit; the disposable fodder.
- **sword-NINJAs** — faster, close with a blade, and crucially can **block/deflect
  thrown shuriken**, forcing melee or a weapon upgrade to punch through.

- Maps to `Enums.EnemyType{THUG, NINJA}`. `Enemy.blocking` (a mutable flag the
  engine toggles) drives the NINJA deflect pose and the "shuriken is blocked"
  combat rule. The domain only *holds* the flag plus AI timer/target fields; the
  ThugAI/NinjaAI logic lives in Task 2.

## 4. Tied-up hostages: points, weapon upgrades, bonuses
Scattered through each stage are tied-up hostages (Musashi's captured ninja
clan). Freeing one — by reaching/touching it (or shooting the binding) — yields
points, and specific rescues **upgrade the weapon** along
`SHURIKEN → KNIFE → GUN`, plus end-of-stage bonuses for freeing them all.

- Maps to `Enums.Weapon{SHURIKEN, KNIFE, GUN}` with `Weapon.upgrade()` walking the
  chain, and `Enums.RescueReward{POINTS, WEAPON_UPGRADE, EXTRA_NINJUTSU}`.
- `Hostage` holds `plane`, `rescued`, and its `RescueReward`. `Player.upgradeWeapon()`
  calls `weapon.upgrade()`; `WorldState` tracks `hostagesRescued / hostagesTotal`
  for the stage-clear bonus.

## 5. Ninjutsu — limited screen-clear magic
Musashi carries a few charges of **Ninjutsu**: a per-stage limited magic that
clears/damages all on-screen enemies with a full-screen flash. Charges are scarce
(start with a couple) and a rescue reward can grant an extra.

- Maps to `Config.START_NINJUTSU = 2`, `Player.ninjutsu` charge count (mutated by
  `useNinjutsu()` / `addNinjutsu()`), `Enums.Phase.NINJUTSU`, and
  `WorldState.ninjutsuFlash` (0→1 animation ramp, −1 idle) that the UI reads to
  drive the flash. The clear *effect* is Task 2's `NinjutsuSystem`.

## 6. Stage timing & lives
Each stage runs against a countdown; running out (or losing all HP) costs a life,
and running out of lives ends the game.

- `Config.LEVEL_TIME_SECONDS = 90`, `Config.START_LIVES = 3`. `WorldState`
  exposes `secondsRemaining` and `phase`; `Player.lives` + `loseLife()` and the
  `invulnTimer` (post-hit i-frames) model the respawn beat.

## Summary mapping table
| Arcade mechanic | Domain field / type | Config / Enums |
|---|---|---|
| Foreground/background lanes | `Entity.plane`, `Platform.plane`, `Hostage.plane` | `Plane`, `GROUND_Y_*` |
| Walk / fixed jump arc | `Entity.vx/vy`, `EntityState` | `MOVE_SPEED`, `JUMP_VELOCITY`, `GRAVITY` |
| THUG vs blocking NINJA | `Enemy.type`, `Enemy.blocking` | `EnemyType` |
| Weapon progression | `Player.weapon` | `Weapon`, `Weapon.upgrade()` |
| Hostage rescue rewards | `Hostage.reward`, `Player.score/weapon/ninjutsu` | `RescueReward` |
| Screen-clear magic | `Player.ninjutsu`, `WorldState.ninjutsuFlash` | `START_NINJUTSU`, `Phase.NINJUTSU` |
| Timer / lives | `WorldState.secondsRemaining`, `Player.lives` | `LEVEL_TIME_SECONDS`, `START_LIVES` |
