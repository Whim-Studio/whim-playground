package com.whimkit.render;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A small, thread-safe decode cache for images referenced by {@code <img>}.
 *
 * <p>Entries are held via {@link SoftReference} so the JVM can reclaim them under
 * memory pressure (a memory-management strategy described in DESIGN.md §15).
 * Decoding uses {@link ImageIO}, which covers PNG/JPEG/GIF/BMP — the raster
 * formats the standard library ships. SVG and animated GIF frames beyond the
 * first are out of scope.</p>
 */
public final class ImageStore {

    private final Map<String, SoftReference<Image>> cache =
            Collections.synchronizedMap(new LinkedHashMap<String, SoftReference<Image>>());

    /** @return the cached decoded image for {@code key}, or {@code null} if absent/reclaimed. */
    public Image get(String key) {
        if (key == null) return null;
        SoftReference<Image> ref = cache.get(key);
        return ref == null ? null : ref.get();
    }

    public void put(String key, Image img) {
        if (key != null && img != null) {
            cache.put(key, new SoftReference<Image>(img));
        }
    }

    /**
     * Decodes raw bytes into an {@link Image} and caches it under {@code key}.
     *
     * @return the decoded image, or {@code null} if the bytes are not a supported
     *         raster format (never throws).
     */
    public Image decode(String key, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            Image img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img != null) put(key, img);
            return img;
        } catch (Exception e) {
            return null;
        }
    }
}
