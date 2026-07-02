# Whim Browser — Java 8 Swing

A standalone desktop web browser built with **only** the Java 8 standard library
(Swing, AWT, `java.net`, `java.io`, `java.util`, `java.util.regex`). No Maven,
Gradle, JCEF, JavaFX, or Chromium embedding — zero external dependencies.

## Build & run

Requires a JDK (8+). From this `browser/` directory:

```sh
javac --release 8 -d out $(find src -name '*.java')
java -cp out com.whim.browser.app.Main
```

(`--release 8` is only needed when compiling with a newer JDK; a real JDK 8
compiles with plain `javac`.)

## Architecture

Three cleanly separated layers, each in its own package:

| Package                     | Responsibility                                                        |
|-----------------------------|-----------------------------------------------------------------------|
| `com.whim.browser.model`    | Thread-safe domain state: `WebResponse`, `Tab`, `HistoryManager`, `TabManager` |
| `com.whim.browser.engine`   | Networking + parsing + YouTube fallback: `BrowserEngine`, `EngineCallback`, `YouTubeHandler` |
| `com.whim.browser.ui` / `.app` | Swing presentation: `BrowserFrame`, `Main`                          |

**Threading model.** All network I/O runs on a `SwingWorker` background thread;
every UI mutation happens on the Event Dispatch Thread. The engine marshals its
`EngineCallback.onStart`/`onResult` calls onto the EDT via
`SwingUtilities.invokeLater`, so the UI never freezes during a fetch.

`WebResponse` is immutable (all fields `final`) and therefore safe to hand from
the worker thread to the EDT. `HistoryManager` and `TabManager` guard all state
with `synchronized` methods; `Tab`'s mutable fields are `volatile`.

## Why JEditorPane, and its hard limits

Swing's only built-in HTML renderer is `JEditorPane` backed by
`HTMLEditorKit`. Being brutally realistic about what that gives us:

- **HTML 3.2 only.** The parser targets a mid-1990s HTML level. Most CSS is
  ignored or partially applied; modern layout (flexbox, grid), semantic
  elements, and web fonts do not render.
- **No JavaScript engine.** There is none in the JDK. Any page that builds its
  content or navigation with JS will appear blank or skeletal. This is why the
  built-in `HyperlinkListener` handles clicks manually and re-routes them
  through the engine instead of letting the pane navigate itself.
- **No HTML5 / no video/audio codecs.** `<video>`, `<audio>`, `<canvas>`, and
  WebGL are unsupported. There is no media pipeline in Swing/AWT capable of
  decoding H.264/VP9/AV1, and no way to host a `<video>` element.

Given these limits, the browser renders *basic, mostly-static* HTML pages
faithfully and does not pretend to do more.

## Why the YouTube fallback works the way it does

True in-pane YouTube playback is **impossible** under these constraints: it
requires a JavaScript runtime, a DASH/HLS streaming stack, and modern video
codecs — none of which exist in the Java 8 standard library. Rather than fake a
player or show a broken page, the engine takes the next-best realistic path:

1. `YouTubeHandler.isYouTube(url)` detects watch / `youtu.be` / `m.youtube.com`
   / `/shorts/` / embed URLs at the **engine layer**, before any rendering.
2. It fetches the page with a browser-like `User-Agent` and extracts the video
   title from the `og:title` / `<title>` metadata using regex (the same native
   approach as the sibling `youtube-metadata-extractor` project — no APIs, no
   libraries).
3. It hands the URL to the user's real browser via
   `java.awt.Desktop.getDesktop().browse(URI)`, guarded by
   `Desktop.isDesktopSupported()` and the `BROWSE` action check so headless
   environments degrade to a clear error instead of throwing.
4. The pane then shows an informational page confirming the video opened
   natively, including the extracted title.

This delivers a working YouTube experience (the video really plays, in a capable
browser) without misrepresenting what the Swing renderer can do.
