# CLAUDE.md

Guidance for Claude agents working in this repo.

## Project Purpose

This is a small Whim demo game. Its job is to show Whim's collaborative coding
workflow and general product feel through a fun, polished artifact that remains
easy for future agents and demo users to understand.

GitHub repo: https://github.com/Whim-Studio/whim-playground

Keep the project a simple HTML5 canvas game built with vanilla JavaScript. Vite
is only the dev/build shell. Do not introduce React, TypeScript, a game engine,
state libraries, asset pipelines, or broad tooling unless the user explicitly
asks and the task truly needs it.

## Working Style

- Make the smallest scoped code change that satisfies the current demo goal.
- Preserve the current design direction: minimal, Whim-themed, polished, and
  easy to extend.
- Prefer clear constants and small helpers over clever abstractions.
- Keep token usage in mind. Read only the files needed for the task, summarize
  findings briefly, and avoid large rewrites when a focused edit is enough.
- Ignore generated/dependency output such as `node_modules/` and `dist/` unless
  the task is specifically about build artifacts.
- Do not optimize for a theoretical full game yet. This repo is a base for demo
  tasks, iteration, and collaborative handoff.

## Code Map

- `src/main.js`: game loop, input, scoring, spawning, collision, and canvas
  rendering.
- `src/whimFaceFrames.js`: embedded Whim face path data extracted from nearby
  Whim assets. Treat this file as the canonical art copy for the demo.
- `src/styles.css`: scorebar, overlay, responsive page layout, and Whim color
  tokens.
- `index.html`: minimal document shell. Add durable UI here only when it should
  live outside the canvas scene.

## Extension Rules

- Keep simulation units in CSS pixels. `resizeCanvas()` handles DPR scaling.
- Start gameplay tuning in `GAME_CONFIG` and visual tuning in `COLORS`.
- Keep draw order intentional: background, effects, pointer guide, hazards,
  bullets, player.
- Keep gameplay instructions available on first entry, and keep the separate
  live-preview hint tied to preview activation so demo users know where changes
  appear.
- Add comments only where they help the next agent safely modify behavior.
- Keep documentation minimal but current. Update `README.md` or this file when
  the architecture, run commands, or collaboration expectations change.

## Verification

Use Node `>=20.19.0`. Run `npm run build` after code changes. For visual or
gameplay changes, also open the dev server and check desktop, phone portrait,
and phone landscape widths.

After any change that affects the demo, ensure the live Vite server is running
before finishing so the shared Whim preview updates automatically. Prefer
`npm run dev`; if a server is already running, reuse it instead of starting a
duplicate. Confirm the preview responds and include the local URL in the final
handoff. Use `npm run preview` only when specifically checking production build
output.
