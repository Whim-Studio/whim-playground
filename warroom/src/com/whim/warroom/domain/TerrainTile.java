package com.whim.warroom.domain;

/** A single grid cell: its biome plus a normalized elevation in [0,1]. */
public class TerrainTile {
    private final int col;
    private final int row;
    private Biome biome;
    private double elevation;

    public TerrainTile(int col, int row, Biome biome, double elevation) {
        this.col = col;
        this.row = row;
        this.biome = biome;
        this.elevation = elevation;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }
}
