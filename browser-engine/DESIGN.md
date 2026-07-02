# WhimKit — A From-Scratch Browser Engine in Pure Java 8

WhimKit is an experimental, modular web-browser **engine** and desktop
application written in **Java 8 with the standard library only** — Swing/AWT for
UI and rendering, `java.net`/`javax.net.ssl` for networking, `javax.script`
(Nashorn) for JavaScript. No JavaFX, JCEF/Chromium, `JEditorPane`, Maven/Gradle,
JNI, or any third-party jar.

It is deliberately *not* the sibling `../browser` project, which is an honest
`JEditorPane` viewer. WhimKit replaces that with a real engine: a custom HTML5
tokenizer/tree-builder, a DOM, a CSS cascade, a box-model layout engine, a Java2D
software renderer, and a Nashorn JavaScript runtime bound to the DOM.

---

## 1. Feasibility Analysis

The single most important act of engineering honesty here is refusing to
hallucinate capability. Java 8 + Swing gives us a genuine software rendering
surface (Java2D), a real scripting engine (Nashorn), and a competent HTTP stack
(`HttpURLConnection` over TLS). It does **not** give us a modern media pipeline,
a compositor, a JIT-class JS engine with modern APIs, or GPU acceleration.

| Feature | Verdict | Justification |
|---|---|---|
| HTTP/HTTPS GET, redirects, headers | **Feasible** | `HttpURLConnection` + `HttpsURLConnection`; TLS via the platform trust store. |
| Cookies, connection reuse, gzip | **Feasible** | `CookieManager`/`CookieHandler`; `Accept-Encoding: gzip` + `GZIPInputStream`; keep-alive is automatic. |
| In-memory + disk resource cache | **Feasible** | `LinkedHashMap` LRU + `java.nio.file`; honor `Cache-Control`/`ETag` best-effort. |
| Async / streaming loads | **Feasible** | `ExecutorService` worker pool; `SwingWorker` marshals to the EDT. |
| HTML5 tokenizer + tolerant tree builder | **Feasible** | A pragmatic subset of the WHATWG state machine; implied-tag and misnest recovery. |
| DOM tree, traversal, mutation, queries | **Feasible** | Custom `Node`/`Element`/`Document`. |
| CSS selectors, specificity, cascade, inheritance | **Feasible (subset)** | Tag/class/id/descendant/child/attribute selectors; the common property set. |
| Block + inline box-model layout | **Feasible (subset)** | Normal flow, box model, basic floats, line-box breaking. |
| Flexbox / CSS Grid | **Partially feasible** | Implementable in principle but large; out of initial scope. Documented as a gap. |
| Java2D text/image/border rendering | **Feasible** | `Graphics2D`, `FontMetrics`, `ImageIO` (PNG/JP/GIF/BMP). |
| JavaScript execution | **Partially feasible** | Nashorn runs ES5.1 (+ some ES6). Bound to a *minimal* DOM. |
| `fetch`/`Promise`/`async`, ES modules, typed arrays-heavy code | **Partially feasible→Impractical** | Nashorn lacks `fetch`, real microtask/event-loop, and modern globals; shims are partial. |
| React/Vue/Angular apps | **Impractical** | They assume a full modern DOM, layout-affecting APIs (`getBoundingClientRect`, `ResizeObserver`), and modern JS. Our DOM/JS subset cannot satisfy them. |
| `<video>`/`<audio>`, WebGL, WebAssembly | **Impossible** | No H.264/VP9/AV1/Opus decoders, no GL/WASM in the JDK. `<img>` raster formats only. |
| Modern YouTube playback in-pane | **Impossible** | Requires DASH/HLS + modern codecs + heavy JS. (The sibling project hands off to the system browser; WhimKit documents the same limit.) |
| WebSockets, Service Workers, WebRTC | **Impossible/Impractical** | No standard-library primitives that fit the model. |

**Why modern SPAs are effectively impossible here (deep dive).** A React app
ships a bundle that, on load, calls `ReactDOM.render`, which walks a virtual DOM
and issues hundreds of `document.createElement`/`appendChild`/`setAttribute`
calls, reads layout via `getBoundingClientRect`/`offsetWidth`, attaches synthetic
event listeners, and often relies on `Promise`/`queueMicrotask`, `fetch`, and
`requestAnimationFrame`. Nashorn provides none of `fetch`, `Promise` (natively —
only via slow shims), `rAF`, or a spec-compliant event loop, and our DOM exposes
only a curated subset without live layout metrics. Even if the bundle parsed,
the first `getBoundingClientRect` returning stub values derails hydration. This
is a *fundamental* API-surface gap, not a bug to fix.

---

## 2. Browser Engine Architecture

WhimKit is a classic staged pipeline. Each stage consumes the previous stage's
data structure and produces the next. The **foundation** package tree owns the
shared data structures; each **subsystem** owns the algorithms.

```
 URL
  │
  ▼
┌──────────────┐   bytes/headers   ┌──────────────┐   text
│  Networking  │ ────────────────▶ │  WebResponse │ ─────────┐
│ (net.http)   │                   │ (foundation) │          │
└──────────────┘                   └──────────────┘          ▼
                                                      ┌──────────────┐  Document
                                                      │ HTML Parser  │ ──────────┐
                                                      │ (html)       │           │
                                                      └──────────────┘           ▼
   ┌───────────────────────────────────────────────────────────┐   ┌──────────────┐
   │  CSS Engine (css.engine): UA + author sheets → cascade      │◀──│     DOM      │
   │  sets ComputedStyle on every Element                        │   │ (dom)        │
   └───────────────────────────────────────────────────────────┘   └──────┬───────┘
                                    │ styled DOM                            │
                                    ▼                                       │
                          ┌──────────────────┐   LayoutBox tree            │
                          │  Layout Engine    │ ─────────────┐              │
                          │  (layout.engine)  │              ▼              │
                          └──────────────────┘      ┌──────────────┐        │
                                                     │  Renderer    │        │
                                                     │  (render)    │──▶ Java2D on JPanel
                                                     └──────────────┘        │
                                    ┌────────────────────────────┐          │
                                    │  JS Runtime (js, Nashorn)   │──────────┘ mutate DOM
                                    │  window/document/timers      │  → reflow → re-layout/paint
                                    └────────────────────────────┘
```

The **UI** (`ui`, `app`) hosts tabs, address bar, history, and a `RenderPanel`,
and drives the pipeline through the interfaces below.

**Interface-driven seams (all in the foundation):**

- `net.ResourceLoader` — `WebResponse load(String url)`
- `html.HtmlParser` — `Document parse(String html, String baseUri)`
- `css.StyleEngine` — `addAuthorCss`, `styleDocument`, `getPropertyValue`
- `layout.LayoutEngine` — `LayoutBox layout(Document, float width, Graphics2D)`
- `render.Renderer` — `void paint(LayoutBox root, Graphics2D)`
- `js.ScriptRuntime` — `bind`, `execute`, `runInlineScripts`, `setReflowListener`

Because the engine coordinator depends only on these interfaces and on the
foundation data types (`dom.*`, `css.ComputedStyle`, `layout.LayoutBox`,
`net.WebResponse`), every subsystem compiles and is testable in isolation.

---

## 3. Package Structure

```
com.whimkit
├── dom          FOUNDATION — Node, Element, TextNode, CommentNode, Document
├── css          FOUNDATION — ComputedStyle, StyleEngine (iface)
│   └── engine   SUBSYSTEM  — tokenizer, parser, selectors, cascade
├── layout       FOUNDATION — LayoutBox, Dimensions, EdgeSizes, Rect, LayoutEngine (iface)
│   └── engine   SUBSYSTEM  — block/inline layout algorithm
├── net          FOUNDATION — WebResponse, Url, ResourceLoader (iface)
│   └── http     SUBSYSTEM  — HttpResourceLoader, cookies, cache, async
├── render       FOUNDATION — Renderer (iface)   SUBSYSTEM — Java2DRenderer, display list
├── html         FOUNDATION — HtmlParser (iface)  SUBSYSTEM — Html5Parser (tokenizer+builder)
├── js           FOUNDATION — ScriptRuntime (iface) SUBSYSTEM — NashornRuntime, DOM bindings
├── ui           SUBSYSTEM  — BrowserFrame, RenderPanel, TabBar, dev panel
└── app          SUBSYSTEM  — Main, BrowserEngine coordinator
```

**Rationale:** shared *state* lives in the foundation half of each package so
there is exactly one definition of each data structure and no subsystem depends
on another subsystem's package to compile. Algorithms live in `*.engine` / `*.http`
subpackages owned by a single implementer, eliminating file-ownership conflicts
during parallel development.

---

## 4. Networking Layer Design  (`com.whimkit.net.http`)

- **Transport:** `HttpURLConnection`/`HttpsURLConnection`. Manual redirect
  following (max 20) so cookies and the final URL are tracked correctly.
- **Headers:** browser-like `User-Agent`, `Accept`, `Accept-Language`,
  `Accept-Encoding: gzip, deflate`. Response gzip/deflate decoded transparently.
- **Cookies:** a process-wide `CookieManager` installed as the default
  `CookieHandler`, with `CookiePolicy.ACCEPT_ORIGINAL_SERVER`.
- **Charset negotiation:** `Content-Type` charset → `<meta charset>` sniff →
  UTF-8 fallback.
- **Cache:** an LRU `LinkedHashMap` (bounded by entry count and total bytes) keyed
  by URL, honoring `Cache-Control: no-store/max-age` heuristically; optional disk
  spill under a temp dir.
- **Async:** a fixed `ExecutorService`; the engine submits loads and the UI uses
  `SwingWorker` so results marshal to the EDT. Per-host connection reuse is left
  to the JDK's keep-alive pool.
- **Failure model:** never throws to callers — returns `WebResponse.failure(url,
  msg)`.

## 5. HTML Parser Design  (`com.whimkit.html`)

Two stages behind the `HtmlParser` interface:

1. **Tokenizer** — a state machine over the character stream emitting DOCTYPE,
   start-tag (+attributes, self-closing flag), end-tag, comment, and character
   tokens. Handles the practical entity set (`&amp; &lt; &gt; &quot; &#nn; &#xNN;`
   and common named entities), raw-text elements (`<script>`, `<style>`) that
   swallow markup until their matching end tag, and attribute quoting variants.
2. **Tree builder** — a simplified insertion-mode machine with an open-element
   stack. It implements implied `<html>/<head>/<body>`, auto-closing of
   optional-end-tag elements (`<p>`, `<li>`, `<td>`, `<tr>`, ...), the void-element
   set (`<br>`, `<img>`, `<input>`, ...), and misnest recovery so malformed pages
   still yield a sane tree. Output is the foundation `Document`.

## 6. DOM Architecture  (`com.whimkit.dom` — foundation, authored)

`Node` (parent, ordered children, owner document, mutation primitives, text
collection) with `Element` (lower-cased tag, ordered attribute map, class list,
computed-style hook, `getElementsByTagName`), `TextNode`, `CommentNode`, and
`Document` (factories, `getElementById`, tag queries, `body`/`head`, base URI).
Single-threaded by contract: mutation happens on the EDT or the JS thread, never
concurrently. This layer is complete and lives in source now.

## 7. CSS Engine Design  (`com.whimkit.css.engine`)

- **Tokenizer/parser** for stylesheets → `Stylesheet` of `Rule`s, each with a
  selector list and a declaration block. Tolerant of unknown at-rules/properties.
- **Selectors:** type, universal `*`, `.class`, `#id`, descendant, child `>`,
  grouping, and `[attr]`/`[attr=val]`. Specificity computed as (id, class/attr,
  type) triples.
- **Cascade:** UA default stylesheet (block/inline defaults, margins for
  headings/paragraphs/lists, link color/underline) < author sheets (source order,
  specificity, `!important`) < inline `style=""`.
- **Inheritance:** implemented via `ComputedStyle.deriveChild()` — inherited
  properties seed from the parent; non-inherited reset to initial; declared values
  overlay. Lengths resolved to px (`em`/`%` relative to parent font size /
  containing block where tractable). Produces one `ComputedStyle` per element via
  `Element.setComputedStyle`.

## 8. Layout Engine Design  (`com.whimkit.layout.engine`)

Builds a `LayoutBox` tree from the styled DOM (`display:none` → no box; block vs
inline formatting contexts; anonymous block boxes wrap inline runs). Implements
the **box model** (content/padding/border/margin), block flow with vertical
stacking and width/`auto`-margin resolution, and **inline layout** with line-box
breaking measured through `FontMetrics`. Basic `float:left/right` and
`text-align` supported; positioned (`absolute`/`fixed`) and flex/grid documented
as gaps. Output boxes carry absolute coordinates and resolved `href` for hit
testing.

## 9. Rendering Pipeline  (`com.whimkit.render`)

`Java2DRenderer` walks the positioned tree building a **display list**
(background rects, border strokes, text runs, images, list markers) then paints
in correct order (background/borders → content → children), honoring the
`Graphics2D` clip for dirty-region repaint. Text uses derived AWT `Font`s +
antialiasing; images come from a shared decode cache (`ImageIO`). Painting is
pure and side-effect free so it can run repeatedly for scroll/resize.

## 10. JavaScript Runtime Integration  (`com.whimkit.js`)

`NashornRuntime` creates a Nashorn `ScriptEngine`, installs a curated global
scope: `window`, `document` (with `getElementById`, `getElementsByTagName`,
`querySelector*` delegating to the CSS engine, `createElement`, `body`), element
wrappers (`textContent`, `innerHTML` best-effort, `setAttribute`, `style`,
`addEventListener`), `console.log`, and `setTimeout`/`setInterval`/`clearTimeout`
on a single-threaded timer that dispatches back on the engine thread. DOM
mutations flag the document dirty and invoke the reflow listener. **Documented
non-support:** `fetch`, `Promise`/microtasks, `requestAnimationFrame`, ES modules,
`localStorage` (optional shim), and layout-metric APIs return stubs.

## 11. Event System Design

DOM events are dispatched by the JS runtime: capture→target→bubble across the
ancestor chain for `click`/`input`/`load`, with `preventDefault`/
`stopPropagation`. UI-level events (link clicks from `LayoutBox.hitTestLink`, form
submit) are translated into DOM events first; if unhandled, the engine performs
the default action (navigate/submit).

## 12. Browser UI Architecture  (`com.whimkit.ui`, `com.whimkit.app`)

`BrowserFrame` (JFrame) hosts a `JTabbedPane` of tabs; each tab pairs a toolbar
(back/forward/reload/stop, address `JTextField`, progress) with a scrollable
`RenderPanel`. `RenderPanel` overrides `paintComponent` to invoke the `Renderer`,
translated by scroll offset, and forwards mouse events for link hit-testing and
hover cursors. A collapsible **dev panel** shows the DOM tree, computed styles for
a selected node, and a JS console. History/bookmarks are per-`BrowserEngine`.

## 13. Threading Model

- **EDT** owns all Swing state and all DOM/layout mutation triggered by UI.
- **Network worker pool** performs blocking I/O only; it hands back immutable
  `WebResponse` objects and marshals via `SwingWorker.done()`/`invokeLater`.
- **JS timers** run on a dedicated single thread but *post DOM work back to the
  EDT*, preserving the single-writer DOM invariant.
- Layout/paint always run on the EDT. `ComputedStyle`/`LayoutBox` are only touched
  on the EDT after the worker handoff, so no locks are needed on them.

## 14. Rendering Optimization Strategies

Dirty-region repaint (`repaint(x,y,w,h)` + clip-aware painter), a retained
display list rebuilt only on reflow (not on every scroll), image decode caching,
`Font`/`FontMetrics` caching keyed by (family,size,style), viewport culling
(skip boxes whose margin box misses the clip), and coalesced reflows (a single
`invokeLater` flush after a burst of DOM mutations).

## 15. Memory Management Strategies

Bounded LRU caches (resources, images, fonts) with byte ceilings; layout trees
discarded and rebuilt per load (no cross-page retention); `SoftReference` for the
image cache so it yields under pressure; explicit `ScriptRuntime.dispose()` to
cancel timers and drop the Nashorn scope on navigation, preventing listener/timer
leaks across page loads.

## 16–17. Source Code & Class Explanations

The foundation classes (this section's data structures and interfaces) are in
`src/com/whimkit/{dom,css,layout,net,html,render,js}` now. Each subsystem's
concrete classes live under its `*.engine`/`*.http` subpackage with class-level
Javadoc explaining purpose, key methods, and threading assumptions. See each
package's source; the interface Javadoc names the concrete implementation class.

## 18. Build Instructions

Requires a JDK (8+). From this `browser-engine/` directory:

```sh
javac --release 8 -d out $(find src -name '*.java')
```

(`--release 8` is only needed on a newer JDK; a genuine JDK 8 uses plain
`javac`. Nashorn is present in JDK 8–14; on newer JDKs the JS subsystem degrades
gracefully if `javax.script` has no `nashorn` engine.)

## 19. Run Instructions

```sh
java -cp out com.whimkit.app.Main            # opens the browser
java -cp out com.whimkit.app.Main <url>      # opens a URL on start
```

Type a URL in the address bar and press Enter; use back/forward/reload; toggle
the dev panel from the toolbar.

## 20. Known Limitations

No flexbox/grid/positioned-layout beyond basics; no `<video>`/`<audio>`/WebGL/
WASM; JS limited to Nashorn ES5.1(+partial ES6) against a DOM subset (no `fetch`/
`Promise`/`rAF`/layout metrics); CSS covers the common property set, not the full
spec; images limited to `ImageIO` raster formats (no SVG). Modern SPAs and modern
YouTube are out of reach by construction (see §1).

## 21. Future Expansion Roadmap

1. Positioned + float completeness, then a flexbox pass.
2. Incremental/streaming parse + progressive render.
3. A `Promise`/microtask + `fetch` shim layer for more JS compatibility.
4. SVG (parse + Java2D paint) and animated GIF playback.
5. A basic accessibility tree and find-in-page.

## 22. Incremental Development Plan

| Phase | Deliverable | Achieves |
|---|---|---|
| 1 | UI shell + tabs + navigation + `about:` pages | A running windowed browser. |
| 2 | Networking + HTML parser + DOM + minimal renderer | Loads & shows static text pages. |
| 3 | CSS engine + full block/inline layout | Styled, laid-out pages. |
| 4 | Nashorn runtime + DOM bindings + events | Simple interactive scripts run. |
| 5 | Render/memory optimizations (dirty regions, caches) | Smooth scroll/resize on large pages. |
| 6 | Partial modern support + documented gaps | Honest boundary; graceful degradation. |

This repository realizes the foundation (phase-1 data model + all pipeline
seams) and then fills the subsystems in parallel against these contracts.
