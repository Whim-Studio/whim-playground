package com.whimkit.net;

/**
 * Contract the networking subsystem exposes to the rest of the engine.
 *
 * <p>Implemented by {@code com.whimkit.net.http.HttpResourceLoader}. The engine
 * coordinator depends on this interface only, so the concrete HTTP stack can
 * evolve independently.</p>
 *
 * <p>Implementations must be usable from a background (worker) thread and must
 * never throw for network errors — failures come back as a {@link WebResponse}
 * whose {@link WebResponse#getError()} is non-null.</p>
 */
public interface ResourceLoader {

    /**
     * Fetches {@code url} synchronously (the caller supplies the worker thread),
     * following redirects and applying cookies/cache per the implementation.
     *
     * @param url an absolute URL.
     * @return the response, or a {@link WebResponse#failure} on transport error.
     */
    WebResponse load(String url);
}
