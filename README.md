# WhimCode Demo

A minimal, Whim-themed, **Duolingo-style game that teaches JavaScript basics**,
built with vanilla JavaScript and Vite. This repo is a small, polished base that
future coding agents can extend without untangling framework or asset
complexity. It exists to demonstrate Whim collaboration through a simple
artifact, not to become a large learning platform.

GitHub repo: https://github.com/Whim-Studio/whim-playground

## Run

Requires Node `>=20.19.0`.

```bash
npm install
npm run dev    # dev server on 0.0.0.0:5173
npm run build  # production build to dist/
```

## How to play

- The home screen shows a **lesson path**: Variables → Strings → Conditionals →
  Loops → Functions. Lessons unlock as you complete the one before.
- Each lesson is a handful of bite-sized exercises mixing four types:
  multiple choice, tap-the-blocks, fill-in-the-blank, and spot-the-bug.
- You start each lesson with **5 hearts**; a wrong answer costs one. Run out and
  you get a retry screen. Correct answers earn **XP** and build a **streak**
  (every 3rd correct answer grants a bonus).
- A **progress bar** fills as you answer, instant feedback explains each answer,
  and a completion screen celebrates the lesson and unlocks the next.
- Total XP and completed lessons persist in `localStorage`, so progress survives
  reloads.
- The "Change the game" link (top-left on the path screen) points to
  https://whim.run/.

## Architecture

- `src/main.js` owns the whole app: the `LESSONS` content data structure, plus
  the screen renderers (home path, exercise, lesson-complete, lesson-failed),
  scoring/hearts/streak mechanics, and `localStorage` persistence. Screens are
  plain functions that build DOM into `#app`.
- `src/styles.css` owns the page chrome: path nodes, exercise card, code blocks,
  feedback footer, result cards, responsive layout, and the Whim color tokens.
- `index.html` is a tiny shell — just the `#app` mount point and the module
  script. WhimCode renders everything from JS.
- `src/whimFaceFrames.js` is a leftover art asset from the previous Asteroids
  demo and is no longer imported. Leave it or remove it as future tasks see fit.

## Adding content

Lessons are pure data. To add a lesson, append an object to `LESSONS` in
`src/main.js` with an `id`, `title`, `icon`, `blurb`, and an `exercises` array.
Each exercise has a `type` (`choice`, `blank`, `blocks`, or `bug`), a `prompt`,
optional `code`, the type-specific fields, and an `explain` string. No rendering
code needs to change.

## Agent Handoff Notes

- Keep the project vanilla JS + Vite. Do not add React, TypeScript, frameworks,
  state libraries, or asset pipelines unless a task truly needs it.
- Tune gameplay in the `CONFIG` constant (hearts, XP, streak bonus) and content
  in `LESSONS`. Avoid burying new constants inside renderers.
- Keep questions genuinely correct and beginner-appropriate; render code in the
  monospace `.code-block`.
- Run `npm run build` after changes. For visual changes, also open the dev URL
  and check desktop, phone portrait, and phone landscape widths.
- Keep `node_modules/` and `dist/` out of reviews and commits.
