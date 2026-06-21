# HBDI App — Shared Interface Contract (Java 8)

Standalone Swing app implementing the Herrmann Brain Dominance Instrument (HBDI).
- App dir: `hbdi/`
- Base package: `com.whim.hbdi`
- Java 8 ONLY. No `var`, no switch expressions, no text blocks, no records, no `Stream` collectors that postdate Java 8 — stick to plain Java 8. No external libraries (only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`).
- 116 questions. Four quadrants: A=Analytical, B=Sequential, C=Interpersonal, D=Imaginative.
- Responses use a 5-point Likert scale (1..5).

## File ownership (NO overlap between tasks)
- **Task 1 (model)** owns: `hbdi/src/main/java/com/whim/hbdi/domain/**` and `hbdi/src/main/resources/com/whim/hbdi/questions.csv`
- **Task 2 (scoring)** owns: `hbdi/src/main/java/com/whim/hbdi/scoring/**` and `hbdi/src/test/java/com/whim/hbdi/scoring/**`
- **Task 3 (ui)** owns: `hbdi/src/main/java/com/whim/hbdi/ui/**`
- **Main class** (`com/whim/hbdi/Main.java`) is written by the orchestrator during consolidation. Do NOT create it.

Tasks 2 and 3 MUST code against the interfaces below verbatim and MUST NOT create their own copies of the `domain` interfaces. The orchestrator compiles the whole project during consolidation.

## Domain interfaces (authored by Task 1, package `com.whim.hbdi.domain`)

```java
public enum Quadrant {
    A("Analytical"), B("Sequential"), C("Interpersonal"), D("Imaginative");
    private final String label;
    Quadrant(String label) { this.label = label; }
    public String getLabel() { return label; }
}

public interface Question {
    int getId();                               // 1..116, unique
    String getText();
    String getCategory();
    java.util.Map<Quadrant, Integer> getQuadrantWeights(); // weight per quadrant, >=0
}

public interface Response {
    int getQuestionId();
    int getValue();                            // Likert 1..5
}

public interface QuadrantScore {
    Quadrant getQuadrant();
    double getRawScore();                       // weighted sum
    double getPercentage();                     // 0..100, all four sum to ~100
}

public interface QuestionBank {
    java.util.List<Question> getQuestions();    // exactly 116, ordered by id
    int size();
}
```

Task 1 also provides concrete implementations: `DefaultQuestion`, `DefaultResponse`, `DefaultQuadrantScore`, and `DefaultQuestionBank` (loads `questions.csv` from the classpath resource `com/whim/hbdi/questions.csv`; falls back to a generated bank if the resource is missing). `DefaultQuestionBank` has a public no-arg constructor.

CSV format (header row required): `id,category,text,weightA,weightB,weightC,weightD`

## Scoring interfaces (authored by Task 2, package `com.whim.hbdi.scoring`)

```java
public final class ValidationResult {
    public boolean isComplete();
    public java.util.List<Integer> getUnansweredIds();
    // public constructor: ValidationResult(boolean complete, List<Integer> unansweredIds)
}

public interface SurveyValidator {
    ValidationResult validate(java.util.List<com.whim.hbdi.domain.Question> questions,
                              java.util.Map<Integer, com.whim.hbdi.domain.Response> responses);
}

public interface ScoringEngine {
    // Returns exactly 4 QuadrantScore (one per Quadrant), percentages summing to ~100.
    java.util.List<com.whim.hbdi.domain.QuadrantScore> score(
        java.util.List<com.whim.hbdi.domain.Question> questions,
        java.util.Map<Integer, com.whim.hbdi.domain.Response> responses);
}
```

Task 2 provides `DefaultSurveyValidator` and `DefaultScoringEngine` (both public no-arg constructors). Scoring: for each answered question, add `likertValue * quadrantWeight` to each quadrant's raw score; percentage = quadrantRaw / totalRaw * 100. Task 2 must include JUnit-free or JUnit4 tests; if no test framework is on the classpath, provide a `main`-runnable self-check class `ScoringSelfTest` under the scoring test dir that prints PASS/FAIL — do not require external test deps to build the app.

## UI (authored by Task 3, package `com.whim.hbdi.ui`)

`MainFrame extends javax.swing.JFrame` with a public no-arg constructor that builds the full wizard using `DefaultQuestionBank`, `DefaultSurveyValidator`, `DefaultScoringEngine`. The orchestrator's `Main` will just do `new MainFrame().setVisible(true)` on the EDT.
- `CardLayout` wizard over the 116 questions (paged, e.g. one or several per card) with a progress indicator and Next/Back; cannot advance past an unanswered question.
- Results view: custom `JPanel` drawing a four-quadrant profile chart with `java.awt.Graphics2D` (no chart libs).
- "Save Report" button exports quadrant scores to a `.txt` file via `JFileChooser`.
