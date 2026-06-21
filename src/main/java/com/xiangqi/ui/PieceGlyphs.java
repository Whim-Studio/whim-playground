package com.xiangqi.ui;

import com.xiangqi.core.Piece;
import com.xiangqi.core.PieceType;
import com.xiangqi.core.Side;

/**
 * Maps a {@link Piece} to the Chinese glyph traditionally printed on a Xiangqi
 * disc, with a single-letter Latin fallback for environments whose fonts cannot
 * render the CJK characters.
 *
 * <p>RED and BLACK use different characters for several piece types, exactly as
 * on a real board (e.g. RED's elephant is 相 while BLACK's is 象).
 */
final class PieceGlyphs {

    private PieceGlyphs() {
    }

    /** The traditional Chinese character for the piece, side-dependent. */
    static String glyph(Piece piece) {
        Side side = piece.side();
        switch (piece.type()) {
            case GENERAL:
                return side == Side.RED ? "帥" : "將"; // 帥 / 將
            case ADVISOR:
                return side == Side.RED ? "仕" : "士"; // 仕 / 士
            case ELEPHANT:
                return side == Side.RED ? "相" : "象"; // 相 / 象
            case HORSE:
                return "馬"; // 馬
            case CHARIOT:
                return "車"; // 車
            case CANNON:
                return side == Side.RED ? "炮" : "砲"; // 炮 / 砲
            case SOLDIER:
                return side == Side.RED ? "兵" : "卒"; // 兵 / 卒
            default:
                return "?";
        }
    }

    /** A single Latin letter used when the CJK glyph cannot be drawn. */
    static String fallbackLetter(PieceType type) {
        switch (type) {
            case GENERAL:
                return "G";
            case ADVISOR:
                return "A";
            case ELEPHANT:
                return "E";
            case HORSE:
                return "H";
            case CHARIOT:
                return "R"; // Rook-like
            case CANNON:
                return "C";
            case SOLDIER:
                return "S";
            default:
                return "?";
        }
    }
}
