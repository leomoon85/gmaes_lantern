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
  private LinearLayout root, nav, management, dispatch, goalBox;
  private TextView title, metrics, message, goalTitle, goalDetail;
  private Button speedButton, goalAction;
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
    if (lastFrame != 0 && !home && !modalOpen() && tab == 0)
      game.tick(Math.min(.1, (frameTime - lastFrame) / 1e9));
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
    d.setStroke(dp(1), 0x263f5874);
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
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(14), dp(10), dp(12), dp(10));
    ImageView mark = new ImageView(this);
    mark.setImageResource(R.drawable.ic_launcher);
    mark.setContentDescription("Fareforge");
    header.addView(mark, new LinearLayout.LayoutParams(dp(40), dp(44)));
    LinearLayout heading = vertical();
    heading.setPadding(dp(10), 0, 0, 0);
    TextView brand = text("FAREFORGE", 10, AMBER, true);
    brand.setLetterSpacing(.20f);
    title = text("CITY OPERATIONS", 18, INK, true);
    heading.addView(brand);
    heading.addView(title);
    header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    speedButton =
        button(
            "1×",
            () -> {
              game.speed = game.speed == 1 ? 2 : game.speed == 2 ? 4 : game.speed == 4 ? 0 : 1;
              updateHud(true);
              saveAsync();
            });
    header.addView(speedButton, new LinearLayout.LayoutParams(dp(46), dp(46)));
    Button menu = button("Menu", () -> showHome(true));
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(58), dp(46));
    mp.leftMargin = dp(6);
    header.addView(menu, mp);
    root.addView(header);
    metrics = text("", 13, MINT, true);
    metrics.setPadding(dp(18), 0, dp(12), dp(11));
    root.addView(metrics);
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
    tools.addView(fit, new LinearLayout.LayoutParams(dp(70), dp(44)));
    streetButton =
        button(
            "Streets",
            () -> {
              map.toggleStreets();
              updateHud(true);
            });
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(75), dp(44));
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
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(70), dp(44));
    ap.leftMargin = dp(5);
    tools.addView(autoButton, ap);
    Button goals = button("Goals", () -> panels.overview());
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(66), dp(44));
    gp.leftMargin = dp(5);
    tools.addView(goals, gp);
    FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    tp.setMargins(dp(10), dp(10), dp(10), 0);
    worldPane.addView(tools, tp);
    goalBox = vertical();
    goalBox.setPadding(dp(15), dp(13), dp(15), dp(12));
    goalBox.setBackground(shape(0xf2142238, 18));
    goalBox.setElevation(dp(7));
    TextView eyebrow = text("YOUR COMPANY  /  NEXT MOVE", 9, MINT, true);
    eyebrow.setLetterSpacing(.12f);
    goalBox.addView(eyebrow);
    goalTitle = text("YOUR COMPANY", 17, INK, true);
    goalTitle.setPadding(0, dp(7), 0, dp(4));
    goalDetail = text("", 12, MUTED, false);
    goalBox.addView(goalTitle);
    goalBox.addView(goalDetail);
    chapterProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    chapterProgress.setMax(100);
    chapterProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(AMBER));
    chapterProgress.setProgressBackgroundTintList(
        android.content.res.ColorStateList.valueOf(0xff2a3e57));
    LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(-1, dp(4));
    progressLayout.topMargin = dp(9);
    goalBox.addView(chapterProgress, progressLayout);
    goalAction = button("Next step", () -> goalPressed());
    goalBox.addView(goalAction);
    FrameLayout.LayoutParams goalParams =
        new FrameLayout.LayoutParams(
            dp(Math.min(286, getResources().getConfiguration().screenWidthDp - 26)),
            -2,
            Gravity.TOP | Gravity.START);
    goalParams.setMargins(dp(12), dp(92), 0, 0);
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
    title.setText(getString(R.string.hud_title, game.currentScenario().city, game.year()));
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
      ticket.setBackground(shape(0xf2142238, 15));
      LinearLayout badge = new LinearLayout(this);
      TextView category = text(ride.kindName(), 9, ride.kindColor(), true);
      category.setLetterSpacing(.10f);
      badge.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
      badge.addView(text(Game.money(ride.fare), 12, AMBER, true));
      ticket.addView(badge);
      TextView origin = text(game.districts.get(ride.from).name, 15, INK, true);
      origin.setPadding(0, dp(7), 0, dp(2));
      origin.setSingleLine(true);
      origin.setEllipsize(android.text.TextUtils.TruncateAt.END);
      ticket.addView(origin);
      TextView destination = text("→ " + game.districts.get(ride.to).name, 12, MUTED, false);
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
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(246), -2);
      p.rightMargin = dp(9);
      row.addView(ticket, p);
    }
    final int position = scrollX;
    scroll.post(() -> scroll.scrollTo(position, 0));
  }

  void actionDone() {
    updateHud(true);
    map.invalidate();
    if (tab > 0) renderManagement();
    saveAsync();
    if (sound) {
      try {
        android.media.ToneGenerator tone =
            new android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 12);
        tone.startTone(android.media.ToneGenerator.TONE_PROP_ACK, 45);
        main.postDelayed(tone::release, 100);
      } catch (RuntimeException ignored) {
      }
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
          if (home) main.post(() -> showHome(saves.exists()));
        });
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
  }

  void confirm(String title, String body, Runnable action) {
    showPaused(
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton("Continue", (d, w) -> action.run())
            .setNegativeButton("Cancel", null)
            .create());
  }

  void showReport(Game.Report r) {
    if (modalOpen()) return;
    lastEdition = game.elapsed;
    r.read = true;
    LinearLayout paper = vertical();
    paper.setPadding(dp(22), dp(20), dp(22), dp(20));
    paper.setBackgroundColor(0xffeee5c9);
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
    page.setBackground(shape(BG, 22));
    page.addView(new BrandArt(this), new LinearLayout.LayoutParams(-1, dp(170)));
    TextView kicker = text("THE CITY IS YOUR NEXT BIG MOVE", 10, MINT, true);
    kicker.setLetterSpacing(.12f);
    kicker.setPadding(0, dp(16), 0, dp(8));
    page.addView(kicker);
    page.addView(text("FAREFORGE", 36, INK, true));
    page.addView(text("MOBILITY TYCOON", 12, AMBER, true));
    if (canContinue) {
      Button resume =
          button(
              "Continue · " + game.currentScenario().city,
              () -> {
                home = false;
                dismissCurrent();
                selectTab(0);
              });
      primary(resume);
      page.addView(resume);
    }
    page.addView(button("Quick Play", () -> chooseQuickCity()));
    page.addView(button("Historical campaigns · 20 scenarios", () -> chooseHistorical()));
    page.addView(
        button(
            "Learn to play",
            () ->
                startNew(
                    () -> {
                      game = new Game();
                      game.startTutorial();
                    })));
    page.addView(
        button(
            night ? "Switch to daylight" : "Switch to night map",
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
            sound ? "Button sounds on · tap to mute" : "Button sounds off · tap to enable",
            () -> {
              sound = !sound;
              getPreferences(0).edit().putBoolean("sound", sound).apply();
              dismissCurrent();
              showHome(canContinue);
            }));
    if (saveError != null) page.addView(text(saveError, 12, AMBER, false));
    TextView foot =
        text(
            "Native Android edition · Offline play\nGame money uses fictional credits.",
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
    dismissCurrent();
    String[] cities = new String[Game.SCENARIOS.length];
    for (int i = 0; i < cities.length; i++) cities[i] = Game.SCENARIOS[i].city;
    showPaused(
        new AlertDialog.Builder(this)
            .setTitle("Choose your city")
            .setItems(cities, (d, city) -> main.post(() -> chooseQuickGoal(city)))
            .setNegativeButton("Back", (d, w) -> main.post(() -> showHome(saves.exists())))
            .create());
  }

  private void chooseQuickGoal(int city) {
    showPaused(
        new AlertDialog.Builder(this)
            .setTitle("Your ambition")
            .setItems(
                Game.QUICK_GOAL_NAMES, (d, goal) -> main.post(() -> chooseDifficulty(city, goal)))
            .setNegativeButton("Back", (d, w) -> main.post(() -> chooseQuickCity()))
            .create());
  }

  private void chooseDifficulty(int city, int goal) {
    showPaused(
        new AlertDialog.Builder(this)
            .setTitle("Choose difficulty")
            .setItems(
                Game.DIFFICULTY_NAMES,
                (d, difficulty) ->
                    main.post(
                        () ->
                            startNew(
                                () -> {
                                  game = new Game();
                                  game.startQuickPlay(city, goal, difficulty);
                                  game.autoDispatch = true;
                                })))
            .setNegativeButton("Back", (d, w) -> main.post(() -> chooseQuickGoal(city)))
            .create());
  }

  private void chooseHistorical() {
    dismissCurrent();
    String[] choices = new String[Game.HISTORICAL_SCENARIOS.length];
    for (int i = 0; i < choices.length; i++)
      choices[i] = Game.HISTORICAL_SCENARIOS[i].title + "\n" + Game.HISTORICAL_SCENARIOS[i].era;
    showPaused(
        new AlertDialog.Builder(this)
            .setTitle("Historical campaigns")
            .setItems(
                choices,
                (d, index) ->
                    main.post(
                        () ->
                            startNew(
                                () -> {
                                  game = new Game();
                                  game.startHistoricalScenario(index);
                                  game.autoDispatch = true;
                                })))
            .setNegativeButton("Back", (d, w) -> main.post(() -> showHome(saves.exists())))
            .create());
  }

  private void startNew(Runnable setup) {
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

  private void dismissCurrent() {
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
