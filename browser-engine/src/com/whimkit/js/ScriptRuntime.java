package com.whimkit.js;

import com.whimkit.dom.Document;

/**
 * Contract for the JavaScript subsystem (Nashorn-backed).
 *
 * <p>Implemented by {@code com.whimkit.js.NashornRuntime}. A fresh runtime is
 * bound to one {@link Document}; it exposes a minimal {@code window}/{@code
 * document} object graph, {@code console}, and {@code setTimeout}/{@code
 * setInterval}. When script mutates the DOM, the runtime notifies the reflow
 * listener so the engine can re-style, re-layout, and repaint.</p>
 */
public interface ScriptRuntime {

    /** Binds the runtime to a document, creating {@code window}/{@code document} globals. */
    void bind(Document doc);

    /** Executes a script source string in the bound global scope. */
    void execute(String source, String sourceName);

    /**
     * Executes all {@code <script>} elements in the bound document in source
     * order (inline first; external script bodies must be pre-fetched and set as
     * text by the caller).
     */
    void runInlineScripts();

    /** Registers a callback invoked whenever script mutates the DOM or a timer fires. */
    void setReflowListener(Runnable listener);

    /** Releases timers/threads. Called when the page is unloaded. */
    void dispose();
}
