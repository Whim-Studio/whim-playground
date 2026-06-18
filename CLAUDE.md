# CLAUDE.md

Guidance for Claude agents working in this repo.

## Project Purpose

This is a small Whim demo. Its job is to show Whim's collaborative coding
workflow and general product feel through a fun, polished artifact that remains
easy for future agents and demo users to understand.

GitHub repo: https://github.com/Whim-Studio/whim-playground

The demo is **WhimCode**: a Duolingo-style game that teaches JavaScript basics
through bite-sized, lesson-based exercises (a lesson path, hearts, XP, streaks,
instant feedback, and a progress bar).

Keep the project a single-page HTML app built with vanilla JavaScript. Vite is
only the dev/build shell. Do not introduce React, TypeScript, frameworks, state
libraries, asset pipelines, or broad tooling unless the user explicitly asks and
the task truly needs it.

## Working Style

- Make the smallest scoped code change that satisfies the current demo goal.
- Preserve the current design direction: minimal, Whim-themed, polished, and
  easy to extend.
- Prefer clear constants and small helpers over clever abstractions.
- Keep token usage in mind. Read only the files needed for the task, summarize
  findings briefly, and avoid large rewrites when a focused edit is enough.
- Ignore generated/dependency output such as `node_modules/` and `dist/` unless
  the task is specifically about build artifacts.

## Code Map

- `src/main.js`: the whole app. Holds the `LESSONS` content data structure and
  the `CONFIG` constants, plus screen renderers (home path, exercise,
  lesson-complete, lesson-failed), scoring/hearts/streak mechanics, and
  `localStorage` persistence. Screens are plain functions building DOM into
  `#app`.
- `src/styles.css`: page chrome — path nodes, exercise card, code blocks,
  feedback footer, result cards, responsive layout, and the Whim color tokens.
- `index.html`: minimal document shell — just the `#app` mount and module
  script. Add durable UI in JS, not here.
- `src/whimFaceFrames.js`: leftover art from the previous Asteroids demo, no
  longer imported. Leave or remove as a task sees fit.

## Extension Rules

- Lessons and exercises are pure data. Add content by appending to `LESSONS`;
  no rendering code should need to change. Each exercise `type` is one of
  `choice`, `blank`, `blocks`, or `bug` and carries an `explain` string.
- Tune mechanics (hearts, XP, streak bonus) in `CONFIG`. Don't bury new tuning
  constants inside renderers.
- Keep questions genuinely correct and beginner-appropriate. Render code in the
  monospace `.code-block`.
- Keep the "Change the game" CTA (to https://whim.run/) visible on the home
  screen.
- Bump `STORAGE_KEY` if you change the persisted progress shape.
- Add comments only where they help the next agent safely modify behavior.
- Keep documentation minimal but current. Update `README.md` or this file when
  the architecture, run commands, or collaboration expectations change.

## Verification

Use Node `>=20.19.0`. Run `npm run build` after code changes. For visual or
gameplay changes, also open the dev server and check desktop, phone portrait,
and phone landscape widths, and play through at least one full lesson.

After any change that affects the demo, ensure the live Vite server is running
before finishing so the shared Whim preview updates automatically. Prefer
`npm run dev` (host `0.0.0.0`); if a server is already running, reuse it instead
of starting a duplicate. Confirm the preview responds and include the local URL
in the final handoff. Use `npm run preview` only when checking production output.
