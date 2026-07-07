# Task 3 — UI & Rendering notes

Package: `com.whim.albion.ui`. Depends **only** on `com.whim.albion.api` (the frozen seam)
and its own package. All art is procedural Java2D — no image/audio assets.

## Layout

`GameFrame` is a fixed ~960×640 `JFrame` using `BorderLayout`:

- **CENTER** — a `centerHolder` panel whose single child is swapped by `GameStateType`
  (manual swap on `onStateChanged()`, not `CardLayout`, so each state maps to exactly one
  live panel and dynamic sub-widgets rebuild cleanly):
  - `OVERWORLD` → `TopDownRenderer`
  - `DUNGEON` → `FirstPersonRenderer`
  - `COMBAT` → `CombatPanel`
  - `DIALOGUE` → `DialoguePanel`
  - `INVENTORY` / `CHARACTER_SHEET` / `JOURNAL` → their overlay panels
  - `MENU` → `MenuPanel` (save/load)
  - `TITLE` / `GAME_OVER` → `TitleScreen`
- **EAST** — `PartyPanel`: portrait strip with LP/SP bars (click = select active member,
  double-click = open character sheet), a minimap, and a compass. Shown only in the
  "chrome" states (overworld/dungeon + the three overlays).
- **SOUTH** — action bar: `Look` / `Use` / `Talk` (all route to context-sensitive
  `interact()`), quick overlay toggles, gold readout, and the transient status banner.

Title / dialogue / combat / menu render full-bleed (no EAST/SOUTH chrome).

### Input
Key bindings live on the root panel via `InputMap`/`ActionMap`
(`WHEN_IN_FOCUSED_WINDOW`) so they work regardless of focus:
- Arrows / WASD → `move(dir)` (grid step outdoors; forward-back = step, left-right = turn
  in the dungeon — the controller interprets N/S/E/W accordingly).
- `E` / `Space` → `interact()`.
- `I` `C` `J` `M` toggle overlays; `Esc` → `closeOverlay()`.
- Top-down mouse click → `moveTo(tileX, tileY)`.

The frame registers `controller.addChangeListener(this)` and every rebuild happens on the
EDT (`SwingUtilities.invokeLater` if a change ever arrives off-thread).

## First-person projection (`FirstPersonRenderer`)

Grid-cell "blobber" projection — no raycasting, no textures, just flat-shaded polygons:

1. March forward from `player()` along `facing()` for up to `MAX_DEPTH` (5) cells.
2. Each depth `d` defines a screen slice. A perspective scale `s(d) = 1 / (1 + d·0.85)`
   shrinks the slice toward a central vanishing point; near/far half-width and half-height
   come from `s(d)` and `s(d+1)` times the screen half-extents.
3. For each slice we draw floor + ceiling trapezoids, then check `tileAt` at the cells to
   the **left** and **right** of the current forward cell — if `blocksSight()` (or the tile
   is `WALL`/`DOOR`/`OBSTACLE`) we draw that side wall as a trapezoid, shaded darker with
   depth.
4. We then check the cell **straight ahead** (depth `d+1`): if it blocks sight we draw a
   flat front-wall quad that closes the corridor (with a door glyph for `DOOR` tiles) and
   stop — nothing beyond is visible. `STAIRS` ahead draw a floor marker.

Depth shading multiplies each wall color by `max(0.35, 1 − 0.16·depth)` for a cheap
fog/attenuation feel. Everything keys off `tileAt` + `blocksSight` + `TileType` only, so it
works identically against the stub and the real `WorldModel`.

## SpriteFactory

Maps `spriteKey` / `decorKey` / `portraitKey` strings to deterministic Graphics2D drawings.
Well-known substrings (`tree`, `chest`, `rock`, `stairs`, `sword`, `potion`, `key`, …) get
bespoke shapes; unknown keys hash (FNV-1a) to a stable HSB color + glyph so nothing ever
renders blank and the same key always looks the same across runs.

## StubController

`StubController implements GameController` is a stateful dev fake so the whole shell runs
before the engine/model land. It hosts a town (`OUTDOOR_2D`) and a crypt (`INDOOR_3D`), a
4-member party, a one-quest journal, a small branching dialogue, and a scripted 3-v-2
combat. It is interactive: walk, click-to-move, descend the stairs into the first-person
crypt, loot the vault, talk to NPCs, and trigger the ambush encounter. Launch it with
`UiMain` (temporary — the orchestrator's `app.Main` injects the real `GameEngine`).

## Verification

- `mvn -q compile` clean (Java 8 target, zero external deps).
- Headless state-machine smoke test exercised title→new-game→overworld→overlays and
  verified non-null world/party/journal views and populated inventories.
- Renderers reviewed for null-safety (world/combat/dialogue views may be null off-state and
  are guarded).

## Assumptions / api notes

- The api was sufficient for the full UI; no changes requested.
- `move(Direction)` semantics in the dungeon (forward/back = step, left/right = turn) are
  interpreted by the controller/engine per the contract; the renderer only reads
  `player().facing()`.
