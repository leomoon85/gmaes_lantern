package com.lantern.dispatch;

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
        ledger(parent);
        break;
      case 5:
        market(parent);
        break;
      case 6:
        news(parent);
        break;
      default:
        break;
    }
  }

  private void addCitySwitcherBannerIfMultiBranch(LinearLayout parent) {
    Game game = g();
    if (game.branchCount() <= 1) return;
    LinearLayout banner =
        ui.card(
            parent,
            "Active City HQ · " + game.currentScenario().city,
            "Operating in "
                + game.branchCount()
                + " cities across "
                + game.establishedCountryCount()
                + " countries. Tap any operating branch below to switch city management.");
    for (int i = 0; i < game.branches.length; i++) {
      if (!game.branches[i]) continue;
      final int cityIdx = i;
      boolean active = (i == game.scenarioId);
      Button b =
          ui.button(
              (active ? "📍 Managing: " : "✈ Switch to ")
                  + game.regions[i]
                  + " ("
                  + Game.SCENARIOS[i].country
                  + ")",
              () -> {
                if (!active) {
                  perform(
                      () -> {
                        boolean ok = g().switchCity(cityIdx);
                        if (ok) ui.onCitySwitched();
                        return ok;
                      });
                }
              });
      if (active) ui.primary(b);
      banner.addView(b);
    }
  }

  private void fleet(LinearLayout parent) {
    Game game = g();
    game.evaluateMedals();
    addCitySwitcherBannerIfMultiBranch(parent);
    if (game.unclaimedCareerRewards > 0) {
      LinearLayout rewardCard =
          ui.card(
              parent,
              "🎁 Level & Medal Rewards Ready!",
              "You reached Lv"
                  + game.founderLevel
                  + " ("
                  + game.founderRankTitle()
                  + ") · "
                  + game.unlockedMedalCount()
                  + "/12 Medals unlocked.");
      Button claimBtn =
          ui.button(
              "🎁 Claim +" + Game.money(game.unclaimedCareerRewards) + " Bonus",
              () -> perform(() -> g().claimCareerRewards()));
      ui.primary(claimBtn);
      rewardCard.addView(claimBtn);
    }
    LinearLayout intro =
        ui.card(
            parent,
            game.companyName + " Fleet · " + game.taxis.size() + " / 12",
            game.currentScenario().city
                + " Branch · "
                + game.idleCount()
                + " available · "
                + game.modelName());
    Button buy =
        ui.button(
            "Buy vehicle · " + Game.money(game.taxiPrice()), () -> perform(() -> g().buyTaxi()));
    ui.primary(buy);
    intro.addView(buy);
    action(
        intro,
        "Upgrade next vehicle · " + Game.money(game.taxiUpgradePrice()),
        () -> g().upgradeTaxi());
    intro.addView(
        ui.buttonRow(
            ui.button("Service All", () -> perform(() -> g().serviceAllTaxis())),
            ui.button("Clean All", () -> perform(() -> g().cleanAllTaxis()))));
    intro.addView(ui.button("Business model · " + game.modelName(), () -> models()));
    float density = ui.getResources().getDisplayMetrics().density;
    for (int i = 0; i < game.taxis.size(); i++) {
      final int index = i;
      Game.Taxi t = game.taxis.get(i);
      Game.Driver assigned = game.driverFor(t);
      String driverLine =
          assigned != null
              ? assigned.name + " (Lv" + assigned.level + ")"
              : (game.isStaffed(t)
                  ? "Autonomous AI"
                  : "⚠ Recruit driver in Team");
      LinearLayout c =
          ui.card(
              parent,
              "Vehicle " + t.id + " · " + t.tierName(),
              new String[] {"Available", "Picking up", "On trip", "Turnaround"}[t.status]
                  + " · "
                  + driverLine
                  + " · Comfort Lv"
                  + t.comfortLevel);
      int condPct = (int) Math.round(t.condition);
      int cleanPct = (int) Math.round(t.cleanliness);
      c.addView(
          BrandArt.meterRow(
              ui,
              "Mechanical",
              condPct,
              condPct >= 70 ? 0xff7ee2b8 : condPct >= 35 ? 0xfff5c472 : 0xffef9078,
              condPct + "%",
              density));
      c.addView(
          BrandArt.meterRow(
              ui,
              "Cleanliness",
              cleanPct,
              cleanPct >= 75 ? 0xff73d6cf : cleanPct >= 50 ? 0xfff5c472 : 0xffef9078,
              cleanPct + "%",
              density));
      c.addView(
          ui.buttonRow(
              ui.button(
                  "Service · " + Game.money(game.managers.get(0).hired ? 220 : 300),
                  () -> perform(() -> g().serviceTaxi(index))),
              ui.button("Clean · " + Game.money(120), () -> perform(() -> g().cleanTaxi(index)))));
      if (assigned == null && !game.isStaffed(t)) {
        Button recruitBtn =
            ui.button(
                "Recruit Driver · " + Game.money(450),
                () -> perform(() -> g().recruitDriver(null)));
        ui.primary(recruitBtn);
        c.addView(recruitBtn);
      }
      c.addView(
          ui.buttonRow(
              ui.button(
                  "Comfort · " + Game.money(600L * t.comfortLevel),
                  () -> perform(() -> g().upgradeComfort(index))),
              ui.button("Route · " + t.routeStops.size() + " hubs", () -> editRoute(index))));
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
    boolean[] selected = new boolean[g().districts.size()];
    LinearLayout body = ui.vertical();
    for (int i = 0; i < g().districts.size(); i++) {
      final int idx = i;
      Game.District d = g().districts.get(i);
      selected[i] = vehicle.routeStops.contains(i);
      CheckBox cb = new CheckBox(ui);
      cb.setText(d.name + (d.open ? " · Level " + d.level : " · Locked Hub"));
      cb.setTextColor(d.open ? MainActivity.INK : MainActivity.MUTED);
      cb.setTextSize(13);
      cb.setEnabled(d.open);
      cb.setChecked(selected[i]);
      cb.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8));
      cb.setOnCheckedChangeListener((btn, isChecked) -> selected[idx] = isChecked);
      body.addView(cb);
    }
    Button apply =
        ui.button(
            "Apply Line Schedule",
            () -> {
              int count = 0;
              for (boolean v : selected) if (v) count++;
              if (count >= 2) {
                vehicle.routeStops.clear();
                for (int i = 0; i < selected.length; i++)
                  if (selected[i]) vehicle.routeStops.add(i);
                g().message = "Route updated. Auto dispatch prioritises these hubs.";
              } else {
                g().message = "Choose at least two open hubs. Previous route kept.";
              }
              ui.dismissCurrent();
              ui.actionDone();
            });
    ui.primary(apply);
    body.addView(apply);
    ui.showThemedModal(
        "LINE DISPATCH SCHEDULE",
        "Service Hubs · Vehicle #" + vehicle.id,
        "Select at least 2 open hubs in " + g().currentScenario().city + " for this vehicle's line.",
        body,
        "Cancel",
        null);
  }

  private void hubs(LinearLayout parent) {
    Game game = g();
    float density = ui.getResources().getDisplayMetrics().density;
    addCitySwitcherBannerIfMultiBranch(parent);
    ui.card(
        parent,
        game.currentScenario().city + " Network",
        game.openCount() + " / 6 hubs open · " + game.trafficName());
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
                  ? "Lv" + d.level + " · " + d.specialty + facInfo
                  : d.landmark + " · Locked");
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
              d.open ? "Manage hub & corridors" : "Open Hub · " + Game.money(game.districtPrice(i)),
              () -> {
                if (d.open) showHub(index);
                else perform(() -> g().unlock(index));
              }));
    }
    LinearLayout branch =
        ui.card(
            parent,
            "Global Multi-City Operations",
            game.establishedCountryCount()
                + " countries · "
                + game.branchCount()
                + " city branches\nLaunch new cities (needs 3 open hubs & 20 rides) or tap any operating city to switch active management.");
    for (int i = 0; i < game.branches.length; i++) {
      final int index = i;
      boolean operating = game.branches[i];
      boolean isCurrent = (i == game.scenarioId);
      String label;
      if (operating) {
        label =
            isCurrent
                ? "📍 " + game.regions[i] + " · Active City (Managing)"
                : "✈ Switch to " + game.regions[i] + " · Manage Branch";
      } else {
        label = "Launch " + game.regions[i] + " · " + Game.money(game.branchPrice(i));
      }
      Button b =
          ui.button(
              label,
              () -> {
                if (operating) {
                  perform(
                      () -> {
                        boolean ok = g().switchCity(index);
                        if (ok) ui.onCitySwitched();
                        return ok;
                      });
                } else {
                  perform(() -> g().expand(index));
                }
              });
      if (isCurrent) ui.primary(b);
      branch.addView(b);
    }
  }

  void showHub(int index) {
    Game game = g();
    Game.District d = game.districts.get(index);
    if (!d.open) {
      LinearLayout body = ui.vertical();
      Button openBtn =
          ui.button(
              "Open " + d.name + " · " + Game.money(game.districtPrice(index)),
              () -> {
                ui.dismissCurrent();
                perform(() -> g().unlock(index));
              });
      ui.primary(openBtn);
      body.addView(openBtn);
      ui.showThemedModal(
          "NEW STATION CHARTER",
          d.name,
          "Landmark: "
              + d.landmark
              + "\nSpecialty: "
              + d.specialty
              + "\nConnect this destination for "
              + Game.money(game.districtPrice(index))
              + ".",
          body,
          "Later",
          null);
      return;
    }
    int[] flags = {Game.FAC_MAINT, Game.FAC_LOUNGE, Game.FAC_CARGO, Game.FAC_CHARGER};
    String[] names = {"Service depot", "Passenger lounge", "Parcel terminal", "Charging station"};
    LinearLayout body = ui.vertical();
    Button upg =
        ui.button(
            d.level >= 3
                ? "✓ Hub at Maximum Level (Lv3 Mega Terminal)"
                : "Upgrade Hub to Lv"
                    + (d.level + 1)
                    + " · "
                    + Game.money(game.districtUpgradePrice(index)),
            () -> {
              ui.dismissCurrent();
              perform(() -> g().upgradeDistrict(index));
            });
    if (d.level < 3) ui.primary(upg);
    else upg.setEnabled(false);
    body.addView(upg);

    for (int i = 0; i < flags.length; i++) {
      final int flag = flags[i];
      boolean inst = d.hasFacility(flag);
      Button fb =
          ui.button(
              names[i]
                  + (inst ? " · ✓ Installed" : " · " + Game.money(game.facilityPrice(flag))),
              () -> {
                ui.dismissCurrent();
                perform(() -> g().buildFacility(index, flag));
              });
      fb.setEnabled(!inst);
      body.addView(fb);
    }
    int hq = game.firstOpenHubIndex();
    if (index != hq) {
      body.addView(
          ui.button(
              "Build Express Corridor (Tier 2) · "
                  + Game.money(game.corridorLinkCost(hq, index, 2)),
              () -> {
                ui.dismissCurrent();
                perform(() -> g().buildCorridor(hq, index, 2));
              }));
      body.addView(
          ui.button(
              "Build High-Speed Corridor (Tier 3) · "
                  + Game.money(game.corridorLinkCost(hq, index, 3)),
              () -> {
                ui.dismissCurrent();
                perform(() -> g().buildCorridor(hq, index, 3));
              }));
    }
    ui.showThemedModal(
        game.currentScenario().city.toUpperCase(Locale.US) + " HUB MANAGEMENT",
        d.name + " · Level " + d.level,
        "Specialty: " + d.specialty + " · Upgrade station facilities and arterial corridors.",
        body,
        "Close",
        null);
  }

  private void team(LinearLayout parent) {
    Game game = g();
    float density = ui.getResources().getDisplayMetrics().density;
    LinearLayout intro =
        ui.card(
            parent,
            "People behind " + game.companyName,
            game.drivers.size()
                + " driver partners in "
                + game.currentScenario().city
                + " · "
                + String.format(Locale.US, "%.2f", game.rating)
                + " stars");
    action(intro, "Recruit driver · " + Game.money(450L), () -> g().recruitDriver(null));
    for (int i = 0; i < game.drivers.size(); i++) {
      final int index = i;
      Game.Driver d = game.drivers.get(i);
      String assignedLabel =
          d.assignedTaxiId > 0 ? "Assigned to Vehicle #" + d.assignedTaxiId : "Standby · Unassigned";
      LinearLayout c =
          ui.card(
              parent,
              d.name + " · Lv" + d.level,
              assignedLabel + " · Skill " + String.format(Locale.US, "%.2f", d.skill) + "★");
      int fatiguePct = (int) Math.round(d.fatigue);
      c.addView(
          BrandArt.meterRow(
              ui,
              "Shift Fatigue",
              fatiguePct,
              fatiguePct >= 80 ? 0xffef9078 : fatiguePct >= 45 ? 0xfff5c472 : 0xff7ee2b8,
              fatiguePct + "%",
              density));
      c.addView(
          ui.buttonRow(
              ui.button(
                  "Train · " + Game.money(350L * d.level),
                  () -> perform(() -> g().trainDriver(index))),
              ui.button("Rest driver", () -> perform(() -> g().restDriver(index)))));
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
                      ? "✓ Active · "
                      : Game.money(m.cost) + " · ")
                  + perk);
      if (!m.hired) action(c, "Hire manager", () -> g().hireManager(index));
    }
    for (int i = 0; i < Math.min(3, game.reviews.size()); i++) {
      Game.Review r = game.reviews.get(i);
      ui.card(parent, r.stars + "★ · " + r.passenger, r.comment);
    }
  }

  private void ledger(LinearLayout parent) {
    Game game = g();
    long ytdProfit = game.annualRevenue - game.annualExpenses;

    // 1. Company Performance Ledger & Annual Investors Meeting
    LinearLayout perf =
        ui.card(
            parent,
            "📊 " + game.companyName + " · Performance Ledger",
            "Fiscal Year "
                + game.year()
                + " YTD\n"
                + "• Revenue: "
                + Game.money(game.annualRevenue)
                + " · Expenses: "
                + Game.money(game.annualExpenses)
                + "\n• YTD Net Profit: "
                + Game.money(ytdProfit)
                + " · Trips: "
                + game.annualRides
                + "\n• Last FY Profit: "
                + Game.money(game.lastYearProfit)
                + " · Investor Approval: "
                + game.investorApprovalRating
                + "%");
    Button meetingBtn =
        ui.button(
            "🏛 Open Annual Investors Meeting & Vote",
            () -> ui.showAnnualMeetingModal());
    ui.primary(meetingBtn);
    perf.addView(meetingBtn);

    // 2. Global City Operations & AI City Managers
    LinearLayout branchCard =
        ui.card(
            parent,
            "🌍 City Operations & AI Managers",
            "Active City: " + game.currentScenario().city + " (" + Game.activeCurrencySymbol + ")");
    for (int i = 0; i < Game.SCENARIOS.length; i++) {
      if (!game.branches[i]) continue;
      final int cityIdx = i;
      Game.Scenario sc = Game.SCENARIOS[i];
      if (i == game.scenarioId) {
        branchCard.addView(
            ui.text(
                "• "
                    + sc.city
                    + " ("
                    + sc.currencySymbol
                    + ") — Active (You · "
                    + game.taxis.size()
                    + " fleet, "
                    + game.openCount()
                    + " hubs)",
                12,
                MainActivity.MINT,
                true));
      } else {
        Game.CityBranchState bs = null;
        for (Game.CityBranchState s : game.cityStates) {
          if (s.cityId == i) {
            bs = s;
            break;
          }
        }
        String mgr =
            bs != null && bs.aiManagerName != null && !bs.aiManagerName.isEmpty()
                ? bs.aiManagerName
                : "AI Manager";
        long earned = bs != null ? bs.aiManagedEarnings : 0L;
        int managedRides = bs != null ? bs.aiManagedRides : 0;
        String actionNote = bs != null && bs.aiLastAction != null ? bs.aiLastAction : "Operating";
        branchCard.addView(
            ui.text(
                "• "
                    + sc.city
                    + " — "
                    + mgr
                    + "\n  "
                    + actionNote
                    + " · "
                    + managedRides
                    + " trips · +"
                    + Game.money(earned),
                11,
                MainActivity.INK,
                false));
        Button switchBtn =
            ui.button(
                "Switch to " + sc.city + " (" + sc.currencySymbol + ")",
                () -> {
                  if (g().switchCity(cityIdx)) ui.onCitySwitched();
                });
        branchCard.addView(switchBtn);
      }
    }

    // 3. Entrepreneur & Corporate Balance Sheet
    LinearLayout entrepreneurCard =
        ui.card(
            parent,
            "👤 " + game.founderName + " · Entrepreneur Ledger",
            "Net Worth: "
                + Game.money(game.entrepreneurNetWorth())
                + " · Personal Cash: "
                + Game.money(game.founderCash)
                + "\nCEO of: "
                + game.activeFounderCeoCompanyName()
                + " · Salary: "
                + Game.money(game.founderSalaryRate)
                + "/qtr · Companies: "
                + game.ownedCompanyCount());

    Button salaryBtn =
        ui.button(
            "CEO Salary Tier: "
                + Game.money(game.founderSalaryRate)
                + "/qtr (Tap to Change)",
            () -> {
              long nextRate =
                  game.founderSalaryRate == 150L
                      ? 350L
                      : game.founderSalaryRate == 350L ? 650L : 150L;
              g().setFounderSalary(nextRate);
              ui.actionDone();
            });
    entrepreneurCard.addView(salaryBtn);

    if (!game.isFounderFlagshipCeo()) {
      Button returnCeo =
          ui.button(
              "Assume CEO Role at "
                  + game.companyName
                  + " (Leave "
                  + game.activeFounderCeoCompanyName()
                  + ")",
              () -> perform(() -> g().assignFounderAsCeo(-1)));
      ui.primary(returnCeo);
      entrepreneurCard.addView(returnCeo);
    }

    action(
        entrepreneurCard,
        "Inject " + Game.money(1500L) + " Personal Cash into " + game.companyName,
        () -> g().investPersonalCashIntoCompany(1500L));

    LinearLayout balance =
        ui.card(
            parent,
            "🏛 " + game.companyName + " · Balance Sheet",
            "Worth: "
                + Game.money(game.companyNetWorth())
                + " · Cash: "
                + Game.money(game.cash)
                + " · Stake: "
                + game.founderEquityPct
                + "%\nCEO: "
                + game.flagshipCeoName()
                + " · Debt: "
                + Game.money(game.marginDebt + game.bondDebt()));

    Button createCo =
        ui.button(
            "+ Incorporate New Company / Spin-Off Venture", () -> ui.showFoundVentureModal());
    ui.primary(createCo);
    balance.addView(createCo);

    action(
        balance,
        "Declare "
            + Game.money(1000L)
            + " Dividend ("
            + game.founderEquityPct
            + "% → "
            + Game.money(1000L * game.founderEquityPct / 100L)
            + " to Founder)",
        () -> g().declareCorporateDividend(1000L));

    if (game.founderEquityPct < 100) {
      action(
          balance,
          "Buy Back 10% Flagship Equity · " + Game.money(game.equitySlicePrice()),
          () -> g().buybackFounderEquity(false));
    }
    if (game.founderEquityPct > 30) {
      action(
          balance,
          "Issue 10% Secondary Equity · +" + Game.money(game.equitySlicePrice()),
          () -> g().issueSecondaryEquity());
    }

    balance.addView(
        ui.button("Rename Flagship Company / Founder", () -> ui.showRenameCompanyModal()));
    action(balance, "Issue bond · +" + Game.money(Game.BOND_FACE), () -> g().issueBond());
    action(balance, "Repay bond · " + Game.money(Game.BOND_FACE), () -> g().repayBond());
    balance.addView(
        ui.button(
            "Repay margin borrowing",
            () -> {
              g().repayMargin();
              ui.actionDone();
            }));

    // 4. Fleet & Route Origination Breakdown
    LinearLayout fleetCard =
        ui.card(
            parent,
            "🚕 Fleet Earnings & Hub Origination (" + game.currentScenario().city + ")",
            "Per-vehicle revenue and hub origination volume:");
    for (Game.Taxi t : game.taxis) {
      String status = t.broken ? "⚠ Broken" : (t.status == Game.IDLE ? "Idle" : "Active");
      fleetCard.addView(
          ui.text(
              "• Car #"
                  + t.id
                  + " (Tier "
                  + t.tier
                  + " · "
                  + status
                  + "): "
                  + t.tripsCompleted
                  + " trips · "
                  + Game.money(t.totalFareEarned)
                  + " · Cond "
                  + Math.round(t.condition)
                  + "%",
              11,
              MainActivity.INK,
              false));
    }
    for (Game.District d : game.districts) {
      if (!d.open && d.tripsOriginating == 0) continue;
      fleetCard.addView(
          ui.text(
              "• Hub "
                  + d.name
                  + " (Lv."
                  + d.level
                  + "): "
                  + d.tripsOriginating
                  + " trips · "
                  + Game.money(d.revenueOriginating),
              11,
              MainActivity.MUTED,
              false));
    }
  }

  private void market(LinearLayout parent) {
    Game game = g();
    float density = ui.getResources().getDisplayMetrics().density;

    int totalCityFleet = game.taxis.size();
    for (Game.Company c : game.companies) if (!c.merged) totalCityFleet += c.fleet;
    totalCityFleet = Math.max(1, totalCityFleet);
    LinearLayout shareCard =
        ui.card(
            parent,
            game.currentScenario().city + " Market Share & Rivalry",
            "Active city fleet: " + totalCityFleet + " vehicles across competing operators");
    int playerPct = Math.round(game.taxis.size() * 100f / totalCityFleet);
    shareCard.addView(
        BrandArt.meterRow(
            ui,
            game.companyName + " (You · " + game.rides + " trips)",
            playerPct,
            0xff77d7c8,
            game.taxis.size() + " cars · " + playerPct + "%",
            density));
    for (Game.Company c : game.companies) {
      if (c.merged) continue;
      int pct = Math.round(c.fleet * 100f / totalCityFleet);
      shareCard.addView(
          BrandArt.meterRow(
              ui,
              c.name + " (" + c.served + " trips)",
              pct,
              c.color,
              c.fleet + " cars · " + pct + "%",
              density));
      TextView act = ui.text("  ↳ " + c.lastAction, 11, MainActivity.MUTED, false);
      act.setPadding(0, 0, 0, ui.dp(4));
      shareCard.addView(act);
    }

    // Entrepreneur's Subsidiary Ventures Section
    if (!game.ventures.isEmpty()) {
      ui.card(
          parent,
          "Your Entrepreneur Ventures (" + game.ventures.size() + ")",
          "Each company has 1 dedicated CEO. You can serve as CEO of 1 company at a time (+25% Venture Revenue).");
      for (int i = 0; i < game.ventures.size(); i++) {
        final int vIdx = i;
        Game.Venture v = game.ventures.get(i);
        boolean founderIsCeoHere = (game.founderCeoTarget == vIdx);
        String statusLine =
            v.sold
                ? "✓ Sold in Buyout"
                : v.merged
                    ? "✓ Merged into " + game.companyName
                    : "Company CEO: "
                        + v.ceoName
                        + (founderIsCeoHere
                            ? " (You · Founder-CEO +25% Rev)"
                            : " (Hired CEO · " + Game.money(v.ceoSalary) + "/qtr)")
                        + "\nCity: "
                        + game.regions[v.cityId]
                        + " (Hubs #"
                        + (v.hubA + 1)
                        + " ↔ #"
                        + (v.hubB + 1)
                        + ")\n"
                        + "Your Equity: "
                        + v.founderEquityPct
                        + "% · Co-Investors: "
                        + (100 - v.founderEquityPct)
                        + "%\n"
                        + "Fleet: "
                        + v.fleet
                        + " vehicles · Valuation: "
                        + Game.money(v.valuation)
                        + " · Cash: "
                        + Game.money(v.cash);
        LinearLayout vc = ui.card(parent, "🏢 " + v.name, statusLine);
        if (v.sold || v.merged) continue;
        vc.addView(
            BrandArt.meterRow(
                ui,
                "Founder Voting Power",
                v.founderEquityPct,
                v.founderEquityPct >= 51 ? 0xff7ee2b8 : 0xfff5c472,
                v.founderEquityPct + "% Stake",
                density));
        if (!founderIsCeoHere) {
          vc.addView(
              ui.button(
                  "Become CEO of "
                      + v.name
                      + " (Appoint Hired CEO to "
                      + game.activeFounderCeoCompanyName()
                      + ")",
                  () -> perform(() -> g().assignFounderAsCeo(vIdx))));
        } else {
          vc.addView(
              ui.button(
                  "Step Down as CEO → Return to " + game.companyName,
                  () -> perform(() -> g().assignFounderAsCeo(-1))));
        }
        if (v.pendingVote) {
          Button voteBtn =
              ui.button(
                  "⚖ Board Vote Pending: " + v.voteType + " Proposal →",
                  () -> showVentureVoteModal(vIdx));
          ui.primary(voteBtn);
          vc.addView(voteBtn);
        } else {
          vc.addView(
              ui.button(
                  "Propose Merger into " + game.companyName + " (Board Vote)",
                  () -> {
                    g().callVentureVote(vIdx, false);
                    ui.actionDone();
                    showVentureVoteModal(vIdx);
                  }));
          vc.addView(
              ui.button(
                  "Solicit Buyout / Sell Company (Board Vote)",
                  () -> {
                    g().callVentureVote(vIdx, true);
                    ui.actionDone();
                    showVentureVoteModal(vIdx);
                  }));
        }
      }
    }

    // Active AI Rivals on the Share Market
    LinearLayout marketHeader =
        ui.card(
            parent,
            game.currentScenario().city + " Stock Exchange & Rivals",
            "Acquire 60% equity in rival companies to force a buyout merger. You can also import human peer companies via Fareforge Convoy QR.");
    Button peerBtn =
        ui.button(
            "🌐 Convoy Multiplayer & QR Backup (E2EE)",
            () -> ui.showPeerMultiplayerModal());
    ui.primary(peerBtn);
    marketHeader.addView(peerBtn);

    for (int i = 0; i < game.companies.size(); i++) {
      final int index = i;
      Game.Company c = game.companies.get(i);
      LinearLayout company =
          ui.card(
              parent,
              c.name + " · " + c.strategyName(),
              c.merged
                  ? "✓ Acquired & Merged"
                  : Game.money(c.price)
                      + "/share · Owned: "
                      + c.owned
                      + "% · Fleet: "
                      + c.fleet
                      + "\n⚡ "
                      + c.lastAction);
      if (c.merged) continue;
      company.addView(
          BrandArt.meterRow(
              ui,
              "Voting Control (60% to merge)",
              c.owned,
              c.owned >= 60 ? 0xff7ee2b8 : 0xfff5c472,
              c.owned + "%",
              density));
      company.addView(
          new PriceGraph(ui, c.priceHistory, c.color),
          new LinearLayout.LayoutParams(-1, ui.dp(55)));
      company.addView(
          ui.buttonRow(
              ui.button(
                  "Buy 10 · " + Game.money(c.price * 10L),
                  () -> perform(() -> g().buyShares(index, false))),
              ui.button("Sell 10", () -> perform(() -> g().sellShares(index)))));
      company.addView(
          ui.buttonRow(
              ui.button(
                  "Margin · " + Game.money(c.price * 5L),
                  () ->
                      ui.confirm(
                          "Borrow to invest?",
                          "Half the purchase cost is borrowed at 2% quarterly interest.",
                          () -> perform(() -> g().buyShares(index, true)))),
              ui.button(
                  "Merge · " + Game.money(game.mergerPrice(index)),
                  () ->
                      ui.confirm(
                          "Acquire " + c.name + "?",
                          "Requires a 60% stake and zero margin debt.",
                          () -> perform(() -> g().merge(index))))));
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

  private void showVentureVoteModal(int ventureIndex) {
    Game game = g();
    if (ventureIndex < 0 || ventureIndex >= game.ventures.size()) return;
    Game.Venture v = game.ventures.get(ventureIndex);
    int outsideStake = 100 - v.founderEquityPct;
    int outsideYes =
        "SELL".equals(v.voteType)
            ? (int) Math.round(outsideStake * 0.75)
            : (int) Math.round(outsideStake * (game.rating >= 4.7 ? 0.70 : 0.45));
    LinearLayout body = ui.vertical();
    float density = ui.getResources().getDisplayMetrics().density;
    body.addView(
        BrandArt.meterRow(
            ui,
            "Your Founder Voting Block",
            v.founderEquityPct,
            0xffedbb73,
            v.founderEquityPct + "%",
            density));
    body.addView(
        BrandArt.meterRow(
            ui,
            "Co-Investor Projected 'YES' Support",
            outsideYes,
            0xff77d7c8,
            outsideYes + "% / " + outsideStake + "%",
            density));

    Button voteYes =
        ui.button(
            "Vote YES (" + (v.founderEquityPct + outsideYes) + "% Total Approval) — Execute " + v.voteType,
            () -> {
              ui.dismissCurrent();
              perform(() -> g().resolveVentureVote(ventureIndex, true));
            });
    ui.primary(voteYes);
    body.addView(voteYes);

    Button voteNo =
        ui.button(
            "Vote NO — Veto / Reject Proposal",
            () -> {
              ui.dismissCurrent();
              perform(() -> g().resolveVentureVote(ventureIndex, false));
            });
    body.addView(voteNo);

    ui.showThemedModal(
        "SHAREHOLDER RESOLUTION VOTE",
        v.name + " · " + v.voteType + " MOTION",
        v.voteSummary + "\n\n51% total shareholder approval is required to pass this motion.",
        body,
        "Defer Vote",
        null);
  }

  private void news(LinearLayout parent) {
    ui.card(
        parent,
        "The Mobility Post",
        "Historical events, dated share observations, and the story of "
            + g().companyName
            + ". Reading an edition pauses play.");
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
    String[] labels = {
      "Commission · retain 32% of fares (Standard)",
      "Corporate commute · retain 40% during rush hour (Unlocks 2015+)",
      "Driver subscription · +" + Game.money(80L) + " / vehicle quarterly (Unlocks 2022+)"
    };
    LinearLayout body = ui.vertical();
    for (int i = 0; i < labels.length; i++) {
      final int idx = i;
      Button b =
          ui.button(
              (g().businessModel == i ? "✓ " : "") + labels[i],
              () -> {
                ui.dismissCurrent();
                perform(() -> g().setModel(idx));
              });
      if (g().businessModel == i) ui.primary(b);
      body.addView(b);
    }
    ui.showThemedModal(
        "REVENUE ARCHITECTURE",
        "Select Business Model",
        "Choose how " + g().companyName + " monetizes trips across its network.",
        body,
        "Back",
        null);
  }

  void overview() {
    Game game = g();
    game.evaluateMedals();
    float density = ui.getResources().getDisplayMetrics().density;
    LinearLayout body = ui.vertical();

    LinearLayout career = ui.vertical();
    career.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
    career.setBackground(ui.shape(0xff182b44, 14));
    LinearLayout.LayoutParams careerLp = new LinearLayout.LayoutParams(-1, -2);
    careerLp.bottomMargin = ui.dp(10);
    career.addView(
        ui.text(
            "🎖 Lv"
                + game.founderLevel
                + " "
                + game.founderRankTitle()
                + "  ·  🔥 Day "
                + game.dailyStreak
                + " Streak",
            15,
            MainActivity.AMBER,
            true));
    career.addView(
        BrandArt.meterRow(
            ui,
            "Career XP (Next Lv" + (game.founderLevel + 1) + ")",
            game.xpProgressPct(),
            MainActivity.AMBER,
            game.founderXp + " / " + game.xpForNextLevel(),
            density));
    int nextMedal = game.nextLockedMedalIndex();
    String nearMiss =
        nextMedal >= 0
            ? "Next Medal: 🏅 "
                + Game.MEDAL_NAMES[nextMedal]
                + " (+"
                + Game.money(Game.MEDAL_REWARDS[nextMedal])
                + ") — "
                + game.medalProgressText(nextMedal)
            : "🏆 All 12 Executive Medals Unlocked!";
    TextView nearText = ui.text(nearMiss, 12, MainActivity.MINT, true);
    LinearLayout.LayoutParams nearLp = new LinearLayout.LayoutParams(-1, -2);
    nearLp.topMargin = ui.dp(6);
    career.addView(nearText, nearLp);
    if (game.unclaimedCareerRewards > 0) {
      Button claimBtn =
          ui.button(
              "🎁 Claim Level & Medal Rewards (+" + Game.money(game.unclaimedCareerRewards) + ")",
              () -> {
                perform(() -> g().claimCareerRewards());
                ui.dismissCurrent();
                overview();
              });
      ui.primary(claimBtn);
      career.addView(claimBtn);
    }
    Button medalsBtn =
        ui.button(
            "🏅 Open Trophy & Medal Cabinet (" + game.unlockedMedalCount() + " / 12)",
            () -> {
              ui.dismissCurrent();
              showMedalsModal();
            });
    career.addView(medalsBtn);

    Button ledgerBtn =
        ui.button(
            "📊 Company Performance Ledger & Investors Review",
            () -> {
              ui.dismissCurrent();
              ui.showAnnualMeetingModal();
            });
    career.addView(ledgerBtn);
    body.addView(career, careerLp);

    LinearLayout stats = ui.vertical();
    stats.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
    stats.setBackground(ui.shape(0xff101d30, 14));
    stats.addView(
        ui.text(
            "👤 ENTREPRENEUR LEDGER ("
                + game.founderName
                + ")\n"
                + "• Entrepreneur Net Worth: "
                + Game.money(game.entrepreneurNetWorth())
                + "\n• Personal Bank Cash: "
                + Game.money(game.founderCash)
                + "\n• Active CEO Seat (1 Co. Limit): "
                + game.activeFounderCeoCompanyName()
                + "\n• CEO Salary: "
                + Game.money(game.founderSalaryRate)
                + "/qtr (Earned: "
                + Game.money(game.founderTotalSalary)
                + ")\n• Personal Dividends Earned: "
                + Game.money(game.founderTotalDividends)
                + "\n\n🏛 COMPANY LEDGER ("
                + game.companyName
                + ")\n"
                + "• Flagship CEO: "
                + game.flagshipCeoName()
                + " · Founder Stake: "
                + game.founderEquityPct
                + "%\n• Company Net Worth: "
                + Game.money(game.companyNetWorth())
                + " · Treasury: "
                + Game.money(game.cash)
                + "\n• Global Fleet: "
                + game.globalFleetCount()
                + " vehicles · Global Open Hubs: "
                + game.globalOpenHubCount()
                + "\n• Owned Companies: "
                + game.ownedCompanyCount()
                + " · Countries: "
                + game.establishedCountryCount(),
            12,
            MainActivity.INK,
            false));
    body.addView(stats);

    Button newVenture =
        ui.button(
            "+ Incorporate Another Company / Venture",
            () -> {
              ui.dismissCurrent();
              ui.showFoundVentureModal();
            });
    ui.primary(newVenture);
    body.addView(newVenture);

    Button renameBtn =
        ui.button(
            "✎ Edit Entrepreneur & Company Names",
            () -> {
              ui.dismissCurrent();
              ui.showRenameCompanyModal();
            });
    body.addView(renameBtn);

    ui.showThemedModal(
        "ENTREPRENEUR VS. COMPANY CHARTER",
        game.founderName + " · " + game.companyName,
        "Current Objective: " + game.objectiveText(),
        body,
        "Keep Building",
        null);
  }

  void showMedalsModal() {
    Game game = g();
    game.evaluateMedals();
    float density = ui.getResources().getDisplayMetrics().density;
    LinearLayout body = ui.vertical();

    LinearLayout rankCard = ui.vertical();
    rankCard.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
    rankCard.setBackground(ui.shape(0xff182b44, 14));
    rankCard.addView(
        ui.text(
            "🎖 Level "
                + game.founderLevel
                + " · "
                + game.founderRankTitle()
                + " · 🔥 Day "
                + game.dailyStreak
                + " Streak",
            15,
            MainActivity.AMBER,
            true));
    rankCard.addView(
        BrandArt.meterRow(
            ui,
            "XP to Lv" + (game.founderLevel + 1),
            game.xpProgressPct(),
            MainActivity.AMBER,
            game.founderXp + " / " + game.xpForNextLevel() + " XP",
            density));
    if (game.unclaimedCareerRewards > 0) {
      Button claim =
          ui.button(
              "🎁 Claim +" + Game.money(game.unclaimedCareerRewards) + " Rewards",
              () -> {
                perform(() -> g().claimCareerRewards());
                ui.dismissCurrent();
                showMedalsModal();
              });
      ui.primary(claim);
      rankCard.addView(claim);
    }
    body.addView(rankCard);

    for (int i = 0; i < Game.MEDAL_NAMES.length; i++) {
      boolean unlocked = game.medals[i];
      LinearLayout row = ui.vertical();
      row.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
      row.setBackground(ui.shape(unlocked ? 0xff16332c : 0xff101d30, 12));
      LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
      rlp.topMargin = ui.dp(6);
      String heading =
          (unlocked ? "🏅 [UNLOCKED] " : "🔒 ")
              + Game.MEDAL_NAMES[i]
              + "  (+"
              + Game.money(Game.MEDAL_REWARDS[i])
              + ")";
      row.addView(ui.text(heading, 14, unlocked ? MainActivity.AMBER : MainActivity.INK, true));
      String detail =
          Game.MEDAL_DESCS[i]
              + (unlocked ? "" : "\nProgress: " + game.medalProgressText(i));
      TextView dView = ui.text(detail, 12, unlocked ? MainActivity.MINT : MainActivity.MUTED, false);
      LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-1, -2);
      dlp.topMargin = ui.dp(3);
      row.addView(dView, dlp);
      body.addView(row, rlp);
    }

    ui.showThemedModal(
        "TROPHY & MEDAL CABINET",
        game.unlockedMedalCount() + " / 12 Executive Medals Unlocked",
        "Complete trips, open hubs, upgrade fleets & merge rivals to earn XP and cash grants.",
        body,
        "Back to Empire",
        null);
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
