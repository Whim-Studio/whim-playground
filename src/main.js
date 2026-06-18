import "./styles.css";

/*
 * WhimCode — a Duolingo-style game that teaches JavaScript basics.
 *
 * The whole app is data-driven. Lessons live in LESSONS below: an array of
 * lesson objects, each holding an array of exercises. To add content, append
 * a lesson or an exercise — no rendering code needs to change.
 *
 * Screens (home path, exercise, lesson-complete, lesson-failed) are plain
 * functions that build DOM into #app. State lives in `progress` (persisted to
 * localStorage) and `session` (per-lesson, in-memory).
 */

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

const CONFIG = {
  hearts: 5, // lives per lesson attempt
  xpPerCorrect: 10, // base XP for a correct answer
  streakBonusEvery: 3, // every Nth consecutive correct answer grants a bonus
  streakBonusXp: 5, // extra XP for hitting a streak milestone
};

const STORAGE_KEY = "whimcode.progress.v1";

// ---------------------------------------------------------------------------
// Lesson + exercise content
//
// Exercise types:
//   "choice"  — multiple choice. options: string[], answer: index.
//   "blocks"  — tap tokens in order to form `solution` (array of tokens).
//               pool may add distractor tokens; defaults to the solution.
//   "blank"   — fill the blank. code uses ___ ; options: string[], answer: index.
//   "bug"     — spot the broken line. lines: string[], answer: index (buggy line).
// Every exercise has an `explain` shown after answering.
// ---------------------------------------------------------------------------

const LESSONS = [
  {
    id: "variables",
    title: "Variables",
    icon: "📦",
    blurb: "Store and name values with let and const.",
    exercises: [
      {
        type: "choice",
        prompt: "What does this print?",
        code: "let score = 5;\nscore = score + 3;\nconsole.log(score);",
        options: ["5", "8", "53", "undefined"],
        answer: 1,
        explain: "score starts at 5, then we reassign it to 5 + 3, which is 8.",
      },
      {
        type: "blank",
        prompt: "Pick the keyword to declare a value that never changes.",
        code: "___ PI = 3.14;",
        options: ["const", "let", "var", "fixed"],
        answer: 0,
        explain: "const declares a constant — it can't be reassigned later.",
      },
      {
        type: "blocks",
        prompt: "Arrange the tokens into a valid variable declaration.",
        solution: ["let", "name", "=", '"Ada"', ";"],
        explain: 'let name = "Ada"; declares a variable and assigns a string.',
      },
      {
        type: "bug",
        prompt: "Which line has the bug?",
        lines: ["let count = 0;", "count = count + 1;", "const count = 2;"],
        answer: 2,
        explain: "count is already declared, so re-declaring it with const throws an error.",
      },
      {
        type: "choice",
        prompt: "What is the value of total?",
        code: 'const a = 4;\nconst b = "4";\nconst total = a + b;',
        options: ["8", '"44"', '"8"', "44"],
        answer: 1,
        explain: 'Adding a number and a string joins them as text, so 4 + "4" is "44".',
      },
    ],
  },
  {
    id: "strings",
    title: "Strings",
    icon: "🔤",
    blurb: "Join, measure, and read text.",
    exercises: [
      {
        type: "choice",
        prompt: "What does this print?",
        code: 'const name = "Sky";\nconsole.log("Hi " + name + "!");',
        options: ["Hi Sky!", "Hi + name!", "HiSky", "Hi name!"],
        answer: 0,
        explain: 'The + operator joins strings, building "Hi Sky!".',
      },
      {
        type: "blank",
        prompt: "Fill the blank to read the text length.",
        code: 'const word = "code";\nconsole.log(word.___);',
        options: ["length", "size", "count", "len"],
        answer: 0,
        explain: 'Strings expose a .length property — "code".length is 4.',
      },
      {
        type: "blocks",
        prompt: "Build a template literal that greets the user.",
        solution: ["`Hello", "${user}", "`"],
        pool: ["`Hello", "${user}", "`", '"user"'],
        explain: "Backticks make a template literal; ${user} inserts the variable.",
      },
      {
        type: "bug",
        prompt: "Which line is broken?",
        lines: ['let a = "hi";', "let b = 'bye';", 'let c = "oops;'],
        answer: 2,
        explain: 'The last string is missing its closing quote: "oops;.',
      },
      {
        type: "choice",
        prompt: "What does this print?",
        code: 'console.log("abc".toUpperCase());',
        options: ["abc", "ABC", "Abc", "error"],
        answer: 1,
        explain: "toUpperCase() returns a new string with every letter capitalized.",
      },
    ],
  },
  {
    id: "conditionals",
    title: "Conditionals",
    icon: "🔀",
    blurb: "Make decisions with if and else.",
    exercises: [
      {
        type: "choice",
        prompt: "What does this print?",
        code: 'const n = 7;\nif (n > 5) {\n  console.log("big");\n} else {\n  console.log("small");\n}',
        options: ["big", "small", "7", "nothing"],
        answer: 0,
        explain: '7 > 5 is true, so the if branch runs and prints "big".',
      },
      {
        type: "blank",
        prompt: "Pick the operator that checks for equal value and type.",
        code: 'if (age ___ 18) {\n  console.log("exact");\n}',
        options: ["===", "=", "=>", "<>"],
        answer: 0,
        explain: "=== is strict equality. A single = assigns instead of comparing.",
      },
      {
        type: "bug",
        prompt: "Which line has the bug?",
        lines: ["if (x = 5) {", '  console.log("five");', "}"],
        answer: 0,
        explain: "x = 5 assigns 5 instead of comparing. Use === to compare.",
      },
      {
        type: "blocks",
        prompt: "Arrange a condition that runs when ready is true.",
        solution: ["if", "(", "ready", ")", "{"],
        explain: "if (ready) { ... } runs the block when ready is truthy.",
      },
      {
        type: "choice",
        prompt: "What does this print?",
        code: 'const score = 3;\nconsole.log(score >= 5 ? "pass" : "fail");',
        options: ["pass", "fail", "3", "true"],
        answer: 1,
        explain: '3 >= 5 is false, so the ternary returns the second value, "fail".',
      },
    ],
  },
  {
    id: "loops",
    title: "Loops",
    icon: "🔁",
    blurb: "Repeat work with for and while.",
    exercises: [
      {
        type: "choice",
        prompt: "How many times does this print?",
        code: "for (let i = 0; i < 3; i++) {\n  console.log(i);\n}",
        options: ["2 times", "3 times", "4 times", "forever"],
        answer: 1,
        explain: "i runs 0, 1, 2 — three iterations — then stops when i reaches 3.",
      },
      {
        type: "blank",
        prompt: "Fill the blank so the loop counts up.",
        code: "for (let i = 0; i < 5; ___) {\n  console.log(i);\n}",
        options: ["i++", "i--", "i", "i + 1"],
        answer: 0,
        explain: "i++ increases i by 1 each pass so the loop eventually ends.",
      },
      {
        type: "bug",
        prompt: "Which line causes an infinite loop?",
        lines: ["let i = 0;", "while (i < 3) {", "  console.log(i);", "}"],
        answer: 3,
        explain: "i is never increased inside the loop, so i < 3 stays true forever.",
      },
      {
        type: "blocks",
        prompt: "Build the start of a for loop.",
        solution: ["for", "(", "let", "i", "=", "0", ";"],
        explain: "A for loop begins with an initializer: for (let i = 0; ...).",
      },
      {
        type: "choice",
        prompt: "What is the last value printed?",
        code: "for (let i = 1; i <= 3; i++) {\n  console.log(i * 2);\n}",
        options: ["3", "4", "6", "8"],
        answer: 2,
        explain: "The loop prints 2, 4, 6. The last value is 3 * 2 = 6.",
      },
    ],
  },
  {
    id: "functions",
    title: "Functions",
    icon: "⚙️",
    blurb: "Package logic you can reuse.",
    exercises: [
      {
        type: "choice",
        prompt: "What does this print?",
        code: "function add(a, b) {\n  return a + b;\n}\nconsole.log(add(2, 4));",
        options: ["2", "4", "6", "24"],
        answer: 2,
        explain: "add(2, 4) returns 2 + 4, so console.log prints 6.",
      },
      {
        type: "blank",
        prompt: "Pick the keyword that sends a value back from a function.",
        code: "function double(n) {\n  ___ n * 2;\n}",
        options: ["return", "give", "out", "send"],
        answer: 0,
        explain: "return hands a value back to whoever called the function.",
      },
      {
        type: "blocks",
        prompt: "Arrange a one-line arrow function.",
        solution: ["const", "square", "=", "n", "=>", "n * n", ";"],
        explain: "Arrow functions are compact: const square = n => n * n;",
      },
      {
        type: "bug",
        prompt: "Which line has the bug?",
        lines: ["function greet(name) {", '  console.log("Hi " + name)', "}", "greet;"],
        answer: 3,
        explain: "greet; references the function but never calls it. Use greet().",
      },
      {
        type: "choice",
        prompt: "What does add() print here?",
        code: "function add(a, b = 1) {\n  return a + b;\n}\nconsole.log(add(5));",
        options: ["5", "6", "1", "NaN"],
        answer: 1,
        explain: "b defaults to 1 when no second argument is passed, so 5 + 1 is 6.",
      },
    ],
  },
];

// ---------------------------------------------------------------------------
// Persistent progress
// ---------------------------------------------------------------------------

function defaultProgress() {
  return { completed: [], totalXp: 0 };
}

function loadProgress() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return defaultProgress();
    const parsed = JSON.parse(raw);
    return {
      completed: Array.isArray(parsed.completed) ? parsed.completed : [],
      totalXp: Number.isFinite(parsed.totalXp) ? parsed.totalXp : 0,
    };
  } catch {
    return defaultProgress();
  }
}

function saveProgress() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
  } catch {
    /* storage unavailable — progress simply won't persist */
  }
}

let progress = loadProgress();

// Per-lesson session state, set up when a lesson starts.
let session = null;

// ---------------------------------------------------------------------------
// Lesson state helpers
// ---------------------------------------------------------------------------

function lessonState(index) {
  const lesson = LESSONS[index];
  if (progress.completed.includes(lesson.id)) return "completed";
  if (index === 0) return "unlocked";
  const prev = LESSONS[index - 1];
  return progress.completed.includes(prev.id) ? "unlocked" : "locked";
}

// ---------------------------------------------------------------------------
// Small DOM helpers
// ---------------------------------------------------------------------------

const app = document.getElementById("app");

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

function clear(node) {
  node.replaceChildren();
}

function shuffle(items) {
  const out = items.slice();
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j], out[i]];
  }
  return out;
}

function changeGameCta() {
  const cta = el("a", "change-game-cta", "Change the game");
  cta.href = "https://whim.run/";
  cta.target = "_blank";
  cta.rel = "noopener noreferrer";
  return cta;
}

// ---------------------------------------------------------------------------
// Home / lesson path screen
// ---------------------------------------------------------------------------

function renderHome() {
  session = null;
  clear(app);

  const shell = el("div", "screen screen--home");

  const top = el("header", "topbar");
  top.append(changeGameCta());
  const xpPill = el("div", "xp-pill");
  xpPill.append(
    el("span", "xp-pill__icon", "✦"),
    el("strong", null, String(progress.totalXp)),
    el("span", "xp-pill__label", "XP"),
  );
  top.append(xpPill);
  shell.append(top);

  const hero = el("div", "hero");
  hero.append(el("h1", "hero__title", "WhimCode"));
  hero.append(el("p", "hero__subtitle", "Learn JavaScript one bite at a time."));
  shell.append(hero);

  const path = el("ol", "path");
  LESSONS.forEach((lesson, index) => {
    const state = lessonState(index);
    const node = el("li", `path-node path-node--${state}`);

    const badge = el("div", "path-node__badge");
    badge.textContent = state === "locked" ? "🔒" : state === "completed" ? "✓" : lesson.icon;
    node.append(badge);

    const body = el("div", "path-node__body");
    body.append(el("span", "path-node__title", lesson.title));
    body.append(el("span", "path-node__blurb", lesson.blurb));
    const meta = el("span", "path-node__meta");
    meta.textContent =
      state === "completed"
        ? `Completed · ${lesson.exercises.length} exercises`
        : state === "locked"
          ? "Locked"
          : `${lesson.exercises.length} exercises`;
    body.append(meta);
    node.append(body);

    if (state !== "locked") {
      const btn = el("button", "path-node__cta", state === "completed" ? "Review" : "Start");
      btn.type = "button";
      btn.addEventListener("click", () => startLesson(index));
      node.append(btn);
      node.classList.add("path-node--clickable");
    }

    path.append(node);
  });
  shell.append(path);

  app.append(shell);
}

// ---------------------------------------------------------------------------
// Lesson session + exercise screen
// ---------------------------------------------------------------------------

function startLesson(index) {
  const lesson = LESSONS[index];
  session = {
    lessonIndex: index,
    lesson,
    exerciseIndex: 0,
    hearts: CONFIG.hearts,
    xpEarned: 0,
    streak: 0,
    correctCount: 0,
    answered: 0,
  };
  renderExercise();
}

function renderExercise() {
  clear(app);
  const { lesson, exerciseIndex } = session;
  const exercise = lesson.exercises[exerciseIndex];

  const shell = el("div", "screen screen--exercise");

  // Header: quit, progress bar, hearts.
  const head = el("header", "ex-head");
  const quit = el("button", "icon-btn", "✕");
  quit.type = "button";
  quit.setAttribute("aria-label", "Quit lesson");
  quit.addEventListener("click", renderHome);
  head.append(quit);

  const bar = el("div", "progress");
  const fill = el("div", "progress__fill");
  fill.style.width = `${(exerciseIndex / lesson.exercises.length) * 100}%`;
  bar.append(fill);
  head.append(bar);

  const hearts = el("div", "hearts");
  hearts.append(el("span", "hearts__icon", "❤"), el("strong", null, String(session.hearts)));
  head.append(hearts);
  shell.append(head);

  // Prompt + body.
  const card = el("div", "ex-card");
  card.append(
    el("span", "ex-card__kicker", `${lesson.title} · ${exerciseIndex + 1}/${lesson.exercises.length}`),
  );
  card.append(el("h2", "ex-card__prompt", exercise.prompt));
  if (exercise.code) {
    const pre = el("pre", "code-block");
    pre.append(el("code", null, exercise.code));
    card.append(pre);
  }

  const interactive = el("div", "ex-card__interactive");
  card.append(interactive);
  shell.append(card);

  // Feedback footer (hidden until answered).
  const footer = el("div", "ex-foot");
  footer.hidden = true;
  shell.append(footer);

  app.append(shell);

  const handlers = {
    choice: renderChoice,
    blank: renderChoice, // blank reuses the option-button UI
    bug: renderBug,
    blocks: renderBlocks,
  };
  (handlers[exercise.type] || renderChoice)(interactive, exercise, footer);
}

// Multiple choice & fill-in-the-blank share an option-button list.
function renderChoice(container, exercise, footer) {
  const options = el("div", "options");
  exercise.options.forEach((label, i) => {
    const btn = el("button", "option", label);
    btn.type = "button";
    btn.addEventListener("click", () => {
      const correct = i === exercise.answer;
      [...options.children].forEach((child, idx) => {
        child.disabled = true;
        if (idx === exercise.answer) child.classList.add("is-correct");
        else if (child === btn) child.classList.add("is-wrong");
      });
      finishExercise(correct, exercise, footer);
    });
    options.append(btn);
  });
  container.append(options);
}

function renderBug(container, exercise, footer) {
  const list = el("div", "bug-lines");
  exercise.lines.forEach((line, i) => {
    const btn = el("button", "bug-line");
    btn.type = "button";
    btn.append(el("span", "bug-line__num", String(i + 1)));
    btn.append(el("code", "bug-line__code", line));
    btn.addEventListener("click", () => {
      const correct = i === exercise.answer;
      [...list.children].forEach((child, idx) => {
        child.disabled = true;
        if (idx === exercise.answer) child.classList.add("is-correct");
        else if (idx === i) child.classList.add("is-wrong");
      });
      finishExercise(correct, exercise, footer);
    });
    list.append(btn);
  });
  container.append(list);
}

function renderBlocks(container, exercise, footer) {
  const solution = exercise.solution;
  const pool = shuffle(exercise.pool || solution);

  const answer = el("div", "blocks-answer");
  const placeholder = el("span", "blocks-answer__hint", "Tap tokens to build the line");
  answer.append(placeholder);

  const tray = el("div", "blocks-tray");
  const picked = []; // { label, source } in chosen order

  function refreshAnswer() {
    clear(answer);
    if (picked.length === 0) {
      answer.append(placeholder);
      return;
    }
    picked.forEach((entry) => {
      const chip = el("button", "token token--picked", entry.label);
      chip.type = "button";
      chip.addEventListener("click", () => {
        entry.source.disabled = false;
        picked.splice(picked.indexOf(entry), 1);
        refreshAnswer();
        updateCheck();
      });
      answer.append(chip);
    });
  }

  pool.forEach((label) => {
    const chip = el("button", "token", label);
    chip.type = "button";
    chip.addEventListener("click", () => {
      chip.disabled = true;
      picked.push({ label, source: chip });
      refreshAnswer();
      updateCheck();
    });
    tray.append(chip);
  });

  const check = el("button", "check-btn", "Check");
  check.type = "button";
  check.disabled = true;
  function updateCheck() {
    check.disabled = picked.length !== solution.length;
  }
  check.addEventListener("click", () => {
    const built = picked.map((p) => p.label);
    const correct = built.length === solution.length && built.every((t, i) => t === solution[i]);
    [...tray.children].forEach((c) => (c.disabled = true));
    [...answer.children].forEach((c) => (c.disabled = true));
    check.hidden = true;
    answer.classList.add(correct ? "is-correct" : "is-wrong");
    if (!correct) {
      const sol = el("div", "blocks-solution");
      sol.append(el("span", "blocks-solution__label", "Answer:"), el("code", null, solution.join(" ")));
      container.append(sol);
    }
    finishExercise(correct, exercise, footer);
  });

  container.append(answer, tray, check);
}

// ---------------------------------------------------------------------------
// Scoring + feedback
// ---------------------------------------------------------------------------

function finishExercise(correct, exercise, footer) {
  session.answered += 1;

  if (correct) {
    session.streak += 1;
    session.correctCount += 1;
    let gained = CONFIG.xpPerCorrect;
    const milestone = session.streak % CONFIG.streakBonusEvery === 0;
    if (milestone) gained += CONFIG.streakBonusXp;
    session.xpEarned += gained;
    showFeedback(footer, true, exercise.explain, gained, milestone);
  } else {
    session.streak = 0;
    session.hearts -= 1;
    showFeedback(footer, false, exercise.explain, 0, false);
  }
}

function showFeedback(footer, correct, explain, gained, milestone) {
  footer.hidden = false;
  footer.className = `ex-foot ex-foot--${correct ? "correct" : "wrong"}`;
  clear(footer);

  const head = el("div", "ex-foot__head");
  head.append(el("span", "ex-foot__icon", correct ? "✓" : "✕"));
  const headText = el("div", "ex-foot__headtext");
  headText.append(el("strong", null, correct ? "Correct!" : "Not quite"));
  if (correct && gained > 0) {
    const note = milestone ? ` · 🔥 ${session.streak} streak` : "";
    headText.append(el("span", "ex-foot__xp", `+${gained} XP${note}`));
  }
  head.append(headText);
  footer.append(head);

  footer.append(el("p", "ex-foot__explain", explain));

  const cont = el("button", "continue-btn", "Continue");
  cont.type = "button";
  cont.addEventListener("click", advance);
  footer.append(cont);
  cont.focus();
}

function advance() {
  if (session.hearts <= 0) {
    renderFailed();
    return;
  }
  session.exerciseIndex += 1;
  if (session.exerciseIndex >= session.lesson.exercises.length) {
    completeLesson();
    return;
  }
  renderExercise();
}

// ---------------------------------------------------------------------------
// Completion + failure screens
// ---------------------------------------------------------------------------

function completeLesson() {
  const { lesson } = session;
  if (!progress.completed.includes(lesson.id)) {
    progress.completed.push(lesson.id);
  }
  progress.totalXp += session.xpEarned;
  saveProgress();

  const accuracy = Math.round((session.correctCount / session.answered) * 100);

  clear(app);
  const shell = el("div", "screen screen--result");
  const panel = el("div", "result-card");

  panel.append(el("div", "result-card__emoji", "🎉"));
  panel.append(el("h2", "result-card__title", "Lesson complete!"));
  panel.append(el("p", "result-card__sub", `${lesson.title} cleared`));

  const stats = el("div", "result-stats");
  stats.append(statTile("XP earned", `+${session.xpEarned}`));
  stats.append(statTile("Accuracy", `${accuracy}%`));
  stats.append(statTile("Correct", `${session.correctCount}/${session.answered}`));
  panel.append(stats);

  const nextIndex = session.lessonIndex + 1;
  const hasNext = nextIndex < LESSONS.length;
  const cta = el("button", "primary-btn", hasNext ? "Back to path" : "Finish");
  cta.type = "button";
  cta.addEventListener("click", renderHome);
  panel.append(cta);

  if (hasNext) {
    panel.append(el("p", "result-card__note", `Next up: ${LESSONS[nextIndex].title} is now unlocked.`));
  }

  shell.append(panel);
  app.append(shell);
}

function renderFailed() {
  const { lesson } = session;
  clear(app);
  const shell = el("div", "screen screen--result");
  const panel = el("div", "result-card");

  panel.append(el("div", "result-card__emoji", "💔"));
  panel.append(el("h2", "result-card__title", "Out of hearts"));
  panel.append(el("p", "result-card__sub", `You ran out on ${lesson.title}. Give it another go!`));

  const stats = el("div", "result-stats");
  stats.append(statTile("Reached", `${session.exerciseIndex + 1}/${lesson.exercises.length}`));
  stats.append(statTile("Correct", String(session.correctCount)));
  panel.append(stats);

  const retry = el("button", "primary-btn", "Retry lesson");
  retry.type = "button";
  retry.addEventListener("click", () => startLesson(session.lessonIndex));
  panel.append(retry);

  const back = el("button", "ghost-btn", "Back to path");
  back.type = "button";
  back.addEventListener("click", renderHome);
  panel.append(back);

  shell.append(panel);
  app.append(shell);
}

function statTile(label, value) {
  const tile = el("div", "stat-tile");
  tile.append(el("strong", "stat-tile__value", value));
  tile.append(el("span", "stat-tile__label", label));
  return tile;
}

// ---------------------------------------------------------------------------
// Boot
// ---------------------------------------------------------------------------

renderHome();
