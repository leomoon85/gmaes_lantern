package com.lantern.dispatch;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Accessible Android controls for the simulation; no actions depend on Canvas hit testing. */
final class GamePanels {
  private final MainActivity ui;

  interface Operation {
    boolean run();
  }

  GamePanels(MainActivity ui) {
    this.ui = ui;
  }

  private Game g() {
    return ui.game;
  }

  private void perform(Operation operation) {
    String old = g().message;
    if (!operation.run() && Objects.equals(old, g().message))
      g().message = "Action unavailable. Check available cash, capacity, and upgrade limits.";
    ui.actionDone();
  }

  private void action(LinearLayout card, String label, Operation operation) {
    card.addView(ui.button(label, () -> perform(operation)));
  }

  void render(int tab, LinearLayout parent) {
    switch (tab) {
      case 1:
        fleet(parent);
        break;
      case 2:
        hubs(parent);
        break;
      case 3:
        team(parent);
        break;
      case 4:
        market(parent);
        break;
      case 5:
        news(parent);
        break;
      default:
        break;
    }
  }

  private void fleet(LinearLayout parent) {
    Game game = g();
    LinearLayout intro =
        ui.card(
            parent,
            "Your fleet · " + game.taxis.size() + " / 12",
            game.idleCount() + " available · " + game.modelName());
    Button buy =
        ui.button(
            "Buy vehicle · " + Game.money(game.taxiPrice()), () -> perform(() -> g().buyTaxi()));
    ui.primary(buy);
    intro.addView(buy);
    action(
        intro,
        "Upgrade next vehicle · " + Game.money(game.taxiUpgradePrice()),
        () -> g().upgradeTaxi());
    intro.addView(ui.button("Business model", () -> models()));
    float density = ui.getResources().getDisplayMetrics().density;
    for (int i = 0; i < game.taxis.size(); i++) {
      final int index = i;
      Game.Taxi t = game.taxis.get(i);
      Game.Driver assigned = game.driverFor(t);
      String driverLine =
          assigned != null
              ? "Operator: " + assigned.name + " (Lv" + assigned.level + ")"
              : (game.isStaffed(t)
                  ? "Operator: Autonomous AI Drive"
                  : "⚠ Unstaffed — Recruit a driver in Team");
      LinearLayout c =
          ui.card(
              parent,
              "Vehicle " + t.id + " · " + t.tierName(),
              new String[] {"Available", "Collecting passenger", "On a trip", "Turnaround"}
                      [t.status]
                  + " · "
                  + driverLine
                  + "\nComfort Lv"
                  + t.comfortLevel
                  + " · Capacity "
                  + t.capacity
                  + " pax");
      int condPct = (int) Math.round(t.condition);
      int cleanPct = (int) Math.round(t.cleanliness);
      c.addView(
          BrandArt.meterRow(
              ui,
              "Mechanical Integrity",
              condPct,
              condPct >= 70 ? 0xff7ee2b8 : condPct >= 35 ? 0xfff5c472 : 0xffef9078,
              condPct + "%",
              density));
      c.addView(
          BrandArt.meterRow(
              ui,
              "Cabin Cleanliness",
              cleanPct,
              cleanPct >= 75 ? 0xff73d6cf : cleanPct >= 50 ? 0xfff5c472 : 0xffef9078,
              cleanPct + "%",
              density));
      action(
          c,
          "Service · " + Game.money(game.managers.get(0).hired ? 220 : 300),
          () -> g().serviceTaxi(index));
      action(c, "Clean · 120 cr", () -> g().cleanTaxi(index));
      action(
          c,
          "Improve comfort · " + Game.money(600L * t.comfortLevel),
          () -> g().upgradeComfort(index));
      c.addView(ui.button("Route · " + t.routeStops.size() + " hubs", () -> editRoute(index)));
      c.addView(
          ui.button(
              t.strictRoute
                  ? "Line service · tap for flexible"
                  : "Flexible service · tap for line only",
              () -> {
                t.strictRoute = !t.strictRoute;
                g().message =
                    t.strictRoute
                        ? "This vehicle serves requests between its selected hubs only."
                        : "This vehicle can serve other hubs when its line has no requests.";
                ui.actionDone();
              }));
    }
  }

  private void editRoute(int vehicleIndex) {
    Game.Taxi vehicle = g().taxis.get(vehicleIndex);
    String[] labels = new String[g().districts.size()];
    boolean[] selected = new boolean[labels.length];
    for (int i = 0; i < labels.length; i++) {
      labels[i] = g().districts.get(i).name + (g().districts.get(i).open ? "" : " · Not open");
      selected[i] = vehicle.routeStops.contains(i);
    }
    ui.showPaused(
        new AlertDialog.Builder(ui)
            .setTitle("Service hubs · Vehicle " + vehicle.id)
            .setMultiChoiceItems(
                labels,
                selected,
                (dialog, index, checked) -> {
                  if (checked && !g().districts.get(index).open) {
                    selected[index] = false;
                    ((AlertDialog) dialog).getListView().setItemChecked(index, false);
                  } else selected[index] = checked;
                })
            .setPositiveButton(
                "Apply route",
                (dialog, which) -> {
                  int count = 0;
                  for (boolean value : selected) if (value) count++;
                  if (count >= 2) {
                    vehicle.routeStops.clear();
                    for (int i = 0; i < selected.length; i++)
                      if (selected[i]) vehicle.routeStops.add(i);
                    g().message =
                        "Route updated. Auto dispatch prioritises requests between these hubs.";
                  } else
                    g().message = "Choose at least two open hubs. The previous route was kept.";
                  ui.actionDone();
                })
            .setNegativeButton("Cancel", null)
            .create());
  }

  private void hubs(LinearLayout parent) {
    Game game = g();
    float density = ui.getResources().getDisplayMetrics().density;
    ui.card(parent, "A city that connects", game.openCount() + " / 6 hubs · " + game.trafficName());
    for (int i = 0; i < game.districts.size(); i++) {
      final int index = i;
      Game.District d = game.districts.get(i);
      StringBuilder facInfo = new StringBuilder();
      if (d.hasFacility(Game.FAC_MAINT)) facInfo.append(" [Depot]");
      if (d.hasFacility(Game.FAC_LOUNGE)) facInfo.append(" [VIP Lounge]");
      if (d.hasFacility(Game.FAC_CARGO)) facInfo.append(" [Cargo Dock]");
      if (d.hasFacility(Game.FAC_CHARGER)) facInfo.append(" [Fast Charger]");
      LinearLayout c =
          ui.card(
              parent,
              d.name + (d.open ? " · " + d.levelName() : ""),
              d.open
                  ? "Level "
                      + d.level
                      + " · "
                      + d.specialty
                      + (facInfo.length() > 0 ? "\nFacilities:" + facInfo : "")
                  : "Landmark: " + d.landmark + " · Ready for expansion");
      if (d.open) {
        int avgDemand =
            (d.demandMult[0] + d.demandMult[1] + d.demandMult[2] + d.demandMult[3]) / 4;
        c.addView(
            BrandArt.meterRow(
                ui,
                "Station Fare Surge",
                Math.min(100, (avgDemand - 70)),
                0xfff5c472,
                avgDemand + "% tariff",
                density));
      }
      c.addView(
          ui.button(
              d.open ? "Manage hub" : "Open · " + Game.money(game.districtPrice(i)),
              () -> {
                if (d.open) showHub(index);
                else perform(() -> g().unlock(index));
              }));
    }
    LinearLayout branch =
        ui.card(
            parent,
            "Beyond this city",
            game.establishedCountryCount()
                + " countries · "
                + game.branchCount()
                + " city branches\nExpansion needs 20 trips and three local hubs.");
    for (int i = 0; i < game.branches.length; i++) {
      final int index = i;
      Button b =
          ui.button(
              game.regions[i]
                  + (game.branches[i] ? " · Operating" : " · " + Game.money(game.branchPrice(i))),
              () -> perform(() -> g().expand(index)));
      b.setEnabled(!game.branches[i]);
      branch.addView(b);
    }
  }

  void showHub(int index) {
    Game game = g();
    Game.District d = game.districts.get(index);
    if (!d.open) {
      ui.showPaused(
          new AlertDialog.Builder(ui)
              .setTitle(d.name)
              .setMessage(
                  "Connect this destination for " + Game.money(game.districtPrice(index)) + ".")
              .setPositiveButton("Open hub", (dialog, which) -> perform(() -> g().unlock(index)))
              .setNegativeButton("Later", null)
              .create());
      return;
    }
    int[] flags = {Game.FAC_MAINT, Game.FAC_LOUNGE, Game.FAC_CARGO, Game.FAC_CHARGER};
    String[] names = {"Service depot", "Passenger lounge", "Parcel terminal", "Charging station"};
    ArrayList<String> choices = new ArrayList<>();
    choices.add(
        d.level >= 3
            ? "Hub fully upgraded"
            : "Upgrade hub · " + Game.money(game.districtUpgradePrice(index)));
    for (int i = 0; i < flags.length; i++)
      choices.add(
          names[i]
              + (d.hasFacility(flags[i])
                  ? " · Installed"
                  : " · " + Game.money(game.facilityPrice(flags[i]))));
    if (index != 0) {
      choices.add("Express corridor · " + Game.money(game.corridorLinkCost(0, index, 2)));
      choices.add("Premium corridor · " + Game.money(game.corridorLinkCost(0, index, 3)));
    }
    ui.showPaused(
        new AlertDialog.Builder(ui)
            .setTitle(d.name + " · Level " + d.level)
            .setItems(
                choices.toArray(new String[0]),
                (dialog, choice) ->
                    perform(
                        () ->
                            choice == 0
                                ? g().upgradeDistrict(index)
                                : choice <= 4
                                    ? g().buildFacility(index, flags[choice - 1])
                                    : g().buildCorridor(0, index, choice == 5 ? 2 : 3)))
            .setNegativeButton("Back", null)
            .create());
  }

  private void team(LinearLayout parent) {
    Game game = g();
    float density = ui.getResources().getDisplayMetrics().density;
    LinearLayout intro =
        ui.card(
            parent,
            "People behind the journeys",
            game.drivers.size()
                + " driver partners · "
                + String.format(Locale.US, "%.2f", game.rating)
                + " stars");
    action(intro, "Recruit driver · 450 cr", () -> g().recruitDriver(null));
    for (int i = 0; i < game.drivers.size(); i++) {
      final int index = i;
      Game.Driver d = game.drivers.get(i);
      String assignedLabel =
          d.assignedTaxiId > 0 ? "Assigned to Vehicle #" + d.assignedTaxiId : "Standby · Unassigned";
      LinearLayout c =
          ui.card(
              parent,
              d.name + " · Lv" + d.level,
              assignedLabel
                  + " · Skill "
                  + String.format(Locale.US, "%.2f", d.skill)
                  + "★");
      int fatiguePct = (int) Math.round(d.fatigue);
      c.addView(
          BrandArt.meterRow(
              ui,
              "Shift Fatigue",
              fatiguePct,
              fatiguePct >= 80 ? 0xffef9078 : fatiguePct >= 45 ? 0xfff5c472 : 0xff7ee2b8,
              fatiguePct + "%",
              density));
      action(c, "Train · " + Game.money(350L * d.level), () -> g().trainDriver(index));
      action(c, "Rest driver", () -> g().restDriver(index));
    }
    String[] perks = {
      "Auto-repairs vehicles < 70% & cuts service cost",
      "Auto-sanitizes cabins +12% after every trip",
      "Halves driver shift fatigue accumulation",
      "Boosts fleet corridor speed by +10%",
      "Softens negative passenger rating impact by 50%"
    };
    for (int i = 0; i < game.managers.size(); i++) {
      final int index = i;
      Game.Manager m = game.managers.get(i);
      String perk = i < perks.length ? perks[i] : "Executive operations bonus";
      LinearLayout c =
          ui.card(
              parent,
              m.role + " · " + m.name,
              (m.hired
                      ? "✓ Active in Executive Cabinet · "
                      : "Recruitment · " + Game.money(m.cost) + " · ")
                  + perk);
      if (!m.hired) action(c, "Hire manager", () -> g().hireManager(index));
    }
    for (Game.Review r : game.reviews)
      ui.card(parent, r.stars + " stars · " + r.passenger, r.comment);
  }

  private void market(LinearLayout parent) {
    Game game = g();
    LinearLayout balance =
        ui.card(
            parent,
            "Company ledger",
            "Net worth "
                + Game.money(game.netWorth())
                + "\nRevenue "
                + Game.money(game.revenue)
                + " · Costs "
                + Game.money(game.expenses)
                + "\nMargin debt "
                + Game.money(game.marginDebt)
                + "\nBonds "
                + Game.money(game.bondDebt())
                + " · Dividends "
                + Game.money(game.dividends));
    action(balance, "Issue bond · +5,000 cr", () -> g().issueBond());
    action(balance, "Repay bond · 5,000 cr", () -> g().repayBond());
    balance.addView(
        ui.button(
            "Repay margin borrowing",
            () -> {
              g().repayMargin();
              ui.actionDone();
            }));
    for (int i = 0; i < game.companies.size(); i++) {
      final int index = i;
      Game.Company c = game.companies.get(i);
      LinearLayout company =
          ui.card(
              parent,
              c.name,
              c.merged
                  ? "Acquired by Fareforge"
                  : Game.money(c.price)
                      + " / share · "
                      + c.owned
                      + "% owned\nFictional exchange · Prices follow this campaign.");
      if (c.merged) continue;
      float density = ui.getResources().getDisplayMetrics().density;
      company.addView(
          BrandArt.meterRow(
              ui,
              "Voting Control (60% needed to merge)",
              c.owned,
              c.owned >= 60 ? 0xff7ee2b8 : 0xfff5c472,
              c.owned + "%",
              density));
      company.addView(
          new PriceGraph(ui, c.priceHistory, c.color),
          new LinearLayout.LayoutParams(-1, ui.dp(55)));
      action(
          company,
          "Buy 10 shares · " + Game.money(c.price * 10L),
          () -> g().buyShares(index, false));
      company.addView(
          ui.button(
              "Buy on margin · " + Game.money(c.price * 5L) + " cash",
              () ->
                  ui.confirm(
                      "Borrow to invest?",
                      "Half the purchase cost is borrowed. Quarterly interest is 2%. Below 30%"
                          + " equity your portfolio is sold to settle debt.",
                      () -> perform(() -> g().buyShares(index, true)))));
      action(company, "Sell 10 shares", () -> g().sellShares(index));
      company.addView(
          ui.button(
              "Acquire · " + Game.money(game.mergerPrice(index)),
              () ->
                  ui.confirm(
                      "Acquire " + c.name + "?",
                      "Requires a 60% stake, no margin debt, and payment for the remaining shares"
                          + " plus integration.",
                      () -> perform(() -> g().merge(index)))));
    }
    LinearLayout history =
        ui.card(
            parent,
            "Historical share-price archive",
            "Dated USD observations, separate from the fictional exchange.");
    int count = 0;
    for (Game.Report r : game.reports)
      if (r.id.startsWith("quote-")
          || r.id.equals("real-lyft-ipo")
          || r.id.equals("real-uber-ipo")) {
        history.addView(ui.button(r.date + " · " + r.title, () -> ui.showReport(r)));
        count++;
      }
    if (count == 0)
      history.addView(
          ui.text(
              "The first source is Lyft’s March 2019 IPO. References unlock with the campaign"
                  + " calendar.",
              12,
              MainActivity.MUTED,
              false));
  }

  private void news(LinearLayout parent) {
    ui.card(
        parent,
        "The Mobility Post",
        "Historical events, dated share observations, and the story of your company. Reading an"
            + " edition pauses play.");
    if (g().reports.isEmpty())
      ui.card(
          parent,
          "The first edition is on its way",
          "Historical editions arrive as the campaign calendar advances.");
    for (Game.Report r : g().reports) {
      LinearLayout c =
          ui.card(parent, r.title, r.date + " · " + r.category + (r.read ? "" : " · New"));
      c.addView(ui.button("Read edition", () -> ui.showReport(r)));
    }
  }

  private void models() {
    ui.showPaused(
        new AlertDialog.Builder(ui)
            .setTitle("Business model")
            .setSingleChoiceItems(
                new String[] {
                  "Commission · retain 32% of fares",
                  "Corporate commute · retain 40% (2015+)",
                  "Driver subscription · 80 cr / quarter (2022+)"
                },
                g().businessModel,
                (dialog, index) -> {
                  dialog.dismiss();
                  perform(() -> g().setModel(index));
                })
            .setNegativeButton("Back", null)
            .create());
  }

  void overview() {
    ui.showPaused(
        new AlertDialog.Builder(ui)
            .setTitle("Your company")
            .setMessage(
                g().objectiveText()
                    + "\n\n"
                    + g().rides
                    + " trips · "
                    + g().taxis.size()
                    + " vehicles\n"
                    + g().openCount()
                    + " hubs · "
                    + g().ownedCompanyCount()
                    + " companies\n"
                    + g().establishedCountryCount()
                    + " countries\n"
                    + Game.money(g().netWorth())
                    + " net worth")
            .setPositiveButton("Keep building", null)
            .create());
  }

  private static final class PriceGraph extends View {
    private final java.util.List<Integer> values;
    private final Paint p = new Paint(3);
    private final int color;

    PriceGraph(Context context, java.util.List<Integer> values, int color) {
      super(context);
      this.values = new ArrayList<>(values);
      this.color = color;
      setContentDescription("Recent fictional share prices: " + values);
    }

    @Override
    protected void onDraw(Canvas c) {
      if (values.size() < 2) return;
      int min = Collections.min(values), max = Collections.max(values);
      p.setColor(color);
      p.setStrokeWidth(4);
      for (int i = 1; i < values.size(); i++)
        c.drawLine(
            (i - 1) * getWidth() / (float) (values.size() - 1),
            getHeight()
                - 8
                - (values.get(i - 1) - min) * (getHeight() - 16f) / Math.max(1, max - min),
            i * getWidth() / (float) (values.size() - 1),
            getHeight() - 8 - (values.get(i) - min) * (getHeight() - 16f) / Math.max(1, max - min),
            p);
    }
  }
}
