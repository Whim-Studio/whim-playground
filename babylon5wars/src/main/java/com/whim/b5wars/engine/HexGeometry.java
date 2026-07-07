package com.whim.b5wars.engine;

import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;
import com.whim.b5wars.model.Section;

/**
 * Firing-geometry helpers: bearing (which hexside a target lies on), relative facing (bearing
 * expressed in a ship's local frame), and the mapping from a hit facing to a hull section.
 *
 * <p>To stay consistent with whatever axial convention {@link Hex#neighbor(Facing)} uses, the
 * six unit direction vectors are derived at runtime from {@code from.neighbor(f)} rather than
 * hardcoded. Bearing is chosen as the facing whose direction vector best aligns (max cube dot
 * product) with the vector from-&gt;to.
 */
final class HexGeometry {

    private HexGeometry() {
    }

    /** Cube x from axial (q,r): x = q. */
    private static int cubeX(int q, int r) {
        return q;
    }

    /** Cube y from axial (q,r): y = -q - r. */
    private static int cubeY(int q, int r) {
        return -q - r;
    }

    /** Cube z from axial (q,r): z = r. */
    private static int cubeZ(int q, int r) {
        return r;
    }

    /**
     * Absolute bearing from {@code from} toward {@code to} as one of the 6 hexsides, or
     * {@code null} if the two hexes are the same.
     */
    static Facing bearing(Hex from, Hex to) {
        int tx = cubeX(to.getQ(), to.getR()) - cubeX(from.getQ(), from.getR());
        int ty = cubeY(to.getQ(), to.getR()) - cubeY(from.getQ(), from.getR());
        int tz = cubeZ(to.getQ(), to.getR()) - cubeZ(from.getQ(), from.getR());
        if (tx == 0 && ty == 0 && tz == 0) {
            return null;
        }
        Facing best = null;
        long bestDot = Long.MIN_VALUE;
        for (Facing f : Facing.values()) {
            Hex n = from.neighbor(f);
            int dx = cubeX(n.getQ(), n.getR()) - cubeX(from.getQ(), from.getR());
            int dy = cubeY(n.getQ(), n.getR()) - cubeY(from.getQ(), from.getR());
            int dz = cubeZ(n.getQ(), n.getR()) - cubeZ(from.getQ(), from.getR());
            long dot = (long) dx * tx + (long) dy * ty + (long) dz * tz;
            if (dot > bestDot) {
                bestDot = dot;
                best = f;
            }
        }
        return best;
    }

    /**
     * Express an absolute bearing in a ship's local frame (F = the ship's nose). Returns the
     * facing whose index is {@code (bearing - shipFacing) mod 6}.
     */
    static Facing relative(Facing bearing, Facing shipFacing) {
        int idx = ((bearing.index() - shipFacing.index()) % 6 + 6) % 6;
        return Facing.values()[idx];
    }

    /** The hull section exposed when a hit lands on the given (target-local) facing. */
    static Section sectionForFacing(Facing hitFacing) {
        switch (hitFacing) {
            case F:
                return Section.FORE;
            case B:
                return Section.AFT;
            case FR:
            case BR:
                return Section.STARBOARD;
            case FL:
            case BL:
                return Section.PORT;
            default:
                return Section.PRIMARY;
        }
    }
}
