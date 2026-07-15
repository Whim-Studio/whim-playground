package com.whim.settlers.io;

import com.whim.settlers.map.TerrainType;
import com.whim.settlers.map.TileMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a hand-built map from a simple text format: an optional {@code #}-comment
 * header, then one line per row of single-character terrain codes (see
 * {@link TerrainType#code()}). Rows are padded/truncated to the width of the
 * first data row so ragged files still load. Unknown characters become grass.
 *
 * <p>Example:
 * <pre>
 * # name: tutorial valley
 * ...fff...
 * ..fwww.f.
 * .cc.ww...
 * </pre>
 */
public final class MapLoader {

    private MapLoader() { }

    public static TileMap fromFile(Path path) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(r);
        }
    }

    /** Load a map bundled on the classpath (e.g. a built-in scenario). */
    public static TileMap fromResource(String resource) throws IOException {
        InputStream in = MapLoader.class.getResourceAsStream(resource);
        if (in == null) throw new IOException("map resource not found: " + resource);
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(r);
        }
    }

    public static TileMap parse(BufferedReader r) throws IOException {
        List<String> rows = new ArrayList<String>();
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("#") || line.trim().isEmpty()) continue;
            rows.add(line);
        }
        if (rows.isEmpty()) throw new IOException("map has no terrain rows");

        int width = rows.get(0).length();
        int height = rows.size();
        TileMap map = new TileMap(width, height);
        for (int y = 0; y < height; y++) {
            String row = rows.get(y);
            for (int x = 0; x < width; x++) {
                char c = x < row.length() ? row.charAt(x) : '.';
                map.set(x, y, TerrainType.fromCode(c));
            }
        }
        return map;
    }
}
