package com.whim.ytmeta.model;

/**
 * Immutable domain model holding the metadata extracted for a single YouTube
 * video, plus the timestamp at which the extraction was performed.
 *
 * The class is intentionally free of any networking or UI concerns so that the
 * logic layer (the parser) and the view layer (the Swing panel) depend only on
 * this definition and never on each other.
 */
public final class VideoMetadata {

    /** Tab character used to separate spreadsheet columns. */
    private static final String TAB = "\t";

    private final String processingTimestamp; // Column 1: YYYY-MM-DD HH:mm:ss
    private final String url;                  // Column 2: input URL
    private final String uploadDate;           // Column 3: YYYY-MM-DD
    private final String duration;             // Column 4: HH:MM:SS
    private final String channelName;          // Column 5: uploader
    // Column 6 and Column 7 are intentionally blank layout placeholders.
    private final String title;                // Column 8: video title

    public VideoMetadata(String processingTimestamp,
                         String url,
                         String uploadDate,
                         String duration,
                         String channelName,
                         String title) {
        this.processingTimestamp = nullSafe(processingTimestamp);
        this.url = nullSafe(url);
        this.uploadDate = nullSafe(uploadDate);
        this.duration = nullSafe(duration);
        this.channelName = nullSafe(channelName);
        this.title = nullSafe(title);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public String getProcessingTimestamp() {
        return processingTimestamp;
    }

    public String getUrl() {
        return url;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getDuration() {
        return duration;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Renders the metadata as a single tab-delimited spreadsheet row in the
     * required index order:
     *
     * <pre>
     * 1: timestamp  2: url  3: uploadDate  4: duration  5: channel
     * 6: (blank)    7: (blank)             8: title
     * </pre>
     *
     * The two consecutive tabs between the channel name and the title preserve
     * the empty columns 6 and 7 so the row aligns correctly when pasted into a
     * spreadsheet.
     */
    public String toTabDelimitedString() {
        StringBuilder row = new StringBuilder();
        row.append(processingTimestamp).append(TAB); // 1
        row.append(url).append(TAB);                 // 2
        row.append(uploadDate).append(TAB);          // 3
        row.append(duration).append(TAB);            // 4
        row.append(channelName).append(TAB);         // 5
        row.append(TAB);                             // 6 (blank)
        row.append(TAB);                             // 7 (blank)
        row.append(title);                           // 8
        return row.toString();
    }

    @Override
    public String toString() {
        return toTabDelimitedString();
    }
}
