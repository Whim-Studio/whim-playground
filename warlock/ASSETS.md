# ASSETS

This project ships **no external image, font, audio, or data files**. Every visual
element is **drawn programmatically** at runtime with Java2D (Swing/AWT).

| Asset | Source | License |
|-------|--------|---------|
| Board, rooms, corridors, tokens | `ui/BoardPanel` custom `paintComponent` | Original code (this repo) |
| Attribute meters / bars | `ui/StatusPanel.Meter` custom paint | Original code (this repo) |
| Combat / card / end screens | Standard Swing components + Java2D | Original code (this repo) |
| Colours & fonts | `ui/Theme` (logical Java fonts: Serif/SansSerif/Monospaced) | Java platform logical fonts |

Notes:
- **No Games Workshop artwork, logos, trademarks, or printed text** are included or
  reproduced. The name of the original work is used only nominatively to describe
  what this fan project recreates.
- All room, monster, item, card, and flavor text is **original writing** (see
  `model/Content.java`) evoking the tone of a dungeon crawl — none of it is
  transcribed from any published source.
- Only the platform's built-in logical fonts are used, so there are no bundled
  font files and no font licensing to track.
- If any sourced (public-domain / CC0) asset were ever added, it would be listed
  here with its provenance and license. None is used today.
