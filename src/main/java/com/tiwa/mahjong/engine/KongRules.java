package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.MeldType;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wall;

/**
 * Kong legality and replacement-draw handling (Section 4).
 *
 * <ul>
 *   <li>Concealed kong: four matching concealed tiles, declared on the player's own turn; draws a
 *       replacement; never robbable.</li>
 *   <li>Exposed kong upgrade: a player holding an EXPOSED PUNG who draws the 4th matching tile may
 *       upgrade it to an exposed kong on their own turn; draws a replacement.</li>
 *   <li>Kong from a discard: never allowed.</li>
 *   <li>Late kong: only during the player's own turn, before they discard. Once discarded, no kong
 *       may be declared until the player's next turn.</li>
 *   <li>Kong on the last tile: allowed, but there is no replacement tile, so the hand is a drawn
 *       game.</li>
 * </ul>
 */
public final class KongRules {

    private KongRules() {
    }

    /** Kong-from-discard is forbidden (Section 4). Always false. */
    public static boolean canClaimKongFromDiscard() {
        return false;
    }

    /**
     * Whether a kong may be declared right now, given turn context. Late kongs are legal only on the
     * player's own turn and only before they have discarded.
     *
     * @param isOwnTurn        true if the declaring player is the active player
     * @param hasAlreadyDiscarded true if the active player has already discarded this turn
     */
    public static boolean canDeclareKongNow(boolean isOwnTurn, boolean hasAlreadyDiscarded) {
        return isOwnTurn && !hasAlreadyDiscarded;
    }

    /**
     * Whether the player can declare a concealed kong of {@code tile}: they hold four matching
     * concealed tiles. (When the 4th was just drawn it is part of the concealed tiles passed in.)
     */
    public static boolean canDeclareConcealedKong(PlayerView player, Tile tile) {
        if (player == null || tile == null) {
            return false;
        }
        int count = 0;
        for (Tile t : player.getConcealedTiles()) {
            if (t.equals(tile)) {
                count++;
            }
        }
        return count >= 4;
    }

    /**
     * Whether the player can upgrade an existing exposed pung of {@code drawnTile} to an exposed
     * kong: they must already have an exposed (not concealed) pung of that tile and hold the 4th.
     */
    public static boolean canUpgradeExposedPung(PlayerView player, Tile drawnTile) {
        if (player == null || drawnTile == null) {
            return false;
        }
        boolean hasExposedPung = false;
        for (Meld meld : player.getMelds()) {
            if (meld.getType() == MeldType.PUNG && !meld.isConcealed()
                    && meld.representative().equals(drawnTile)) {
                hasExposedPung = true;
                break;
            }
        }
        if (!hasExposedPung) {
            return false;
        }
        for (Tile t : player.getConcealedTiles()) {
            if (t.equals(drawnTile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draw the replacement tile for a declared kong. If the wall is empty or the kong was declared
     * on the last tile, there is no replacement and the hand is a drawn game (Section 4).
     */
    public static KongResult drawReplacement(Wall wall) {
        if (wall == null) {
            throw new IllegalArgumentException("wall must not be null");
        }
        if (wall.isEmpty() || wall.isLastTile()) {
            return KongResult.drawnGame();
        }
        return KongResult.withReplacement(wall.drawReplacement());
    }
}
