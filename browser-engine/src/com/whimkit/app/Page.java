package com.whimkit.app;

import com.whimkit.dom.Document;
import com.whimkit.layout.LayoutBox;
import com.whimkit.net.WebResponse;

/**
 * The result of loading and preparing one page: the styled {@link Document}, the
 * originating {@link WebResponse}, and (once laid out by the render panel) the
 * root {@link LayoutBox}. A small mutable value object passed from the engine to
 * the UI.
 */
public final class Page {
    public final String requestedUrl;
    public final String finalUrl;
    public final Document document;
    public final WebResponse response;
    public final String error;      // non-null if the load failed
    public LayoutBox layoutRoot;    // set by the render panel after layout

    public Page(String requestedUrl, String finalUrl, Document document, WebResponse response, String error) {
        this.requestedUrl = requestedUrl;
        this.finalUrl = finalUrl;
        this.document = document;
        this.response = response;
        this.error = error;
    }

    public boolean isOk() {
        return error == null && document != null;
    }

    public String getTitle() {
        String t = document != null ? document.getTitle() : null;
        return (t == null || t.isEmpty()) ? finalUrl : t;
    }
}
