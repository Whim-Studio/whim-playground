package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.MeldType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClaimRulesTest {

    @Test
    public void pungCanBeClaimedFromDiscard() {
        assertTrue(ClaimRules.canClaimFromDiscard(MeldType.PUNG));
    }

    @Test
    public void chowCannotBeClaimedFromDiscard() {
        assertFalse(ClaimRules.canClaimFromDiscard(MeldType.CHOW));
    }

    @Test
    public void kongCannotBeClaimedFromDiscard() {
        assertFalse(ClaimRules.canClaimFromDiscard(MeldType.KONG));
        assertFalse(KongRules.canClaimKongFromDiscard());
    }

    @Test
    public void pairCannotBeClaimedFromDiscard() {
        assertFalse(ClaimRules.canClaimFromDiscard(MeldType.PAIR));
    }
}
