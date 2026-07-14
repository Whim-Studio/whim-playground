package com.whim.necromunda.persistence;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.whim.necromunda.model.Gang;

/**
 * Reads and writes gang roster files ({@code *.gang.json}) using the
 * dependency-free {@link Json} writer and {@link GangCodec}. UTF-8 throughout.
 */
public final class SaveManager {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private SaveManager() {
    }

    /** Serialize a gang to a JSON string. */
    public static String toJsonString(Gang gang) {
        return Json.write(GangCodec.toJson(gang));
    }

    /** Parse a gang from a JSON string. */
    @SuppressWarnings("unchecked")
    public static Gang fromJsonString(String json) {
        Object parsed = Json.read(json);
        return GangCodec.fromJson((Map<String, Object>) parsed);
    }

    public static void saveGang(Gang gang, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, toJsonString(gang).getBytes(UTF8));
    }

    public static Gang loadGang(String filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        return fromJsonString(new String(bytes, UTF8));
    }
}
