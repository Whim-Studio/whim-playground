package com.whim.oggalaxy.ui;

import com.whim.oggalaxy.api.Views;

/** A UI panel that re-reads the latest game snapshot when the poll timer fires. */
public interface Refreshable {
    void refresh(Views.GameStateView state);
}
