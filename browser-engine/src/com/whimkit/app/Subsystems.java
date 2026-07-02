package com.whimkit.app;

import com.whimkit.css.StyleEngine;
import com.whimkit.html.HtmlParser;
import com.whimkit.layout.LayoutEngine;
import com.whimkit.layout.engine.BlockLayoutEngine;
import com.whimkit.net.ResourceLoader;
import com.whimkit.render.Java2DRenderer;

/**
 * Wires the engine's pluggable subsystems behind their foundation interfaces.
 *
 * <p>The parse/style/network subsystems are built as independent sibling tasks
 * and may not all be present on the classpath during incremental development. So
 * they are located by class name via reflection, each with a built-in
 * <em>fallback</em> implementation. This is the concrete payoff of the
 * interface-driven architecture: the browser always assembles and runs, and
 * transparently upgrades to a real subsystem the moment its classes are added.
 * The layout and render subsystems live in this repository and are referenced
 * directly.</p>
 */
public final class Subsystems {

    public final ResourceLoader loader;
    public final HtmlParser parser;
    public final StyleEngine styles;
    public final LayoutEngine layout;
    public final Java2DRenderer renderer;

    public Subsystems() {
        this.loader = (ResourceLoader) instantiate(
                "com.whimkit.net.http.HttpResourceLoader", new FallbackResourceLoader(), "networking");
        this.parser = (HtmlParser) instantiate(
                "com.whimkit.html.Html5Parser", new FallbackHtmlParser(), "HTML parser");
        this.styles = (StyleEngine) instantiate(
                "com.whimkit.css.engine.CascadingStyleEngine", new FallbackStyleEngine(), "CSS engine");
        this.layout = new BlockLayoutEngine();
        this.renderer = new Java2DRenderer();
    }

    private static Object instantiate(String className, Object fallback, String label) {
        try {
            Class<?> c = Class.forName(className);
            Object impl = c.getDeclaredConstructor().newInstance();
            System.out.println("[subsystems] using " + label + ": " + className);
            return impl;
        } catch (Throwable t) {
            System.out.println("[subsystems] " + label + " subsystem not present; using built-in fallback ("
                    + fallback.getClass().getSimpleName() + ")");
            return fallback;
        }
    }
}
