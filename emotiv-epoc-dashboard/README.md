# Emotiv EPOC v1 — Live Swing Dashboard

A desktop Java 8 + Swing application that connects to the Emotiv **EmoEngine**
(`edk.dll`) via **JNA** and displays live neuroheadset data for the original
**14-channel EPOC v1** (Research/Developer SDK, EmoEngine era).

> Target: **64-bit JVM + 64-bit `edk.dll`**. On x64 there is a single calling
> convention, so a plain JNA `Library` mapping is correct (no `StdCallLibrary`).

## Features

| Area | What it shows |
|------|---------------|
| Contact quality | Per-sensor colored grid (NO_SIGNAL → GOOD) |
| Connection | Supervised connect with auto-reconnect + user attach/detach |
| Power / link | Battery charge (v1 scale ~0–5) and wireless signal |
| Affectiv | Engagement, excitement (short/long), meditation, frustration |
| Expressiv | Upper/lower face action + power; blink/wink/look indicators |
| Cognitiv | Live command + power, in-app training wizard (neutral/push) |
| Profiles | Save/Load `.emu`; auto-load `default.emu` on attach |
| Logging | CSV of all detections on a background thread |
| Emulator | EmoComposer toggle (no hardware needed) |
| Raw EEG | Live 128 Hz 14-channel waveform (**Research/Raw-EEG license only**) |

## Requirements

- 64-bit JDK 8 and a 64-bit JRE.
- [JNA](https://github.com/java-native-access/jna) `jna-5.x.jar` (e.g. 5.14.0) — the only third-party dependency.
- Emotiv SDK / Control Panel installed, USB dongle driver installed.
- A **64-bit `edk.dll`** (e.g. from a later SDK release or the Emotiv Community
  SDK `bin/win64`) plus its dependency DLLs.

> **64-bit + v1 caveat:** the original SDK v1.0.0.x shipped a 32-bit `edk.dll`
> only. 64-bit builds appear in later releases / the Community SDK. That a given
> 64-bit `edk.dll` drives *v1 hardware specifically* is unproven until
> `EE_EngineConnect` returns `EDK_OK` with your headset — that is the ground-truth test.

## Build

```bash
# from this directory (emotiv-epoc-dashboard/)
javac -cp jna-5.14.0.jar -d out src/com/whim/emotiv/*.java
```

## Run

```bash
java -cp "out;jna-5.14.0.jar" -Djna.library.path=C:\path\to\edk\win64 \
     com.whim.emotiv.HeadsetDashboard
```

- **Bitness must match:** a 64-bit JVM cannot load a 32-bit `edk.dll`. Verify with
  `dumpbin /headers edk.dll | findstr machine` → expect `x64`, and
  `java -XshowSettings:properties -version` → `sun.arch.data.model = 64`.
- Put the 64-bit `edk.dll` **and its dependency DLLs** on `-Djna.library.path`
  (or `PATH`).
- Close the Emotiv Control Panel while the app uses `EE_EngineConnect` (both grab
  the headset). For hardware-free development, launch **EmoComposer** and tick the
  **EmoComposer (emulator)** box (connects to `127.0.0.1:1726`).

## Quick validation

1. Connect → status shows `Connected via headset — waiting…`, then `Headset attached`.
2. Green the contact cells.
3. Affectiv gauges drift after ~30–60 s warm-up.
4. Expressiv: blink flashes the indicator; smile drives the Lower gauge.
5. Cognitiv: Enable Push → Train Neutral (Accept) → Train Push (Accept) → reproduce.
6. Save Profile, disconnect/reconnect, Load Profile → training restored.
7. Raw EEG (licensed): tick **Raw EEG**; if `numSamples` stays 0 your edition
   lacks the Raw-EEG license (EmoComposer still emits synthetic raw data).

## Constants to verify once (`EdkConstants.java`)

Items tagged `// VERIFY` against your `EmoStateDLL.h` / `edk.h`:

- Contact-quality channel count/order (16 vs 18) — labels only; values are always correct.
- Expressiv/Cognitiv action hex values and the Cognitiv training-event ordering.
- Raw EEG `ED_*` channel indices.

A wrong `VERIFY` value only mislabels — it never crashes.

## Files

| File | Role |
|------|------|
| `EdkLibrary.java` | JNA binding to `edk.dll` |
| `EdkConstants.java` | Consolidated enum/constant table (single source of truth) |
| `EmoEnginePoller.java` | Supervised event pump; all native calls; EDT-safe publishing |
| `AffectivGauge.java` | Labeled 0–1 bar component |
| `WaveformPanel.java` | Stacked per-channel raw EEG traces (DC-removed) |
| `DetectionLogger.java` | Background CSV writer |
| `HeadsetDashboard.java` | Swing UI; `main` entry point |

## Threading model

One daemon **poller thread** owns every `edk` call and the engine/state/data
handles. It publishes immutable snapshots to the EDT via `SwingUtilities.invokeLater`.
UI → engine actions (Cognitiv training, profile save/load) are queued and executed
on the poller thread. **Native never runs on the EDT; Swing never runs off it.**
