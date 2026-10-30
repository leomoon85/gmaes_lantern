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
  static final int BG = 0xff101f1a,
      PANEL = 0xff1c3329,
      INK = 0xfff2eedc,
      MUTED = 0xffb4c6b7,
      AMBER = 0xffedbb73,
      MINT = 0xffa6d4bd;
  Game game;
  NativeNews news;
  private SaveStore saves;
  private static final ExecutorService DISK =
      Executors.newSingleThreadExecutor(
          task -> {
            Thread thread = new Thread(task, "Lantern saves");
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
  CityMapView map;
  private ScrollView managementScroll;
  private GamePanels panels;
  private boolean foreground = false, home = true, night = true, sound = true, closed = false;
  private int tab = 0;
  private long lastFrame = 0, lastPaint = 0, lastHud = 0, lastSave = 0;
  private double lastEdition = -100;
  private String requestKey = "";
  private AlertDialog currentDialog;
  private volatile String saveError;
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
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    night = getPreferences(0).getBoolean("night", true);
    sound = getPreferences(0).getBoolean("sound", true);
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
  protected void onResume() {
    super.onResume();
    foreground = true;
    main.removeCallbacks(pausedFrameTick);
    lastFrame = lastPaint = 0;
    Choreographer.getInstance().removeFrameCallback(this);
    Choreographer.getInstance().postFrameCallback(this);
  }

  @Override
  protected void onPause() {
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
    } catch (IOException error) {
      saveError = "Progress snapshot could not be created.";
    }
  }

  @Override
  protected void onDestroy() {
    closed = true;
    Choreographer.getInstance().removeFrameCallback(this);
    main.removeCallbacksAndMessages(null);
    super.onDestroy();
  }

  @Override
  public void doFrame(long frameTime) {
    if (!foreground || closed) return;
    if (lastFrame != 0 && !home && !modalOpen() && tab == 0)
      game.tick(Math.min(.1, (frameTime - lastFrame) / 1e9));
    lastFrame = frameTime;
    if (!home && !modalOpen() && game.speed > 0 && tab == 0 && frameTime - lastPaint > 33_000_000) {
      map.invalidate();
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
    v.setBackground(shape(0xff304d3c, 10));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(6), 0, 0);
    v.setLayoutParams(p);
    v.setOnClickListener(view -> action.run());
    return v;
  }

  void primary(Button b) {
    b.setTextColor(BG);
    b.setBackground(shape(AMBER, 10));
  }

  LinearLayout card(LinearLayout parent, String heading, String description) {
    LinearLayout c = vertical();
    c.setPadding(dp(16), dp(15), dp(16), dp(15));
    c.setBackground(shape(PANEL, 14));
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
    header.setPadding(dp(16), dp(8), dp(12), dp(8));
    LinearLayout heading = vertical();
    title = text("LANTERN", 18, INK, true);
    metrics = text("", 11, MINT, false);
    heading.addView(title);
    heading.addView(metrics);
    header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
    speedButton =
        button(
            "1×",
            () -> {
              game.speed = game.speed == 1 ? 2 : game.speed == 2 ? 4 : game.speed == 4 ? 0 : 1;
              updateHud(true);
              saveAsync();
            });
    header.addView(speedButton, new LinearLayout.LayoutParams(dp(58), dp(48)));
    Button menu = button("Menu", () -> showHome(true));
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(66), dp(48));
    mp.leftMargin = dp(6);
    header.addView(menu, mp);
    root.addView(header);
    goalBox = vertical();
    goalBox.setPadding(dp(16), dp(8), dp(16), dp(10));
    goalBox.setBackgroundColor(0xff20382c);
    goalTitle = text("YOUR COMPANY", 15, AMBER, true);
    goalDetail = text("", 12, MUTED, false);
    goalBox.addView(goalTitle);
    goalBox.addView(goalDetail);
    goalAction = button("Continue", () -> goalPressed());
    goalBox.addView(goalAction);
    root.addView(goalBox);
    LinearLayout content = new LinearLayout(this);
    boolean wide = getResources().getConfiguration().screenWidthDp >= 720;
    content.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
    FrameLayout world = new FrameLayout(this);
    map = new CityMapView(this);
    map.bind(game, index -> panels.showHub(index));
    map.setNight(night);
    world.addView(map, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout tools = new LinearLayout(this);
    tools.addView(
        button("Fit city", () -> map.resetCamera()), new LinearLayout.LayoutParams(dp(75), dp(48)));
    Button auto =
        button(
            "Auto",
            () -> {
              game.autoDispatch = !game.autoDispatch;
              game.message = "Auto dispatch " + (game.autoDispatch ? "enabled" : "disabled") + ".";
              actionDone();
            });
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(75), dp(48));
    ap.leftMargin = dp(6);
    tools.addView(auto, ap);
    tools.addView(
        button("Goals", () -> panels.overview()), new LinearLayout.LayoutParams(dp(75), dp(48)));
    FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
    tp.setMargins(dp(8), dp(8), dp(8), 0);
    world.addView(tools, tp);
    content.addView(world, new LinearLayout.LayoutParams(wide ? 0 : -1, wide ? -1 : 0, 1));
    managementScroll = new ScrollView(this);
    management = vertical();
    management.setPadding(0, dp(4), 0, dp(12));
    managementScroll.addView(management);
    content.addView(
        managementScroll,
        new LinearLayout.LayoutParams(wide ? dp(330) : -1, wide ? -1 : 0, wide ? 0 : 1));
    managementScroll.setVisibility(View.GONE);
    dispatch = vertical();
    dispatch.setPadding(dp(10), dp(4), dp(10), dp(4));
    root.addView(dispatch);
    message = text("", 11, MUTED, false);
    message.setMaxLines(2);
    message.setPadding(dp(16), dp(4), dp(16), dp(4));
    root.addView(message);
    nav = new LinearLayout(this);
    nav.setPadding(dp(6), dp(4), dp(6), dp(6));
    for (int i = 0; i < tabs.length; i++) {
      final int index = i;
      Button item = button(tabs[i], () -> selectTab(index));
      item.setTextSize(11);
      nav.addView(item, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    root.addView(nav);
  }

  void selectTab(int value) {
    tab = value;
    managementScroll.setVisibility(tab == 0 ? View.GONE : View.VISIBLE);
    goalBox.setVisibility(
        tab == 0 && getResources().getConfiguration().screenHeightDp >= 480
            ? View.VISIBLE
            : View.GONE);
    dispatch.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
    for (int i = 0; i < nav.getChildCount(); i++) {
      Button b = (Button) nav.getChildAt(i);
      b.setTextColor(i == tab ? BG : MUTED);
      b.setBackground(shape(i == tab ? MINT : BG, 10));
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
    dispatch.removeAllViews();
    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout row = new LinearLayout(this);
    scroll.addView(row);
    dispatch.addView(scroll);
    if (game.requests.isEmpty()) {
      TextView empty =
          text(
              game.autoDispatch
                  ? "Auto dispatch is working. Your next opportunity is on its way."
                  : "No waiting trips. Explore the city or enable Auto.",
              12,
              MUTED,
              false);
      empty.setPadding(dp(6), dp(10), dp(6), dp(10));
      row.addView(empty);
      return;
    }
    for (Game.Ride ride : new ArrayList<>(game.requests)) {
      Button trip =
          button(
              ride.kindName()
                  + " · "
                  + Game.money(ride.fare)
                  + "\n"
                  + game.districts.get(ride.from).name
                  + " → "
                  + game.districts.get(ride.to).name,
              () -> {
                game.dispatch(ride.id);
                actionDone();
              });
      trip.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(260), dp(68));
      p.rightMargin = dp(8);
      row.addView(trip, p);
    }
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
    TextView mast = text("The Lantern Ledger", 29, 0xff283c2c, true);
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
    page.setPadding(dp(24), dp(18), dp(24), dp(18));
    page.addView(text("LANTERN", 33, AMBER, true));
    page.addView(text("A city. A single vehicle. An empire to build.", 15, MUTED, false));
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
            sound ? "Sound on · tap to mute" : "Sound off · tap to enable",
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
