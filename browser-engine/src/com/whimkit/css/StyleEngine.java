package com.whimkit.css;

import com.whimkit.dom.Document;
import com.whimkit.dom.Element;

/**
 * Contract for the CSS subsystem: parse stylesheets, run the cascade, and attach
 * a {@link ComputedStyle} to every {@link Element} in a document.
 *
 * <p>Implemented by {@code com.whimkit.css.engine.CascadingStyleEngine}. The
 * engine ships with a User-Agent default stylesheet; author styles are added via
 * {@link #addAuthorCss} (from {@code <style>} blocks and linked sheets) before
 * {@link #styleDocument} is called.</p>
 */
public interface StyleEngine {

    /** Adds an author stylesheet source to the cascade (order = source order). */
    void addAuthorCss(String css);

    /** Clears any previously added author stylesheets (for a fresh page load). */
    void reset();

    /**
     * Runs selector matching + specificity + cascade + inheritance across the
     * whole tree, calling {@link Element#setComputedStyle} on each element. Also
     * honors the {@code style="..."} inline attribute at highest author priority.
     */
    void styleDocument(Document doc);

    /**
     * Resolves a single declared property for the JS {@code getComputedStyle}
     * binding. May return an empty string when unknown.
     */
    String getPropertyValue(Element element, String property);
}
