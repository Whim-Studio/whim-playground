package com.whimkit.js;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;
import com.whimkit.dom.Node;
import com.whimkit.dom.TextNode;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A JavaScript runtime bound to a {@link Document}, backed by the JDK's Nashorn
 * engine via the portable {@code javax.script} API only (no {@code jdk.nashorn.*}
 * imports, so this class compiles on any JDK).
 *
 * <p><b>Graceful degradation:</b> Nashorn was removed from the JDK in Java 15. If
 * {@code getEngineByName("nashorn")} returns {@code null}, every method becomes a
 * logged no-op and the browser still runs — it simply executes no script. On a
 * JDK 8–14 the runtime is live.</p>
 *
 * <p><b>Exposed API (a deliberate subset):</b> {@code window}, {@code document}
 * ({@code getElementById}, {@code getElementsByTagName}, {@code querySelector*},
 * {@code createElement}, {@code body}, {@code title}), element wrappers
 * ({@code getAttribute}/{@code setAttribute}, {@code textContent},
 * {@code innerHTML} best-effort, {@code appendChild}, {@code style.setProperty},
 * {@code addEventListener}), {@code console.log/warn/error}, and
 * {@code setTimeout}/{@code setInterval}/{@code clear*}.</p>
 *
 * <p><b>Not supported</b> (documented limits): {@code fetch}, {@code Promise}/
 * microtasks, {@code requestAnimationFrame}, ES modules, and layout-metric APIs.
 * These are fundamental gaps versus a modern engine (see DESIGN.md §10).</p>
 *
 * <p><b>Threading:</b> timer callbacks and DOM-event dispatch are routed through
 * a UI {@link Executor} (set by the browser to marshal onto the EDT), preserving
 * the single-writer-DOM invariant.</p>
 */
public final class NashornRuntime implements ScriptRuntime {

    private final ScriptEngine engine;
    private Document document;
    private Runnable reflowListener = null;
    private Executor uiExecutor = new Executor() {
        public void execute(Runnable r) { r.run(); }
    };
    private final Timer timer = new Timer("whimkit-js-timers", true);
    private final Map<Integer, TimerTask> tasks = new IdentityHashMap<Integer, TimerTask>();
    private final AtomicInteger timerIds = new AtomicInteger(1);
    private final IdentityHashMap<Element, ElementBridge> bridges = new IdentityHashMap<Element, ElementBridge>();
    private boolean warned = false;

    public NashornRuntime() {
        ScriptEngine e = null;
        try {
            e = new ScriptEngineManager().getEngineByName("nashorn");
        } catch (Throwable t) {
            e = null;
        }
        this.engine = e;
    }

    public boolean isAvailable() {
        return engine != null;
    }

    /** Sets the executor used to run timer/event callbacks (browser sets this to the EDT). */
    public void setUiExecutor(Executor executor) {
        if (executor != null) this.uiExecutor = executor;
    }

    @Override
    public void setReflowListener(Runnable listener) {
        this.reflowListener = listener;
    }

    void reflow() {
        if (reflowListener != null) {
            try { reflowListener.run(); } catch (RuntimeException ignore) { }
        }
    }

    @Override
    public void bind(Document doc) {
        this.document = doc;
        bridges.clear();
        if (engine == null) { warnOnce(); return; }
        try {
            engine.put("document", new DocumentBridge(doc, this));
            engine.put("window", new WindowBridge(this));
            engine.put("console", new ConsoleBridge());
            // Alias window members as globals so bare `document`, `setTimeout`, etc. work.
            engine.eval("var self = window; this.setTimeout = function(f,d){return window.setTimeout(f,d||0);};"
                    + "this.setInterval = function(f,d){return window.setInterval(f,d||0);};"
                    + "this.clearTimeout = function(id){return window.clearTimeout(id);};"
                    + "this.clearInterval = function(id){return window.clearInterval(id);};"
                    + "this.alert = function(m){console.log('[alert] '+m);};");
        } catch (ScriptException ex) {
            System.err.println("[js] bind failed: " + ex.getMessage());
        }
    }

    @Override
    public void execute(String source, String sourceName) {
        if (engine == null) { warnOnce(); return; }
        if (source == null || source.trim().isEmpty()) return;
        try {
            if (sourceName != null) engine.put(ScriptEngine.FILENAME, sourceName);
            engine.eval(source);
            reflow();
        } catch (ScriptException ex) {
            System.err.println("[js] error in " + sourceName + ": " + ex.getMessage());
        } catch (RuntimeException ex) {
            System.err.println("[js] runtime error: " + ex);
        }
    }

    @Override
    public void runInlineScripts() {
        if (engine == null || document == null) { warnOnce(); return; }
        for (Element script : document.getElementsByTagName("script")) {
            String type = script.getAttribute("type");
            if (type != null && !type.isEmpty()
                    && !type.toLowerCase(Locale.ROOT).contains("javascript")
                    && !type.equalsIgnoreCase("module")
                    && !type.equalsIgnoreCase("text/babel")) {
                continue; // data blocks, JSON-LD, etc.
            }
            String code = script.getTextContent();
            if (code != null && !code.trim().isEmpty()) {
                execute(code, "inline-script");
            }
        }
    }

    /** Dispatches a DOM event of {@code type} to the target element's listeners. */
    public void dispatchEvent(final Element target, final String type) {
        final ElementBridge b = bridges.get(target);
        if (b == null) return;
        uiExecutor.execute(new Runnable() {
            public void run() {
                b.fire(type);
                reflow();
            }
        });
    }

    @Override
    public void dispose() {
        timer.cancel();
        tasks.clear();
        bridges.clear();
    }

    ElementBridge bridgeFor(Element e) {
        if (e == null) return null;
        ElementBridge b = bridges.get(e);
        if (b == null) {
            b = new ElementBridge(e, this);
            bridges.put(e, b);
        }
        return b;
    }

    private void warnOnce() {
        if (!warned) {
            warned = true;
            System.err.println("[js] No Nashorn engine on this JDK (Java 15+ removed it); "
                    + "JavaScript is disabled. The browser runs without scripting.");
        }
    }

    // ------------------------------------------------------------------
    // Bridges exposed to script. All members are public for Nashorn access.
    // ------------------------------------------------------------------

    /** {@code console.*}. */
    public static final class ConsoleBridge {
        public void log(Object... args) { print("log", args); }
        public void info(Object... args) { print("info", args); }
        public void warn(Object... args) { print("warn", args); }
        public void error(Object... args) { print("error", args); }
        public void debug(Object... args) { print("debug", args); }
        private void print(String level, Object[] args) {
            StringBuilder sb = new StringBuilder("[console.").append(level).append("] ");
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(String.valueOf(args[i]));
                }
            }
            System.out.println(sb);
        }
    }

    /** {@code window} — timers plus a couple of globals. */
    public static final class WindowBridge {
        private final NashornRuntime rt;
        WindowBridge(NashornRuntime rt) { this.rt = rt; }

        public int setTimeout(final Runnable cb, double delay) {
            return schedule(cb, (long) Math.max(0, delay), false);
        }
        public int setInterval(final Runnable cb, double delay) {
            return schedule(cb, (long) Math.max(1, delay), true);
        }
        public void clearTimeout(int id) { cancel(id); }
        public void clearInterval(int id) { cancel(id); }

        private int schedule(final Runnable cb, long delay, boolean repeat) {
            if (cb == null) return -1;
            final int id = rt.timerIds.getAndIncrement();
            TimerTask task = new TimerTask() {
                public void run() {
                    rt.uiExecutor.execute(new Runnable() {
                        public void run() {
                            try { cb.run(); } catch (RuntimeException ex) {
                                System.err.println("[js] timer callback error: " + ex);
                            }
                            rt.reflow();
                        }
                    });
                }
            };
            rt.tasks.put(id, task);
            if (repeat) rt.timer.schedule(task, delay, delay);
            else rt.timer.schedule(task, delay);
            return id;
        }
        private void cancel(int id) {
            TimerTask t = rt.tasks.remove(id);
            if (t != null) t.cancel();
        }
    }

    /** {@code document}. */
    public static final class DocumentBridge {
        private final Document doc;
        private final NashornRuntime rt;
        DocumentBridge(Document doc, NashornRuntime rt) { this.doc = doc; this.rt = rt; }

        public ElementBridge getElementById(String id) {
            return rt.bridgeFor(doc.getElementById(id));
        }
        public ElementBridge[] getElementsByTagName(String tag) {
            List<Element> els = doc.getElementsByTagName(tag);
            ElementBridge[] out = new ElementBridge[els.size()];
            for (int i = 0; i < els.size(); i++) out[i] = rt.bridgeFor(els.get(i));
            return out;
        }
        public ElementBridge createElement(String tag) {
            return rt.bridgeFor(doc.createElement(tag));
        }
        /** Minimal selector support: {@code #id}, {@code .class}, {@code tag}. */
        public ElementBridge querySelector(String sel) {
            List<Element> m = SimpleSelector.match(doc, sel);
            return m.isEmpty() ? null : rt.bridgeFor(m.get(0));
        }
        public ElementBridge[] querySelectorAll(String sel) {
            List<Element> m = SimpleSelector.match(doc, sel);
            ElementBridge[] out = new ElementBridge[m.size()];
            for (int i = 0; i < m.size(); i++) out[i] = rt.bridgeFor(m.get(i));
            return out;
        }
        public ElementBridge getBody() { return rt.bridgeFor(doc.getBody()); }
        public String getTitle() { return doc.getTitle(); }
        public void setTitle(String t) { doc.setTitle(t); }
    }

    /** {@code element}. */
    public static final class ElementBridge {
        private final Element el;
        private final NashornRuntime rt;
        private final Map<String, List<Runnable>> listeners = new IdentityHashMap<String, List<Runnable>>();
        private final StyleBridge style;

        ElementBridge(Element el, NashornRuntime rt) {
            this.el = el; this.rt = rt; this.style = new StyleBridge(el, rt);
        }
        public Element node() { return el; }
        public String getTagName() { return el.getTagName(); }
        public String getAttribute(String n) { return el.getAttribute(n); }
        public void setAttribute(String n, String v) { el.setAttribute(n, v); rt.reflow(); }
        public String getId() { return el.getId(); }
        public void setId(String v) { el.setAttribute("id", v); }
        public String getClassName() { return el.getClassName(); }
        public void setClassName(String v) { el.setAttribute("class", v); rt.reflow(); }
        public String getTextContent() { return el.getTextContent(); }
        public void setTextContent(String t) {
            List<Node> kids = new ArrayList<Node>(el.getChildNodes());
            for (Node k : kids) el.removeChild(k);
            el.appendChild(new TextNode(t == null ? "" : t));
            rt.reflow();
        }
        /** innerHTML getter returns text; setter replaces children with a text node (no re-parse). */
        public String getInnerHTML() { return el.getTextContent(); }
        public void setInnerHTML(String html) { setTextContent(stripTags(html)); }
        public StyleBridge getStyle() { return style; }
        public ElementBridge appendChild(ElementBridge child) {
            if (child != null) { el.appendChild(child.el); rt.reflow(); }
            return child;
        }
        public void removeChild(ElementBridge child) {
            if (child != null) { el.removeChild(child.el); rt.reflow(); }
        }
        public ElementBridge[] getElementsByTagName(String tag) {
            List<Element> els = el.getElementsByTagName(tag);
            ElementBridge[] out = new ElementBridge[els.size()];
            for (int i = 0; i < els.size(); i++) out[i] = rt.bridgeFor(els.get(i));
            return out;
        }
        public void addEventListener(String type, Runnable handler) {
            if (type == null || handler == null) return;
            List<Runnable> l = listeners.get(type.toLowerCase(Locale.ROOT));
            if (l == null) { l = new ArrayList<Runnable>(); listeners.put(type.toLowerCase(Locale.ROOT), l); }
            l.add(handler);
        }
        public void click() { fire("click"); rt.reflow(); }
        void fire(String type) {
            List<Runnable> l = listeners.get(type == null ? "" : type.toLowerCase(Locale.ROOT));
            if (l == null) return;
            for (Runnable r : new ArrayList<Runnable>(l)) {
                try { r.run(); } catch (RuntimeException ex) {
                    System.err.println("[js] listener error: " + ex);
                }
            }
        }
        private static String stripTags(String s) {
            return s == null ? "" : s.replaceAll("<[^>]*>", "");
        }
    }

    /** {@code element.style} — writes into the element's ComputedStyle raw map. */
    public static final class StyleBridge {
        private final Element el;
        private final NashornRuntime rt;
        StyleBridge(Element el, NashornRuntime rt) { this.el = el; this.rt = rt; }
        public void setProperty(String name, String value) {
            if (el.getComputedStyle() != null && name != null) {
                el.getComputedStyle().raw.put(name.toLowerCase(Locale.ROOT), value);
            }
            rt.reflow();
        }
        public String getPropertyValue(String name) {
            if (el.getComputedStyle() == null || name == null) return "";
            String v = el.getComputedStyle().raw.get(name.toLowerCase(Locale.ROOT));
            return v == null ? "" : v;
        }
    }

    /** Tiny standalone selector matcher for {@code querySelector} (no CSS-engine dependency). */
    static final class SimpleSelector {
        static List<Element> match(Document doc, String sel) {
            List<Element> out = new ArrayList<Element>();
            if (doc == null || sel == null) return out;
            sel = sel.trim();
            if (sel.isEmpty()) return out;
            if (sel.startsWith("#")) {
                Element e = doc.getElementById(sel.substring(1));
                if (e != null) out.add(e);
            } else if (sel.startsWith(".")) {
                String cls = sel.substring(1);
                for (Element e : doc.getElementsByTagName("*")) {
                    if (e.getClassList().contains(cls)) out.add(e);
                }
            } else {
                out.addAll(doc.getElementsByTagName(sel));
            }
            return out;
        }
    }
}
