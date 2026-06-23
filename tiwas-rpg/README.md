# Tiwas RPG — Demo Version

A standalone, single-window **Java 8 Swing** implementation of the Tiwas RPG: a
character creator and a modular adventure loader, with strict JSON save/load.

**Zero external dependencies** — only `javax.swing`, `java.awt`, `java.util`,
`java.io`, `java.lang`. Java 8 source/target. No Gson/Jackson: JSON is a
hand-written parser/writer.

This is a **Demo Version** (the label is shown prominently in the UI).

## Run it

No Maven needed — plain `javac`/`java` (JDK 8+):

```bash
cd tiwas-rpg
javac -d out $(find src -name '*.java')
java -cp out com.tiwas.rpg.app.Main
```

Then:
- **Character Creator** tab → type a name → **Roll Character** (rolls 1d100 for each
  of the 24 attributes and shows them plus the derived stats). **Save JSON** /
  **Load JSON** to persist a hero (a ready-made one is in `characters/sample-hero.json`).
- **Adventure** tab → **Load Adventure JSON** (`adventures/the-bone-vault.json`) →
  **Select Character JSON** → **Begin Session**. Pick a skill, set a Difficulty
  Modifier, **Attempt Action** — the engine rolls d100 roll-under, pays exertion
  equal to the roll, and bleeds any overflow straight into HP.

## Architecture (strict package boundaries)

```
tiwas-rpg/src/com/tiwas/rpg/
├── domain/     # data + JSON mapping only — no dice, no rules, no UI
│   ├── AttributeCode   # the 24 zodiac attributes (body group, then mind group)
│   ├── Skill           # name, tier, attribute formula, value; maxCap = Σattrs / tier
│   ├── Character        # 24 attributes, named skills, live HP/PE/MP pools, derived stats
│   ├── Npc, Scene, AdventureModule   # streamlined §9 stat block + adventure container
│   └── (JSON: toMap/fromMap/toJson/fromJson/save/load on each)
├── json/       # generic hand-written JSON
│   ├── Json            # parse / write / writePretty + asObject/asArray/asString/asInt/asBoolean
│   └── JsonException
├── engine/     # all rules math — imports domain only
│   ├── Dice            # d100() uniform 1..100; seedable for tests
│   ├── CharacterGenerator  # rolls 24×1d100, builds the 24 Tier-1 skills, restores pools
│   ├── ActionResolver  # roll-under + exertion + overflow→HP + margin + failure-XP + doubles + recovery
│   ├── ActionResult    # immutable result carrier with describe()
│   ├── CombatMath      # damage = DmgMod + Margin + Might/Agility bonus − Armor (min 0)
│   └── EngineSmokeTest # self-contained `public static void main`, no JUnit
├── ui/         # Swing only — talks to engine + domain through public APIs
│   ├── MainFrame, CharacterCreatorPanel, AdventurePanel
└── app/
    └── Main            # entry point; system L&F, EDT launch
```

The UI imports `domain` + `engine`; the engine imports only `domain`; the domain
contains no rules logic or UI. The full interface contract lives in
`../TIWAS_RPG_CONTRACT.md`.

## Core rules implemented (per the project brief)

- **d100 roll-under.** Success = roll ≤ effective Skill (Skill value + Difficulty
  Modifier, clamped 1–99). All fractional math rounds **DOWN**.
- **Exertion.** Every test costs Physical Energy (Body skills) or Mental Points
  (Mind skills) equal to the number rolled.
- **Overflow.** If the cost exceeds the remaining pool, the excess is dealt
  immediately as direct damage to HP.
- **24-attribute matrix → derived stats.** HP = Σ 12 Body attrs; MP = Σ 12 Mind
  attrs; Physical Energy = bep+bes+bee; Speed = bsp+bss+bse; Energy/MP Regen =
  bep+bes / mep+mes; Movement = (bsp+bss)/15.
- **Margin, Failure XP, doubles epiphany, and ½-Regen post-test recovery.**

## Verification

- `javac --release 8` compiles the whole tree clean (33 classes), proving Java-8
  legality and that UI ↔ engine ↔ domain wire up against the shared contract.
- `java -cp out com.tiwas.rpg.engine.EngineSmokeTest` → `ALL ENGINE SMOKE TESTS PASSED`
  (seeded generation, deterministic overflow case, doubles-fail flagging).
