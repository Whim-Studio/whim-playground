package com.tiwa.mahjong.engine;

import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClaimResolverTest {

    private final ClaimResolver resolver = new ClaimResolver();

    @Test
    public void mahjongBeatsKongBeatsPung() {
        Claim pung = new Claim(1, ClaimType.PUNG);
        Claim kong = new Claim(2, ClaimType.KONG);
        Claim mahjong = new Claim(3, ClaimType.MAHJONG);

        Optional<Claim> winner = resolver.resolve(Arrays.asList(pung, kong, mahjong), 0);
        assertTrue(winner.isPresent());
        assertEquals(ClaimType.MAHJONG, winner.get().getType());
    }

    @Test
    public void kongBeatsPung() {
        Claim pung = new Claim(1, ClaimType.PUNG);
        Claim kong = new Claim(2, ClaimType.KONG);

        Optional<Claim> winner = resolver.resolve(Arrays.asList(pung, kong), 0);
        assertTrue(winner.isPresent());
        assertEquals(ClaimType.KONG, winner.get().getType());
    }

    @Test
    public void multipleMahjongResolvedByCounterClockwiseTurnOrder() {
        // Discarder is seat 0. Counter-clockwise (decreasing index) order from 0 is 3, 2, 1.
        // Seat 3 is nearest counter-clockwise and must win over seats 2 and 1.
        Claim s1 = new Claim(1, ClaimType.MAHJONG);
        Claim s2 = new Claim(2, ClaimType.MAHJONG);
        Claim s3 = new Claim(3, ClaimType.MAHJONG);

        Optional<Claim> winner = resolver.resolve(Arrays.asList(s1, s2, s3), 0);
        assertTrue(winner.isPresent());
        assertEquals(3, winner.get().getSeatIndex());
    }

    @Test
    public void multipleMahjongNearestCounterClockwiseFromSeatTwo() {
        // Discarder seat 2. CCW order: 1, 0, 3. Seat 1 nearest, beats seat 0.
        Claim s0 = new Claim(0, ClaimType.MAHJONG);
        Claim s1 = new Claim(1, ClaimType.MAHJONG);

        Optional<Claim> winner = resolver.resolve(Arrays.asList(s0, s1), 2);
        assertTrue(winner.isPresent());
        assertEquals(1, winner.get().getSeatIndex());
    }

    @Test
    public void claimAtTimeoutBoundaryIsRejected() {
        // 6000 ms is exactly the window: too late.
        Claim late = new Claim(1, ClaimType.PUNG, ClaimResolver.CLAIM_TIMEOUT_MILLIS);
        Optional<Claim> winner = resolver.resolve(Arrays.asList(late), 0);
        assertFalse(winner.isPresent());
    }

    @Test
    public void claimJustBeforeTimeoutAccepted() {
        Claim inTime = new Claim(1, ClaimType.PUNG, ClaimResolver.CLAIM_TIMEOUT_MILLIS - 1);
        Optional<Claim> winner = resolver.resolve(Arrays.asList(inTime), 0);
        assertTrue(winner.isPresent());
        assertEquals(1, winner.get().getSeatIndex());
    }

    @Test
    public void timeoutWindowIsSixSeconds() {
        assertEquals(6, ClaimResolver.CLAIM_TIMEOUT_SECONDS);
        assertEquals(6000L, ClaimResolver.CLAIM_TIMEOUT_MILLIS);
    }

    @Test
    public void noClaimsYieldsEmpty() {
        assertFalse(resolver.resolve(java.util.Collections.<Claim>emptyList(), 0).isPresent());
    }

    @Test
    public void discarderCannotClaimOwnDiscard() {
        Claim self = new Claim(0, ClaimType.PUNG);
        assertFalse(resolver.resolve(Arrays.asList(self), 0).isPresent());
    }
}
