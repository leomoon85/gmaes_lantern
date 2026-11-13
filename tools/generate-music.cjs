// Original Fareforge 6-track Tycoon Radio soundtrack. No samples or external assets.
// Rebuild with: node tools/generate-music.cjs
const fs = require('node:fs');
const path = require('node:path');

const rate = 24000;
const tau = Math.PI * 2;
const hz = midi => 440 * Math.pow(2, (midi - 69) / 12);

function makeRng(init) {
  let seed = init;
  return () => {
    seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0;
    return seed / 2147483648 - 1;
  };
}

function createTrack(bpm, beatsCount) {
  const beat = 60 / bpm;
  const duration = beatsCount * beat;
  const samples = new Float64Array(Math.round(rate * duration));
  const noise = makeRng(Math.round(bpm * 137 + beatsCount));

  function tone(start, length, midi, gain, instrument) {
    const n = Math.round(length * rate);
    const offset = Math.round(start * rate);
    const f = hz(midi);
    for (let i = 0; i < n; i++) {
      const t = i / rate;
      let value = 0;
      if (instrument === 'pad') {
        const env = Math.min(1, t / 0.28) * Math.min(1, (length - t) / 0.55);
        value = env * (Math.sin(tau * f * t) + 0.24 * Math.sin(tau * f * 2 * t));
      } else if (instrument === 'brass') {
        const env = (1 - Math.exp(-t * 45)) * Math.exp(-t * 1.9) * Math.min(1, (length - t) / 0.06);
        value = env * (
          0.62 * Math.sin(tau * f * t) +
          0.28 * Math.sin(tau * f * 2 * t) +
          0.16 * Math.sin(tau * f * 3 * t)
        );
      } else if (instrument === 'marimba' || instrument === 'rag') {
        const env = (1 - Math.exp(-t * 140)) * Math.exp(-t * 4.5) * Math.min(1, (length - t) / 0.04);
        value = env * (Math.sin(tau * f * t) + 0.35 * Math.sin(tau * f * 4 * t) * Math.exp(-t * 14));
      } else if (instrument === 'synth') {
        const env = (1 - Math.exp(-t * 95)) * Math.exp(-t * 3.1) * Math.min(1, (length - t) / 0.05);
        const mod = 0.9 * Math.exp(-t * 4) * Math.sin(tau * f * 2 * t);
        value = env * (Math.sin(tau * f * t + mod) + 0.2 * Math.sin(tau * f * 3 * t));
      } else if (instrument === 'flute') {
        const env = Math.min(1, t / 0.08) * Math.min(1, (length - t) / 0.12);
        const vib = 1 + 0.004 * Math.sin(tau * 5.2 * t);
        value = env * (Math.sin(tau * f * vib * t) + 0.12 * Math.sin(tau * f * 2 * vib * t));
      } else {
        const decay = instrument === 'bass' ? 3.4 : 2.5;
        const env = (1 - Math.exp(-t * 90)) * Math.exp(-t * decay) * Math.min(1, (length - t) / 0.07);
        const fm = instrument === 'keys' ? 0.8 * Math.exp(-t * 5) * Math.sin(tau * f * 2 * t) : 0;
        value = env * (Math.sin(tau * f * t + fm) + 0.14 * Math.sin(tau * f * 2 * t));
      }
      samples[(offset + i) % samples.length] += value * gain;
    }
  }

  function perc(start, kind, gain) {
    const offset = Math.round(start * rate);
    if (kind === 'kick') {
      const n = Math.round(rate * 0.12);
      for (let i = 0; i < n; i++) {
        const t = i / rate;
        const freq = 48 + 90 * Math.exp(-t * 28);
        samples[(offset + i) % samples.length] += Math.sin(tau * freq * t) * Math.exp(-t * 18) * gain;
      }
    } else if (kind === 'shaker' || kind === 'hat') {
      const len = kind === 'shaker' ? 0.045 : 0.055;
      const decay = kind === 'shaker' ? 0.009 : 0.011;
      for (let i = 0; i < rate * len; i++) {
        samples[(offset + i) % samples.length] += noise() * Math.exp(-i / (rate * decay)) * gain;
      }
    } else if (kind === 'clave') {
      const n = Math.round(rate * 0.04);
      for (let i = 0; i < n; i++) {
        const t = i / rate;
        samples[(offset + i) % samples.length] += Math.sin(tau * 1850 * t) * Math.exp(-t * 48) * gain;
      }
    }
  }

  function writeWav(fileName) {
    let peak = 0;
    for (const value of samples) peak = Math.max(peak, Math.abs(value));
    const safePeak = peak || 1;
    const wav = Buffer.alloc(44 + samples.length * 2);
    wav.write('RIFF');
    wav.writeUInt32LE(wav.length - 8, 4);
    wav.write('WAVEfmt ', 8);
    wav.writeUInt32LE(16, 16);
    wav.writeUInt16LE(1, 20);
    wav.writeUInt16LE(1, 22);
    wav.writeUInt32LE(rate, 24);
    wav.writeUInt32LE(rate * 2, 28);
    wav.writeUInt16LE(2, 32);
    wav.writeUInt16LE(16, 34);
    wav.write('data', 36);
    wav.writeUInt32LE(samples.length * 2, 40);
    for (let i = 0; i < samples.length; i++) {
      wav.writeInt16LE(Math.round((samples[i] / safePeak) * 0.78 * 32767), 44 + i * 2);
    }
    const target = path.join(__dirname, '../res/raw/', fileName);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, wav);
    console.log(`${fileName}: ${duration.toFixed(1)}s @ ${bpm} BPM, ${wav.length} bytes`);
  }

  return { beat, tone, perc, writeWav };
}

// 1. City Evening (SimCity 4 style smooth Rhodes jazz-lounge)
{
  const { beat, tone, perc, writeWav } = createTrack(96, 64);
  const chords = [[57, 60, 64, 67, 71], [53, 57, 60, 64, 67], [48, 55, 59, 62, 64], [55, 59, 62, 64, 69]];
  const melody = [76, 0, 79, 74, 72, 0, 71, 74, 76, 0, 72, 69, 67, 0, 71, 74];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const chord = chords[Math.floor(bar / 2) % 4];
    if (bar % 2 === 0) chord.slice(1).forEach(note => tone(start, 8 * beat + 0.5, note, 0.024, 'pad'));
    for (let b = 0; b < 4; b++) {
      tone(start + b * beat, 0.5, chord[0] - 12, 0.13, 'bass');
      tone(start + (b + 0.5) * beat, 1.1, chord[1 + (b % 4)], 0.045, 'keys');
      perc(start + (b + 0.5) * beat, 'hat', 0.012);
    }
    for (let b = 0; b < 2; b++) {
      const note = melody[(bar * 2 + b) % melody.length];
      if (note) tone(start + (b * 2 + 0.25) * beat, 1.5, note, 0.055, 'keys');
    }
  }
  writeWav('city_evening.wav');
}

// 2. Rush Hour Express (Transport Tycoon Deluxe style upbeat jazz-funk groove)
{
  const { beat, tone, perc, writeWav } = createTrack(124, 64);
  const roots = [41, 41, 46, 48, 44, 44, 43, 48]; // Fm7 - Bb9 - Abmaj7 - Gm/C7
  const stabs = [[60, 65, 68, 72], [62, 65, 68, 72], [60, 63, 67, 70], [59, 62, 67, 70]];
  const lead = [77, 80, 82, 84, 82, 80, 79, 77, 75, 77, 79, 80, 77, 75, 72, 75];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const root = roots[bar % roots.length];
    const stab = stabs[Math.floor(bar / 2) % 4];
    for (let s = 0; s < 8; s++) {
      const t = start + s * 0.5 * beat;
      if (s % 2 === 0) perc(t, 'kick', 0.11);
      else perc(t, 'hat', 0.018);
      if (s === 0 || s === 3 || s === 6) tone(t, 0.32, root + (s === 6 ? 7 : 0), 0.14, 'bass');
      if (s === 1 || s === 4 || s === 7) stab.forEach(n => tone(t, 0.22, n, 0.032, 'keys'));
    }
    const mel1 = lead[(bar * 2) % lead.length];
    const mel2 = lead[(bar * 2 + 1) % lead.length];
    tone(start + 0.25 * beat, 1.1 * beat, mel1, 0.068, 'brass');
    tone(start + 2.5 * beat, 1.2 * beat, mel2, 0.065, 'brass');
  }
  writeWav('rush_hour_express.wav');
}

// 3. RailBaron Stride (Railroad Tycoon II style cheerful ragtime/stride parlor piano)
{
  const { beat, tone, perc, writeWav } = createTrack(116, 64);
  const bassNotes = [48, 43, 41, 43, 48, 45, 50, 43]; // C - G - F - G - C - Am - D7 - G7
  const chords = [[64, 67, 72], [62, 67, 71], [65, 69, 72], [62, 67, 71]];
  const ragMel = [76, 79, 76, 72, 74, 77, 76, 72, 79, 81, 79, 76, 74, 72, 71, 67];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const bn = bassNotes[bar % bassNotes.length];
    const ch = chords[bar % chords.length];
    for (let b = 0; b < 4; b++) {
      if (b % 2 === 0) {
        tone(start + b * beat, 0.42 * beat, bn + (b === 2 ? 7 : 0), 0.13, 'bass');
      } else {
        ch.forEach(n => tone(start + b * beat, 0.35 * beat, n, 0.036, 'rag'));
      }
      const m1 = ragMel[(bar * 4 + b) % ragMel.length];
      tone(start + (b + 0.5) * beat, 0.55 * beat, m1, 0.062, 'rag');
      perc(start + b * beat, 'shaker', 0.01);
    }
  }
  writeWav('boardroom_rag.wav');
}

// 4. Harbor Bossa (Tropico style sunny marimba & bossa nova)
{
  const { beat, tone, perc, writeWav } = createTrack(108, 64);
  const chords = [[62, 65, 69, 72], [67, 71, 74, 77], [60, 64, 67, 71], [57, 60, 64, 67]]; // Dm7 G7 Cmaj7 Am7
  const fluteLine = [76, 74, 72, 74, 76, 79, 77, 76, 74, 72, 69, 72, 76, 74, 72, 0];
  const claveSteps = [0, 1.5, 3, 5, 6.5];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const ch = chords[Math.floor(bar / 2) % 4];
    tone(start, 1.4 * beat, ch[0] - 24, 0.13, 'bass');
    tone(start + 1.5 * beat, 1.1 * beat, ch[0] - 17, 0.11, 'bass');
    tone(start + 2.0 * beat, 1.3 * beat, ch[0] - 24, 0.13, 'bass');
    for (const st of claveSteps) {
      perc(start + (st * 0.5) * beat, 'clave', 0.045);
    }
    for (let s = 0; s < 8; s++) {
      const t = start + s * 0.5 * beat;
      perc(t, 'shaker', 0.014);
      if (s === 0 || s === 3 || s === 6) ch.forEach(n => tone(t, 0.45 * beat, n, 0.028, 'marimba'));
    }
    const fNote = fluteLine[bar % fluteLine.length];
    if (fNote) tone(start + 0.5 * beat, 2.4 * beat, fNote, 0.055, 'flute');
  }
  writeWav('harbor_bossa.wav');
}

// 5. Midnight Maglev (Mini Metro style crisp neon synth pulse)
{
  const { beat, tone, perc, writeWav } = createTrack(118, 64);
  const arps = [
    [52, 59, 64, 67], // Em9
    [48, 55, 60, 64], // Cmaj7
    [50, 57, 62, 66], // D6
    [47, 54, 59, 62]  // Bm7
  ];
  const bell = [76, 0, 79, 78, 76, 0, 74, 71, 72, 0, 76, 74, 71, 0, 74, 76];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const arp = arps[Math.floor(bar / 2) % 4];
    tone(start, 4 * beat + 0.2, arp[0], 0.025, 'pad');
    tone(start, 4 * beat + 0.2, arp[2], 0.022, 'pad');
    for (let s = 0; s < 8; s++) {
      const t = start + s * 0.5 * beat;
      tone(t, 0.35 * beat, arp[s % 4] - 12, 0.09, 'bass');
      if (s % 2 === 1) perc(t, 'hat', 0.012);
    }
    const bn = bell[bar % bell.length];
    if (bn) tone(start + 0.5 * beat, 2.1 * beat, bn, 0.055, 'synth');
  }
  writeWav('midnight_maglev.wav');
}

// 6. Exchange Floor (Wall Street Swing — walking upright bass & vibes)
{
  const { beat, tone, perc, writeWav } = createTrack(104, 64);
  const walk = [
    [48, 52, 55, 57], // C6 walk
    [53, 57, 60, 58], // F7 walk
    [50, 53, 57, 55], // Dm7 walk
    [43, 47, 50, 53]  // G7 walk
  ];
  const comp = [[64, 67, 70, 74], [63, 67, 69, 72], [65, 69, 72, 76], [62, 65, 71, 74]];
  const vibe = [79, 76, 74, 72, 74, 77, 76, 74, 72, 76, 79, 81, 79, 74, 76, 72];
  for (let bar = 0; bar < 16; bar++) {
    const start = bar * 4 * beat;
    const w = walk[bar % 4];
    const ch = comp[bar % 4];
    for (let b = 0; b < 4; b++) {
      tone(start + b * beat, 0.85 * beat, w[b], 0.14, 'bass');
      perc(start + b * beat, 'hat', 0.015);
      if (b === 1 || b === 3) ch.forEach(n => tone(start + (b + 0.66) * beat, 0.6 * beat, n, 0.032, 'keys'));
    }
    const vn = vibe[bar % vibe.length];
    tone(start + 0.33 * beat, 1.8 * beat, vn, 0.058, 'marimba');
  }
  writeWav('exchange_blues.wav');
}
