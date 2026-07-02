package com.whim.browser.engine;

import com.whim.browser.model.WebResponse;

/**
 * Callback contract between the {@link BrowserEngine} and the Swing UI.
 *
 * <p>Both methods are guaranteed to be invoked on the AWT Event Dispatch
 * Thread (EDT), so implementations may freely touch Swing components without
 * additional marshalling. The engine performs all blocking network work on a
 * background {@link javax.swing.SwingWorker} thread and only hops back onto the
 * EDT to fire these callbacks.</p>
 */
public interface EngineCallback {

    /**
     * Invoked on the EDT immediately before background work begins. A UI can
     * use this to show a spinner, disable the address bar, or update the title.
     *
     * @param url the (normalized) URL that is about to be loaded.
     */
    void onStart(String url);

    /**
     * Invoked on the EDT once loading has finished (successfully or not). The
     * supplied {@link WebResponse} describes the outcome: rendered HTML, a
     * YouTube delegation to the native browser, or an error.
     *
     * @param response the immutable result of the load.
     */
    void onResult(WebResponse response);
}
