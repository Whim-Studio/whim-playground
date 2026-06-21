package com.janggi.ui;

import com.janggi.core.Piece;
import com.janggi.core.Side;

/** Maps pieces to the traditional Janggi glyphs used on the board. */
final class PieceGlyphs {

    private PieceGlyphs() {
    }

    static String label(Piece piece) {
        boolean cho = piece.side() == Side.CHO;
        switch (piece.type()) {
            case GENERAL:
                return cho ? "楚" : "漢"; // Cho / Han generals
            case GUARD:
                return "士";
            case HORSE:
                return "馬";
            case ELEPHANT:
                return "象";
            case CHARIOT:
                return "車";
            case CANNON:
                return cho ? "包" : "砲";
            case SOLDIER:
                return cho ? "卒" : "兵";
            default:
                return "?";
        }
    }
}
