package com.whimkit.net.http;

import com.whimkit.net.ResourceLoader;
import com.whimkit.net.WebResponse;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thin asynchronous facade over a synchronous {@link ResourceLoader}, backed by
 * a fixed thread pool. It lets the UI submit resource loads without blocking the
 * event dispatch thread; callers await the {@link Future} (or a {@code SwingWorker}
 * wraps it) and marshal the resulting {@link WebResponse} back to the EDT.
 *
 * <p>Because the underlying {@link HttpResourceLoader} never throws, the returned
 * {@link Future} completes normally with a {@link WebResponse} — a failed fetch is
 * a {@link WebResponse#failure} value, not a {@link java.util.concurrent.ExecutionException}.</p>
 *
 * <p><strong>Thread-safety.</strong> Submission is safe from any thread. The
 * wrapped loader must itself be safe for concurrent use (as {@link HttpResourceLoader}
 * is). Call {@link #shutdown()} when the pool is no longer needed to release threads.</p>
 */
public final class AsyncResourceLoader {

    /** Default worker pool size. */
    public static final int DEFAULT_POOL_SIZE = 6;

    private final ResourceLoader delegate;
    private final ExecutorService pool;

    /** Creates an async loader over a fresh {@link HttpResourceLoader} with the default pool size. */
    public AsyncResourceLoader() {
        this(new HttpResourceLoader(), DEFAULT_POOL_SIZE);
    }

    /**
     * Creates an async loader over the given synchronous loader.
     *
     * @param delegate the loader that performs the actual (blocking) fetch.
     * @param poolSize the number of worker threads (clamped to at least 1).
     */
    public AsyncResourceLoader(ResourceLoader delegate, int poolSize) {
        if (delegate == null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
        this.pool = Executors.newFixedThreadPool(Math.max(1, poolSize), new NamedDaemonFactory());
    }

    /**
     * Submits a load for asynchronous execution.
     *
     * @param url the URL to fetch.
     * @return a {@link Future} that completes with the {@link WebResponse} (which
     *         may be a {@link WebResponse#failure} on transport error).
     */
    public Future<WebResponse> load(final String url) {
        return pool.submit(new Callable<WebResponse>() {
            @Override
            public WebResponse call() {
                return delegate.load(url);
            }
        });
    }

    /** @return the synchronous loader this facade delegates to. */
    public ResourceLoader getDelegate() {
        return delegate;
    }

    /** Initiates an orderly shutdown; previously submitted loads still complete. */
    public void shutdown() {
        pool.shutdown();
    }

    /** Attempts to stop all active loads immediately and halts pending ones. */
    public void shutdownNow() {
        pool.shutdownNow();
    }

    /** Names worker threads and marks them daemon so they never block JVM exit. */
    private static final class NamedDaemonFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "whimkit-net-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
