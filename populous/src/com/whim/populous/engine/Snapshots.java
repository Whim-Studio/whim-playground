package com.whim.populous.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.GodPower;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;
import com.whim.populous.api.Views.FollowerView;
import com.whim.populous.api.Views.GameStateView;
import com.whim.populous.api.Views.MapView;
import com.whim.populous.api.Views.PapalMagnetView;
import com.whim.populous.api.Views.TileView;

/**
 * Immutable, snapshot-consistent projections built by {@link SimulationEngine}
 * at the end of each tick (and after each player action) while holding the
 * engine lock. The UI reads these on the EDT with zero risk of tearing or
 * {@link java.util.ConcurrentModificationException}, because every field is a
 * defensive copy of the live domain taken under the lock.
 *
 * These classes depend ONLY on the {@code api} package (never on domain
 * concretes), so the snapshot layer is completely decoupled from Task 1.
 */
final class Snapshots {

    private Snapshots() { }

    /** Copy of one terrain cell. */
    static final class SnapTile implements TileView {
        private final int col;
        private final int row;
        private final int elevation;
        private final TerrainType terrain;
        private final Allegiance owner;
        private final SettlementType settlement;
        private final int settlementLevel;

        SnapTile(TileView t) {
            this.col = t.col();
            this.row = t.row();
            this.elevation = t.elevation();
            this.terrain = t.terrain();
            this.owner = t.owner();
            this.settlement = t.settlement();
            this.settlementLevel = t.settlementLevel();
        }

        public int col() { return col; }
        public int row() { return row; }
        public int elevation() { return elevation; }
        public TerrainType terrain() { return terrain; }
        public Allegiance owner() { return owner; }
        public SettlementType settlement() { return settlement; }
        public int settlementLevel() { return settlementLevel; }
    }

    /**
     * Copy of the whole landscape. Terrain/elevation/ownership are copied into
     * flat primitive arrays under the engine lock; {@link #tileAt} constructs a
     * lightweight flyweight on demand so we do not allocate 4096 tile objects
     * per tick — only when the renderer actually asks for a cell.
     */
    static final class SnapMap implements MapView {
        private final int cols;
        private final int rows;
        private final int seaLevel;
        private final int[] elev;
        private final TerrainType[] terr;
        private final Allegiance[] owner;
        private final SettlementType[] settle;
        private final int[] level;

        SnapMap(MapView src) {
            this.cols = src.cols();
            this.rows = src.rows();
            this.seaLevel = src.seaLevel();
            int n = cols * rows;
            this.elev = new int[n];
            this.terr = new TerrainType[n];
            this.owner = new Allegiance[n];
            this.settle = new SettlementType[n];
            this.level = new int[n];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int i = r * cols + c;
                    TileView t = src.tileAt(c, r);
                    elev[i] = t.elevation();
                    terr[i] = t.terrain();
                    owner[i] = t.owner();
                    settle[i] = t.settlement();
                    level[i] = t.settlementLevel();
                }
            }
        }

        public int cols() { return cols; }
        public int rows() { return rows; }
        public int seaLevel() { return seaLevel; }

        public TileView tileAt(final int col, final int row) {
            final int c = clamp(col, cols);
            final int r = clamp(row, rows);
            final int i = r * cols + c;
            return new TileView() {
                public int col() { return c; }
                public int row() { return r; }
                public int elevation() { return elev[i]; }
                public TerrainType terrain() { return terr[i]; }
                public Allegiance owner() { return owner[i]; }
                public SettlementType settlement() { return settle[i]; }
                public int settlementLevel() { return level[i]; }
            };
        }

        private static int clamp(int v, int max) {
            if (v < 0) return 0;
            if (v >= max) return max - 1;
            return v;
        }
    }

    /** Copy of a single walker. */
    static final class SnapFollower implements FollowerView {
        private final double x;
        private final double y;
        private final Allegiance allegiance;
        private final int health;
        private final int stamina;
        private final boolean alive;

        SnapFollower(FollowerView f) {
            this.x = f.x();
            this.y = f.y();
            this.allegiance = f.allegiance();
            this.health = f.health();
            this.stamina = f.stamina();
            this.alive = f.alive();
        }

        public double x() { return x; }
        public double y() { return y; }
        public Allegiance allegiance() { return allegiance; }
        public int health() { return health; }
        public int stamina() { return stamina; }
        public boolean alive() { return alive; }
    }

    /** Copy of a papal magnet. */
    static final class SnapMagnet implements PapalMagnetView {
        private final boolean active;
        private final int col;
        private final int row;
        private final Allegiance side;

        SnapMagnet(PapalMagnetView m) {
            this.active = m.active();
            this.col = m.col();
            this.row = m.row();
            this.side = m.side();
        }

        public boolean active() { return active; }
        public int col() { return col; }
        public int row() { return row; }
        public Allegiance side() { return side; }
    }

    /** Full immutable frame handed to the UI. */
    static final class SnapState implements GameStateView {
        private final MapView map;
        private final List<FollowerView> followers;
        private final int goodMana;
        private final int evilMana;
        private final int maxMana;
        private final int goodPopulation;
        private final int evilPopulation;
        private final int populationCap;
        private final GodPower selectedPower;
        private final boolean[] affordable;
        private final PapalMagnetView goodMagnet;
        private final PapalMagnetView evilMagnet;
        private final boolean gameOver;
        private final Allegiance winner;
        private final long tick;
        private final String statusLine;

        SnapState(GameStateView live) {
            this.map = new SnapMap(live.map());
            List<FollowerView> copy = new ArrayList<FollowerView>();
            List<FollowerView> src = live.followers();
            for (int i = 0; i < src.size(); i++) {
                copy.add(new SnapFollower(src.get(i)));
            }
            this.followers = Collections.unmodifiableList(copy);
            this.goodMana = live.goodMana();
            this.evilMana = live.evilMana();
            this.maxMana = live.maxMana();
            this.goodPopulation = live.goodPopulation();
            this.evilPopulation = live.evilPopulation();
            this.populationCap = live.populationCap();
            this.selectedPower = live.selectedPower();
            GodPower[] powers = GodPower.values();
            this.affordable = new boolean[powers.length];
            for (int i = 0; i < powers.length; i++) {
                this.affordable[i] = live.powerAffordable(powers[i]);
            }
            this.goodMagnet = new SnapMagnet(live.goodMagnet());
            this.evilMagnet = new SnapMagnet(live.evilMagnet());
            this.gameOver = live.gameOver();
            this.winner = live.winner();
            this.tick = live.tick();
            this.statusLine = live.statusLine();
        }

        public MapView map() { return map; }
        public List<FollowerView> followers() { return followers; }
        public int goodMana() { return goodMana; }
        public int evilMana() { return evilMana; }
        public int maxMana() { return maxMana; }
        public int goodPopulation() { return goodPopulation; }
        public int evilPopulation() { return evilPopulation; }
        public int populationCap() { return populationCap; }
        public GodPower selectedPower() { return selectedPower; }
        public boolean powerAffordable(GodPower p) { return affordable[p.ordinal()]; }
        public PapalMagnetView goodMagnet() { return goodMagnet; }
        public PapalMagnetView evilMagnet() { return evilMagnet; }
        public boolean gameOver() { return gameOver; }
        public Allegiance winner() { return winner; }
        public long tick() { return tick; }
        public String statusLine() { return statusLine; }
    }
}
