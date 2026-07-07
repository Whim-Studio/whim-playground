package com.whim.b5wars.ui;

import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Hex;

import java.awt.Polygon;
import java.awt.geom.Point2D;

/**
 * Flat-top hex ↔ pixel projection for the play area, consistent with the model's axial
 * {@link Hex#neighbor(Facing)} convention (F = north, B = south, the other four diagonal).
 *
 * <p>Centers: {@code x = originX + size*1.5*q}, {@code y = originY + size*√3*(r + q/2)}.
 * Adjacent hex centers are exactly {@code √3*size} apart, so one hex of range == {@code √3*size}
 * pixels — used for range rings and movement vectors.
 */
final class HexView {

    static final double SQRT3 = Math.sqrt(3.0);

    private final double size;      // center-to-vertex radius
    private final double originX;
    private final double originY;

    HexView(double size, double originX, double originY) {
        this.size = size;
        this.originX = originX;
        this.originY = originY;
    }

    double size() {
        return size;
    }

    /** Pixels per hex of distance (uniform for this flat-top layout). */
    double hexPitch() {
        return SQRT3 * size;
    }

    Point2D.Double center(int q, int r) {
        double x = originX + size * 1.5 * q;
        double y = originY + size * SQRT3 * (r + q / 2.0);
        return new Point2D.Double(x, y);
    }

    Point2D.Double center(Hex h) {
        return center(h.getQ(), h.getR());
    }

    /** The 6 corners of the flat-top hex at (q,r). */
    Polygon hexPolygon(int q, int r) {
        Point2D.Double c = center(q, r);
        Polygon p = new Polygon();
        for (int i = 0; i < 6; i++) {
            double ang = Math.toRadians(60.0 * i);
            int px = (int) Math.round(c.x + size * Math.cos(ang));
            int py = (int) Math.round(c.y + size * Math.sin(ang));
            p.addPoint(px, py);
        }
        return p;
    }

    /** Screen-space unit vector (dx,dy, y-down) a facing's nose points along. */
    static double[] facingUnit(Facing f) {
        double h = SQRT3 / 2.0; // 0.8660254
        switch (f) {
            case F:  return new double[] {0.0, -1.0};
            case FR: return new double[] {h, -0.5};
            case BR: return new double[] {h, 0.5};
            case B:  return new double[] {0.0, 1.0};
            case BL: return new double[] {-h, 0.5};
            case FL: return new double[] {-h, -0.5};
            default: return new double[] {0.0, -1.0};
        }
    }

    /** Compass angle (radians, screen y-down) of a facing's nose. */
    static double facingAngle(Facing f) {
        double[] u = facingUnit(f);
        return Math.atan2(u[1], u[0]);
    }

    /** Nearest hex (q,r) to a pixel point — inverse of {@link #center}. */
    int[] pixelToHex(double px, double py) {
        double qf = (px - originX) / (size * 1.5);
        double rf = (py - originY) / (size * SQRT3) - qf / 2.0;
        // Round in cube space for correctness.
        double xf = qf;
        double zf = rf;
        double yf = -xf - zf;
        int rx = (int) Math.round(xf);
        int ry = (int) Math.round(yf);
        int rz = (int) Math.round(zf);
        double dx = Math.abs(rx - xf);
        double dy = Math.abs(ry - yf);
        double dz = Math.abs(rz - zf);
        if (dx > dy && dx > dz) {
            rx = -ry - rz;
        } else if (dy > dz) {
            ry = -rx - rz;
        } else {
            rz = -rx - ry;
        }
        return new int[] {rx, rz};
    }
}
