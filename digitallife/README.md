# thisisyourdigitallife — a local personality quiz

A standalone **Java 8 / Swing** desktop personality-quiz app inspired by the
*question → answer → scored result → shareable summary* format of Facebook's
"thisisyourdigitallife" quiz — rebuilt from scratch as an **original,
fully offline, single-user** application.

> ⚠️ This app does **not** connect to Facebook or any social network, performs
> **no networking of any kind**, and only ever touches the answers of the single
> local user running it. Everything stays on your own machine. "Saving" and
> "loading" results simply read/write a plain `.txt` file you choose yourself.

## Features

- **Welcome screen** — title, description, drawn logo mark, `Start Quiz`, and
  `Load Previous Results`.
- **12 multiple-choice questions**, one per screen, with a live progress
  indicator (`Question 4 of 12`) and a Graphics2D progress bar.
- **Back / Next navigation** — answers are preserved when you go back.
- **Results screen** — dominant trait, a generated summary paragraph, and a
  hand-drawn **Graphics2D bar chart** of all five trait dimensions (no external
  chart libraries).
- **Save My Results** — writes your answers + computed scores to a local `.txt`
  file via `JFileChooser`.
- **Load Previous Results** — reads a saved file back onto the results screen.
- **Retake Quiz** — resets and starts over.

## Requirements

- A **Java 8+** JDK (`javac` / `java`). Compiles cleanly with `--release 8`;
  runs on any later JRE too. No Maven/Gradle and **no third-party libraries** —
  JDK standard library only.

## Build & run

From the `digitallife/` directory:

```bash
# 1. compile every source file into ./out
mkdir -p out
find src -name '*.java' > sources.txt
javac -d out @sources.txt

# 2. run
java -cp out com.whim.digitallife.Main
```

Or use the convenience script:

```bash
./run.sh
```

To pin the Java 8 language level explicitly (JDK 9+):

```bash
javac --release 8 -d out @sources.txt
```

## Project structure

```
digitallife/
├── README.md
├── run.sh                       # compile + launch helper
└── src/com/whim/digitallife/
    ├── Main.java                # entry point (launches Swing on the EDT)
    ├── model/
    │   ├── Trait.java           # the 5 personality dimensions (enum + colors)
    │   ├── Choice.java          # one answer option + its trait weights
    │   ├── Question.java        # a prompt + list of choices
    │   └── ResultProfile.java   # computed scores, dominant trait, summary
    ├── quiz/
    │   ├── QuizData.java        # the 12 questions (content only)
    │   ├── ScoringEngine.java   # pure scoring + normalization logic
    │   └── QuizController.java  # state, navigation, answer storage
    ├── io/
    │   └── ResultsIO.java       # local .txt save / load (no network)
    └── ui/
        ├── QuizFrame.java       # CardLayout host + navigation wiring
        ├── Theme.java           # colors, fonts, styled-button factories
        ├── WelcomeScreen.java   # start screen + load
        ├── QuestionScreen.java  # one question at a time + progress
        ├── ResultScreen.java    # summary + save/retake
        └── BarChartPanel.java   # Graphics2D trait bar chart
```

## Architecture (MVC-ish)

- **Model** (`model/`): plain immutable data — `Trait`, `Choice`, `Question`,
  `ResultProfile`. No UI or IO.
- **Controller / logic** (`quiz/`): `QuizController` owns navigation and the
  answer array; `ScoringEngine` turns answers into a `ResultProfile`;
  `QuizData` holds question content.
- **View** (`ui/`): Swing screens driven by a `CardLayout` in `QuizFrame`. The
  views read from the controller and call back into the frame to navigate.
- **IO** (`io/`): `ResultsIO` handles local, offline text persistence only.

---

## Design note — scoring model & trait categories

The quiz scores five dimensions, a friendly re-interpretation of the well-known
**Big Five (OCEAN)** personality model — chosen because it is widely recognized,
easy to explain, and maps naturally onto multiple-choice lifestyle questions:

| Dimension            | In this app means…                                  |
|----------------------|-----------------------------------------------------|
| **Openness**         | curiosity, imagination, love of novelty             |
| **Conscientiousness**| organization, reliability, goal focus               |
| **Extraversion**     | sociability, energy from people                     |
| **Agreeableness**    | warmth, cooperation, consideration                  |
| **Emotional Stability** | calm, resilience, steadiness under pressure      |

**How scoring works.** Each answer `Choice` carries a small map of trait → points
(typically 0–2), and a single choice may feed more than one trait (e.g. "rally a
team" adds to both Extraversion and Agreeableness). Selecting an answer adds its
weights to running per-trait totals in `ScoringEngine`.

**Normalization.** To draw a fair 0–100% bar chart, `ScoringEngine` computes a
per-trait *ceiling* — the score you'd reach if you always picked that trait's
best-weighted option on every question — and reports each trait as a percentage
of that ceiling. The **dominant trait** is simply the highest raw total (ties
broken by a stable enum order), and the summary paragraph is generated from the
dominant plus runner-up trait.

Why this design: it keeps the model transparent and fully deterministic (a saved
file round-trips back to the exact same profile), needs no data about anyone but
the local user, and is trivial to extend — add a `Question` to `QuizData` or a
value to the `Trait` enum and everything else adapts automatically.
