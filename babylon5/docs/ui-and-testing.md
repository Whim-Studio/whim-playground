# Babylon 5 CCG — UI, Threading & Testing Plan (Task 3)

## 1. UI wireframe → Swing component map

```
┌───────────────────────────────────────────────┬──────────────────┐
│ BABYLON 5 — CCG            Turn N · Phase · Act │   Game Log       │
├───────────────────────────────────────────────┤  (JTextArea in   │
│  OPPONENTS  (JScrollPane > BoxLayout Y)         │   JScrollPane,   │
│   ┌── AI 1 PlayerPanel ─────────────────────┐  │   GameListener   │
│   │ header: name/faction/influence/power    │  │   mirror)        │
│   │ Inner Circle row  (FlowLayout CardView) │  │                  │
│   │ Supporting  row   (FlowLayout CardView) │  │                  │
│   └─────────────────────────────────────────┘  │                  │
│   AI 2 PlayerPanel … AI 3 PlayerPanel           │                  │
│ ┌── Conflict Resolution (TitledBorder) ───────┐ │                  │
│ │  centerLabel: last ConflictResult / winner  │ │                  │
│ └─────────────────────────────────────────────┘ │                  │
│  YOUR Inner Circle / Supporting (PlayerPanel)   │                  │
│  YOUR HAND  (JScrollPane > FlowLayout CardView) │                  │
├───────────────────────────────────────────────┴──────────────────┤
│ [Advance Phase ▶] [Sponsor Selected] [Declare Conflict]   hint     │
└────────────────────────────────────────────────────────────────────┘
```

| Region | Swing component | Source |
| --- | --- | --- |
| Window | `MainWindow extends JFrame` | `ui/MainWindow.java` |
| Player region | `PlayerPanel extends JPanel` (custom `paintComponent`) | `ui/PlayerPanel.java` |
| Single card | `CardView extends JComponent` (Java2D + `ImageLoader`) | `ui/CardView.java` |
| Palette/fonts | `UiTheme` | `ui/UiTheme.java` |
| Conflict area | `JLabel` inside a `TitledBorder` | MainWindow `centerLabel` |
| Influence/Power trackers | per-player HTML `JLabel` header | PlayerPanel header |
| Log | `JTextArea` in `JScrollPane` | MainWindow sidebar |
| Controls | `JButton` ×3 in a `FlowLayout` bar | MainWindow `buildControls` |

Card art is fetched/cached by `data.ImageLoader` (placeholder until loaded); no copyrighted
bytes are embedded. Conflict types and zones are colour-coded via `UiTheme`.

## 2. Threading model (EDT safety)

- `start()` shows the frame on the EDT, then hands control to a single **daemon worker
  thread** (`b5-engine`, an `ExecutorService`).
- **All** `GameEngine` / `AIPlayer` work — `advancePhase`, `sponsorCharacter`,
  `resolveConflict`, `runAiTurn`, `checkVictory` — runs on that worker via `submit(...)`.
  The EDT never calls the engine; the worker never touches Swing directly.
- `GameListener` callbacks fire on the worker and **marshal to the EDT** with
  `SwingUtilities.invokeLater` before mutating components.
- AI turns are driven by `pumpAiIfNeeded()` on the worker: it loops while the active
  player is an AI, calling `runAiTurn`, with a short `Thread.sleep` so moves are visible.
- Buttons are enabled only on the human's turn and only in the legal phase, so the user
  cannot race the engine.

## 3. Project file / folder layout

```
babylon5/
├── docs/
│   ├── rulebook-source.txt        (rule authority; extracted from uploaded PDF)
│   ├── research-dossier.md        (Task 1)
│   ├── design-and-ai.md           (Task 2)
│   └── ui-and-testing.md          (Task 3 — this file)
├── src/main/resources/cards/*.json    (Task 1)
└── src/
    ├── main/java/com/whim/babylon5/
    │   ├── Main.java               (orchestrator)
    │   ├── domain/  (Task 1)
    │   ├── data/    (Task 1: CardDatabase, ImageLoader)
    │   ├── engine/  (Task 2: GameEngine, AIPlayer, Conflict, ConflictResult, AiDifficulty)
    │   └── ui/      (Task 3: MainWindow, PlayerPanel, CardView, UiTheme)
    └── test/java/com/whim/babylon5/engine/   (Task 2 self-test harness)
```

## 4. Testing strategy

### 4.1 Rule-fidelity unit tests (owned by Task 2, plain `java` harness — no JUnit dep)

Exact intended signatures (assertion harness throwing `AssertionError`):

```java
static void testConflict_supportMustStrictlyExceedOpposition();  // 5 vs 5 -> initiator LOSES
static void testConflict_strictExceedBoundary();                 // 6 vs 5 -> initiator WINS
static void testConflict_neutralizationAppliesDamage();
static void testPhaseProgression_readyToDrawThenNextPlayer();
static void testSponsor_spendsInfluenceAndRejectsWhenBroke();
static void testVictory_requires20PowerAndStrictlyHighest();
static void testVictory_tieAt20IsNotAWin();
static void testComputePower_baseEqualsInfluenceRating();
```

Run: `javac -d out $(find babylon5/src -name '*.java') && java -cp out com.whim.babylon5.engine.EngineSelfTest`.

### 4.2 AI behaviour — integration / manual cases

| Case | Setup | Expected |
| --- | --- | --- |
| EASY plays a legal turn | seed game, `runAiTurn(1)` | no exception; phase returns to a human-reachable state; some influence spent or pass |
| MEDIUM sponsors affordable best card | hand with mixed costs, full pool | `chooseCharacterToSponsor` returns highest-value affordable character |
| HARD declines unwinnable conflict | opposition >> support | `chooseConflict` returns `null` |
| 3 AIs to victory | run headless loop until `checkVictory()!=null` | terminates; winner has ≥20 power and strictly highest |
| Manual smoke (UI) | run `Main`, advance phases | AI turns animate, log scrolls, no EDT exceptions in console |

### 4.3 Manual UI checklist
- Window opens at ≥1180×800, resizes cleanly; opponent/hand scroll panes work.
- Selecting a hand card highlights it; "Sponsor Selected" only enabled in ACTION on your turn.
- "Declare Conflict" only enabled in CONFLICT; result shows in the center area + log.
- Card art loads asynchronously (placeholder first) with no UI freeze.
