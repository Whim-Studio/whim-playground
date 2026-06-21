package com.whim.ebs.spi;

import com.whim.ebs.domain.SessionState;

/**
 * Renders and exports an emotional bank statement from a {@link SessionState}.
 */
public interface ExportService {

    /**
     * Renders the formatted plain-text statement.
     *
     * @param state the session state
     * @return the formatted statement text
     */
    String render(SessionState state);

    /**
     * Exports the rendered statement to the given file.
     *
     * @param state  the session state
     * @param target the destination file
     * @throws java.io.IOException if writing fails
     */
    void exportToFile(SessionState state, java.io.File target) throws java.io.IOException;
}
