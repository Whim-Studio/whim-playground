package com.whim.xcom.rules.model;

/**
 * Immutable snapshot of everything the {@link AccuracyModel} needs about a shot,
 * decoupled from the (not-yet-built) battlescape entities so the pure rules layer
 * stays independent of the eventual unit classes.
 */
public final class ShotContext {

    private final int firingAccuracy;   // shooter's Firing Accuracy stat (0..~120)
    private final boolean kneeling;
    private final boolean weaponTwoHanded;
    private final boolean usingBothHands; // false => one-handed a two-handed weapon
    private final int fatalWoundsToFiringArm;
    private final int distanceTiles;
    private final boolean targetSmoke;    // target obscured by smoke

    public ShotContext(int firingAccuracy,
                       boolean kneeling,
                       boolean weaponTwoHanded,
                       boolean usingBothHands,
                       int fatalWoundsToFiringArm,
                       int distanceTiles,
                       boolean targetSmoke) {
        this.firingAccuracy = firingAccuracy;
        this.kneeling = kneeling;
        this.weaponTwoHanded = weaponTwoHanded;
        this.usingBothHands = usingBothHands;
        this.fatalWoundsToFiringArm = fatalWoundsToFiringArm;
        this.distanceTiles = distanceTiles;
        this.targetSmoke = targetSmoke;
    }

    /** Convenience: a standing shooter, weapon held correctly, no wounds, point-blank, no smoke. */
    public static ShotContext basic(int firingAccuracy) {
        return new ShotContext(firingAccuracy, false, true, true, 0, 1, false);
    }

    public int firingAccuracy() {
        return firingAccuracy;
    }

    public boolean kneeling() {
        return kneeling;
    }

    public boolean weaponTwoHanded() {
        return weaponTwoHanded;
    }

    public boolean usingBothHands() {
        return usingBothHands;
    }

    public int fatalWoundsToFiringArm() {
        return fatalWoundsToFiringArm;
    }

    public int distanceTiles() {
        return distanceTiles;
    }

    public boolean targetSmoke() {
        return targetSmoke;
    }
}
