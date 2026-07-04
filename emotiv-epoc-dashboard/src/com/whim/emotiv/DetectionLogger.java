package com.whim.emotiv;

import java.io.*;
import java.util.concurrent.*;

/** Appends one CSV row per Snapshot on a background thread. EDT-safe. */
public class DetectionLogger {

    private final BlockingQueue<String> rows = new LinkedBlockingQueue<String>(4096);
    private volatile boolean running = false;
    private Thread writer;
    private boolean headerWritten = false;
    private int droppedReported = 0;

    public interface Note { void status(String msg); }
    private final Note note;

    public DetectionLogger(Note note) { this.note = note; }

    public synchronized void start(final File file) {
        if (running) return;
        running = true;
        headerWritten = false;
        writer = new Thread(new Runnable() {
            public void run() {
                BufferedWriter out = null;
                try {
                    out = new BufferedWriter(new FileWriter(file));
                    while (running || !rows.isEmpty()) {
                        String line = rows.poll(200, TimeUnit.MILLISECONDS);
                        if (line != null) { out.write(line); out.write("\n"); out.flush(); }
                    }
                } catch (Exception e) {
                    if (note != null) note.status("Log error: " + e.getMessage());
                } finally {
                    if (out != null) try { out.close(); } catch (IOException ignored) {}
                }
            }
        }, "Detection-Logger");
        writer.setDaemon(true);
        writer.start();
        if (note != null) note.status("Logging → " + file.getName());
    }

    public synchronized void stop() {
        running = false;
        if (writer != null) { writer.interrupt(); writer = null; }
    }

    /** Called on the EDT from onSnapshot. Non-blocking. */
    public void log(EmoEnginePoller.Snapshot s) {
        if (!running) return;
        if (!headerWritten) { headerWritten = true; enqueue(header(s)); }
        enqueue(row(s));
    }

    private void enqueue(String line) {
        if (!rows.offer(line)) {   // queue full -> drop, report once per batch
            if (droppedReported++ % 100 == 0 && note != null)
                note.status("Log queue full — dropping rows (disk slow?)");
        }
    }

    private String header(EmoEnginePoller.Snapshot s) {
        StringBuilder b = new StringBuilder("time_ms");
        for (int i = 0; i < s.names.length; i++) b.append(",cq_").append(s.names[i]);
        b.append(",battery,batteryMax,signal");
        b.append(",engagement,excite_short,excite_long,meditation,frustration");
        b.append(",upperFace,upperPower,lowerFace,lowerPower");
        b.append(",blink,winkL,winkR,lookL,lookR");
        b.append(",cognitiv,cognitivPower");
        return b.toString();
    }

    private String row(EmoEnginePoller.Snapshot s) {
        StringBuilder b = new StringBuilder();
        b.append(System.currentTimeMillis());
        for (int i = 0; i < s.quality.length; i++) b.append(',').append(s.quality[i]);
        b.append(',').append(s.battery).append(',').append(s.batteryMax).append(',').append(s.signal);
        b.append(',').append(s.engagement).append(',').append(s.excitementST)
         .append(',').append(s.excitementLT).append(',').append(s.meditation)
         .append(',').append(s.frustration);
        b.append(',').append(s.upperFace).append(',').append(s.upperPower)
         .append(',').append(s.lowerFace).append(',').append(s.lowerPower);
        b.append(',').append(s.blink ? 1 : 0).append(',').append(s.winkL ? 1 : 0)
         .append(',').append(s.winkR ? 1 : 0).append(',').append(s.lookL ? 1 : 0)
         .append(',').append(s.lookR ? 1 : 0);
        b.append(',').append(s.cognitivAction).append(',').append(s.cognitivPower);
        return b.toString();
    }
}
