package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.GameContext;
import com.tiwa.mahjong.api.PlayerView;
import com.tiwa.mahjong.api.Tile;
import com.tiwa.mahjong.api.Wall;
import com.tiwa.mahjong.api.Wind;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TurnControllerTest {

    @Test
    public void advancesCounterClockwiseByDecreasingSeat() {
        assertEquals(3, TurnController.nextSeatCounterClockwise(0));
        assertEquals(2, TurnController.nextSeatCounterClockwise(3));
        assertEquals(1, TurnController.nextSeatCounterClockwise(2));
        assertEquals(0, TurnController.nextSeatCounterClockwise(1));
    }

    @Test
    public void advanceTurnUpdatesContext() {
        FakeContext ctx = new FakeContext();
        ctx.current = 0;
        TurnController controller = new TurnController(ctx);
        assertEquals(3, controller.advanceTurn());
        assertEquals(3, ctx.current);
        assertEquals(2, controller.advanceTurn());
    }

    @Test
    public void mahjongDuringReplacementOnlyFromActivePlayer() {
        FakeContext ctx = new FakeContext();
        ctx.current = 2;
        TurnController controller = new TurnController(ctx);
        assertTrue(controller.isMahjongAcceptableDuringReplacementDraw(2));
        assertFalse(controller.isMahjongAcceptableDuringReplacementDraw(0));
        assertFalse(controller.isMahjongAcceptableDuringReplacementDraw(1));
    }

    private static final class FakeContext implements GameContext {
        int current;

        public List<? extends PlayerView> getPlayers() {
            return new ArrayList<PlayerView>();
        }

        public Wall getWall() {
            return null;
        }

        public Wind getRoundWind() {
            return Wind.EAST;
        }

        public int getDealerIndex() {
            return 0;
        }

        public int getCurrentPlayerIndex() {
            return current;
        }

        public void setCurrentPlayerIndex(int seatIndex) {
            this.current = seatIndex;
        }

        public List<Tile> getDiscardPile() {
            return new ArrayList<Tile>();
        }
    }
}
