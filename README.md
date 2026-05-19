# Whim Asteroids Demo

Minimal Whim-themed HTML5 canvas game built with vanilla JavaScript and Vite.
This repo is intended as a small, polished base that future coding agents can
extend without first untangling framework or asset complexity. It exists to
demonstrate Whim collaboration through a simple artifact, not to become a large
game engine.

GitHub repo: https://github.com/Whim-Studio/whim-playground

## Run

Requires Node `>=20.19.0`.

```bash
npm install
npm run dev
npm run build
```

## Controls

- The game tutorial appears on first entry. A separate live-preview hint appears
  when the preview becomes active, so users know where changes appear.
- Arrow keys move the Whim face.
- Pointer drag or touch moves toward the target point.
- Space shoots on keyboard.
- The glowing round button starts/restarts and shoots on mobile/coarse-pointer
  screens.
- After a collision, press Enter, Space, any arrow key, or Restart.

## Architecture

- `src/main.js` owns the whole game loop: input, state updates, scoring,
  collision detection, spawning, and canvas rendering.
- `src/whimFaceFrames.js` contains Whim face SVG path frames extracted from
  `../whim-wireframe/assets/landing/face*.svg`. The embedded path data is the
  canonical art copy for this demo; the nearby path is provenance, not a runtime
  dependency.
- `src/styles.css` owns the page chrome: scorebar, overlay, responsive layout,
  and Whim color tokens.
- `index.html` should stay small. Add persistent UI there only when it is not
  part of the canvas scene.

## Agent Handoff Notes

- Simulation uses CSS pixels. `resizeCanvas()` scales the backing canvas for
  device pixel ratio, so game physics should not use raw `canvas.width`.
- Start balancing changes in `GAME_CONFIG` and visual changes in `COLORS`.
  Avoid burying new constants inside update or draw functions.
- Keep the v1 render order clear: background, effects, pointer guide, hazards,
  bullets, player. New layers should be added deliberately to `draw()`.
- Best score is the only persisted state and uses `localStorage` key
  `whim-asteroids-best`.
- Run `npm run build` after changes. For visual changes, also open the dev URL
  and check desktop, phone portrait, and phone landscape widths.
- Keep `node_modules/` and `dist/` out of reviews and commits; they are ignored
  generated/dependency output.
