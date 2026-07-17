package com.whim.xcom.rules.data;

import java.util.HashMap;
import java.util.Map;

import com.whim.xcom.model.DamageType;
import com.whim.xcom.rules.def.ArmorDef;

/** Immutable {@link ArmorDef} backed by data-pack fields. */
public final class DataArmorDef implements ArmorDef {

    private String id;
    private String name;
    private int front;
    private int side;
    private int rear;
    private int under;
    private Map<DamageType, Double> resistances = new HashMap<DamageType, Double>();

    public DataArmorDef(String id, String name, int front, int side, int rear, int under,
                        Map<DamageType, Double> resistances) {
        this.id = id;
        this.name = name;
        this.front = front;
        this.side = side;
        this.rear = rear;
        this.under = under;
        if (resistances != null) {
            this.resistances = resistances;
        }
    }

    DataArmorDef() {
    }

    @Override public String id() { return id; }
    @Override public String name() { return name; }
    @Override public int front() { return front; }
    @Override public int side() { return side; }
    @Override public int rear() { return rear; }
    @Override public int under() { return under; }
    @Override public int defaultArmor() { return front; }

    @Override
    public double resistance(DamageType type) {
        Double r = resistances == null ? null : resistances.get(type);
        return r == null ? 1.0 : r;
    }
}
