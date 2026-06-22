package com.whim.babylon5.data;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.imageio.ImageIO;

/**
 * Loads card art lazily and legally. {@link #get(String)} returns immediately —
 * a cached image if available, otherwise a generated {@link #placeholder()} —
 * and fetches the real image asynchronously off the EDT. Fetched bytes are
 * cached on disk under {@code ~/.b5ccg/cache} keyed by a SHA-256 of the URL.
 *
 * <p>This class NEVER embeds copyrighted art: nothing ships in the jar, images
 * are only ever fetched from the public URLs supplied in the card data and
 * stored in the user's own cache directory.</p>
 */
public final class ImageLoader {

    private static final File CACHE_DIR =
            new File(System.getProperty("user.home", "."), ".b5ccg/cache");

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static final int PLACEHOLDER_W = 180;
    private static final int PLACEHOLDER_H = 252; // ~5:7 card ratio

    /** url -> decoded image (real art once loaded). */
    private static final ConcurrentHashMap<String, Image> MEMORY =
            new ConcurrentHashMap<String, Image>();

    /** urls currently being fetched, to avoid duplicate work. */
    private static final ConcurrentHashMap<String, Boolean> IN_FLIGHT =
            new ConcurrentHashMap<String, Boolean>();

    private static final Image PLACEHOLDER = makePlaceholder();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            new ThreadFactory() {
                private int n = 0;
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "b5-image-loader-" + (n++));
                    t.setDaemon(true);
                    return t;
                }
            });

    static {
        try { CACHE_DIR.mkdirs(); } catch (Throwable ignored) { }
    }

    private ImageLoader() { }

    /**
     * Return a cached image immediately if present, otherwise a placeholder, and
     * kick off an asynchronous load. Safe to call from the EDT.
     */
    public static Image get(String url) {
        if (url == null || url.trim().isEmpty()) return PLACEHOLDER;
        Image cached = MEMORY.get(url);
        if (cached != null) return cached;

        // Try the disk cache synchronously only if cheap; otherwise go async.
        if (IN_FLIGHT.putIfAbsent(url, Boolean.TRUE) == null) {
            POOL.submit(new Loader(url));
        }
        return PLACEHOLDER;
    }

    public static void preload(Collection<String> urls) {
        if (urls == null) return;
        for (String u : urls) {
            get(u);
        }
    }

    public static Image placeholder() {
        return PLACEHOLDER;
    }

    // ---------------------------------------------------------------------

    private static final class Loader implements Runnable {
        private final String url;
        Loader(String url) { this.url = url; }

        public void run() {
            try {
                Image img = loadFromDisk(url);
                if (img == null) {
                    byte[] bytes = fetch(url);
                    if (bytes != null) {
                        writeToDisk(url, bytes);
                        img = decode(bytes);
                    }
                }
                if (img != null) {
                    MEMORY.put(url, img);
                }
            } catch (Throwable t) {
                // Leave the placeholder in place on any failure.
            } finally {
                IN_FLIGHT.remove(url);
            }
        }
    }

    private static Image loadFromDisk(String url) {
        try {
            File f = cacheFile(url);
            if (f.isFile() && f.length() > 0) {
                return ImageIO.read(f);
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private static byte[] fetch(String url) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(url);
            String proto = u.getProtocol();
            if (!"http".equals(proto) && !"https".equals(proto)) return null;
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "b5ccg-prototype/1.0");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            InputStream in = conn.getInputStream();
            try {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                return out.toByteArray();
            } finally {
                in.close();
            }
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static Image decode(byte[] bytes) {
        try {
            return ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (Throwable t) {
            return null;
        }
    }

    private static void writeToDisk(String url, byte[] bytes) {
        try {
            File f = cacheFile(url);
            File parent = f.getParentFile();
            if (parent != null) parent.mkdirs();
            OutputStream os = new FileOutputStream(f);
            try {
                os.write(bytes);
            } finally {
                os.close();
            }
        } catch (Throwable ignored) { }
    }

    private static File cacheFile(String url) {
        return new File(CACHE_DIR, sha256(url) + ".img");
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            // Fallback: a stable but non-cryptographic key.
            return "u" + Integer.toHexString(s.hashCode());
        }
    }

    private static Image makePlaceholder() {
        BufferedImage img = new BufferedImage(PLACEHOLDER_W, PLACEHOLDER_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(28, 30, 46));
            g.fillRoundRect(0, 0, PLACEHOLDER_W, PLACEHOLDER_H, 16, 16);
            g.setColor(new Color(70, 76, 110));
            g.drawRoundRect(2, 2, PLACEHOLDER_W - 5, PLACEHOLDER_H - 5, 14, 14);
            g.setColor(new Color(150, 160, 200));
            g.drawString("BABYLON 5", 44, PLACEHOLDER_H / 2 - 6);
            g.drawString("loading...", 56, PLACEHOLDER_H / 2 + 14);
        } finally {
            g.dispose();
        }
        return img;
    }
}
