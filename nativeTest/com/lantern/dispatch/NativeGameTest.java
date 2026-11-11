package com.lantern.dispatch;

import static org.junit.Assert.*;

import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowChoreographer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w412dp-h915dp")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeGameTest {
  ActivityController<MainActivity> controller;
  MainActivity activity;

  @Before
  public void start() {
    org.robolectric.shadows.ShadowMediaPlayer.setMediaInfoProvider(
        source -> new org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(40000, 0));
    ShadowChoreographer.setPaused(true);
    controller = Robolectric.buildActivity(MainActivity.class).setup();
    activity = controller.get();
  }

  @After
  public void stop() {
    if (controller != null) controller.pause().stop().destroy();
  }

  private Button find(View view, String prefix) {
    if (view instanceof Button && ((Button) view).getText().toString().startsWith(prefix))
      return (Button) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        Button result = find(group.getChildAt(i), prefix);
        if (result != null) return result;
      }
    }
    return null;
  }

  private Button dialogButton(String prefix) {
    AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
    Button button = find(dialog.getWindow().getDecorView(), prefix);
    assertNotNull(prefix, button);
    return button;
  }

  private void tutorial() {
    dialogButton("Learn to play").performClick();
    AlertDialog latest = ShadowAlertDialog.getLatestAlertDialog();
    if (latest.isShowing()
        && latest.getButton(AlertDialog.BUTTON_POSITIVE) != null
        && latest.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString().equals("Continue"))
      latest.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
  }

  @Test
  public void nativeNavigationAndOfflineCanvas() throws Exception {
    screenshot("native-home", 412, 915);
    tutorial();
    assertEquals(Game.MODE_TUTORIAL, activity.game.gameMode);
    activity.selectTab(0);
    assertFalse(containsBrowser(activity.getWindow().getDecorView()));
    screenshot("native-city", 412, 915);
    for (int tab = 1; tab <= 5; tab++) {
      activity.selectTab(tab);
      screenshot("native-tab-" + tab, 412, 915);
    }
    activity.selectTab(0);
    screenshot("native-landscape", 915, 412);
  }

  @Test
  public void newspaperPausesAndUsesRealArchive() throws Exception {
    tutorial();
    activity.game.speed = 4;
    activity.game.elapsed = 8;
    activity.game.seenNews.add("opening");
    Game.Report report =
        new Game.Report(
            "test",
            "2013-10-02",
            "Historical headline",
            "An attributed event.",
            "Game interpretation.",
            "https://www.uber.com/",
            "Source",
            "Historical record");
    activity.showReport(report);
    double before = activity.game.elapsed;
    activity.doFrame(1_000_000_000L);
    activity.doFrame(1_100_000_000L);
    assertEquals(before, activity.game.elapsed, 0);
    screenshot("native-newspaper", 412, 915);
    dialogButton("Back to business").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals(4, activity.game.speed);
    assertFalse("Newspaper dismissed", activity.modalOpen());
    assertEquals(
        "Game has left menu",
        false,
        (boolean) org.robolectric.util.ReflectionHelpers.getField(activity, "home"));
    activity.doFrame(2_000_000_000L);
    activity.doFrame(2_100_000_000L);
    assertTrue("Clock resumes after closing the newspaper", activity.game.elapsed > before);
  }

  @Test
  public void historicalDatesAndRegionalSources() throws Exception {
    Game game = new Game(0);
    NativeNews archive = new NativeNews(activity);
    game.elapsed = 2 * 60 + 11.0 / 12 * 60;
    archive.collect(game);
    assertTrue(game.seenNews.contains("real-blr-uber"));
    assertFalse(game.seenNews.contains("real-tokyo-go"));
    assertFalse(game.seenNews.contains("real-uber-ipo"));
    game.elapsed = 8 * 60 + 11.0 / 12 * 60;
    archive.collect(game);
    assertTrue(game.seenNews.contains("real-uber-ipo"));
    int count = game.reports.size();
    archive.collect(game);
    assertEquals(count, game.reports.size());
    assertEquals("2019", NativeNews.date(game).substring(0, 4));
  }

  @Test
  public void atomicStorePreservesActiveGame() throws Exception {
    SaveStore store = new SaveStore(activity);
    Game game = new Game(4);
    game.dispatch(game.requests.get(0).id);
    game.tick(.2);
    store.write(SaveCodec.encode(game));
    Game restored = store.load();
    assertEquals(4, restored.scenarioId);
    assertEquals(game.taxis.get(0).status, restored.taxis.get(0).status);
    assertArrayEquals(game.taxis.get(0).path, restored.taxis.get(0).path);
  }

  @Test
  public void rotationSnapshotRetainsCampaignAndScreen() {
    tutorial();
    activity.game.cash = 87654;
    activity.game.chapter = 2;
    activity.game.reports.add(
        new Game.Report(
            "old-brand",
            "2013-10-02",
            "Lantern report",
            "Lantern earned a fare.",
            "Lantern ledger",
            "",
            "Lantern desk",
            "Your campaign"));
    activity.selectTab(1);
    android.os.Bundle snapshot = new android.os.Bundle();
    controller.saveInstanceState(snapshot).pause().stop().destroy();
    controller =
        Robolectric.buildActivity(MainActivity.class).create(snapshot).start().resume().visible();
    activity = controller.get();
    assertEquals(87654, activity.game.cash);
    assertEquals(2, activity.game.chapter);
    assertEquals("Fareforge report", activity.game.reports.get(0).title);
    assertNotNull(find(activity.getWindow().getDecorView(), "Buy vehicle"));
  }

  @Test
  @Config(qualifiers = "w915dp-h412dp-land")
  public void landscapeControlsRemainUsable() throws Exception {
    tutorial();
    activity.selectTab(0);
    screenshot("native-landscape", 915, 412);
    assertNotNull(find(activity.getWindow().getDecorView(), "Goals"));
    activity.selectTab(4);
    screenshot("native-landscape-market", 915, 412);
  }

  @Test
  public void musicTogglePersistsIndependentlyOfButtonSounds() {
    dialogButton("Music on").performClick();
    assertNotNull(dialogButton("Music off"));
    assertFalse(activity.getPreferences(0).getBoolean("music", true));
    assertNotNull(dialogButton("Button sounds on"));
    android.os.Bundle snapshot = new android.os.Bundle();
    controller.saveInstanceState(snapshot).pause().stop().destroy();
    controller =
        Robolectric.buildActivity(MainActivity.class).create(snapshot).start().resume().visible();
    activity = controller.get();
    assertNotNull(dialogButton("Music off"));
    assertNotNull(dialogButton("Button sounds on"));
  }

  @Test
  public void realCityAnchorsMatchHubCoordinates() {
    for (int city = 0; city < 6; city++) {
      Game g = new Game(city);
      for (int i = 0; i < 6; i++) {
        double[] point = CityGeography.hub(city, i);
        double[] mapped =
            CityGeography.position(city, g.districts.get(i).cell(), g.districts.get(i).cell(), 0);
        assertArrayEquals(point, mapped, .000001);
      }
    }
  }

  @Test
  public void mapControlsKeepOfflineGamePlayable() {
    tutorial();
    activity.map.toggleStreets();
    assertFalse(activity.map.isUsingStreets());
    assertTrue(activity.map.status().contains("Offline"));
    activity.game.autoDispatch = true;
    for (int i = 0; i < 120; i++) activity.game.tick(.2);
    assertTrue(activity.game.rides > 0);
  }

  @Test
  public void cancellingStartNewCompanyRestoresHomeMenu() throws Exception {
    SaveStore store = new SaveStore(activity);
    store.write(SaveCodec.encode(new Game(0)));
    dialogButton("Learn to play").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    AlertDialog confirm = ShadowAlertDialog.getLatestAlertDialog();
    assertNotNull(confirm);
    assertTrue(confirm.isShowing());
    confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertTrue("Home menu re-opens after cancelling new company", activity.modalOpen());
    assertNotNull(dialogButton("Learn to play"));
  }

  @Test
  public void entrepreneurMultiCompanyCitySwitchAndAiCompetition() throws Exception {
    Game g = new Game(0);
    g.configureFounderCompany("Rajesh", "Deccan Mobility", 60, 0, 2);
    assertEquals("Rajesh", g.founderName);
    assertEquals("Deccan Mobility", g.companyName);
    assertEquals(60, g.founderEquityPct);
    assertTrue(g.districts.get(0).open);
    assertTrue(g.districts.get(2).open);
    assertEquals(Game.CUE_FANFARE, g.pollSoundCue());

    // Open a 3rd hub and complete 20 rides so we can expand to London (city 1)
    g.cash = 50000;
    g.unlock(1);
    g.rides = 25;
    assertTrue(g.expand(1));
    assertTrue(g.branches[1]);

    // Switch active management from Bengaluru (0) to London (1) and back
    assertTrue(g.switchCity(1));
    assertEquals(1, g.scenarioId);
    assertEquals("Westminster", g.districts.get(0).name);
    g.unlock(2); // Open Heathrow in London
    assertTrue(g.switchCity(0));
    assertEquals(0, g.scenarioId);
    assertEquals("K.R. Market", g.districts.get(0).name);
    assertTrue(g.switchCity(1));
    assertTrue("London hub state preserved across city switches", g.districts.get(2).open);
    g.switchCity(0);

    // Found a 2nd company in Bengaluru (same city, 2 hubs) and a 3rd company in Tokyo (city 3)
    assertTrue(g.foundVenture("Namma Shuttle", 0, 3, 4, 80));
    assertTrue(g.foundVenture("Tokyo SkyLine", 3, 0, 1, 60));
    assertEquals(3, g.ownedCompanyCount());
    assertTrue(g.branches[3]);

    // Verify 1 Person = 1 Company CEO rule & Entrepreneur vs. Company ledger separation
    assertTrue(g.isFounderFlagshipCeo());
    assertEquals("Rajesh", g.flagshipCeoName());
    assertTrue(g.assignFounderAsCeo(0));
    assertFalse(g.isFounderFlagshipCeo());
    assertEquals("Namma Shuttle", g.activeFounderCeoCompanyName());
    assertEquals("Rajesh", g.ventures.get(0).ceoName);

    long founderCashBeforeDiv = g.founderCash;
    assertTrue(g.declareCorporateDividend(1000L));
    assertEquals(founderCashBeforeDiv + 600L, g.founderCash);
    assertTrue(g.buybackFounderEquity(false));
    assertEquals(70, g.founderEquityPct);
    assertTrue(g.companyNetWorth() > 0);
    assertTrue(g.entrepreneurNetWorth() > 0);

    // Call and pass a shareholder vote to sell Namma Shuttle (returns Rajesh to Flagship CEO)
    long cashBeforeSale = g.cash;
    assertTrue(g.callVentureVote(0, true));
    assertTrue(g.ventures.get(0).pendingVote);
    assertTrue(g.resolveVentureVote(0, true));
    assertTrue(g.ventures.get(0).sold);
    assertTrue(g.cash > cashBeforeSale);
    assertTrue("Founder returns to Flagship CEO when venture is sold", g.isFounderFlagshipCeo());

    // Verify SaveCodec roundtrip with ventures & multi-city state
    byte[] encoded = SaveCodec.encode(g);
    Game decoded = SaveCodec.decode(encoded);
    assertEquals("Deccan Mobility", decoded.companyName);
    assertEquals("Rajesh", decoded.founderName);
    assertEquals(2, decoded.ventures.size());
    assertEquals(g.founderCash, decoded.founderCash);

    // Verify multi-language switcher
    dialogButton("Language / Idioma / 言語").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertNotNull(dialogButton("Español"));
    dialogButton("Español").performClick();
    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    assertEquals("es", activity.getPreferences(0).getString("lang", "en"));
  }

  private boolean containsBrowser(View view) {
    if (view.getClass().getName().contains("WebView")) return true;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++)
        if (containsBrowser(group.getChildAt(i))) return true;
    }
    return false;
  }

  private void screenshot(String name, int width, int height) throws Exception {
    View view = activity.getWindow().getDecorView();
    AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
    if (dialog != null && dialog.isShowing()) view = dialog.getWindow().getDecorView();
    boolean dialogVisible = dialog != null && dialog.isShowing();
    view.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(
            height, dialogVisible ? View.MeasureSpec.AT_MOST : View.MeasureSpec.EXACTLY));
    height = view.getMeasuredHeight();
    view.layout(0, 0, width, height);
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    view.draw(new Canvas(bitmap));
    File folder = new File("build/native-previews");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
    }
    bitmap.recycle();
  }
}
