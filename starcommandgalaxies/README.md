# Star Command: Galaxies — Java 8 / Swing recreation

A standalone, zero-dependency **Java 8 + Swing** desktop game that recreates the
*spirit and mechanics* of **Star Command Galaxies** (Warballoon, ~2015). Original
clean-room implementation: all graphics are drawn procedurally with Java2D, all
text/names are original — **no art, sprites, or verbatim text from the original**.

You are a starship Captain: crew and outfit your ship, fly a galaxy of star
systems, fight real-time-with-pause space battles, teleport a boarding party onto
disabled enemies, manage crew stats/happiness, and upgrade your tech.

## Build & run

Requires a **JDK 8+**.

```bash
cd starcommandgalaxies
mvn -q compile
mvn -q exec:java -Dexec.mainClass=com.whim.scg.app.Main
```

Plain JDK (no Maven):
```bash
cd starcommandgalaxies
javac -d out $(find src -name '*.java')
java -cp out:src com.whim.scg.app.Main     # `src` on the classpath bundles data/*.json
```

## Controls (summary)

- **Menu:** N new game · C continue (autosave) · H help · Q quit
- **Global:** Space = pause/resume · Esc = main menu · S = save
- **Ship interior:** drag a crew member onto a room to station them
- **Galaxy:** click/arrow to a linked system to jump · scan · dock at starports
- **Space combat:** target enemy rooms, charge & fire weapons, allocate power,
  begin boarding when enemy shields drop
- **Boarding:** select a marine, arrows/click to move, attack adjacent hostiles

See `../STARCOMMANDGALAXIES_CONTRACT.md` for the full design brief, architecture,
and package layout.

## Status

Built via the workspace's parallel-task pattern: an orchestrator-owned `api` seam
+ runnable `app` shell, with the engine and UI screens filled in by parallel
child tasks. The shell runs on its own (stub engine + placeholder screens) so the
build is green at every step.
