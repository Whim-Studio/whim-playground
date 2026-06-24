# Star Trek RPG (Coda System) Character Creator — Shared Interface Contract (Java 8)

Standalone desktop **character generator** for the Decipher *Star Trek Roleplaying Game* (Coda System).
Pure Java 8 + Swing. **No external libraries** (no Maven/Gradle build required to run, no Gson/Jackson).
Only `javax.swing`, `java.awt`, `java.util`, `java.io`, `java.nio`.

- App dir: `startrek-coda-chargen/`
- Base package: `com.whim.coda`
- **Java 8 ONLY.** No `var`, no text blocks, no switch expressions, no records, no post-Java-8 APIs. Lambdas/streams OK.
- Tasks 2 and 3 **import and use** the `model`/`data` types authored by Task 1. They MUST NOT redeclare them.
- Compile/run target (orchestrator will verify):
  `find startrek-coda-chargen/src -name '*.java' > /tmp/srcs && javac -d /tmp/out @/tmp/srcs && java -cp /tmp/out com.whim.coda.Main`

## File ownership (NO overlap)

- **Task 1 (domain + data)** owns: `startrek-coda-chargen/src/com/whim/coda/model/**` and `startrek-coda-chargen/src/com/whim/coda/data/**`
- **Task 2 (engine)** owns: `startrek-coda-chargen/src/com/whim/coda/engine/**`
- **Task 3 (ui)** owns: `startrek-coda-chargen/src/com/whim/coda/ui/**`
- **`com/whim/coda/Main.java`** is written by the orchestrator at consolidation. Do NOT create it.

Match the package, name, and signatures **verbatim**. Add fields/helpers freely; do not change/remove anything specified here.

---

## MODEL (Task 1 — package `com.whim.coda.model`)

```java
public enum Attribute { STRENGTH, AGILITY, INTELLECT, VITALITY, PRESENCE, PERCEPTION }

public enum Reaction { QUICKNESS, SAVVY, STAMINA, WILLPOWER }
```

### AttributeSet
Holds the six BASE scores (pre-species) and exposes adjusted scores + modifiers.
```java
public class AttributeSet {
    public int getBase(Attribute a);
    public void setBase(Attribute a, int score);
    public int getSpeciesMod(Attribute a);          // set by engine when species applied
    public void setSpeciesMod(Attribute a, int mod);
    public int getAdjusted(Attribute a);            // base + speciesMod
    public int getModifier(Attribute a);            // = modifierFor(getAdjusted(a))
    /** Coda attribute-modifier table (authoritative — see RULES below). */
    public static int modifierFor(int score);
}
```

### Ability (species racial trait / note)
```java
public class Ability {
    public Ability(String name, String description);
    public String getName();
    public String getDescription();
}
```

### Species
```java
public class Species {
    public Species(String name, java.util.Map<Attribute,Integer> mods,
                   java.util.List<Ability> abilities, int bonusCourage);
    public String getName();
    public int getMod(Attribute a);                 // 0 if none
    public java.util.Map<Attribute,Integer> getMods();
    public java.util.List<Ability> getAbilities();
    public int getBonusCourage();                   // Bajoran Pagh = 1, else 0
}
```

### Skill / SkillRank, Edge, Flaw
```java
public class Skill   { public Skill(String name, Attribute key); public String getName(); public Attribute getKey(); }
public class Edge    { public Edge(String name, String description); public String getName(); public String getDescription(); }
public class Flaw    { public Flaw(String name, String description); public String getName(); public String getDescription(); }

/** A skill possessed at a rank, optionally with a specialty.
 *  Base-skill uniqueness rule: the SAME Skill name may appear MULTIPLE times only if specialties differ. */
public class SkillRank {
    public SkillRank(Skill skill, int rank, String specialty); // specialty may be null/empty
    public Skill getSkill();
    public int getRank();
    public String getSpecialty();
}
```

### CharacterSheet (the central state object both other tasks read/write)
```java
public class CharacterSheet {
    public String getName();             public void setName(String n);
    public Species getSpecies();         public void setSpecies(Species s);
    public AttributeSet getAttributes(); // never null
    public java.util.List<SkillRank> getSkills();   // mutable list
    public java.util.List<Edge> getEdges();         // mutable list
    public java.util.List<Flaw> getFlaws();         // mutable list
    // Derived values are CACHED here by the engine after recompute:
    public int getHealth();   public void setHealth(int v);
    public int getDefense();  public void setDefense(int v);
    public int getReaction(Reaction r);  public void setReaction(Reaction r, int v);
    public int getCourage();  public void setCourage(int v);
    public int getRenown();   public void setRenown(int v);
}
```

---

## DATA (Task 1 — package `com.whim.coda.data`)

### DataRepository — hardcoded game data
```java
public final class DataRepository {
    public static java.util.List<Species> species();   // EXACT roster below, ordered
    public static Species speciesByName(String name);
    public static java.util.List<Skill> skills();       // a reasonable Coda skill list (see notes)
    public static java.util.List<Edge> edges();         // a reasonable Coda edge list
    public static java.util.List<Flaw> flaws();         // a reasonable Coda flaw list
}
```

**Species roster (exact, in this order). Romulan modifiers/abilities intentionally EMPTY placeholder. EXCLUDE Ocampa, Talaxian, Trill.**

| Species | Attribute mods | Abilities | bonusCourage |
|---|---|---|---|
| Bajoran | none | Artistic (+1 Craft), Faithful (+2 Religion), Pagh (+1 Courage point) | 1 |
| Betazoid | Presence +1 | Psionic; Peaceful (+4 Negotiate); Telepathy 2 | 0 |
| Cardassian | Perception +1, Vitality +1, Agility −1, Presence −1 | Eidetic Memory; High Pain Threshold; Devious (+2 Influence); Prying (+2 Inquire); Vesala | 0 |
| Ferengi | Presence +1, Perception +1, Strength −2 | Keen Hearing; Eye for Profit; Four-Lobed Brain; Head for Numbers; Lobes for Business | 0 |
| Human | none | (none) | 0 |
| Klingon | Strength +1, Vitality +1, Intellect −1, Perception −1 | (none required) | 0 |
| Romulan | **EMPTY placeholder** (no mods) | **EMPTY placeholder** | 0 |
| Vulcan | Intellect +1, Strength +2, Presence −3 | Psionic (commonly) | 0 |

> Note Pagh's "+1 Courage point" is modeled via `bonusCourage=1`, NOT an attribute mod.

### JsonWriter — custom JSON serializer (NO external libs)
```java
public final class JsonWriter {
    /** Produce a pretty-printed JSON string of the full character sheet. */
    public static String toJson(com.whim.coda.model.CharacterSheet sheet);
}
```
Output object shape (keys): `name, species, attributes{base{},speciesMod{},adjusted{},modifier{}},
reactions{quickness,savvy,stamina,willpower}, derived{health,defense,courage,renown},
skills[{name,key,rank,specialty}], edges[{name}], flaws[{name}]`. Escape quotes/backslashes/control chars.

---

## ENGINE (Task 2 — package `com.whim.coda.engine`)

```java
public final class AttributeGenerator {
    /** RANDOM: roll 2d6 nine times, DROP the three lowest, return the six kept scores (high→low).
     *  NO exploding dice. Caller assigns them to attributes. */
    public static java.util.List<Integer> rollScores(java.util.Random rng);
    /** Point-buy validation: each base must be 2..12 and total spent within budget. */
    public static int POINT_BUY_BUDGET = 46; // sum of standard example array (10+9+7+7+5+4)+8
    public static boolean validatePointBuy(com.whim.coda.model.AttributeSet attrs);
}

public final class RulesEngine {
    public static final int MAX_START_ATTRIBUTE = 12; // BEFORE species adjustments
    public static final int BASE_DEFENSE = 7;
    public static final int STARTING_COURAGE = 3;
    public static final int STARTING_RENOWN = 0;

    /** Enforce the pre-adjustment cap: returns false (and the UI should block) if any BASE > 12. */
    public static boolean validateBaseCaps(com.whim.coda.model.AttributeSet attrs);
    /** Copy the selected species' attribute mods into attrs.speciesMod. */
    public static void applySpecies(com.whim.coda.model.CharacterSheet sheet);
    /** Recompute ALL derived values and cache them on the sheet:
     *  Health = adjusted Vitality + Strength modifier
     *  Defense = 7 + Agility modifier
     *  Quickness = max(mod Perception, mod Agility); Savvy = max(mod Presence, mod Perception);
     *  Stamina  = max(mod Strength, mod Vitality);  Willpower = max(mod Intellect, mod Vitality)
     *  Courage = 3 + species.bonusCourage; Renown = 0 */
    public static void recomputeDerived(com.whim.coda.model.CharacterSheet sheet);
}

public final class PackageBuilder {
    /** Custom Personal / Custom Professional packages are FREE-FORM: no predefined package lists. */
    /** Add a skill rank. Throws IllegalArgumentException if it duplicates an existing BASE skill
     *  WITHOUT a distinct specialty (same skill name allowed only when specialties differ). */
    public static void addSkill(com.whim.coda.model.CharacterSheet sheet,
                                com.whim.coda.model.SkillRank rank);
    public static boolean canAddSkill(com.whim.coda.model.CharacterSheet sheet,
                                      com.whim.coda.model.Skill skill, String specialty);
    public static void addEdge(com.whim.coda.model.CharacterSheet sheet, com.whim.coda.model.Edge e);
    public static void addFlaw(com.whim.coda.model.CharacterSheet sheet, com.whim.coda.model.Flaw f);
}
```

---

## UI (Task 3 — package `com.whim.coda.ui`)

- `MainFrame extends JFrame` — assembles the whole window with `BorderLayout` / `GridBagLayout`.
  Public constructor `MainFrame()` builds and shows nothing on its own; orchestrator's `Main` calls
  `new MainFrame().setVisible(true)` on the EDT.
- Sections: character name field; species `JComboBox<Species>`; attribute entry (six spinners) with a
  toggle between **Random roll** (calls `AttributeGenerator.rollScores`) and **Point-buy**; a live panel
  showing adjusted scores, modifiers, and all derived values (calls `RulesEngine.recomputeDerived`).
- Custom Package area: `JComboBox` of skills + rank spinner + specialty field + "Add" → a `JTable`/`JList`
  of chosen `SkillRank`s (uses `PackageBuilder.addSkill`; show the validation error in a dialog).
  Separate add/remove lists for Edges and Flaws.
- Bottom: a read-only `JTextArea` previewing `JsonWriter.toJson(sheet)`, refreshed on every change, with
  **Copy to Clipboard** and **Save to File** (`JFileChooser`) buttons.
- All engine/data access goes THROUGH the contract signatures above — no game logic in the UI.

---

## GAME RULES (authoritative, from the Players Guide)

- **Attribute modifier table** (`AttributeSet.modifierFor`): confirmed by worked examples
  6→0, 7→0, 8→+1, 9→+1, 10→+2, 11→+2, 12→+3, 13→+3. Negative side (standard symmetric Coda):
  5→−1, 4→−1, 3→−2, 2→−2, 1→−3. Implement as a single lookup method; modifiers range −3..+3.
- **Stat cap:** max starting BASE attribute is **12 before species adjustments**. Only species mods may
  push an *adjusted* score above 12. **No exploding dice** during character generation.
- **Random generation:** roll 2d6 nine times, discard the three lowest, keep six.
- **Skill rule:** a character may not possess the same BASE skill twice, BUT may possess the same skill
  multiple times with **distinct specialties**.
- **Packages:** ONLY "Custom Personal Package" and "Custom Professional Package" (free choice of any
  skills + edges). No predefined professional/personal packages.
- **Derived:** Health = Vitality + Strength mod; Defense = 7 + Agility mod; reactions per formulas above
  (a reaction equals the higher MODIFIER of its two governing attributes); Courage starts at 3
  (+1 for Bajoran Pagh); Renown starts at 0.
