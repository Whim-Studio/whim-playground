package com.tiwas.rpg.domain;

import com.tiwas.rpg.json.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A single adventure scene. Data + JSON mapping only. */
public final class Scene {

    private String id;
    private String title;
    private String narrative;
    private final List<String> npcNames;

    public Scene() {
        this.npcNames = new ArrayList<String>();
    }

    public String getId() {
        return id;
    }

    public void setId(String s) {
        this.id = s;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        this.title = s;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String s) {
        this.narrative = s;
    }

    public List<String> getNpcNames() {
        return npcNames;
    }

    // ----------------------------------------------------------------- JSON

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("title", title);
        m.put("narrative", narrative);
        List<Object> names = new ArrayList<Object>();
        for (String n : npcNames) {
            names.add(n);
        }
        m.put("npcNames", names);
        return m;
    }

    public static Scene fromMap(Map<String, Object> m) {
        Scene s = new Scene();
        if (m.get("id") != null) {
            s.setId(Json.asString(m.get("id")));
        }
        if (m.get("title") != null) {
            s.setTitle(Json.asString(m.get("title")));
        }
        if (m.get("narrative") != null) {
            s.setNarrative(Json.asString(m.get("narrative")));
        }
        Object namesObj = m.get("npcNames");
        if (namesObj != null) {
            for (Object o : Json.asArray(namesObj)) {
                s.getNpcNames().add(Json.asString(o));
            }
        }
        return s;
    }
}
