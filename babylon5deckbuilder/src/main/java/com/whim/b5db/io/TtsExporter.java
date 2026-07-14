package com.whim.b5db.io;

import com.whim.b5db.model.Card;
import com.whim.b5db.model.ContestType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Exports the loaded card catalogue as a flat CSV suitable for importing into
 * Tabletop Simulator (or a spreadsheet). One row per card with its stats and a
 * flattened effect description.
 */
public final class TtsExporter {

    /** Build the CSV text for a catalogue. */
    public String toCsv(List<Card> cards) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,faction,type,cost,prestige,diplomacy,intrigue,military,psi,contest,difficulty,effects,text\n");
        for (Card c : cards) {
            sb.append(csv(c.id())).append(',')
                    .append(csv(c.name())).append(',')
                    .append(csv(c.faction().name())).append(',')
                    .append(csv(c.type().name())).append(',')
                    .append(c.cost()).append(',')
                    .append(c.prestige()).append(',')
                    .append(c.attribute(ContestType.DIPLOMACY)).append(',')
                    .append(c.attribute(ContestType.INTRIGUE)).append(',')
                    .append(c.attribute(ContestType.MILITARY)).append(',')
                    .append(c.attribute(ContestType.PSI)).append(',')
                    .append(csv(c.contest() == null ? "" : c.contest().name())).append(',')
                    .append(c.difficulty()).append(',')
                    .append(csv(c.effects().toString())).append(',')
                    .append(csv(c.text())).append('\n');
        }
        return sb.toString();
    }

    public void export(List<Card> cards, Path path) throws IOException {
        if (path.toAbsolutePath().getParent() != null) {
            Files.createDirectories(path.toAbsolutePath().getParent());
        }
        Files.write(path, toCsv(cards).getBytes(StandardCharsets.UTF_8));
    }

    /** Quote a CSV field if it contains a comma, quote, or newline. */
    private String csv(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
