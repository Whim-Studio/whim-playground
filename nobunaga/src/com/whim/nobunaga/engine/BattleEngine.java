package com.whim.nobunaga.engine;

import com.whim.nobunaga.domain.BattleState;
import com.whim.nobunaga.domain.BattleUnit;
import com.whim.nobunaga.domain.Daimyo;
import com.whim.nobunaga.domain.GameState;
import com.whim.nobunaga.domain.Province;

import java.awt.Color;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Grid tactical battles: unit setup, daily movement (player orders + AI),
 * melee resolution, supply burn, and victory detection. All randomness draws
 * from {@link GameState#rng} (stored at battle start so daily resolution stays
 * seeded). Queued player orders are kept off the {@link BattleState} in an
 * identity map so the domain class needs no extra field.
 */
public class BattleEngine {

    private static final int MAX_DAYS = 30;
    private static final int UNITS_PER_SIDE = 4;

    /** rng + queued orders, kept per live battle. */
    private final Map<BattleState, java.util.Random> rngs = new IdentityHashMap<BattleState, java.util.Random>();
    private final Map<BattleState, Map<Integer, int[]>> orders =
            new IdentityHashMap<BattleState, Map<Integer, int[]>>();

    public BattleState startBattle(GameState s, int attackerProvId, int defenderProvId,
                                   int committedSoldiers, int committedRice) {
        Province from = s.province(attackerProvId);
        Province to = s.province(defenderProvId);
        int attackerDaimyo = from.getOwnerId();
        int defenderDaimyo = to.getOwnerId(); // may be -1 (neutral garrison)

        BattleState b = new BattleState(attackerProvId, defenderProvId, attackerDaimyo, defenderDaimyo);
        b.attackerRice = committedRice;
        b.defenderRice = Math.max(1, to.getRice());

        // March the committed force out of the attacking fief now.
        from.setSoldiers(Math.max(0, from.getSoldiers() - committedSoldiers));
        from.setRice(Math.max(0, from.getRice() - committedRice));

        Color atkColor = colorOf(s, attackerDaimyo, new Color(140, 140, 140));
        Color defColor = colorOf(s, defenderDaimyo, new Color(110, 110, 110));
        String atkAbbrev = abbrevOf(s, attackerDaimyo, "ATK");
        String defAbbrev = abbrevOf(s, defenderDaimyo, "DEF");

        int defenders = Math.max(0, to.getSoldiers());
        int uid = 0;
        uid = deploy(b, attackerDaimyo, true, committedSoldiers, 1, atkAbbrev, atkColor, uid);
        uid = deploy(b, defenderDaimyo, false, defenders, b.cols - 2, defAbbrev, defColor, uid);

        rngs.put(b, s.rng);
        orders.put(b, new java.util.HashMap<Integer, int[]>());
        b.log = "Battle for " + to.getName() + " begins.";
        return b;
    }

    private int deploy(BattleState b, int daimyoId, boolean attacker, int totalTroops,
                       int col, String abbrev, Color color, int uid) {
        int n = totalTroops <= 0 ? 1 : Math.min(UNITS_PER_SIDE, Math.max(1, totalTroops / 200 + 1));
        int per = totalTroops / n;
        int rem = totalTroops - per * n;
        // Spread units vertically and centre the column block.
        int startRow = (b.rows - n) / 2;
        for (int i = 0; i < n; i++) {
            int troops = per + (i == 0 ? rem : 0);
            boolean commander = (i == 0);
            int row = startRow + i;
            if (row < 0) row = 0;
            if (row >= b.rows) row = b.rows - 1;
            b.units.add(new BattleUnit(uid++, daimyoId, attacker, commander, col, row,
                    troops, abbrev, color));
        }
        return uid;
    }

    /** Queue a player move/attack for next {@link #battleAdvanceDay}. */
    public void issueOrder(BattleState b, int unitId, int targetCol, int targetRow) {
        Map<Integer, int[]> q = orders.get(b);
        if (q == null) {
            q = new java.util.HashMap<Integer, int[]>();
            orders.put(b, q);
        }
        q.put(Integer.valueOf(unitId), new int[]{targetCol, targetRow});
    }

    public boolean battleResolved(BattleState b) {
        return b.winnerDaimyoId != null;
    }

    /** Resolve a single day: movement, melee, supply burn, victory check. */
    public void battleAdvanceDay(BattleState b) {
        if (battleResolved(b)) {
            return;
        }
        // rng is registered in startBattle and only removed once resolved; the
        // early return above guarantees it is present here.
        java.util.Random rng = rngs.get(b);
        Map<Integer, int[]> q = orders.get(b);
        if (q == null) {
            q = new java.util.HashMap<Integer, int[]>();
        }

        // 1. Movement: player-ordered units head for their target; everyone else
        //    advances toward the nearest living enemy.
        for (BattleUnit u : b.units) {
            if (!u.isAlive()) {
                continue;
            }
            int[] tgt = q.get(Integer.valueOf(u.getId()));
            if (tgt == null) {
                tgt = nearestEnemyTile(b, u);
            }
            if (tgt != null) {
                stepToward(b, u, tgt[0], tgt[1]);
            }
        }
        q.clear(); // orders are single-use

        // 2. Melee: compute all damage first, then apply (order-independent).
        Map<Integer, Integer> dmg = new java.util.HashMap<Integer, Integer>();
        for (BattleUnit a : b.units) {
            if (!a.isAlive()) continue;
            for (BattleUnit d : b.units) {
                if (!d.isAlive() || d.getDaimyoId() == a.getDaimyoId()) continue;
                if (a.isAttacker() == d.isAttacker()) continue;
                if (adjacent(a, d)) {
                    int hit = (int) Math.round(a.getTroops() * (a.getMorale() / 100.0)
                            * (0.06 + 0.06 * rng.nextDouble()));
                    Integer key = Integer.valueOf(d.getId());
                    Integer prev = dmg.get(key);
                    dmg.put(key, Integer.valueOf((prev == null ? 0 : prev.intValue()) + hit));
                }
            }
        }
        for (BattleUnit u : b.units) {
            Integer hit = dmg.get(Integer.valueOf(u.getId()));
            if (hit == null) continue;
            u.setTroops(Math.max(0, u.getTroops() - hit.intValue()));
            int morale = u.getMorale() - (5 + rng.nextInt(11));
            u.setMorale(Math.max(0, morale));
            if (u.getMorale() <= 0 && !u.isCommander() && rng.nextDouble() < 0.5) {
                u.setTroops(0); // routed off the field
            }
        }

        // 3. Supply burn proportional to surviving troops.
        b.attackerRice = Math.max(0, b.attackerRice - supplyBurn(livingTroops(b, true)));
        b.defenderRice = Math.max(0, b.defenderRice - supplyBurn(livingTroops(b, false)));

        // 4. Victory check (commander → wipeout → starvation → siege cap).
        b.log = "Day " + b.day + ": A " + livingTroops(b, true) + " (rice " + b.attackerRice
                + ") vs D " + livingTroops(b, false) + " (rice " + b.defenderRice + ")";
        b.winnerDaimyoId = decideWinner(b);
        if (b.winnerDaimyoId != null) {
            b.log += "  — winner: daimyo " + b.winnerDaimyoId;
            rngs.remove(b);
            orders.remove(b);
        }
        b.day++;
    }

    private Integer decideWinner(BattleState b) {
        boolean atkCmd = commanderAlive(b, true);
        boolean defCmd = commanderAlive(b, false);
        if (!atkCmd && defCmd) return Integer.valueOf(b.defenderDaimyoId);
        if (!defCmd && atkCmd) return Integer.valueOf(b.attackerDaimyoId);
        if (!atkCmd && !defCmd) return Integer.valueOf(b.defenderDaimyoId); // double-KO → defender holds

        int atk = livingTroops(b, true);
        int def = livingTroops(b, false);
        if (atk <= 0 && def > 0) return Integer.valueOf(b.defenderDaimyoId);
        if (def <= 0 && atk > 0) return Integer.valueOf(b.attackerDaimyoId);
        if (atk <= 0 && def <= 0) return Integer.valueOf(b.defenderDaimyoId);

        if (b.attackerRice <= 0) return Integer.valueOf(b.defenderDaimyoId);
        if (b.defenderRice <= 0) return Integer.valueOf(b.attackerDaimyoId);

        if (b.day >= MAX_DAYS) return Integer.valueOf(b.defenderDaimyoId); // siege repelled
        return null;
    }

    /** Apply troop losses and, if the attacker won, transfer the province. */
    public void applyBattleOutcome(GameState s, BattleState b) {
        Province from = s.province(b.attackerProvId);
        Province to = s.province(b.defenderProvId);
        int atkSurv = livingTroops(b, true);
        int defSurv = livingTroops(b, false);
        boolean attackerWon = b.winnerDaimyoId != null
                && b.winnerDaimyoId.intValue() == b.attackerDaimyoId;

        if (attackerWon) {
            // Occupy: survivors garrison the conquered fief; ownership transfers.
            transferProvince(s, to, b.attackerDaimyoId);
            to.setSoldiers(atkSurv);
            to.setRice(Math.max(to.getRice(), b.attackerRice));
            to.setLoyalty(Math.max(30, to.getLoyalty() - 20)); // newly conquered, restless
        } else {
            // Defender holds; attacker's survivors retreat home.
            from.setSoldiers(from.getSoldiers() + atkSurv);
            from.setRice(from.getRice() + b.attackerRice);
            to.setSoldiers(defSurv);
            to.setRice(b.defenderRice);
        }
    }

    /** Reassign a province to {@code newOwner}, fixing both daimyo province lists. */
    private void transferProvince(GameState s, Province p, int newOwner) {
        int old = p.getOwnerId();
        if (old >= 0) {
            s.daimyo(old).getProvinceIds().remove(Integer.valueOf(p.getId()));
        }
        p.setOwnerId(newOwner);
        if (newOwner >= 0) {
            List<Integer> ids = s.daimyo(newOwner).getProvinceIds();
            if (!ids.contains(Integer.valueOf(p.getId()))) {
                ids.add(Integer.valueOf(p.getId()));
            }
        }
    }

    // ---- helpers ----------------------------------------------------------

    private int supplyBurn(int troops) {
        return (int) Math.ceil(troops / 100.0); // ~1 koku per 100 men per day
    }

    private int livingTroops(BattleState b, boolean attacker) {
        int t = 0;
        for (BattleUnit u : b.units) {
            if (u.isAlive() && u.isAttacker() == attacker) t += u.getTroops();
        }
        return t;
    }

    private boolean commanderAlive(BattleState b, boolean attacker) {
        for (BattleUnit u : b.units) {
            if (u.isCommander() && u.isAttacker() == attacker && u.isAlive()) return true;
        }
        return false;
    }

    private boolean adjacent(BattleUnit a, BattleUnit d) {
        return Math.abs(a.getCol() - d.getCol()) + Math.abs(a.getRow() - d.getRow()) == 1;
    }

    private int[] nearestEnemyTile(BattleState b, BattleUnit u) {
        BattleUnit best = null;
        int bestD = Integer.MAX_VALUE;
        for (BattleUnit e : b.units) {
            if (!e.isAlive() || e.isAttacker() == u.isAttacker()) continue;
            int d = Math.abs(e.getCol() - u.getCol()) + Math.abs(e.getRow() - u.getRow());
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best == null ? null : new int[]{best.getCol(), best.getRow()};
    }

    /** Move one tile toward (tc,tr); won't step onto an occupied tile. */
    private void stepToward(BattleState b, BattleUnit u, int tc, int tr) {
        int dc = Integer.compare(tc, u.getCol());
        int dr = Integer.compare(tr, u.getRow());
        // Already adjacent to target tile → stay put and let melee resolve.
        if (Math.abs(tc - u.getCol()) + Math.abs(tr - u.getRow()) <= 1) {
            return;
        }
        // Prefer the larger axis first, then fall back to the other.
        if (Math.abs(tc - u.getCol()) >= Math.abs(tr - u.getRow())) {
            if (tryMove(b, u, u.getCol() + dc, u.getRow())) return;
            tryMove(b, u, u.getCol(), u.getRow() + dr);
        } else {
            if (tryMove(b, u, u.getCol(), u.getRow() + dr)) return;
            tryMove(b, u, u.getCol() + dc, u.getRow());
        }
    }

    private boolean tryMove(BattleState b, BattleUnit u, int c, int r) {
        if (!b.inBounds(c, r) || (c == u.getCol() && r == u.getRow())) return false;
        if (b.unitAt(c, r) != null) return false;
        u.setCol(c);
        u.setRow(r);
        return true;
    }

    private Color colorOf(GameState s, int daimyoId, Color fallback) {
        if (daimyoId < 0) return fallback;
        Daimyo d = s.daimyo(daimyoId);
        return d == null ? fallback : d.getColor();
    }

    private String abbrevOf(GameState s, int daimyoId, String fallback) {
        if (daimyoId < 0) return fallback;
        Daimyo d = s.daimyo(daimyoId);
        return d == null ? fallback : d.getAbbrev();
    }
}
