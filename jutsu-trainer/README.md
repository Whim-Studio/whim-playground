# Jutsu Database & Seal Trainer (Java 8 / Swing)

A standalone, zero-dependency Naruto **jutsu encyclopedia + interactive hand-seal
training simulator**. No external libraries, no build tool, no network — just the JDK.

## Run

```sh
javac -d out $(find jutsu-trainer/src -name '*.java')
java -cp out com.whim.jutsutrainer.app.Main
```

## Features

- **Database view** — search by name (partial, case-insensitive), filter by chakra
  nature, and read each jutsu's rank, user, description, and full hand-seal sequence.
- **Training Dojo** — click hand-seal buttons one at a time; the app live-filters every
  jutsu whose required sequence *begins with* your input, marking exact matches as
  `✓ READY`. "Clear Sequence" resets.
- 38 authentic jutsu across all chakra natures, with accurate ordered seal sequences,
  including unusual seals (crossed-finger Clone Seal, clapping for Summoning/Edo Tensei,
  one-handed Haku seals, and freeform/no-seal techniques like Rasengan).

## Architecture

Strict package layering — see `JUTSU_TRAINER_CONTRACT.md` at the repo root.

- `domain/` — `HandSeal`, `ChakraNature` enums and the immutable `Jutsu` model.
- `data/` — `JutsuRepository`, the hardcoded in-memory catalog.
- `engine/` — `JutsuService`: pure search + sequence-matching logic, zero Swing imports.
- `ui/` — `MainWindow`: the Swing front end.
- `app/` — `Main`: wires repository → service → window on the EDT.

**Java 8 only.** No `var`, text blocks, switch expressions, records, or `List.of`.
