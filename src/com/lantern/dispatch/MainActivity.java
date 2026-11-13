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
  private TextView title, metrics, message, goalEyebrow, goalTitle, goalDetail, campaignGoalText;
  private ImageView advisorAvatar;
  private Button speedButton, goalAction, goalCollapseButton, fitButton;
  private String tickerKey = "", lastNewsNoticeId = "";
  private boolean goalCollapsed = true;
  private boolean goalHasUnread = false;
  private boolean dispatchCollapsed = false;
  private TextView notificationBanner;
  private final Runnable hideNotificationRunnable =
      () -> {
        if (notificationBanner != null) notificationBanner.setVisibility(View.GONE);
      };
  WorldMapView map;
  private FrameLayout worldPane;
  private boolean wideLayout;
  private TextView mapCaption;
  private Button autoButton, streetButton, goalsButton;
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
  private final String[] tabs = {"City", "Fleet", "Hubs", "Team", "Ledger", "Market", "News"};

  String tabLabel(int idx) {
    String lang = game != null ? game.language : "en";
    switch (idx) {
      case 0: return I18n.get(lang, "tab_city");
      case 1: return I18n.get(lang, "tab_fleet");
      case 2: return I18n.get(lang, "tab_hubs");
      case 3: return I18n.get(lang, "tab_team");
      case 4: return I18n.get(lang, "tab_ledger");
      case 5: return I18n.get(lang, "tab_market");
      case 6: return I18n.get(lang, "tab_news");
      default: return tabs[idx];
    }
  }
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
    "👋 Hi! I'm Maya, your Chief Dispatcher.\nFirst, tap 'Dispatch →' at the bottom to pick up your first rider!",
    "Nice! Car #1 is driving along the road.\nTap '⚡ Complete Trip' below or watch it reach its destination.",
    "Awesome! Next, let's expand your fleet.\nTap 'Open Fleet →' below and buy a 2nd vehicle.",
    "More stations bring bigger fares!\nOpen 'Hubs' and unlock a 3rd station in your city.",
    "Great! Now upgrade any open station in 'Hubs' to Level 2 for +15% fares.",
    "Let's hire help! Open 'Team' and recruit a Manager to automate upkeep.",
    "Final lesson: Open 'Market' and buy 10 shares in a rival company.",
    "🎓 Test level complete! +" + Game.money(5000L) + " bonus awarded.\nYou're ready to build your mobility empire!"
  };

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    savedMapState = state == null ? null : state.getBundle("streetMap");
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    night = getPreferences(0).getBoolean("night", true);
    sound = getPreferences(0).getBoolean("sound", true);
    music = getPreferences(0).getBoolean("music", sound);
    String savedLang = getPreferences(0).getString("lang", null);
    soundtrack = new BackgroundMusic(this, music);
    soundtrack.setSfxEnabled(sound);
    soundtrack.setAutoRotate(getPreferences(0).getBoolean("radioAuto", true));
    int savedTrack = getPreferences(0).getInt("trackIndex", 0);
    if (savedTrack != 0) soundtrack.selectTrack(savedTrack);
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
    if (savedLang != null) game.setLanguage(savedLang);
    long comebackBonus = game.evaluateDailyAndOfflineComeback(System.currentTimeMillis());
    if (restored && comebackBonus > 0) {
      game.message =
          "🔥 Welcome back! Day "
              + game.dailyStreak
              + " streak & +"
              + Game.money(comebackBonus)
              + " bonus ready in Lv/Medals!";
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
      if (game != null && game.annualMeetingPending && !modalOpen() && !home) {
        game.annualMeetingPending = false;
        showAnnualMeetingModal();
      }
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
    v.setTextSize(14);
    v.setTextColor(INK);
    v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    v.setAllCaps(false);
    v.setMinHeight(dp(48));
    v.setMinimumWidth(0);
    v.setPadding(dp(12), dp(8), dp(12), dp(8));
    v.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x30ffffff), shape(0xff1e314b, 12), null));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.setMargins(0, dp(6), 0, 0);
    v.setLayoutParams(p);
    v.setOnClickListener(view -> action.run());
    return v;
  }

  LinearLayout buttonRow(Button first, Button second) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1f);
    a.setMargins(0, dp(6), dp(4), 0);
    first.setLayoutParams(a);
    row.addView(first);
    if (second != null) {
      LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, -2, 1f);
      b.setMargins(dp(4), dp(6), 0, 0);
      second.setLayoutParams(b);
      row.addView(second);
    }
    return row;
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
    c.addView(text(heading, 18, INK, true));
    if (description != null && !description.isEmpty()) {
      TextView desc = text(description, 14, MUTED, false);
      LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(-1, -2);
      d.topMargin = dp(6);
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
    header.setPadding(dp(10), dp(6), dp(10), dp(6));
    header.setBackground(medallionShape(0xff162740, 0xff0d1829, 0x55edbb73, 0));
    ImageView mark = new ImageView(this);
    mark.setImageDrawable(BrandArt.crestDrawable(0xff14253b, AMBER, density));
    mark.setContentDescription("Fareforge");
    header.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));
    LinearLayout heading = vertical();
    heading.setPadding(dp(9), 0, dp(4), 0);
    title = text("CITY OPERATIONS", 16, INK, true);
    title.setSingleLine(true);
    title.setEllipsize(android.text.TextUtils.TruncateAt.END);
    metrics = text("", 12, MINT, true);
    metrics.setSingleLine(true);
    metrics.setEllipsize(android.text.TextUtils.TruncateAt.END);
    metrics.setPadding(0, dp(1), 0, 0);
    heading.addView(title);
    heading.addView(metrics);
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
    speedButton.setBackground(medallionShape(0xff243b59, 0xff16263b, AMBER, 20));
    speedButton.setTextColor(AMBER);
    header.addView(speedButton, new LinearLayout.LayoutParams(dp(46), dp(40)));
    Button menu = button("Menu", () -> showHome(true));
    menu.setBackground(medallionShape(0xff213652, 0xff142338, 0x88edbb73, 12));
    LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(60), dp(40));
    mp.leftMargin = dp(6);
    header.addView(menu, mp);
    root.addView(header);

    HorizontalScrollView tickerScroll = new HorizontalScrollView(this);
    tickerScroll.setHorizontalScrollBarEnabled(false);
    tickerScroll.setBackgroundColor(0xff091220);
    cityTickerRow = new LinearLayout(this);
    cityTickerRow.setGravity(Gravity.CENTER_VERTICAL);
    cityTickerRow.setPadding(dp(10), dp(3), dp(10), dp(4));
    cityTickerRow.setVisibility(View.GONE);
    tickerScroll.addView(cityTickerRow);
    root.addView(tickerScroll, new LinearLayout.LayoutParams(-1, -2));

    LinearLayout content = new LinearLayout(this);
    wideLayout = getResources().getConfiguration().screenWidthDp >= 720;
    content.setOrientation(wideLayout ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
    root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
    worldPane = new FrameLayout(this);
    map = new WorldMapView(this, savedMapState);
    map.bind(game, index -> panels.showHub(index));
    map.setNight(night);
    worldPane.addView(map, new FrameLayout.LayoutParams(-1, -1));
    LinearLayout toolsWrapper = new LinearLayout(this);
    toolsWrapper.setOrientation(LinearLayout.HORIZONTAL);
    toolsWrapper.setGravity(Gravity.CENTER_VERTICAL);

    final LinearLayout toolsDrawer = new LinearLayout(this);
    toolsDrawer.setOrientation(LinearLayout.VERTICAL);
    toolsDrawer.setGravity(Gravity.CENTER_HORIZONTAL);
    toolsDrawer.setPadding(dp(5), dp(6), dp(5), dp(6));
    toolsDrawer.setBackground(medallionShape(0xee142337, 0xee0b1420, 0x77edbb73, 14));

    fitButton = button("Fit", () -> map.resetCamera());
    fitButton.setTextSize(11);
    fitButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x77edbb73, 12));
    LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(dp(56), dp(34));
    fp.bottomMargin = dp(4);
    toolsDrawer.addView(fitButton, fp);

    streetButton =
        button(
            "Streets",
            () -> {
              map.toggleStreets();
              updateHud(true);
            });
    streetButton.setTextSize(11);
    streetButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x77edbb73, 12));
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(56), dp(34));
    sp.bottomMargin = dp(4);
    toolsDrawer.addView(streetButton, sp);

    autoButton =
        button(
            "Auto",
            () -> {
              game.autoDispatch = !game.autoDispatch;
              dispatchCollapsed = game.autoDispatch;
              game.message = "Auto dispatch " + (game.autoDispatch ? "enabled" : "disabled") + ".";
              actionDone();
            });
    autoButton.setTextSize(11);
    autoButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x7777d7c8, 12));
    LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(56), dp(34));
    ap.bottomMargin = dp(4);
    toolsDrawer.addView(autoButton, ap);

    goalsButton = button("Goals", () -> panels.overview());
    goalsButton.setTextSize(11);
    goalsButton.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x99edbb73, 12));
    LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(dp(56), dp(34));
    gp.bottomMargin = dp(4);
    toolsDrawer.addView(goalsButton, gp);

    Button trackSkip =
        button(
            "🎵",
            () -> {
              soundtrack.nextTrack();
              getPreferences(0).edit().putInt("trackIndex", soundtrack.currentTrackIndex()).apply();
              showGameNotification("🎵 " + soundtrack.currentTrackName());
            });
    trackSkip.setTextSize(13);
    trackSkip.setContentDescription("Switch soundtrack");
    trackSkip.setBackground(medallionShape(0xee1c304b, 0xee111f33, 0x77edbb73, 12));
    LinearLayout.LayoutParams tpBtn = new LinearLayout.LayoutParams(dp(56), dp(34));
    toolsDrawer.addView(trackSkip, tpBtn);

    toolsWrapper.addView(toolsDrawer);
    toolsDrawer.setVisibility(View.GONE);

    final Button toolsToggleBtn = button("▶", null);
    toolsToggleBtn.setTextSize(11);
    toolsToggleBtn.setPadding(dp(2), dp(8), dp(2), dp(8));
    toolsToggleBtn.setBackground(medallionShape(0xee1a2c42, 0xee0e1927, 0x99edbb73, 10));
    LinearLayout.LayoutParams tglLp = new LinearLayout.LayoutParams(dp(20), dp(58));
    tglLp.leftMargin = dp(2);
    toolsToggleBtn.setLayoutParams(tglLp);
    toolsToggleBtn.setOnClickListener(
        v -> {
          boolean willHide = (toolsDrawer.getVisibility() == View.VISIBLE);
          toolsDrawer.setVisibility(willHide ? View.GONE : View.VISIBLE);
          toolsToggleBtn.setText(willHide ? "▶" : "◀");
        });

    final float[] touchStartX = new float[1];
    toolsDrawer.setOnTouchListener(
        (v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartX[0] = event.getX();
            return false;
          } else if (event.getAction() == MotionEvent.ACTION_UP
              || event.getAction() == MotionEvent.ACTION_CANCEL) {
            float dx = event.getX() - touchStartX[0];
            if (dx < -dp(20)) {
              toolsDrawer.setVisibility(View.GONE);
              toolsToggleBtn.setText("▶");
              return true;
            }
          }
          return false;
        });

    toolsWrapper.addView(toolsToggleBtn);
    FrameLayout.LayoutParams tp =
        new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.CENTER_VERTICAL);
    tp.leftMargin = dp(4);
    worldPane.addView(toolsWrapper, tp);

    notificationBanner = text("", 12, INK, true);
    notificationBanner.setVisibility(View.GONE);
    notificationBanner.setPadding(dp(14), dp(8), dp(14), dp(8));
    notificationBanner.setBackground(medallionShape(0xf2182b46, 0xf20f1b2d, 0xaaedbb73, 14));
    notificationBanner.setElevation(dp(8));
    FrameLayout.LayoutParams nlp =
        new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    nlp.topMargin = dp(46);
    worldPane.addView(notificationBanner, nlp);

    goalBox = vertical();
    goalBox.setPadding(dp(13), dp(10), dp(13), dp(10));
    goalBox.setBackground(medallionShape(0xf2192b45, 0xf2101d31, 0xaaedbb73, 18));
    goalBox.setElevation(dp(7));
    LinearLayout advisorTopRow = new LinearLayout(this);
    advisorTopRow.setGravity(Gravity.CENTER_VERTICAL);
    advisorAvatar = new ImageView(this);
    advisorAvatar.setImageDrawable(BrandArt.advisorPortraitDrawable(density));
    LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(44), dp(44));
    avLp.rightMargin = dp(10);
    advisorTopRow.addView(advisorAvatar, avLp);
    LinearLayout advisorHeadCol = vertical();
    goalEyebrow = text("CHIEF DISPATCHER MAYA", 10, AMBER, true);
    goalEyebrow.setLetterSpacing(.10f);
    advisorHeadCol.addView(goalEyebrow);
    goalTitle = text("YOUR COMPANY", 17, INK, true);
    goalTitle.setPadding(0, dp(2), 0, 0);
    advisorHeadCol.addView(goalTitle);
    advisorTopRow.addView(advisorHeadCol, new LinearLayout.LayoutParams(0, -2, 1));
    goalCollapseButton = button("+", () -> toggleGoalBox());
    goalCollapseButton.setMinHeight(dp(28));
    goalCollapseButton.setPadding(dp(6), dp(2), dp(6), dp(2));
    LinearLayout.LayoutParams collapseLp = new LinearLayout.LayoutParams(dp(40), dp(28));
    collapseLp.setMargins(dp(6), 0, 0, 0);
    goalCollapseButton.setLayoutParams(collapseLp);
    advisorTopRow.addView(goalCollapseButton);
    advisorTopRow.setOnClickListener(v -> toggleGoalBox());
    goalBox.addView(advisorTopRow);

    goalBody = vertical();
    goalBody.setVisibility(View.GONE);
    goalDetail = text("", 14, INK, false);
    goalDetail.setPadding(0, dp(6), 0, 0);
    goalBody.addView(goalDetail);
    campaignGoalText = text("", 12, MINT, true);
    campaignGoalText.setPadding(0, dp(4), 0, 0);
    goalBody.addView(campaignGoalText);
    chapterProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    chapterProgress.setMax(100);
    chapterProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(AMBER));
    chapterProgress.setProgressBackgroundTintList(
        android.content.res.ColorStateList.valueOf(0xff2a3e57));
    LinearLayout.LayoutParams progressLayout = new LinearLayout.LayoutParams(-1, dp(6));
    progressLayout.topMargin = dp(7);
    goalBody.addView(chapterProgress, progressLayout);
    goalAction = button("Next step", () -> goalPressed());
    primary(goalAction);
    goalBody.addView(goalAction);
    goalBox.addView(goalBody);
    FrameLayout.LayoutParams goalParams =
        new FrameLayout.LayoutParams(
            dp(Math.min(340, getResources().getConfiguration().screenWidthDp - 24)),
            -2,
            Gravity.TOP | Gravity.START);
    goalParams.setMargins(dp(12), dp(8), dp(12), 0);
    worldPane.addView(goalBox, goalParams);
    dispatch = vertical();
    dispatch.setPadding(dp(8), dp(4), dp(8), dp(4));
    FrameLayout.LayoutParams deck = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
    deck.bottomMargin = dp(6);
    worldPane.addView(dispatch, deck);
    mapCaption = text("Connecting street map…", 10, MUTED, false);
    mapCaption.setVisibility(View.GONE);
    worldPane.addView(mapCaption, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START));
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
    message = text("", 12, MUTED, false);
    message.setMaxLines(1);
    message.setEllipsize(android.text.TextUtils.TruncateAt.END);
    message.setPadding(dp(12), dp(3), dp(12), dp(3));
    message.setVisibility(View.GONE);
    root.addView(message);
    nav = new LinearLayout(this);
    nav.setPadding(dp(6), dp(4), dp(6), dp(4));
    for (int i = 0; i < tabs.length; i++) {
      final int index = i;
      Button item = button(tabLabel(i), () -> selectTab(index));
      item.setTextSize(10);
      item.setPadding(dp(2), dp(4), dp(2), dp(4));
      item.setCompoundDrawablePadding(dp(2));
      item.setCompoundDrawablesWithIntrinsicBounds(
          null, BrandArt.icon(i, MUTED, getResources().getDisplayMetrics().density), null, null);
      LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0, dp(52), 1);
      np.setMargins(dp(2), 0, dp(2), 0);
      nav.addView(item, np);
    }
    root.addView(nav);
  }

  private boolean tutorialTabRelevant(int tabIdx) {
    if (game == null || game.gameMode != Game.MODE_TUTORIAL || game.tutorialStep >= 8) return true;
    int maxTab = new int[] {0, 0, 0, 1, 2, 2, 3, 5}[Math.max(0, Math.min(7, game.tutorialStep))];
    return tabIdx <= maxTab;
  }

  private int unreadReportCount() {
    int count = 0;
    if (game != null) for (Game.Report r : game.reports) if (!r.read) count++;
    return count;
  }

  private void refreshNavTabs() {
    int unread = unreadReportCount();
    for (int i = 0; i < nav.getChildCount(); i++) {
      Button b = (Button) nav.getChildAt(i);
      String label = tabLabel(i);
      if (i == 6 && unread > 0 && (game == null || game.gameMode != Game.MODE_TUTORIAL))
        label = label + " (" + unread + ")";
      b.setText(label);
      b.setTextColor(i == tab ? BG : MUTED);
      b.setBackground(shape(i == tab ? AMBER : BG, 13));
      b.setAlpha(tutorialTabRelevant(i) ? 1f : 0.42f);
      b.setCompoundDrawablesWithIntrinsicBounds(
          null,
          BrandArt.icon(i, i == tab ? BG : MUTED, getResources().getDisplayMetrics().density),
          null,
          null);
    }
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
    refreshNavTabs();
    if (tab > 0) renderManagement();
    requestKey = "";
    updateHud(true);
  }

  private void updateHud(boolean force) {
    if (game.gameMode == Game.MODE_TUTORIAL && game.tutorialStep < 8 && game.tutorialReady()) {
      if (game.advanceTutorial()) {
        force = true;
        if (sound) soundtrack.playCue(BackgroundMusic.CUE_FANFARE);
      }
    }
    String cityDisplay =
        game.currentScenario().city
            + (game.branchCount() > 1 ? " ▾ (" + game.branchCount() + " Cities)" : "");
    String dateBadge =
        game.monthName() + " " + game.year() + (tab == 0 ? "" : " · ⏸ Planning");
    title.setText(getString(R.string.hud_title, cityDisplay, dateBadge));
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
    message.setVisibility(saveError != null ? View.VISIBLE : View.GONE);
    updateGoal();
    refreshNavTabs();
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
    boolean inTutorial = game.gameMode == Game.MODE_TUTORIAL && game.tutorialStep < 8;
    if (autoButton != null) {
      autoButton.setVisibility(inTutorial ? View.GONE : View.VISIBLE);
      autoButton.setText(game.autoDispatch ? "Auto on" : "Manual");
      autoButton.setTextColor(game.autoDispatch ? MINT : INK);
    }
    if (streetButton != null) {
      streetButton.setVisibility(inTutorial ? View.GONE : View.VISIBLE);
      streetButton.setText(map.isUsingStreets() ? "Streets" : "Offline");
    }
    if (goalsButton != null) {
      goalsButton.setText(
          game.unclaimedCareerRewards > 0
              ? ("Goals 🎁 Lv" + game.founderLevel)
              : ("Goals · Lv" + game.founderLevel + " (" + game.unlockedMedalCount() + "/12)"));
    }
    boolean showTicker = !inTutorial && (game.branchCount() > 1 || !game.ventures.isEmpty());
    String tKey =
        showTicker
            + ":"
            + game.scenarioId
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
      cityTickerRow.setVisibility(showTicker ? View.VISIBLE : View.GONE);
      cityTickerRow.removeAllViews();
      if (showTicker) {
        Button ceoPill =
            button(
                "👤 "
                    + game.founderName
                    + ": "
                    + Game.money(game.founderCash)
                    + " · CEO: "
                    + game.activeFounderCeoCompanyName(),
                () -> panels.overview());
        ceoPill.setTextSize(11);
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
          cityBtn.setTextSize(11);
          cityBtn.setMinHeight(dp(28));
          cityBtn.setPadding(dp(9), dp(2), dp(9), dp(2));
          if (active) primary(cityBtn);
          else cityBtn.setBackground(medallionShape(0xff182b44, 0xff0f1c2e, 0x5577d7c8, 10));
          LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, dp(28));
          bp.setMargins(0, 0, dp(5), 0);
          cityTickerRow.addView(cityBtn, bp);
        }
      }
    }
    String key = game.requests.toString() + ":" + game.autoDispatch + ":" + dispatchCollapsed;
    if (tab == 0 && (force || !key.equals(requestKey))) {
      requestKey = key;
      renderDispatch();
    }
    if (news != null && !home && !inTutorial) {
      news.collect(game);
      Game.Report pending = null;
      for (Game.Report r : game.reports)
        if (!r.read && (pending == null || r.urgent)) {
          pending = r;
          if (r.urgent) break;
        }
      if (pending != null && !pending.id.equals(lastNewsNoticeId)) {
        lastNewsNoticeId = pending.id;
        lastEdition = game.elapsed;
        game.message = "📰 News: " + pending.title + " (Read in News tab)";
        showGameNotification(game.message);
        refreshNavTabs();
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
    boolean inTutorial = game.gameMode == Game.MODE_TUTORIAL;
    if (goalEyebrow != null) {
      goalEyebrow.setText(
          inTutorial
              ? (game.tutorialStep < 8
                  ? "CHIEF DISPATCHER MAYA · STEP " + game.tutorialStep + " OF 7"
                  : "CHIEF DISPATCHER MAYA · GRADUATED")
              : "ADVISOR · MISSION SCROLL");
    }
    if (campaignGoalText != null) {
      campaignGoalText.setVisibility(inTutorial ? View.GONE : View.VISIBLE);
      campaignGoalText.setText("Victory Goal: " + game.objectiveText());
    }
    if (inTutorial) {
      goalTitle.setText(
          game.tutorialStep < 8
              ? getString(R.string.academy_step, game.tutorialStep)
              : getString(R.string.academy_graduate));
      goalDetail.setText(tasks[Math.min(8, game.tutorialStep)]);
      if (game.tutorialStep == 8) {
        goalAction.setText("Start Free Play →");
      } else if (game.tutorialReady()) {
        goalAction.setText("Complete Step ✓");
      } else if (game.tutorialStep == 1) {
        goalAction.setText("Dispatch First Car →");
      } else if (game.tutorialStep == 2) {
        goalAction.setText("⚡ Complete Trip →");
      } else {
        String targetTabName = tabLabel(new int[] {0, 0, 0, 1, 2, 2, 3, 4}[game.tutorialStep]);
        goalAction.setText("Open " + targetTabName + " →");
      }
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
    boolean hasReadyUpdate =
        inTutorial
            ? (game.tutorialReady() || game.tutorialStep == 8)
            : (game.chapter < 6 ? chapterValue() >= targets[game.chapter] : game.scenarioWon);
    if (hasReadyUpdate) goalHasUnread = true;
    if (goalCollapseButton != null) {
      if (goalCollapsed) {
        goalCollapseButton.setText(goalHasUnread ? "+ ●" : "+");
        if (goalHasUnread && goalEyebrow != null) {
          goalEyebrow.setText(goalEyebrow.getText() + "  ●  UPDATE");
        }
      } else {
        goalCollapseButton.setText("–");
      }
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
        return;
      }
      if (game.tutorialStep == 1 && !game.requests.isEmpty()) {
        game.dispatch(game.requests.get(0).id);
        actionDone();
        return;
      }
      if (game.tutorialStep == 2) {
        for (int i = 0; i < 120 && !game.tutorialReady(); i++) game.tick(0.25);
        actionDone();
        return;
      }
      selectTab(new int[] {0, 0, 0, 1, 2, 2, 3, 4}[game.tutorialStep]);
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

    LinearLayout deckHeader = new LinearLayout(this);
    deckHeader.setGravity(Gravity.CENTER_VERTICAL);
    deckHeader.setPadding(dp(10), dp(4), dp(8), dp(4));
    deckHeader.setBackground(medallionShape(0xe8132236, 0xe80d1828, 0x6677d7c8, 12));
    String headerLabel =
        game.autoDispatch
            ? ("⚡ AUTO-DISPATCH  ·  "
                + game.requests.size()
                + " queued  ·  "
                + game.idleCount()
                + " idle")
            : ("DISPATCH BOARD  ·  " + game.requests.size() + " WAITING");
    TextView deckTitle = text(headerLabel, 11, MINT, true);
    deckTitle.setLetterSpacing(.08f);
    deckTitle.setSingleLine(true);
    deckTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
    deckHeader.addView(deckTitle, new LinearLayout.LayoutParams(0, -2, 1));

    Button deckToggle =
        button(
            dispatchCollapsed ? "+" : "–",
            () -> {
              dispatchCollapsed = !dispatchCollapsed;
              updateHud(true);
            });
    deckToggle.setTextSize(12);
    deckToggle.setMinHeight(dp(24));
    deckToggle.setPadding(dp(6), 0, dp(6), 0);
    deckHeader.addView(deckToggle, new LinearLayout.LayoutParams(dp(34), dp(24)));
    deckHeader.setOnClickListener(
        v -> {
          dispatchCollapsed = !dispatchCollapsed;
          updateHud(true);
        });
    LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1, -2);
    hLp.bottomMargin = dispatchCollapsed ? 0 : dp(5);
    dispatch.addView(deckHeader, hLp);

    if (dispatchCollapsed) return;

    HorizontalScrollView scroll = new HorizontalScrollView(this);
    scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout row = new LinearLayout(this);
    scroll.addView(row);
    dispatch.addView(scroll);
    if (game.requests.isEmpty()) {
      LinearLayout ready = vertical();
      ready.setPadding(dp(12), dp(9), dp(12), dp(9));
      ready.setBackground(shape(0xee142238, 14));
      ready.addView(
          text(
              game.autoDispatch
                  ? "Your network is working."
                  : "The next opportunity is on its way.",
              14,
              INK,
              true));
      ready.addView(
          text(
              game.idleCount() + " available vehicles · " + game.rides + " completed trips",
              12,
              MUTED,
              false));
      row.addView(ready, new LinearLayout.LayoutParams(-1, -2));
      return;
    }
    for (Game.Ride ride : new ArrayList<>(game.requests)) {
      LinearLayout ticket = vertical();
      ticket.setPadding(dp(10), dp(8), dp(10), dp(8));
      ticket.setBackground(medallionShape(0xf21b2d47, 0xf2111e32, ride.kindColor(), 14));
      LinearLayout badge = new LinearLayout(this);
      badge.setGravity(Gravity.CENTER_VERTICAL);
      TextView category = text(ride.kindName(), 10, ride.kindColor(), true);
      category.setLetterSpacing(.08f);
      badge.addView(category, new LinearLayout.LayoutParams(0, -2, 1));
      long netRev = game.estimatedIncome(ride);
      badge.addView(text("+" + Game.money(netRev), 14, MINT, true));
      ticket.addView(badge);
      TextView origin = text(game.districts.get(ride.from).name, 14, INK, true);
      origin.setPadding(0, dp(3), 0, dp(1));
      origin.setSingleLine(true);
      origin.setEllipsize(android.text.TextUtils.TruncateAt.END);
      ticket.addView(origin);
      TextView destination =
          text(
              "→ "
                  + game.districts.get(ride.to).name
                  + " · "
                  + Game.money(ride.fare),
              12,
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
      pp.topMargin = dp(6);
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
      send.setMinHeight(dp(38));
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
      LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(224), -2);
      p.rightMargin = dp(8);
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
    if (game.gameMode == Game.MODE_TUTORIAL && game.tutorialStep < 8 && game.tutorialReady()) {
      game.advanceTutorial();
      if (sound) soundtrack.playCue(BackgroundMusic.CUE_FANFARE);
      if (tab > 0 && game.tutorialStep < 8) {
        int nextTab = new int[] {0, 0, 0, 1, 2, 2, 3, 4}[game.tutorialStep];
        if (nextTab == 0) selectTab(0);
      }
    }
    updateHud(true);
    map.invalidate();
    if (tab > 0) renderManagement();
    saveAsync();
    int cue = game.pollSoundCue();
    if (sound) {
      soundtrack.playCue(cue != 0 ? cue : BackgroundMusic.CUE_CASH);
    }
    showGameNotification(game.message);
  }

  private void toggleGoalBox() {
    goalCollapsed = !goalCollapsed;
    if (goalBody != null) goalBody.setVisibility(goalCollapsed ? View.GONE : View.VISIBLE);
    goalHasUnread = false;
    if (goalCollapseButton != null) goalCollapseButton.setText(goalCollapsed ? "+" : "–");
    if (goalEyebrow != null) goalEyebrow.setText("CHIEF DISPATCHER MAYA");
    updateHud(true);
  }

  void showGameNotification(String msg) {
    if (msg == null || msg.trim().isEmpty()) return;
    if (notificationBanner != null) {
      notificationBanner.setText(msg);
      notificationBanner.setVisibility(View.VISIBLE);
      notificationBanner.removeCallbacks(hideNotificationRunnable);
      notificationBanner.postDelayed(hideNotificationRunnable, 2800);
    }
  }

  void showAnnualMeetingModal() {
    if (game == null) return;
    LinearLayout body = vertical();
    body.setPadding(dp(14), dp(10), dp(14), dp(10));

    // Investor Confidence Card
    LinearLayout sentCard = vertical();
    sentCard.setPadding(dp(12), dp(10), dp(12), dp(10));
    sentCard.setBackground(medallionShape(0xff182b44, 0xff101e30, 0x66edbb73, 14));
    LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(-1, -2);
    scLp.bottomMargin = dp(10);
    sentCard.setLayoutParams(scLp);

    int approval = game.investorApprovalRating;
    String mood =
        approval >= 75
            ? "★ STRONG BUY · High Investor Confidence"
            : (approval >= 50
                ? "HOLD · Stable Governance"
                : "CONCERNED · Board Demands Capital Efficiency");
    sentCard.addView(text("BOARD OF DIRECTORS & SHAREHOLDERS", 10, AMBER, true));
    sentCard.addView(
        text(
            approval + "% Investor Approval Rating",
            17,
            approval >= 75 ? MINT : (approval >= 50 ? AMBER : 0xffed8d73),
            true));
    sentCard.addView(text(mood, 11, MUTED, false));
    body.addView(sentCard);

    // Performance Ledger Card
    LinearLayout ledCard = vertical();
    ledCard.setPadding(dp(12), dp(10), dp(12), dp(10));
    ledCard.setBackground(medallionShape(0xff16273d, 0xff0f1b2c, 0x5577d7c8, 14));
    LinearLayout.LayoutParams lcLp = new LinearLayout.LayoutParams(-1, -2);
    lcLp.bottomMargin = dp(10);
    ledCard.setLayoutParams(lcLp);

    ledCard.addView(text("COMPANY PERFORMANCE LEDGER", 10, MINT, true));
    ledCard.addView(
        text("Gross Fares: " + game.formatCityMoney(game.lastYearRevenue), 13, INK, false));
    ledCard.addView(
        text("Operating Expenses: " + game.formatCityMoney(game.lastYearExpenses), 13, INK, false));
    long netProfit = game.lastYearProfit;
    ledCard.addView(
        text(
            "Net Operating Profit: "
                + (netProfit >= 0 ? "+" : "")
                + game.formatCityMoney(netProfit),
            14,
            netProfit >= 0 ? MINT : 0xffed8d73,
            true));
    ledCard.addView(
        text("Annual Hauls: " + game.lastYearRides + " trips completed", 12, MUTED, false));
    ledCard.addView(
        text("Company Valuation: " + game.formatCityMoney(game.netWorth()), 13, AMBER, true));
    body.addView(ledCard);

    // Active Fleet Ledger Card
    LinearLayout fleetCard = vertical();
    fleetCard.setPadding(dp(12), dp(10), dp(12), dp(10));
    fleetCard.setBackground(medallionShape(0xff16273d, 0xff0f1b2c, 0x5577d7c8, 14));
    LinearLayout.LayoutParams fcLp = new LinearLayout.LayoutParams(-1, -2);
    fcLp.bottomMargin = dp(10);
    fleetCard.setLayoutParams(fcLp);
    fleetCard.addView(text("FLEET ASSET PERFORMANCE", 10, AMBER, true));
    for (Game.Taxi t : game.taxis) {
      fleetCard.addView(
          text(
              "Consist #"
                  + t.id
                  + " ("
                  + t.tierName()
                  + ") · "
                  + t.tripsCompleted
                  + " trips · "
                  + game.formatCityMoney(t.totalFareEarned)
                  + " earned · "
                  + (int) t.condition
                  + "% Cond",
              11,
              INK,
              false));
    }
    body.addView(fleetCard);

    // Route & Hub Origination Ledger Card
    LinearLayout routeCard = vertical();
    routeCard.setPadding(dp(12), dp(10), dp(12), dp(10));
    routeCard.setBackground(medallionShape(0xff16273d, 0xff0f1b2c, 0x5577d7c8, 14));
    LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(-1, -2);
    rcLp.bottomMargin = dp(10);
    routeCard.setLayoutParams(rcLp);
    routeCard.addView(text("ROUTE & HUB PERFORMANCE", 10, MINT, true));
    for (Game.District d : game.districts) {
      if (!d.open && d.tripsOriginating == 0) continue;
      routeCard.addView(
          text(
              d.name
                  + " ("
                  + d.levelName()
                  + ") · "
                  + d.tripsOriginating
                  + " trips · "
                  + game.formatCityMoney(d.revenueOriginating),
              11,
              INK,
              false));
    }
    body.addView(routeCard);

    // Executive Compensation & Dividend Governance
    LinearLayout govCard = vertical();
    govCard.setPadding(dp(12), dp(10), dp(12), dp(10));
    govCard.setBackground(medallionShape(0xff182b44, 0xff101e30, 0x66edbb73, 14));
    LinearLayout.LayoutParams gcLp = new LinearLayout.LayoutParams(-1, -2);
    gcLp.bottomMargin = dp(10);
    govCard.setLayoutParams(gcLp);
    govCard.addView(text("EXECUTIVE COMPENSATION & DIVIDENDS", 10, AMBER, true));

    final TextView salText =
        text(
            "CEO Salary: " + game.formatCityMoney(game.founderSalaryRate) + " / quarter",
            13,
            INK,
            true);
    govCard.addView(salText);
    LinearLayout salRow = new LinearLayout(this);
    Button cutSal =
        button(
            "Reduce -50",
            () -> {
              game.adjustCeoSalary(-50);
              salText.setText(
                  "CEO Salary: " + game.formatCityMoney(game.founderSalaryRate) + " / quarter");
            });
    cutSal.setTextSize(11);
    Button raiseSal =
        button(
            "Raise +50",
            () -> {
              game.adjustCeoSalary(50);
              salText.setText(
                  "CEO Salary: " + game.formatCityMoney(game.founderSalaryRate) + " / quarter");
            });
    raiseSal.setTextSize(11);
    LinearLayout.LayoutParams sBtnLp = new LinearLayout.LayoutParams(0, dp(34), 1);
    sBtnLp.rightMargin = dp(4);
    salRow.addView(cutSal, sBtnLp);
    salRow.addView(raiseSal, new LinearLayout.LayoutParams(0, dp(34), 1));
    govCard.addView(salRow);

    final TextView divText =
        text(
            "Corporate Dividend: "
                + game.formatCityMoney(game.corporateDividendRate)
                + " / distribution",
            13,
            INK,
            true);
    divText.setPadding(0, dp(6), 0, 0);
    govCard.addView(divText);
    LinearLayout divRow = new LinearLayout(this);
    Button cutDiv =
        button(
            "Retain Cash -50",
            () -> {
              game.adjustCorporateDividend(-50);
              divText.setText(
                  "Corporate Dividend: "
                      + game.formatCityMoney(game.corporateDividendRate)
                      + " / distribution");
            });
    cutDiv.setTextSize(11);
    Button raiseDiv =
        button(
            "Reward Investors +50",
            () -> {
              game.adjustCorporateDividend(50);
              divText.setText(
                  "Corporate Dividend: "
                      + game.formatCityMoney(game.corporateDividendRate)
                      + " / distribution");
            });
    raiseDiv.setTextSize(11);
    LinearLayout.LayoutParams dBtnLp = new LinearLayout.LayoutParams(0, dp(34), 1);
    dBtnLp.rightMargin = dp(4);
    divRow.addView(cutDiv, dBtnLp);
    divRow.addView(raiseDiv, new LinearLayout.LayoutParams(0, dp(34), 1));
    govCard.addView(divRow);

    body.addView(govCard);

    Button adjournBtn =
        button(
            "Ratify Resolutions & Adjourn Meeting ✓",
            () -> {
              dismissCurrent();
              actionDone();
            });
    primary(adjournBtn);
    body.addView(adjournBtn);

    showThemedModal(
        "ANNUAL INVESTORS MEETING",
        "Fiscal Year " + (game.year() - 1) + " Annual Report",
        "Review fleet performance ledger and vote on executive salary and dividend policy.",
        body,
        "Close",
        null);
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
              "Executive Response: Fleet & Hospitality Drive (-" + Game.money(250L) + ")",
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
            "🎵 Tycoon Radio · " + soundtrack.currentTrackName(),
            () -> showRadioModal(canContinue)));
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
    page.addView(
        button(
            "Language / Idioma / 言語 · " + currentLanguageName(),
            () -> showLanguageModal(canContinue)));
    page.addView(
        button(
            "🌐 Convoy Multiplayer & QR Backup (E2EE)",
            () -> showPeerMultiplayerModal()));
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

  private void showRadioModal(boolean canContinue) {
    LinearLayout list = vertical();
    Button nextBtn =
        button(
            "⏭ Skip to Next Track · " + soundtrack.currentTrackName(),
            () -> {
              soundtrack.nextTrack();
              getPreferences(0).edit().putInt("trackIndex", soundtrack.currentTrackIndex()).apply();
              dismissCurrent();
              showRadioModal(canContinue);
            });
    primary(nextBtn);
    list.addView(nextBtn);
    list.addView(
        button(
            soundtrack.isAutoRotate()
                ? "🔁 Auto-Rotate Playlist: ON"
                : "🔁 Auto-Rotate Playlist: OFF (Loop Current)",
            () -> {
              boolean nextVal = !soundtrack.isAutoRotate();
              soundtrack.setAutoRotate(nextVal);
              getPreferences(0).edit().putBoolean("radioAuto", nextVal).apply();
              dismissCurrent();
              showRadioModal(canContinue);
            }));
    for (int i = 0; i < soundtrack.trackCount(); i++) {
      final int idx = i;
      boolean active = (i == soundtrack.currentTrackIndex());
      Button trackBtn =
          button(
              (active ? "▶ " : "🎵 ") + BackgroundMusic.TRACK_NAMES[i],
              () -> {
                soundtrack.selectTrack(idx);
                getPreferences(0).edit().putInt("trackIndex", idx).apply();
                dismissCurrent();
                showRadioModal(canContinue);
              });
      if (active) primary(trackBtn);
      list.addView(trackBtn);
    }
    showThemedModal(
        "TYCOON RADIO  ·  6 SOUNDTRACKS",
        "Select Soundtrack Station",
        "Switch between 6 offline Tycoon Radio tracks inspired by classic transport and city games.",
        list,
        "Back",
        () -> showHome(canContinue));
  }

  void showPeerMultiplayerModal() {
    LinearLayout body = vertical();

    // 1. Fareforge Convoy Multiplayer Lobby Section
    LinearLayout multiCard = vertical();
    multiCard.setPadding(dp(14), dp(12), dp(14), dp(12));
    multiCard.setBackground(shape(0xff182b44, 14));
    multiCard.addView(
        text("🌐 FAREFORGE CONVOY MULTIPLAYER", 15, AMBER, true));
    multiCard.addView(
        text(
            "Host or join an offline-first E2EE tycoon match. Share a QR Room Pass with a friend to import their real company into your stock market!",
            12,
            MUTED,
            false));

    Button hostBtn =
        button(
            "⚡ Create Room & Export My Company Envelope",
            () -> {
              try {
                String code = PeerSyncCrypto.generateRoomCode();
                String envelope = PeerMultiplayerSync.exportMultiplayerRoomEnvelope(game, code);
                showExportEnvelopeModal("MULTIPLAYER ROOM ENVELOPE", code, envelope);
              } catch (Exception e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
              }
            });
    primary(hostBtn);
    multiCard.addView(hostBtn);

    Button joinBtn =
        button(
            "📥 Join Room & Import Friend's Company",
            () -> showImportRoomModal());
    multiCard.addView(joinBtn);
    body.addView(multiCard);

    // 2. Serverless Encrypted Cloud Backup Section
    LinearLayout backupCard = vertical();
    backupCard.setPadding(dp(14), dp(12), dp(14), dp(12));
    backupCard.setBackground(shape(0xff111e32, 14));
    LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(-1, -2);
    bLp.topMargin = dp(10);
    backupCard.addView(
        text("🔐 SERVERLESS E2EE CLOUD BACKUP", 15, MINT, true));
    backupCard.addView(
        text(
            "Zero central servers. Back up your entire empire with AES-GCM-256 encryption. Restore across any phone using your passphrase.",
            12,
            MUTED,
            false));

    Button exportBackupBtn =
        button(
            "📤 Export Encrypted Save Backup",
            () -> showCreateBackupPassphraseModal());
    backupCard.addView(exportBackupBtn);

    Button importBackupBtn =
        button(
            "📥 Restore Encrypted Save Backup",
            () -> showRestoreBackupModal());
    backupCard.addView(importBackupBtn);
    body.addView(backupCard, bLp);

    showThemedModal(
        "FAREFORGE CONVOY & E2EE QR SYNC",
        "Encrypted Zero-Server Exchange",
        "Uses Fareforge Convoy AES-256-GCM encryption & QR passes. No accounts, no central servers, 100% private.",
        body,
        "Close",
        null);
  }

  private void showExportEnvelopeModal(String title, String roomCode, String envelope) {
    LinearLayout body = vertical();
    body.addView(text("CONVOY ROOM CODE: " + roomCode, 13, AMBER, true));

    String qrPayload =
        roomCode != null && !roomCode.isEmpty()
            ? QrCodeHelper.packRoomPayload(roomCode, envelope)
            : envelope;
    try {
      Bitmap qrBmp = QrCodeHelper.encodeQr(qrPayload, dp(220));
      ImageView qrView = new ImageView(this);
      qrView.setImageBitmap(qrBmp);
      qrView.setBackgroundColor(0xffffffff);
      qrView.setPadding(dp(10), dp(10), dp(10), dp(10));
      LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(dp(220), dp(220));
      qp.gravity = Gravity.CENTER_HORIZONTAL;
      qp.topMargin = dp(10);
      qp.bottomMargin = dp(10);
      body.addView(qrView, qp);
      body.addView(
          text(
              "Have your friend tap 'Scan QR Code with Camera' in Join Room to import instantly.",
              11,
              MINT,
              false));
    } catch (Exception e) {
      body.addView(text("QR generation unavailable: " + e.getMessage(), 11, AMBER, false));
    }

    Button copyBtn =
        button(
            "📋 Copy Encrypted Pass (Fallback)",
            () -> {
              android.content.ClipboardManager cb =
                  (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
              if (cb != null) {
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Fareforge Convoy", qrPayload));
                Toast.makeText(this, "Copied Convoy Pass!", Toast.LENGTH_SHORT).show();
              }
            });
    primary(copyBtn);
    body.addView(copyBtn);

    showThemedModal(
        title,
        "Room: " + roomCode,
        "Your friend scans this QR code with their mobile camera to compete against your company.",
        body,
        "Done",
        null);
  }

  private Runnable pendingCameraScanAction;
  private static final int REQ_CAMERA_QR = 7041;

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQ_CAMERA_QR) {
      if (grantResults.length > 0
          && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
          && pendingCameraScanAction != null) {
        Runnable run = pendingCameraScanAction;
        pendingCameraScanAction = null;
        run.run();
      } else {
        pendingCameraScanAction = null;
        Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT)
            .show();
      }
    }
  }

  @SuppressWarnings("MissingPermission")
  private void openQrScannerDialog(
      EditText codeInput, EditText envInput, Runnable onAutoJoin) {
    if (checkSelfPermission(android.Manifest.permission.CAMERA)
        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
      pendingCameraScanAction = () -> openQrScannerDialog(codeInput, envInput, onAutoJoin);
      requestPermissions(new String[] {android.Manifest.permission.CAMERA}, REQ_CAMERA_QR);
      return;
    }

    final android.hardware.camera2.CameraManager camMgr =
        (android.hardware.camera2.CameraManager) getSystemService(Context.CAMERA_SERVICE);
    String backCamId = null;
    try {
      for (String id : camMgr.getCameraIdList()) {
        Integer facing =
            camMgr
                .getCameraCharacteristics(id)
                .get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
        if (facing != null
            && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
          backCamId = id;
          break;
        }
      }
      if (backCamId == null && camMgr.getCameraIdList().length > 0) {
        backCamId = camMgr.getCameraIdList()[0];
      }
    } catch (Exception e) {
      Toast.makeText(this, "No camera available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
      return;
    }
    if (backCamId == null) {
      Toast.makeText(this, "No camera found on this device.", Toast.LENGTH_SHORT).show();
      return;
    }

    final HandlerThread bgThread = new HandlerThread("ConvoyQrCam");
    bgThread.start();
    final Handler bgHandler = new Handler(bgThread.getLooper());
    final android.hardware.camera2.CameraDevice[] camHolder =
        new android.hardware.camera2.CameraDevice[1];
    final android.hardware.camera2.CameraCaptureSession[] sessionHolder =
        new android.hardware.camera2.CameraCaptureSession[1];
    final android.hardware.camera2.CaptureRequest.Builder[] reqHolder =
        new android.hardware.camera2.CaptureRequest.Builder[1];
    final android.media.ImageReader[] readerHolder = new android.media.ImageReader[1];
    final boolean[] handled = {false};
    final boolean[] torchOn = {false};

    LinearLayout scanBox = vertical();
    scanBox.addView(text("Point camera at friend's Fareforge Convoy QR code", 12, MINT, true));
    TextureView preview = new TextureView(this);
    LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(260), dp(260));
    tp.gravity = Gravity.CENTER_HORIZONTAL;
    tp.topMargin = dp(8);
    scanBox.addView(preview, tp);

    final Button torchBtn = button("🔦 Torch: Off", null);
    torchBtn.setOnClickListener(
        v -> {
          if (sessionHolder[0] == null || reqHolder[0] == null) return;
          torchOn[0] = !torchOn[0];
          torchBtn.setText(torchOn[0] ? "🔦 Torch: On" : "🔦 Torch: Off");
          try {
            reqHolder[0].set(
                android.hardware.camera2.CaptureRequest.FLASH_MODE,
                torchOn[0]
                    ? android.hardware.camera2.CaptureRequest.FLASH_MODE_TORCH
                    : android.hardware.camera2.CaptureRequest.FLASH_MODE_OFF);
            sessionHolder[0].setRepeatingRequest(reqHolder[0].build(), null, bgHandler);
          } catch (Exception ignored) {
          }
        });
    LinearLayout.LayoutParams tbp = new LinearLayout.LayoutParams(-1, -2);
    tbp.topMargin = dp(8);
    scanBox.addView(torchBtn, tbp);

    final AlertDialog scanDialog =
        new AlertDialog.Builder(this)
            .setTitle("📷 Scan Convoy QR Code")
            .setView(scanBox)
            .setNegativeButton("Cancel", null)
            .create();
    scanDialog.setOnDismissListener(
        d -> {
          if (camHolder[0] != null) {
            try {
              camHolder[0].close();
            } catch (Exception ignored) {
            }
          }
          if (readerHolder[0] != null) {
            try {
              readerHolder[0].close();
            } catch (Exception ignored) {
            }
          }
          bgThread.quitSafely();
        });

    final String selectedCamId = backCamId;
    preview.setSurfaceTextureListener(
        new TextureView.SurfaceTextureListener() {
          @Override
          public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
            try {
              st.setDefaultBufferSize(640, 480);
              Surface previewSurface = new Surface(st);
              android.media.ImageReader reader =
                  android.media.ImageReader.newInstance(
                      640, 480, android.graphics.ImageFormat.YUV_420_888, 2);
              readerHolder[0] = reader;
              reader.setOnImageAvailableListener(
                  r -> {
                    android.media.Image img = r.acquireLatestImage();
                    if (img == null) return;
                    try {
                      if (handled[0]) return;
                      android.media.Image.Plane yPlane = img.getPlanes()[0];
                      java.nio.ByteBuffer buf = yPlane.getBuffer();
                      int w = img.getWidth();
                      int h = img.getHeight();
                      int rowStride = yPlane.getRowStride();
                      byte[] yData = new byte[w * h];
                      if (rowStride == w) {
                        buf.get(yData, 0, w * h);
                      } else {
                        byte[] row = new byte[rowStride];
                        for (int y = 0; y < h; y++) {
                          buf.position(y * rowStride);
                          buf.get(row, 0, Math.min(rowStride, buf.remaining()));
                          System.arraycopy(row, 0, yData, y * w, w);
                        }
                      }
                      String decoded = QrCodeHelper.decodeYuv(yData, w, h);
                      if (decoded != null && !decoded.isEmpty()) {
                        handled[0] = true;
                        runOnUiThread(
                            () -> {
                              scanDialog.dismiss();
                              String[] unpacked = QrCodeHelper.unpackRoomPayload(decoded);
                              if (unpacked != null) {
                                codeInput.setText(unpacked[0]);
                                envInput.setText(unpacked[1]);
                                onAutoJoin.run();
                              } else {
                                envInput.setText(decoded);
                                Toast.makeText(
                                        MainActivity.this,
                                        "QR scanned! Enter Room Code & tap Import.",
                                        Toast.LENGTH_SHORT)
                                    .show();
                              }
                            });
                      }
                    } finally {
                      img.close();
                    }
                  },
                  bgHandler);

              camMgr.openCamera(
                  selectedCamId,
                  new android.hardware.camera2.CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(android.hardware.camera2.CameraDevice camera) {
                      camHolder[0] = camera;
                      try {
                        android.hardware.camera2.CaptureRequest.Builder req =
                            camera.createCaptureRequest(
                                android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW);
                        reqHolder[0] = req;
                        req.addTarget(previewSurface);
                        req.addTarget(reader.getSurface());
                        req.set(
                            android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                            android.hardware.camera2.CaptureRequest
                                .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        camera.createCaptureSession(
                            Arrays.asList(previewSurface, reader.getSurface()),
                            new android.hardware.camera2.CameraCaptureSession.StateCallback() {
                              @Override
                              public void onConfigured(
                                  android.hardware.camera2.CameraCaptureSession session) {
                                sessionHolder[0] = session;
                                try {
                                  session.setRepeatingRequest(req.build(), null, bgHandler);
                                } catch (Exception ignored) {
                                }
                              }

                              @Override
                              public void onConfigureFailed(
                                  android.hardware.camera2.CameraCaptureSession session) {}
                            },
                            bgHandler);
                      } catch (Exception ignored) {
                      }
                    }

                    @Override
                    public void onDisconnected(android.hardware.camera2.CameraDevice camera) {
                      camera.close();
                    }

                    @Override
                    public void onError(android.hardware.camera2.CameraDevice camera, int error) {
                      camera.close();
                    }
                  },
                  bgHandler);
            } catch (Exception e) {
              runOnUiThread(
                  () -> {
                    scanDialog.dismiss();
                    Toast.makeText(
                            MainActivity.this,
                            "Camera error: " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                        .show();
                  });
            }
          }

          @Override
          public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}

          @Override
          public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            return true;
          }

          @Override
          public void onSurfaceTextureUpdated(SurfaceTexture st) {}
        });
    scanDialog.show();
  }

  private void showImportRoomModal() {
    LinearLayout body = vertical();
    final EditText codeInput = new EditText(this);
    codeInput.setHint("FORGE-XXXXXX");
    codeInput.setTextColor(INK);
    codeInput.setHintTextColor(MUTED);

    final EditText envInput = new EditText(this);
    envInput.setHint("Paste Convoy Pass or Scan QR above...");
    envInput.setTextColor(INK);
    envInput.setHintTextColor(MUTED);
    envInput.setLines(3);

    Runnable performImport =
        () -> {
          String rawEnv = envInput.getText().toString().trim();
          String[] unpacked = QrCodeHelper.unpackRoomPayload(rawEnv);
          String code = unpacked != null ? unpacked[0] : codeInput.getText().toString().trim();
          String env = unpacked != null ? unpacked[1] : rawEnv;
          if (code.isEmpty() || env.isEmpty()) {
            Toast.makeText(this, "Scan a QR code or enter Room Code and Pass", Toast.LENGTH_SHORT)
                .show();
            return;
          }
          try {
            PeerMultiplayerSync.PeerRivalProfile rival =
                PeerMultiplayerSync.parsePeerRival(env, code);
            boolean ok = PeerMultiplayerSync.injectRivalIntoGame(game, rival);
            if (ok) {
              actionDone();
              dismissCurrent();
              Toast.makeText(
                      this,
                      "Imported rival: " + rival.companyName + " by " + rival.founderName + "!",
                      Toast.LENGTH_LONG)
                  .show();
              selectTab(5); // Switch to Market tab
            }
          } catch (Exception e) {
            Toast.makeText(this, "Invalid Convoy QR or Code: " + e.getMessage(), Toast.LENGTH_LONG)
                .show();
          }
        };

    Button scanBtn =
        button(
            "📷 Scan QR Code with Camera",
            () -> openQrScannerDialog(codeInput, envInput, performImport));
    primary(scanBtn);
    body.addView(scanBtn);

    TextView orLabel = text("Or paste Convoy Room Code & Pass manually:", 11, MUTED, false);
    LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, -2);
    olp.topMargin = dp(8);
    body.addView(orLabel, olp);
    body.addView(codeInput);
    body.addView(envInput);

    Button importAction = button("⚔ Join Convoy & Import Rival", performImport);
    body.addView(importAction);

    showThemedModal(
        "JOIN CONVOY MULTIPLAYER",
        "QR Code & Pass Matchmaking",
        "Scan your friend's QR code to decrypt their company and place them on your Stock Exchange.",
        body,
        "Cancel",
        null);
  }

  private void showCreateBackupPassphraseModal() {
    LinearLayout body = vertical();
    body.addView(text("Set a Secure Passphrase or Key to Encrypt Save:", 12, AMBER, true));
    final EditText passInput = new EditText(this);
    passInput.setHint("e.g. secret-mobility-vault-2026");
    passInput.setTextColor(INK);
    passInput.setHintTextColor(MUTED);
    body.addView(passInput);

    Button createBtn =
        button(
            "🔒 Encrypt & Export Save",
            () -> {
              String pass = passInput.getText().toString().trim();
              if (pass.length() < 4) {
                Toast.makeText(this, "Passphrase must be at least 4 characters", Toast.LENGTH_SHORT).show();
                return;
              }
              try {
                String backup = PeerMultiplayerSync.exportEncryptedBackup(game, pass);
                dismissCurrent();
                showExportEnvelopeModal("ENCRYPTED BACKUP ENVELOPE", "KEY PROTECTED", backup);
              } catch (Exception e) {
                Toast.makeText(this, "Encryption failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
              }
            });
    primary(createBtn);
    body.addView(createBtn);

    showThemedModal(
        "E2EE SAVE BACKUP",
        "Serverless Zero-Knowledge Vault",
        "Your save is encrypted with AES-GCM on this device before leaving. No server can ever read it.",
        body,
        "Cancel",
        null);
  }

  private void showRestoreBackupModal() {
    LinearLayout body = vertical();
    body.addView(text("Enter Backup Passphrase:", 12, AMBER, true));
    final EditText passInput = new EditText(this);
    passInput.setTextColor(INK);
    body.addView(passInput);

    TextView envLabel = text("Paste Encrypted Backup Envelope:", 12, MUTED, false);
    LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(-1, -2);
    elp.topMargin = dp(6);
    body.addView(envLabel, elp);

    final EditText envInput = new EditText(this);
    envInput.setHint("-----BEGIN FAREFORGE ENCRYPTED BACKUP-----\n...");
    envInput.setTextColor(INK);
    envInput.setHintTextColor(MUTED);
    envInput.setLines(6);
    body.addView(envInput);

    Button restoreBtn =
        button(
            "🔓 Decrypt & Restore Save",
            () -> {
              String pass = passInput.getText().toString().trim();
              String env = envInput.getText().toString().trim();
              if (pass.isEmpty() || env.isEmpty()) {
                Toast.makeText(this, "Passphrase and backup content required", Toast.LENGTH_SHORT).show();
                return;
              }
              try {
                Game restoredGame = PeerMultiplayerSync.importEncryptedBackup(env, pass);
                game = restoredGame;
                home = false;
                actionDone();
                dismissCurrent();
                selectTab(0);
                Toast.makeText(this, "Restored " + game.companyName + " successfully!", Toast.LENGTH_LONG).show();
              } catch (Exception e) {
                Toast.makeText(this, "Restore failed (Check passphrase or envelope): " + e.getMessage(), Toast.LENGTH_LONG).show();
              }
            });
    primary(restoreBtn);
    body.addView(restoreBtn);

    showThemedModal(
        "RESTORE ENCRYPTED SAVE",
        "E2EE Zero-Server Restore",
        "Restores your empire without requiring any server account or cloud database.",
        body,
        "Cancel",
        null);
  }

  private String currentLanguageName() {
    String lang = getPreferences(0).getString("lang", game != null ? game.language : "en");
    for (int i = 0; i < I18n.SUPPORTED_CODES.length; i++) {
      if (I18n.SUPPORTED_CODES[i].equalsIgnoreCase(lang)) return I18n.SUPPORTED_NAMES[i];
    }
    return "English";
  }

  void showLanguageModal(boolean canContinue) {
    LinearLayout list = vertical();
    String cur = getPreferences(0).getString("lang", game != null ? game.language : "en");
    for (int i = 0; i < I18n.SUPPORTED_CODES.length; i++) {
      final String code = I18n.SUPPORTED_CODES[i];
      final String name = I18n.SUPPORTED_NAMES[i];
      boolean active = code.equalsIgnoreCase(cur);
      Button b =
          button(
              (active ? "✓ " : "") + name + " (" + code.toUpperCase(Locale.US) + ")",
              () -> {
                getPreferences(0).edit().putString("lang", code).apply();
                if (game != null) game.setLanguage(code);
                dismissCurrent();
                updateHud(true);
                showHome(canContinue);
              });
      if (active) primary(b);
      list.addView(b);
    }
    showThemedModal(
        "GLOBAL LOCALIZATION",
        "Select Language / Idioma",
        "Choose your preferred display language for UI labels, metrics, and dialogues.",
        list,
        "Back",
        () -> showHome(canContinue));
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
      item.addView(text(sc.city + " · " + sc.country, 17, AMBER, true));
      item.addView(text(sc.title, 13, MUTED, false));
      Button quickLaunch =
          button(
              "⚡ Quick Start in " + sc.city + " →",
              () ->
                  startNew(
                      () -> {
                        game = new Game();
                        game.startQuickPlay(cityIdx, Game.GOAL_WORTH_5CR, Game.DIFF_MEDIUM);
                        game.autoDispatch = true;
                      },
                      false));
      primary(quickLaunch);
      item.addView(quickLaunch);
      item.addView(
          button("Customize Goal & Company Name…", () -> main.post(() -> chooseQuickGoal(cityIdx))));
      list.addView(item);
    }
    showThemedModal(
        "QUICK PLAY  ·  SELECT CITY",
        "Choose Your Starting City",
        "Tap Quick Start to jump right in, or customize your goal and company details.",
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
      item.addView(text(Game.quickGoalNameForCity(i, city), 14, INK, true));
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
      item.addView(text(h.title, 17, AMBER, true));
      item.addView(text(Game.formatHistoricalObjective(h), 13, INK, false));
      Button playNow =
          button(
              "⚡ Play Scenario #" + (i + 1) + " →",
              () ->
                  startNew(
                      () -> {
                        game = new Game();
                        game.startHistoricalScenario(index);
                        game.autoDispatch = true;
                      },
                      false));
      primary(playNow);
      item.addView(playNow);
      item.addView(
          button(
              "Customize Founder & Equity…",
              () ->
                  main.post(
                      () ->
                          showFounderRegistrationModal(
                              h.cityId,
                              () -> {
                                game = new Game();
                                game.startHistoricalScenario(index);
                                game.autoDispatch = true;
                              }))));
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
      "100% Ownership · Bootstrapped (+" + Game.money(0L) + " Outside Seed, Full Control)",
      "80% Ownership · Angel Co-Invested (+" + Game.money(3200L) + " Seed Capital)",
      "60% Ownership · Venture Syndicate (+" + Game.money(6400L) + " Seed Capital)",
      "40% Ownership · Public Float (+" + Game.money(9600L) + " Seed Capital)"
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
            + " (" + Game.money(240L) + "/qtr) · Keep "
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
      game.lastActiveTimestampMs = System.currentTimeMillis();
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
