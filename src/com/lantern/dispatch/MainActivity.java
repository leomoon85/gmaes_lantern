package com.lantern.dispatch;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Native Android entry point: no browser runtime or HTML assets. */
public final class MainActivity extends Activity implements Choreographer.FrameCallback {
  static final int BG = 0xff0b1322,
      PANEL = 0xff142238,
      INK = 0xfff2eedc,
      MUTED = 0xff9cacc2,
      AMBER = 0xffedbb73,
      MINT = 0xff77d7c8;
  Game game;
  NativeNews news;
  private SaveStore saves;
  private BackgroundMusic soundtrack;
  private boolean music = true;
  private static final ExecutorService DISK =
      Executors.newSingleThreadExecutor(
          task -> {
            Thread thread = new Thread(task, "Fareforge saves");
            thread.setDaemon(true);
            return thread;
          });
  final Handler main = new Handler(Looper.getMainLooper());
  private final Runnable pausedFrameTick =
      () -> {
        if (this.foreground && !this.closed) Choreographer.getInstance().postFrameCallback(this);
      };
  private LinearLayout root, nav, management, dispatch, goalBox, goalBody, cityTickerRow;
  private TextView title, metrics, message, goalTitle, goalDetail, campaignGoalText;
  private Button speedButton, goalAction, goalCollapseButton;
  private String tickerKey = "";
  private boolean goalCollapsed = false;
  WorldMapView map;
  private FrameLayout worldPane;
  private boolean wideLayout;
  private TextView mapCaption;
  private Button autoButton, streetButton;
  private ProgressBar chapterProgress;
  private final HashMap<Integer, ProgressBar> requestMeters = new HashMap<>();
  private ScrollView managementScroll;
  private GamePanels panels;
  private boolean foreground = false, home = true, night = true, sound = true, closed = false;
  private int tab = 0;
  private long lastFrame = 0, lastPaint = 0, lastHud = 0, lastSave = 0;
  private double lastEdition = -100;
  private String requestKey = "";
  private AlertDialog currentDialog;
  private volatile String saveError;
  private Bundle savedMapState;
  private final String[] tabs = {"City", "Fleet", "Hubs", "Team", "Market", "News"};
  private final String[] chapters = {
    "Your first regulars",
    "Make room for growth",
    "A bigger neighbourhood",
    "Build a reputation",
    "Invest in the journey",
    "The city knows your name"
  };
  private final int[] targets = {5, 2, 3, 25, 1, 75},
      rewards = {1200, 1800, 2200, 3000, 2500, 5000},
      chapterTabs = {0, 1, 2, 3, 2, 0};
  private final String[] tasks = {
    "",
    "Tap a waiting trip to dispatch your vehicle.",
    "Complete one trip. Let your vehicle reach its destination.",
    "Buy a second vehicle in Fleet.",
    "Open a third station in Hubs.",
    "Upgrade one hub to level two.",
    "Hire a manager in Team.",
    "Buy a rival’s shares in Market.",
    "Academy complete. Your company is ready to grow."
  };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    savedMapState = state == null ? null : state.getBundle("streetMap");
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    night = getPreferences(0).getBoolean("night", true);
    sound = getPreferences(0).getBoolean("sound", true);
    music = getPreferences(0).getBoolean("music", sound);
    soundtrack = new BackgroundMusic(this, music);
    soundtrack.setSfxEnabled(sound);
    setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
    saves = new SaveStore(this);
    boolean restored = false;
    try {
      game =
          state != null && state.containsKey("nativeCampaign")
              ? SaveCodec.decode(state.getByteArray("nativeCampaign"))
              : saves.load();
      restored = true;
    } catch (FileNotFoundException ignored) {
      game = new Game();
    } catch (Exception e) {
      game = new Game();
      saveError = "The previous save could not be restored. It has been kept on this device.";
    }
    for (Game.Report report : game.reports) {
      report.title = report.title.replace("Lantern", "Fareforge");
      report.body = report.body.replace("Lantern", "Fareforge");
      report.impact = report.impact.replace("Lantern", "Fareforge");
      report.source = report.source.replace("Lantern", "Fareforge");
    }
    try {
      news = new NativeNews(this);
    } catch (Exception e) {
      saveError = "Historical archive could not load.";
    }
    panels = new GamePanels(this);
    buildLayout();
    updateHud(true);
    if (state != null && !state.getBoolean("home", true) && restored) {
      home = false;
      selectTab(state.getInt("tab", 0));
    } else showHome(restored);
    if (Build.VERSION.SDK_INT >= 33)
      getOnBackInvokedDispatcher()
          .registerOnBackInvokedCallback(
              android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> handleBack());
  }

  @Override
  public void onBackPressed() {
    handleBack();
  }

  private void handleBack() {
    if (modalOpen()) {
      if (currentDialog != null) currentDialog.cancel();
      return;
    }
    if (tab != 0) {
      selectTab(0);
      return;
    }
    if (home) {
      finish();
      return;
    }
    showHome(true);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (map != null) map.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
    soundtrack.resume();
    if (map != null) map.resume();
    foreground = true;
    main.removeCallbacks(pausedFrameTick);
    lastFrame = lastPaint = 0;
    Choreographer.getInstance().removeFrameCallback(this);
    Choreographer.getInstance().postFrameCallback(this);
  }

  @Override
  protected void onPause() {
    soundtrack.pause();
    if (map != null) map.pause();
    foreground = false;
    main.removeCallbacks(pausedFrameTick);
    Choreographer.getInstance().removeFrameCallback(this);
    saveAsync();
    super.onPause();
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    try {
      state.putByteArray("nativeCampaign", SaveCodec.encode(game));
      state.putBoolean("home", home);
      state.putInt("tab", tab);
      Bundle mapState = new Bundle();
      map.saveState(mapState);
      state.putBundle("streetMap", mapState);
    } catch (IOException error) {
      saveError = "Progress snapshot could not be created.";
    }
  }

  @Override
  protected void onStop() {
    if (map != null) map.stop();
    super.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    if (map != null) map.lowMemory();
  }

  @Override
  protected void onDestroy() {
    soundtrack.close();
    closed = true;
    Choreographer.getInstance().removeFrameCallback(this);
    main.removeCallbacksAndMessages(null);
    if (map != null) map.destroy();
    super.onDestroy();
  }

  @Override
  public void doFrame(long frameTime) {
    if (!foreground || closed) return;
    if (lastFrame != 0 && !home && !modalOpen() && tab == 0) {
      game.tick(Math.min(.1, (frameTime - lastFrame) / 1e9));
      int cue = game.pollSoundCue();
      if (cue != 0 && sound) soundtrack.playCue(cue);
    }
    lastFrame = frameTime;
    if (!home && !modalOpen() && game.speed > 0 && tab == 0 && frameTime - lastPaint > 33_000_000) {
      map.tick();
      lastPaint = frameTime;
    }
    if (frameTime - lastHud > 500_000_000) {
      updateHud(false);
      lastHud = frameTime;
    }
    if (!home && frameTime - lastSave > 10_000_000_000L) {
      saveAsync();
      lastSave = frameTime;
    }
    if (!home && !modalOpen() && tab == 0 && game.speed > 0)
      Choreographer.getInstance().postFrameCallback(this);
    else main.postDelayed(pausedFrameTick, 500);
  }

  int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    d.setStroke(dp(1), 0x45d8ab61);
    return d;
  }

  GradientDrawable medallionShape(int topColor, int bottomColor, int strokeColor, int radius) {
    GradientDrawable d =
        new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, new int[] {topColor, bottomColor});
    d.setCornerRadius(dp(radius));
    d.setStroke(Math.max(1, dp(1.4f)), strokeColor);
    return d;
  }

  LinearLayout vertical() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  TextView text(String value, int size, int color, boolean bold) {
    TextView v = new TextView(this);
    v.setText(value);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    v.setIncludeFontPadding(false);
    v.setLineSpacing(dp(3), 1);
    return v;
  }

  Button button(String value, Runnable action) {
    Button v = new Button(this);
    v.setText(value);
    v.setTextSize(12);
    v.setTextColor(INK);
    v.setAllCaps(false);
    v.setMinHeight(dp(48));
    v.setMinimumWidth(0);
    v.setPadding(dp(10), dp(6), dp(10), dp(6));
    v.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x30ffffff), shape(0xff1e314b, 12), null));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(6), 0, 0);
    v.setLayoutParams(p);
    v.setOnClickListener(view -> action.run());
    return v;
  }

  void primary(Button b) {
    b.setTextColor(BG);
    b.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x30ffffff), shape(AMBER, 12), null));
  }

  LinearLayout card(LinearLayout parent, String heading, String description) {
    LinearLayout c = vertical();
    c.setPadding(dp(16), dp(15), dp(16), dp(15));
    c.setBackground(shape(PANEL, 18));
    c.setElevation(dp(2));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(dp(10), dp(7), dp(10), dp(4));
    parent.addView(c, p);
    c.addView(text(heading, 17, INK, true));
    if (description != null && !description.isEmpty()) {
      TextView desc = text(description, 13, MUTED, false);
      LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(-1, -2);
      d.topMargin = dp(8);
      c.addView(desc, d);
    }
    return c;
  }

  private void buildLayout() {
    root = vertical();
    root.setBackgroundColor(BG);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets b =
                insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(b.left, b.top, b.right, b.bottom);
          } else
            v.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    root.requestApplyInsets();
    float density = getResources().getDisplayMetrics().density;
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(12), dp(8), dp(12), dp(6));
    header.setBackground(medallionShape(0xff162740, 0xff0d1829, 0x55edbb73, 0));
    ImageView mark = new ImageView(this);
    mark.setImageDrawable(BrandArt.crestDrawable(0xff14253b, AMBER, density));
    mark.setContentDescription("Fareforge");
    header.addView(mark, new LinearLayout.LayoutParams(dp(38), dp(38)));
    LinearLayout heading = vertical();
    heading.setPadding(dp(10), 0, 0, 0);
    TextView brand = text("FAREFORGE  ·  TYCOON ALMANAC", 9, AMBER, true);
    brand.setLetterSpacing(.16f);
    title = text("CITY OPERATIONS", 17, INK, true);
    heading.addView(brand);
    heading.addView(title);
    heading.setOnClickListener(v -> showCityBranchSwitcherModal());
    header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    speedButton =
        button(
            "1×",
            () -> {
              game.speed = game.speed == 1 ? 2 : game.speed == 2 ? 4 : game.speed == 4 ? 0 : 1;
              updateHud(true);
              saveAsync();
            });
    speedButton.setBackground(medallionShape(0xff243b59, 0xff16263b, AMBER, 22));
    speedButton.setTextColor(AMBER);
    header.addView(speedButton, new LinearLayout.LayoutParams(dp(48), dp(44)));
    Button menu = button("Menu", () -> showHome(true));
    menu.setBackground(medallionShape(0xff213652, 0xff142338, 0x88edbb73, 14));
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(60), dp(44));
    mp.leftMargin = dp(6);
    header.addView(menu, mp);
    root.addView(header);

    LinearLayout almanacWrap = vertical();
    almanacWrap.setPadding(dp(10), dp(4), dp(10), dp(5));
    almanacWrap.setBackgroundColor(0xff091220);
    metrics = text("", 12, MINT, true);
    metrics.setPadding(dp(12), dp(6), dp(12), dp(6));
    metrics.setBackground(medallionShape(0xff172a42, 0xff101d30, 0x6677d7c8, 12));
    almanacWrap.addView(metrics, new LinearLayout.LayoutParams(-1, -2));

    HorizontalScrollView tickerScroll = new HorizontalScrollView(this);
    tickerScroll.setHorizontalScrollBarEnabled(false);
    cityTickerRow = new LinearLayout(this);
    cityTickerRow.setGravity(Gravity.CENTER_VERTICAL);
    cityTickerRow.setPadding(0, dp(4), 0, 0);
    tickerScroll.addView(cityTickerRow);
    almanacWrap.addView(tickerScroll, new LinearLayout.LayoutParams(-1, -2));
    root.addView(almanacWrap);

    LinearLayout content = new LinearLayout(this);
    wideLayout = getResources().getConfiguration().screenWidthDp >= 720;
    content.setOrientation(wideLayout ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
    worldPane = new FrameLayout(this);
    map = new WorldMapView(this, savedMapState);
    map.bind(game, index -> panels.showHub(index));
    map.setNight(night);
    worldPane.addView(map, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout tools = new LinearLayout(this);
    tools.setGravity(Gravity.CENTER_VERTICAL);
    Button fit = button("Fit city", () -> map.resetCamera());
    fit.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x77edbb73, 14));
    tools.addView(fit, new LinearLayout.LayoutParams(dp(70), dp(42)));
    streetButton =
        button(
            "Streets",
            () -> {
              map.toggleStreets();
              updateHud(true);
            });
    streetButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x77edbb73, 14));
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(74), dp(42));
    sp.leftMargin = dp(5);
    tools.addView(streetButton, sp);
    autoButton =
        button(
            "Auto",
            () -> {
              game.autoDispatch = !game.autoDispatch;
              game.message = "Auto dispatch " + (game.autoDispatch ? "enabled" : "disabled") + ".";
              actionDone();
            });
    autoButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x7777d7c8, 14));
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(70), dp(42));
    ap.leftMargin = dp(5);
    tools.addView(autoButton, ap);
    Button goals = button("Goals", () -> panels.overview());
    goals.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x99edbb73, 14));
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(66), dp(42));
    gp.leftMargin = dp(5);
    tools.addView(goals, gp);
    FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    tp.setMargins(dp(10), dp(8), dp(10), 0);
    worldPane.addView(tools, tp);

    goalBox = vertical();
    goalBox.setPadding(dp(14), dp(11), dp(14), dp(11));
    goalBox.setBackground(medallionShape(0xf2192b45, 0xf2101d31, 0x99edbb73, 18));
    goalBox.setElevation(dp(7));
    LinearLayout eyebrowRow = new LinearLayout(this);
    eyebrowRow.setGravity(Gravity.CENTER_VERTICAL);
    TextView eyebrow = text("ADVISOR  ·  MISSION SCROLL", 9, AMBER, true);
    eyebrow.setLetterSpacing(.12f);
    eyebrowRow.addView(eyebrow, new LinearLayout.LayoutParams(0, -2, 1));
    goalCollapseButton =
        button(
            "–",
            () -> {
              goalCollapsed = !goalCollapsed;
              if (goalBody != null) goalBody.setVisibility(goalCollapsed ? View.GONE : View.VISIBLE);
              goalCollapseButton.setText(goalCollapsed ? "+" : "–");
            });
    goalCollapseButton.setMinHeight(dp(26));
    goalCollapseButton.setPadding(dp(6), dp(2), dp(6), dp(2));
    LinearLayout.LayoutParams collapseLp = new LinearLayout.LayoutParams(dp(30), dp(26));
    collapseLp.setMargins(dp(6), 0, 0, 0);
    goalCollapseButton.setLayoutParams(collapseLp);
    eyebrowRow.addView(goalCollapseButton);
    goalBox.addView(eyebrowRow);

    goalTitle = text("YOUR COMPANY", 16, INK, true);
    goalTitle.setPadding(0, dp(5), 0, dp(3));
    goalBox.addView(goalTitle);

    goalBody = vertical();
    goalDetail = text("", 12, MUTED, false);
    goalBody.addView(goalDetail);
    campaignGoalText = text("", 11, MINT, true);
    campaignGoalText.setPadding(0, dp(4), 0, 0);
    goalBody.addView(campaignGoalText);
    chapterProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    chapterProgress.setMax(100);
    chapterProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(AMBER));
    chapterProgress.setProgressBackgroundTintList(
        android.content.res.ColorStateList.valueOf(0xff2a3e57));
    LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(-1, dp(5));
    progressLayout.topMargin = dp(8);
    goalBody.addView(chapterProgress, progressLayout);
    goalAction = button("Next step", () -> goalPressed());
    primary(goalAction);
    goalBody.addView(goalAction);
    goalBox.addView(goalBody);
    FrameLayout.LayoutParams goalParams =
        new FrameLayout.LayoutParams(
            dp(Math.min(286, getResources().getConfiguration().screenWidthDp - 26)),
            -2,
            Gravity.TOP | Gravity.START);
    goalParams.setMargins(dp(12), dp(88), 0, 0);
    worldPane.addView(goalBox, goalParams);
    dispatch = vertical();
    dispatch.setPadding(dp(9), dp(6), dp(9), dp(6));
    FrameLayout.LayoutParams deck = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
    deck.bottomMargin = dp(36);
    worldPane.addView(dispatch, deck);
    mapCaption = text("Connecting street map…", 10, MUTED, false);
    mapCaption.setPadding(dp(10), dp(5), dp(10), dp(5));
    mapCaption.setBackground(shape(0xe6101c2e, 7));
    FrameLayout.LayoutParams caption =
        new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
    caption.setMargins(dp(12), dp(60), dp(8), 0);
    worldPane.addView(mapCaption, caption);
    content.addView(
        worldPane, new LinearLayout.LayoutParams(wideLayout ? 0 : -1, wideLayout ? -1 : 0, 1));
    managementScroll = new ScrollView(this);
    management = vertical();
    management.setPadding(0, dp(8), 0, dp(16));
    managementScroll.addView(management);
    content.addView(
        managementScroll,
        new LinearLayout.LayoutParams(
            wideLayout ? dp(360) : -1, wideLayout ? -1 : 0, wideLayout ? 0 : 1));
    managementScroll.setVisibility(View.GONE);
    message = text("", 10, MUTED, false);
    message.setMaxLines(1);
    message.setEllipsize(android.text.TextUtils.TruncateAt.END);
    message.setPadding(dp(16), dp(5), dp(16), dp(5));
    root.addView(message);
    nav = new LinearLayout(this);
    nav.setPadding(dp(8), dp(5), dp(8), dp(7));
    for (int i = 0; i < tabs.length; i++) {
      final int index = i;
      Button item = button(tabs[i], () -> selectTab(index));
      item.setTextSize(10);
      item.setCompoundDrawablePadding(dp(4));
      item.setCompoundDrawablesWithIntrinsicBounds(
          null, BrandArt.icon(i, MUTED, getResources().getDisplayMetrics().density), null, null);
      LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, dp(64), 1);
      np.setMargins(dp(2), 0, dp(2), 0);
      nav.addView(item, np);
    }
    root.addView(nav);
  }

  void selectTab(int value) {
    tab = value;
    worldPane.setVisibility(tab == 0 || wideLayout ? View.VISIBLE : View.GONE);
    managementScroll.setVisibility(tab == 0 ? View.GONE : View.VISIBLE);
    goalBox.setVisibility(
        tab == 0 && getResources().getConfiguration().screenHeightDp >= 480
            ? View.VISIBLE
            : View.GONE);
    dispatch.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
    for (int i = 0; i < nav.getChildCount(); i++) {
      Button b = (Button) nav.getChildAt(i);
      b.setTextColor(i == tab ? BG : MUTED);
      b.setBackground(shape(i == tab ? AMBER : BG, 13));
      b.setCompoundDrawablesWithIntrinsicBounds(
          null,
          BrandArt.icon(i, i == tab ? BG : MUTED, getResources().getDisplayMetrics().density),
          null,
          null);
    }
    if (tab > 0) renderManagement();
    requestKey = "";
    updateHud(true);
  }

  private void updateHud(boolean force) {
    String cityDisplay =
        game.currentScenario().city
            + (game.branchCount() > 1 ? " ▾ (" + game.branchCount() + " Cities)" : "");
    title.setText(getString(R.string.hud_title, cityDisplay, game.year()));
    soundtrack.updateAtmosphere(game.rushHour(), game.speed, false);
    metrics.setText(
        getString(
            R.string.hud_metrics,
            Game.money(game.cash),
            game.rating,
            game.rides,
            tab == 0 ? "" : getString(R.string.planning_paused)));
    speedButton.setText(
        game.speed == 0
            ? getString(R.string.resume_symbol)
            : getString(R.string.speed_value, game.speed));
    speedButton.setContentDescription(
        game.speed == 0 ? "Resume simulation" : "Change speed. Currently " + game.speed + " times");
    message.setText(saveError != null ? saveError : game.message);
    updateGoal();
    if (chapterProgress != null) {
      int progress =
          game.gameMode == Game.MODE_TUTORIAL
              ? (game.tutorialStep - 1) * 100 / 7
              : game.chapter < 6
                  ? (int) Math.min(100, chapterValue() * 100 / targets[game.chapter])
                  : 100;
      chapterProgress.setProgress(progress);
    }
    for (Game.Ride request : game.requests) {
      ProgressBar meter = requestMeters.get(request.id);
      if (meter != null)
        meter.setProgress((int) Math.max(0, 65000 - (game.elapsed - request.created) * 1000));
    }
    if (mapCaption != null) mapCaption.setText(map.status());
    if (autoButton != null) {
      autoButton.setText(game.autoDispatch ? "Auto on" : "Manual");
      autoButton.setTextColor(game.autoDispatch ? MINT : INK);
    }
    if (streetButton != null) streetButton.setText(map.isUsingStreets() ? "Streets" : "Offline");
    String tKey =
        game.scenarioId
            + ":"
            + game.branchCount()
            + ":"
            + game.founderCash
            + ":"
            + game.founderCeoTarget
            + ":"
            + game.globalFleetCount();
    if (cityTickerRow != null && (force || !tKey.equals(tickerKey))) {
      tickerKey = tKey;
      cityTickerRow.removeAllViews();
      Button ceoPill =
          button(
              "👤 "
                  + game.founderName
                  + ": "
                  + Game.money(game.founderCash)
                  + " · CEO: "
                  + game.activeFounderCeoCompanyName(),
              () -> panels.overview());
      ceoPill.setTextSize(10);
      ceoPill.setMinHeight(dp(28));
      ceoPill.setPadding(dp(10), dp(2), dp(10), dp(2));
      ceoPill.setBackground(medallionShape(0xff1e334e, 0xff111e32, 0x88edbb73, 10));
      LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(28));
      cp.setMargins(0, 0, dp(5), 0);
      cityTickerRow.addView(ceoPill, cp);

      for (int cIdx = 0; cIdx < game.branches.length; cIdx++) {
        if (!game.branches[cIdx]) continue;
        final int targetCity = cIdx;
        boolean active = (cIdx == game.scenarioId);
        Button cityBtn =
            button(
                (active ? "● " : "✈ ")
                    + game.regions[cIdx]
                    + " ("
                    + game.cityFleetCount(cIdx)
                    + "🚋)",
                () -> {
                  if (targetCity != game.scenarioId) {
                    game.switchCity(targetCity);
                    map.bind(game, idx -> panels.showHub(idx));
                    map.resetCamera();
                    actionDone();
                  }
                });
        cityBtn.setTextSize(10);
        cityBtn.setMinHeight(dp(28));
        cityBtn.setPadding(dp(9), dp(2), dp(9), dp(2));
        if (active) primary(cityBtn);
        else cityBtn.setBackground(medallionShape(0xff182b44, 0xff0f1c2e, 0x5577d7c8, 10));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, dp(28));
        bp.setMargins(0, 0, dp(5), 0);
        cityTickerRow.addView(cityBtn, bp);
      }
    }
    String key = game.requests.toString() + ":" + game.autoDispatch;
    if (tab == 0 && (force || !key.equals(requestKey))) {
      requestKey = key;
      renderDispatch();
    }
    if (news != null && !home) {
      news.collect(game);
      if (!modalOpen() && tab == 0) {
        Game.Report pending = null;
        for (Game.Report r : game.reports)
          if (!r.read && (pending == null || r.urgent)) {
            pending = r;
            if (r.urgent) break;
          }
        if (pending != null && (pending.urgent || game.elapsed - lastEdition >= 20)) {
          showReport(pending);
          lastEdition = game.elapsed;
        }
      }
    }
  }

  private long chapterValue() {
    switch (game.chapter) {
      case 0:
      case 3:
      case 5:
        return game.rides;
      case 1:
        return game.taxis.size();
      case 2:
        return game.openCount();
      case 4:
        return game.totalHubLevels();
      default:
        return 1;
    }
  }

  private void updateGoal() {
    if (campaignGoalText != null) {
      campaignGoalText.setText(
          game.gameMode == Game.MODE_TUTORIAL
              ? "Academy Charter · Complete all 8 steps"
              : "Victory Goal: " + game.objectiveText());
    }
    if (game.gameMode == Game.MODE_TUTORIAL) {
      goalTitle.setText(
          game.tutorialStep < 8
              ? getString(R.string.academy_step, game.tutorialStep)
              : getString(R.string.academy_graduate));
      goalDetail.setText(tasks[Math.min(8, game.tutorialStep)]);
      goalAction.setText(
          game.tutorialStep == 8
              ? "Continue building"
              : game.tutorialReady() ? "Complete task" : "Show me where");
      return;
    }
    if (game.chapter < 6) {
      goalTitle.setText(chapters[game.chapter]);
      goalDetail.setText(
          getString(
              R.string.chapter_progress,
              Math.min(chapterValue(), targets[game.chapter]),
              targets[game.chapter],
              Game.money(rewards[game.chapter])));
      goalAction.setText(
          chapterValue() >= targets[game.chapter] ? R.string.claim_reward : R.string.next_step);
    } else {
      goalTitle.setText(game.scenarioWon ? R.string.campaign_complete : R.string.build_empire);
      goalDetail.setText(game.objectiveText());
      goalAction.setText(R.string.company_overview);
    }
  }

  private void goalPressed() {
    if (game.gameMode == Game.MODE_TUTORIAL) {
      if (game.tutorialStep == 8) {
        game.gameMode = Game.MODE_CITY;
        actionDone();
        return;
      }
      if (game.tutorialReady()) {
        game.advanceTutorial();
        actionDone();
      } else selectTab(new int[] {0, 0, 0, 1, 2, 2, 3, 4}[game.tutorialStep]);
      return;
    }
    if (game.chapter >= 6) {
      panels.overview();
      return;
    }
    if (chapterValue() >= targets[game.chapter]) {
      game.cash += rewards[game.chapter];
      game.chapter++;
      game.message = "Chapter complete. Your growth grant has arrived.";
      actionDone();
    } else selectTab(chapterTabs[game.chapter]);
  }

  private void renderDispatch() {
    int scrollX = 0;
    for (int i = 0; i < dispatch.getChildCount(); i++)
      if (dispatch.getChildAt(i) instanceof HorizontalScrollView)
        scrollX = dispatch.getChildAt(i).getScrollX();
    dispatch.removeAllViews();
    requestMeters.clear();
    TextView deckTitle =
        text(
            game.autoDispatch
                ? "AUTO DISPATCH  /  LIVE DEMAND"
                : "DISPATCH BOARD  /  " + game.requests.size() + " WAITING",
            9,
            MINT,
            true);
    deckTitle.setLetterSpacing(.12f);
    deckTitle.setPadding(dp(5), 0, 0, dp(7));
    dispatch.addView(deckTitle);
    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout row = new LinearLayout(this);
    scroll.addView(row);
    dispatch.addView(scroll);
    if (game.requests.isEmpty()) {
      LinearLayout ready = vertical();
      ready.setPadding(dp(15), dp(12), dp(15), dp(12));
      ready.setBackground(shape(0xee142238, 14));
      ready.addView(
          text(
              game.autoDispatch
                  ? "Your network is working."
                  : "The next opportunity is on its way.",
              15,
              INK,
              true));
      ready.addView(
          text(
              game.idleCount() + " available vehicles · " + game.rides + " completed trips",
              11,
              MUTED,
              false));
      row.addView(ready, new LinearLayout.LayoutParams(-1, -2));
      return;
    }
    for (Game.Ride ride : new ArrayList<>(game.requests)) {
      LinearLayout ticket = vertical();
      ticket.setPadding(dp(13), dp(11), dp(13), dp(10));
      ticket.setBackground(medallionShape(0xf21b2d47, 0xf2111e32, ride.kindColor(), 15));
      LinearLayout badge = new LinearLayout(this);
      badge.setGravity(Gravity.CENTER_VERTICAL);
      TextView category = text(ride.kindName(), 9, ride.kindColor(), true);
      category.setLetterSpacing(.10f);
      badge.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
      long netRev = game.estimatedIncome(ride);
      badge.addView(text("+" + Game.money(netRev), 13, MINT, true));
      ticket.addView(badge);
      TextView origin = text(game.districts.get(ride.from).name, 15, INK, true);
      origin.setPadding(0, dp(6), 0, dp(2));
      origin.setSingleLine(true);
      origin.setEllipsize(android.text.TextUtils.TruncateAt.END);
      ticket.addView(origin);
      TextView destination =
          text(
              "→ "
                  + game.districts.get(ride.to).name
                  + " · Tariff "
                  + Game.money(ride.fare),
              11,
              MUTED,
              false);
      destination.setSingleLine(true);
      destination.setEllipsize(android.text.TextUtils.TruncateAt.END);
      ticket.addView(destination);
      ProgressBar patience = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
      patience.setMax(65000);
      patience.setProgress((int) Math.max(0, 65000 - (game.elapsed - ride.created) * 1000));
      patience.setProgressTintList(android.content.res.ColorStateList.valueOf(ride.kindColor()));
      patience.setProgressBackgroundTintList(
          android.content.res.ColorStateList.valueOf(0xff293c55));
      LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(3));
      pp.topMargin = dp(9);
      ticket.addView(patience, pp);
      requestMeters.put(ride.id, patience);
      Button send =
          button(
              "Dispatch →",
              () -> {
                game.dispatch(ride.id);
                actionDone();
              });
      primary(send);
      send.setContentDescription(
          "Dispatch trip from "
              + game.districts.get(ride.from).name
              + " to "
              + game.districts.get(ride.to).name);
      send.setMinHeight(dp(44));
      if (getResources().getConfiguration().screenHeightDp >= 480) ticket.addView(send);
      else {
        ticket.setOnClickListener(
            v -> {
              game.dispatch(ride.id);
              actionDone();
            });
        ticket.setFocusable(true);
        ticket.setContentDescription(send.getContentDescription());
      }
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(248), -2);
      p.rightMargin = dp(9);
      row.addView(ticket, p);
    }
    final int position = scrollX;
    scroll.post(() -> scroll.scrollTo(position, 0));
  }

  void onCitySwitched() {
    map.bind(game, index -> panels.showHub(index));
    map.resetCamera();
    map.invalidate();
    updateHud(true);
    if (tab > 0) renderManagement();
  }

  void showCityBranchSwitcherModal() {
    LinearLayout body = vertical();
    for (int i = 0; i < game.branches.length; i++) {
      final int idx = i;
      boolean open = game.branches[i];
      boolean current = (i == game.scenarioId);
      LinearLayout row = vertical();
      row.setPadding(dp(14), dp(12), dp(14), dp(12));
      row.setBackground(
          medallionShape(
              current ? 0xff1f3654 : 0xff132238,
              0xff0d1828,
              current ? AMBER : (open ? MINT : 0x449cacc2),
              14));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(8);
      row.setLayoutParams(lp);
      row.addView(
          text(
              Game.SCENARIOS[i].city + " · " + Game.SCENARIOS[i].country,
              15,
              current ? AMBER : INK,
              true));
      row.addView(
          text(
              open
                  ? (current
                      ? "✓ Active Operations HQ (Currently Viewing)"
                      : "Operating Branch · Tap below to switch city map & fleet")
                  : "Unopened Territory · Launch cost: " + Game.money(game.branchPrice(i)),
              11,
              open ? MINT : MUTED,
              false));
      Button act =
          button(
              open
                  ? (current ? "Currently Managing " + game.regions[i] : "✈ Switch to " + game.regions[i])
                  : "Launch Branch · " + Game.money(game.branchPrice(i)),
              () -> {
                dismissCurrent();
                if (open) {
                  if (game.switchCity(idx)) onCitySwitched();
                  actionDone();
                } else {
                  game.expand(idx);
                  actionDone();
                }
              });
      if (open && !current) primary(act);
      row.addView(act);
      body.addView(row);
    }
    showThemedModal(
        "GLOBAL OPERATIONS NETWORK",
        "Switch Active City or Expand",
        game.companyName
            + " operates in "
            + game.branchCount()
            + " of 6 world cities. Switch freely between your operating branches.",
        body,
        "Close",
        null);
  }

  void actionDone() {
    updateHud(true);
    map.invalidate();
    if (tab > 0) renderManagement();
    saveAsync();
    int cue = game.pollSoundCue();
    if (sound) {
      soundtrack.playCue(cue != 0 ? cue : BackgroundMusic.CUE_CASH);
    }
    Toast.makeText(this, game.message, Toast.LENGTH_SHORT).show();
  }

  private void renderManagement() {
    int y = managementScroll.getScrollY();
    management.removeAllViews();
    panels.render(tab, management);
    managementScroll.post(() -> managementScroll.scrollTo(0, y));
  }

  void showPaused(AlertDialog dialog) {
    currentDialog = dialog;
    dialog.setOnDismissListener(
        d -> {
          if (currentDialog == dialog) currentDialog = null;
          lastFrame = 0;
          saveAsync();
        });
    dialog.setOnCancelListener(
        d -> {
          if (home) showHome(saves.exists());
        });
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
    if (dialog.getWindow() != null) {
      dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
      dialog
          .getWindow()
          .clearFlags(
              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                  | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
      dialog
          .getWindow()
          .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
  }

  void showThemedModal(
      String eyebrow,
      String headingText,
      String subText,
      View contentBody,
      String negativeLabel,
      Runnable onNegative) {
    dismissCurrent();
    float density = getResources().getDisplayMetrics().density;
    LinearLayout shell = vertical();
    shell.setPadding(dp(20), dp(18), dp(20), dp(18));
    shell.setBackground(medallionShape(0xff162840, 0xff0b1322, AMBER, 20));

    LinearLayout topRow = new LinearLayout(this);
    topRow.setGravity(Gravity.CENTER_VERTICAL);
    ImageView crest = new ImageView(this);
    crest.setImageDrawable(BrandArt.crestDrawable(0xff14253b, AMBER, density));
    topRow.addView(crest, new LinearLayout.LayoutParams(dp(30), dp(30)));
    TextView kick = text("  " + eyebrow, 10, AMBER, true);
    kick.setLetterSpacing(.14f);
    topRow.addView(kick, new LinearLayout.LayoutParams(0, -2, 1));
    shell.addView(topRow);

    TextView h = text(headingText, 21, INK, true);
    h.setPadding(0, dp(8), 0, dp(4));
    shell.addView(h);

    if (subText != null && !subText.isEmpty()) {
      TextView sub = text(subText, 12, MUTED, false);
      sub.setPadding(0, 0, 0, dp(12));
      shell.addView(sub);
    }

    if (contentBody != null) {
      shell.addView(contentBody);
    }

    if (negativeLabel != null && !negativeLabel.isEmpty()) {
      Button backBtn =
          button(
              negativeLabel,
              () -> {
                dismissCurrent();
                if (onNegative != null) onNegative.run();
                else if (home) showHome(saves.exists());
              });
      shell.addView(backBtn);
    }

    ScrollView scroll = new ScrollView(this);
    scroll.addView(shell);
    AlertDialog dialog = new AlertDialog.Builder(this).setView(scroll).create();
    showPaused(dialog);
  }

  EditText styledInput(String initialValue, String hint) {
    EditText input = new EditText(this);
    input.setText(initialValue);
    input.setHint(hint);
    input.setHintTextColor(MUTED);
    input.setTextColor(INK);
    input.setTextSize(14);
    input.setSingleLine(true);
    input.setFocusable(true);
    input.setFocusableInTouchMode(true);
    input.setSelectAllOnFocus(true);
    input.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    input.setPadding(dp(12), dp(10), dp(12), dp(10));
    input.setBackground(medallionShape(0xff101d30, 0xff0b1322, 0x8877d7c8, 12));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.topMargin = dp(4);
    lp.bottomMargin = dp(10);
    input.setLayoutParams(lp);
    return input;
  }

  void confirm(String titleText, String bodyText, Runnable action) {
    dismissCurrent();
    float density = getResources().getDisplayMetrics().density;
    LinearLayout shell = vertical();
    shell.setPadding(dp(20), dp(18), dp(20), dp(12));
    shell.setBackground(medallionShape(0xff162840, 0xff0b1322, AMBER, 18));
    LinearLayout topRow = new LinearLayout(this);
    topRow.setGravity(Gravity.CENTER_VERTICAL);
    ImageView crest = new ImageView(this);
    crest.setImageDrawable(BrandArt.crestDrawable(0xff14253b, AMBER, density));
    topRow.addView(crest, new LinearLayout.LayoutParams(dp(28), dp(28)));
    TextView kick = text("  EXECUTIVE CONFIRMATION", 10, AMBER, true);
    kick.setLetterSpacing(.14f);
    topRow.addView(kick, new LinearLayout.LayoutParams(0, -2, 1));
    shell.addView(topRow);
    TextView h = text(titleText, 20, INK, true);
    h.setPadding(0, dp(8), 0, dp(4));
    shell.addView(h);
    TextView b = text(bodyText, 13, MUTED, false);
    b.setPadding(0, 0, 0, dp(8));
    shell.addView(b);

    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setView(shell)
            .setPositiveButton("Continue", (d, w) -> action.run())
            .setNegativeButton(
                "Cancel",
                (d, w) -> {
                  if (home) showHome(saves.exists());
                })
            .create();
    showPaused(dialog);
    Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
    if (pos != null) {
      pos.setTextColor(AMBER);
      pos.setOnClickListener(
          v -> {
            dismissCurrent();
            action.run();
          });
    }
    Button neg = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
    if (neg != null) {
      neg.setTextColor(MUTED);
      neg.setOnClickListener(
          v -> {
            dismissCurrent();
            if (home) showHome(saves.exists());
          });
    }
  }

  void showReport(Game.Report r) {
    if (modalOpen()) return;
    lastEdition = game.elapsed;
    r.read = true;
    if (sound) soundtrack.playCue(BackgroundMusic.CUE_NEWS);
    LinearLayout paper = vertical();
    paper.setPadding(dp(22), dp(20), dp(22), dp(20));
    paper.setBackground(medallionShape(0xfff2e8cc, 0xffe4d7b2, 0xff8b6b35, 18));
    TextView mast = text("The Mobility Post", 29, 0xff283c2c, true);
    mast.setTypeface(Typeface.SERIF);
    paper.addView(mast);
    TextView date = text(r.date + " · " + r.category.toUpperCase(Locale.US), 10, 0xff5d6a52, false);
    date.setPadding(0, dp(12), 0, dp(10));
    paper.addView(date);
    TextView headline = text(r.title, 24, 0xff26392c, true);
    headline.setTypeface(Typeface.create("serif", Typeface.BOLD));
    paper.addView(headline);
    TextView body = text(r.body, 16, 0xff344331, false);
    body.setTypeface(Typeface.SERIF);
    body.setPadding(0, dp(16), 0, dp(16));
    paper.addView(body);
    TextView impact = text(r.impact, 13, 0xff44583f, false);
    impact.setPadding(dp(12), dp(12), dp(12), dp(12));
    impact.setBackground(shape(0xffdce0c7, 6));
    paper.addView(impact);
    if (r.url != null && !r.url.isEmpty()) {
      TextView source = text(r.source, 11, 0xff465b3a, false);
      source.setPadding(0, dp(16), 0, 0);
      paper.addView(source);
      Button link = button("Read original source", () -> openSource(r.url));
      link.setTextColor(BG);
      link.setBackground(shape(0xffd0d9bb, 8));
      paper.addView(link);
    }
    if (game.cash >= 250) {
      Button edict =
          button(
              "Executive Response: Fleet & Hospitality Drive (-250 cr)",
              () -> {
                if (game.cash >= 250) {
                  game.cash -= 250;
                  game.expenses += 250;
                  for (Game.Taxi t : game.taxis) {
                    t.cleanliness = Math.min(100.0, t.cleanliness + 20.0);
                    t.condition = Math.min(100.0, t.condition + 15.0);
                    t.broken = t.condition < 25.0;
                  }
                  for (Game.Driver d : game.drivers) d.fatigue = Math.max(0.0, d.fatigue - 25.0);
                  game.rating = Math.min(5.0, game.rating + 0.04);
                  game.message = "Executive drive completed: fleet polished & drivers refreshed.";
                }
                dismissCurrent();
                actionDone();
              });
      edict.setTextColor(0xff1d3124);
      edict.setBackground(shape(0xffd6dfc2, 10));
      paper.addView(edict);
    }
    ScrollView scroll = new ScrollView(this);
    Button closeEdition = button("Back to business", this::dismissCurrent);
    primary(closeEdition);
    paper.addView(closeEdition);
    scroll.addView(paper);
    showPaused(new AlertDialog.Builder(this).setView(scroll).create());
    saveAsync();
  }

  private void showHome(boolean canContinue) {
    if (!home) saveAsync();
    home = true;
    LinearLayout page = vertical();
    page.setPadding(dp(20), dp(18), dp(20), dp(18));
    page.setBackground(medallionShape(0xff15263e, BG, AMBER, 22));
    page.addView(new BrandArt(this), new LinearLayout.LayoutParams(-1, dp(170)));
    TextView kicker = text("ENTREPRENEUR EDITION  ·  MULTI-COMPANY TYCOON", 10, MINT, true);
    kicker.setLetterSpacing(.12f);
    kicker.setPadding(0, dp(16), 0, dp(8));
    page.addView(kicker);
    page.addView(text("FAREFORGE", 36, INK, true));
    page.addView(text("MOBILITY TYCOON", 12, AMBER, true));
    if (canContinue) {
      Button resume =
          button(
              "Continue · "
                  + game.companyName
                  + " ("
                  + game.currentScenario().city
                  + ")",
              () -> {
                home = false;
                dismissCurrent();
                selectTab(0);
              });
      primary(resume);
      page.addView(resume);
    }
    page.addView(button("Quick Play · Custom Entrepreneur Sandbox", () -> chooseQuickCity()));
    page.addView(button("Historical Campaigns · 20 Scenarios", () -> chooseHistorical()));
    page.addView(
        button(
            "Learn to play · Fareforge Academy",
            () ->
                startNew(
                    () -> {
                      game = new Game();
                      game.startTutorial();
                    },
                    false)));
    page.addView(
        button(
            night ? "Switch to daylight map" : "Switch to night map",
            () -> {
              night = !night;
              map.setNight(night);
              getPreferences(0).edit().putBoolean("night", night).apply();
              dismissCurrent();
              showHome(canContinue);
            }));
    page.addView(
        button(
            music ? "Music on · tap to mute" : "Music off · tap to enable",
            () -> {
              music = !music;
              soundtrack.setEnabled(music);
              getPreferences(0).edit().putBoolean("music", music).apply();
              dismissCurrent();
              showHome(canContinue);
            }));
    page.addView(
        button(
            sound
                ? "Button sounds on · incident & cash chimes"
                : "Button sounds off · tap to enable",
            () -> {
              sound = !sound;
              soundtrack.setSfxEnabled(sound);
              getPreferences(0).edit().putBoolean("sound", sound).apply();
              dismissCurrent();
              showHome(canContinue);
            }));
    if (saveError != null) page.addView(text(saveError, 12, AMBER, false));
    TextView foot =
        text(
            "Native Android Edition · Offline Play\nFound companies, manage equity stakes, and compete with AI transit rivals.",
            11,
            MUTED,
            false);
    foot.setPadding(0, dp(15), 0, 0);
    page.addView(foot);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(page);
    AlertDialog d = new AlertDialog.Builder(this).setView(scroll).create();
    showPaused(d);
    d.setOnCancelListener(
        v -> {
          if (canContinue) home = false;
          else finish();
        });
  }

  private void chooseQuickCity() {
    LinearLayout list = vertical();
    for (int i = 0; i < Game.SCENARIOS.length; i++) {
      final int cityIdx = i;
      Game.Scenario sc = Game.SCENARIOS[i];
      LinearLayout item = vertical();
      item.setPadding(dp(14), dp(12), dp(14), dp(12));
      item.setBackground(medallionShape(0xff182b46, 0xff101d30, 0x66edbb73, 14));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(8);
      item.setLayoutParams(lp);
      item.addView(text(sc.city + " · " + sc.country, 16, AMBER, true));
      item.addView(text(sc.title + " — " + sc.goalText, 11, MUTED, false));
      Button pick =
          button(
              "Establish Flagship HQ in " + sc.city + " →",
              () -> main.post(() -> chooseQuickGoal(cityIdx)));
      primary(pick);
      item.addView(pick);
      list.addView(item);
    }
    showThemedModal(
        "STEP 1 OF 4  ·  HEADQUARTERS TERRITORY",
        "Choose Your Starting City",
        "Select where your flagship company will incorporate. You can expand to other cities and spin off sister companies later.",
        list,
        "Back to Main Menu",
        () -> showHome(saves.exists()));
  }

  private void chooseQuickGoal(int city) {
    LinearLayout list = vertical();
    String[] badges = {
      "WEALTH BARON",
      "SERIAL ENTREPRENEUR",
      "GLOBAL MOGUL",
      "LOGISTICS KING",
      "PUBLIC FLOAT",
      "HOSPITALITY CROWN"
    };
    for (int i = 0; i < Game.QUICK_GOAL_NAMES.length; i++) {
      final int goalIdx = i;
      LinearLayout item = vertical();
      item.setPadding(dp(14), dp(11), dp(14), dp(11));
      item.setBackground(medallionShape(0xff182b46, 0xff101d30, 0x6677d7c8, 14));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(8);
      item.setLayoutParams(lp);
      item.addView(text(badges[i % badges.length], 9, MINT, true));
      item.addView(text(Game.QUICK_GOAL_NAMES[i], 14, INK, true));
      Button pick =
          button(
              "Select Ambition →",
              () -> main.post(() -> chooseDifficulty(city, goalIdx)));
      item.addView(pick);
      list.addView(item);
    }
    showThemedModal(
        "STEP 2 OF 4  ·  ENTREPRENEUR AMBITION",
        "Define Your Victory Charter",
        "Starting City: " + Game.SCENARIOS[city].city + ". Choose your primary business milestone.",
        list,
        "Back to Cities",
        this::chooseQuickCity);
  }

  private void chooseDifficulty(int city, int goal) {
    String[] desc = {
      "135% Fares · 40% Vehicle Wear · Ideal for relaxed empire building",
      "115% Fares · 65% Vehicle Wear · Forgiving market conditions",
      "100% Standard Fares & Wear · Balanced AI rival competition",
      "90% Tighter Margins · 135% Wear · Aggressive AI rivals",
      "82% Cutthroat Fares · 170% Wear · Full executive challenge"
    };
    LinearLayout list = vertical();
    for (int i = 0; i < Game.DIFFICULTY_NAMES.length; i++) {
      final int diffIdx = i;
      LinearLayout item = vertical();
      item.setPadding(dp(14), dp(11), dp(14), dp(11));
      item.setBackground(medallionShape(0xff182b46, 0xff101d30, 0x66edbb73, 14));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(8);
      item.setLayoutParams(lp);
      item.addView(text(Game.DIFFICULTY_NAMES[i] + " Mode", 15, AMBER, true));
      item.addView(text(desc[i], 11, MUTED, false));
      Button pick =
          button(
              "Next: Incorporate Company →",
              () ->
                  main.post(
                      () ->
                          showFounderRegistrationModal(
                              city,
                              () -> {
                                game = new Game();
                                game.startQuickPlay(city, goal, diffIdx);
                                game.autoDispatch = true;
                              })));
      if (i == Game.DIFF_MEDIUM) primary(pick);
      item.addView(pick);
      list.addView(item);
    }
    showThemedModal(
        "STEP 3 OF 4  ·  MARKET CONDITIONS",
        "Select Simulation Difficulty",
        "Adjust passenger fare margins, mechanical wear, and AI competitor pressure.",
        list,
        "Back to Ambitions",
        () -> chooseQuickGoal(city));
  }

  private void chooseHistorical() {
    LinearLayout list = vertical();
    for (int i = 0; i < Game.HISTORICAL_SCENARIOS.length; i++) {
      final int index = i;
      Game.HistoricalScenarioDef h = Game.HISTORICAL_SCENARIOS[i];
      LinearLayout item = vertical();
      item.setPadding(dp(14), dp(12), dp(14), dp(12));
      item.setBackground(medallionShape(0xff182b46, 0xff101d30, 0x66edbb73, 14));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.bottomMargin = dp(9);
      item.setLayoutParams(lp);
      item.addView(
          text(
              "SCENARIO #" + (i + 1) + "  ·  " + h.era.toUpperCase(Locale.US),
              9,
              MINT,
              true));
      item.addView(text(h.title, 16, AMBER, true));
      item.addView(text("Charter Goal: " + h.objective, 12, INK, false));
      Button launch =
          button(
              "Incorporate & Launch Scenario #" + (i + 1) + " →",
              () ->
                  main.post(
                      () ->
                          showFounderRegistrationModal(
                              h.cityId,
                              () -> {
                                game = new Game();
                                game.startHistoricalScenario(index);
                                game.autoDispatch = true;
                              })));
      primary(launch);
      item.addView(launch);
      list.addView(item);
    }
    showThemedModal(
        "20 HISTORICAL CAMPAIGNS (2010–2025)",
        "Select a Historical Scenario",
        "Relive pivotal moments in urban mobility across 6 world capitals.",
        list,
        "Back to Main Menu",
        () -> showHome(saves.exists()));
  }

  /** Step 4: Entrepreneur sets Owner Name, Company Name, Ownership %, and Starting Hubs. */
  private void showFounderRegistrationModal(int cityId, Runnable baseSetup) {
    LinearLayout form = vertical();
    String cityName = Game.SCENARIOS[Math.max(0, Math.min(Game.SCENARIOS.length - 1, cityId))].city;

    form.addView(text("ENTREPRENEUR / CEO NAME (YOUR NAME)", 10, MINT, true));
    EditText ownerInput =
        styledInput("", "Type your Entrepreneur / CEO Name...");
    form.addView(ownerInput);

    form.addView(text("FLAGSHIP COMPANY NAME (YOUR COMPANY)", 10, MINT, true));
    EditText companyInput =
        styledInput("", "Type your Flagship Company Name...");
    form.addView(companyInput);

    form.addView(
        text(
            "FOUNDER EQUITY OWNERSHIP % (SELLING SHARES ADDS SEED CAPITAL)",
            10,
            MINT,
            true));
    final int[] equityOptions = {100, 80, 60, 40};
    final String[] equityLabels = {
      "100% Ownership · Bootstrapped (+0 cr Outside Seed, Full Control)",
      "80% Ownership · Angel Co-Invested (+3,200 cr Seed Capital)",
      "60% Ownership · Venture Syndicate (+6,400 cr Seed Capital)",
      "40% Ownership · Public Float (+9,600 cr Seed Capital)"
    };
    final int[] selectedEquity = {80};
    RadioGroup equityGroup = new RadioGroup(this);
    for (int i = 0; i < equityOptions.length; i++) {
      final int pct = equityOptions[i];
      RadioButton rb = new RadioButton(this);
      rb.setText(equityLabels[i]);
      rb.setTextColor(INK);
      rb.setTextSize(12);
      rb.setChecked(pct == 80);
      rb.setOnClickListener(v -> selectedEquity[0] = pct);
      equityGroup.addView(rb);
    }
    form.addView(equityGroup);

    form.addView(text("STARTING 2-HUB CORRIDOR IN " + cityName.toUpperCase(Locale.US), 10, MINT, true));
    final int[][] hubPairs = {{0, 1}, {1, 2}, {0, 3}};
    final String[] pairLabels = {
      "Central Core ↔ Financial District (Hubs #1 & #2)",
      "Financial District ↔ Airport Gate (Hubs #2 & #3)",
      "Central Core ↔ Rail Terminus (Hubs #1 & #4)"
    };
    final int[] selectedPair = {0};
    RadioGroup pairGroup = new RadioGroup(this);
    for (int i = 0; i < hubPairs.length; i++) {
      final int pIdx = i;
      RadioButton rb = new RadioButton(this);
      rb.setText(pairLabels[i]);
      rb.setTextColor(INK);
      rb.setTextSize(12);
      rb.setChecked(i == 0);
      rb.setOnClickListener(v -> selectedPair[0] = pIdx);
      pairGroup.addView(rb);
    }
    form.addView(pairGroup);

    Button launchBtn =
        button(
            "Incorporate Company & Begin Operations →",
            () -> {
              final String chosenOwner = ownerInput.getText().toString().trim();
              final String chosenCompany = companyInput.getText().toString().trim();
              final int eq = selectedEquity[0];
              final int[] hubs = hubPairs[selectedPair[0]];
              startNew(
                  () -> {
                    baseSetup.run();
                    game.configureFounderCompany(
                        chosenOwner.isEmpty() ? "Founder" : chosenOwner,
                        chosenCompany.isEmpty() ? (cityName + " Mobility") : chosenCompany,
                        eq,
                        hubs[0],
                        hubs[1]);
                  },
                  true);
            });
    primary(launchBtn);
    form.addView(launchBtn);

    showThemedModal(
        "ARTICLES OF INCORPORATION",
        "Register Your Flagship Company",
        "Name your company, choose how much equity ownership you retain, and select your initial 2-hub corridor.",
        form,
        "Back",
        () -> showHome(saves.exists()));
  }

  /** Modal to create a 2nd, 3rd, or 4th company in the same city (2 hubs) or another city! */
  void showFoundVentureModal() {
    LinearLayout form = vertical();
    form.addView(text("NEW COMPANY / VENTURE NAME (ENTER YOUR CUSTOM NAME)", 10, MINT, true));
    EditText nameInput =
        styledInput("", "Type your new Company Name...");
    form.addView(nameInput);

    form.addView(text("CHOOSE OPERATING CITY (SAME OR NEW CITY)", 10, MINT, true));
    final int[] chosenCity = {game.scenarioId};
    RadioGroup cityGroup = new RadioGroup(this);
    for (int i = 0; i < Game.SCENARIOS.length; i++) {
      final int cIdx = i;
      RadioButton rb = new RadioButton(this);
      rb.setText(
          Game.SCENARIOS[i].city
              + (i == game.scenarioId
                  ? " (Current City — Launch 2nd Hub Line)"
                  : (game.branches[i] ? " (Operating Branch)" : " (New City Expansion)")));
      rb.setTextColor(INK);
      rb.setTextSize(12);
      rb.setChecked(i == game.scenarioId);
      rb.setOnClickListener(v -> chosenCity[0] = cIdx);
      cityGroup.addView(rb);
    }
    form.addView(cityGroup);

    form.addView(text("SELECT 2-HUB CORRIDOR FOR THIS COMPANY", 10, MINT, true));
    final int[][] pairs = {{2, 4}, {3, 5}, {0, 2}, {1, 4}};
    final String[] pairDesc = {
      "Airport Hub (#3) ↔ Tech / Commercial Corridor (#5)",
      "Rail Terminus (#4) ↔ Southern / Port Hub (#6)",
      "Central Hub (#1) ↔ Airport Express (#3)",
      "Financial Hub (#2) ↔ Commercial Ward (#5)"
    };
    final int[] chosenPair = {0};
    RadioGroup hubGroup = new RadioGroup(this);
    for (int i = 0; i < pairs.length; i++) {
      final int pIdx = i;
      RadioButton rb = new RadioButton(this);
      rb.setText(pairDesc[i]);
      rb.setTextColor(INK);
      rb.setTextSize(12);
      rb.setChecked(i == 0);
      rb.setOnClickListener(v -> chosenPair[0] = pIdx);
      hubGroup.addView(rb);
    }
    form.addView(hubGroup);

    form.addView(text("YOUR FOUNDER OWNERSHIP % IN THIS COMPANY", 10, MINT, true));
    final int[] eqOpts = {100, 80, 60, 40};
    final int[] chosenEq = {60};
    RadioGroup eqGroup = new RadioGroup(this);
    for (int pct : eqOpts) {
      RadioButton rb = new RadioButton(this);
      long cost = game.ventureStartupCost(chosenCity[0], pct);
      rb.setText(
          pct
              + "% Owner Equity · Co-Investors pay "
              + (100 - pct)
              + "% (Your cash cost: ~"
              + Game.money(cost)
              + ")");
      rb.setTextColor(INK);
      rb.setTextSize(12);
      rb.setChecked(pct == 60);
      rb.setOnClickListener(v -> chosenEq[0] = pct);
      eqGroup.addView(rb);
    }
    form.addView(eqGroup);

    form.addView(text("CAPITAL FUNDING SOURCE", 10, MINT, true));
    final boolean[] usePersonal = {false};
    RadioGroup fundGroup = new RadioGroup(this);
    RadioButton corpFund = new RadioButton(this);
    corpFund.setText(
        "Corporate Treasury (" + Game.money(game.cash) + ") — Spin-Off Subsidiary");
    corpFund.setTextColor(INK);
    corpFund.setTextSize(12);
    corpFund.setChecked(true);
    corpFund.setOnClickListener(v -> usePersonal[0] = false);
    fundGroup.addView(corpFund);

    RadioButton persFund = new RadioButton(this);
    persFund.setText(
        "Entrepreneur Personal Wealth ("
            + Game.money(game.founderCash)
            + ") — Personal Venture");
    persFund.setTextColor(INK);
    persFund.setTextSize(12);
    persFund.setOnClickListener(v -> usePersonal[0] = true);
    fundGroup.addView(persFund);
    form.addView(fundGroup);

    form.addView(text("COMPANY CEO APPOINTMENT (1 PERSON = 1 COMPANY CEO)", 10, MINT, true));
    final boolean[] becomeCeoHere = {false};
    RadioGroup ceoGroup = new RadioGroup(this);
    String hiredName = Game.hiredCeoNameFor(game.ventures.size());
    RadioButton hiredCeoRb = new RadioButton(this);
    hiredCeoRb.setText(
        "Appoint Hired CEO "
            + hiredName
            + " (240 cr/qtr) · Keep "
            + game.founderName
            + " as CEO of "
            + game.activeFounderCeoCompanyName());
    hiredCeoRb.setTextColor(INK);
    hiredCeoRb.setTextSize(12);
    hiredCeoRb.setChecked(true);
    hiredCeoRb.setOnClickListener(v -> becomeCeoHere[0] = false);
    ceoGroup.addView(hiredCeoRb);

    RadioButton founderCeoRb = new RadioButton(this);
    founderCeoRb.setText(
        game.founderName
            + " becomes CEO here (+25% Venture Rev · Appoints Hired CEO to "
            + game.activeFounderCeoCompanyName()
            + ")");
    founderCeoRb.setTextColor(INK);
    founderCeoRb.setTextSize(12);
    founderCeoRb.setOnClickListener(v -> becomeCeoHere[0] = true);
    ceoGroup.addView(founderCeoRb);
    form.addView(ceoGroup);

    Button createBtn =
        button(
            "Incorporate New Company →",
            () -> {
              String vName = nameInput.getText().toString().trim();
              int[] hp = pairs[chosenPair[0]];
              dismissCurrent();
              if (game.foundVenture(vName, chosenCity[0], hp[0], hp[1], chosenEq[0], usePersonal[0])
                  && becomeCeoHere[0]) {
                game.assignFounderAsCeo(game.ventures.size() - 1);
              }
              actionDone();
            });
    primary(createBtn);
    form.addView(createBtn);

    showThemedModal(
        "SERIAL ENTREPRENEUR DESK",
        "Incorporate a New Mobility Company",
        "Launch an additional company in your current city (connecting 2 hubs) or in a new international city. Later you can vote to merge or sell it.",
        form,
        "Cancel",
        null);
  }

  void showRenameCompanyModal() {
    LinearLayout form = vertical();
    form.addView(text("ENTREPRENEUR / CEO NAME", 10, MINT, true));
    EditText ownerIn = styledInput(game.founderName, "Type your Entrepreneur Name");
    form.addView(ownerIn);

    form.addView(text("FLAGSHIP COMPANY NAME", 10, MINT, true));
    EditText compIn = styledInput(game.companyName, "Type your Flagship Company Name");
    form.addView(compIn);

    final ArrayList<EditText> ventureInputs = new ArrayList<>();
    for (int i = 0; i < game.ventures.size(); i++) {
      Game.Venture v = game.ventures.get(i);
      form.addView(
          text(
              "SUBSIDIARY COMPANY #" + (i + 1) + " (" + game.regions[v.cityId] + ")",
              10,
              MINT,
              true));
      EditText vIn = styledInput(v.name, "Type Subsidiary Company Name");
      ventureInputs.add(vIn);
      form.addView(vIn);
    }

    Button saveBtn =
        button(
            "Save Entrepreneur & Company Names",
            () -> {
              String o = ownerIn.getText().toString().trim();
              String c = compIn.getText().toString().trim();
              if (!o.isEmpty()) {
                game.founderName = o;
                if (game.founderCeoTarget >= 0 && game.founderCeoTarget < game.ventures.size()) {
                  game.ventures.get(game.founderCeoTarget).ceoName = o;
                }
              }
              if (!c.isEmpty()) game.companyName = c;
              for (int i = 0; i < ventureInputs.size() && i < game.ventures.size(); i++) {
                String vn = ventureInputs.get(i).getText().toString().trim();
                if (!vn.isEmpty()) game.ventures.get(i).name = vn;
              }
              game.message =
                  "Updated identity: " + game.companyName + " (CEO/Founder: " + game.founderName + ").";
              dismissCurrent();
              actionDone();
            });
    primary(saveBtn);
    form.addView(saveBtn);

    showThemedModal(
        "CORPORATE REGISTRY",
        "Customize Entrepreneur & Company Names",
        "Enter your own custom name for the Entrepreneur and for each of your companies.",
        form,
        "Cancel",
        null);
  }

  private void startNew(Runnable setup, boolean alreadyConfigured) {
    dismissCurrent();
    Runnable start =
        () -> {
          setup.run();
          home = false;
          lastEdition = -100;
          lastFrame = 0;
          saveError = null;
          map.bind(game, index -> panels.showHub(index));
          map.resetCamera();
          selectTab(0);
          saveAsync();
        };
    if (saves.exists())
      confirm(
          "Start a new company?", "This replaces the native campaign saved on this device.", start);
    else start.run();
  }

  void dismissCurrent() {
    if (currentDialog != null) currentDialog.dismiss();
  }

  boolean modalOpen() {
    return currentDialog != null && currentDialog.isShowing();
  }

  private void openSource(String url) {
    try {
      Uri uri = Uri.parse(url);
      if ("https".equals(uri.getScheme())) startActivity(new Intent(Intent.ACTION_VIEW, uri));
    } catch (ActivityNotFoundException e) {
      Toast.makeText(this, "No browser is available to open this source.", Toast.LENGTH_LONG)
          .show();
    }
  }

  private void saveAsync() {
    if (home || game == null || closed) return;
    try {
      byte[] bytes = SaveCodec.encode(game);
      DISK.execute(
          () -> {
            try {
              saves.write(bytes);
              saveError = null;
            } catch (IOException e) {
              saveError = "Save failed. Free some device storage and try again.";
            }
          });
    } catch (Exception e) {
      saveError = "Your progress could not be saved. " + e.getMessage();
    }
  }
}
