// audio.js — self-contained synthesized audio engine for Whim Asteroids.
//
// Per CLAUDE.md's "no asset pipelines" rule, this module ships ZERO audio
// files. Every sound effect is generated at runtime via the Web Audio API
// using OscillatorNode + GainNode and a procedurally-built noise buffer, so
// there is nothing for Vite to bundle and no binary assets to commit.
//
// Vanilla JS ES module, no dependencies. One shared AudioContext is created
// lazily on first use; mobile autoplay policy is handled via resumeAudio(),
// which the game calls from the first user gesture. Every exported call
// no-ops safely if audio is unavailable, suspended, or muted.

const MUTE_KEY = 'whim-muted';

let ctx = null;
let noiseBuffer = null;
let muted = readMutedFlag();

// --- Music state -----------------------------------------------------------
// The bed is in A minor. Frequencies (Hz) for the notes the bassline and
// arpeggio draw from, so every SFX (tuned to the same key below) sits in tune
// with the music.
const A2 = 110.0;
const C3 = 130.81;
const D3 = 146.83;
const E3 = 164.81;
const G3 = 196.0;
const A3 = 220.0;
const C4 = 261.63;
const D4 = 293.66;
const E4 = 329.63;
const G4 = 392.0;
const A4 = 440.0;

// Slow root movement (one chord root per bar) and a sparse pentatonic
// arpeggio drawn from A-minor — unobtrusive but recognisably "space arcade".
const BASS_LINE = [A2, A2, G3, G3, C3, C3, E3, E3];
const ARP_NOTES = [A3, C4, E4, A4, G4, E4, D4, C4];

let musicGain = null; // dedicated low master for the music bed
let musicTimer = null; // setInterval id driving the lookahead scheduler
let musicStep = 0; // 16th-note counter
let nextNoteTime = 0; // absolute AudioContext time of the next step
let musicRunning = false;

const TEMPO = 92; // BPM
const SECONDS_PER_16TH = 60 / TEMPO / 4;
const LOOKAHEAD_MS = 25; // how often the scheduler wakes up
const SCHEDULE_AHEAD = 0.12; // how far ahead (s) we schedule notes
const MUSIC_LEVEL = 0.06; // master music gain (kept well below SFX)

function readMutedFlag() {
  try {
    return localStorage.getItem(MUTE_KEY) === 'true';
  } catch (_e) {
    return false;
  }
}

// Lazily create (or return) the single shared AudioContext.
function getCtx() {
  if (ctx) return ctx;
  try {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    ctx = new AC();
  } catch (_e) {
    ctx = null;
  }
  return ctx;
}

// Build a 1-second white-noise buffer once, reused by all noise voices.
function getNoiseBuffer(audio) {
  if (noiseBuffer) return noiseBuffer;
  try {
    const len = Math.floor(audio.sampleRate * 1);
    const buf = audio.createBuffer(1, len, audio.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1;
    noiseBuffer = buf;
  } catch (_e) {
    noiseBuffer = null;
  }
  return noiseBuffer;
}

// True only when it is safe + desired to actually emit sound.
function ready() {
  if (muted) return null;
  const audio = getCtx();
  if (!audio) return null;
  if (audio.state === 'suspended') return null;
  return audio;
}

// Resume the AudioContext from a user gesture (mobile autoplay policy).
export function resumeAudio() {
  try {
    const audio = getCtx();
    if (audio && audio.state === 'suspended') audio.resume();
  } catch (_e) {
    /* no-op */
  }
}

// Schedule a simple oscillator voice with an attack/decay gain envelope.
function tone(audio, { type, freqStart, freqEnd, gain, attack, duration }) {
  try {
    const now = audio.currentTime;
    const osc = audio.createOscillator();
    const g = audio.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freqStart, now);
    if (freqEnd != null && freqEnd !== freqStart) {
      osc.frequency.exponentialRampToValueAtTime(Math.max(1, freqEnd), now + duration);
    }
    g.gain.setValueAtTime(0.0001, now);
    g.gain.exponentialRampToValueAtTime(gain, now + attack);
    g.gain.exponentialRampToValueAtTime(0.0001, now + duration);
    osc.connect(g).connect(audio.destination);
    osc.start(now);
    osc.stop(now + duration + 0.02);
  } catch (_e) {
    /* no-op */
  }
}

// Short, snappy laser blip — tight envelope so rapid fire never smears.
export function shoot() {
  const audio = ready();
  if (!audio) return;
  tone(audio, {
    type: 'square',
    freqStart: 990, // B5, in key with the A-minor bed
    freqEnd: 1480,
    gain: 0.05,
    attack: 0.003,
    duration: 0.07,
  });
}

// Layered impact: filtered noise burst + a short low sine thump for body.
export function explode() {
  const audio = ready();
  if (!audio) return;
  try {
    const now = audio.currentTime;

    // Noise burst (the "crack"), swept down for a debris tail.
    const buf = getNoiseBuffer(audio);
    if (buf) {
      const src = audio.createBufferSource();
      src.buffer = buf;
      const filter = audio.createBiquadFilter();
      filter.type = 'lowpass';
      filter.frequency.setValueAtTime(2200, now);
      filter.frequency.exponentialRampToValueAtTime(180, now + 0.28);
      const ng = audio.createGain();
      ng.gain.setValueAtTime(0.13, now);
      ng.gain.exponentialRampToValueAtTime(0.0001, now + 0.32);
      src.connect(filter).connect(ng).connect(audio.destination);
      src.start(now);
      src.stop(now + 0.36);
    }

    // Low sine thump (the "boom") — root A, gives the hit weight.
    const osc = audio.createOscillator();
    const tg = audio.createGain();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(A2, now);
    osc.frequency.exponentialRampToValueAtTime(55, now + 0.18);
    tg.gain.setValueAtTime(0.0001, now);
    tg.gain.exponentialRampToValueAtTime(0.13, now + 0.008);
    tg.gain.exponentialRampToValueAtTime(0.0001, now + 0.22);
    osc.connect(tg).connect(audio.destination);
    osc.start(now);
    osc.stop(now + 0.26);
  } catch (_e) {
    /* no-op */
  }
}

// Clean ascending three-note arpeggio (A–C–E) — a major-ish lift in key.
export function powerup() {
  const audio = ready();
  if (!audio) return;
  try {
    const start = audio.currentTime;
    const notes = [A4, C4 * 2, E4 * 2]; // A4, C5, E5
    notes.forEach((freq, i) => {
      const t = start + i * 0.07;
      const osc = audio.createOscillator();
      const g = audio.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, t);
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(0.07, t + 0.008);
      g.gain.exponentialRampToValueAtTime(0.0001, t + 0.16);
      osc.connect(g).connect(audio.destination);
      osc.start(t);
      osc.stop(t + 0.18);
    });
  } catch (_e) {
    /* no-op */
  }
}

// Low square-wave thud (player took damage) — punchy, fast decay.
export function hit() {
  const audio = ready();
  if (!audio) return;
  tone(audio, {
    type: 'square',
    freqStart: 165, // E3
    freqEnd: 55,
    gain: 0.12,
    attack: 0.004,
    duration: 0.18,
  });
}

// Descending tone (game over) — resolves down to the A-minor root.
export function gameOver() {
  const audio = ready();
  if (!audio) return;
  tone(audio, {
    type: 'sawtooth',
    freqStart: A4,
    freqEnd: A2 / 2, // down to A1
    gain: 0.1,
    attack: 0.01,
    duration: 0.7,
  });
}

// --- Background music ------------------------------------------------------

// Lazily create the dedicated low-level music master and apply the mute state.
function getMusicGain(audio) {
  if (musicGain) return musicGain;
  try {
    musicGain = audio.createGain();
    musicGain.gain.value = muted ? 0 : MUSIC_LEVEL;
    musicGain.connect(audio.destination);
  } catch (_e) {
    musicGain = null;
  }
  return musicGain;
}

// Reflect the current mute flag onto the music master (called from setMuted).
function applyMusicMute() {
  if (!musicGain || !ctx) return;
  try {
    const now = ctx.currentTime;
    musicGain.gain.cancelScheduledValues(now);
    musicGain.gain.setValueAtTime(muted ? 0 : MUSIC_LEVEL, now);
  } catch (_e) {
    /* no-op */
  }
}

// Schedule one plucked note routed through the music master.
function scheduleNote(audio, dest, { type, freq, time, dur, level }) {
  try {
    const osc = audio.createOscillator();
    const g = audio.createGain();
    osc.type = type;
    osc.frequency.setValueAtTime(freq, time);
    g.gain.setValueAtTime(0.0001, time);
    g.gain.exponentialRampToValueAtTime(level, time + 0.02);
    g.gain.exponentialRampToValueAtTime(0.0001, time + dur);
    osc.connect(g).connect(dest);
    osc.start(time);
    osc.stop(time + dur + 0.02);
  } catch (_e) {
    /* no-op */
  }
}

// Emit the voices that fall on the current 16th-note step.
function scheduleStep(audio, dest, step, time) {
  const bar = Math.floor(step / 16) % BASS_LINE.length;

  // Bassline: one sustained root note at the top of each bar.
  if (step % 16 === 0) {
    scheduleNote(audio, dest, {
      type: 'triangle',
      freq: BASS_LINE[bar],
      time,
      dur: SECONDS_PER_16TH * 14,
      level: 0.5,
    });
  }

  // Sparse arpeggio: a note every other 8th note (steps 0,4,8,12 …).
  if (step % 4 === 0) {
    const idx = (step / 4) % ARP_NOTES.length;
    scheduleNote(audio, dest, {
      type: 'sine',
      freq: ARP_NOTES[idx],
      time,
      dur: SECONDS_PER_16TH * 3,
      level: 0.32,
    });
  }
}

// Lookahead scheduler: schedule all steps that fall within SCHEDULE_AHEAD.
function musicScheduler() {
  const audio = getCtx();
  if (!audio || audio.state === 'suspended') return;
  const dest = getMusicGain(audio);
  if (!dest) return;
  while (nextNoteTime < audio.currentTime + SCHEDULE_AHEAD) {
    scheduleStep(audio, dest, musicStep, nextNoteTime);
    nextNoteTime += SECONDS_PER_16TH;
    musicStep += 1;
  }
}

// Start the procedural music bed. Idempotent; no-ops if audio is unavailable.
export function startMusic() {
  if (musicRunning) return;
  const audio = getCtx();
  if (!audio) return;
  if (!getMusicGain(audio)) return;
  musicRunning = true;
  musicStep = 0;
  nextNoteTime = audio.currentTime + 0.1;
  try {
    musicScheduler();
    musicTimer = setInterval(musicScheduler, LOOKAHEAD_MS);
  } catch (_e) {
    musicRunning = false;
    musicTimer = null;
  }
}

// Stop the music bed and clear the scheduler timer.
export function stopMusic() {
  if (musicTimer != null) {
    clearInterval(musicTimer);
    musicTimer = null;
  }
  musicRunning = false;
  musicStep = 0;
}

// Set the muted flag and persist it to localStorage.
export function setMuted(value) {
  muted = !!value;
  try {
    localStorage.setItem(MUTE_KEY, muted ? 'true' : 'false');
  } catch (_e) {
    /* no-op */
  }
  applyMusicMute();
  return muted;
}

// Toggle mute and return the new state.
export function toggleMute() {
  return setMuted(!muted);
}
