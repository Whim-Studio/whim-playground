# YouTube Hub & Launcher

Standalone Java 8 + Swing desktop dashboard to catalog YouTube videos and launch
them in the OS default browser. Zero external libraries.

## Build & run
```
cd youtube-hub
javac -d out $(find src -name "*.java")
java -cp out com.whim.ythub.app.Main
```

## Architecture
- `model/` — `VideoRecord`, the frozen immutable domain contract shared by all layers.
- `io/`    — `LibraryManager`: CSV persistence via standard `java.nio.file`.
- `logic/` — `UrlValidator` (regex) + `VideoLauncher` (`Desktop.browse`).
- `ui/`    — Swing GUI (form + JTable + Watch action).
- `app/`   — `Main` entry point.
