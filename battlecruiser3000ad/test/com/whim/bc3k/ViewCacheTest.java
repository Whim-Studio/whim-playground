package com.whim.bc3k;

import com.whim.bc3k.api.Enums;
import com.whim.bc3k.engine.Engine;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * Verifies the render-path view projections are cached (review finding #2): the same
 * instance is returned across frames when nothing changed, and rebuilt when the
 * underlying model container is swapped.
 */
public class ViewCacheTest {

    @Test public void listProjectionsAreStableAcrossFrames() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "Cache");
        assertSame(e.view().crew(), e.view().crew());
        assertSame(e.view().craft(), e.view().craft());
        assertSame(e.view().galaxy().systems(), e.view().galaxy().systems());
    }

    @Test public void scalarWrappersAreStableAcrossFrames() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.XTREME_CARNAGE, "Cache");
        assertSame(e.view().combat(), e.view().combat());
        assertSame(e.view().ship(), e.view().ship());
    }

    @Test public void galaxyCacheRebuildsWhenGalaxyIsReplaced() {
        Engine e = new Engine();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "A");
        List<?> first = e.view().galaxy().systems();
        e.newGame(Enums.GameMode.FREE_FLIGHT, "B");   // fresh Galaxy instance
        List<?> second = e.view().galaxy().systems();
        assertNotSame(first, second);
    }
}
