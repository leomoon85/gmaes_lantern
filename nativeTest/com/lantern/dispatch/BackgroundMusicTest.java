package com.lantern.dispatch;

import static org.junit.Assert.*;

import android.content.*;
import android.media.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public class BackgroundMusicTest {
  Context context;
  BackgroundMusic music;
  ShadowAudioManager audio;

  @Before
  public void setup() {
    context = RuntimeEnvironment.getApplication();
    ShadowMediaPlayer.setMediaInfoProvider(source -> new ShadowMediaPlayer.MediaInfo(40000, 0));
    audio = Shadows.shadowOf((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
    music = new BackgroundMusic(context, true);
  }

  @After
  public void cleanup() {
    music.close();
  }

  private MediaPlayer player() {
    return ReflectionHelpers.getField(music, "player");
  }

  @Test
  public void loopMuteAndLifecyclePreservePlaybackPosition() {
    music.resume();
    MediaPlayer track = player();
    assertNotNull(track);
    assertTrue(track.isLooping());
    assertTrue(track.isPlaying());
    assertEquals(R.raw.city_evening, Shadows.shadowOf(track).getSourceResId());
    Shadows.shadowOf(track).setCurrentPosition(9000);
    music.setEnabled(false);
    assertFalse(track.isPlaying());
    music.pause();
    music.resume();
    assertFalse(track.isPlaying());
    music.setEnabled(true);
    assertSame(track, player());
    assertTrue(track.isPlaying());
    assertEquals(9000, track.getCurrentPosition());
    music.pause();
    assertFalse(track.isPlaying());
    music.resume();
    assertTrue(track.isPlaying());
    music.close();
    assertEquals(ShadowMediaPlayer.State.END, Shadows.shadowOf(track).getState());
  }

  @Test
  public void focusAndHeadphoneInterruptionsDoNotOverrideMute() {
    music.resume();
    MediaPlayer track = player();
    AudioManager.OnAudioFocusChangeListener listener =
        change ->
            ReflectionHelpers.callInstanceMethod(
                music, "focusChanged", ReflectionHelpers.ClassParameter.from(int.class, change));
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
    assertFalse(track.isPlaying());
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    assertTrue(track.isPlaying());
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
    assertEquals(.08f, Shadows.shadowOf(track).getLeftVolume(), .001f);
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    assertEquals(.35f, Shadows.shadowOf(track).getLeftVolume(), .001f);
    context.sendBroadcast(new Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertFalse(track.isPlaying());
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    assertFalse(track.isPlaying());
    music.setEnabled(false);
    listener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
    assertFalse(track.isPlaying());
  }

  @Test
  public void deniedFocusLeavesOtherAudioUndisturbed() {
    audio.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED);
    music.resume();
    assertNull(player());
  }
}
