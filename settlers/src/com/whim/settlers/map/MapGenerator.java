package com.whim.settlers.map;

import java.util.Random;

/**
 * Seeded procedural map generator. Produces a {@link TileMap} from fractal value
 * noise so the same seed always yields the same map — important for reproducible
 * scenarios and debugging.
 *
 * <p>The pipeline is deliberately simple and dependency-free:
 * <ol>
 *   <li>an <b>elevation</b> field decides water / lowland / mountain,</li>
 *   <li>a <b>moisture</b> field turns lowland into grass / forest / desert,</li>
 *   <li>a <b>mineral</b> field assigns each mountain tile a resource.</li>
 * </ol>
 * All three are fractal value noise built from a seeded {@link Random}; no JDK-9+
 * APIs are used.
 */
public final class MapGenerator {

    private MapGenerator() { }

    public static TileMap generate(int width, int height, long seed) {
        Random rng = new Random(seed);
        float[] elevation = fractalNoise(width, height, seed ^ 0xA11CE, 5);
        float[] moisture  = fractalNoise(width, height, seed ^ 0xB0B, 4);
        float[] mineral   = fractalNoise(width, height, seed ^ 0xC0DE, 3);

        // A radial falloff pulls elevation down at the edges so maps tend to be
        // land in the middle with water around the rim — a natural playfield.
        // We bake the falloff in, then renormalise so the thresholds below always
        // span the field's full range (guaranteeing water, land, and mountains).
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                float e = elevation[idx] * falloff(x, y, width, height);
                elevation[idx] = e;
                if (e < lo) lo = e;
                if (e > hi) hi = e;
            }
        }
        float span = Math.max(1e-6f, hi - lo);

        TileMap map = new TileMap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                float e = (elevation[idx] - lo) / span; // now spans [0,1]
                TerrainType t;
                if (e < 0.30f) {
                    t = TerrainType.WATER;
                } else if (e > 0.74f) {
                    t = mountainFor(mineral[idx], rng);
                } else {
                    float m = moisture[idx];
                    if (m > 0.60f)      t = TerrainType.FOREST;
                    else if (m < 0.30f) t = TerrainType.DESERT;
                    else                t = TerrainType.GRASS;
                }
                map.set(x, y, t);
            }
        }
        carveRivers(map, seed);
        return map;
    }

    /**
     * Carve one or more thin, meandering rivers from near the map centre out to an
     * edge, adding inland water (and with it coastal build spots for fishermen and
     * shipyards). Fully deterministic (seeded) and mountain-preserving — rivers
     * flow around mountains rather than through them — so the terrain-variety and
     * determinism invariants the self-test checks still hold.
     */
    private static void carveRivers(TileMap map, long seed) {
        int w = map.width(), h = map.height();
        if (Math.min(w, h) < 24) return; // too small to bother
        Random rng = new Random(seed ^ 0x52_1F_E7L);
        int rivers = Math.max(1, Math.min(3, Math.max(w, h) / 40));
        for (int r = 0; r < rivers; r++) {
            double x = w * (0.35 + 0.30 * rng.nextDouble());
            double y = h * (0.35 + 0.30 * rng.nextDouble());
            // Head toward a random map edge (where the falloff has put the sea).
            double angle = rng.nextDouble() * Math.PI * 2;
            double dx = Math.cos(angle), dy = Math.sin(angle);
            int steps = (w + h);
            for (int s = 0; s < steps; s++) {
                int ix = (int) Math.round(x), iy = (int) Math.round(y);
                if (!map.inBounds(ix, iy)) break;
                TerrainType t = map.get(ix, iy);
                if (t.isWater()) break;              // reached the sea — done
                if (!t.isMountain()) map.set(ix, iy, TerrainType.WATER);
                // Meander: bias to the chosen heading with a little wander.
                angle += (rng.nextDouble() - 0.5) * 0.6;
                dx = Math.cos(angle); dy = Math.sin(angle);
                x += dx; y += dy;
            }
        }
    }

    /** Assign a mountain resource from the mineral field, weighted toward stone/coal. */
    private static TerrainType mountainFor(float m, Random rng) {
        if (m > 0.80f) return TerrainType.MOUNTAIN_GOLD;   // rarest
        if (m > 0.60f) return TerrainType.MOUNTAIN_IRON;
        if (m > 0.35f) return TerrainType.MOUNTAIN_COAL;
        return TerrainType.MOUNTAIN_STONE;
    }

    /** Radial falloff in [0,1]: ~1 at the centre, dropping toward the edges. */
    private static float falloff(int x, int y, int w, int h) {
        double nx = (x / (double) (w - 1)) * 2 - 1;
        double ny = (y / (double) (h - 1)) * 2 - 1;
        double d = Math.min(1.0, Math.sqrt(nx * nx + ny * ny) / 1.15);
        return (float) (1.0 - d * d);
    }

    /**
     * Fractal (fBm) value noise normalised to [0,1]. Sums {@code octaves} layers
     * of bilinearly-interpolated lattice noise at doubling frequency and halving
     * amplitude.
     */
    private static float[] fractalNoise(int w, int h, long seed, int octaves) {
        float[] out = new float[w * h];
        float amp = 1f, totalAmp = 0f;
        int freq = 2;
        Random seeder = new Random(seed);
        for (int o = 0; o < octaves; o++) {
            addLatticeOctave(out, w, h, freq, amp, seeder.nextLong());
            totalAmp += amp;
            amp *= 0.5f;
            freq *= 2;
        }
        // Normalise.
        for (int i = 0; i < out.length; i++) out[i] /= totalAmp;
        return out;
    }

    private static void addLatticeOctave(float[] out, int w, int h, int freq,
                                         float amp, long seed) {
        int gw = freq + 1, gh = freq + 1;
        float[] lattice = new float[gw * gh];
        Random rng = new Random(seed);
        for (int i = 0; i < lattice.length; i++) lattice[i] = rng.nextFloat();

        for (int y = 0; y < h; y++) {
            float gy = (y / (float) (h - 1)) * freq;
            int y0 = (int) gy; int y1 = Math.min(y0 + 1, gh - 1);
            float fy = smooth(gy - y0);
            for (int x = 0; x < w; x++) {
                float gx = (x / (float) (w - 1)) * freq;
                int x0 = (int) gx; int x1 = Math.min(x0 + 1, gw - 1);
                float fx = smooth(gx - x0);
                float top = lerp(lattice[y0 * gw + x0], lattice[y0 * gw + x1], fx);
                float bot = lerp(lattice[y1 * gw + x0], lattice[y1 * gw + x1], fx);
                out[y * w + x] += lerp(top, bot, fy) * amp;
            }
        }
    }

    private static float smooth(float t) { return t * t * (3 - 2 * t); } // smoothstep
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
