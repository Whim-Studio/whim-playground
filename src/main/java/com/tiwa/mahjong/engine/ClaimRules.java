package com.tiwa.mahjong.engine;

import com.tiwa.mahjong.api.MeldType;

/**
 * Static legality checks for what may be claimed from a discard (Section 2-3).
 *
 * <ul>
 *   <li>PUNG: may be claimed from a discard.</li>
 *   <li>CHOW: may NOT be claimed from a discard (chows must be concealed).</li>
 *   <li>KONG: may NOT be formed from a discard (kong-from-discard is forbidden, Section 4).</li>
 *   <li>PAIR: never claimed (the pair is completed only on a winning Mahjong claim).</li>
 * </ul>
 *
 * <p>A Mahjong win is claimable from a discard, but it is a win rather than a {@link MeldType},
 * so it is handled by {@link ClaimResolver}/{@link ClaimType} rather than here.</p>
 */
public final class ClaimRules {

    private ClaimRules() {
    }

    /** True only for PUNG. Chow and Kong are not claimable from a discard. */
    public static boolean canClaimFromDiscard(MeldType meldType) {
        return meldType == MeldType.PUNG;
    }
}
