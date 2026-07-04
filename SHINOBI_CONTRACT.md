# Shinobi (1987) — Build Contract

A standalone, **Java 8**, zero-dependency, procedural-graphics Swing adaptation of
the 1987 Sega arcade side-scrolling ninja platformer **Shinobi**. Three parallel
tasks build against the shared `api` package. This file is the single source of
truth for the seams between them.

## Hard constraints (ALL tasks)

- **Java 8 ONLY.** No `var`, no switch expressions, no text blocks, no records, no
  `List.of`/`Map.of`, no `Stream.toList()`, no local type inference. Standard
  `switch` statements and Java 8 streams/lambdas are fine.
  `maven.compiler.source/target = 1.8`.
- **Zero external libraries.** Only `javax.swing`, `java.awt`, `java.util`,
  `java.io`, `java.lang`. No Maven/Gradle plugins beyond the jar plugin already in
  `pom.xml`. No downloaded assets — every visual is drawn with `Graphics2D`
  (shapes, polygons, gradients) or hardcoded int[] pixel arrays.
- **Package root:** `com.whim.shinobi` — **Source root:** `shinobi/src/`
- Every task's code **must compile** and import ONLY from `api` (and its own
  sub-package). No task edits another task's sub-package.
- 60-ticks/second simulation on a **background thread**, fully decoupled from the
  Swing repaint timer.
- When done: **push your branch and `send_prompt` a short report back to the
  orchestrator task** (do NOT open a PR into main yourself).

## Package ownership

| Package | Owner | Contents |
|---|---|---|
| `com.whim.shinobi.api` | **Orchestrator (DONE — do not edit)** | `Config`, `Enums`, `Views`, `GameController` |
| `com.whim.shinobi.domain` | **Task 1** | `Aabb`, `Entity`, `Player`, `Enemy`, `Projectile`, `Hostage`, `Platform`, `LevelMap`, `WorldState`, `LevelBuilder`, plus `RESEARCH.md` and `DomainSelfCheck` |
| `com.whim.shinobi.engine` | **Task 2** | `GameEngine` (implements `api.GameController`), `TickLoop`, `Physics` (AABB), `CombatSystem` (proximity), `ThugAI`, `NinjaAI`, `NinjutsuSystem`, `EngineSelfCheck` |
| `com.whim.shinobi.ui` | **Task 3** | `GameFrame`, `GamePanel`, `Renderer`, `Camera`, `Hud`, `InputHandler`, `Palette`, `StubController` (dev only), `UiPreview` |
| `com.whim.shinobi.app` | **Orchestrator (final)** | `Main` — wires engine + ui |

## The `api` package (already committed — read it, DO NOT modify)

- **`Config`** — world constants: `TICK_HZ=60`, `DT`, viewport `VIEW_W=512`
  `VIEW_H=448`, `LEVEL_W=4096`, entity box `ENTITY_W=28` `ENTITY_H=44`, plane
  ground lines `GROUND_Y_LOWER=384` / `GROUND_Y_UPPER=224`, `GRAVITY`,
  `MOVE_SPEED`, `JUMP_VELOCITY`, `MELEE_RANGE=40`, starting lives/ninjutsu/time.
  **All coordinates are world pixels; (x,y) = top-left of the collision box.**
- **`Enums`** — `Plane{LOWER,UPPER}`, `Facing`, `Weapon{SHURIKEN,KNIFE,GUN}`
  (each has `projectileSpeed()`, `cooldownTicks()`, `damage()`, `upgrade()`),
  `EnemyType{THUG,NINJA}`, `AttackMode{PROJECTILE,MELEE}`,
  `EntityState{IDLE,WALK,JUMP,ATTACK,BLOCK,DEAD}`,
  `RescueReward{POINTS,WEAPON_UPGRADE,EXTRA_NINJUTSU}`,
  `Phase{PLAYING,NINJUTSU,LEVEL_CLEAR,GAME_OVER,PAUSED}`.
- **`Views`** — read-only per-frame snapshot interfaces: `BoxView`, `EntityView`,
  `PlayerView`, `EnemyView`, `ProjectileView`, `HostageView`, `PlatformView`,
  `GameStateView`. **UI reads ONLY these and never casts to a concrete class.**
- **`GameController`** — the single UI↔engine seam: `state()`, held movement
  `setLeft/setRight/setCrouch(boolean)`, discrete `jump()`, `shiftPlane()`,
  `attack()`, `ninjutsu()`, and lifecycle `newGame()/start()/stop()/togglePause()`.

**Data flow:** the UI polls `controller.state()` on a Swing `javax.swing.Timer`
(~60 fps) and repaints; user input calls `GameController` methods. The engine
mutates domain state on its background tick thread; the UI re-reads `state()` and
repaints. UI never casts a `*View` to a concrete domain/engine class.

## Interface expectations between tasks

- **Task 1 (domain)** provides the concrete data classes that **implement the
  `Views` interfaces** so the engine can hand them straight to the UI. `Aabb`
  implements `BoxView` and exposes intersect/overlap helpers. `LevelMap` holds
  platforms + hostage spawns for both planes; `LevelBuilder.firstLevel()` returns
  a fully-populated `WorldState` (implements `GameStateView`).
- **Task 2 (engine)** owns ALL mutation and timing. `GameEngine implements
  api.GameController`, runs `TickLoop` at 60 Hz on a background thread, and
  returns the current `WorldState` from `state()` (snapshot-safe). It uses
  `domain.Aabb` for collision and reads `Config.MELEE_RANGE` for proximity.
- **Task 3 (ui)** never blocks the EDT on the engine. It develops against a
  `StubController implements api.GameController` returning a hand-built
  `WorldState` (or its own tiny fake) so it can run before Task 2 lands.

## Final level (Task 1 must deliver)

`LevelBuilder.firstLevel()`: a scrollable `LEVEL_W`-wide stage with continuous
lower-plane ground, several raised upper-plane segments, 3–5 tied hostages spread
across both planes, and enemy spawns (mix of THUG + NINJA). Enough content to play
and clear the first level.
