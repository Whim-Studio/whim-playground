package com.tiwa.mahjong.scoring;

import com.tiwa.mahjong.api.Meld;
import com.tiwa.mahjong.api.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a single winning hand, plus the table configuration needed to score it.
 *
 * <p>This is the input value-type for {@link ScoreCalculator}. It is built only from the shared
 * {@code com.tiwa.mahjong.api} interfaces, so the demo orchestrator (and Task 2's rules engine) can
 * populate it from whatever concrete {@code model} classes Task 1 ships.</p>
 *
 * <p>{@link #getMelds()} holds the four scoring sets (PUNG / KONG / CHOW). The pair is supplied
 * separately via {@link #getPair()} and contributes 0 base points. Bonus tiles (Flowers / Seasons)
 * are listed in {@link #getBonusTiles()} and score 4 points each.</p>
 *
 * <p>Most doubles are derived from the melds; the win-condition doubles (Heavenly / Earthly / Human
 * hands and the timing of the winning tile) cannot be inferred from tiles and are passed as flags.</p>
 */
public final class WinContext {

    private final List<Meld> melds;
    private final Meld pair;
    private final List<Tile> bonusTiles;

    private final boolean dealer;
    private final boolean heavenlyHand;
    private final boolean earthlyHand;
    private final boolean humanHand;
    private final boolean mahjongOnLastDrawnTile;
    private final boolean mahjongOnFinalDiscard;

    private final SpecialHand specialHand;

    private final int pointsLimit;
    private final boolean unlimitedPoints;
    private final long moneyLimit;
    private final boolean unlimitedMoney;

    private WinContext(Builder b) {
        this.melds = Collections.unmodifiableList(new ArrayList<Meld>(b.melds));
        this.pair = b.pair;
        this.bonusTiles = Collections.unmodifiableList(new ArrayList<Tile>(b.bonusTiles));
        this.dealer = b.dealer;
        this.heavenlyHand = b.heavenlyHand;
        this.earthlyHand = b.earthlyHand;
        this.humanHand = b.humanHand;
        this.mahjongOnLastDrawnTile = b.mahjongOnLastDrawnTile;
        this.mahjongOnFinalDiscard = b.mahjongOnFinalDiscard;
        this.specialHand = b.specialHand;
        this.pointsLimit = b.pointsLimit;
        this.unlimitedPoints = b.unlimitedPoints;
        this.moneyLimit = b.moneyLimit;
        this.unlimitedMoney = b.unlimitedMoney;
    }

    public List<Meld> getMelds() {
        return melds;
    }

    /** The pair (eyes); may be {@code null} for special hands that have no pair. */
    public Meld getPair() {
        return pair;
    }

    public List<Tile> getBonusTiles() {
        return bonusTiles;
    }

    public boolean isDealer() {
        return dealer;
    }

    public boolean isHeavenlyHand() {
        return heavenlyHand;
    }

    public boolean isEarthlyHand() {
        return earthlyHand;
    }

    public boolean isHumanHand() {
        return humanHand;
    }

    public boolean isMahjongOnLastDrawnTile() {
        return mahjongOnLastDrawnTile;
    }

    public boolean isMahjongOnFinalDiscard() {
        return mahjongOnFinalDiscard;
    }

    public SpecialHand getSpecialHand() {
        return specialHand;
    }

    public int getPointsLimit() {
        return pointsLimit;
    }

    public boolean isUnlimitedPoints() {
        return unlimitedPoints;
    }

    public long getMoneyLimit() {
        return moneyLimit;
    }

    public boolean isUnlimitedMoney() {
        return unlimitedMoney;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder. Defaults: standard 1000-point limit, $1000 money limit, no special hand. */
    public static final class Builder {
        private List<Meld> melds = new ArrayList<Meld>();
        private Meld pair;
        private List<Tile> bonusTiles = new ArrayList<Tile>();

        private boolean dealer;
        private boolean heavenlyHand;
        private boolean earthlyHand;
        private boolean humanHand;
        private boolean mahjongOnLastDrawnTile;
        private boolean mahjongOnFinalDiscard;

        private SpecialHand specialHand = SpecialHand.NONE;

        private int pointsLimit = 1000;
        private boolean unlimitedPoints = false;
        private long moneyLimit = 1000L;
        private boolean unlimitedMoney = false;

        public Builder melds(List<Meld> value) {
            this.melds = new ArrayList<Meld>(value);
            return this;
        }

        public Builder addMeld(Meld value) {
            this.melds.add(value);
            return this;
        }

        public Builder pair(Meld value) {
            this.pair = value;
            return this;
        }

        public Builder bonusTiles(List<Tile> value) {
            this.bonusTiles = new ArrayList<Tile>(value);
            return this;
        }

        public Builder addBonusTile(Tile value) {
            this.bonusTiles.add(value);
            return this;
        }

        public Builder dealer(boolean value) {
            this.dealer = value;
            return this;
        }

        public Builder heavenlyHand(boolean value) {
            this.heavenlyHand = value;
            return this;
        }

        public Builder earthlyHand(boolean value) {
            this.earthlyHand = value;
            return this;
        }

        public Builder humanHand(boolean value) {
            this.humanHand = value;
            return this;
        }

        public Builder mahjongOnLastDrawnTile(boolean value) {
            this.mahjongOnLastDrawnTile = value;
            return this;
        }

        public Builder mahjongOnFinalDiscard(boolean value) {
            this.mahjongOnFinalDiscard = value;
            return this;
        }

        public Builder specialHand(SpecialHand value) {
            this.specialHand = value;
            return this;
        }

        public Builder pointsLimit(int value) {
            this.pointsLimit = value;
            return this;
        }

        public Builder unlimitedPoints(boolean value) {
            this.unlimitedPoints = value;
            return this;
        }

        public Builder moneyLimit(long value) {
            this.moneyLimit = value;
            return this;
        }

        public Builder unlimitedMoney(boolean value) {
            this.unlimitedMoney = value;
            return this;
        }

        public WinContext build() {
            return new WinContext(this);
        }
    }
}
