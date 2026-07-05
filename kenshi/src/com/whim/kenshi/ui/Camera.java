package com.whim.kenshi.ui;

import com.whim.kenshi.api.Config;

/**
 * World &harr; screen transform with pan and zoom. The camera is defined by the
 * world coordinate sitting at the centre of the viewport ({@code cx,cy}) and a
 * {@code zoom} factor (screen-pixels per world-unit). All game logic works in
 * world units; only the {@link Renderer} and {@link InputHandler} convert.
 */
public final class Camera {

    private double cx;
    private double cy;
    private double zoom = Config.DEFAULT_ZOOM;

    private int viewW = Config.VIEW_W;
    private int viewH = Config.VIEW_H;

    public Camera() {
        // Start centred on the middle of the world; callers usually re-centre.
        this.cx = Config.WORLD_SIZE * 0.5;
        this.cy = Config.WORLD_SIZE * 0.5;
    }

    public void setViewport(int w, int h) {
        if (w > 0) this.viewW = w;
        if (h > 0) this.viewH = h;
        clamp();
    }

    public int viewW() { return viewW; }
    public int viewH() { return viewH; }
    public double zoom() { return zoom; }
    public double centerX() { return cx; }
    public double centerY() { return cy; }

    public void centerOn(double worldX, double worldY) {
        this.cx = worldX;
        this.cy = worldY;
        clamp();
    }

    // ---- transforms ------------------------------------------------------
    public double toScreenX(double worldX) { return (worldX - cx) * zoom + viewW * 0.5; }
    public double toScreenY(double worldY) { return (worldY - cy) * zoom + viewH * 0.5; }
    public double toWorldX(double screenX) { return (screenX - viewW * 0.5) / zoom + cx; }
    public double toWorldY(double screenY) { return (screenY - viewH * 0.5) / zoom + cy; }

    /** Convert a world length to on-screen pixels. */
    public double scale(double worldLen) { return worldLen * zoom; }

    // ---- panning ---------------------------------------------------------
    /** Pan the camera by a number of SCREEN pixels (e.g. drag / edge-scroll). */
    public void panScreen(double dxScreen, double dyScreen) {
        this.cx -= dxScreen / zoom;
        this.cy -= dyScreen / zoom;
        clamp();
    }

    /** Pan the camera by a number of WORLD units. */
    public void panWorld(double dxWorld, double dyWorld) {
        this.cx += dxWorld;
        this.cy += dyWorld;
        clamp();
    }

    // ---- zooming ---------------------------------------------------------
    /**
     * Multiply the zoom by {@code factor} while keeping the world point that is
     * currently under ({@code anchorScreenX,anchorScreenY}) fixed on screen.
     */
    public void zoomBy(double factor, double anchorScreenX, double anchorScreenY) {
        double beforeWx = toWorldX(anchorScreenX);
        double beforeWy = toWorldY(anchorScreenY);
        double nz = clampZoom(zoom * factor);
        if (nz == zoom) return;
        this.zoom = nz;
        // shift centre so the anchor world point maps back to the same screen px
        double afterWx = toWorldX(anchorScreenX);
        double afterWy = toWorldY(anchorScreenY);
        this.cx += beforeWx - afterWx;
        this.cy += beforeWy - afterWy;
        clamp();
    }

    public void setZoom(double z) {
        this.zoom = clampZoom(z);
        clamp();
    }

    private static double clampZoom(double z) {
        if (z < Config.MIN_ZOOM) return Config.MIN_ZOOM;
        if (z > Config.MAX_ZOOM) return Config.MAX_ZOOM;
        return z;
    }

    /** Keep the camera centre within the world bounds (with a little slack so
     * the map edge can reach the middle of the viewport). */
    private void clamp() {
        double min = 0.0;
        double max = Config.WORLD_SIZE;
        if (cx < min) cx = min;
        if (cx > max) cx = max;
        if (cy < min) cy = min;
        if (cy > max) cy = max;
    }
}
