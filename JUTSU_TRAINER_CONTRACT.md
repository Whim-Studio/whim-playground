# Jutsu Database & Seal Trainer — Shared Interface Contract (Java 8)

A standalone Swing desktop app: an offline Naruto **Jutsu encyclopedia + interactive
hand-seal training simulator**. Searchable/filterable database view, plus a "Training
Dojo" where the user clicks hand-seal buttons one at a time and the app live-filters
every jutsu whose required seal sequence *begins with* (or exactly matches) the input.

- App dir: `jutsu-trainer/`
- Source root: `jutsu-trainer/src/` (plain layout, package dirs under it — e.g. `jutsu-trainer/src/com/whim/jutsutrainer/domain/Jutsu.java`)
- Base package: `com.whim.jutsutrainer`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no `Stream.toList()`, no `List.of`/`Map.of`. Plain Java 8 (`Arrays.asList`, `Collections.unmodifiableList`, anonymous classes / lambdas OK).
- **No external libraries.** Only `javax.swing`, `java.awt`, `java.util`. No Maven/Gradle, no Gson/Jackson, no downloaded assets.
- **No copyrighted assets.** Hand seals are rendered as **text labels** (their names). No external images.
- Compile: `javac -d out $(find jutsu-trainer/src -name '*.java')`. Run: `java -cp out com.whim.jutsutrainer.app.Main`.

## File ownership (NO overlap between tasks)

- **Task 1 (research + domain + database)** owns `jutsu-trainer/src/com/whim/jutsutrainer/domain/**` and `jutsu-trainer/src/com/whim/jutsutrainer/data/**`.
- **Task 2 (logic + training engine)** owns `jutsu-trainer/src/com/whim/jutsutrainer/engine/**`.
- **Task 3 (Swing UI)** owns `jutsu-trainer/src/com/whim/jutsutrainer/ui/**`.
- **Main class** (`jutsu-trainer/src/com/whim/jutsutrainer/app/Main.java`) is written by the **orchestrator** during consolidation. Do NOT create it.

Task 2 imports the `domain` types **verbatim**. Task 3 imports `domain` types and the
`engine.JutsuService` entry point **verbatim**. Nobody re-declares a type owned by
another task. The contract fixes **public signatures**; internal implementation is each
task's own. Each task may add small private helpers *inside its own package only*.

---

## `com.whim.jutsutrainer.domain` — authored by Task 1 (fixed public API)

### `enum HandSeal`
The 12 basic Zodiac seals **plus** unusual/custom seals. Each constant exposes a display name.
```java
public enum HandSeal {
    // 12 Zodiac (basic)
    RAT, OX, TIGER, HARE, DRAGON, SNAKE, HORSE, RAM, MONKEY, BIRD, DOG, BOAR,
    // Unusual / custom seals
    CLONE_SEAL,    // crossed fingers (Shadow Clone etc.)
    CLAP,          // clasped/clapping hands (Summoning, Edo Tensei)
    HALF_RAM,      // one-handed Ram
    SNAKE_HALF,    // one-handed (Haku-style single-hand seals)
    NONE_FREEFORM; // technique cast with no formal seal / channeled

    public String getDisplayName();   // e.g. RAT -> "Rat", CLONE_SEAL -> "Clone Seal"
    public boolean isZodiac();        // true for the 12 basic seals only
}
```
Task 1 MAY add a few more unusual seals as constants if research warrants, but MUST keep
the 12 zodiac constants named exactly as above and MUST keep `getDisplayName()` / `isZodiac()`.

### `enum ChakraNature`
```java
public enum ChakraNature {
    FIRE, WATER, LIGHTNING, EARTH, WIND, YIN, YANG, YIN_YANG, NONE;
    public String getDisplayName();   // e.g. YIN_YANG -> "Yin–Yang", NONE -> "None"
}
```

### `class Jutsu` (immutable)
```java
public final class Jutsu {
    public Jutsu(String name, ChakraNature nature, String rank, String user,
                 String description, java.util.List<HandSeal> seals);
    public String getName();
    public ChakraNature getNature();
    public String getRank();                       // e.g. "A", "S", "—" if unknown
    public String getUser();                       // notable user, or "" 
    public String getDescription();
    public java.util.List<HandSeal> getSeals();    // unmodifiable, ordered, may be empty
}
```

## `com.whim.jutsutrainer.data` — authored by Task 1 (fixed public API)

### `class JutsuRepository`
```java
public final class JutsuRepository {
    public JutsuRepository();                       // builds the hardcoded catalog in-memory
    public java.util.List<Jutsu> all();             // unmodifiable list of every Jutsu
}
```
Research mandate: hardcode a **comprehensive, authentic** catalog (target 30+ jutsu) with
accurate seal sequences and real descriptions. Required exemplars (use these exact sequences):
- **Fire Style: Fireball Jutsu** — Snake → Ram → Monkey → Boar → Horse → Tiger
- **Chidori / Lightning Blade** — Ox → Hare → Monkey
- **Summoning Jutsu** — Boar → Dog → Bird → Monkey → Ram
- **Shadow Clone Jutsu** — Clone Seal (crossed fingers)
- **Water Dragon Jutsu** — long sequence (the famous 44-seal feat); represent a long ordered list.
No placeholder data.

---

## `com.whim.jutsutrainer.engine` — authored by Task 2 (fixed public API)

Pure logic, **zero** Swing/AWT imports. Constructed from a plain `List<Jutsu>` so it is
fully decoupled from the `data` package.
```java
public final class JutsuService {
    public JutsuService(java.util.List<Jutsu> catalog);

    // Database view
    public java.util.List<Jutsu> all();
    // Combined search: partial case-insensitive name match AND nature filter.
    // nameQuery null/blank = match all names; nature null = match all natures.
    public java.util.List<Jutsu> search(String nameQuery, ChakraNature nature);

    // Training simulator: every jutsu whose seal list STARTS WITH `input`
    // (prefix match, in order). Empty/null input returns all jutsu (that have >=1 seal).
    public java.util.List<Jutsu> matchPrefix(java.util.List<HandSeal> input);
    // Subset of matchPrefix where the sequence is an EXACT full match.
    public java.util.List<Jutsu> matchExact(java.util.List<HandSeal> input);
}
```

## `com.whim.jutsutrainer.ui` — authored by Task 3 (fixed public API)

```java
public final class MainWindow extends javax.swing.JFrame {
    public MainWindow(com.whim.jutsutrainer.engine.JutsuService service);
}
```
- `BorderLayout`/`GridBagLayout` based. A `JTabbedPane` (or split) with two views:
  - **Database**: name search field + `ChakraNature` dropdown (incl. "All") + results list +
    detail area showing description and the seal sequence of the selected jutsu.
  - **Training Dojo**: a button per `HandSeal` (text labels). Clicking appends to a visible
    "Current Sequence". A live results list shows `service.matchPrefix(currentSequence)`,
    highlighting exact matches. A "Clear Sequence" button resets it.
- All UI work on the EDT. Do NOT mutate domain objects.

---

## Consolidation (orchestrator)
Writes `app/Main.java`:
```java
public final class Main {
    public static void main(String[] args) {
        // SwingUtilities.invokeLater -> new MainWindow(new JutsuService(new JutsuRepository().all())).setVisible(true)
    }
}
```
Then compiles the whole tree and runs a smoke check.
