package com.whim.necromunda.test;

import java.util.Map;

import com.whim.necromunda.engine.roster.RosterRules;
import com.whim.necromunda.engine.setup.DemoSetup;
import com.whim.necromunda.model.Fighter;
import com.whim.necromunda.model.FighterType;
import com.whim.necromunda.model.Gang;
import com.whim.necromunda.model.House;
import com.whim.necromunda.model.Stat;
import com.whim.necromunda.model.StatLine;
import com.whim.necromunda.persistence.GangCodec;
import com.whim.necromunda.persistence.Json;
import com.whim.necromunda.persistence.SaveManager;

/**
 * Milestone 3 — dependency-free JSON round-trip fidelity and roster validation.
 */
public final class PersistenceTest {

    public static void main(String[] args) {
        Assert a = new Assert();

        a.section("JSON reader/writer round-trip");
        String src = "{\"a\":1,\"b\":\"he said \\\"hi\\\"\",\"c\":[-3,2,{\"d\":true}],\"e\":[]}";
        Object parsed = Json.read(src);
        String rewritten = Json.write(parsed);
        a.that("round-trip is stable", src.equals(rewritten));
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) parsed;
        a.that("escaped quote preserved", "he said \"hi\"".equals(obj.get("b")));
        a.that("nested negative int", Json.write(parsed).contains("-3"));

        a.section("Gang save/load fidelity");
        Gang gang = DemoSetup.gangA();
        String json = SaveManager.toJsonString(gang);
        Gang loaded = SaveManager.fromJsonString(json);
        a.equals("gang name", gang.name(), loaded.name());
        a.equals("house", gang.house(), loaded.house());
        a.equalsInt("credits", gang.credits(), loaded.credits());
        a.equalsInt("roster size", gang.roster().size(), loaded.roster().size());
        a.equalsInt("rating preserved", gang.rating(), loaded.rating());

        Fighter of = gang.roster().get(0);
        Fighter lf = loaded.roster().get(0);
        a.equals("fighter name", of.name(), lf.name());
        a.equals("fighter role", of.type(), lf.type());
        a.equals("fighter armour", of.armour(), lf.armour());
        a.equalsInt("fighter experience", of.experience(), lf.experience());
        a.equalsInt("fighter WS", of.stat(Stat.WS), lf.stat(Stat.WS));
        a.equalsInt("weapon count", of.weapons().size(), lf.weapons().size());
        a.that("weapon resolved from catalogue",
                !lf.weapons().isEmpty() && lf.weapons().get(0) != null);

        // Idempotency: save(load(x)) == save(x)
        String json2 = SaveManager.toJsonString(loaded);
        a.that("save is idempotent", json.equals(json2));

        a.section("Roster validation");
        a.that("sample gang is legal", RosterRules.validate(gang).isLegal());

        Gang noLeader = new Gang("Headless", House.ORLOCK);
        noLeader.add(ganger("g1"));
        noLeader.add(ganger("g2"));
        noLeader.add(ganger("g3"));
        RosterRules.Result r1 = RosterRules.validate(noLeader);
        a.that("no-leader gang is illegal", !r1.isLegal());
        a.that("...flags the missing leader", joined(r1).contains("Leader"));

        Gang tiny = new Gang("Duo", House.ORLOCK);
        tiny.add(leader("l1"));
        tiny.add(ganger("g1"));
        a.that("2-fighter gang is illegal", !RosterRules.validate(tiny).isLegal());

        Gang juveHeavy = new Gang("Rookies", House.ORLOCK);
        juveHeavy.add(leader("l1"));
        juveHeavy.add(ganger("g1"));
        juveHeavy.add(juve("j1"));
        juveHeavy.add(juve("j2"));
        a.that("juves outnumbering gangers is illegal",
                !RosterRules.validate(juveHeavy).isLegal());

        a.finish();
    }

    private static String joined(RosterRules.Result r) {
        StringBuilder sb = new StringBuilder();
        for (String p : r.problems()) {
            sb.append(p).append(' ');
        }
        return sb.toString();
    }

    private static Fighter make(String id, FighterType type) {
        return new Fighter(id, id, type, StatLine.of(4, 3, 3, 3, 3, 1, 3, 1, 7));
    }

    private static Fighter leader(String id) { return make(id, FighterType.LEADER); }
    private static Fighter ganger(String id) { return make(id, FighterType.GANGER); }
    private static Fighter juve(String id) { return make(id, FighterType.JUVE); }
}
