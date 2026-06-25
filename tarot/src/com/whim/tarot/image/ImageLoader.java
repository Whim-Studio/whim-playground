package com.whim.tarot.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe loader for card images with a two-tier cache:
 * an in-memory cache of decoded {@link BufferedImage}s plus an on-disk byte cache
 * under {@code java.io.tmpdir}. Network fetches happen on the calling (background)
 * thread — callers must never invoke {@link #load} on the Swing EDT.
 */
public final class ImageLoader {

    private static final ImageLoader INSTANCE = new ImageLoader();

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String USER_AGENT =
            "WhimTarot/1.0 (Java; public-domain RWS image fetch)";

    private final ConcurrentHashMap<String, BufferedImage> memoryCache =
            new ConcurrentHashMap<String, BufferedImage>();
    // Per-URL lock objects so concurrent loads of the SAME url don't double-fetch.
    private final ConcurrentHashMap<String, Object> locks =
            new ConcurrentHashMap<String, Object>();
    private final File cacheDir;

    private ImageLoader() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "whim-tarot-images");
        // best-effort; if it fails we simply skip the disk tier
        dir.mkdirs();
        this.cacheDir = dir;
    }

    /** Singleton accessor. */
    public static ImageLoader getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a cached image, fetching over HTTP(S) on first use.
     * Safe to call from any background thread; never call on the EDT for network I/O.
     *
     * @throws IOException on network/decoding failure
     */
    public BufferedImage load(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isEmpty()) {
            throw new IOException("imageUrl is null/empty");
        }
        BufferedImage cached = memoryCache.get(imageUrl);
        if (cached != null) {
            return cached;
        }
        Object lock = lockFor(imageUrl);
        synchronized (lock) {
            // Re-check inside the lock — another thread may have populated it.
            cached = memoryCache.get(imageUrl);
            if (cached != null) {
                return cached;
            }
            BufferedImage img = loadFromDisk(imageUrl);
            if (img == null) {
                byte[] bytes = fetch(imageUrl);
                img = decode(bytes);
                if (img == null) {
                    throw new IOException("Could not decode image: " + imageUrl);
                }
                writeToDisk(imageUrl, bytes);
            }
            memoryCache.put(imageUrl, img);
            return img;
        }
    }

    /** Non-throwing convenience: returns {@code null} instead of throwing. */
    public BufferedImage loadQuietly(String imageUrl) {
        try {
            return load(imageUrl);
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Object lockFor(String url) {
        Object existing = locks.get(url);
        if (existing != null) {
            return existing;
        }
        Object created = new Object();
        Object prev = locks.putIfAbsent(url, created);
        return prev != null ? prev : created;
    }

    private byte[] fetch(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        InputStream in = null;
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " for " + imageUrl);
            }
            in = conn.getInputStream();
            return readAll(in);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) { }
            }
            conn.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private BufferedImage loadFromDisk(String imageUrl) {
        File f = cacheFile(imageUrl);
        if (f == null || !f.isFile() || f.length() == 0) {
            return null;
        }
        try {
            return ImageIO.read(f);
        } catch (IOException e) {
            return null;
        }
    }

    private void writeToDisk(String imageUrl, byte[] bytes) {
        File f = cacheFile(imageUrl);
        if (f == null) {
            return;
        }
        // Write to a temp file then rename, so a partial write is never read as complete.
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        OutputStream out = null;
        try {
            out = new FileOutputStream(tmp);
            out.write(bytes);
            out.flush();
            out.close();
            out = null;
            if (!tmp.renameTo(f)) {
                // Fallback: destination may already exist on some platforms.
                if (f.exists()) {
                    f.delete();
                }
                tmp.renameTo(f);
            }
        } catch (IOException e) {
            // disk cache is best-effort; ignore
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) { }
            }
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    private File cacheFile(String imageUrl) {
        if (cacheDir == null) {
            return null;
        }
        return new File(cacheDir, hash(imageUrl) + extensionOf(imageUrl));
    }

    private static String extensionOf(String url) {
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        if (dot > slash && dot >= 0 && (path.length() - dot) <= 5) {
            return path.substring(dot).toLowerCase();
        }
        return ".img";
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (int i = 0; i < digest.length; i++) {
                int v = digest[i] & 0xff;
                if (v < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to a filesystem-safe encoding of the hash code.
            return "u" + Integer.toHexString(s.hashCode());
        }
    }
}
