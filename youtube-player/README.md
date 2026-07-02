# Whim.Run — Java 8 Swing YouTube Player

A standalone Java 8 Swing utility that plays YouTube videos in-pane. It is **not**
a WhimKit subsystem — WhimKit deliberately excludes in-pane YouTube playback. This
app is allowed native dependencies and third-party libraries (Java 8 compatible).

It works by combining two external pieces:

- **yt-dlp** (external subprocess) resolves a YouTube watch URL into direct,
  playable stream URLs. Signature deciphering is left entirely to yt-dlp.
- **vlcj → libVLC** (native) decodes and renders the stream (DASH/HLS,
  H.264/VP9/AV1) into an embedded heavyweight AWT surface inside the Swing window.

## Architecture

```
PlayerFrame (Swing, EDT)
   │  address bar + Play
   ▼
StreamResolver ──subprocess──► yt-dlp        (runs on a SwingWorker, off the EDT)
   │  ResolvedStream (video URL [+ audio URL], title)
   ▼
VideoPlayer ──► vlcj EmbeddedMediaPlayerComponent ──► libVLC (native decode/render)
   ▲  libVLC events on native threads
   └── re-marshalled onto the EDT via PlaybackCallback
```

| File | Role |
|------|------|
| `Main.java` | Entry point: native discovery, builds player + frame on the EDT |
| `ui/PlayerFrame.java` | The window: address bar, video surface, transport controls, status line |
| `media/StreamResolver.java` | Shells out to yt-dlp, classifies failures |
| `media/ResolvedStream.java` | Resolved video URL (+ optional adaptive audio URL) and title |
| `media/ExtractionException.java` | Typed extraction failures → user-facing messages |
| `player/VideoPlayer.java` | vlcj wrapper; marshals native callbacks to the EDT |
| `player/PlaybackCallback.java` | EDT-guaranteed playback events consumed by the frame |

## Dependencies

| Artifact | Version | Java 8? |
|----------|---------|---------|
| `uk.co.caprica:vlcj` | `4.8.2` | Yes — the 4.x line is deliberately Java 8 (5.x requires Java 11+/JPMS). Pulls in `vlcj-natives` and JNA 5.x, all Java 8 safe. |

Declared in `pom.xml`. The Maven Shade plugin builds a runnable fat JAR.

**Format selection:** `bestvideo[height<=1080]+bestaudio/best[height<=1080]/best`.
YouTube's muxed (progressive) formats cap at ~720p, so to actually reach 1080p we
take adaptive tracks (separate video + audio) and combine them in libVLC via the
`:input-slave` option. The fallbacks degrade to a single muxed stream, then to
whatever is available, so older/short videos still play.

## Prerequisites

### 1. yt-dlp (must be on PATH)
- **Windows:** `winget install yt-dlp` or download `yt-dlp.exe` onto PATH
- **macOS:** `brew install yt-dlp`
- **Linux:** `sudo pip install yt-dlp` or your distro package
- Verify: `yt-dlp --version`

### 2. Native VLC / libVLC (64-bit; must match your JVM architecture)
- **Windows:** install 64-bit VLC from videolan.org
- **macOS:** install `VLC.app` into `/Applications`
- **Linux:** `sudo apt install vlc` (or `libvlc-dev` / `vlc-plugin-base`)
- Verify: `vlc --version`

If libVLC is missing, the app shows a dialog with install instructions and exits
cleanly (no JNA stack trace).

## Build & run

```bash
cd youtube-player
mvn clean package          # produces target/ytplayer.jar (fat JAR)
java -jar target/ytplayer.jar
```

Then paste a YouTube URL (or a bare 11-char video ID) and press **Play**.

## Known limitations

Stated once, plainly:

- **DRM (Widevine) videos cannot be played.** libVLC has no CDM integration.
- **Age-restricted / login-required videos** generally fail extraction unless
  yt-dlp is configured with cookies (not wired up here).
- **yt-dlp breakage:** when YouTube changes its player, extraction can break until
  yt-dlp is updated (`yt-dlp -U`). This is inherent to the approach.
- **Native dependency:** this is not pure Java — it requires libVLC per OS.
- **ToS:** using yt-dlp to extract stream URLs sits outside YouTube's Terms of
  Service; intended for personal/local use.
