package com.whim.xcom.rules.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Loads a {@link RulesetData} data pack from JSON (classpath resource or any
 * {@link Reader}) using Gson. This is the extension point for mods/variants:
 * point it at a different pack to change all content without touching the engine.
 */
public final class DataRulesetLoader {

    private final Gson gson = new Gson();

    private DataRulesetLoader() {
    }

    public static DataRulesetLoader create() {
        return new DataRulesetLoader();
    }

    /**
     * @param resourcePath classpath path, e.g. {@code "data/rules1994.json"}
     * @return the parsed data pack
     * @throws IOException if the resource is missing or malformed
     */
    public RulesetData loadFromClasspath(String resourcePath) throws IOException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            in = DataRulesetLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (in == null) {
            throw new IOException("Data pack not found on classpath: " + resourcePath);
        }
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        try {
            return parse(reader);
        } finally {
            reader.close();
        }
    }

    public RulesetData parse(Reader reader) throws IOException {
        try {
            RulesetData data = gson.fromJson(reader, RulesetData.class);
            if (data == null) {
                throw new IOException("Empty data pack");
            }
            return data;
        } catch (JsonSyntaxException e) {
            throw new IOException("Malformed data pack: " + e.getMessage(), e);
        }
    }
}
