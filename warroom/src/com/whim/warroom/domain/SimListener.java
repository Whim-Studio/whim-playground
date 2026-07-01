package com.whim.warroom.domain;

/**
 * Callback the UI implements to receive frames. Invoked on the engine's
 * background thread; implementations MUST marshal any Swing work onto the EDT
 * via {@code SwingUtilities.invokeLater}.
 */
public interface SimListener {
    void onFrame(SimSnapshot snap);
}
