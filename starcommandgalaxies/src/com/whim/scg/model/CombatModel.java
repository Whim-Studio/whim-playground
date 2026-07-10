package com.whim.scg.model;

import com.whim.scg.api.Views;

import java.util.ArrayList;
import java.util.List;

/** Mutable 1-v-1 space combat state. */
public final class CombatModel implements Views.CombatView {
    public ShipModel playerShip;   // reference to the game's player ship
    public ShipModel enemyShip;
    public final List<ProjectileModel> projectiles = new ArrayList<ProjectileModel>();
    public boolean over;
    public boolean playerWon;
    public int salvage;            // credits awarded on victory
    public boolean boarded;        // a boarding action is in progress from this fight

    /** Enemy AI accumulators. */
    public double enemyFireCooldown;
    public double enemyRetargetTimer;

    @Override public ShipModel player() { return playerShip; }
    @Override public ShipModel enemy() { return enemyShip; }

    @Override public List<Views.ProjectileView> projectiles() {
        List<Views.ProjectileView> out = new ArrayList<Views.ProjectileView>(projectiles);
        return out;
    }

    @Override public boolean canBoard() {
        return !over && enemyShip != null && enemyShip.shields <= 0 && enemyShip.hull > 0
                && playerHasBoardParty();
    }

    private boolean playerHasBoardParty() {
        if (playerShip == null) return false;
        RoomModel tele = playerShip.firstRoom(com.whim.scg.api.Enums.RoomType.TELEPORTER);
        if (tele == null || tele.hp <= 0) return false;
        for (CrewModel c : playerShip.crew) if (c.alive()) return true;
        return false;
    }

    @Override public boolean over() { return over; }
    @Override public boolean playerWon() { return playerWon; }
}
