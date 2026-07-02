# WhimKit — a from-scratch browser engine in pure Java 8

WhimKit is an experimental web-browser **engine** and desktop app built with
**only the Java 8 standard library** — Swing/AWT, `java.net`/`javax.net.ssl`,
`javax.script` (Nashorn), `javax.imageio`. No JavaFX, JCEF/Chromium,
`JEditorPane`, Maven/Gradle, JNI, or any third-party jar.

It is a real engine, not an HTML viewer: a custom HTML5 tokenizer + tree builder,
a DOM, a CSS cascade, a block/inline box-model layout engine, a Java2D software
renderer, and a Nashorn JavaScript runtime bound to the DOM.

See **[DESIGN.md](DESIGN.md)** for the full architecture, feasibility analysis,
per-subsystem design, threading model, limitations, and roadmap.

## Build

Requires a JDK (8+). From this directory:

```sh
javac --release 8 -d out $(find src -name '*.java')
```

(`--release 8` is only needed on a newer JDK. Nashorn ships in JDK 8–14; on JDK
15+ it is absent and the JavaScript subsystem degrades to a logged no-op — the
browser still runs and renders.)

## Run

```sh
java -cp out com.whimkit.app.Main            # opens the welcome page
java -cp out com.whimkit.app.Main example.com # opens a URL
```

Type a URL and press Enter; use ← / → / ↻; open **DevTools** to inspect the DOM
tree and run JavaScript in the console.

## Architecture at a glance

Staged pipeline; the **foundation** owns the shared data structures and each
**subsystem** owns an algorithm, so every subsystem compiles independently:

```
URL → net.http → WebResponse → html → DOM(dom) → css.engine(ComputedStyle)
    → layout.engine(LayoutBox) → render(Java2D on a JPanel)
    ⤷ js(Nashorn) mutates DOM → reflow → re-style/-layout/-paint
```

| Package | Role |
|---|---|
| `com.whimkit.dom` | DOM node model (foundation) |
| `com.whimkit.net`, `.net.http` | `WebResponse`/`ResourceLoader` + HTTP stack (cookies, cache, gzip, redirects) |
| `com.whimkit.html` | HTML5 tokenizer + tolerant tree builder |
| `com.whimkit.css`, `.css.engine` | `ComputedStyle` + selectors/specificity/cascade/inheritance |
| `com.whimkit.layout`, `.layout.engine` | `LayoutBox` tree + block/inline box-model layout |
| `com.whimkit.render` | Java2D renderer (backgrounds, borders, text, images, markers) |
| `com.whimkit.js` | Nashorn runtime + DOM bindings + timers/events |
| `com.whimkit.ui`, `.app` | Swing UI (tabs, address bar, dev panel) + `BrowserEngine` coordinator |

Subsystems are wired behind their interfaces by `com.whimkit.app.Subsystems`,
which locates the concrete implementation by name and falls back to a built-in
minimal implementation if one is absent — so the app always assembles and runs.

## Known limitations (by design)

No flexbox/grid/positioned layout beyond basics; no `<video>`/`<audio>`/WebGL/
WASM; JS is Nashorn (ES5.1 + partial ES6) against a DOM subset (no `fetch`,
`Promise`, `requestAnimationFrame`, or layout-metric APIs); CSS covers the common
property set; images are `ImageIO` raster formats only. Modern SPAs (React/Vue/
Angular) and modern YouTube are out of reach by construction — see DESIGN.md §1.
