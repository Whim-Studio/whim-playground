package com.whimrun.ytplayer.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resolves a YouTube watch URL (or bare video ID) into direct stream URLs by
 * shelling out to {@code yt-dlp}.
 *
 * <p>We deliberately do NOT reimplement YouTube's signature deciphering in Java —
 * that logic changes constantly and is exactly what yt-dlp exists to handle. We
 * treat yt-dlp as a black box that turns a watch URL into direct googlevideo URLs.
 *
 * <p><b>Format choice.</b> We ask for
 * {@code bestvideo[height<=1080]+bestaudio/best[height<=1080]/best}. Rationale:
 * YouTube's muxed (progressive) formats cap at ~720p, so to actually reach 1080p
 * we must take adaptive tracks (separate video + audio) and let libVLC combine
 * them via an input-slave. The fallbacks degrade gracefully to a muxed stream and
 * then to whatever is available, so short/old videos without 1080p still play.
 *
 * <p><b>Threading.</b> This class blocks on the subprocess and must be called off
 * the EDT (see {@code PlayerFrame}, which runs it on a SwingWorker). It performs
 * no Swing access itself.
 */
public final class StreamResolver {

    /** How long we allow yt-dlp to run before giving up. Extraction is usually a few seconds. */
    private static final long TIMEOUT_SECONDS = 60;

    private final String ytDlpPath;

    public StreamResolver() {
        this("yt-dlp");
    }

    public StreamResolver(String ytDlpPath) {
        this.ytDlpPath = ytDlpPath;
    }

    /**
     * Resolve the given input to playable stream URLs.
     *
     * @param urlOrId a full YouTube URL or a bare 11-char video ID
     * @return the resolved stream (video URL, optional separate audio URL, title)
     * @throws ExtractionException with a classified {@link ExtractionException.Kind}
     *         on any failure — never returns a partial/empty result.
     */
    public ResolvedStream resolve(String urlOrId) throws ExtractionException {
        final String target = normalize(urlOrId);

        // First get the direct URL(s) via -g. Adaptive selection prints two lines
        // (video, then audio); a muxed selection prints one. --no-playlist keeps us
        // from accidentally resolving an entire playlist. --get-title gives us a
        // window title in the same call.
        List<String> command = new ArrayList<String>();
        command.add(ytDlpPath);
        command.add("--no-playlist");
        command.add("-f");
        command.add("bestvideo[height<=1080]+bestaudio/best[height<=1080]/best");
        command.add("--get-title");
        command.add("-g");
        command.add(target);

        ProcessOutput out = run(command);

        if (out.exitCode != 0) {
            throw classifyFailure(out);
        }

        // Successful output ordering with --get-title + -g is:
        //   line 0: title
        //   line 1: video URL
        //   line 2: audio URL   (only present for adaptive selections)
        List<String> lines = out.stdoutLines;
        if (lines.size() < 2) {
            throw new ExtractionException(ExtractionException.Kind.NO_PLAYABLE_FORMAT,
                    "yt-dlp produced no stream URL. stderr: " + out.firstStderrLine());
        }

        String title = lines.get(0);
        String videoUrl = lines.get(1);
        String audioUrl = lines.size() >= 3 ? lines.get(2) : null;

        // Defensive: -g URLs must be http(s). Anything else means we misparsed.
        if (!looksLikeUrl(videoUrl)) {
            throw new ExtractionException(ExtractionException.Kind.UNKNOWN,
                    "Unexpected yt-dlp output; first URL line was: " + videoUrl);
        }
        if (audioUrl != null && !looksLikeUrl(audioUrl)) {
            // A non-URL third line just means the selection was muxed after all.
            audioUrl = null;
        }

        return new ResolvedStream(videoUrl, audioUrl, title);
    }

    /** Accept bare 11-character IDs as a convenience; pass full URLs through untouched. */
    private static String normalize(String input) {
        String s = input == null ? "" : input.trim();
        if (s.isEmpty()) {
            return s;
        }
        if (s.matches("[A-Za-z0-9_-]{11}")) {
            return "https://www.youtube.com/watch?v=" + s;
        }
        return s;
    }

    private static boolean looksLikeUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    /**
     * Map a non-zero yt-dlp exit to a specific {@link ExtractionException.Kind}
     * by inspecting stderr. yt-dlp's messages are fairly stable strings.
     */
    private ExtractionException classifyFailure(ProcessOutput out) {
        String err = out.stderrJoined().toLowerCase();

        if (err.contains("drm") || err.contains("this video is protected")) {
            return new ExtractionException(ExtractionException.Kind.DRM_PROTECTED,
                    out.stderrJoined());
        }
        if (err.contains("private video") || err.contains("video unavailable")
                || err.contains("removed") || err.contains("age") || err.contains("sign in")
                || err.contains("not available in your country")) {
            return new ExtractionException(ExtractionException.Kind.VIDEO_UNAVAILABLE,
                    out.stderrJoined());
        }
        if (err.contains("unable to download") || err.contains("failed to resolve")
                || err.contains("network") || err.contains("timed out")
                || err.contains("getaddrinfo") || err.contains("connection")) {
            return new ExtractionException(ExtractionException.Kind.NETWORK_ERROR,
                    out.stderrJoined());
        }
        if (err.contains("requested format") && err.contains("not available")) {
            return new ExtractionException(ExtractionException.Kind.NO_PLAYABLE_FORMAT,
                    out.stderrJoined());
        }
        return new ExtractionException(ExtractionException.Kind.UNKNOWN,
                out.stderrJoined().isEmpty() ? "yt-dlp exited with code " + out.exitCode
                        : out.stderrJoined());
    }

    /**
     * Launch the subprocess, drain stdout/stderr concurrently (so a full pipe
     * buffer can never deadlock the child), and enforce a timeout.
     */
    private ProcessOutput run(List<String> command) throws ExtractionException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        final Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            // The classic "command not found" surfaces here on most platforms.
            throw new ExtractionException(ExtractionException.Kind.YT_DLP_NOT_FOUND,
                    "Could not start yt-dlp (" + ytDlpPath + "): " + e.getMessage(), e);
        }

        StreamGobbler outGobbler = new StreamGobbler(process.getInputStream());
        StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream());
        Thread tOut = new Thread(outGobbler, "yt-dlp-stdout");
        Thread tErr = new Thread(errGobbler, "yt-dlp-stderr");
        tOut.setDaemon(true);
        tErr.setDaemon(true);
        tOut.start();
        tErr.start();

        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ExtractionException(ExtractionException.Kind.UNKNOWN,
                    "Interrupted while waiting for yt-dlp.", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new ExtractionException(ExtractionException.Kind.NETWORK_ERROR,
                    "yt-dlp timed out after " + TIMEOUT_SECONDS + "s.");
        }

        // Make sure we have all the piped output before reading the buffers.
        try {
            tOut.join(2000);
            tErr.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ProcessOutput result = new ProcessOutput();
        result.exitCode = process.exitValue();
        result.stdoutLines = outGobbler.lines();
        result.stderrLines = errGobbler.lines();
        return result;
    }

    /** Reads a stream fully on its own thread into a list of trimmed, non-empty lines. */
    private static final class StreamGobbler implements Runnable {
        private final InputStream in;
        private final List<String> collected = new ArrayList<String>();

        StreamGobbler(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        synchronized (collected) {
                            collected.add(trimmed);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Stream closed as the process died; whatever we have is enough.
            }
        }

        List<String> lines() {
            synchronized (collected) {
                return new ArrayList<String>(collected);
            }
        }
    }

    /** Simple value holder for a completed process invocation. */
    private static final class ProcessOutput {
        int exitCode;
        List<String> stdoutLines = new ArrayList<String>();
        List<String> stderrLines = new ArrayList<String>();

        String stderrJoined() {
            StringBuilder sb = new StringBuilder();
            for (String l : stderrLines) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(l);
            }
            return sb.toString();
        }

        String firstStderrLine() {
            return stderrLines.isEmpty() ? "(none)" : stderrLines.get(0);
        }
    }
}
