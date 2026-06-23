package com.tiwas.mahjong.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A completed set: a pung (3 identical), kong (4 identical) or chow (3-run).
 * Carries whether it is concealed, which affects scoring.
 */
public final class Meld {

    private final MeldType type;
    private final List<Tile> tiles;
    private final boolean concealed;

    public Meld(MeldType type, List<Tile> tiles, boolean concealed) {
        this.type = type;
        this.tiles = new ArrayList<Tile>(tiles);
        this.concealed = concealed;
    }

    public MeldType getType() {
        return type;
    }

    public List<Tile> getTiles() {
        return new ArrayList<Tile>(tiles);
    }

    public boolean isConcealed() {
        return concealed;
    }

    /** The representative (face) tile of the meld. */
    public Tile representative() {
        return tiles.get(0);
    }

    public boolean isPung() {
        return type == MeldType.PUNG;
    }

    public boolean isKong() {
        return type == MeldType.KONG;
    }

    public boolean isChow() {
        return type == MeldType.CHOW;
    }

    /** True if this meld is built from honour tiles (winds / dragons). */
    public boolean isHonourMeld() {
        return representative().isHonour();
    }

    /** True if this meld is built from terminal-or-honour tiles only. */
    public boolean isTerminalOrHonourMeld() {
        for (int i = 0; i < tiles.size(); i++) {
            if (!tiles.get(i).isTerminalOrHonour()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(concealed ? "[" : "(");
        for (int i = 0; i < tiles.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(tiles.get(i).code());
        }
        sb.append(concealed ? "]" : ")");
        return sb.toString();
    }
}
