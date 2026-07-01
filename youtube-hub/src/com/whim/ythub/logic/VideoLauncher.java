package com.whim.ythub.logic;

import com.whim.ythub.model.VideoRecord;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Opens YouTube URLs in the operating system's default web browser.
 *
 * <p>Part of the logic layer ({@code com.whim.ythub.logic}); it deliberately
 * carries no Swing imports so it stays decoupled from the UI. Every launch path
 * is guarded and every failure is reported through a returned error string
 * rather than a thrown exception, so callers never need a try/catch.</p>
 *
 * <p>Written for strict Java 8 (no {@code var}, no text blocks, no post-8 APIs).</p>
 */
public final class VideoLauncher {

    /**
     * Attempts to open {@code url} in the default browser.
     *
     * @param url the URL to open
     * @return {@code null} on success, otherwise a user-facing error message
     */
    public String open(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "No URL was provided to open.";
        }
        String target = url.trim();

        if (!Desktop.isDesktopSupported()) {
            return "Your system does not support launching a browser.";
        }

        Desktop desktop;
        try {
            desktop = Desktop.getDesktop();
        } catch (UnsupportedOperationException ex) {
            return "Your system does not support launching a browser.";
        } catch (SecurityException ex) {
            return "Launching a browser was blocked by your system's security settings.";
        } catch (RuntimeException ex) {
            // e.g. HeadlessException on a headless environment.
            return "No graphical environment is available to open a browser.";
        }

        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            return "Your system does not support opening web pages automatically.";
        }

        URI uri;
        try {
            uri = new URI(target);
        } catch (URISyntaxException ex) {
            return "The URL is malformed and cannot be opened: " + target;
        }

        try {
            desktop.browse(uri);
            return null;
        } catch (IOException ex) {
            return "Unable to open the browser. Please copy the link manually: " + target;
        } catch (UnsupportedOperationException ex) {
            return "Your system does not support opening web pages automatically.";
        } catch (SecurityException ex) {
            return "Launching a browser was blocked by your system's security settings.";
        } catch (RuntimeException ex) {
            // Catch-all for headless / platform-specific failures so we never throw.
            return "Unable to open the browser for this link: " + target;
        }
    }

    /**
     * Convenience overload that opens the URL stored in a {@link VideoRecord}.
     *
     * @param record the record whose URL should be opened
     * @return {@code null} on success, otherwise a user-facing error message
     */
    public String open(VideoRecord record) {
        if (record == null) {
            return "No video was provided to open.";
        }
        return open(record.getUrl());
    }
}
