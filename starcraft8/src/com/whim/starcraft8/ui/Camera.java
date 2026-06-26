package com.whim.starcraft8.ui;

import java.awt.Point;

/**
 * Maps continuous world tile coordinates &lt;-&gt; screen pixels for the world viewport.
 * Owns the pan offset (in pixels) and the tile pixel size. Pure presentation helper.
 */
final class Camera {

    static final int TILE = 16; // pixels per tile at base zoom

    private int mapW;
    private int mapH;
    private int viewW;
    private int viewH;

    // top-left of the viewport in world pixels
    private double offX = 0;
    private double offY = 0;

    void setMap(int mapW, int mapH) {
        this.mapW = mapW;
        this.mapH = mapH;
    }

    void setViewport(int viewW, int viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
        clamp();
    }

    void pan(double dxPixels, double dyPixels) {
        offX += dxPixels;
        offY += dyPixels;
        clamp();
    }

    /** Center the camera on a world tile (used by minimap clicks). */
    void centerOnTile(double tx, double ty) {
        offX = tx * TILE - viewW / 2.0;
        offY = ty * TILE - viewH / 2.0;
        clamp();
    }

    private void clamp() {
        double maxX = Math.max(0, mapW * TILE - viewW);
        double maxY = Math.max(0, mapH * TILE - viewH);
        if (offX < 0) offX = 0;
        if (offY < 0) offY = 0;
        if (offX > maxX) offX = maxX;
        if (offY > maxY) offY = maxY;
    }

    int worldToScreenX(double tileX) {
        return (int) Math.round(tileX * TILE - offX);
    }

    int worldToScreenY(double tileY) {
        return (int) Math.round(tileY * TILE - offY);
    }

    double screenToWorldX(int px) {
        return (px + offX) / TILE;
    }

    double screenToWorldY(int py) {
        return (py + offY) / TILE;
    }

    Point worldToScreen(double tileX, double tileY) {
        return new Point(worldToScreenX(tileX), worldToScreenY(tileY));
    }

    int tilePx() {
        return TILE;
    }
}
