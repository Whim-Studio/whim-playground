package com.whimkit.net.http;

import com.whimkit.net.WebResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A bounded, thread-safe in-memory LRU cache of {@link WebResponse} objects keyed
 * by request URL.
 *
 * <p>The cache is bounded two ways simultaneously: by <em>entry count</em> and by
 * total <em>body bytes</em>. Whenever inserting a new entry would breach either
 * ceiling, least-recently-used entries are evicted until both limits are again
 * satisfied. Access order (not insertion order) drives eviction, so a frequently
 * re-read resource stays warm.</p>
 *
 * <p><strong>Thread-safety.</strong> Every public method synchronizes on this
 * instance, so the cache is safe to share across the networking worker pool. The
 * stored {@link WebResponse} values are themselves immutable, so callers may read
 * them without further coordination.</p>
 *
 * <p>The cache stores only responses the HTTP layer deems cacheable (successful
 * GETs without {@code Cache-Control: no-store}); the decision is made by
 * {@link HttpResourceLoader}, not here. This class is a pure storage mechanism.</p>
 */
public final class ResourceCache {

    /** Default maximum number of cached entries. */
    public static final int DEFAULT_MAX_ENTRIES = 200;

    /** Default maximum total body size held in the cache (32 MiB). */
    public static final long DEFAULT_MAX_BYTES = 32L * 1024 * 1024;

    private final int maxEntries;
    private final long maxBytes;

    /** Guarded by {@code this}; running total of body bytes currently held. */
    private long currentBytes;

    /** Access-ordered map providing LRU semantics; guarded by {@code this}. */
    private final LinkedHashMap<String, WebResponse> map;

    /** Creates a cache with the default count/byte ceilings. */
    public ResourceCache() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_BYTES);
    }

    /**
     * Creates a cache with explicit ceilings.
     *
     * @param maxEntries maximum entry count (must be &gt; 0).
     * @param maxBytes   maximum total body bytes (must be &gt; 0).
     */
    public ResourceCache(int maxEntries, long maxBytes) {
        if (maxEntries <= 0) throw new IllegalArgumentException("maxEntries must be > 0");
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be > 0");
        this.maxEntries = maxEntries;
        this.maxBytes = maxBytes;
        // accessOrder=true → iteration/eldest reflects least-recently-accessed.
        this.map = new LinkedHashMap<String, WebResponse>(16, 0.75f, true);
    }

    /**
     * Looks up a cached response.
     *
     * @param url the request URL key.
     * @return the cached response, or {@code null} if absent.
     */
    public synchronized WebResponse get(String url) {
        if (url == null) return null;
        return map.get(url);
    }

    /**
     * Inserts (or replaces) a cached response, evicting LRU entries as needed to
     * honor the count and byte ceilings. An oversized single body (larger than the
     * whole byte budget) is silently skipped rather than evicting everything.
     *
     * @param url      the request URL key.
     * @param response the response to cache; ignored if {@code null}.
     */
    public synchronized void put(String url, WebResponse response) {
        if (url == null || response == null) return;
        long size = response.getBody().length;
        // A body that alone exceeds the whole budget is never worth caching.
        if (size > maxBytes) {
            remove(url);
            return;
        }
        WebResponse prev = map.remove(url);
        if (prev != null) currentBytes -= prev.getBody().length;

        map.put(url, response);
        currentBytes += size;

        evictIfNeeded();
    }

    /** Removes a single entry if present. */
    public synchronized void remove(String url) {
        if (url == null) return;
        WebResponse prev = map.remove(url);
        if (prev != null) currentBytes -= prev.getBody().length;
    }

    /** Empties the cache. */
    public synchronized void clear() {
        map.clear();
        currentBytes = 0;
    }

    /** @return the current number of cached entries. */
    public synchronized int size() {
        return map.size();
    }

    /** @return the current total body bytes held. */
    public synchronized long byteSize() {
        return currentBytes;
    }

    /** Evicts least-recently-used entries until both ceilings are satisfied. */
    private void evictIfNeeded() {
        // Iterator over an access-ordered map yields eldest (LRU) first.
        while ((map.size() > maxEntries || currentBytes > maxBytes) && !map.isEmpty()) {
            Map.Entry<String, WebResponse> eldest = map.entrySet().iterator().next();
            map.remove(eldest.getKey());
            currentBytes -= eldest.getValue().getBody().length;
        }
    }
}
