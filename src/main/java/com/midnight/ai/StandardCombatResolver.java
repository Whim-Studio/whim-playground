package com.midnight.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.midnight.core.Character;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Side;
import com.midnight.core.Stronghold;
import com.midnight.core.Terrain;

/**
 * The standard, seedable combat resolver.
 *
 * <p>A battle pits every alive character of each side present at a location
 * (plus the local stronghold garrison) against the other. Each side's raw
 * strength is the sum, over its lords, of
 *
 * <pre>
 *     (warriors + riders * RIDER_FACTOR) * courageMul * energyMul
 * </pre>
 *
 * where {@code courageMul} and {@code energyMul} each scale in {@code [0.5, 1.5]}
 * with the lord's courage and remaining energy (a fatigued or craven lord fights
 * at roughly a third the punch of a fresh, valiant one). The garrison adds raw
 * troops at full vigour.
 *
 * <p>The <em>defender</em> &mdash; the stronghold's owner if one stands here,
 * otherwise FREE (Doomdark is the aggressor at night) &mdash; multiplies its
 * total strength by a {@linkplain #terrainBonus(Terrain) terrain defensive
 * bonus}: citadels, keeps, towers, mountains and forest are strong; open plains
 * are weak. A small seeded random fluctuation (&plusmn;15%) is applied to each
 * side so equal armies do not always tie, while keeping the resolver fully
 * deterministic for a given seed.
 *
 * <p>The stronger side wins (a near-tie is reported as indecisive). The loser
 * sheds the larger fraction of its troops; lords on the losing side who are
 * stripped of every soldier in a decisive defeat are slain. If an attacker takes
 * a defended stronghold, its owner flips and the survivors become the new
 * garrison.
 */
public class StandardCombatResolver implements CombatResolver {

    /** Mounted soldiers count for more than foot in open battle. */
    private static final double RIDER_FACTOR = 1.5;
    /** A clash inside this relative margin is reported indecisive. */
    private static final double INDECISIVE_MARGIN = 0.05;
    /** Default seed so the no-arg resolver is reproducible. */
    private static final long DEFAULT_SEED = 0x10D50F1DL;

    private final Random rng;

    public StandardCombatResolver() {
        this(DEFAULT_SEED);
    }

    public StandardCombatResolver(long seed) {
        this.rng = new Random(seed);
    }

    @Override
    public BattleResult resolveBattle(GameState state, Location where) {
        List<Character> free = new ArrayList<Character>();
        List<Character> doom = new ArrayList<Character>();
        for (Character c : state.charactersAt(where)) {
            if (c == null || !c.isAlive()) {
                continue;
            }
            if (c.side() == Side.FREE) {
                free.add(c);
            } else {
                doom.add(c);
            }
        }

        Stronghold sh = state.map().strongholdAt(where);
        Terrain terrain = state.map().terrainAt(where);

        // Defender holds the stronghold; absent one, FREE defends (Doomdark attacks at night).
        Side defender = (sh != null) ? sh.owner() : Side.FREE;
        Side attacker = defender.opponent();

        double freeStr = sideStrength(free);
        double doomStr = sideStrength(doom);
        if (sh != null) {
            if (sh.owner() == Side.FREE) {
                freeStr += sh.garrison();
            } else {
                doomStr += sh.garrison();
            }
        }

        double bonus = terrainBonus(terrain);
        if (defender == Side.FREE) {
            freeStr *= bonus;
        } else {
            doomStr *= bonus;
        }

        freeStr *= fluctuation();
        doomStr *= fluctuation();

        Side victor;
        double max = Math.max(freeStr, doomStr);
        if (max <= 0.0) {
            victor = null;
        } else if (Math.abs(freeStr - doomStr) < INDECISIVE_MARGIN * max) {
            victor = null;
        } else {
            victor = (freeStr > doomStr) ? Side.FREE : Side.DOOMDARK;
        }

        double total = freeStr + doomStr;
        double freeFrac;
        double doomFrac;
        if (victor == Side.FREE) {
            doomFrac = clamp(0.40 + 0.50 * (freeStr / total), 0.0, 0.95);
            freeFrac = clamp(0.30 * (doomStr / total), 0.0, 0.60);
        } else if (victor == Side.DOOMDARK) {
            freeFrac = clamp(0.40 + 0.50 * (doomStr / total), 0.0, 0.95);
            doomFrac = clamp(0.30 * (freeStr / total), 0.0, 0.60);
        } else {
            // Indecisive: both bloodied, neither broken.
            freeFrac = total > 0 ? clamp(0.30 * (doomStr / total), 0.0, 0.45) : 0.0;
            doomFrac = total > 0 ? clamp(0.30 * (freeStr / total), 0.0, 0.45) : 0.0;
        }

        int freeLosses = applyLosses(free, freeFrac, victor == Side.DOOMDARK);
        int doomLosses = applyLosses(doom, doomFrac, victor == Side.FREE);

        if (sh != null) {
            double ownerFrac = (sh.owner() == Side.FREE) ? freeFrac : doomFrac;
            int g = sh.garrison();
            int ng = (int) Math.round(g * (1.0 - ownerFrac));
            if (ng < 0) {
                ng = 0;
            }
            if (sh.owner() == Side.FREE) {
                freeLosses += (g - ng);
            } else {
                doomLosses += (g - ng);
            }
            sh.setGarrison(ng);
        }

        String capture = "";
        if (sh != null && victor != null && victor != sh.owner()) {
            sh.setOwner(victor);
            int newGarrison = survivingTroops((victor == Side.FREE) ? free : doom);
            sh.setGarrison(newGarrison);
            capture = " " + sideName(victor) + " seizes " + sh.name() + "!";
        }

        String text = describe(where, terrain, sh, victor, freeLosses, doomLosses) + capture;
        return new BattleResult(where, victor, freeLosses, doomLosses, text);
    }

    /** Sum of fighting strength for one side's lords. */
    private double sideStrength(List<Character> side) {
        double sum = 0.0;
        for (Character c : side) {
            double troops = c.warriors() + c.riders() * RIDER_FACTOR;
            sum += troops * courageMul(c) * energyMul(c);
        }
        return sum;
    }

    /** Courage in [0,127] maps to a [0.5, 1.5] multiplier. */
    private static double courageMul(Character c) {
        return 0.5 + clamp(c.courage(), 0, 127) / 127.0;
    }

    /** Energy/stamina in [0,127] maps to a [0.5, 1.5] multiplier (fatigue). */
    private static double energyMul(Character c) {
        return 0.5 + clamp(c.energy(), 0, 127) / 127.0;
    }

    /**
     * Defensive multiplier applied to the defender's strength. Strongholds and
     * rough country shelter defenders; open plains do not.
     */
    public static double terrainBonus(Terrain t) {
        if (t == null) {
            return 1.0;
        }
        switch (t) {
            case CITADEL:
                return 2.0;
            case KEEP:
                return 1.8;
            case TOWER:
                return 1.6;
            case MOUNTAINS:
                return 1.7;
            case FOREST:
                return 1.5;
            case DOWNS:
                return 1.2;
            case SNOW:
            case WASTELAND:
            case RUINS:
                return 1.1;
            case VILLAGE:
            case HENGE:
            case LAKE:
                return 1.05;
            case PLAINS:
            default:
                return 1.0;
        }
    }

    /** Seeded fluctuation in roughly [0.85, 1.15]. */
    private double fluctuation() {
        return 0.85 + rng.nextDouble() * 0.30;
    }

    /**
     * Apply a casualty fraction to each lord, returning total soldiers lost.
     * On a decisive defeat, a lord stripped of his whole army is slain.
     */
    private static int applyLosses(List<Character> side, double frac, boolean losingSide) {
        int lost = 0;
        for (Character c : side) {
            int w = c.warriors();
            int r = c.riders();
            int nw = (int) Math.round(w * (1.0 - frac));
            int nr = (int) Math.round(r * (1.0 - frac));
            if (nw < 0) {
                nw = 0;
            }
            if (nr < 0) {
                nr = 0;
            }
            lost += (w - nw) + (r - nr);
            c.setWarriors(nw);
            c.setRiders(nr);
            if (losingSide && nw == 0 && nr == 0) {
                c.kill();
            }
        }
        return lost;
    }

    private static int survivingTroops(List<Character> side) {
        int sum = 0;
        for (Character c : side) {
            if (c.isAlive()) {
                sum += c.warriors() + c.riders();
            }
        }
        return sum;
    }

    private static String describe(Location where, Terrain terrain, Stronghold sh,
                                   Side victor, int freeLosses, int doomLosses) {
        StringBuilder sb = new StringBuilder();
        String place = (sh != null) ? sh.name() : ("the " + terrainName(terrain) + " at " + where);
        sb.append("Battle at ").append(place).append(": ");
        if (victor == Side.FREE) {
            sb.append("the Free hold the field");
        } else if (victor == Side.DOOMDARK) {
            sb.append("Doomdark's armies prevail");
        } else {
            sb.append("the clash is indecisive");
        }
        sb.append(" (Free lose ").append(freeLosses)
          .append(", Doomdark lose ").append(doomLosses).append(").");
        return sb.toString();
    }

    private static String terrainName(Terrain t) {
        if (t == null) {
            return "plains";
        }
        return t.name().toLowerCase();
    }

    private static String sideName(Side s) {
        return s == Side.FREE ? "The Free" : "Doomdark";
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
