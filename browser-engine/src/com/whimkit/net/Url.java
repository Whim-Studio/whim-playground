package com.whimkit.net;

import java.net.URI;

/**
 * Small URL-resolution helper. The networking layer and the layout/render stages
 * both need to turn a relative reference (from {@code href}/{@code src}) into an
 * absolute URL against a document's base URI; this centralizes that logic on top
 * of {@link java.net.URI} so behavior stays consistent.
 */
public final class Url {

    private Url() {}

    /**
     * Resolves {@code ref} against {@code base}.
     *
     * @return an absolute URL string, or {@code ref} unchanged if resolution fails.
     */
    public static String resolve(String base, String ref) {
        if (ref == null) return base;
        ref = ref.trim();
        if (ref.isEmpty()) return base;
        // Already absolute (has a scheme like http:, https:, data:, mailto:).
        if (ref.matches("(?i)[a-z][a-z0-9+.-]*:.*")) return ref;
        try {
            URI b = new URI(base);
            return b.resolve(ref).toString();
        } catch (Exception e) {
            return ref;
        }
    }

    /** @return {@code true} for schemes the engine can actually fetch over the network. */
    public static boolean isFetchable(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file:")
                || u.startsWith("data:") || u.startsWith("about:");
    }
}
