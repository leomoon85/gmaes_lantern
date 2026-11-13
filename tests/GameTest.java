package com.lantern.dispatch;

public class GameTest {
  static void check(boolean value, String why) {
    if (!value) throw new AssertionError(why);
  }

  static void run(Game g, int steps) {
    for (int i = 0; i < steps; i++) g.tick(.2);
  }

  public static void main(String[] args) throws Exception {
    Game g = new Game();
    for (Game.District a : g.districts)
      for (Game.District b : g.districts) {
        int[] path = g.route(a.cell(), b.cell());
        check(path != null, "Every district reachable");
        for (int i = 1; i < path.length; i++) {
          int x = path[i] % Game.W, y = path[i] / Game.W;
          check(Game.road(x, y), "Path must follow roads");
          check(
              Math.abs(x - path[i - 1] % Game.W) + Math.abs(y - path[i - 1] / Game.W) == 1,
              "Adjacent road cells");
        }
      }
    check(g.dispatch(g.requests.get(0).id), "Available driver dispatches");
    check(!g.dispatch(g.requests.get(0).id), "Busy driver cannot double book");
    run(g, 100);
    check(
        g.rides > 0 && g.revenue > 0 && g.grossFares > g.revenue, "Fare split and completed ride");
    double time = g.elapsed;
    g.speed = 0;
    g.tick(.2);
    check(g.elapsed == time, "Paused clock");
    Game poor = new Game();
    poor.cash = 0;
    check(!poor.buyTaxi() && poor.taxis.size() == 1, "Unaffordable driver purchase atomic");
    check(!poor.unlock(2) && !poor.districts.get(2).open, "Unaffordable district atomic");
    Game market = new Game();
    long cash = market.cash;
    check(market.buyShares(0, true), "Margin buy");
    check(
        market.companies.get(0).owned == 10
            && market.marginDebt == 275
            && cash - market.cash == 275,
        "50% loan accounting");
    check(market.sellShares(0), "Sell shares");
    check(market.marginDebt == 0 && market.cash == cash, "Sale repays loan first");
    market.buyShares(0, true);
    market.companies.get(0).price = 15;
    market.maintainMargin();
    check(
        market.marginCalls == 1 && market.marginDebt == 0 && market.portfolioValue() == 0,
        "Margin liquidation");
    check(market.cash == cash - 400, "Negative equity shortfall charged to cash");
    Game merger = new Game();
    merger.cash = 100000;
    for (int i = 0; i < 6; i++) check(merger.buyShares(1, false), "Acquire shares");
    int fleet = merger.taxis.size();
    long before = merger.cash,
        price = merger.mergerPrice(1),
        treasury = merger.companies.get(1).cash;
    check(merger.merge(1), "Controlling stake merger");
    check(
        merger.taxis.size() == fleet + 4 && merger.districts.get(2).open,
        "Merge fleet and territory");
    check(merger.cash == before - price + treasury, "Merger consideration and treasury");
    check(!merger.merge(1), "Cannot acquire twice");
    Game debt = new Game();
    for (int i = 0; i < 6; i++) debt.buyShares(0, true);
    check(!debt.merge(0), "Debt blocks acquisition");
    debt.repayMargin();
    check(debt.marginDebt == 0, "Repay debt");
    Game history = new Game();
    check(!history.setModel(2), "Historical model gate");
    history.elapsed = 9 * 60 - .1;
    history.tick(.2);
    check(history.year() == 2020 && history.historyStage == 3, "Historical event progression");
    int crash = history.companies.get(0).price;
    history.tick(.2);
    check(history.companies.get(0).price == crash, "Shock is not repeated");
    check(history.demandFactor() == .22, "Lockdown demand scenario");
    history.elapsed = 11 * 60;
    history.tick(.2);
    check(history.setModel(2), "Subscription unlock");
    Game expansion = new Game();
    expansion.cash = 100000;
    check(!expansion.expand(1), "Expansion needs track record");
    expansion.unlock(2);
    expansion.rides = 20;
    check(expansion.expand(1) && expansion.branchCount() == 2, "Launch regional branch");
    check(!expansion.expand(1), "No duplicate launch");
    Game auto = new Game();
    auto.autoDispatch = true;
    run(auto, 400);
    check(auto.rides > 0, "Auto dispatch serves passengers");
    Game goal = new Game();
    goal.cash = 1000000;
    goal.rides = 100;
    for (int i = 2; i < 6; i++) goal.unlock(i);
    for (int i = 1; i < 6; i++) goal.expand(i);
    for (int i = 0; i < 9; i++) goal.buyTaxi();
    goal.tick(.2);
    check(goal.milestone, "National objective achievable");
    long awarded = goal.cash;
    goal.tick(.2);
    check(goal.cash == awarded, "One-time national grant");

    // Test Railroad Tycoon mechanics: hub upgrades, facilities, corridor building, ride classes,
    // fleet tiers, bonds/insolvency, world city scenarios
    Game hubs = new Game();
    hubs.cash = 50000;
    check(!hubs.upgradeDistrict(2), "Closed district cannot upgrade hub");
    hubs.requests.clear();
    hubs.createRide(0, 1);
    long baseFare = hubs.requests.get(0).fare;
    check(
        hubs.upgradeDistrict(0) && hubs.districts.get(0).level == 2,
        "Upgrade open district to Lv2");
    check(
        hubs.upgradeDistrict(0) && hubs.districts.get(0).level == 3,
        "Upgrade open district to Lv3");
    check(!hubs.upgradeDistrict(0), "Cannot upgrade beyond Lv3");
    check(
        hubs.buildFacility(0, Game.FAC_LOUNGE)
            && hubs.districts.get(0).hasFacility(Game.FAC_LOUNGE),
        "Build VIP Lounge station facility");
    check(hubs.buildCorridor(0, 1, 2), "Upgrade corridor between stations to Tier 2 Express");
    hubs.requests.clear();
    hubs.createRide(0, 1);
    long upgradedFare = hubs.requests.get(0).fare;
    check(upgradedFare > baseFare, "Upgraded hub & facility increases route fares");
    hubs.unlock(2);
    hubs.requests.clear();
    hubs.createRide(2, 0);
    check(
        hubs.requests.get(0).kind == Game.KIND_AIRPORT, "Airport district generates Airport class");

    Game fleetTest = new Game();
    fleetTest.cash = 20000;
    check(
        fleetTest.upgradeTaxi() && fleetTest.taxis.get(0).tier == 2,
        "Upgrade consist to Tier 2 Hybrid");
    check(
        fleetTest.upgradeTaxi() && fleetTest.taxis.get(0).tier == 3,
        "Upgrade consist to Tier 3 Maglev EV");
    check(!fleetTest.upgradeTaxi(), "Cannot upgrade consist past Tier 3");

    Game finance = new Game();
    long startCash = finance.cash;
    check(
        finance.issueBond() && finance.bonds == 1 && finance.cash == startCash + 5000,
        "Issue corporate bond");
    check(
        finance.repayBond() && finance.bonds == 0 && finance.cash == startCash,
        "Retire corporate bond");
    finance.cash = -3000;
    finance.checkSolvency();
    check(
        finance.restructurings == 1 && finance.bonds == 1 && finance.cash == 1500,
        "Emergency bond restructuring on insolvency");

    // Test all 6 famous world city scenarios & currencies
    for (int s = 0; s < Game.SCENARIOS.length; s++) {
      Game city = new Game(s);
      check(
          city.currentScenario().id == s
              && city.districts.size() == 6
              && city.companies.size() == 3,
          "Scenario " + city.currentScenario().city + " initialized");
      check(
          city.currentScenario().currencySymbol != null
              && city.currentScenario().baseFare > 0
              && city.currentScenario().perKmRate > 0,
          "City currency and tariff standard defined");
      city.cash = 100000;
      city.rides = city.currentScenario().targetRides;
      for (int d = 2; d < 6; d++) city.unlock(d);
      city.tick(.2);
      check(
          city.scenarioWon,
          "Scenario " + city.currentScenario().city + " win condition achievable");
    }

    // Test Drivers, Vehicle Maintenance/Cleanliness/Comfort, User Reviews, Managers &
    // Multi-Language
    Game ops = new Game(0);
    ops.cash = 25000;
    check(ops.formatCityMoney(1500).startsWith("₹"), "Bengaluru currency symbol ₹");
    check(ops.setLanguage("es") && "es".equals(ops.language), "Multi-language switch Spanish");
    check(I18n.isSupported("es") && I18n.isSupported("ja") && I18n.isSupported("ar"), "Supported global languages");
    check("Ciudad".equals(I18n.get("es", "tab_city")), "Spanish localization dictionary");
    check("都市".equals(I18n.get("ja", "tab_city")), "Japanese localization dictionary");
    check("المدينة".equals(I18n.get("ar", "tab_city")), "Arabic localization dictionary");
    ops.taxis.get(0).condition = 55.0;
    ops.taxis.get(0).cleanliness = 45.0;
    check(ops.serviceTaxi(0) && ops.taxis.get(0).condition == 100.0, "Vehicle mechanical service");
    check(
        ops.cleanTaxi(0) && ops.taxis.get(0).cleanliness == 100.0,
        "Vehicle interior cleanliness wash");
    check(
        ops.upgradeComfort(0) && ops.taxis.get(0).comfortLevel == 2,
        "Vehicle cabin comfort upgrade");
    check(ops.recruitDriver("Ramesh K.") && ops.drivers.size() == 2, "Recruit new driver");
    check(ops.trainDriver(1) && ops.drivers.get(1).level == 2, "Train driver skill");
    check(ops.hireManager(0) && ops.managers.get(0).hired, "Hire Fleet Maintenance Manager");
    check(!ops.reviews.isEmpty(), "Passenger reviews tracked");
    for (int d = Game.DIFF_EASIEST; d <= Game.DIFF_PROFESSIONAL; d++) {
      check(
          ops.setDifficulty(d) && ops.difficulty == d,
          "Difficulty mode " + Game.DIFFICULTY_NAMES[d]);
    }
    check(
        ops.setDifficulty(Game.DIFF_EASIEST)
            && ops.difficultyFareFactor() > 1.0
            && ops.difficultyWearFactor() < 1.0,
        "Easiest mode modifiers");
    check(
        ops.setDifficulty(Game.DIFF_PROFESSIONAL)
            && ops.difficultyFareFactor() < 1.0
            && ops.difficultyWearFactor() > 1.0,
        "Professional mode modifiers");

    // Test Solo Quick Play, 20 Railroad-Tycoon Historical Scenarios, and 8-Step Interactive
    // Tutorial
    check(Game.HISTORICAL_SCENARIOS.length >= 20, "20+ historical scenarios defined");
    for (int h = 0; h < Game.HISTORICAL_SCENARIOS.length; h++) {
      Game hs = new Game();
      hs.startHistoricalScenario(h);
      check(
          hs.gameMode == Game.MODE_SOLO_SCENARIO && hs.historicalScenarioId == h,
          "Historical scenario #" + (h + 1) + " initialized");
    }
    Game qp = new Game();
    qp.startQuickPlay(2, Game.GOAL_OWN_4_COMPANIES, Game.DIFF_HIGH);
    check(
        qp.gameMode == Game.MODE_SOLO_QUICK
            && qp.scenarioId == 2
            && qp.quickGoalType == Game.GOAL_OWN_4_COMPANIES
            && qp.difficulty == Game.DIFF_HIGH,
        "Quick Play custom goal & city initialized");
    check(
        qp.ownedCompanyCount() == 1 && qp.establishedCountryCount() == 1,
        "Initial owned companies & countries count");

    Game tut = new Game();
    tut.startTutorial();
    check(
        tut.gameMode == Game.MODE_TUTORIAL && tut.tutorialStep == 1,
        "Tutorial mode starts at step 1");
    check(!tut.advanceTutorial(), "Tutorial cannot skip an unfinished task");
    tut.dispatch(tut.requests.get(0).id);
    check(tut.advanceTutorial(), "Dispatch task");
    tut.rides = 1;
    check(tut.advanceTutorial(), "First completed trip task");
    tut.buyTaxi();
    check(tut.advanceTutorial(), "Fleet task");
    tut.unlock(2);
    check(tut.advanceTutorial(), "Expansion task");
    tut.upgradeDistrict(0);
    check(tut.advanceTutorial(), "Hub upgrade task");
    tut.hireManager(0);
    check(tut.advanceTutorial(), "Manager task");
    tut.buyShares(0, false);
    long preGradCash = tut.cash;
    check(tut.advanceTutorial(), "Shareholding task");
    check(
        tut.tutorialStep == 8 && tut.cash == preGradCash + 5000 && !tut.advanceTutorial(),
        "One-time tutorial grant");

    Game dates = new Game();
    dates.startHistoricalScenario(10);
    check(dates.year() == 2017, "Historical start year applied");
    dates.elapsed = 10000;
    check(dates.year() == 2023, "Historical end year respected");
    Game countries = new Game(2);
    countries.branches[4] = true;
    check(
        countries.branchCount() == 2 && countries.establishedCountryCount() == 1,
        "US cities count as one country");
    Game ambition = new Game();
    ambition.startQuickPlay(0, Game.GOAL_OWN_4_COMPANIES, 2);
    ambition.cash = 100000;
    ambition.rides = 100;
    for (Game.District d : ambition.districts) d.open = true;
    ambition.tick(.1);
    check(!ambition.scenarioWon, "Quick Play does not use city victory condition");
    for (Game.Company c : ambition.companies) c.merged = true;
    ambition.tick(.1);
    check(ambition.scenarioWon, "Selected Quick Play objective is enforced");
    check(new Game(4).districts.get(0).name.equals("Market Street"), "San Francisco is not Paris");
    Game lines = new Game();
    lines.cash = 50000;
    lines.unlock(2);
    lines.requests.clear();
    lines.createRide(2, 0);
    lines.autoDispatch = true;
    lines.taxis.get(0).strictRoute = true;
    lines.tick(.1);
    check(lines.taxis.get(0).status == Game.IDLE, "Line-only vehicle declines unrelated hub");
    lines.taxis.get(0).routeStops.add(2);
    lines.tick(.1);
    check(lines.taxis.get(0).status != Game.IDLE, "Edited route handles matching requests");
    for (int scenario = 0; scenario < 20; scenario++) {
      Game victory = new Game();
      victory.startHistoricalScenario(scenario);
      check(!victory.objectiveComplete(), "Historical objective starts incomplete");
      victory.cash = 2000000;
      victory.rides = 1000;
      victory.cargoTons = 200;
      victory.revenue = 100000;
      victory.rating = 5;
      victory.bonds = 0;
      victory.marginDebt = 0;
      victory.businessModel = 2;
      for (Game.District d : victory.districts) {
        d.open = true;
        d.level = 3;
      }
      for (Game.Company c : victory.companies) c.merged = true;
      java.util.Arrays.fill(victory.branches, true);
      java.util.Arrays.fill(victory.corridorTier, 3);
      for (Game.Manager m : victory.managers) m.hired = true;
      while (victory.taxis.size() < 8) victory.buyTaxi();
      while (victory.drivers.size() < 8) victory.recruitDriver(null);
      for (Game.Taxi t : victory.taxis) {
        t.tier = 3;
        t.cleanliness = 100;
        t.condition = 100;
        t.comfortLevel = 3;
      }
      check(victory.objectiveComplete(), "Historical objective " + scenario + " achievable");
    }

    // Regression tests for CODE-REVIEW.md findings:
    // 1. Modern campaigns (e.g. 2021+) must not replay the 2020 stock crash on first tick
    Game modern = new Game();
    modern.startHistoricalScenario(6); // starts in 2021
    int initialPrice = modern.companies.get(0).price;
    modern.tick(.1);
    check(
        modern.companies.get(0).price == initialPrice,
        "2021+ historical scenario does not replay 2020 stock crash at startup");

    // 2. Passenger trips must not increment cargoTons; cargo trips do
    Game cargoCheck = new Game();
    cargoCheck.requests.clear();
    cargoCheck.createRide(0, 1); // KIND_EXECUTIVE passenger trip
    check(cargoCheck.dispatch(cargoCheck.requests.get(0).id), "Dispatch passenger trip");
    run(cargoCheck, 120);
    check(
        cargoCheck.rides >= 1 && cargoCheck.cargoTons == 0,
        "Passenger trips do not count toward cargo tonnage");
    cargoCheck.cash = 50000;
    cargoCheck.unlock(3); // District 3 generates KIND_CARGO
    cargoCheck.requests.clear();
    cargoCheck.createRide(3, 0);
    for (Game.Taxi t : cargoCheck.taxis) {
      t.status = Game.IDLE;
      t.charge = 0;
    }
    check(cargoCheck.dispatch(cargoCheck.requests.get(0).id), "Dispatch cargo trip");
    run(cargoCheck, 120);
    check(cargoCheck.cargoTons > 0, "Cargo trips increment cargoTons");

    // 3. Unstaffed vehicles cannot dispatch until a driver is assigned, and mergers assign waiting drivers
    Game staffing = new Game();
    staffing.cash = 50000;
    check(staffing.buyTaxi(), "Buy second vehicle without recruiting second driver");
    staffing.taxis.get(0).status = Game.CHARGING;
    staffing.taxis.get(0).charge = 50;
    check(
        !staffing.dispatch(staffing.requests.get(0).id),
        "Unstaffed vehicle cannot be dispatched without a driver");
    check(staffing.recruitDriver("Meera S."), "Recruit driver for second vehicle");
    check(
        staffing.driverFor(staffing.taxis.get(1)) != null,
        "Recruited driver is paired with unstaffed vehicle");
    check(
        staffing.dispatch(staffing.requests.get(0).id),
        "Newly staffed second vehicle dispatches waiting ride");

    Game mergeStaff = new Game();
    mergeStaff.cash = 100000;
    mergeStaff.recruitDriver("Standby Driver");
    for (int i = 0; i < 6; i++) mergeStaff.buyShares(1, false);
    check(mergeStaff.merge(1), "Merge rival company");
    check(
        mergeStaff.driverFor(mergeStaff.taxis.get(1)) != null,
        "Merger pairs waiting unassigned driver with inherited vehicle");

    // 4. Career XP, Level-Up Increments, 12 Medals & Daily/Offline Comeback rewards
    Game career = new Game();
    career.cash = 100000;
    career.rides = 1;
    career.unlock(2);
    career.buyTaxi();
    career.awardXp(200);
    check(career.founderLevel >= 2, "XP gain increments Founder Level");
    check(career.medals[0], "First Dispatch medal unlocks on ride completion");
    check(career.unclaimedCareerRewards > 0, "Level-up and medal unlock queue unclaimed rewards");
    long preClaimCash = career.cash;
    long preClaimPersonal = career.founderCash;
    long queuedReward = career.unclaimedCareerRewards;
    check(career.claimCareerRewards(), "Claiming career rewards succeeds");
    check(
        career.unclaimedCareerRewards == 0
            && career.cash == preClaimCash + queuedReward
            && career.founderCash == preClaimPersonal + Math.max(100L, queuedReward / 4L),
        "Claiming transfers queued rewards to corporate cash & personal bonus");

    long day0 = 100L * 86_400_000L;
    career.lastCheckInEpochDay = 0L;
    career.lastActiveTimestampMs = 0L;
    career.evaluateDailyAndOfflineComeback(day0);
    check(career.dailyStreak == 1, "Initial daily check-in sets Day 1 streak");
    long comebackGain = career.evaluateDailyAndOfflineComeback(day0 + 86_400_000L);
    check(
        career.dailyStreak == 2 && comebackGain > 0,
        "Consecutive day advances streak & queues offline/daily comeback reward");
    long spoofedBackwards = career.evaluateDailyAndOfflineComeback(day0 - 86_400_000L);
    check(
        spoofedBackwards == 0 && career.dailyStreak == 2,
        "Backward system clock change cannot exploit comeback rewards");

    // 5. PeerSplit serverless E2EE backup and multiplayer room match
    String secretPass = "test-vault-secret-2026";
    String backupArmor = PeerMultiplayerSync.exportEncryptedBackup(career, secretPass);
    check(backupArmor.contains("BEGIN FAREFORGE ENCRYPTED BACKUP"), "Encrypted backup armored export");
    Game restoredBackup = PeerMultiplayerSync.importEncryptedBackup(backupArmor, secretPass);
    check(
        restoredBackup.founderLevel == career.founderLevel
            && restoredBackup.cash == career.cash
            && restoredBackup.medals[0],
        "PeerSplit E2EE zero-server restore matches original game state");

    String roomCode = PeerSyncCrypto.generateRoomCode();
    check(roomCode.startsWith("FORGE-"), "Room code generated");
    String roomEnvelope = PeerMultiplayerSync.exportMultiplayerRoomEnvelope(career, roomCode);
    check(roomEnvelope.contains("BEGIN FAREFORGE MULTIPLAYER ROOM"), "Room envelope armored export");
    PeerMultiplayerSync.PeerRivalProfile peer =
        PeerMultiplayerSync.parsePeerRival(roomEnvelope, roomCode);
    check(
        peer.companyName.equals(career.companyName) && peer.founderName.equals(career.founderName),
        "Decrypted peer rival matches host company");
    Game matchGame = new Game(0);
    check(PeerMultiplayerSync.injectRivalIntoGame(matchGame, peer), "Inject peer rival into market");
    check(matchGame.companies.get(0).name.contains(peer.companyName), "Rival slot populated by peer player");

    // 5. City Currency Standardization & Ambition Formatting (Year-Scaled)
    Game londonGame = new Game(1);
    check(Game.money(1000).startsWith("£"), "London active currency symbol is £");
    check(londonGame.formatCityMoney(2500).startsWith("£"), "London formatCityMoney starts with £");
    check(Game.quickGoalNameForCity(Game.GOAL_WORTH_5CR, 1).contains("£"), "London ambition in £");
    Game tokyoGame = new Game(3);
    check(Game.money(1000).startsWith("¥"), "Tokyo active currency symbol is ¥");
    check(Game.quickGoalNameForCity(Game.GOAL_WORTH_5CR, 3).contains("¥"), "Tokyo ambition in ¥");
    check(
        Game.scaleMoney(2000L, "INR", 2024) == 800000L
            && Game.scaleMoney(2000L, "INR", 1965) == 30000L,
        "INR fleet price scales by year standard (₹8,00,000 in 2024 vs ₹30,000 in 1965)");
    Game branchSwitch = new Game(0);
    branchSwitch.cash = 100000L;
    branchSwitch.districts.get(2).open = true;
    branchSwitch.rides = 25;
    check(branchSwitch.expand(1), "Unlock London branch");
    check(branchSwitch.switchCity(1), "Switch active city to London");
    check("£".equals(Game.activeCurrencySymbol), "Switching city updates activeCurrencySymbol to £");
    Game.CityBranchState prevCityState = null;
    for (Game.CityBranchState bs : branchSwitch.cityStates) {
      if (bs.cityId == 0) prevCityState = bs;
    }
    check(
        prevCityState != null
            && prevCityState.aiManaged
            && prevCityState.aiManagerName.contains("AI Manager"),
        "Previous city gets an AI Manager when switching cities");

    Game calendarGame = new Game(0);
    calendarGame.campaignStartYear = 2025;
    calendarGame.campaignEndYear = 2025;
    calendarGame.elapsed = 0;
    check(calendarGame.year() == 2025 && "Jan".equals(calendarGame.monthName()), "Starts at Jan 2025");
    calendarGame.elapsed = 5.5;
    check("Feb".equals(calendarGame.monthName()), "Month advances from Jan to Feb");
    calendarGame.elapsed = 65.0;
    check(calendarGame.year() == 2026 && "Feb".equals(calendarGame.monthName()), "Year advances past 2025 to 2026");

    // 6. Multi-AI Rival Scenarios & Multiple Rival Fleet Cars
    Game multiAi = new Game();
    multiAi.startHistoricalScenario(20);
    check(multiAi.companies.get(0).fleet == 4 && multiAi.companies.get(1).fleet == 4, "Multi-AI London scenario sets 4 fleet per rival");
    check(multiAi.currentHistoricalScenario().targetCompanies == 3, "Multi-AI takeover objective configured");

    // 7. Company Ledger & Annual Investors Meeting
    Game ledgerGame = new Game(0);
    ledgerGame.cash = 50000;
    ledgerGame.createRide(0, 1);
    ledgerGame.dispatch(ledgerGame.requests.get(0).id);
    run(ledgerGame, 120);
    check(ledgerGame.taxis.get(0).tripsCompleted > 0, "Consist performance ledger tracks trips completed");
    check(ledgerGame.taxis.get(0).totalFareEarned > 0, "Consist performance ledger tracks fare revenue");
    check(ledgerGame.districts.get(0).tripsOriginating > 0, "District ledger tracks originating volume");
    check(ledgerGame.lastAnnualMeetingYear > 0, "Annual investors meeting convened on year crossing");
    check(ledgerGame.adjustCeoSalary(100) && ledgerGame.founderSalaryRate == 450L, "Adjust CEO salary in annual meeting");
    check(ledgerGame.adjustCorporateDividend(100) && ledgerGame.corporateDividendRate == 500L, "Adjust dividend in annual meeting");

    ledgerGame.taxis.get(0).condition = 50.0;
    ledgerGame.taxis.get(0).cleanliness = 50.0;
    check(ledgerGame.serviceAllTaxis() && ledgerGame.taxis.get(0).condition == 100.0, "Bulk Service All restores condition");
    check(ledgerGame.cleanAllTaxis() && ledgerGame.taxis.get(0).cleanliness == 100.0, "Bulk Clean All restores cleanliness");

    System.out.println(
        "PASS: roads, dispatch, fares, pause, funds, margin accounting/liquidation, mergers,"
            + " history, subscriptions, expansion, auto dispatch, national goal, hub upgrades,"
            + " station facilities, corridors, fleet tiers, bonds/insolvency, 6 world city"
            + " scenarios, currencies, drivers, maintenance/cleanliness/comfort, managers, reviews,"
            + " multi-language, 5 difficulty modes, Quick Play, 22 Historical Scenarios,"
            + " Interactive Tutorial, Company Ledger & Annual Investors Meeting");
  }
}
