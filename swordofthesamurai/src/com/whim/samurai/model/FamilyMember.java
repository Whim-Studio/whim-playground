package com.whim.samurai.model;

import java.io.Serializable;

/**
 * A member of the player's household: wife, sons and daughters. Sons are
 * eligible heirs; family members can be kidnapped by rivals, prompting a
 * rescue quest (design ref §5).
 */
public class FamilyMember implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Relation { WIFE, SON, DAUGHTER }

    public String name;
    public Relation relation;
    public int age;
    public boolean alive = true;
    public boolean kidnapped = false;
    /** If kidnapped, the clan id holding them hostage. */
    public int captorClanId = -1;

    public FamilyMember() { }
    public FamilyMember(String name, Relation relation, int age) {
        this.name = name; this.relation = relation; this.age = age;
    }

    public boolean isHeirEligible() { return relation == Relation.SON && alive && !kidnapped && age >= 15; }
}
