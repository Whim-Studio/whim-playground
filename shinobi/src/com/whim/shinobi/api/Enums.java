package com.whim.shinobi.api;

/** Shared enums. DO NOT modify — all three tasks reference these constants. */
public final class Enums {
    private Enums() {}

    /** The two side-scrolling paths the player can leap between. */
    public enum Plane { LOWER, UPPER }

    public enum Facing { LEFT, RIGHT }

    /**
     * Player weapon progression (hostage upgrades walk SHURIKEN -> KNIFE -> GUN).
     * Each carries the projectile speed and cooldown in ticks used by the engine.
     */
    public enum Weapon {
        SHURIKEN(6.5, 14, 1),
        KNIFE(8.0, 11, 1),
        GUN(11.0, 8, 2);

        private final double projectileSpeed;
        private final int cooldownTicks;
        private final int damage;

        Weapon(double projectileSpeed, int cooldownTicks, int damage) {
            this.projectileSpeed = projectileSpeed;
            this.cooldownTicks = cooldownTicks;
            this.damage = damage;
        }
        public double projectileSpeed() { return projectileSpeed; }
        public int cooldownTicks() { return cooldownTicks; }
        public int damage() { return damage; }
        /** Next weapon in the upgrade chain (GUN is terminal). */
        public Weapon upgrade() {
            switch (this) {
                case SHURIKEN: return KNIFE;
                case KNIFE: return GUN;
                default: return GUN;
            }
        }
    }

    /** Enemy archetypes. THUG patrols/shoots; NINJA melees and can block shuriken. */
    public enum EnemyType { THUG, NINJA }

    /** Whether a context-sensitive attack resolved to a thrown weapon or a swing. */
    public enum AttackMode { PROJECTILE, MELEE }

    /** Coarse animation/behavior state for rendering + AI. */
    public enum EntityState { IDLE, WALK, JUMP, ATTACK, BLOCK, DEAD }

    /** Bonus granted by rescuing a hostage. */
    public enum RescueReward { POINTS, WEAPON_UPGRADE, EXTRA_NINJUTSU }

    /** Overall game phase; UI uses this to switch what it draws. */
    public enum Phase { PLAYING, NINJUTSU, LEVEL_CLEAR, GAME_OVER, PAUSED }
}
