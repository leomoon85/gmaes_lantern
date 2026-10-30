// Original Fareforge soundtrack "City Evening". No samples or external assets.
// Rebuild with: node tools/generate-music.cjs
const fs = require('node:fs');
const path = require('node:path');
const rate = 24000, beat = 60 / 96, duration = 64 * beat;
const samples = new Float64Array(Math.round(rate * duration));
const tau = Math.PI * 2;
const hz = midi => 440 * Math.pow(2, (midi - 69) / 12);
let seed = 9217;
const noise = () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 2147483648 - 1; };
function tone(start, length, midi, gain, instrument) {
  const n = Math.round(length * rate), offset = Math.round(start * rate), f = hz(midi);
  for (let i = 0; i < n; i++) {
    const t = i / rate;
    let value;
    if (instrument === 'pad') {
      const env = Math.min(1, t / .3) * Math.min(1, (length - t) / .65);
      value = env * (Math.sin(tau * f * t) + .22 * Math.sin(tau * f * 2 * t));
    } else {
      const env = (1 - Math.exp(-t * 90)) * Math.exp(-t * (instrument === 'bass' ? 3.4 : 2.5)) * Math.min(1, (length - t) / .08);
      value = env * (Math.sin(tau * f * t + (instrument === 'keys' ? .8 * Math.exp(-t * 5) * Math.sin(tau * f * 2 * t) : 0)) + .12 * Math.sin(tau * f * 2 * t));
    }
    samples[(offset + i) % samples.length] += value * gain;
  }
}
const chords = [[57, 60, 64, 67, 71], [53, 57, 60, 64, 67], [48, 55, 59, 62, 64], [55, 59, 62, 64, 69]];
const melody = [76, 0, 79, 74, 72, 0, 71, 74, 76, 0, 72, 69, 67, 0, 71, 74];
for (let bar = 0; bar < 16; bar++) {
  const start = bar * 4 * beat, chord = chords[Math.floor(bar / 2) % 4];
  if (bar % 2 === 0) chord.slice(1).forEach(note => tone(start, 8 * beat + .5, note, .024, 'pad'));
  for (let b = 0; b < 4; b++) {
    tone(start + b * beat, .5, chord[0] - 12, .13, 'bass');
    tone(start + (b + .5) * beat, 1.1, chord[1 + (b % 4)], .045, 'keys');
    const onset = Math.round((start + (b + .5) * beat) * rate);
    for (let i = 0; i < rate * .055; i++) samples[(onset + i) % samples.length] += noise() * Math.exp(-i / (rate * .011)) * .012;
  }
  for (let b = 0; b < 2; b++) {
    const note = melody[(bar * 2 + b) % melody.length];
    if (note) tone(start + (b * 2 + .25) * beat, 1.5, note, .055, 'keys');
  }
}
let peak = 0;
for (const value of samples) peak = Math.max(peak, Math.abs(value));
const wav = Buffer.alloc(44 + samples.length * 2);
wav.write('RIFF'); wav.writeUInt32LE(wav.length - 8, 4); wav.write('WAVEfmt ', 8);
wav.writeUInt32LE(16, 16); wav.writeUInt16LE(1, 20); wav.writeUInt16LE(1, 22);
wav.writeUInt32LE(rate, 24); wav.writeUInt32LE(rate * 2, 28); wav.writeUInt16LE(2, 32); wav.writeUInt16LE(16, 34);
wav.write('data', 36); wav.writeUInt32LE(samples.length * 2, 40);
for (let i = 0; i < samples.length; i++) wav.writeInt16LE(Math.round(samples[i] / peak * .78 * 32767), 44 + i * 2);
const target = path.join(__dirname, '../res/raw/city_evening.wav');
fs.mkdirSync(path.dirname(target), { recursive: true }); fs.writeFileSync(target, wav);
console.log(`City Evening: ${duration}s, ${rate}Hz mono PCM, ${wav.length} bytes`);
