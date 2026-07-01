package com.whim.warroom.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static data-dictionary of generic, historically-flavored unit archetypes
 * across the three eras. Four archetypes per era (12 total): a line/heavy unit,
 * a shock unit, a ranged unit, and a siege/support unit, so every era fields a
 * comparable rock-paper-scissors mix.
 *
 * <h3>Stat table (health / attack / defense / speed / range / morale)</h3>
 * <pre>
 * ERA        ID                 HP   ATK  DEF  SPD  RNG  MOR   ROLE
 * ---------  -----------------  ---  ---  ---  ---  ---  ---   ----------------------------
 * ANTIQUITY  roman-legionary    120   14   10   34   22   80   durable disciplined line
 * ANTIQUITY  greek-hoplite      130   11   14   28   20   85   phalanx wall, best defense
 * ANTIQUITY  archer              70   10    3   36  150   55   cheap ranged skirmisher
 * ANTIQUITY  war-elephant       260   26    8   30   26   58   shock bruiser, brittle morale
 * MEDIEVAL   knight             170   22   13   60   24   82   fast heavy cavalry charge
 * MEDIEVAL   man-at-arms        130   15   12   32   22   78   reliable infantry anchor
 * MEDIEVAL   longbowman          75   13    4   34  190   58   strong massed ranged
 * MEDIEVAL   trebuchet           90   46    2    8  360   50   siege: huge dmg, immobile
 * MODERN     rifle-infantry     100   18    6   30  220   70   flexible ranged line
 * MODERN     mg-team             85   30    5   18  260   66   suppressive area fire
 * MODERN     main-battle-tank   400   40   22   70  300   90   armored spearhead
 * MODERN     artillery          110   60    4   12  520   55   long-range glass cannon
 * </pre>
 *
 * <h3>Balance rationale</h3>
 * Stats trade along four axes so no archetype dominates:
 * <ul>
 *   <li><b>Durability vs. reach.</b> Ranged units (archer, longbowman, mg-team,
 *       artillery) have long range but low HP/defense — they win at a distance
 *       and fold in melee. Line units invert that.</li>
 *   <li><b>Speed vs. mass.</b> Cavalry/tanks are fast but not the toughest per
 *       point; siege (trebuchet, artillery) is nearly immobile, forcing escorts.</li>
 *   <li><b>Morale as a soft counter.</b> Elephants and glass-cannon siege carry
 *       low morale, so shock and losses rout them before their raw stats matter,
 *       rewarding flanking over slugging.</li>
 *   <li><b>Cross-era escalation.</b> Absolute numbers grow modestly per era
 *       (a tank out-armors a legionary) but the internal ratios stay comparable,
 *       so mixed-era sandbox fights remain readable rather than one-sided.</li>
 * </ul>
 * Speeds are world-units/second (a tile is 32 units): infantry ~28-36 ≈ one tile/s,
 * cavalry/tanks ~60-70, siege 8-12. Ranges span melee (~20, under a tile) to
 * artillery (520, ~16 tiles) to keep positioning meaningful.
 */
public final class UnitCatalog {

    private static final List<UnitType> ALL;

    static {
        List<UnitType> list = new ArrayList<UnitType>();

        // ---- Antiquity ----
        list.add(new UnitType("roman-legionary", "Roman Legionary", Era.ANTIQUITY,
                120, 14, 10, 34, 22, 80));
        list.add(new UnitType("greek-hoplite", "Greek Hoplite", Era.ANTIQUITY,
                130, 11, 14, 28, 20, 85));
        list.add(new UnitType("archer", "Archer", Era.ANTIQUITY,
                70, 10, 3, 36, 150, 55));
        list.add(new UnitType("war-elephant", "War Elephant", Era.ANTIQUITY,
                260, 26, 8, 30, 26, 58));

        // ---- Medieval ----
        list.add(new UnitType("knight", "Knight", Era.MEDIEVAL,
                170, 22, 13, 60, 24, 82));
        list.add(new UnitType("man-at-arms", "Man-at-Arms", Era.MEDIEVAL,
                130, 15, 12, 32, 22, 78));
        list.add(new UnitType("longbowman", "Longbowman", Era.MEDIEVAL,
                75, 13, 4, 34, 190, 58));
        list.add(new UnitType("trebuchet", "Trebuchet", Era.MEDIEVAL,
                90, 46, 2, 8, 360, 50));

        // ---- Modern ----
        list.add(new UnitType("rifle-infantry", "Rifle Infantry", Era.MODERN,
                100, 18, 6, 30, 220, 70));
        list.add(new UnitType("mg-team", "Machine-gun Team", Era.MODERN,
                85, 30, 5, 18, 260, 66));
        list.add(new UnitType("main-battle-tank", "Main Battle Tank", Era.MODERN,
                400, 40, 22, 70, 300, 90));
        list.add(new UnitType("artillery", "Artillery", Era.MODERN,
                110, 60, 4, 12, 520, 55));

        ALL = Collections.unmodifiableList(list);
    }

    private UnitCatalog() {
    }

    /** Every archetype, in a stable order (antiquity → medieval → modern). */
    public static List<UnitType> all() {
        return ALL;
    }

    public static List<UnitType> byEra(Era era) {
        List<UnitType> out = new ArrayList<UnitType>();
        for (int i = 0; i < ALL.size(); i++) {
            UnitType t = ALL.get(i);
            if (t.getEra() == era) {
                out.add(t);
            }
        }
        return out;
    }

    /** Archetype with this id, or {@code null} if none matches. */
    public static UnitType byId(String id) {
        if (id == null) {
            return null;
        }
        for (int i = 0; i < ALL.size(); i++) {
            UnitType t = ALL.get(i);
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }
}
