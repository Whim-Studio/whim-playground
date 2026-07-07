package com.whim.ythub.io;

import com.whim.ythub.model.VideoRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence layer for the YouTube Hub video library.
 *
 * <p>Reads and writes a list of {@link VideoRecord}s to a UTF-8 CSV file
 * ({@code video-library.csv}) in the working directory, using only
 * {@code java.nio.file}. The CSV uses RFC 4180 style quoting so that titles or
 * URLs containing commas, double quotes, or embedded newlines round-trip
 * losslessly: fields are wrapped in double quotes and any embedded {@code "} is
 * escaped as {@code ""}.</p>
 *
 * <p>Column order is fixed as {@code url,title,category,dateAdded} and a header
 * line is written and skipped on load. Written for strict Java 8 (no
 * {@code var}, no text blocks, no post-8 APIs).</p>
 */
public final class LibraryManager {

    /** Default library file name, resolved against the working directory. */
    private static final String DEFAULT_FILE_NAME = "video-library.csv";

    /** Fixed CSV header, matching the field write order. */
    private static final String HEADER = "url,title,category,dateAdded";

    /** Number of logical columns per record. */
    private static final int COLUMN_COUNT = 4;

    private final Path file;

    /** Creates a manager backed by {@code video-library.csv} in the CWD. */
    public LibraryManager() {
        this(Paths.get(DEFAULT_FILE_NAME));
    }

    /**
     * Creates a manager backed by a specific file (useful for testing).
     *
     * @param file the CSV file path; must not be {@code null}
     */
    public LibraryManager(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.file = file;
    }

    /**
     * Loads all records from the library file.
     *
     * <p>Returns an empty list if the file does not exist. The header line is
     * skipped, and blank or malformed lines are silently ignored so a partially
     * corrupt file never aborts the whole load.</p>
     *
     * @return a mutable list of records (never {@code null})
     * @throws IOException if the file exists but cannot be read
     */
    public List<VideoRecord> load() throws IOException {
        List<VideoRecord> records = new ArrayList<VideoRecord>();
        if (!Files.exists(file)) {
            return records;
        }

        // Read the raw text and re-split into logical records: a quoted field
        // may itself contain newlines, so we cannot naively iterate file lines.
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        List<String> rows = splitRows(content);

        boolean headerSkipped = false;
        for (int i = 0; i < rows.size(); i++) {
            String row = rows.get(i);
            if (!headerSkipped) {
                // Skip the first non-empty row as the header.
                if (row.trim().isEmpty()) {
                    continue;
                }
                headerSkipped = true;
                if (HEADER.equals(row.trim())) {
                    continue;
                }
                // If the first row was not the expected header, fall through and
                // still try to parse it as a record (be forgiving of headerless
                // files written by hand).
            }
            if (row.trim().isEmpty()) {
                continue;
            }
            VideoRecord record = decodeRow(row);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    /**
     * Overwrites the library file with the given records (UTF-8).
     *
     * @param records the records to persist; must not be {@code null}
     * @throws IOException if the file cannot be written
     */
    public void save(List<VideoRecord> records) throws IOException {
        if (records == null) {
            throw new IllegalArgumentException("records must not be null");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(HEADER);
            writer.write('\n');
            for (VideoRecord record : records) {
                writer.write(encodeRow(record));
                writer.write('\n');
            }
        }
    }

    /**
     * Appends a single record to the library, persisting it immediately.
     *
     * <p>If the file does not yet exist, a header line is written first.</p>
     *
     * @param record the record to append; must not be {@code null}
     * @throws IOException if the file cannot be written
     */
    public void add(VideoRecord record) throws IOException {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        boolean needsHeader = !Files.exists(file) || Files.size(file) == 0L;
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND)) {
            if (needsHeader) {
                writer.write(HEADER);
                writer.write('\n');
            }
            writer.write(encodeRow(record));
            writer.write('\n');
        }
    }

    // ------------------------------------------------------------------
    // CSV encode / decode helpers
    // ------------------------------------------------------------------

    /** Encodes one record into a single CSV row (no trailing newline). */
    private static String encodeRow(VideoRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(encodeField(record.getUrl())).append(',');
        sb.append(encodeField(record.getTitle())).append(',');
        sb.append(encodeField(record.getCategory())).append(',');
        sb.append(encodeField(record.getDateAdded()));
        return sb.toString();
    }

    /** Quotes and escapes a single field per RFC 4180. */
    private static String encodeField(String value) {
        String v = value == null ? "" : value;
        StringBuilder sb = new StringBuilder(v.length() + 2);
        sb.append('"');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '"') {
                sb.append('"').append('"'); // escape " as ""
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Decodes one CSV row into a record, or {@code null} if it is malformed
     * (e.g. wrong number of columns).
     */
    private static VideoRecord decodeRow(String row) {
        List<String> fields = parseFields(row);
        if (fields.size() != COLUMN_COUNT) {
            return null;
        }
        return new VideoRecord(
                fields.get(0),
                fields.get(1),
                fields.get(2),
                fields.get(3));
    }

    /**
     * Parses a single logical CSV row into its fields, honoring double-quote
     * quoting and {@code ""} escapes. Unquoted fields are also accepted.
     */
    private static List<String> parseFields(String row) {
        List<String> fields = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int n = row.length();
        while (i < n) {
            char c = row.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < n && row.charAt(i + 1) == '"') {
                        current.append('"'); // unescape "" -> "
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * Splits raw file content into logical rows, treating newlines inside
     * quoted fields as part of the field rather than row separators. Handles
     * both {@code \n} and {@code \r\n} line endings.
     */
    private static List<String> splitRows(String content) {
        List<String> rows = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int n = content.length();
        while (i < n) {
            char c = content.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                i++;
            } else if (!inQuotes && (c == '\n' || c == '\r')) {
                // End of a logical row; collapse a following \n after \r.
                if (c == '\r' && i + 1 < n && content.charAt(i + 1) == '\n') {
                    i++;
                }
                rows.add(current.toString());
                current.setLength(0);
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        if (current.length() > 0) {
            rows.add(current.toString());
        }
        return rows;
    }
}
