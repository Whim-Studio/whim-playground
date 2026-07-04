// TODO integrate domain — PLACEHOLDER (Task 2 engine stub; Task 1 replaces this file). See PLACEHOLDER_README.md.
package com.whim.shinobi.domain;

import com.whim.shinobi.api.Enums;
import com.whim.shinobi.api.Views;

/** A hostile entity (THUG or NINJA) with self-contained AI bookkeeping. */
public class Enemy extends Entity implements Views.EnemyView {
    public Enums.EnemyType type = Enums.EnemyType.THUG;
    public boolean blocking = false;
    public int hp = 1;

    /** Patrol bounds (world x of the box left edge). */
    public double patrolMinX, patrolMaxX;
    /** Generic countdown reused by the AI state machines (shoot/act cadence). */
    public int actTimer = 0;
    /** Ninja block windows. */
    public int blockTimer = 0;
    /** True once the enemy has noticed the player (leaves pure patrol). */
    public boolean aggro = false;

    public Enemy(Enums.EnemyType type) {
        this.type = type;
        this.hp = (type == Enums.EnemyType.NINJA) ? 2 : 1;
    }

    @Override public Enums.EnemyType type() { return type; }
    @Override public boolean blocking() { return blocking; }
}
