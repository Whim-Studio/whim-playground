package com.whim.ythub.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.whim.ythub.model.VideoRecord;

/**
 * Read-only {@link AbstractTableModel} backing the Hub's library table.
 *
 * <p>Columns are Title, Category, URL and Date Added. The model keeps its own
 * mutable list of {@link VideoRecord} instances so the UI can append rows as
 * videos are added without rebuilding the whole table. All mutation happens on
 * the Event Dispatch Thread, so no synchronisation is required.</p>
 *
 * <p>Strict Java 8: no {@code var}, no text blocks, no post-8 APIs.</p>
 */
final class VideoTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = { "Title", "Category", "URL", "Date Added" };

    private final List<VideoRecord> records = new ArrayList<VideoRecord>();

    VideoTableModel() {
    }

    /** Replaces all rows with the supplied records (used on startup load). */
    void setRecords(List<VideoRecord> newRecords) {
        records.clear();
        if (newRecords != null) {
            records.addAll(newRecords);
        }
        fireTableDataChanged();
    }

    /** Appends a single record and notifies the table of the new row. */
    void addRecord(VideoRecord record) {
        if (record == null) {
            return;
        }
        int newRow = records.size();
        records.add(record);
        fireTableRowsInserted(newRow, newRow);
    }

    /** Returns the record at the given <em>model</em> row index. */
    VideoRecord getRecordAt(int modelRow) {
        return records.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return records.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        VideoRecord record = records.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return record.getTitle();
            case 1:
                return record.getCategory();
            case 2:
                return record.getUrl();
            case 3:
                return record.getDateAdded();
            default:
                return "";
        }
    }
}
