package com.whim.kenshi.domain;

import com.whim.kenshi.api.Enums.FactionId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A named grouping of characters under one faction. Holds member ids only; the
 * {@link Character}s themselves live in {@link WorldState}.
 */
public final class Squad {

    private final String id;
    private String name;
    private final FactionId faction;
    private final List<String> memberIds = new ArrayList<String>();

    public Squad(String id, String name, FactionId faction) {
        this.id = id;
        this.name = name;
        this.faction = faction;
    }

    public String id() { return id; }
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public FactionId faction() { return faction; }

    /** Live, read-only view of member ids (canonical add order). */
    public List<String> memberIds() {
        return Collections.unmodifiableList(memberIds);
    }

    public void addMember(String characterId) {
        if (!memberIds.contains(characterId)) memberIds.add(characterId);
    }

    public void removeMember(String characterId) {
        memberIds.remove(characterId);
    }

    public int size() { return memberIds.size(); }
}
