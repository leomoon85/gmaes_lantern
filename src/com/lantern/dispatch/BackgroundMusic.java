package com.lantern.dispatch;

import android.content.*;
import android.media.*;
import android.os.*;

/** Local soundtrack, scoped to the visible activity and Android audio focus. */
final class BackgroundMusic implements AutoCloseable {
  private final Context context;
  private final AudioManager audio;
  private final AudioAttributes attributes;
  private final AudioFocusRequest focus;
  private MediaPlayer player;
  private boolean enabled, foreground, requested, focused, closed, listening, interrupted;
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
        player.setVolume(.35f, .35f);
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
      if (player != null) player.setVolume(.35f, .35f);
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
    releasePlayer();
  }
}
