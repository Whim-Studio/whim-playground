package com.whim.kenshi.domain;

import com.whim.kenshi.api.Config;
import com.whim.kenshi.api.Enums.AiState;
import com.whim.kenshi.api.Enums.FactionId;
import com.whim.kenshi.api.Enums.MoveState;
import com.whim.kenshi.api.Enums.OrderType;
import com.whim.kenshi.api.Enums.WeaponClass;

/**
 * A single agent in the world: identity, position/heading, weapon, anatomy,
 * skills, survival state (hunger/blood/bleed), and its current order + AI state.
 *
 * <p>This class is pure mutable state plus <b>derivations that a
 * {@link com.whim.kenshi.api.Views.CharacterView} needs</b> — {@link #moveState()}
 * and {@link #effectiveWeapon()}. All simulation behaviour (movement integration,
 * combat, AI decisions, survival ticking) belongs to the engine, which reads and
 * writes these fields directly.
 */
public final class Character {

    // ---- identity -------------------------------------------------------
    private final String id;
    private String name;
    private FactionId faction;

    // ---- kinematics (world units; (x,y) == CENTER) ----------------------
    private double x;
    private double y;
    private double heading; // radians, 0 = +x, CCW

    // ---- loadout & body -------------------------------------------------
    private WeaponClass weapon = WeaponClass.UNARMED;
    private final Anatomy anatomy = new Anatomy();
    private final Skills skills = new Skills();

    // ---- survival -------------------------------------------------------
    private double hunger = Config.HUNGER_MAX;
    private double blood = Config.BLOOD_MAX;
    private double bleedRate = 0.0;

    // ---- current order --------------------------------------------------
    private OrderType orderType = OrderType.NONE;
    private double targetX;
    private double targetY;
    private String targetId;   // ATTACK target character id, or null
    private String nodeId;     // INTERACT node id, or null

    // ---- AI / selection -------------------------------------------------
    private AiState aiState = AiState.IDLE;
    private boolean selected;

    public Character(String id, String name, FactionId faction, double x, double y) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.x = x;
        this.y = y;
    }

    // ---- identity accessors --------------------------------------------
    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public FactionId faction() { return faction; }
    public void setFaction(FactionId faction) { this.faction = faction; }

    /** True for the user-controlled squad. */
    public boolean playerControlled() { return faction == FactionId.PLAYER; }

    // ---- kinematics -----------------------------------------------------
    public double x() { return x; }
    public double y() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setPosition(double x, double y) { this.x = x; this.y = y; }
    public double heading() { return heading; }
    public void setHeading(double heading) { this.heading = heading; }

    // ---- loadout & body -------------------------------------------------
    public WeaponClass weapon() { return weapon; }
    public void setWeapon(WeaponClass weapon) { this.weapon = weapon; }
    public Anatomy anatomy() { return anatomy; }
    public Skills skills() { return skills; }

    // ---- survival -------------------------------------------------------
    public double hunger() { return hunger; }
    public void setHunger(double hunger) { this.hunger = clamp(hunger, 0.0, Config.HUNGER_MAX); }
    public double blood() { return blood; }
    public void setBlood(double blood) { this.blood = clamp(blood, 0.0, Config.BLOOD_MAX); }
    public double bleedRate() { return bleedRate; }
    public void setBleedRate(double bleedRate) { this.bleedRate = Math.max(0.0, bleedRate); }
    public void addBleedRate(double delta) { setBleedRate(this.bleedRate + delta); }

    /** True when blood has fallen below the pass-out threshold. */
    public boolean bloodLow() { return blood < Config.BLOOD_UNCONSCIOUS_AT; }

    // ---- order ----------------------------------------------------------
    public OrderType orderType() { return orderType; }
    public double targetX() { return targetX; }
    public double targetY() { return targetY; }
    public String targetId() { return targetId; }
    public String nodeId() { return nodeId; }

    /** Clear any current order. */
    public void clearOrder() {
        orderType = OrderType.NONE;
        targetId = null;
        nodeId = null;
    }

    /** Issue a MOVE order to a world position. */
    public void orderMove(double wx, double wy) {
        orderType = OrderType.MOVE;
        targetX = wx;
        targetY = wy;
        targetId = null;
        nodeId = null;
    }

    /** Issue an ATTACK order against a target character id. */
    public void orderAttack(String targetId) {
        orderType = OrderType.ATTACK;
        this.targetId = targetId;
        nodeId = null;
    }

    /** Issue an INTERACT order against a world-node id. */
    public void orderInteract(String nodeId) {
        orderType = OrderType.INTERACT;
        this.nodeId = nodeId;
        targetId = null;
    }

    /** Set only the current combat/attack target id (used by auto-attack). */
    public void setTargetId(String targetId) { this.targetId = targetId; }

    // ---- AI / selection -------------------------------------------------
    public AiState aiState() { return aiState; }
    public void setAiState(AiState aiState) { this.aiState = aiState; }
    public boolean selected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    // ---- derivations the Views need ------------------------------------
    public boolean isDead() { return anatomy.isDead(); }
    public boolean isDowned() { return anatomy.isDowned(bloodLow()); }
    public boolean alive() { return !isDead(); }

    /**
     * The weapon actually usable right now: a two-handed weapon degrades to
     * effective {@code UNARMED} when an arm is disabled.
     */
    public WeaponClass effectiveWeapon() {
        if (weapon == WeaponClass.TWO_HANDED && anatomy.anyArmDown()) {
            return WeaponClass.UNARMED;
        }
        return weapon;
    }

    /**
     * Coarse physical state derived from anatomy + blood:
     * DEAD &gt; DOWNED &gt; CRAWLING &gt; MOVING &gt; IDLE. Movement (MOVING) is
     * inferred from having a live MOVE order; the engine may override to MOVING
     * whenever it is actively integrating a path.
     */
    public MoveState moveState() {
        if (isDead()) return MoveState.DEAD;
        if (isDowned()) return MoveState.DOWNED;
        if (anatomy.bothLegsDown()) return MoveState.CRAWLING;
        if (orderType == OrderType.MOVE) return MoveState.MOVING;
        return MoveState.IDLE;
    }

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
