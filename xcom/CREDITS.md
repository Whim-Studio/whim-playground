# Credits & Art / Content Policy

## Clean-room statement
This project is a **clean-room** recreation of *UFO: Enemy Unknown* / *X-COM: UFO
Defense* (MicroProse / Mythos Games, 1994). It reconstructs game behaviour from
**public documentation only**. It contains **no** original game code, graphics,
audio, text, maps, or data files, and it does **not** require the original game to
build or run.

## Art
All in-game visuals are **drawn procedurally with Java2D** (see
`com.whim.xcom.view` and `com.whim.xcom.geo.view`). No sprites, tilesets, or fonts
from the original game are used. This includes the Phase 8 additions: the
**terror-site markers** on the Geoscape and the **UFOpaedia entry glyphs**
(`UfopaediaScreen`) are original Java2D drawings, and all UFOpaedia article text is
**generated at runtime from the data pack** (`rules1994.json`), not copied from any
source. If any third-party art is ever added it will be **permissively/openly
licensed** and attributed here.

### Flagged placeholder geography
The Geoscape continents and the Phase 8 **terror-mission cities** (New York, London,
Tokyo, …) use approximate, hand-picked normalised coordinates as placeholder
geography — they are not derived from any original asset or map data.

## Third-party software
- **Gson** 2.10.1 — © Google, **Apache License 2.0**. Used for JSON data-pack
  loading.
- **JUnit** 4.13.2 — **Eclipse Public License 1.0**. Test scope only.

## Documentation sources (behavioural reconstruction)
- **UFOpaedia** — community reference wiki, `https://www.ufopaedia.org/`.
- **OpenXcom** — open-source re-implementation, `https://openxcom.org/`.

Trademarks and the original game are the property of their respective owners. This
is a non-commercial preservation/educational project.
