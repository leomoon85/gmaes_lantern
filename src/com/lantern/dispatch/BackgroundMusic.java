package com.lantern.dispatch;

import android.content.*;
import android.media.*;
import android.os.*;
import java.util.concurrent.*;

/**
 * Local soundtrack and dynamic procedural audio cue synthesizer, scoped to the visible activity
 * and Android audio focus.
 */
final class BackgroundMusic implements AutoCloseable {
  static final int CUE_NONE = 0,
      CUE_CASH = 1,
      CUE_INCIDENT = 2,
      CUE_FANFARE = 3,
      CUE_NEWS = 4,
      CUE_DISPATCH = 5;

  private final Context context;
  private final AudioManager audio;
  private final AudioAttributes attributes;
  private final AudioFocusRequest focus;
  private final ExecutorService synthPool =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "FareforgeAudioSynth");
            t.setDaemon(true);
            return t;
          });
  private MediaPlayer player;
  private boolean enabled,
      sfxEnabled = true,
      foreground,
      requested,
      focused,
      closed,
      listening,
      interrupted;
  private float baseVolume = 0.35f;

  private final BroadcastReceiver unplugged =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
            interrupted = true;
            stopPlayback();
          }
        }
      };

  BackgroundMusic(Context context, boolean enabled) {
    this.context = context.getApplicationContext();
    this.enabled = enabled;
    audio = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    attributes =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
    focus =
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(this::focusChanged, new Handler(Looper.getMainLooper()))
            .build();
  }

  void setEnabled(boolean value) {
    enabled = value;
    interrupted = false;
    if (value) playIfAllowed();
    else stopPlayback();
  }

  void setSfxEnabled(boolean value) {
    sfxEnabled = value;
  }

  /** Modulate background music warmth & level dynamically based on traffic rush and game speed. */
  void updateAtmosphere(boolean rushHour, int speed, boolean urgentIncident) {
    if (closed || player == null || !focused) return;
    try {
      float targetVol = speed == 0 ? 0.18f : (rushHour ? 0.44f : 0.34f);
      if (urgentIncident) targetVol = 0.22f; // Duck slightly so incident cue stands out
      baseVolume = targetVol;
      player.setVolume(baseVolume, baseVolume);
      if (Build.VERSION.SDK_INT >= 23 && player.isPlaying()) {
        float pitch = urgentIncident ? 0.96f : (rushHour ? 1.04f : 1.0f);
        float rate = speed >= 4 ? 1.08f : (speed == 2 ? 1.03f : 1.0f);
        PlaybackParams params = new PlaybackParams();
        params.setPitch(pitch);
        params.setSpeed(rate);
        player.setPlaybackParams(params);
      }
    } catch (RuntimeException ignored) {
    }
  }

  /** Play a distinct musical motif for incidents, cash transactions, news, or milestones. */
  void playCue(int cueType) {
    if (closed || !foreground || !sfxEnabled || cueType == CUE_NONE) return;
    synthPool.execute(() -> synthesizeCue(cueType));
  }

  private void synthesizeCue(int cueType) {
    try {
      final int sampleRate = 24000;
      double[] freqs;
      double[] durations;
      double gain;
      switch (cueType) {
        case CUE_INCIDENT:
          // Urgent minor staccato alert motif (G4 -> Eb4 -> C4)
          freqs = new double[] {392.0, 311.13, 261.63};
          durations = new double[] {0.11, 0.11, 0.22};
          gain = 0.34;
          break;
        case CUE_FANFARE:
          // Triumphant major brass arpeggio (C5 -> E5 -> G5 -> C6)
          freqs = new double[] {523.25, 659.25, 783.99, 1046.50};
          durations = new double[] {0.09, 0.09, 0.10, 0.26};
          gain = 0.32;
          break;
        case CUE_NEWS:
          // Classic telegraph / press bulletin chime (A4 -> E5 -> A5)
          freqs = new double[] {440.0, 659.25, 880.0};
          durations = new double[] {0.07, 0.07, 0.16};
          gain = 0.28;
          break;
        case CUE_DISPATCH:
          // Crisp departure bell (D5 -> A5)
          freqs = new double[] {587.33, 880.0};
          durations = new double[] {0.05, 0.09};
          gain = 0.22;
          break;
        case CUE_CASH:
        default:
          // Mechanical brass register chime (E5 -> B5)
          freqs = new double[] {659.25, 987.77};
          durations = new double[] {0.06, 0.12};
          gain = 0.25;
          break;
      }

      int totalSamples = 0;
      for (double d : durations) totalSamples += (int) (d * sampleRate);
      short[] pcm = new short[totalSamples];
      int offset = 0;
      for (int n = 0; n < freqs.length; n++) {
        int len = (int) (durations[n] * sampleRate);
        double f = freqs[n];
        for (int i = 0; i < len && (offset + i) < totalSamples; i++) {
          double t = i / (double) sampleRate;
          double env = Math.min(1.0, i / (sampleRate * 0.008)) * Math.exp(-t * 9.5);
          // Warm harmonic blend (fundamental + octave + fifth overtone)
          double wave =
              0.68 * Math.sin(2.0 * Math.PI * f * t)
                  + 0.22 * Math.sin(4.0 * Math.PI * f * t)
                  + 0.10 * Math.sin(6.0 * Math.PI * f * t);
          pcm[offset + i] = (short) (wave * env * gain * Short.MAX_VALUE);
        }
        offset += len;
      }

      AudioAttributes sfxAttr =
          new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build();
      AudioFormat format =
          new AudioFormat.Builder()
              .setSampleRate(sampleRate)
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build();
      AudioTrack track =
          new AudioTrack(
              sfxAttr,
              format,
              pcm.length * 2,
              AudioTrack.MODE_STATIC,
              AudioManager.AUDIO_SESSION_ID_GENERATE);
      track.write(pcm, 0, pcm.length);
      track.play();
      Thread.sleep((long) (totalSamples * 1000L / sampleRate) + 40L);
      track.stop();
      track.release();
    } catch ( Throwable ignored) {
    }
  }

  void resume() {
    if (closed) return;
    foreground = true;
    interrupted = false;
    if (!listening) {
      IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
      if (Build.VERSION.SDK_INT >= 33)
        context.registerReceiver(unplugged, filter, Context.RECEIVER_NOT_EXPORTED);
      else context.registerReceiver(unplugged, filter);
      listening = true;
    }
    playIfAllowed();
  }

  void pause() {
    foreground = false;
    stopPlayback();
    if (listening) {
      context.unregisterReceiver(unplugged);
      listening = false;
    }
  }

  private void playIfAllowed() {
    if (closed || !foreground || !enabled || interrupted || audio == null) return;
    try {
      if (!requested) {
        focused = audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        requested = focused;
      }
      if (!focused) return;
      if (player == null) {
        player = MediaPlayer.create(context, R.raw.city_evening, attributes, 0);
        if (player == null) {
          stopPlayback();
          return;
        }
        player.setLooping(true);
        player.setVolume(baseVolume, baseVolume);
        player.setOnErrorListener(
            (failed, what, extra) -> {
              releasePlayer();
              stopPlayback();
              return true;
            });
      }
      if (!player.isPlaying()) player.start();
    } catch (RuntimeException error) {
      releasePlayer();
      stopPlayback();
    }
  }

  private void focusChanged(int change) {
    if (closed) return;
    if (change == AudioManager.AUDIOFOCUS_GAIN) {
      focused = true;
      if (player != null) player.setVolume(baseVolume, baseVolume);
      playIfAllowed();
    } else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
      if (player != null) player.setVolume(.08f, .08f);
    } else {
      focused = false;
      pausePlayer();
      if (change == AudioManager.AUDIOFOCUS_LOSS) stopPlayback();
    }
  }

  private void pausePlayer() {
    if (player != null && player.isPlaying()) player.pause();
  }

  private void stopPlayback() {
    pausePlayer();
    if (requested && audio != null) audio.abandonAudioFocusRequest(focus);
    requested = focused = false;
  }

  private void releasePlayer() {
    if (player != null) {
      player.release();
      player = null;
    }
  }

  @Override
  public void close() {
    pause();
    closed = true;
    synthPool.shutdownNow();
    releasePlayer();
  }
}
