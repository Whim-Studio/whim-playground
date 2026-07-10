# Merchant Prince — Java 8 / Swing recreation

A standalone, clean-room recreation of QQP's 1994 trading & political-intrigue
strategy game *Merchant Prince*. Lead a Venetian merchant house from 1300: trade goods
across the Afro-Eurasian map, run ships and caravans, and climb Venetian and Church
politics to become the wealthiest family — first to a million florins, or the highest
net worth when the era ends.

## Build & run
Requires a Java 8+ JDK and Maven.

```bash
cd merchantprince
mvn -q package
java -jar target/merchantprince-1.0.0-SNAPSHOT.jar
# or, from classes:
mvn -q compile exec:java -Dexec.mainClass=com.whim.merchantprince.MerchantPrince
```

## Design docs
- `GAME_DESIGN_REFERENCE.md` — researched mechanics + labelled assumptions.
- `ARCHITECTURE.md` — package/class layout and screen flow.

Pure Java standard library + Swing; no third-party runtime dependencies. All tunable
numbers live in `engine/Constants.java`.
