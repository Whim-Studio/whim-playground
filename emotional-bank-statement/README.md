# The Emotional Bank Statement

A standalone Java 8 Swing desktop app that walks you through a short
self-coaching exercise: pick a core belief, record three real-life proofs that
you have lived it, and commit to one concrete action today to build further
proof. The completed statement can be exported to a clean `.txt` file.

**Zero external libraries** — standard JDK only. Java 8 compatible.

## Run

```bash
./run.sh
```

or manually:

```bash
find src/main/java -name '*.java' > sources.txt
javac -source 8 -target 8 -d out @sources.txt
java -cp out com.whim.ebs.Main
```

## The three steps

1. **Select a Belief** — a searchable dropdown of 96 core values
   (Accountability … Simplicity). Type to filter, case-insensitive.
2. **Collect Proof** — three separate text areas for concrete real-life
   examples where you lived the belief.
3. **Build Proof Through Action** — one field for the single action you will
   take today, plus **Save / Export**. Export is blocked until a belief is
   chosen and all three proofs + the action are filled in.

## Architecture

Clean layering with dependency inversion — the UI depends only on interfaces,
never on concrete logic classes (those are wired in `Main`).

```
com.whim.ebs
├── Main.java                 entry point — wires impls into the UI on the EDT
├── domain/                   (Task 1) core model + 96-belief catalog
│   ├── Belief.java
│   ├── Beliefs.java
│   └── SessionState.java
├── spi/                      (Task 1) integration contracts
│   ├── ValidationResult.java
│   ├── ValidationService.java
│   └── ExportService.java
├── logic/                    (Task 2) implementations of the spi contracts
│   ├── DefaultValidationService.java
│   └── TextExportService.java
└── ui/                       (Task 3) Swing
    └── MainFrame.java        3-step CardLayout wizard
```
