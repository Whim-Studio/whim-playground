package com.whim.ythub.model;

/**
 * Immutable domain model for a single saved YouTube entry in the Hub.
 *
 * <p>This class is the shared contract between every layer of the application.
 * The I/O layer ({@code com.whim.ythub.io}), the logic layer
 * ({@code com.whim.ythub.logic}) and the Swing UI ({@code com.whim.ythub.ui})
 * all depend only on this definition and never on each other. It is
 * deliberately free of any networking, file, or UI concerns.</p>
 *
 * <p><strong>Frozen contract:</strong> the public constructor and getter
 * signatures below must not change, since other layers compile against them.
 * Written for strict Java 8 (no {@code var}, no text blocks, no post-8 APIs).</p>
 */
public final class VideoRecord {

    private final String url;
    private final String title;
    private final String category;
    private final String dateAdded; // ISO date, yyyy-MM-dd

    public VideoRecord(String url, String title, String category, String dateAdded) {
        this.url = nullSafe(url);
        this.title = nullSafe(title);
        this.category = nullSafe(category);
        this.dateAdded = nullSafe(dateAdded);
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String toString() {
        return "VideoRecord{title='" + title + "', category='" + category
                + "', url='" + url + "', dateAdded='" + dateAdded + "'}";
    }
}
