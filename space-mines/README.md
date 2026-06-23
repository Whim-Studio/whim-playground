# Space Mines

A standalone Java 8 Swing port of the C64 BASIC resource-management game *Space Mines*.
You run a mining colony for 10 years: buy mines, trade ore, feed your colonists, and
keep morale up — or watch the people revolt.

Zero external dependencies: only `javax.swing`, `java.awt`, and `java.util`.

## Architecture (MVC)

```
space-mines/
├── src/com/spacemines/
│   ├── ColonyState.java     # Model state: year(V) population(P) money(M) storedOre(C)
│   │                        #              mines(L) satisfaction(S) foodPrice(FP) orePerMine(CE)
│   ├── GameConstants.java   # Initial values, thresholds, newGame() factory
│   ├── RandomEvents.java    # java.util.Random wrapper; legacy INT(RND*range+base) math
│   ├── PlayerActions.java   # Per-turn input: minesToBuy, oreToSell, foodToBuy
│   ├── TurnResult.java      # Per-turn output: narrative, gameOver, gameOverReason
│   ├── GameEngine.java      # The Model logic — ALL game math / state transitions
│   ├── SpaceMinesUI.java    # The View — Swing dashboard (GridBagLayout), no game logic
│   └── Main.java            # Entry point; wires ColonyState + GameEngine + SpaceMinesUI
└── test/com/spacemines/
    └── GameEngineTest.java  # Self-contained test runner (no JUnit); exits 1 on failure
```

The View never computes game math — it reads `ColonyState` fields for display and calls
`GameEngine.processYear(...)`.

## Build & run from the command line

From the `space-mines/` directory:

```bash
# Compile everything (Java 8 compatible)
mkdir -p out
javac -source 8 -target 8 -d out -cp src $(find src test -name '*.java')

# Run the game (opens the Swing window)
java -cp out com.spacemines.Main

# Run the test suite (prints PASS/FAIL, exits non-zero on failure)
java -cp out com.spacemines.GameEngineTest
```

A JDK 8 or newer is required (verified with `javac 17` targeting Java 8 bytecode).
