package com.tiwas.rpg.domain;

import com.tiwas.rpg.json.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A loadable adventure: metadata, scenes, and NPCs, with JSON save/load. */
public final class AdventureModule {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private String title;
    private String author;
    private String description;
    private final List<Scene> scenes;
    private final List<Npc> npcs;

    public AdventureModule() {
        this.scenes = new ArrayList<Scene>();
        this.npcs = new ArrayList<Npc>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String s) {
        this.title = s;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String s) {
        this.author = s;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String s) {
        this.description = s;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    // ----------------------------------------------------------------- JSON

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("title", title);
        m.put("author", author);
        m.put("description", description);
        List<Object> sc = new ArrayList<Object>();
        for (Scene s : scenes) {
            sc.add(s.toMap());
        }
        m.put("scenes", sc);
        List<Object> np = new ArrayList<Object>();
        for (Npc n : npcs) {
            np.add(n.toMap());
        }
        m.put("npcs", np);
        return m;
    }

    public static AdventureModule fromMap(Map<String, Object> m) {
        AdventureModule a = new AdventureModule();
        if (m.get("title") != null) {
            a.setTitle(Json.asString(m.get("title")));
        }
        if (m.get("author") != null) {
            a.setAuthor(Json.asString(m.get("author")));
        }
        if (m.get("description") != null) {
            a.setDescription(Json.asString(m.get("description")));
        }
        Object scObj = m.get("scenes");
        if (scObj != null) {
            for (Object o : Json.asArray(scObj)) {
                a.getScenes().add(Scene.fromMap(Json.asObject(o)));
            }
        }
        Object npObj = m.get("npcs");
        if (npObj != null) {
            for (Object o : Json.asArray(npObj)) {
                a.getNpcs().add(Npc.fromMap(Json.asObject(o)));
            }
        }
        return a;
    }

    public String toJson() {
        return Json.writePretty(toMap());
    }

    public static AdventureModule fromJson(String json) {
        return fromMap(Json.asObject(Json.parse(json)));
    }

    public void save(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(fos, UTF8);
            w.write(toJson());
            w.flush();
        } finally {
            fos.close();
        }
    }

    public static AdventureModule load(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        try {
            Reader r = new InputStreamReader(fis, UTF8);
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return fromJson(sb.toString());
        } finally {
            fis.close();
        }
    }
}
