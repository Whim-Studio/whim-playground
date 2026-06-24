package com.whim.coda.data;

import com.whim.coda.model.Attribute;
import com.whim.coda.model.AttributeSet;
import com.whim.coda.model.CharacterSheet;
import com.whim.coda.model.Edge;
import com.whim.coda.model.Flaw;
import com.whim.coda.model.Reaction;
import com.whim.coda.model.SkillRank;

import java.util.List;

/** Custom pretty-printed JSON serializer for a {@link CharacterSheet}. No external libraries. */
public final class JsonWriter {

    private static final String INDENT = "  ";

    private JsonWriter() {
    }

    /** Produce a pretty-printed JSON string of the full character sheet. */
    public static String toJson(CharacterSheet sheet) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        // name
        line(sb, 1, "\"name\": " + str(sheet.getName()) + ",");

        // species
        String speciesName = sheet.getSpecies() == null ? null : sheet.getSpecies().getName();
        line(sb, 1, "\"species\": " + str(speciesName) + ",");

        // attributes
        AttributeSet attrs = sheet.getAttributes();
        line(sb, 1, "\"attributes\": {");
        attrGroup(sb, "base", attrs, "base");
        sb.append(",\n");
        attrGroup(sb, "speciesMod", attrs, "speciesMod");
        sb.append(",\n");
        attrGroup(sb, "adjusted", attrs, "adjusted");
        sb.append(",\n");
        attrGroup(sb, "modifier", attrs, "modifier");
        sb.append("\n");
        line(sb, 1, "},");

        // reactions
        line(sb, 1, "\"reactions\": {");
        line(sb, 2, "\"quickness\": " + sheet.getReaction(Reaction.QUICKNESS) + ",");
        line(sb, 2, "\"savvy\": " + sheet.getReaction(Reaction.SAVVY) + ",");
        line(sb, 2, "\"stamina\": " + sheet.getReaction(Reaction.STAMINA) + ",");
        line(sb, 2, "\"willpower\": " + sheet.getReaction(Reaction.WILLPOWER));
        line(sb, 1, "},");

        // derived
        line(sb, 1, "\"derived\": {");
        line(sb, 2, "\"health\": " + sheet.getHealth() + ",");
        line(sb, 2, "\"defense\": " + sheet.getDefense() + ",");
        line(sb, 2, "\"courage\": " + sheet.getCourage() + ",");
        line(sb, 2, "\"renown\": " + sheet.getRenown());
        line(sb, 1, "},");

        // skills
        appendSkills(sb, sheet.getSkills());
        sb.append(",\n");

        // edges
        appendNamed(sb, "edges", edgeNames(sheet.getEdges()));
        sb.append(",\n");

        // flaws
        appendNamed(sb, "flaws", flawNames(sheet.getFlaws()));
        sb.append("\n");

        sb.append("}");
        return sb.toString();
    }

    private static void attrGroup(StringBuilder sb, String key, AttributeSet attrs, String which) {
        line(sb, 2, "\"" + key + "\": {");
        Attribute[] vals = Attribute.values();
        for (int i = 0; i < vals.length; i++) {
            Attribute a = vals[i];
            int v;
            if ("base".equals(which)) {
                v = attrs.getBase(a);
            } else if ("speciesMod".equals(which)) {
                v = attrs.getSpeciesMod(a);
            } else if ("adjusted".equals(which)) {
                v = attrs.getAdjusted(a);
            } else {
                v = attrs.getModifier(a);
            }
            String comma = (i < vals.length - 1) ? "," : "";
            line(sb, 3, "\"" + a.name().toLowerCase() + "\": " + v + comma);
        }
        indent(sb, 2);
        sb.append("}");
    }

    private static void appendSkills(StringBuilder sb, List<SkillRank> skills) {
        if (skills.isEmpty()) {
            indent(sb, 1);
            sb.append("\"skills\": []");
            return;
        }
        line(sb, 1, "\"skills\": [");
        for (int i = 0; i < skills.size(); i++) {
            SkillRank sr = skills.get(i);
            line(sb, 2, "{");
            line(sb, 3, "\"name\": " + str(sr.getSkill().getName()) + ",");
            line(sb, 3, "\"key\": " + str(sr.getSkill().getKey().name().toLowerCase()) + ",");
            line(sb, 3, "\"rank\": " + sr.getRank() + ",");
            line(sb, 3, "\"specialty\": " + str(sr.getSpecialty()));
            indent(sb, 2);
            sb.append("}");
            sb.append(i < skills.size() - 1 ? ",\n" : "\n");
        }
        indent(sb, 1);
        sb.append("]");
    }

    private static void appendNamed(StringBuilder sb, String key, List<String> names) {
        if (names.isEmpty()) {
            indent(sb, 1);
            sb.append("\"" + key + "\": []");
            return;
        }
        line(sb, 1, "\"" + key + "\": [");
        for (int i = 0; i < names.size(); i++) {
            line(sb, 2, "{");
            line(sb, 3, "\"name\": " + str(names.get(i)));
            indent(sb, 2);
            sb.append("}");
            sb.append(i < names.size() - 1 ? ",\n" : "\n");
        }
        indent(sb, 1);
        sb.append("]");
    }

    private static java.util.List<String> edgeNames(List<Edge> edges) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Edge e : edges) {
            out.add(e.getName());
        }
        return out;
    }

    private static java.util.List<String> flawNames(List<Flaw> flaws) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Flaw f : flaws) {
            out.add(f.getName());
        }
        return out;
    }

    private static void line(StringBuilder sb, int depth, String content) {
        indent(sb, depth);
        sb.append(content);
        sb.append("\n");
    }

    private static void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append(INDENT);
        }
    }

    /** Render a JSON string literal, or the literal null. */
    private static String str(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
